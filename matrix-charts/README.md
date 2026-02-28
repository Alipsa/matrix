[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-charts/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-charts)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-charts/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-charts)
# Charts
Groovy library for creating graphs based on Matrix or [][] data

Matrix-charts is a "native" chart library that creates charts as SVGs.
An SVG chart can be exported to Swing, JavaFX, Image, PNG, or JPG using
the exporters in the se.alipsa.matrix.chartexport package.

There are 2 APIs in matrix-charts, sharing the same Charm rendering engine:
1. **[Charm](docs/charm.md)** The core chart library based on the principles of Grammar of Graphics.
   Idiomatic Groovy closure DSL with typed specifications and immutable compiled charts.
2. **[Charts](docs/charts.md)** The `se.alipsa.matrix.pict` package contains charts in a "familiar style"
    (begin with the chart type, e.g. `AreaChart`, then add data and styling).
    Backed by Charm internally.

> For ggplot2-style API, see **[matrix-ggplot](../matrix-ggplot/README.md)** — a compatibility
> layer mimicking the ggplot2 API in R. It depends on matrix-charts and delegates to Charm under the hood.

> Note: the [matrix-xchart](../matrix-xchart/readme.md) module exists as an alternative charting module
> making it easy to use the xcharts library with the rest of the matrix ecosystem.

To use matrix-charts, add the following dependencies to your gradle build script
```groovy
implementation(platform( 'se.alipsa.matrix:matrix-bom:2.4.1'))
implementation 'se.alipsa.matrix-core'
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

## Charts Example

```groovy
import java.time.LocalDate
import se.alipsa.matrix.core.*
import se.alipsa.matrix.pict.*
import se.alipsa.matrix.chartexport.ChartToPng

def empData = Matrix.builder().data(
    emp_id: 1..5,
    emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
    salary: [623.3, 515.2, 611.0, 729.0, 843.25],
    start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"))
    .types(int, String, Number, LocalDate)
    .build()

def areaChart = AreaChart.builder(empData)
    .title("Salaries").x("emp_name").y("salary").build()

def barChart = BarChart.builder(empData)
    .title("Salaries").x("emp_name").y("salary").vertical().build()

def pieChart = PieChart.builder(empData)
    .title("Salaries").x("emp_name").y("salary").build()

// Export to PNG via chartexport
ChartToPng.export(areaChart, new File("areaChart.png"))
ChartToPng.export(barChart, new File("barChart.png"))
```

See **[charts.md](docs/charts.md)** for comprehensive documentation.

# Release version compatibility matrix
See the [Matrix BOM](https://mvnrepository.com/artifact/se.alipsa.matrix/matrix-bom) for the recommended matrix library versions.
