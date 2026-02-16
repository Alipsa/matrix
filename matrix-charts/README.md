[![Maven Central](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-charts/badge.svg)](https://maven-badges.sml.io/maven-central/se.alipsa.matrix/matrix-charts)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-charts/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-charts)
# Charts
Groovy library for creating graphs based on Matrix or [][] data

Matrix-charts is a "native" chart library that creates charts as SVGs. 
An SVG chart can be exported to Swing, JavaFX, Image, PNG, or JPG using 
the exporters in the se.alipsa.matrix.chartexport package. 

There are 3 APIs in matrix-charts:
1. **Charm** This is the core chart library based on the principles of Grammar Of Graphics.
2. **Charts** The se.alipsa.matrix.chart package contains charts in a "familiar style" for those used to
    chart libraries such as xcharts and JavaFX charts (i.e. you begin with the chart type (e.g. AreaChart), 
    then add data and styling).
3. **[gg](ggPlot.md)** This is a compatibility layer mimicking the ggplot2 API in R making migrations from
    R applications easy.

> Note: the [matrix-xchart](../matrix-xchart/readme.md) module exists as an alternative charting module 
> making it easy to use the xcharts library with the rest of the matrix ecosystem.

To use matrix-charts, add the following dependencies to your gradle build script
```groovy
implementation(platform( 'se.alipsa.matrix:matrix-bom:2.4.0'))
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
      <version>2.4.0</version>
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


## Example usage

```groovy
import java.time.LocalDate
import se.alipsa.matrix.core.*
import se.alipsa.matrix.charts.*

def empData = Matrix.builder().data(
    emp_id: 1..5,
    emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
    salary: [623.3,515.2,611.0,729.0,843.25],
    start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"))
    .types(int, String, Number, LocalDate)
    .build()

def areaChart = AreaChart.create("Salaries", empData, "emp_name", "salary")
def barChart = BarChart.createVertical("Salaries", empData, "emp_name", ChartType.NONE, "salary")
def pieChart = PieChart.create("Salaries", empData, "emp_name", "salary")

// Use the Plot class to output the chart, e.g:
Plot.png(areaChart, new File("areaChart.png"))

Plot.svg(barChart, new File("barChart.svg"))

javafx.scene.chart.Chart jfxPieChart = Plot.jfx(pieChart)
```

# GGPlotting

The library also supports ggplot2-style charting via the GgPlot class. The API closely follows R's ggplot2 library, making it easy to port R code to Groovy with minimal changes.

**Quick Example:**
```groovy
@Grab('se.alipsa.matrix:matrix-core:3.6.0')
@Grab('se.alipsa.matrix:matrix-charts:0.4.0')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2')
@Grab('se.alipsa.matrix:matrix-stats:2.2.1')

import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset

def chart = ggplot(Dataset.mpg(), aes('cty', 'hwy')) +
    geom_point() +
    geom_smooth(method: 'lm') +
    labs(title: 'City vs Highway MPG')

write(chart.render(), new File('my_plot.svg'))
```

**Key Features:**
- 54+ geoms (geometric objects) including point, line, bar, histogram, boxplot, heatmap, and more
- Complete scale system (color, size, shape, transformations)
- Faceting for multi-panel plots
- Full theming support
- Statistical transformations
- Annotations and labels
- Multiple output formats (SVG, PNG, JavaFX)

**Getting Started:**

See **[ggPlot.md](ggPlot.md)** for comprehensive documentation including:
- Complete API reference
- Core concepts (aesthetics, geoms, scales, coordinates, facets, themes)
- Detailed examples
- Differences from R's ggplot2
- Tips and best practices

**Examples:**

Additional examples can be found in `matrix-charts/examples/gg/`

# Release version compatibility matrix
See the [Matrix BOM](https://mvnrepository.com/artifact/se.alipsa.matrix/matrix-bom) for the recommended matrix library versions. 
