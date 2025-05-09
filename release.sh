#!/usr/bin/env bash
set -e
source ~/.sdkman/bin/sdkman-init.sh
source jdk21

function release() {
   pushd "$1"
   ./release.sh
   popd
}
#release matrix-core
#release matrix-datasets
release matrix-stats
release matrix-sql
release matrix-spreadsheet
release matrix-json
release matrix-csv
release matrix-parquet
release matrix-bigquery
release matrix-charts
release matrix-xchart
release matrix-tablesaw
pushd matrix-bom
    mvn -DstagingProcessTimeoutMinutes=10 -Prelease -f bom.xml clean deploy
popd
echo "done"
