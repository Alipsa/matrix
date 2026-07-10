# Matrix XChart release history

## v0.3.2, 2026-07-10
- Upgrade to xchart 4.0.2 (from 3.8.8)
  - `AbstractChart.getSeries(String)`/`getSeries()` now use xchart's new public `getSeries(String)`/`getSeriesCollection()` instead of the now package-private `getSeriesMap()`
  - `AbstractChart.setXLabel`/`setYLabel`/`getXLabel`/`getYLabel` now only apply to chart types that support axes (xchart moved `X/YAxisTitle` from `Chart` down to the new `AxesChart` subclass); calling them on `PieChart` or `RadarChart` is now a no-op (`getXLabel`/`getYLabel` return `null`) instead of silently storing an unused value
- Fix `HeatmapChart.addSeries(String, List<?> columnLabels, List<?> rowLabels, List<Column>)` passing the column/row labels to xchart in the wrong order, which dropped or mislabeled cells whenever the grid was not square

## v0.3.1, 2026-07-04
- Fix xchart correlation heatmap diagonal
- Handle undefined heatmap correlations
- Use BigDecimal.ONE and ZERO instead of 1G and 0G which must be coerced to BigDecimal.
- Fix CorrelationHeatmapChart auto-series so heatmap cells are labeled with the correct row/column pair for 3+ columns (previously off-diagonal cells showed the correlation of a different, mirrored column pair)

## v0.3.0, 2026-05-25
- fix histogram default bin calculation so Scott's rule is treated as a bin width and converted to a bucket count from the data range
- add clear validation for invalid histogram input and invalid heatmap shapes
- breaking: vector heatmap input whose value count is not evenly divisible by the requested column count now throws `IllegalArgumentException` instead of dropping trailing values
- heatmap auto-detection now requires the value count to have an integer square root; pass an explicit column count for rectangular heatmaps
- require a named matrix when using `HeatmapChart.addAllToSeriesBy`, avoiding null or fallback XChart series names
- align `OhlcChart.addSeries` with XChart's date-axis OHLC API and validate that x/open/high/low/close lists have equal lengths
- validate correlation heatmap column names and numeric column types before rendering, with clear `IllegalArgumentException` messages
- allow `LineChart.create(matrix)` and `AreaChart.create(matrix)` without explicit width and height, matching the other chart factories
- make chart `display()` safe to call from the Swing event dispatch thread
- enforce CodeNarc for the module after clearing existing main and test violations
- fix default series naming in XY charts to use the Y column name instead of the X column name, consistent with category charts
- fix ScatterChart GroovyDoc typo ("ChatterChart" → "ScatterChart")
- fix PieChart `getXchart()` method that shadowed the parent's `xchart` field access
- fix HeatmapChart local `numberArray` variable shadowing
- fix raw `List` types in CorrelationHeatmapChart and HeatmapChart method signatures
- make `numSeries` fields private in BubbleChart and RadarChart
- add missing `getTitle()` getter to AbstractChart
- add `exportPdf(File)` and `exportPdf(OutputStream)` to all chart types
- add `create(String title, Matrix, ...)` factory methods to all 12 chart types
- add `addAllSeries(String xCol, List<String> yCols)` bulk methods to XY and category charts
- add `addAllSeries()` to BoxChart for adding all numeric columns as series
- add closure-based `createDonut` to PieChart for custom donut styling
- extract `initChart()` helper in AbstractChart, removing ~12 lines of duplicated initialization from 10 chart classes
- HistogramChart now extends AbstractCategoryChart, eliminating duplicated constructor logic
- add GroovyDoc to all previously undocumented public methods

## v0.2.3, 2026-01-31
- add @CompileStatic to all 17 classes for performance and type safety (100% static compilation, no @CompileDynamic needed)
- complete empty test methods in HistogramChartTest (testDensityHistogram, testFrequencyHistogramCustom)
- add comprehensive GroovyDoc to all abstract classes (AbstractChart, AbstractXYChart, AbstractCategoryChart)
- add comprehensive edge case tests for improved test coverage
- replace Math calls with NumberExtension methods for idiomatic Groovy (Math.sqrt() → .sqrt())
- expand GroovyDoc on all public methods with detailed parameter descriptions and examples
- remove commented println debug statements
- fix IndexOutOfBoundsException in AbstractChart.makeFillTransparent() method
- fix multiple calculation issues in chart rendering
- update README version references and complete StickChart description

### Code Quality Achievements
- all chart classes now use @CompileStatic with full static type checking
- no @CompileDynamic fallbacks needed anywhere in codebase
- compile-time type safety across entire module
- maximum performance with no dynamic dispatch overhead

## v0.2.2, 2025-09-06
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-xchart/0.2.2/matrix-xchart-0.2.2.jar)
- upgrade to Groovy 5 for modern language features
- upgrade dependencies
- improve static compilation support

## v0.2.1, 2025-05-26
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-xchart/0.2.1/matrix-xchart-0.2.1.jar)
- add display method to MatrixXChart to display the chart in a Swing frame
- rename getXchart() to getXChart() and add it to MatrixXChart
- Add CorrelationHeatmapChart
- Allow stick chart weight and height to be omitted when creating a chart
- add javadocs to all charts and add chart descriptions to the readme

## v0.2.0, 2025-04-01
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-xchart/0.2.0/matrix-xchart-0.2.0.jar)
- add simple create method to ScatterChart to create a scatter chart with a single series.
- add MatrixXChart interface as the top level chart interface
- change setxLabel and setyLabel to setXLabel and setYLabel and return the chart instead of void

## v0.1, 2025-03-12
- Initial release
