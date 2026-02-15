# Matrix Charts Charm Overhaul Plan

## 1. Goals and Constraints
1.1 [ ] Keep `se.alipsa.matrix.gg` public API stable for end users while moving rendering and chart composition internals to `se.alipsa.matrix.charm`.
1.2 [ ] Design `se.alipsa.matrix.charm` as an idiomatic Groovy Grammar-of-Graphics API (Groovy naming, inheritance/composition, closures/builders where useful).
1.3 [ ] Rebuild `se.alipsa.matrix.charts` (Area/Box/Scatter/Line/etc.) as a familiar high-level API backed by `charm`.
1.4 [ ] Standardize rendering on SVG generation in `charm`; export to PNG/JPEG/JavaFX/Swing via `se.alipsa.matrix.chartexport`.
1.5 [ ] Keep code DRY by removing duplicated logic between `gg`, `charts`, and backend converters.
1.6 [ ] Resolve current architecture duality as a first-class objective:
  pipeline A = `charts` -> `Plot` -> `JfxConverter`/`PngConverter` (JavaFX-native chart path),
  pipeline B = `gg` -> `GgChart.render()` -> `Svg` -> `chartexport`.
1.7 [ ] Eliminate JavaFX toolkit requirement for PNG export in the `charts` path by converging on `Svg` -> `chartexport` export flow.
1.8 [ ] Add and maintain architecture diagrams (current state and target state) as a reference artifact for migration decisions.
1.9 [ ] Treat `matrix-charts/charm-specification.md` as the v1 source-of-truth for Charm DSL/object-model semantics; plan tasks and implementation must conform or explicitly document deviations.

Current architecture (baseline):
```text
gg ------renders------> Svg (gsvg)
gg ------exports------> chartexport -----> PNG/JPEG/JFX

charts --converts-----> charts.jfx (JavaFX charts)
charts --converts-----> charts.swing (xchart)
charts --converts-----> charts.png (via JavaFX snapshot)
charts --renders------> charts.svg (hand-rolled, incomplete)
```

Target architecture:
```text
charm ---renders-----> Svg (gsvg)
gg ------adapts------> charm ---renders-> Svg
charts --builds------> charm ---renders-> Svg

Svg ----exports-----> chartexport -------> PNG/JPEG/JFX/Swing
```

## 2. Baseline and Inventory
2.1 [x] Create an API inventory for all public classes/methods in:
  `matrix-charts/src/main/groovy/se/alipsa/matrix/gg`,
  `matrix-charts/src/main/groovy/se/alipsa/matrix/charts`,
  `matrix-charts/src/main/groovy/se/alipsa/matrix/chartexport`.
  Inventory artifacts:
  `matrix-charts/section2-inventory/gg-public-api.txt`,
  `matrix-charts/section2-inventory/charts-public-api.txt`,
  `matrix-charts/section2-inventory/chartexport-public-api.txt`.
2.2 [x] Inventory current export pipeline duality and call graph explicitly:
  `charts` path: `Plot.jfx/png/base64` -> `JfxConverter`/`PngConverter` (JavaFX toolkit/snapshot dependent),
  `gg` path: `GgChart.render()` -> `se.alipsa.groovy.svg.Svg` -> `ChartToPng/Jfx/Jpeg/Swing/Image`.
2.3 [x] Document JavaFX PNG constraint as baseline risk:
  `charts.png.PngConverter` depends on `Platform.runLater` + `Scene.snapshot`, requiring JavaFX toolkit initialization and UI-thread orchestration.
2.4 [x] Document current test-coverage asymmetry as a migration risk:
  Baseline counts recorded in `matrix-charts/section2-baseline.md`:
  `gg`: 100 test files, `charts`: 5 test files, `export`: 5 test files.
2.5 [x] Create missing baseline tests for `se.alipsa.matrix.charts` before fixture capture:
  add coverage for chart families and behavior currently untested (Area, Bar, Bubble, Pie, Scatter, styling/defaults, legend/title behavior, invalid input handling).
  Added:
  `matrix-charts/src/test/groovy/chart/ChartFactoryBaselineTest.groovy`.
