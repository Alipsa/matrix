#!/usr/bin/env bash
set -euo pipefail

source ~/.sdkman/bin/sdkman-init.sh
source jdk21
./gradlew :matrix-avro:clean :matrix-avro:build :matrix-avro:release
PROJECT=$(basename "$PWD")
VERSION=$(sed -n "s/^version = '\\(.*\\)'$/\\1/p" build.gradle)
if grep "version '" build.gradle | grep -q 'SNAPSHOT'; then
  echo "$PROJECT snapshot published"
else
  echo "$PROJECT $VERSION uploaded, release it if it checks out"
  echo "see https://central.sonatype.com/publishing/deployments for more info"
fi
