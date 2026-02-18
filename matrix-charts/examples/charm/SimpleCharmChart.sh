#!/usr/bin/env bash

set -e
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd "$SCRIPT_DIR/../.."
echo "Building matrix-charts project..."
./gradlew build -x test
CLASSPATH=$(./gradlew -q printCp)
cd "$SCRIPT_DIR"
# echo "Using CLASSPATH: $CLASSPATH"
echo "Running SimpleCharmChart.groovy..."
groovy -cp "$CLASSPATH" SimpleCharmChart.groovy