2.6 [x] Capture current behavior with baseline fixtures for representative chart types (point, line, bar, histogram, boxplot, facets, transforms, themes, guides), using both existing `gg` tests and the newly-added `charts` baselines.
  Fixture mapping and baseline notes recorded in:
  `matrix-charts/section2-baseline.md`.
2.7 [x] Define explicit compatibility tiers:
  Tier A: no observable behavior change (`gg` public API),
  Tier B: safe output differences (layout/style refinement),
  Tier C: documented breaking changes (only in `charts` internals or deprecated backends).
2.8 [x] Add this section’s command log once executed:
  `./gradlew :matrix-charts:test --tests "chart.*" -Pheadless=true`
  `./gradlew :matrix-charts:test --tests "gg.*" -Pheadless=true`
  `./gradlew :matrix-charts:test -Pheadless=true`
  Outcomes recorded in:
  `matrix-charts/section2-baseline.md`.

## 3. Charm API Specification (Design First)
3.1 [x] Finalize Charm naming strategy and import-conflict policy:
  prefer concise names in `se.alipsa.matrix.charm` (`Chart`, `Layer`, `Geom`, `Scale`, `Coord`, `Facet`, `Theme`, `Guide`, `Aes`, `Stat`, `Position`) and reserve suffixed names (`*Spec`, `*Model`) for internal types where needed.
  Implemented in:
  `matrix-charts/section3-api-design.md`.
3.2 [x] Document import ambiguity handling for coexistence with `se.alipsa.matrix.charts.Chart` and other common type names:
  use import aliasing (`import se.alipsa.matrix.charm.Chart as CharmChart`), static DSL entrypoints (`import static se.alipsa.matrix.charm.Charts.chart`), and fully-qualified names as fallback.
  Validated by:
  `matrix-charts/src/test/groovy/charm/api/CharmApiDesignTest.groovy`.
3.3 [x] Define explicit render/output contract:
  Charm renders to `se.alipsa.groovy.svg.Svg` objects (gsvg), not SVG strings; string conversion (if needed) happens via `svg.toXml()`/`SvgWriter`/`chartexport`.
3.4 [x] Define idiomatic Groovy API conventions:
  camelCase names, map/closure configuration, typed builders, inheritance/composition-friendly design, no R-style underscore factory naming in Charm.
3.5 [x] Compare and map Charm package layout to existing `gg` structure; ensure GoG concept parity:
  include equivalents for `aes`, `stat`, `position`, and `sf` in addition to `coord`, `facet`, `geom`, `layer`, `render`, `scale`, `theme`, `util`.
3.6 [x] Specify a stable public package layout under `se.alipsa.matrix.charm` (for example: `aes`, `coord`, `facet`, `geom`, `layer`, `position`, `render`, `scale`, `sf`, `stat`, `theme`, `dsl`, `util`).
3.7 [x] Define conversion contract from gg specs to charm specs (`gg` adapter layer), including aes/stat/position/scale/theme mapping.
3.8 [x] Define a minimal Charm MVP scope (must-have for first cutover): point, line, bar/col, histogram, boxplot, smooth, facets, major scales, theme basics, legend.
3.9 [x] Add explicit Charm API usage examples for design review using the closure DSL as primary syntax, plus gg-style façade examples as secondary syntax.

Draft API sketches to validate ergonomics:
```groovy
import static se.alipsa.matrix.charm.Charts.plot

def spec = plot(mpg) {
  aes {
    x = col.cty
    y = col.hwy
    color = col['class']
  }
  points {
    size = 2
    alpha = 0.7
  }
  smooth {
    method = 'lm'
  }
  labels {
    title = 'City vs Highway MPG'
  }
}

def chart = spec.build()
def svg = chart.render()  // returns se.alipsa.groovy.svg.Svg
```

