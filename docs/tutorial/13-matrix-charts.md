# Matrix Charts Module

The Matrix Charts module is a Groovy library for creating various types of graphs and charts based on Matrix data. 
It provides a simple and intuitive API for generating visualizations that can be exported to different formats but the
"native" format is SVG. It uses the gsvg SVG model to greate charts and it is always possible to access the model directly
for advanced customization.

## Installation

To use the matrix-charts module, add the following dependency to your project:

### Gradle Configuration

```groovy
implementation platform('se.alipsa.matrix:matrix-bom:2.1.1')
implementation 'se.alipsa.matrix:charts'
implementation 'se.alipsa.matrix:core'
implementation 'se.alipsa.matrix:stats'
```

### Maven Configuration

```xml
<project>
   <dependencyManagement>
      <dependencies>
         <dependency>
            <groupId>se.alipsa.matrix</groupId>
            <artifactId>matrix-bom</artifactId>
            <version>2.1.1</version>
            <type>pom</type>
            <scope>import</scope>
         </dependency>
      </dependencies>
   </dependencyManagement>
   <dependencies>
      <dependency>
         <groupId>org.apache.groovy</groupId>
         <artifactId>groovy</artifactId>
         <version>4.0.26</version>
      </dependency>
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
</project>
```

## Chart Types

The matrix-charts module supports several types of charts:

1. **Area Chart**: Displays data as filled areas under a line
2. **Bar Chart**: Displays data as horizontal or vertical bars
3. **Pie Chart**: Displays data as slices of a circle
4. **Line Chart**: Displays data as points connected by lines
5. **Scatter Chart**: Displays data as individual points

## Creating Charts

### Basic Chart Creation

All chart types follow a similar pattern for creation. You typically need:

1. A Matrix object containing your data
2. Column names for categories/labels
3. Column names for values

Here's a basic example of creating different types of charts:

```groovy
import java.time.LocalDate
import se.alipsa.matrix.core.*
import se.alipsa.matrix.charts.*

// Create a sample Matrix with employee data
def empData = Matrix.builder().data(
    emp_id: 1..5,
    emp_name: ["Rick", "Dan", "Michelle", "Ryan", "Gary"],
    salary: [623.3, 515.2, 611.0, 729.0, 843.25],
    start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
).types(int, String, Number, LocalDate)
 .build()

// Create different types of charts
def areaChart = AreaChart.create("Salaries", empData, "emp_name", "salary")
def barChart = BarChart.createVertical("Salaries", empData, "emp_name", ChartType.NONE, "salary")
def pieChart = PieChart.create("Salaries", empData, "emp_name", "salary")
```

### Area Chart

Area charts are useful for showing trends over time or comparing values across categories with an emphasis on the magnitude of the values.

```groovy
import se.alipsa.matrix.core.*
import se.alipsa.matrix.charts.*

// Create a Matrix with time series data
def timeData = Matrix.builder().data(
    month: ["Jan", "Feb", "Mar", "Apr", "May", "Jun"],
    sales: [10.2, 15.1, 12.7, 14.3, 18.5, 16.2],
    profit: [5.1, 6.3, 5.5, 7.1, 9.2, 8.4]
).types(String, Number, Number)
 .build()

// Create an area chart with multiple series
def areaChart = AreaChart.create("Monthly Performance", timeData, "month", "sales", "profit")

// Export the chart to PNG
Plot.png(areaChart, new File("monthly_performance.png"))
```

### Bar Chart

Bar charts are excellent for comparing values across different categories. You can create both vertical and horizontal bar charts.

