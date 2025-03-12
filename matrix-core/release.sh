#!/usr/bin/env bash
set -e
source ~/.sdkman/bin/sdkman-init.sh
source jdk21
./gradlew :matrix-core:clean :matrix-core:build :matrix-core:release
PROJECT=$(basename "$PWD")
if grep "version '" build.gradle | grep -q 'SNAPSHOT'; then
  echo "$PROJECT snapshot published"
else
  echo "$PROJECT uploaded and released"
  echo "see https://central.sonatype.org/publish/release/ for more info"
  #browse https://oss.sonatype.org &
fi