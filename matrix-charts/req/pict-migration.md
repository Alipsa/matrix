# matrix-pict extraction plan

> **For agentic workers:** Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Context

Pict is the chart-type-first API (BarChart, LineChart, ScatterChart, etc.) that delegates to the Charm rendering engine via `CharmBridge`. It currently lives inside `matrix-charts`, but its relationship to Charm is structurally identical to `matrix-ggplot`'s: a user-facing facade with its own data model that bridges into Charm's PlotSpec. Keeping pict inside `matrix-charts` conflates the engine with a consumer of the engine, and forces users who only want the simpler pict API to take the full Charm engine as a direct dependency rather than a transitive one.

This plan extracts pict into a new `matrix-pict` module (version 0.5.0-SNAPSHOT) so that the dependency graph mirrors ggplot:

```
matrix-charts  (Charm engine + export utilities)
    ├── matrix-ggplot   (ggplot2-style API → Charm)
    └── matrix-pict     (chart-type API → Charm)   ← new
```

No package renames. The `se.alipsa.matrix.pict` package stays as-is; only the artifact coordinate changes from `matrix-charts` to `matrix-pict`. This is a clean cut — 0.x versions allow breaking changes.

> **Note on phase ordering:** Phases 2 and 3 (source move and test move) are bundled into a single PR. Once pict source leaves `matrix-charts`, any remaining pict test imports in `matrix-charts` fail to compile — matrix-charts cannot depend on matrix-pict. The source and test moves must land together.

---

## Phase 1: Scaffold matrix-pict module

**PR scope:** Create the empty module wired into the build, before any source moves.

- 1.1 [x] Create directory `matrix-pict/src/main/groovy/se/alipsa/matrix/pict/` and `matrix-pict/src/test/groovy/`
- 1.2 [x] Create `matrix-pict/docs/` directory (needed for the `pict.md` move in Phase 4)
- 1.3 [x] Write `matrix-pict/build.gradle` modelled on `matrix-ggplot/build.gradle`:
  - `version = '0.5.0-SNAPSHOT'`
  - `description = "Groovy chart-type-first API for matrix (pict)"`
  - `api project(':matrix-charts')` — transitively provides gsvg, chartexport, Charm, JavaFX compileOnly. Use `api` (same as matrix-ggplot) because `Plot.svg()` returns `Svg` from gsvg; callers need that type visible without adding matrix-charts to their own dependencies.
  - `compileOnly project(':matrix-core')`
  - `compileOnly project(':matrix-stats')`
  - `compileOnly libs.groovy`
  - JavaFX `compileOnly` quartet (graphics, base, controls, swing) — same qualifier logic as matrix-ggplot
  - Standard test dependencies: groovy, groovy-xml, matrix-core, matrix-stats, matrix-datasets, JavaFX, groovier-junit, junit-jupiter, junit-jupiter-engine, junit-platform-launcher
  - `testFast` task identical to matrix-charts/matrix-ggplot
  - Maven publishing block: `name = 'Groovy Matrix Pict'`, SCM url `matrix-pict`
  - Signing block (same guard as other modules)
