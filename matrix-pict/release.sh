#!/usr/bin/env bash
source ~/.sdkman/bin/sdkman-init.sh
source jdk21

PROJECT=$(basename "$PWD")

../gradlew :$PROJECT:clean :$PROJECT:build :$PROJECT:release || exit 1

if grep "version '" build.gradle | grep -q 'SNAPSHOT'; then
  echo "$PROJECT snapshot published"
else
  echo "$PROJECT uploaded and released"
  echo "see https://central.sonatype.com/publishing/deployments for more info"
  # browse https://oss.sonatype.org &
fi