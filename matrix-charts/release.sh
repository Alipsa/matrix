#!/usr/bin/env bash
source ~/.sdkman/bin/sdkman-init.sh
source jdk21
#./gradlew clean publishToSonatype closeAndReleaseSonatypeStagingRepository
../gradlew :matrix-charts:clean :matrix-charts:build :matrix-charts:release || exit 1
PROJECT=$(basename "$PWD")
if grep "version '" build.gradle | grep -q 'SNAPSHOT'; then
  echo "$PROJECT snapshot published"
else
  echo "$PROJECT uploaded and released"
  echo "see https://central.sonatype.org/publish/release/ for more info"
  # browse https://oss.sonatype.org &
fi