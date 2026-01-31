# Matrix-charts Release History

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