```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.gg.GgChart
import se.alipsa.groovy.svg.Svg

GgChart gg = ggplot(mpg, aes(x: 'cty', y: 'hwy', colour: 'class')) +
    geom_point(size: 2, alpha: 0.7) +
    geom_smooth(method: 'lm') +
    labs(title: 'City vs Highway MPG')

Svg svg = gg.render()  // same underlying PlotSpec/engine
```

3.10 [x] Add API design tests in `matrix-charts/src/test/groovy/charm/api` that compile and execute the example snippets.
  Added:
  `matrix-charts/src/test/groovy/charm/api/CharmApiDesignTest.groovy`.
3.11 [x] Adopt the spec decision that Charm core uses its own typed `AesSpec`/`ColumnExpr` model; `gg.aes.Aes` is adapted via the gg façade/adapter layer (not reused as Charm core type).
3.12 [x] Adopt thread-safety default for v1: per-chart explicit state and immutable compiled charts, with no global mutable theme in Charm core.
  Optional thread-local convenience layer may be added later if needed, without changing core semantics.
3.13 [x] Define Charm error-handling strategy:
  use domain-specific exceptions (e.g. `CharmException` hierarchy) and consistent diagnostic messages for mapping/render/validation failures.
3.14 [x] Review and formally approve `charm-specification.md` sections 4-10 as locked v1 decisions (grammar, `col` semantics, object model, lifecycle, static requirements, non-goals).
  Any planned deviation must be documented explicitly in this plan before implementation.
3.15 [x] Define ggplot2-style façade requirement from spec section 10:
  it must build the same underlying `PlotSpec`, support `+` composition for familiarity, and must not alter Charm DSL grammar rules.
  Outcomes and references for 3.1-3.15:
  `matrix-charts/section3-api-design.md`.

## 4. Implement Charm Core Domain Model
4.1 [x] Create `matrix-charts/src/main/groovy/se/alipsa/matrix/charm` source files for immutable/mostly-immutable core model objects.
4.2 [x] Implement inheritance/composition hierarchy to reduce duplication:
  shared base types for geoms/scales/coords/themes instead of repeated ad-hoc logic.
4.3 [x] Explicitly address `gg` scale hierarchy debt while designing Charm:
  avoid one-class-per-variation duplication and replace patterns like `ScaleXLog10`, `ScaleXSqrt`, `ScaleXReverse` with parameterized transform-based scale types where feasible (e.g., axis + transform strategy).
4.4 [x] Define reusable transformation strategy interfaces/classes (log10/sqrt/reverse/time/date/custom) to keep scale logic DRY across x/y variants.
4.5 [x] Verify and review deleted Charm prototype from commit `115bd930` and classify each removed type as:
  reuse with refactor, partial reuse, or discard.
  First step: run `git show --name-only --oneline 115bd930` and confirm the file list before assessment.
  Files to assess:
  `ChartBuilder.groovy`, `CoordinateSystem.groovy`, `Graph.groovy`, `GridLines.groovy`, `Legend.groovy`, `Style.groovy`, `SubTitle.groovy`, `Text.groovy`, `Title.groovy`, `ChartBuilderTest.groovy`.
4.6 [x] Separately review recently removed `se.alipsa.matrix.charts.charmfx` files (`CharmChartFx`, `ChartPane`, `LegendPane`, `PlotPane`, `TitlePane`, `Position`, horizontal/vertical legend panes) as a distinct prototype iteration and classify reuse/discard rationale.
4.7 [x] Document prototype assessment outcomes in this plan (or companion design doc), including rationale for what is intentionally not carried forward.
4.8 [x] Move reusable utility logic from `gg` and `charts` into shared Charm utilities where appropriate.
4.9 [x] Introduce strict validation and error messages for invalid mappings, missing required aesthetics, and unsupported combos.
4.10 [x] Add unit tests for model and validation behavior in `matrix-charts/src/test/groovy/charm/core`.
4.11 [x] Implement thread-safety model from Section 3:
  enforce per-chart explicit state and immutable compiled charts in core; do not introduce global mutable theme state.
