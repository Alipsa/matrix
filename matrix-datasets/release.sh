#!/usr/bin/env bash
source ~/.sdkman/bin/sdkman-init.sh
source jdk21
./gradlew clean build release || exit 1
PROJECT=$(basename "$PWD")
if grep "version '" build.gradle | grep -q 'SNAPSHOT'; then
  echo "$PROJECT snapshot published"
else
  echo "$PROJECT released"
  echo "see https://central.sonatype.com/publishing/deployments for more info"
  # browse https://oss.sonatype.org &
fi