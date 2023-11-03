# Charts
Groovy library for creating graphs based on Matrix or [][] data

add the following dependency to your gradle build script
```groovy
implementation 'se.alipsa.groovy:charts:1.0.0-SNAPSHOT'
```

... or maven pom.xml
```xml
<dependency>
    <groupId>se.alipsa.groovy</groupId>
    <artifactId>charts</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Example usage

```groovy
import java.time.LocalDate
import se.alipsa.groovy.matrix.*
import se.alipsa.groovy.charts.*

def empData = Matrix.create(
    emp_id: 1..5,
    emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
    salary: [623.3,515.2,611.0,729.0,843.25],
    start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27"),
    [int, String, Number, LocalDate]
)

def areaChart = AreaChart.create("Salaries", empData, "emp_name", "salary")
def barChart = BarChart.createVertical("Salaries", empData, "emp_name", ChartType.NONE, "salary")
def pieChart = PieChart.create("Salaries", empData, "emp_name", "salary")

// Use the Plot class to output the chart, e.g:
Plot.png(areaChart, new File("areaChart.png"))

Plot.svg(barChart, new File("barChart.svg"))

javafx.scene.chart.Chart jfxPieChart = Plot.jfx(pieChart)
```