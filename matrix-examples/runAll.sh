#!/usr/bin/env bash
# Runs the example workflows for every matrix-examples Gradle subproject.
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository_root="$(cd "${script_dir}/.." && pwd)"

cd "${repository_root}"

exec ./gradlew --rerun-tasks -Pheadless=true \
  :matrix-examples:AnalysisScenario1:test \
  :matrix-examples:DecisionTree:test \
  :matrix-examples:HousePrices:test \
  :matrix-examples:LinearRegression:test \
  :matrix-examples:XChartDemo:test \
  :matrix-examples:candles:test \
  :matrix-examples:whiskey:test
