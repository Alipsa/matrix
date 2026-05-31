[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-charts/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-charts)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-charts/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-charts)
# Charts
Groovy library for creating graphs based on Matrix or [][] data

Matrix-charts is a "native" chart library that creates charts as SVGs. The Charm
rendering engine and export utilities live here. The `se.alipsa.matrix.chartexport`
package provides Swing, JavaFX, Image, PNG, JPG, and PDF conversion for SVG and
Charm charts.

> **PICT has moved:** The chart-type-first API (`AreaChart`, `BarChart`, etc.) previously
> in this module has been extracted to **[matrix-pict](../matrix-pict/README.md)**.
> It depends on `matrix-charts` and delegates to Charm under the hood.

> For ggplot2-style API, see **[matrix-ggplot](../matrix-ggplot/README.md)** — a compatibility
> layer mimicking the ggplot2 API in R. It depends on matrix-charts and delegates to Charm under the hood.

> Note: the [matrix-xchart](../matrix-xchart/readme.md) module exists as an alternative charting module
> making it easy to use the xcharts library with the rest of the matrix ecosystem.

To use matrix-charts, add the following dependencies to your gradle build script
```groovy
implementation(platform( 'se.alipsa.matrix:matrix-bom:2.5.0'))
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-charts'
implementation 'se.alipsa.matrix:matrix-stats'
```

... or maven pom.xml
```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-bom</artifactId>
      <version>2.4.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```
```xml
<dependencies>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-charts</artifactId>
  </dependency>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-core</artifactId>
  </dependency>
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-stats</artifactId>
  </dependency>
</dependencies>
```


## Charm Example (Recommended)

```groovy
import static se.alipsa.matrix.charm.Charts.plot
import se.alipsa.matrix.datasets.Dataset

def chart = plot(Dataset.mpg()) {
  mapping {
    x = 'cty'
    y = 'hwy'
    color = 'class'
  }
  layers {
    geomPoint().size(2)
    geomSmooth().method('lm')
  }
  labels { title = 'City vs Highway MPG' }
}.build()

chart.writeTo('mpg_chart.svg')
```

See **[charm.md](docs/charm.md)** for comprehensive documentation.

### Charm Export Examples

```groovy
import se.alipsa.matrix.chartexport.ChartToPdf

chart.writeTo('mpg_chart.svg')
chart.writeTo('mpg_chart.png')
ChartToPdf.export(chart, new File('mpg_chart.pdf'))

// Raw SVG XML can also be exported directly to PDF.
String svgXml = chart.render().toXml()
ChartToPdf.export(svgXml, new File('mpg_chart_from_svg.pdf'))
```

Plot grids support explicit output dimensions for every writable format:

```groovy
import static se.alipsa.matrix.charm.Charts.plotGrid

def grid = plotGrid([chart, chart], 2)
grid.writeTo('mpg_dashboard.png', 1200, 700)
grid.writeTo('mpg_dashboard.pdf', 1200, 700)
```

Segment and curve layers can render SVG arrow markers:

```groovy
import se.alipsa.matrix.charm.ArrowSpec

def arrowChart = plot(Dataset.mtcars()) {
  mapping { x = 'mpg'; y = 'wt'; xend = 'hp'; yend = 'qsec' }
  layers {
    geomSegment().arrow(ArrowSpec.end(8, 6)).color('#336699')
  }
}.build()
```

# Release version compatibility matrix
See the [Matrix BOM](https://mvnrepository.com/artifact/se.alipsa.matrix/matrix-bom) for the recommended matrix library versions.
