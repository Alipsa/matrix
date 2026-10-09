#!/usr/bin/env bash
set -euo pipefail
RELEASE_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
cd "$RELEASE_ROOT"
source "$RELEASE_ROOT/scripts/release-all/preflight.sh"
release_tools_preflight
mode=release
resume=
case "${1:-}" in
  '') [[ $# == 0 ]] || exit 2 ;;
  --dry-run) mode=plan; [[ $# == 1 ]] || exit 2 ;;
  --bundle-only) mode=bundle; [[ $# == 1 ]] || exit 2 ;;
  --resume) mode=resume; [[ $# == 2 ]] || exit 2; resume=$2 ;;
  *) echo 'usage: ./releaseAll.sh [--dry-run|--bundle-only|--resume deployment-id]' >&2; exit 2 ;;
esac
export RUN_EXTERNAL_TESTS=${RUN_EXTERNAL_TESTS:-true}
export RUN_SLOW_TESTS=${RUN_SLOW_TESTS:-true}
export RELEASE_ALL_DEDICATED_EXTERNAL_TESTS=${RELEASE_ALL_DEDICATED_EXTERNAL_TESTS:-false}
if [[ "$mode" != resume ]]; then
  for flag_name in RUN_EXTERNAL_TESTS RUN_SLOW_TESTS RELEASE_ALL_DEDICATED_EXTERNAL_TESTS; do
    [[ "${!flag_name}" == true || "${!flag_name}" == false ]] || { echo "$flag_name must be true or false" >&2; exit 2; }
  done
  if [[ "$RELEASE_ALL_DEDICATED_EXTERNAL_TESTS" == true && "$RUN_EXTERNAL_TESTS" == false ]]; then
    echo 'Dedicated external tests require RUN_EXTERNAL_TESTS=true' >&2
    exit 2
  fi
fi
# Until the enhanced plugin has its own release, use its explicit composite build.
plugin_dir=${RELEASE_ALL_PLUGIN_DIR:-$RELEASE_ROOT/../nexus-release-plugin}
[[ -f "$plugin_dir/src/main/groovy/se/alipsa/gradle/plugin/release/ReleaseAllPlanTask.groovy" ]] || {
  echo 'Enhanced nexus-release-plugin checkout required; set RELEASE_ALL_PLUGIN_DIR' >&2; exit 1;
}
gradle=("$RELEASE_ROOT/gradlew" -p "$RELEASE_ROOT" "-PreleaseAllPluginDir=$plugin_dir" -PreleaseAllMode=true "-PrunExternalTests=$RUN_EXTERNAL_TESTS" "-PrunSlowTests=$RUN_SLOW_TESTS" --configuration-cache-problems=fail)
if [[ "$mode" == resume ]]; then
  "${gradle[@]}" releaseAll "-PreleaseAllResume=$resume"
  exit
fi
[[ -z "$(git status --porcelain)" ]] || { echo 'Commit or stash changes before release planning' >&2; exit 1; }
"${gradle[@]}" releaseAllPlan
manifest="$RELEASE_ROOT/build/releaseAll/manifest.json"
selected=$(groovy -e 'println new groovy.json.JsonSlurper().parse(new File(args[0])).selected.size()' "$manifest")
if [[ "$selected" == 0 ]]; then echo 'No eligible unreleased components'; exit; fi
if [[ "$mode" == plan ]]; then
  "${gradle[@]}" -PreleaseAllSigningReport=true releaseAllSigningCheck
else
  "${gradle[@]}" releaseAllSigningCheck
fi
maven_selected=$(groovy -e 'println new groovy.json.JsonSlurper().parse(new File(args[0])).selected.any { !it.projectPath }' "$manifest")
if [[ "$maven_selected" == true ]]; then
  if [[ ! -d "${GNUPGHOME:-$HOME/.gnupg}" ]] || ! command -v gpg >/dev/null || ! gpg --batch --list-secret-keys --with-colons 2>/dev/null | rg '^sec:' >/dev/null; then
    echo 'Maven signing requires GPG and an available secret key; configure your existing release-profile GPG settings.' >&2
    [[ "$mode" == plan ]] || exit 1
  fi
fi
requirements=$(groovy "$RELEASE_ROOT/scripts/release-all/external-inventory.groovy" "$manifest" "$RELEASE_ROOT")
while IFS= read -r requirement; do
  case "$requirement" in
    gsheets) "${gradle[@]}" :matrix-gsheets:releaseAuthPreflight ;;
    datasets)
      groovy -e '["https://raw.githubusercontent.com/vincentarelbundock/Rdatasets/master/datasets.csv", "https://raw.githubusercontent.com/vincentarelbundock/Rdatasets/master/csv/datasets/mtcars.csv"].each { endpoint -> def c=URI.create(endpoint).toURL().openConnection(); c.connectTimeout=15000; c.readTimeout=15000; try { if(c.responseCode != 200) throw new IOException("Dataset endpoint unavailable: " + endpoint) } finally { c.disconnect() } }' || {
        echo 'Dataset tests require reachable Rdatasets endpoints; see docs/releaseAll.md or set RUN_EXTERNAL_TESTS=false.' >&2; exit 1;
      } ;;
    bigquery)
      [[ -n "${GOOGLE_CLOUD_PROJECT:-}" ]] || { echo 'GOOGLE_CLOUD_PROJECT required' >&2; exit 1; }
      if ! command -v docker >/dev/null || ! docker info >/dev/null; then echo 'Running Docker daemon required' >&2; exit 1; fi
      adc_file=${GOOGLE_APPLICATION_CREDENTIALS:-$HOME/.config/gcloud/application_default_credentials.json}
      [[ -f "$adc_file" ]] || { echo 'BigQuery ADC required; see docs/releaseAll.md or set RELEASE_ALL_DEDICATED_EXTERNAL_TESTS=false.' >&2; exit 1; } ;;
  esac
done <<< "$requirements"
if [[ "$mode" == plan ]]; then
  echo 'Dry run completed; signing configuration will be checked during preparation.'
  exit
fi
scope=$(groovy -e 'new groovy.json.JsonSlurper().parse(new File(args[0])).verificationScope.each { println it }' "$manifest")
analysis=(); formatting=(); builds=()
while IFS= read -r module; do
  [[ -n "$module" ]] || continue
  analysis+=("$module:codenarcMain")
  formatting+=("$module:spotlessCheck")
  builds+=("$module:build")
  [[ "$module" != ':matrix-gsheets' ]] || analysis+=(':matrix-gsheets:codenarcAuthPreflight')
  if [[ "$module" == ':matrix-bigquery' && "$RELEASE_ALL_DEDICATED_EXTERNAL_TESTS" == true ]]; then builds+=(':matrix-bigquery:releaseExternalTest'); fi
done <<< "$scope"
if (( ${#builds[@]} > 0 )); then
  "${gradle[@]}" --no-configuration-cache "${analysis[@]}"
  "${gradle[@]}" --no-configuration-cache "${formatting[@]}"
  "${gradle[@]}" --no-configuration-cache "${builds[@]}" -Pheadless=true
fi
"${gradle[@]}" -PreleaseAllPrepare=true releaseAllStage
"${gradle[@]}" releaseAllPrepareMaven
"${gradle[@]}" bundleAll
[[ "$mode" == bundle ]] || "${gradle[@]}" releaseAll
