#!/usr/bin/env bash
source ~/.sdkman/bin/sdkman-init.sh
source jdk21
#./gradlew clean publishToSonatype closeAndReleaseSonatypeStagingRepository
../gradlew :matrix-bigquery:clean :matrix-bigquery:build :matrix-bigquery:release || exit 1
PROJECT=$(basename "$PWD")
if grep "version '" build.gradle | grep -q 'SNAPSHOT'; then
  echo "$PROJECT snapshot published"
else
  echo "$PROJECT uploaded, release it if it checks out"
  echo "see https://central.sonatype.org/publish/release/ for more info"
  # browse https://oss.sonatype.org &
fi