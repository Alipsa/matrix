#!/usr/bin/env bash
set -e pipefail

source ~/.sdkman/bin/sdkman-init.sh
source jdk21
./gradlew :matrix-avro:clean :matrix-avro:build :matrix-avro:release
PROJECT=$(basename "$PWD")
VERSION=$(sed -nE "s/^[[:space:]]*version[[:space:]]*=[[:space:]]*['\"](.*)['\"][[:space:]]*$/\\1/p" build.gradle)
if [ -z "$VERSION" ]; then
  echo "Failed to determine version from build.gradle" >&2
  exit 1
fi
if [[ "$VERSION" == *SNAPSHOT* ]]; then
  echo "$PROJECT snapshot published"
else
  echo "$PROJECT $VERSION uploaded, release it if it checks out"
  echo "see https://central.sonatype.com/publishing/deployments for more info"
fi
