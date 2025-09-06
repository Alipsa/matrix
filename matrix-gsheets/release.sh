#!/usr/bin/env bash
set -e
source ~/.sdkman/bin/sdkman-init.sh
source jdk21
#./gradlew clean publishToSonatype closeAndReleaseSonatypeStagingRepository
./gradlew -PrunExternalTests=true :matrix-gsheets:clean :matrix-gsheets:build :matrix-gsheets:release || exit 1
PROJECT=$(basename "$PWD")
if grep "version '" build.gradle | grep -q 'SNAPSHOT'; then
  echo "$PROJECT snapshot published"
else
  echo "$PROJECT published"
  echo "see https://central.sonatype.com/publishing/deployments for more info"
  # browse https://oss.sonatype.org &
fi