```groovy
import se.alipsa.matrix.core.*
import se.alipsa.matrix.charts.*

// Create a Matrix with categorical data
def productData = Matrix.builder().data(
    product: ["Product A", "Product B", "Product C", "Product D"],
    sales_2022: [120, 150, 90, 110],
    sales_2023: [140, 160, 100, 130]
).types(String, Number, Number)
 .build()

// Create a vertical bar chart with multiple series
def verticalBarChart = BarChart.createVertical(
    "Product Sales Comparison", 
    productData, 
    "product", 
    ChartType.GROUPED, 
    "sales_2022", "sales_2023"
)

// Create a horizontal bar chart
def horizontalBarChart = BarChart.createHorizontal(
    "Product Sales Comparison", 
    productData, 
    "product", 
    ChartType.GROUPED, 
    "sales_2022", "sales_2023"
)

// Export the charts
Plot.png(verticalBarChart, new File("vertical_bar_chart.png"))
Plot.png(horizontalBarChart, new File("horizontal_bar_chart.png"))
```

The `ChartType` enum provides different styles for bar charts:
- `ChartType.NONE`: Standard bars
- `ChartType.GROUPED`: Groups bars for multiple series
- `ChartType.STACKED`: Stacks bars for multiple series

### Pie Chart

Pie charts are useful for showing proportions of a whole.

```groovy
import se.alipsa.matrix.core.*
import se.alipsa.matrix.charts.*

// Create a Matrix with market share data
def marketData = Matrix.builder().data(
    company: ["Company A", "Company B", "Company C", "Others"],
    market_share: [35, 28, 22, 15]
).types(String, Number)
 .build()

// Create a pie chart
def pieChart = PieChart.create("Market Share", marketData, "company", "market_share")

// Export the chart
Plot.png(pieChart, new File("market_share.png"))
```

### Line Chart

Line charts are ideal for showing trends over time or continuous data.

```groovy
import se.alipsa.matrix.core.*
import se.alipsa.matrix.charts.*
import java.time.LocalDate

// Create a Matrix with time series data
def temperatureData = Matrix.builder().data(
    date: [
        LocalDate.of(2023, 1, 1),
        LocalDate.of(2023, 2, 1),
        LocalDate.of(2023, 3, 1),
        LocalDate.of(2023, 4, 1),
        LocalDate.of(2023, 5, 1),
        LocalDate.of(2023, 6, 1)
    ],
    city_a: [5, 7, 12, 18, 23, 28],
    city_b: [2, 4, 9, 15, 20, 25]
).types(LocalDate, Number, Number)
 .build()

// Create a line chart
def lineChart = LineChart.create(
    "Temperature Trends", 
    temperatureData, 
    "date", 
    "city_a", "city_b"
)

// Export the chart
Plot.png(lineChart, new File("temperature_trends.png"))
```

### Scatter Chart

Scatter charts are useful for showing the relationship between two variables.

```groovy
import se.alipsa.matrix.core.*
import se.alipsa.matrix.charts.*

// Create a Matrix with correlation data
def correlationData = Matrix.builder().data(
    height: [165, 170, 175, 180, 185, 190],
    weight: [60, 65, 70, 80, 85, 90]
).types(Number, Number)
 .build()

// Create a scatter chart
def scatterChart = ScatterChart.create(
    "Height vs Weight", 
    correlationData, 
    "height", 
    "weight"
)

// Export the chart
Plot.png(scatterChart, new File("height_weight_correlation.png"))
```

## Exporting Charts

The matrix-charts module provides several ways to export charts using the `Plot` class:

### Export to PNG

```groovy
// Export to PNG file
Plot.png(chart, new File("chart.png"))

// Export to PNG with custom dimensions
Plot.png(chart, new File("chart.png"), 800, 600)
```

### Export to SVG

```groovy
// Export to SVG file
Plot.svg(chart, new File("chart.svg"))

// Export to SVG with custom dimensions
Plot.svg(chart, new File("chart.svg"), 800, 600)
```

### Export to JavaFX

If you're working with a JavaFX application, you can convert the chart to a JavaFX chart:

```groovy
// Get a JavaFX chart object
javafx.scene.chart.Chart jfxChart = Plot.jfx(chart)

// Now you can add this chart to your JavaFX scene
```

## Customizing Charts

### Setting Titles and Labels

You can customize chart titles and axis labels:

