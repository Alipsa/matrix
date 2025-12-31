# Charts
Groovy library for creating graphs based on Matrix or [][] data

add the following dependency to your gradle build script
```groovy
implementation 'se.alipsa.matrix:charts:0.3.1'
```

... or maven pom.xml
```xml
<dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>charts</artifactId>
    <version>0.3.1</version>
</dependency>
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
The library also supports ggplot2-style charting via the GgPlot class. The api is very similar to the R ggplot2 library and very few modifications are needed to port code from R to Groovy. Essentiallyy:
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
@Grab('se.alipsa.matrix:matrix-core:3.5.1')
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

# Release version compatibility matrix
See the [Matrix BOM](https://mvnrepository.com/artifact/se.alipsa.matrix/matrix-bom) for the recommended matrix library versions. 