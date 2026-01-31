#!/usr/bin/env bash
set -e
source ~/.sdkman/bin/sdkman-init.sh
source jdk21
./gradlew :matrix-spreadsheet:clean :matrix-spreadsheet:build
echo "Build was successful, publishing to maven central..."
./gradlew :matrix-spreadsheet:release
PROJECT=$(basename "$PWD")
if grep "version '" build.gradle | grep -q 'SNAPSHOT'; then
  echo "$PROJECT snapshot published"
else
  echo "$PROJECT published"
  echo "see https://central.sonatype.com/publishing/deployments for more info"
  # browse https://oss.sonatype.org &
fi