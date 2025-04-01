# Charts
Groovy library for creating graphs based on Matrix or [][] data

add the following dependency to your gradle build script
```groovy
implementation 'se.alipsa.matrix:charts:0.3.0'
```

... or maven pom.xml
```xml
<dependency>
    <groupId>se.alipsa.matrix</groupId>
    <artifactId>charts</artifactId>
    <version>0.3.0</version>
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

# Release version compatibility matrix
The following table illustrates the version compatibility of 
matrix-charts, matrix-core, and matrix-stats

| Matrix charts | Matrix core | Matrix stats |
|--------------:|------------:|-------------:|
|  0.2 -> 0.3.0 |       3.0.0 |        2.0.0 |