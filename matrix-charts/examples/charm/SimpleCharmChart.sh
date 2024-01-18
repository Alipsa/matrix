#!/usr/bin/env bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd "$SCRIPT_DIR/../.." || exit
. ./gradlew build
cd "$SCRIPT_DIR" || exit

groovy -cp "$SCRIPT_DIR/../../build/libs/matrix-charts-1.0.0-SNAPSHOT.jar" SimpleCharmChart.groovy

