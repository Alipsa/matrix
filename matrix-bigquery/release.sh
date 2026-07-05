#!/usr/bin/env bash
if [[ -f ~/.sdkman/bin/sdkman-init.sh ]]; then
  source ~/.sdkman/bin/sdkman-init.sh
fi

if (command -v jdk21 >/dev/null 2>&1); then
  source jdk21
fi

javaVersion=$(java -version 2>&1 | head -1 | cut -d '"' -f2 | sed '/^1\./s///' | cut -d '.' -f1)

if [ ! "$javaVersion" = 21 ]; then
  echo "java must be 21 to release"
  exit 1
fi

if (command -v gclogin >/dev/null 2>&1); then
  gclogin
  export RUN_EXTERNAL_TESTS=true
else
  echo "gclogin not found, skipping external tests"
fi

./gradlew :matrix-bigquery:clean :matrix-bigquery:build :matrix-bigquery:release || exit 1
PROJECT=$(basename "$PWD")
if grep "version '" build.gradle | grep -q 'SNAPSHOT'; then
  echo "$PROJECT snapshot published"
else
  echo "$PROJECT uploaded"
  echo "see https://central.sonatype.com/publishing/deployments for more info"
  # browse https://oss.sonatype.org &
fi