- 1.4 [x] Add `include 'matrix-pict'` to `settings.gradle` (place it after `matrix-ggplot`)
- 1.5 [x] Copy `testutil/Slow.groovy` to `matrix-pict/src/test/groovy/testutil/Slow.groovy` (it's a local JUnit tag annotation; each module keeps its own copy)

**Verification:**
```bash
./gradlew :matrix-pict:build
```
Expected: empty module compiles and produces an (empty) jar.

---

## Phase 2: Add Charm coverage and clean pict references from matrix-charts tests

**PR scope:** Prepare `matrix-charts` tests for the source move. Add Charm-level equivalents for any export functionality currently tested only through pict, and surgically remove pict references from the two Charm test files that contain them. No files are moved in this phase.

### Charm coverage additions

- 2.1 [x] **Add `testCharmChartToBase64` to `export/CharmExportTest.groovy`** — `CharmExportTest` has `testCharmChartToImage` but no base64 test. Moving `testBase64ReturnsValidDataUri` (PlotCompatibilityTest) and `testBarchartToBase64` (PngTest) to matrix-pict in Phase 3 would leave no Charm-level `ChartToImage.base64()` coverage in matrix-charts. Add:
  ```groovy
  @Test
  void testCharmChartToBase64() {
      CharmChart chart = buildCharmChart()
      String dataUri = ChartToImage.base64(chart)
      assertNotNull(dataUri)
      assertTrue(dataUri.startsWith('data:image/png;base64,'))
  }
  ```

- 2.2 [x] **Extract `testBarchartToPng` from `PngTest.groovy` into `export/WriteToAndPlotGridExportTest.groovy`** — this test uses `Charts.plot()` (Charm DSL), not pict. Moving all of PngTest in Phase 3 would lose this Charm PNG test from matrix-charts. Rename it `testCharmChartToPngViaExporter` in WriteToAndPlotGridExportTest to avoid clashing with the existing `testCharmChartToPng` in CharmExportTest.

### Surgical edits (remove pict references from Charm test files)

- 2.3 [x] **Edit `charm/api/CharmApiDesignTest.groovy`** (do not move — it tests the Charm DSL):
  - Extract `testImportAliasStrategyCompilesAndRuns` and stage it for addition to `chart/PlotCompatibilityTest.groovy` in matrix-pict (Phase 3, step 3.6). In matrix-pict's test classpath, pict.Chart is present directly and charm.Chart transitively, so the test works without build.gradle changes.
  - Remove the extracted method and the `import se.alipsa.matrix.pict.Chart as LegacyChart` line from CharmApiDesignTest.

- 2.4 [x] **Edit `export/CharmExportTest.groovy`** (do not move — it is a Charm test file):
  - Extract `testChartToSvgPictOutputStream` and `testChartToSvgLegacyWriter` and stage them for addition to `chart/PlotCompatibilityTest.groovy` in matrix-pict (Phase 3, step 3.6).
  - Extract the `Plot.swing(pictChart)` assertion from `testChartToSwingTypedOverloads` into a new `testPlotSwingReturnsSvgPanel` test staged for PlotCompatibilityTest; remove that assertion from CharmExportTest.
  - Remove the now-unused pict imports (`se.alipsa.matrix.pict.Chart`, `se.alipsa.matrix.pict.Plot`, `se.alipsa.matrix.pict.ScatterChart`) and the `buildPictChart()` helper from CharmExportTest.

**Verification:**
```bash
./gradlew :matrix-charts:test -Pheadless=true
```
All matrix-charts tests must pass. Confirm no pict imports remain in CharmApiDesignTest or CharmExportTest:
```bash
grep -r "se.alipsa.matrix.pict" matrix-charts/src/test/groovy/charm/ matrix-charts/src/test/groovy/export/
```
Expected: zero results.

---

## Phase 3: Move pict source and tests to matrix-pict

**PR scope:** Move all 17 production source files and 8 test files in one PR. Source move and test move must be atomic — splitting them would leave matrix-charts with uncompilable pict imports.

### Source files

Move from `matrix-charts/src/main/groovy/se/alipsa/matrix/pict/` to the identical path under `matrix-pict/src/main/groovy/`:

- 3.1 [x] `AreaChart.groovy`
- 3.2 [x] `AxisScale.groovy`
- 3.3 [x] `BarChart.groovy`
- 3.4 [x] `BoxChart.groovy`
- 3.5 [x] `BubbleChart.groovy`
- 3.6 [x] `Chart.groovy`
- 3.7 [x] `CharmBridge.groovy`
- 3.8 [x] `ChartDirection.groovy`
- 3.9 [x] `ChartType.groovy`
- 3.10 [x] `DataType.groovy`
- 3.11 [x] `Histogram.groovy`
- 3.12 [x] `Legend.groovy`
- 3.13 [x] `LineChart.groovy`
- 3.14 [x] `PieChart.groovy`
- 3.15 [x] `Plot.groovy`
- 3.16 [x] `ScatterChart.groovy`
- 3.17 [x] `Style.groovy`

After moving, delete `matrix-charts/src/main/groovy/se/alipsa/matrix/pict/` entirely.

Confirm no pict source remains in matrix-charts:
```bash
grep -r "se.alipsa.matrix.pict" matrix-charts/src/main/
```
Expected: zero results.

### Test files

Move from `matrix-charts/src/test/groovy/` to the identical path under `matrix-pict/src/test/groovy/`:

- 3.18 [x] `chart/BoxChartTest.groovy`
- 3.19 [x] `chart/ChartBuilderTest.groovy`
- 3.20 [x] `chart/ChartFactoryBaselineTest.groovy`
- 3.21 [x] `chart/ChartsCharmIntegrationTest.groovy`
- 3.22 [x] `chart/HistogramTest.groovy`
- 3.23 [x] `chart/LineChartTest.groovy`
- 3.24 [x] `chart/PlotCompatibilityTest.groovy` — add the methods staged in Phase 2 steps 2.3 and 2.4 here before moving
- 3.25 [x] `PngTest.groovy` — `testBarchartToPng` must already be extracted (Phase 2 step 2.2) before this move

After moving, delete the now-empty `chart/` directory from `matrix-charts/src/test/groovy/`.

**Tests that stay in `matrix-charts`:**
- `charm/api/CharmApiDesignTest.groovy` — Charm DSL design tests; has no pict imports after Phase 2
- `export/CharmExportTest.groovy` — Charm export tests; has no pict imports after Phase 2
- `export/WriteToAndPlotGridExportTest.groovy` — no pict imports; gains the Charm PNG test from Phase 2

**Build dependency check:** `matrix-charts/build.gradle` already has `testImplementation project(':matrix-ggplot')`. This remains needed — `CharmApiDesignTest` imports `se.alipsa.matrix.gg.GgChart`. Do not remove it. No `project(':matrix-pict')` test dependency is needed.

**Verification:**
```bash
./gradlew :matrix-pict:compileGroovy
./gradlew :matrix-charts:compileGroovy
./gradlew :matrix-pict:test -Pheadless=true
./gradlew :matrix-charts:test -Pheadless=true
```
All tests in both modules must pass.

---

## Phase 4: Update docs and AGENTS.md

**PR scope:** Move `pict.md` into the new module and update all documentation to reflect the new artifact coordinate. No code changes.

- 4.1 [x] Move `matrix-charts/docs/pict.md` to `matrix-pict/docs/pict.md`
- 4.2 [x] Update `docs/cookbook/matrix-charts.md` — replace any pict import/dependency examples that reference `matrix-charts` with `matrix-pict`
- 4.3 [x] Update `docs/tutorial/13-matrix-charts.md` — same coordinate change for pict usage
- 4.4 [x] Update `matrix-charts/README.md` — remove pict API docs/references; note that pict is now in `matrix-pict`
- 4.5 [x] Update `AGENTS.md` module table — add `matrix-pict` row: `"chart-type-first API (BarChart, LineChart, etc.), delegates to Charm in matrix-charts"`; update `matrix-charts` description to remove the pict reference

**Verification:** Manual review of changed doc files. No build step required.

---

## Phase 5: Update matrix-bom

**PR scope:** Register `matrix-pict` in the BOM so consumers can manage its version centrally.

> **Note:** The published BOM definition is `matrix-bom/bom.xml`. The sibling `matrix-bom/pom.xml` is the BOM integration-test project. Edit `matrix-bom/bom.xml` directly.

- 5.1 [x] Add `<matrixPictVersion>0.5.0-SNAPSHOT</matrixPictVersion>` to the `bom.xml` `<properties>` block immediately after `<matrixGgplotVersion>`. Verify that `<matrixChartsVersion>` in `bom.xml` matches the version in `matrix-charts/build.gradle` before committing — they must stay in sync.
- 5.2 [x] Add a `<dependency>` entry for `matrix-pict` in `bom.xml`'s `<dependencyManagement>/<dependencies>` block, immediately after the `matrix-ggplot` entry:

```xml
<dependency>
  <groupId>se.alipsa.matrix</groupId>
  <artifactId>matrix-pict</artifactId>
  <version>${matrixPictVersion}</version>
</dependency>
```

**Verification:**
```bash
mvn -f matrix-bom/bom.xml validate
```
`bom.xml` must parse without errors.

---

## Final regression check

Run the full test suite to confirm no cross-module regressions:

```bash
./gradlew test -Pheadless=true
```