4.12 [x] Implement Charm exception hierarchy and wire through validation/renderer/adapter boundaries with actionable error context.
4.13 [x] Implement typed spec model classes from Section 3 (`PlotSpec`, `AesSpec`, `LayerSpec`, `ScaleSpec`, `ThemeSpec`, `FacetSpec`, `LabelsSpec`, `AnnotationSpec`, `ColumnRef`/`ColumnExpr`) with builder-friendly APIs.
4.14 [x] Implement `col` namespace mechanism for static compilation:
  add typed `Cols` proxy with `getAt(String)` for static-safe `col['name']` and `getProperty(String)` for `col.name` in dynamic DSL closures.
4.15 [x] Implement `ColumnExpr` resolution/coercion rules from spec:
  accept `ColumnExpr` and `CharSequence` in aes setters/maps, coerce deterministically at specification time, and reject unsupported types with clear errors.
4.16 [x] Define and implement DSL-to-model mapping rules:
  `points {}` / `line {}` / `tile {}` / `smooth {}` -> `LayerSpec`,
  `labels {}` -> `LabelsSpec`,
  `annotate { text/rect/segment }` -> `AnnotationSpec` hierarchy.
  Include explicit `smooth {}` mapping semantics (geom/stat/params).
4.17 [x] Implement compile/build boundary:
  `PlotSpec.build()` returns immutable compiled chart model and freezes defaults/mappings deterministically.
4.18 [x] Implement schema-aware data contract validation where metadata exists:
  validate unknown columns and invalid mappings with deterministic, user-friendly diagnostics during build (or documented render-time fallback).
4.19 [x] Add conformance tests for spec semantics:
  closure `=` rule, named-arg `:` rule, `col.*` behavior, aes inheritance (`inheritAes`), mutable->immutable lifecycle guarantees.
4.20 [x] Add this section’s command log once executed:
  `git show --name-only --oneline 115bd930`
  `git status --short`
  `./gradlew :matrix-charts:test --tests "charm.core.*" -Pheadless=true`
  Outcomes and rationale for 4.1-4.20:
  `matrix-charts/section4-core-model.md`.

## 5. Implement Charm Rendering Pipeline (SVG First)
5.1 [x] Build `charm` renderer pipeline to output `se.alipsa.groovy.svg.Svg`.
5.2 [x] Perform a class-by-class migration assessment of `se.alipsa.matrix.gg.render`:
  `GgRenderer`, `AxisRenderer`, `GridRenderer`, `FacetRenderer`, `LegendRenderer`, `RenderContext`.
  For each class, label logic as:
  portable as-is, portable with refactor, or redesign required.
5.3 [x] Extract and port portable rendering primitives first:
  axis tick/label generation, grid line generation, legend item rendering, facet panel layout, clip-path setup, and theme style application.
5.4 [x] Redesign non-portable renderer concerns currently coupled to `gg` internals:
  hardcoded margin constants in `GgRenderer`,
  tight dependence on `GgChart`/`Layer` types in renderer signatures,
  mutable global-style rendering state in `RenderContext`.
5.5 [x] Introduce Charm render configuration objects (margins, panel spacing, legend placement, label paddings) to replace hardcoded constants.
5.6 [x] Introduce Charm-native render context abstractions independent of `gg` domain types; keep `gg` coupling only in adapter layer.
5.7 [x] Evaluate historical `charts.svg.SvgBarChart` (`matrix-charts/src/main/groovy/se/alipsa/matrix/charts/svg/SvgBarChart.groovy`) as reference and cautionary example:
  useful ideas: explicit axis/tick/label/bar drawing flow.
  anti-patterns to avoid in Charm: hardcoded colors/styles, `println` debugging, incomplete legend positioning, chart-type-specific rendering logic without shared GoG abstractions.
5.8 [x] Ensure data flow is explicit and testable:
  data + mappings -> stat -> position -> scale -> coord -> geom -> theme -> svg.
