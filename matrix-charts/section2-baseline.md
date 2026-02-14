# Section 2 Baseline and Inventory (Charm Overhaul)

This document records execution artifacts for Section 2 in `matrix-charts/charm-plan.md`.

## 2.1 API Inventory

Public API inventory snapshots are generated from source for:

- `se.alipsa.matrix.gg`
- `se.alipsa.matrix.charts`
- `se.alipsa.matrix.chartexport`

Inventory files:

- `matrix-charts/section2-inventory/gg-public-api.txt`
- `matrix-charts/section2-inventory/charts-public-api.txt`
- `matrix-charts/section2-inventory/chartexport-public-api.txt`

Current source-file counts:

- `gg`: 200 files
- `charts`: 44 files
- `chartexport`: 6 files

## 2.2 Export Pipeline Duality (Current State)

Current call graph baseline:

- `charts` pipeline:
  - `Plot.jfx/png/base64`
  - `-> JfxConverter / PngConverter`
  - `-> JavaFX charts or JavaFX snapshot path`
- `gg` pipeline:
  - `GgChart.render()`
  - `-> Svg (gsvg)`
  - `-> ChartToPng / ChartToJpeg / ChartToJfx / ChartToSwing / ChartToImage`

## 2.3 JavaFX PNG Constraint Baseline

`charts.png.PngConverter` currently depends on:

- JavaFX toolkit initialization (`new JFXPanel()`)
- JavaFX application thread scheduling (`Platform.runLater`)
- scene snapshot rendering (`Scene.snapshot`)

This is recorded as a migration risk and a baseline behavior to replace with SVG-first export.

## 2.4 Test Coverage Asymmetry Baseline

Current test file counts:

- `gg` tests: 100 files (`matrix-charts/src/test/groovy/gg`)
- `charts` tests: 5 files (`matrix-charts/src/test/groovy/chart`)
- `export` tests: 5 files (`matrix-charts/src/test/groovy/export`)

Risk statement: `charts` baseline coverage is materially lower than `gg`.

## 2.5 Missing Charts Baseline Tests Added

Added:

- `matrix-charts/src/test/groovy/chart/ChartFactoryBaselineTest.groovy`

New baseline coverage from this test:

- Area chart factory behavior + invalid input handling
- Bar chart factory direction/stacking behavior
- Pie chart factory behavior
- Scatter chart factory behavior
- Bubble chart stub behavior (`Not yet implemented`)
- Chart style/legend/title setter baseline behavior
- Histogram invalid input behavior
- `Chart.validateSeries` invalid input behavior

## 2.6 Baseline Fixture Matrix (Representative)

Existing `gg` fixture coverage references:

- Point: `matrix-charts/src/test/groovy/gg/GgPlotTest.groovy`
- Line: `matrix-charts/src/test/groovy/gg/geom/GeomLineTest.groovy`
- Bar/Col: `matrix-charts/src/test/groovy/gg/geom/GeomBarColTest.groovy`
- Histogram: `matrix-charts/src/test/groovy/gg/geom/GeomHistogramTest.groovy`
- Boxplot: `matrix-charts/src/test/groovy/gg/geom/GeomBoxplotTest.groovy`
- Facets: `matrix-charts/src/test/groovy/gg/FacetTest.groovy`
- Transforms: `matrix-charts/src/test/groovy/gg/TransformationsTest.groovy`
- Themes: `matrix-charts/src/test/groovy/gg/theme/ThemeVariantsTest.groovy`
- Guides: `matrix-charts/src/test/groovy/gg/guide/GuideColorStepsTest.groovy`

`charts` baseline fixture coverage references:

- `matrix-charts/src/test/groovy/chart/BoxChartTest.groovy`
- `matrix-charts/src/test/groovy/chart/HistogramTest.groovy`
- `matrix-charts/src/test/groovy/chart/LineChartTest.groovy`
- `matrix-charts/src/test/groovy/chart/ChartFactoryBaselineTest.groovy`
- `matrix-charts/src/test/groovy/chart/svg/SvgBarChartTest.groovy`

## 2.7 Compatibility Tiers

- Tier A: no observable behavior change (`gg` public API)
- Tier B: safe output differences (layout/style refinement)
- Tier C: documented breaking changes (only in `charts` internals or deprecated backends)

## 2.8 Command Log

Executed commands and outcomes:

- `./gradlew :matrix-charts:test --tests "chart.*" -Pheadless=true`
  - Result: SUCCESS
  - Summary: 17 tests, 17 passed, 0 failed, 0 skipped
- `./gradlew :matrix-charts:test --tests "gg.*" -Pheadless=true`
  - Result: SUCCESS
  - Summary: 1654 tests, 1654 passed, 0 failed, 0 skipped
- `./gradlew :matrix-charts:test -Pheadless=true`
  - Result: SUCCESS
  - Summary: 1697 tests, 1697 passed, 0 failed, 0 skipped
