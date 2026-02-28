# Matrix-charts Release History

## v0.5.0 (unreleased)

This release unifies all charting APIs under the Charm rendering engine and removes legacy backends.
See [charm.md](docs/charm.md) for the Charm user guide and [ggPlot.md](ggPlot.md) for the gg API guide.

**Architecture**
  - Introduced `se.alipsa.matrix.charm` as the single rendering engine for all three charting APIs (Charm, gg, charts).
  - All chart rendering now produces SVG via gsvg. Export to PNG/JPEG/JavaFX/Swing goes through `se.alipsa.matrix.chartexport`.
  - The gg API (`se.alipsa.matrix.gg`) is preserved as a thin compatibility wrapper delegating to Charm.
  - The pictura API (`se.alipsa.matrix.pict`) is backed by Charm via `CharmBridge`. `Plot` is `@Deprecated` but functional.
  - `Plot.png()` no longer requires JavaFX toolkit initialization.

**Breaking Changes**
  - **`se.alipsa.matrix.charts.charmfx` removed.** Deleted classes: `CharmChartFx`, `ChartPane`, `LegendPane`, `PlotPane`, `TitlePane`, `Position`, `HorizontalLegendPane`, `VerticalLegendPane`. Use Charm core + `chartexport` instead.
  - **`Plot.jfx()` return type changed** from `javafx.scene.chart.Chart` to `javafx.scene.Node`. Code using `view()` or similar methods that accept `Node` is unaffected.
  - **Legacy backend packages removed:** `se.alipsa.matrix.charts.jfx`, `se.alipsa.matrix.charts.swing`, `se.alipsa.matrix.charts.png`, `se.alipsa.matrix.charts.svg`, and `se.alipsa.matrix.charts.util.StyleUtil`. Use `chartexport` classes (`ChartToPng`, `ChartToJfx`, `ChartToSwing`, `ChartToJpeg`, `ChartToImage`) instead.
  - **`org.knowm.xchart:xchart` dependency removed.** If you depended on xchart transitively, add it directly.

**New Features**
  - Charm closure DSL for idiomatic Groovy chart specifications (`Charts.plot(data) { ... }`).
  - Immutable compiled chart models (`Chart`) with deterministic lifecycle (spec -> build -> render).
  - Typed column references via `col` proxy (`col.name`, `col['name']`).
  - `@CompileStatic` support throughout Charm core.
  - Charm export overloads added to all five chartexport classes.
  - `verifyGgRegression` Gradle task for pre-merge gg test regression gating.

**Documentation**
  - Added [charm.md](docs/charm.md) with comprehensive Charm DSL guide and migration notes.
  - Updated [README.md](README.md) to position Charm as the core API.
  - Updated [ggPlot.md](docs/ggPlot.md) with charm-backed implementation note.
  - Refreshed `examples/charm/SimpleCharmChart.groovy` to use the new Charm DSL.

## v0.4.0, 2026-01-31

This release introduces a ggplot2-style charting module. See [ggPlot.md](ggPlot.md) for more details.

**API and Architecture Improvements**
  - Refactored the ChartBuilder API to use setLegend, setGridLines, setTitle, setSubTitle, setCoordinateSystem, and a new setStyle method instead of multiple add* methods, making the API more consistent and extensible (ChartBuilder.groovy).
  - Introduced a new Style class and made Title and SubTitle extend a new abstract Text class, improving the internal structure and potential for future styling features (Style.groovy, Title.groovy, SubTitle.groovy, Text.groovy).
  - GGplot class is the entrace point for the ggplot2 inspired chart api.
  - Annotated the classes with @CompileStatic for performance and type safety.

**Dependency and Version Updates**
  - Updated the matrix-charts version to 0.4.0-SNAPSHOT in both build.gradle and the BOM, signaling a breaking or significant feature release (build.gradle, bom.xml). [1] [2]
  - Upgraded the gsvg dependency from 0.2.0 to 1.0.0 and changed its scope from implementation to api to ensure consumers get the correct version transitively (build.gradle).
  - Commented out the jfreechart dependency, indicating a shift in chart rendering strategy (build.gradle).

**Examples and Usage**
  - Added a new Groovy example script demonstrating how to create a scatter plot with a regression line using the updated API and dependencies (scatterWithRegressionLine.groovy).

## v0.3.1, 2025-07-19
- Upgrade dependencies
  - org.jfree:jfreechart [1.5.5 -> 1.5.6]

Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-charts/0.3.1/matrix-charts-0.3.1.jar)

## v0.3.0, 2025-04-01
- Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-charts/0.3.0/matrix-charts-0.3.0.jar)
- 
- enable fluent interaction

## v0.2, 2025-03-12
- require JDK 21

## v0.1, 2025-02-16
- initial release