5.9 [x] Add deterministic rendering tests with direct SVG object assertions for core chart types.
5.10 [x] Add fixture comparison tests against selected existing gg outputs (structure/element counts/critical attributes, not fragile full-string equality).
5.11 [x] Ensure renderer consumes immutable compiled chart model (not mutable `PlotSpec`) and keeps rendering side-effect free.
5.12 [x] Add this section’s command log once executed:
  `./gradlew :matrix-charts:compileGroovy`
  `./gradlew :matrix-charts:test --tests "charm.render.*" -Pheadless=true`
  `./gradlew :matrix-charts:test -Pheadless=true`
  Outcomes and rationale for 5.1-5.12:
  `matrix-charts/section5-rendering.md`.

## 6. Add GG -> Charm Adapter Layer (Keep GG API Stable)
6.1 [ ] Place adapter implementation in `se.alipsa.matrix.gg.adapter` (or internal `gg` package scope), not in `charm`, to preserve dependency direction:
  `gg` depends on `charm`; `charm` has zero knowledge of `gg`.
6.2 [ ] Define adapter architecture for `gg` scale/surface size (currently 200 source files under `se/alipsa/matrix/gg`, many geom/scale/stat/coord variants):
  use registry/strategy mapping plus parameterized Charm model types instead of one adapter class per gg type.
6.3 [ ] Define canonical parameterized Charm targets for mapping:
  geoms -> small core set + options,
  scales -> axis + transform/palette strategy,
  stats -> stat type + parameter map,
  coords -> coord type + parameters.
6.4 [ ] Build and maintain a gg-to-charm mapping matrix covering geoms/scales/stats/coords/positions/themes:
  each gg type must map to either a supported Charm representation or a documented compatibility fallback/error.
6.5 [ ] Change `GgChart.render()` and related render entrypoints to delegate to Charm renderer while preserving method signatures.
6.6 [ ] Keep existing static factories in `GgPlot.groovy` unchanged for users; route implementation through adapter.
6.7 [ ] Modernize touched `gg` switch statements to Groovy 5 arrow syntax during adapter refactoring, including:
  `parseStatType()` and `parsePositionType()` in `matrix-charts/src/main/groovy/se/alipsa/matrix/gg/GgChart.groovy`.
6.8 [ ] Remove/merge duplicated rendering and transformation logic from `gg` once delegated (DRY cleanup).
6.9 [ ] Validate parity with existing `gg` test suite and targeted regression fixtures.
6.10 [ ] Ensure gg adapter/façade generates the same underlying Charm `PlotSpec`/compiled model as native Charm API for equivalent semantics.
6.11 [ ] Add this section’s command log once executed:
  `./gradlew :matrix-charts:test --tests "gg.GgPlotTest" -Pheadless=true`
  `./gradlew :matrix-charts:test --tests "gg.*" -Pheadless=true`

## 7. Rebuild Charts API on Top of Charm
7.1 [ ] Audit `se.alipsa.matrix.charts` types for implementation status and usage before refactor:
  note that `BubbleChart` is currently a stub (`create(...)` throws `Not yet implemented`, ~271 bytes).
7.2 [ ] Decide Bubble chart strategy explicitly:
  implement as Charm-backed chart now, or deprecate/drop until fully supported.
  Record decision and migration impact in this plan.
7.3 [ ] Refactor implemented `charts` chart types (`AreaChart`, `BarChart`, `BoxChart`, `Histogram`, `LineChart`, `PieChart`, `ScatterChart`) to produce Charm specs.
7.4 [ ] Preserve the “chart type first” user flow in `charts` API:
  choose chart type -> add data -> labels/legend -> style/customization.
7.5 [ ] Keep `Plot` as a deprecated compatibility shim (documented), preserving method signatures where feasible:
  `Plot.jfx(chart)`, `Plot.png(chart, file, ...)`, `Plot.base64(chart, ...)`.
