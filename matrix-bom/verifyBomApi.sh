#!/usr/bin/env bash
set -euo pipefail

if (( BASH_VERSINFO[0] < 4 )); then
  echo "verifyBomApi.sh requires bash 4 or newer" >&2
  exit 1
fi

if ! command -v groovy >/dev/null 2>&1; then
  echo "verifyBomApi.sh requires the Groovy CLI (groovy) on PATH" >&2
  exit 1
fi

docker_available=false
if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
  docker_available=true
fi

BOM_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)
ROOT_DIR=$(dirname "$BOM_DIR")
USER_REPO_SET=${BOM_VERIFY_REPO+x}
REPO="${BOM_VERIFY_REPO:-$ROOT_DIR/.bom-verify-repo}"
cd "$BOM_DIR"
RELEASE_ROOT=$ROOT_DIR
source "$ROOT_DIR/scripts/release-all/preflight.sh"
release_tools_preflight bom

reject_dotdot() {
  case "/$1/" in
    */../*)
      echo "refusing: BOM_VERIFY_REPO contains a '..' component: $1" >&2
      exit 1
      ;;
  esac
}


reject_symlink_components() {
  local path=$1
  local stop_at=${2:-/}
  while [[ "$path" != "$stop_at" ]]; do
    [[ "$path" != "/" && -n "$path" ]] || {
      echo "refusing: $path is outside $stop_at" >&2
      exit 1
    }
    [[ ! -L "$path" ]] || {
      echo "refusing: $path is a symlink" >&2
      exit 1
    }
    path=$(dirname "$path")
  done
}

assert_safe_repo_path() {
  [[ -n "$REPO" ]] || { echo "BOM_VERIFY_REPO is empty" >&2; exit 1; }
  [[ "$REPO" = /* ]] || { echo "BOM_VERIFY_REPO must be absolute: $REPO" >&2; exit 1; }
  reject_dotdot "$REPO"
  if [[ -n "${USER_REPO_SET:-}" ]]; then
    reject_symlink_components "$REPO"
  elif [[ -L "$REPO" ]]; then
    echo "refusing: default repository path is a symlink: $REPO" >&2
    exit 1
  fi

  local normalized root bom m2 user_home
  normalized=$(RELEASE_PATHS_STRICT_COUNT=1 RELEASE_PATHS_LINES=true groovy "$ROOT_DIR/scripts/release-all/Paths.groovy" "$REPO" "$ROOT_DIR" "$BOM_DIR" "${HOME}/.m2/repository" "$HOME")
  local paths
  mapfile -t paths <<< "$normalized"
  REPO=${paths[0]}
  root=${paths[1]}
  bom=${paths[2]}
  m2=${paths[3]}
  user_home=${paths[4]}

  [[ "$REPO" != "/" ]] || { echo "refusing to delete /" >&2; exit 1; }
  [[ "$REPO" != "$user_home" ]] || { echo 'refusing to delete the home directory' >&2; exit 1; }
  [[ "$REPO" != "$root" ]] || { echo "refusing to delete the repository root" >&2; exit 1; }
  [[ "$REPO" != "$bom" ]] || { echo "refusing to delete matrix-bom" >&2; exit 1; }
  [[ "$REPO" != "$m2" ]] || { echo "refusing to delete the real local repo" >&2; exit 1; }
  case "$root/" in
    "$REPO"/*)
      echo "BOM_VERIFY_REPO contains the project" >&2
      exit 1
      ;;
  esac

  if [[ -e "$REPO" ]]; then
    [[ -d "$REPO" ]] || { echo "$REPO is not a directory" >&2; exit 1; }
    [[ -f "$REPO/.matrix-bom-verify-repo" ]] || {
      echo "$REPO exists but has no .matrix-bom-verify-repo marker; refusing to delete" >&2
      exit 1
    }
    for guard in .git pom.xml build.gradle settings.gradle; do
      [[ ! -e "$REPO/$guard" ]] || {
        echo "$REPO looks like a project directory ($guard); refusing" >&2
        exit 1
      }
    done
  fi
}

norm() {
  printf '%s' "$1" | tr -d '-' | tr '[:upper:]' '[:lower:]'
}

mapfile -t projects < <(sed -nE "s/^[[:space:]]*include[[:space:]]+'(matrix-[a-z-]+)'.*/\1/p" "$ROOT_DIR/settings.gradle")

property_to_module() {
  local wanted project
  wanted=$(norm "${1%Version}")
  for project in "${projects[@]}"; do
    if [[ "$(norm "$project")" == "$wanted" ]]; then
      printf '%s' "$project"
      return 0
    fi
  done
  return 1
}

declare -A all_versions=()
all_output=$(groovy "$BOM_DIR/BomSnapshots.groovy" bom.xml --all)
while IFS='=' read -r property value; do
  [[ -n "$property" ]] || continue
  all_versions["$property"]=$value
done <<< "$all_output"

declare -a releasing=()
declare -A release_versions=()
manifest_mode=false
if (( $# > 0 )) && [[ "$1" == --manifest ]]; then
  [[ $# == 6 && "$3" == --staging && "$5" == --local ]] || { echo 'usage: --manifest FILE --staging DIR --local DIR' >&2; exit 1; }
  manifest_mode=true
  manifest_file=$2
  staging_repo=$4
  REPO=$6
  manifest_properties=$(groovy "$ROOT_DIR/scripts/release-all/manifest-bom.groovy" "$manifest_file" "$BOM_DIR/bom.xml" "$staging_repo")
  while IFS='=' read -r property value; do
    [[ -n "$property" ]] || continue
    if [[ "$property" == __bom_selected ]]; then
      manifest_bom_selected=$value
      continue
    fi
    releasing+=("$property")
    release_versions["$property"]=$value
  done <<< "$manifest_properties"
elif (( $# > 0 )); then
  [[ "$1" == "--modules" && $# == 2 ]] || {
    echo "usage: $0 [--modules property[,property...]]" >&2
    exit 1
  }
  if [[ "$2" != "none" ]]; then
    IFS=',' read -r -a override_properties <<< "$2"
    for property in "${override_properties[@]}"; do
      [[ -n "$property" ]] || { echo "empty property in --modules override" >&2; exit 1; }
      [[ -n "${all_versions[$property]+present}" ]] || {
        echo "unknown BOM property in --modules override: $property" >&2
        exit 1
      }
      releasing+=("$property")
      release_versions["$property"]=${all_versions[$property]}
    done
  fi
else
  detected_output=$(groovy "$BOM_DIR/BomSnapshots.groovy" bom.xml)
  if [[ -n "$detected_output" ]]; then
    while IFS='=' read -r property value; do
      releasing+=("$property")
      release_versions["$property"]=$value
    done <<< "$detected_output"
  fi
fi

it_excluded_groups=(jfx)
if [[ "${RUN_EXTERNAL_TESTS:-false}" != true ]]; then
  it_excluded_groups+=(external)
fi
if [[ "$docker_available" != true ]]; then
  it_excluded_groups+=(emulator)
fi
it_excluded_groups_csv=$(IFS=,; printf '%s' "${it_excluded_groups[*]}")

if [[ "$manifest_mode" == true ]]; then
  # Neither cache nor staging is ever deleted in manifest mode.
  RELEASE_PATHS_RELEASE=true groovy "$ROOT_DIR/scripts/release-all/Paths.groovy" "$staging_repo" "$REPO" "$ROOT_DIR" "$BOM_DIR" "$HOME" "$HOME/.m2/repository" >/dev/null
  mkdir -p "$REPO"
else
assert_safe_repo_path

if [[ "${BOM_VERIFY_FULL_WIPE:-false}" == true ]]; then
  WIPE_MODE="full wipe"
  rm -rf "$REPO"
else
  WIPE_MODE=scoped
  reject_symlink_components "$REPO/se/alipsa/matrix" "$REPO"
  rm -rf "$REPO/se/alipsa/matrix"
fi
mkdir -p "$REPO"
if [[ ! -e "$REPO/.matrix-bom-verify-repo" ]]; then
  : > "$REPO/.matrix-bom-verify-repo"
fi

fi

echo "BOM API verification (${WIPE_MODE:-manifest})"
echo "repository: $REPO"
if [[ "$docker_available" == true ]]; then
  echo "Docker: available — emulator API tests enabled"
else
  echo "Docker: unavailable — emulator API tests skipped"
fi
if (( ${#releasing[@]} == 0 )); then
  echo "no modules under release — all artifacts resolve from Central"
else
  echo "modules under release:"
  for property in "${releasing[@]}"; do
    module=$(property_to_module "$property") || {
      echo "no Gradle project in settings.gradle for $property" >&2
      exit 1
    }
    echo "  $property=${release_versions[$property]} -> $module"
  done
fi

tasks=()
for property in "${releasing[@]}"; do
  module=$(property_to_module "$property") || {
    echo "no Gradle project in settings.gradle for $property" >&2
    exit 1
  }
  tasks+=(":$module:publishToMavenLocal")
done
if [[ "$manifest_mode" == true ]]; then
  echo "using existing staged release artifacts — no Gradle publishing"
elif (( ${#tasks[@]} == 0 )); then
  echo "no modules under release — skipping the Gradle publish"
else
  "$ROOT_DIR/gradlew" -p "$ROOT_DIR" -Dmaven.repo.local="$REPO" "${tasks[@]}"
fi

matrix_repo="${staging_repo:-$REPO}/se/alipsa/matrix"
listing_paths=("$matrix_repo")
for property in "${releasing[@]}"; do
  listing_paths+=("$matrix_repo/$(property_to_module "$property")")
done
listing_output=$(RELEASE_PATHS_TSV=true groovy "$ROOT_DIR/scripts/release-all/Paths.groovy" "${listing_paths[@]}")
declare -A path_entries=() path_directories=()
while IFS='|' read -r listing_path entries directories; do
  path_entries["$listing_path"]=$entries
  path_directories["$listing_path"]=$directories
done <<< "$listing_output"
if (( ${#releasing[@]} == 0 )); then
  if [[ "$manifest_mode" != true && -e "$matrix_repo" ]] && find "$matrix_repo" -mindepth 1 -print -quit | grep -q .; then
    echo "$matrix_repo is not empty although no modules were under release" >&2
    exit 1
  fi
else
  [[ -d "$matrix_repo" ]] || { echo "missing published repository subtree: $matrix_repo" >&2; exit 1; }
  IFS=',' read -r -a actual_entries <<< "${path_entries[$matrix_repo]-}"
  expected_entries=()
  for property in "${releasing[@]}"; do
    expected_entries+=("$(property_to_module "$property")")
  done
  mapfile -t expected_entries < <(printf '%s\n' "${expected_entries[@]}" | sort)
  # Manifest staging also includes selected modules absent from, or newer than, BOM pins.
  [[ "$manifest_mode" == true || "${actual_entries[*]-}" == "${expected_entries[*]-}" ]] || {
    echo "published module set does not match detected BOM properties" >&2
    echo "expected: ${expected_entries[*]-<empty>}" >&2
    echo "actual:   ${actual_entries[*]-<empty>}" >&2
    exit 1
  }

  for property in "${releasing[@]}"; do
    module=$(property_to_module "$property")
    version=${release_versions[$property]}
    module_dir="$matrix_repo/$module"
    version_dir="$module_dir/$version"
    [[ -d "$version_dir" ]] || { echo "missing published version: $module:$version" >&2; exit 1; }
    IFS=',' read -r -a version_entries <<< "${path_directories[$module_dir]-}"
    [[ "${version_entries[*]-}" == "$version" ]] || {
      echo "$module contains an unexpected version directory: ${version_entries[*]-<empty>}" >&2
      exit 1
    }
    compgen -G "$version_dir/*.pom" >/dev/null || { echo "missing POM for $module:$version" >&2; exit 1; }
    compgen -G "$version_dir/*.jar" >/dev/null || { echo "missing JAR for $module:$version" >&2; exit 1; }
  done
fi

verification_settings="$BOM_DIR/verify-settings.xml"
if [[ "$manifest_mode" == true ]]; then
  settings_directory=$(mktemp -d "${TMPDIR:-/tmp}/matrix-bom-settings.XXXXXX")
  verification_settings="$settings_directory/verify-settings.xml"
  trap 'rm -f "$verification_settings"; rmdir "$settings_directory"' EXIT
  groovy "$ROOT_DIR/scripts/release-all/staged-settings.groovy" "$BOM_DIR/verify-settings.xml" "$staging_repo" "$verification_settings"
fi
MVN_ISOLATED=(-s "$verification_settings" -gs "$verification_settings" -Dmaven.repo.local="$REPO")
if [[ "$manifest_mode" != true || "${manifest_bom_selected:-false}" == true ]]; then
  mvn "${MVN_ISOLATED[@]}" -f bom.xml install
else
  echo 'unselected matching BOM resolves from Central; not installing the source BOM'
fi
mvn "${MVN_ISOLATED[@]}" -Papi-it -Dit.excludedGroups="$it_excluded_groups_csv" clean verify

japicmp_new=
for property in "${releasing[@]}"; do
  if [[ "$property" == matrixCoreVersion ]]; then
    japicmp_new=${release_versions[$property]}
    break
  fi
done

if [[ -z "$japicmp_new" ]]; then
  echo "japicmp: matrix-core is not under release — comparison skipped"
else
  japicmp_old="${BOM_VERIFY_JAPICMP_OLD:-${all_versions[matrixCoreBaselineVersion]:-}}"
  [[ -n "$japicmp_old" ]] || { echo "matrixCoreBaselineVersion is unset" >&2; exit 1; }
  [[ "$japicmp_old" != *-SNAPSHOT ]] || { echo "japicmp baseline must not be a SNAPSHOT: $japicmp_old" >&2; exit 1; }
  echo "japicmp: comparing matrix-core $japicmp_old -> $japicmp_new"
  sed -e "s/@OLD_VERSION@/$japicmp_old/g" -e "s/@NEW_VERSION@/$japicmp_new/g" \
    japicmp/pom.xml.template > japicmp/pom.xml
  japicmp_rc=0
  if mvn "${MVN_ISOLATED[@]}" -f japicmp/pom.xml verify; then
    :
  else
    japicmp_rc=$?
  fi
  japicmp_report=japicmp/target/japicmp/cmp.xml
  if (( japicmp_rc != 0 )) || [[ ! -f "$japicmp_report" ]]; then
    echo "japicmp failed to run (exit $japicmp_rc) — infrastructure problem, not a compatibility result" >&2
    exit 1
  fi
  if grep -Eq -e 'binaryCompatible="false"|sourceCompatible="false"' "$japicmp_report"; then
    echo "japicmp: COMPATIBILITY CHANGES FOUND in matrix-core — review before releasing:"
    japicmp_diff=japicmp/target/japicmp/cmp.diff
    if [[ -f "$japicmp_diff" ]]; then
      changed_entries=$(
        grep '^[[:space:]]*===\*' "$japicmp_diff" |
          grep -v '===\* UNCHANGED CLASS:' |
          sed -E \
            -e 's/^[[:space:]]*===\* UNCHANGED (METHOD|CONSTRUCTOR|FIELD):/  AFFECTED \1:/' \
            -e 's/^[[:space:]]*===\* /  /' || true
      )
      if [[ -n "$changed_entries" ]]; then
        echo "affected API signatures (class summaries omitted):"
        printf '%s\n' "$changed_entries"
      else
        echo "No concise changed-entry summary was generated."
      fi
    fi
    echo "full reports: $japicmp_report and $japicmp_diff"
  else
    echo "japicmp: matrix-core $japicmp_old -> $japicmp_new is binary and source compatible"
  fi
fi

mvn "${MVN_ISOLATED[@]}" dependency:copy-dependencies \
  -DincludeGroupIds=se.alipsa.matrix -DincludeScope=test -DoutputDirectory=target/api-jars
mvn "${MVN_ISOLATED[@]}" dependency:copy \
  -Dartifact=org.jacoco:org.jacoco.cli:0.8.14:jar:nodeps \
  -DoutputDirectory=target -Dmdep.stripVersion=true
java -jar target/org.jacoco.cli-nodeps.jar report target/jacoco-it.exec \
  --classfiles target/api-jars \
  --html target/site/jacoco-bom-api --xml target/jacoco-bom-api.xml

mkdir -p target/jacoco-per-module
shopt -s nullglob
api_jars=(target/api-jars/*.jar)
(( ${#api_jars[@]} > 0 )) || { echo "no Matrix jars were copied for coverage" >&2; exit 1; }
for jar in "${api_jars[@]}"; do
  module=$(basename "$jar" .jar)
  module=${module%-[0-9]*}
  java -jar target/org.jacoco.cli-nodeps.jar report target/jacoco-it.exec \
    --classfiles "$jar" --name "$module" \
    --xml "target/jacoco-per-module/$module.xml" >/dev/null
done

echo "JaCoCo per-module totals (BOM API integration tests):"
printf '%-24s %35s %35s\n' "Module" "Instructions" "Branches"
printf '%-24s %35s %35s\n' "" "covered / missed / total / coverage" "covered / missed / total / coverage"

format_counter() {
  local report=$1
  local type=$2
  local counter missed covered total tenths
  counter=$(grep -o "<counter type=\"$type\"[^>]*/>" "$report" | tail -1 || true)
  if [[ -z "$counter" ]]; then
    printf '%s' 'n/a'
    return
  fi
  missed=$(sed -nE 's/.*missed="([0-9]+)".*/\1/p' <<< "$counter")
  covered=$(sed -nE 's/.*covered="([0-9]+)".*/\1/p' <<< "$counter")
  total=$((missed + covered))
  if (( total == 0 )); then
    printf '%s' "$covered / $missed / $total (n/a)"
  else
    tenths=$(( (covered * 1000 + total / 2) / total ))
    printf '%s / %s / %s (%d.%d%%)' \
      "$covered" "$missed" "$total" "$((tenths / 10))" "$((tenths % 10))"
  fi
}

for report in target/jacoco-per-module/*.xml; do
  module=$(basename "$report" .xml)
  instruction=$(format_counter "$report" INSTRUCTION)
  branch=$(format_counter "$report" BRANCH)
  printf '%-24s %35s %35s\n' "$module" "$instruction" "$branch"
done
