# Matrix-charts Release History

## v0.4.0, in progress
This release finalised the ggplot2-style charting module with improved API, architecture, and documentation.

**API and Architecture Improvements**
  - Refactored the ChartBuilder API to use setLegend, setGridLines, setTitle, setSubTitle, setCoordinateSystem, and a new setStyle method instead of multiple add* methods, making the API more consistent and extensible (ChartBuilder.groovy).
  - Introduced a new Style class and made Title and SubTitle extend a new abstract Text class, improving the internal structure and potential for future styling features (Style.groovy, Title.groovy, SubTitle.groovy, Text.groovy). 
  - Enhanced the GgChart class with explicit fields for data, aesthetics, layers, coordinates, scales, theme, facet, labels, and dimensions. Added detailed documentation and improved the plus operator overloads for composability. Added a render() method to generate SVG output using a renderer (GgChart.groovy).
  - Annotated the Label class with @CompileStatic for performance and type safety (Label.groovy).

**Dependency and Version Updates**
  - Updated the matrix-charts version to 0.4.0-SNAPSHOT in both build.gradle and the BOM, signaling a breaking or significant feature release (build.gradle, bom.xml). [1] [2]
  - Upgraded the gsvg dependency from 0.2.0 to 0.3.0 and changed its scope from implementation to api to ensure consumers get the correct version transitively (build.gradle).
  - Commented out the jfreechart dependency, indicating a shift in chart rendering strategy (build.gradle).

**Documentation Enhancements**
  - Added a comprehensive CLAUDE.md file that describes the project structure, build/test commands, module purposes, architecture, JDK constraints, code style, and key dependencies, greatly improving onboarding and contributor experience (CLAUDE.md).
  - Removed the outdated and redundant TODO.md file, likely replaced by the new documentation and issue tracking (TODO.md). 
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