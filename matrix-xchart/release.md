# Matrix XChart release history

## v0.2.1, 2025-05-26
- add display method to MatrixXChart to display the chart in a Swing frame
- rename getXchart() to getXChart() and add it to MatrixXChart
- Add CorrelationHeatmapChart
- Allow stick chart weight and height to be omitted when creating a chart
- add javadocs to all charts and add chart descriptions to the readme

## v0.2.0, 2025-04-01
Jar available at [maven central](https://repo1.maven.org/maven2/se/alipsa/matrix/matrix-xchart/0.2.0/matrix-xchart-0.2.0.jar)
- 
- add simple create method to ScatterChart to create a scatter chart with a single series.
- add MatrixXChart interface as the top level chart interface
- change setxLabel and setyLabel to setXLabel and setYLabel and return the chart instead of void

## v0.1, 2025-03-12
- Initial release