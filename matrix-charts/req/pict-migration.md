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

---

## Phase 1: Scaffold matrix-pict module

**PR scope:** Create the empty module wired into the build, before any source moves.

- 1.1 [ ] Create directory `matrix-pict/src/main/groovy/se/alipsa/matrix/pict/` and `matrix-pict/src/test/groovy/`
- 1.2 [ ] Write `matrix-pict/build.gradle` modelled on `matrix-ggplot/build.gradle`:
  - `version = '0.5.0-SNAPSHOT'`
  - `description = "Groovy chart-type-first API for matrix (pict)"`
  - `api project(':matrix-charts')` — transitively provides gsvg, chartexport, Charm, JavaFX compileOnly. Use `api` (not `implementation` as matrix-ggplot does) because `Plot.svg()` returns `Svg` from gsvg; callers need that type visible without adding matrix-charts to their own dependencies.
  - `compileOnly project(':matrix-core')`
  - `compileOnly project(':matrix-stats')`
  - `compileOnly libs.groovy`
  - JavaFX `compileOnly` quartet (graphics, base, controls, swing) — same qualifier logic as matrix-ggplot
  - Standard test dependencies: groovy, groovy-xml, matrix-core, matrix-stats, matrix-datasets, JavaFX, groovier-junit, junit-jupiter, junit-jupiter-engine, junit-platform-launcher
  - `testFast` task identical to matrix-charts/matrix-ggplot
  - Maven publishing block: `name = 'Groovy Matrix Pict'`, SCM url `matrix-pict`
  - Signing block (same guard as other modules)