7.6 [ ] Rewire `Plot` implementation to Charm/SVG-first export path:
  `Plot.png(...)` -> chart renders to `Svg` -> `ChartToPng`,
  `Plot.jfx(...)` -> chart renders to `Svg` -> `ChartToJfx`,
  `Plot.base64(...)` -> `Svg` -> image bytes/base64 via shared chartexport path.
  No JavaFX snapshot dependency in the new `Plot.png(...)` flow.
7.7 [ ] Keep convenient defaults and expose explicit customization points that map cleanly to charm theme/scale/guide options.
7.8 [ ] Evaluate `se.alipsa.matrix.charts.util` (`ColorUtil`, `DateUtil`, `StyleUtil`) for extraction/migration:
  move shared utilities to `charm.util` (or a shared neutral package) and update both `gg` and `charts` call sites to avoid duplication.
7.9 [ ] Update or replace tests under `matrix-charts/src/test/groovy/chart` to validate charm-backed outputs and compatibility shim behavior.
7.10 [ ] Add/adjust focused compatibility tests for `Plot` methods (png/jfx/base64) to confirm preserved behavior with new implementation path.
7.11 [ ] Add this section’s command log once executed:
  `./gradlew :matrix-charts:test --tests "chart.*" -Pheadless=true`
  `./gradlew :matrix-charts:test --tests "PngTest" -Pheadless=true`

## 8. Consolidate Export Path Through chartexport
8.1 [ ] Standardize export inputs in `se.alipsa.matrix.chartexport` to accept `Svg` and Charm chart types directly.
8.2 [ ] Add Charm overloads to export entrypoints (`ChartToPng`, `ChartToJpeg`, `ChartToJfx`, `ChartToSwing`, `ChartToImage`) that delegate through Charm render -> `Svg`.
8.3 [ ] Keep `GgChart` overloads as compatibility shims initially, internally delegating to shared SVG export path.
8.4 [ ] Remove backend-specific chart generation dependencies from `charts` (direct jfx/swing/png converters) where replaced by chartexport.
8.5 [ ] Explicitly retire JavaFX snapshot dependency for `charts` PNG output:
  converged path should be `chart` -> Charm render (`Svg`) -> `ChartToPng`, without JavaFX toolkit initialization.
8.6 [ ] Refactor/update:
  `ChartToPng.groovy`, `ChartToJpeg.groovy`, `ChartToJfx.groovy`, `ChartToSwing.groovy`, `ChartToImage.groovy`.
8.7 [ ] Add/refresh tests in `matrix-charts/src/test/groovy/export` for all supported export targets via SVG path.
8.8 [ ] Add this section’s command log once executed:
  `./gradlew :matrix-charts:test --tests "PngTest" -Pheadless=true`
  `./gradlew :matrix-charts:test --tests "export.*" -Pheadless=true`

## 9. Deprecation and Cleanup of Legacy Backends
9.1 [ ] Mark legacy backend-specific implementation packages for deprecation/removal:
  `se.alipsa.matrix.charts.jfx`,
  `se.alipsa.matrix.charts.swing`,
  `se.alipsa.matrix.charts.png`,
  `se.alipsa.matrix.charts.svg` (legacy wrappers only),
  `se.alipsa.matrix.charts.charmfx` (prototype UI classes).
9.1.1 [ ] Document `se.alipsa.matrix.charts.charmfx` removal as an intentional breaking change for the next release,
  including impacted classes (`CharmChartFx`, `ChartPane`, `LegendPane`, `PlotPane`, `TitlePane`, `Position`, `HorizontalLegendPane`, `VerticalLegendPane`)
  and recommended migration path (Charm core + `chartexport`).
9.2 [ ] Deprecate (do not abruptly remove) `se.alipsa.matrix.charts.Plot` as a compatibility facade:
  keep behavior routed to Charm/SVG + `chartexport` per Section 7, document replacement APIs, and define removal timeline.
9.3 [ ] Remove dead code and duplicated conversion utilities after parity is confirmed.
9.4 [ ] Dependency cleanup matrix: explicitly remove dependencies no longer needed after backend convergence:
  remove `org.knowm.xchart:xchart` once `charts.swing` converters/`SwingPlot` are retired.
