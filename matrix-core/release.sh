#!/usr/bin/env bash
set -eo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
CENTRAL_REPO_URL="${CENTRAL_REPO_URL:-https://repo.maven.apache.org/maven2}"
CENTRAL_VERIFY_ATTEMPTS="${CENTRAL_VERIFY_ATTEMPTS:-30}"
CENTRAL_VERIFY_DELAY_SECONDS="${CENTRAL_VERIFY_DELAY_SECONDS:-10}"

if [[ ! "$CENTRAL_VERIFY_ATTEMPTS" =~ ^[1-9][0-9]*$ ]]; then
  echo "CENTRAL_VERIFY_ATTEMPTS must be a positive integer" >&2
  exit 1
fi
if [[ ! "$CENTRAL_VERIFY_DELAY_SECONDS" =~ ^[0-9]+$ ]]; then
  echo "CENTRAL_VERIFY_DELAY_SECONDS must be a non-negative integer" >&2
  exit 1
fi
if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required to verify the release on Maven Central" >&2
  exit 1
fi

cd "$SCRIPT_DIR"
source ~/.sdkman/bin/sdkman-init.sh
source jdk21
set -u
./gradlew :matrix-core:clean :matrix-core:build :matrix-core:release

PROJECT=matrix-core
RELEASE_VERSION=$(sed -nE "s/^[[:space:]]*version[[:space:]]*=[[:space:]]*'([^']+)'[[:space:]]*$/\1/p" build.gradle | head -n 1)
if [[ -z "$RELEASE_VERSION" ]]; then
  echo "Could not determine the matrix-core version from $SCRIPT_DIR/build.gradle" >&2
  exit 1
fi

if [[ "$RELEASE_VERSION" == *-SNAPSHOT ]]; then
  echo "$PROJECT snapshot published"
  exit 0
fi

if [[ ! "$RELEASE_VERSION" =~ ^[0-9A-Za-z][0-9A-Za-z.-]*$ ]]; then
  echo "Unsupported release version: $RELEASE_VERSION" >&2
  exit 1
fi

ARTIFACT_BASE="$CENTRAL_REPO_URL/se/alipsa/matrix/matrix-core/$RELEASE_VERSION/matrix-core-$RELEASE_VERSION"
POM_URL="$ARTIFACT_BASE.pom"
JAR_URL="$ARTIFACT_BASE.jar"

echo "$PROJECT release task completed for $RELEASE_VERSION"
echo "Waiting for the POM and JAR to become available from Maven Central..."
for ((attempt = 1; attempt <= CENTRAL_VERIFY_ATTEMPTS; attempt++)); do
  pom_available=false
  jar_available=false
  if curl --fail --silent --show-error --location --head --retry 3 --retry-all-errors \
      --connect-timeout 10 --max-time 30 "$POM_URL" >/dev/null; then
    pom_available=true
  fi
  if curl --fail --silent --show-error --location --head --retry 3 --retry-all-errors \
      --connect-timeout 10 --max-time 30 "$JAR_URL" >/dev/null; then
    jar_available=true
  fi
  if [[ "$pom_available" == true && "$jar_available" == true ]]; then
    break
  fi
  if (( attempt < CENTRAL_VERIFY_ATTEMPTS )); then
    echo "Central does not have both release files yet (attempt $attempt/$CENTRAL_VERIFY_ATTEMPTS); retrying in ${CENTRAL_VERIFY_DELAY_SECONDS}s"
    sleep "$CENTRAL_VERIFY_DELAY_SECONDS"
  fi
done

if [[ "$pom_available" != true || "$jar_available" != true ]]; then
  echo "Release $PROJECT:$RELEASE_VERSION was not verified on Maven Central; the BOM baseline was not changed." >&2
  echo "POM: $POM_URL" >&2
  echo "JAR: $JAR_URL" >&2
  exit 1
fi

echo "Verified $PROJECT:$RELEASE_VERSION on Maven Central (POM and JAR)."
echo "The BOM matrixCoreBaselineVersion remains unchanged for its pending compatibility comparison."