```groovy
// Create a chart with custom title and axis labels
def barChart = BarChart.createVertical(
    "Product Sales 2023",  // Chart title
    productData, 
    "product", 
    ChartType.NONE, 
    "sales"
)
.setXAxisLabel("Products")  // X-axis label
.setYAxisLabel("Sales (in thousands)")  // Y-axis label
```

### Customizing Colors

You can customize the colors used in charts:

```groovy
import javafx.scene.paint.Color

// Create a pie chart with custom colors
def pieChart = PieChart.create("Market Share", marketData, "company", "market_share")
    .setColors([
        Color.BLUE,
        Color.GREEN,
        Color.RED,
        Color.ORANGE
    ])
```

### Adding Legends

You can control the display of legends:

```groovy
// Create a chart with a legend
def lineChart = LineChart.create("Temperature Trends", temperatureData, "date", "city_a", "city_b")
    .setLegendVisible(true)
    .setLegendSide(javafx.geometry.Side.RIGHT)
```

## Complete Example

Here's a complete example that demonstrates creating and exporting multiple chart types:

```groovy
import java.time.LocalDate
import javafx.scene.paint.Color
import se.alipsa.matrix.core.*
import se.alipsa.matrix.charts.*

// Create a sample Matrix with sales data
def salesData = Matrix.builder().data(
        quarter: ['Q1', 'Q2', 'Q3', 'Q4'],
        product_a: [120, 150, 160, 180],
        product_b: [90, 110, 130, 150],
        product_c: [70, 80, 100, 120]
).types(String, Number, Number, Number)
        .build()

// Create a bar chart
def barChart = BarChart.createVertical(
        "Quarterly Sales by Product",
        salesData,
        "quarter",
        ChartType.GROUPED,
        "product_a", "product_b", "product_c"
)
        .setXAxisTitle("Quarter")
        .setYAxisTitle("Sales (in thousands)")
File file = new File("quarterly_sales_bar.png")
Plot.png(barChart, file)
println "saved barchart to $file.absolutePath"

salesData['quarter'] = [1,2,3,4]

// Create a line chart
def lineChart = LineChart.create(
        "Sales Trends",
        salesData,
        "quarter",
        "product_a", "product_b", "product_c"
)
        .setXAxisTitle("Quarter")
        .setYAxisTitle("Sales (in thousands)")
file = new File("quarterly_sales_line.png")
Plot.png(lineChart, file)
println "saved lineChart to $file.absolutePath"

// Create a pie chart (using only one quarter for demonstration)
def q4Data = Matrix.builder().data(
        product: ["Product A", "Product B", "Product C"],
        sales: [180, 150, 120]
).types(String, Number)
        .build()

def pieChart = PieChart.create(
        "Q4 Sales Distribution",
        q4Data,
        "product",
        "sales"
)
file = new File("q4_sales_pie.png")
Plot.png(pieChart, file)
println "saved pieChart to $file.absolutePath"
println "Charts have been exported successfully."
```

## Best Practices

1. **Choose the Right Chart Type**: Select the appropriate chart type for your data and the story you want to tell.
   - Use line charts for trends over time
   - Use bar charts for comparing categories
   - Use pie charts for showing proportions
   - Use scatter charts for showing correlations

2. **Keep It Simple**: Avoid cluttering your charts with too many series or data points.

3. **Use Meaningful Labels**: Always include clear titles and axis labels to make your charts self-explanatory.

4. **Consider Color Choices**: Choose colors that are visually distinct and accessible to people with color vision deficiencies.

5. **Size Appropriately**: Export charts at appropriate dimensions for their intended use.

## Conclusion

The matrix-charts module provides a powerful and flexible way to create various types of charts from Matrix data. It offers a simple API for generating visualizations that can be exported to different formats, making it easy to include data visualizations in your Groovy applications.

In the next section, we'll explore the matrix-tablesaw module, which provides interoperability with the Tablesaw library for advanced data manipulation and analysis.

Go to [previous section](12-matrix-bigquery.md) | Go to [next section](14-matrix-tablesaw.md) | Back to [outline](outline.md)