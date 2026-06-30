#!/usr/bin/env bash
SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
echo "Releasing project in $SCRIPT_DIR"
cd "$SCRIPT_DIR" || exit 1

# shellcheck disable=SC1090
source ~/.sdkman/bin/sdkman-init.sh
source jdk21

PROJECT=$(basename "$PWD")

../gradlew :"$PROJECT":clean :"$PROJECT":build :"$PROJECT":release || exit 1

if grep "version '" build.gradle | grep -q 'SNAPSHOT'; then
  echo "$PROJECT snapshot published"
else
  echo "$PROJECT uploaded and released"
  echo "see https://central.sonatype.com/publishing/deployments for more info"
  # browse https://oss.sonatype.org &
fi