9.5 [ ] Dependency cleanup matrix: audit and remove residual JFreeChart references if present (imports, comments, transitive assumptions):
  verify no active `org.jfree.*` usage remains.
9.6 [ ] Dependency cleanup matrix: narrow JavaFX dependencies to what remains required for SVG-based export path:
  drop JavaFX chart-converter-specific requirements (`javafx.scene.chart` path),
  retain only modules needed by remaining JavaFX integrations (for example `fxsvgimage` / `ChartToJfx` and related tests).
9.7 [ ] Update `matrix-charts/build.gradle` dependency scopes (`api` vs `compileOnly` vs `testImplementation`) to reflect final architecture and minimize published surface.
9.8 [ ] Confirm no public API regressions in `gg` and intentional/documented changes in `charts`.
9.9 [ ] Add this section’s command log once executed:
  `rg -n "org\\.knowm\\.xchart|org\\.jfree" matrix-charts/src/main matrix-charts/src/test matrix-charts/build.gradle`
  `./gradlew :matrix-charts:dependencies --configuration runtimeClasspath`
  `./gradlew :matrix-charts:test -Pheadless=true`

## 10. Documentation, Examples, and Migration Guides
10.1 [ ] Add `charm` user documentation (new `matrix-charts/charm.md` or docs page) with idiomatic Groovy examples.
10.2 [ ] Update `matrix-charts/README.md` positioning:
  `charm` = core grammar API,
  `charts` = quick familiar chart API,
  `gg` = ggplot2-compatible migration API.
10.3 [ ] Update `matrix-charts/ggPlot.md` to clarify compatibility promise and charm-backed implementation.
10.4 [ ] Add/refresh examples in `matrix-charts/examples/charm`, `matrix-charts/examples/gg`, and `matrix-charts/examples/charts` (if added).
10.5 [ ] Add migration notes for:
  old `charts` backend behavior,
  direct converter usage,
  recommended future usage patterns,
  explicit note that `se.alipsa.matrix.charts.charmfx` was removed as a breaking change in this release line.
10.6 [ ] Add Charm DSL documentation extracted from `charm-specification.md`:
  closure assignment rule (`=`), named-argument rule (`:`), `col.*` canonical mappings, and lifecycle (`spec -> build -> render`).

## 11. Quality Gates and Release Readiness
11.1 [ ] Add regression gate requiring unchanged pass for core `gg` tests before merge.
11.2 [ ] Add targeted visual regression checks (golden SVG snapshots or structural assertions) for critical chart families.
11.3 [ ] Run full module test suite and record commands here when passing:
  `./gradlew :matrix-charts:test -Pheadless=true`
  `./gradlew test`
11.4 [ ] Prepare release notes summarizing architecture shift and migration guidance.
11.4.1 [ ] Ensure release notes contain a dedicated "Breaking Changes" section that explicitly lists removal of `se.alipsa.matrix.charts.charmfx`.
11.5 [ ] Only mark tasks complete (`[x]`) when corresponding tests have passed and commands are recorded in this file or PR description.

## 12. Suggested Delivery Milestones
12.1 [ ] Milestone 0 (Architecture Spike): implement one chart type end-to-end (recommended: bar chart) through both
  primary Charm closure DSL (`plot(data) { ... }`) and gg adapter/façade path, render both to `Svg`, and add comparison tests asserting equivalent output semantics.
  Include chartexport verification (`Svg` -> PNG/JPEG/JFX/Swing where applicable) and a short findings report on architectural fit/risk.
12.2 [ ] Milestone A: design and validate Charm API ergonomics with concrete usage examples and API tests before scaling renderer work; then complete Charm MVP + initial renderer + adapter coverage for `ggplot + geom_point/line/bar`.
12.3 [ ] Milestone B: Full gg delegation to charm with parity for existing `gg` tests.
12.4 [ ] Milestone C: charts API fully charm-backed + export path consolidated in `chartexport`.
12.5 [ ] Milestone D: legacy backend cleanup, docs finalization, release candidate.