- 1.3 [ ] Add `include 'matrix-pict'` to `settings.gradle` (place it after `matrix-ggplot`)
- 1.4 [ ] Copy `testutil/Slow.groovy` to `matrix-pict/src/test/groovy/testutil/Slow.groovy` (it's a local JUnit tag annotation; each module keeps its own copy)
- 1.5 [ ] Create `matrix-pict/docs/` directory (needed for the `pict.md` move in Phase 5.1)

**Verification:**
```bash
./gradlew :matrix-pict:build
```
Expected: empty module compiles and produces an (empty) jar.

---

## Phase 2: Move pict source files

**PR scope:** Move all 17 production source files from `matrix-charts` to `matrix-pict`. No content changes — move only.

Files to move from `matrix-charts/src/main/groovy/se/alipsa/matrix/pict/` to the identical path under `matrix-pict/`:

- 2.1 [ ] `AreaChart.groovy`
- 2.2 [ ] `AxisScale.groovy`
- 2.3 [ ] `BarChart.groovy`
- 2.4 [ ] `BoxChart.groovy`
- 2.5 [ ] `BubbleChart.groovy`
- 2.6 [ ] `Chart.groovy`
- 2.7 [ ] `CharmBridge.groovy`
- 2.8 [ ] `ChartDirection.groovy`
- 2.9 [ ] `ChartType.groovy`
- 2.10 [ ] `DataType.groovy`
- 2.11 [ ] `Histogram.groovy`
- 2.12 [ ] `Legend.groovy`
- 2.13 [ ] `LineChart.groovy`
- 2.14 [ ] `PieChart.groovy`
- 2.15 [ ] `Plot.groovy`
- 2.16 [ ] `ScatterChart.groovy`
- 2.17 [ ] `Style.groovy`

After moving, delete `matrix-charts/src/main/groovy/se/alipsa/matrix/pict/` entirely.

**Verification:**
```bash
./gradlew :matrix-pict:compileGroovy
./gradlew :matrix-charts:compileGroovy
```
Both must compile cleanly with no unresolved references.

After moving, confirm no pict source remains in matrix-charts:
```bash
grep -r "se.alipsa.matrix.pict" matrix-charts/src/main/
```
Expected: zero results.

---

## Phase 3: Move pict tests

**PR scope:** Relocate tests that depend on `se.alipsa.matrix.pict` from `matrix-charts` to `matrix-pict`. Includes one surgical edit to `CharmApiDesignTest.groovy` (see 3.0 below) to remove its sole pict reference before moving the pict test files.

- 3.0 [ ] **Edit `charm/api/CharmApiDesignTest.groovy` in `matrix-charts`** (do not move this file — it tests the Charm DSL and belongs in matrix-charts):
  - Extract the `testImportAliasStrategyCompilesAndRuns` test method into `chart/PlotCompatibilityTest.groovy` in `matrix-pict` (add it there as part of step 3.7). In matrix-pict's test classpath, pict.Chart is present directly and charm.Chart is available transitively, so the test works without any build.gradle changes.
  - Remove the extracted method and the `import se.alipsa.matrix.pict.Chart as LegacyChart` line from `CharmApiDesignTest.groovy`.

Tests to move from `matrix-charts/src/test/groovy/` to the identical path under `matrix-pict/src/test/groovy/`:

- 3.1 [ ] `chart/BoxChartTest.groovy`
- 3.2 [ ] `chart/ChartBuilderTest.groovy`
- 3.3 [ ] `chart/ChartFactoryBaselineTest.groovy`
- 3.4 [ ] `chart/ChartsCharmIntegrationTest.groovy`
- 3.5 [ ] `chart/HistogramTest.groovy`
- 3.6 [ ] `chart/LineChartTest.groovy`
- 3.7 [ ] `chart/PlotCompatibilityTest.groovy` — add the `testImportAliasStrategyCompilesAndRuns` method extracted in step 3.0 here
- 3.8 [ ] `PngTest.groovy`
- 3.9 [ ] `export/CharmExportTest.groovy` — imports `se.alipsa.matrix.pict.Chart` and `se.alipsa.matrix.pict.ScatterChart`; since `matrix-pict` has access to all chartexport utilities transitively, this test moves cleanly

After moving, delete the now-empty `chart/` directory from `matrix-charts/src/test/groovy/`.

**Tests that stay in `matrix-charts`:**
- `charm/api/CharmApiDesignTest.groovy` — Charm DSL design tests; after step 3.0 it has no pict imports
- `export/WriteToAndPlotGridExportTest.groovy` — no pict imports; tests PlotGrid and SVG/PNG export at the chartexport level

**Build dependency check:** `matrix-charts/build.gradle` already has `testImplementation project(':matrix-ggplot')`. This remains needed — `CharmApiDesignTest` imports `se.alipsa.matrix.gg.GgChart`. Do not remove it. No `project(':matrix-pict')` test dependency is needed.

**Verification:**
```bash
./gradlew :matrix-pict:test -Pheadless=true
./gradlew :matrix-charts:test -Pheadless=true
```
All tests in both modules must pass.

---

## Phase 4: Update matrix-bom

**Note:** The published BOM definition is `matrix-bom/bom.xml`. The sibling
`matrix-bom/pom.xml` is the BOM integration-test project. Edit `matrix-bom/bom.xml`
directly.

- 4.1 [ ] Add `<matrixPictVersion>0.5.0-SNAPSHOT</matrixPictVersion>` to the
  `bom.xml` `<properties>` block immediately after `<matrixGgplotVersion>`.
  Verify that the `<matrixChartsVersion>` property in `bom.xml` matches the version
  in `matrix-charts/build.gradle` before committing — they must stay in sync.
- 4.2 [ ] Add a `<dependency>` entry for `matrix-pict` in `bom.xml`'s
  `<dependencyManagement>/<dependencies>` block, immediately after the
  `matrix-ggplot` entry:

```xml
<dependency>
  <groupId>se.alipsa.matrix</groupId>
  <artifactId>matrix-pict</artifactId>
  <version>${matrixPictVersion}</version>
</dependency>
```

This follows the existing `matrix-charts` and `matrix-ggplot` convention: each module
has a dedicated version property in `bom.xml`. Use `0.5.0-SNAPSHOT` consistently for
the initial `matrix-pict` module and BOM property.

**Verification:**
```bash
mvn -f matrix-bom/bom.xml validate
```
`bom.xml` must parse without errors.

---

## Phase 5: Update docs and AGENTS.md

**PR scope:** Update all documentation to reflect the new artifact coordinate.

- 5.1 [ ] Move `matrix-charts/docs/pict.md` to `matrix-pict/docs/pict.md`
- 5.2 [ ] Update `docs/cookbook/matrix-charts.md` — replace any pict import/dependency examples that reference `matrix-charts` with `matrix-pict`
- 5.3 [ ] Update `docs/tutorial/13-matrix-charts.md` — same coordinate change for pict usage
- 5.4 [ ] Update `matrix-charts/README.md` — remove pict API docs/references; note that pict is now in `matrix-pict`
- 5.5 [ ] Update `AGENTS.md` module table — add `matrix-pict` row: `"chart-type-first API (BarChart, LineChart, etc.), delegates to Charm in matrix-charts"`; update `matrix-charts` description to remove the pict reference

**Verification:**
Manual review of changed doc files. No build step required.

---

## Final regression check

Run the full test suite to confirm no cross-module regressions:

```bash
./gradlew test -Pheadless=true
```
