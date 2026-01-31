# Matrix XChart release history

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