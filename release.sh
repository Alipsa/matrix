#!/usr/bin/env bash
set -e
source ~/.sdkman/bin/sdkman-init.sh
source jdk 21

# Enable external tests such a BigQuery and Gsheets
export RUN_EXTERNAL_TESTS=true
export RUN_SLOW_TESTS=true

function release() {
   pushd "$1"
   version=$(./gradlew -q printVersion)
   if [[ $version == *-SNAPSHOT ]]; then
      echo "Module $(basename "$PWD") version is a snapshot ($version), skipping..."
   else
      ./release.sh
   fi
   popd
}
./gradlew spotlessApply
if [ -n "$(git status --porcelain)" ]; then
  echo "Error: Git working tree is not clean. Commit or stash changes before publishing."
  exit 1
fi
release matrix-groovy-ext
release matrix-logging
release matrix-core
release matrix-stats
release matrix-datasets
release matrix-sql
release matrix-spreadsheet
release matrix-gsheets
release matrix-json
release matrix-csv
release matrix-arff
release matrix-avro
release matrix-json
release matrix-parquet
release matrix-bigquery
release matrix-smile
release matrix-charts
release matrix-pict
release matrix-ggplot
release matrix-xchart
release matrix-tablesaw
release matrix-jupyter
pushd matrix-bom
    mvn -DstagingProcessTimeoutMinutes=10 -Prelease -f bom.xml clean deploy
popd
unset RUN_EXTERNAL_TESTS
echo "done"
