# Charts
Groovy library for creating graphs based on Matrix or [][] data

add the following dependencies to your gradle build script
```groovy
implementation(platform( 'se.alipsa.matrix:matrix-bom:2.4.0'))
implementation 'se.alipsa.matrix-core'
implementation 'se.alipsa.matrix:matrix-charts'
implementation 'se.alipsa.matrix:matrix-stats'
implementation 'se.alipsa.matrix:matrix-groovy-ext'
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
  <dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>matrix-groovy-ext</artifactId>
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
The library also supports ggplot2-style charting via the GgPlot class. The api is very similar to the R ggplot2 library and very few modifications are needed to port code from R to Groovy. Essentially:
1. Use a Matrix instead of a data.frame
2. Quote column names and constants

Example:
```R
library(ggplot2)

chart <- ggplot(mpg, aes(cty, hwy)) +
  geom_point() +
  coord_fixed()

ggsave("my_plot.svg", plot = chart)
```
The above R code can be translated to Groovy as:

```groovy
@Grab('se.alipsa.matrix:matrix-core:3.6.0')
@Grab('se.alipsa.matrix:matrix-charts:0.4.0')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2')
@Grab('se.alipsa.matrix:matrix-stats:2.2.1')

import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset

def chart = ggplot(Dataset.mpg(), aes('cty', 'hwy')) +
    geom_point() +
    coord_fixed()

write(chart.render(), new File('my_plot.svg'))
```

## GGPlot Phase 1 core enhancements
These ggplot2-style helpers are implemented and covered by tests.

### Essential geoms (1.1)
```groovy
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.core.Matrix

def band = Matrix.builder().data(
    x: [1, 2, 3, 4],
    y: [10, 12, 9, 14],
    ymin: [8, 10, 7, 12],
    ymax: [12, 14, 11, 16]
).types(Integer, Integer, Integer, Integer).build()

ggplot(band, aes('x', 'y')) + geom_ribbon(ymin: 'ymin', ymax: 'ymax')
ggplot(band, aes('x', 'y')) + geom_pointrange(ymin: 'ymin', ymax: 'ymax')
ggplot(band, aes('x', 'y')) + geom_linerange(ymin: 'ymin', ymax: 'ymax')
ggplot(band, aes('x', 'y')) + geom_crossbar(ymin: 'ymin', ymax: 'ymax')

def grid = Matrix.builder().data(
    x: [1, 2, 3],
    y: [1, 2, 3],
    value: [5, 10, 15],
    xmin: [0.5, 1.5, 2.5],
    xmax: [1.5, 2.5, 3.5],
    ymin: [0.5, 1.5, 2.5],
    ymax: [1.5, 2.5, 3.5]
).types(Integer, Integer, Integer, Double, Double, Double, Double).build()

ggplot(grid, aes('x', 'y', fill: 'value')) + geom_tile()
ggplot(grid, aes()) + geom_rect(xmin: 'xmin', xmax: 'xmax', ymin: 'ymin', ymax: 'ymax')

def ordered = Matrix.builder().data(
    x: [1, 2, 3, 4],
    y: [2, 5, 3, 6]
).types(Integer, Integer).build()

ggplot(ordered, aes('x', 'y')) + geom_path()
ggplot(ordered, aes('x', 'y')) + geom_step(direction: 'vh')
```

### Transform scales (1.2)
```groovy
ggplot(band, aes('x', 'y')) + geom_point() + scale_x_log10()
ggplot(band, aes('x', 'y')) + geom_point() + scale_y_log10()
ggplot(band, aes('x', 'y')) + geom_point() + scale_x_sqrt()
ggplot(band, aes('x', 'y')) + geom_point() + scale_y_sqrt()
ggplot(band, aes('x', 'y')) + geom_point() + scale_x_reverse()
ggplot(band, aes('x', 'y')) + geom_point() + scale_y_reverse()
```

### Date/time scales (1.3)
```groovy
import java.time.LocalDate
import java.time.LocalDateTime

def dates = Matrix.builder().data(
    date: [LocalDate.parse('2024-01-01'), LocalDate.parse('2024-02-01')],
    value: [10, 12]
).types(LocalDate, Integer).build()

ggplot(dates, aes('date', 'value')) + geom_line() + scale_x_date()
ggplot(dates, aes('value', 'date')) + geom_point() + scale_y_date()

def datetimes = Matrix.builder().data(
    ts: [LocalDateTime.parse('2024-01-01T10:00:00'), LocalDateTime.parse('2024-01-01T12:00:00')],
    value: [5, 7]
).types(LocalDateTime, Integer).build()

ggplot(datetimes, aes('ts', 'value')) + geom_line() + scale_x_datetime()
ggplot(datetimes, aes('value', 'ts')) + geom_point() + scale_y_datetime()
```

### Annotations (1.4)
```groovy
ggplot(band, aes('x', 'y')) +
    geom_point() +
    annotate('text', x: 2, y: 12, label: 'Peak') +
    annotate('rect', xmin: 1, xmax: 3, ymin: 9, ymax: 13, alpha: 0.15) +
    annotate('segment', x: 1, xend: 3, y: 10, yend: 10, color: 'red')
```

### Guide system (1.5)
```groovy
ggplot(grid, aes('x', 'y', fill: 'value')) +
    geom_tile() +
    guides(fill: guide_colorbar())

ggplot(grid, aes('x', 'y', fill: 'value')) +
    geom_tile() +
    guides(fill: guide_legend())

ggplot(grid, aes('x', 'y', fill: 'value')) +
    geom_tile() +
    guides(fill: 'none')
```

### Limit helpers (1.6)
```groovy
ggplot(band, aes('x', 'y')) +
    geom_line() +
    lims(x: [1, 4], y: [8, 16])

ggplot(band, aes('x', 'y')) +
    geom_line() +
    expand_limits(x: [0, 5], y: 0)
```

# Release version compatibility matrix
See the [Matrix BOM](https://mvnrepository.com/artifact/se.alipsa.matrix/matrix-bom) for the recommended matrix library versions. 
