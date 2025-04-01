# Matrix XChart Module

The matrix-xchart module integrates the Matrix library with the XChart library, providing a convenient way to create various types of charts and visualizations from Matrix data. This module makes it easy to generate professional-looking charts with minimal code.

## Installation

To use the matrix-xchart module, you need to add it as a dependency to your project.

### Gradle Configuration

```groovy
implementation 'org.apache.groovy:groovy:4.0.26'
implementation platform('se.alipsa.matrix:matrix-bom:2.2.0')
implementation 'se.alipsa.matrix:matrix-core'
implementation 'se.alipsa.matrix:matrix-xchart'
```

### Maven Configuration

```xml
<project>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>se.alipsa.matrix</groupId>
        <artifactId>matrix-bom</artifactId>
        <version>2.2.0</version>
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
      <artifactId>matrix-core</artifactId>
    </dependency>
    <dependency>
      <groupId>se.alipsa.matrix</groupId>
      <artifactId>matrix-xchart</artifactId>
    </dependency>
  </dependencies>
</project>
```

## Chart Types

The matrix-xchart module provides factory classes for creating various types of charts:

1. **LineChart** - For creating line charts
2. **BoxChart** - For creating box plots
3. **BubbleChart** - For creating bubble charts
4. **CategoryChart** - For creating bar charts and other category-based charts
5. **HeatmapChart** - For creating heatmap visualizations
6. **HistogramChart** - For creating histograms
7. **OhlcChart** - For creating Open-High-Low-Close charts (commonly used for financial data)
8. **PieChart** - For creating pie charts
9. **RadarChart** - For creating radar/spider charts
10. **XyChart** - For creating scatter plots and other XY-based charts

## Creating Charts

Each chart type has a similar API pattern, making it easy to learn and use. Let's explore how to create different types of charts.

### Line Chart

Line charts are useful for showing trends over time or other continuous variables.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.LineChart

// Create a Matrix with data for the line chart
Matrix matrix = Matrix.builder()
  .data(
      X1: [0.0, 1.0, 2.0],
      Y1: [2.0, 1.0, 0.0],
      X2: [1.8, 1.5, 0.5],
      Y2: [0.0, 1.0, 1.5]
  )
  .types([Double] * 4)  // All columns are Double type
  .matrixName("Lines")
  .build()

// Create a line chart with specified width and height
LineChart chart = LineChart.create(matrix, 600, 500)
    // Add a series with a custom name, using X1 and Y1 columns
    .addSeries('First', 'X1', 'Y1')
    // Add another series using X2 and Y2 columns (series name will be "X2, Y2")
    .addSeries('X2', 'Y2')

// Export the chart to a PNG file
chart.exportPng(new File("./lineChart.png"))
```

### Box Chart

Box charts (box plots) are useful for showing the distribution of data.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.BoxChart

// Create a Matrix with data for the box chart
Matrix matrix = Matrix.builder().data(
    'aaa': [40, 30, 20, 60, 50],
    'bbb': [-20, -10, -30, -15, -25],
    'ccc': [50, -20, 10, 5, 1]
).types([Number] * 3)
  .matrixName("Box chart")
  .build()

// Create a box chart
def bc = BoxChart.create(matrix)
    // Add series using column names
    .addSeries('aaa')
    // Add series with a custom name
    .addSeries('BBB', matrix.bbb)
    // Add series using column access syntax
    .addSeries(matrix['ccc'])

// Export the chart to a PNG file
File file = new File("boxChart.png")
bc.exportPng(file)
```

### Bubble Chart

Bubble charts are similar to scatter plots but add a third dimension represented by the size of the bubbles.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.BubbleChart

// Create a Matrix with data for the bubble chart
Matrix matrix = Matrix.builder().data(
    X: [1, 2, 3, 4, 5],
    Y: [10, 15, 8, 12, 7],
    Bubble: [20, 30, 15, 25, 10]  // Bubble size
).types([Number] * 3)
  .matrixName("Bubbles")
  .build()

// Create a bubble chart
def bc = BubbleChart.create(matrix)
    // Add a series with X, Y, and Bubble columns
    .addSeries("Bubble Series", "X", "Y", "Bubble")

// Export the chart to a PNG file
bc.exportPng(new File("bubbleChart.png"))
```

### Bar Chart

Category charts include bar charts, which are useful for comparing values across categories.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.BarChart

// Create a Matrix with data for the category chart
Matrix matrix = Matrix.builder().data(
    Category: ["A", "B", "C", "D", "E"],
    Value1: [10, 15, 8, 12, 7],
    Value2: [5, 8, 12, 9, 11]
).types([String, Number, Number])
  .matrixName("Categories")
  .build()

// Create a category chart
def cc = BarChart.create(matrix)
    // Add series using category and value columns
    .addSeries("Series 1", "Category", "Value1")
    .addSeries("Series 2", "Category", "Value2")

// Export the chart to a PNG file
cc.exportPng(new File("barChart.png"))
```

### Heatmap Chart

Heatmap charts are useful for visualizing data in a matrix format with colors representing values.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.HeatmapChart

// Create a Matrix with data for the heatmap
Matrix matrix = Matrix.builder().data(
    X: [1, 2, 3, 4, 5, 6, 7, 8],
    Heat: [10, 15, 8, 12, 7, 8, 12, 11]
).types([Number] * 2)
    .matrixName("Heatmap")
    .build()

// Create a heatmap chart
def hc = HeatmapChart.create(matrix)
// Add a series with X and Heat columns
    .addSeries("Heat Series", "Heat")

// Export the chart to a PNG file
hc.exportPng(new File("heatmapChart.png"))
```

### Histogram Chart

Histogram charts are useful for showing the distribution of a single variable.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.HistogramChart

// Create a Matrix with data for the histogram
Matrix matrix = Matrix.builder().data(
    Values: [1, 2, 2, 3, 3, 3, 4, 4, 5, 5, 5, 5, 6, 7, 8, 9, 10, 4, 8, 5]
).types([Number])
    .matrixName("Histogram")
    .build()

// Create a histogram chart
def hc = HistogramChart.create(matrix).setTitle("Distribution")
    // Add a series using the Values column, distribute in 6 buckets
    .addSeries("Values", 6)

// Export the chart to a PNG file
hc.exportPng(new File("histogramChart.png"))
```

### OHLC Chart

OHLC (Open-High-Low-Close) charts are commonly used for financial data, particularly stock prices.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.OhlcChart
import java.time.LocalDate
import static se.alipsa.matrix.core.ListConverter.*

// Create a Matrix with data for the OHLC chart
Matrix matrix = Matrix.builder().data(
    Date: toDates('2023-01-01', '2023-01-2', '2023-01-03'),
    Open: [100.0, 105.0, 103.0],
    High: [110.0, 108.0, 107.0],
    Low: [95.0, 102.0, 100.0],
    Close: [105.0, 103.0, 106.0]
).types([Date, Double, Double, Double, Double])
    .matrixName("Stock Prices")
    .build()

// Create an OHLC chart
def oc = OhlcChart.create(matrix)
// Add a series with Date, Open, High, Low, and Close columns
    .addSeries("Stock", "Date", "Open", "High", "Low", "Close")

// Export the chart to a PNG file
oc.exportPng(new File("ohlcChart.png"))
```

### Pie Chart

Pie charts are useful for showing proportions of a whole.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.PieChart

// Create a Matrix with data for the pie chart
Matrix matrix = Matrix.builder().data(
    Category: ["A", "B", "C", "D", "E"],
    Value: [10, 15, 8, 12, 7]
).types([String, Number])
  .matrixName("Pie")
  .build()

// Create a pie chart
def pc = PieChart.create(matrix)
    // Add a series using category and value columns
    .addSeries("Category", "Value")

// Export the chart to a PNG file
pc.exportPng(new File("pieChart.png"))
```

### Radar Chart

Radar charts (also known as spider charts) are useful for comparing multiple variables.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.RadarChart
import se.alipsa.matrix.stats.Normalize

// Create a Matrix with data for the radar chart
Matrix matrix = Matrix.builder()
    .columnNames("Player", "Speed", "Power", "Agility", "Endurance", "Accuracy")
    .rows([
        ['Player1', 8d, 7d, 9d, 6d, 8d],
        ['Player2', 6d, 9d, 7d, 5d, 7d],
        ['Player3', 5d, 8d, 4d, 8d, 9d]
    ]).types([String] + [Double]*5)
    .matrixName("Radar")
    .build()

def normalizedMatrix = Normalize.minMaxNorm(matrix, 3)
println(normalizedMatrix.content())
// Create a radar chart
def rc = RadarChart.create(normalizedMatrix)
    // Add series using the Player column and the other columns as values
    .addSeries("Player")
// Export the chart to a PNG file
rc.exportPng(new File("radarChart.png"))
```

### Scatter Chart

Scatter plots are useful for showing relationships between two variables.

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.ScatterChart

// Create a Matrix with data for the XY chart
Matrix matrix = Matrix.builder().data(
    X1: [1, 2, 3, 4, 5],
    Y1: [10, 15, 8, 12, 7],
    X2: [1.5, 2.5, 3.5, 4.5, 5.5],
    Y2: [8, 12, 10, 14, 9]
).types([Number] * 4)
    .matrixName("Scatter")
    .build()

// Create an XY chart
def xyc = ScatterChart.create(matrix)
// Add series using X and Y columns
    .addSeries("Series 1", "X1", "Y1")
    .addSeries("Series 2", "X2", "Y2")

// Export the chart to a PNG file
xyc.exportPng(new File("ScatterChart.png"))
```

## Exporting Charts

The matrix-xchart module provides several methods for exporting charts:

### Export to PNG

```groovy
// Export to a PNG file
chart.exportPng(new File("chart.png"))
```

### Export to SVG

```groovy
// Export to an SVG file
chart.exportSvg(new File("chart.svg"))

// Or export to an output stream
try (FileOutputStream fos = new FileOutputStream("chart.svg")) {
    chart.exportSvg(fos)
}
```

### Display in Swing

```groovy
// Display the chart in a Swing window
JFrame frame=new JFrame("Main")
frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
JDialog d=new JDialog(frame, "Chart")
d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
d.setContentPane(chart.exportSwing())
d.pack()
d.setLocationRelativeTo(frame)
d.setVisible(true)
```

### Custom Export Formats

If you need to export to other formats, you can access the underlying XChart object and use the XChart encoders:

```groovy
import org.knowm.xchart.VectorGraphicsEncoder

// Export to PDF
VectorGraphicsEncoder.saveVectorGraphic(
    chart.xchart,
    "chart.pdf",
    VectorGraphicsEncoder.VectorGraphicsFormat.PDF
)
```

## Customizing Charts

You can customize various aspects of the charts:

### Chart Title and Axis Labels

```groovy
LineChart chart = LineChart.create(matrix, 600, 500)
    .setTitle("My Line Chart")
    .setXAxisTitle("X Axis")
    .setYAxisTitle("Y Axis")
    .addSeries("Series 1", "X1", "Y1")
```

### Chart Styling

```groovy
import org.knowm.xchart.style.Styler.ChartTheme

LineChart chart = LineChart.create(matrix, 600, 500, ChartTheme.GGPlot2)
    .addSeries("Series 1", "X1", "Y1")
```

### Series Styling

```groovy
import java.awt.Color
import org.knowm.xchart.style.markers.SeriesMarkers

LineChart chart = LineChart.create(matrix)
    .addSeries("Series 1", "X1", "Y1")

// Access the underlying XChart to customize series
chart.xchart.getSeriesMap().get("Series 1").setMarker(SeriesMarkers.CIRCLE)
chart.xchart.getSeriesMap().get("Series 1").setLineColor(Color.RED)
```

## Advanced Usage

### Combining Multiple Series Types

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.XyChart
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle

// Create a Matrix with data
Matrix matrix = Matrix.builder().data(
    X: [1, 2, 3, 4, 5],
    Line: [10, 15, 13, 17, 20],
    Scatter: [8, 14, 11, 15, 18]
).types([Number] * 3)
  .build()

// Create an XY chart
def chart = XyChart.create(matrix)
    // Add a line series
    .addSeries("Line Series", "X", "Line")
    // Add a scatter series
    .addSeries("Scatter Series", "X", "Scatter")

// Set different render styles for each series
chart.xchart.getSeriesMap().get("Line Series").setXYSeriesRenderStyle(XYSeriesRenderStyle.Line)
chart.xchart.getSeriesMap().get("Scatter Series").setXYSeriesRenderStyle(XYSeriesRenderStyle.Scatter)

// Export the chart
chart.exportPng(new File("combinedChart.png"))
```

### Creating Multiple Charts

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.LineChart
import se.alipsa.matrix.xchart.PieChart

// Create a Matrix with data
Matrix salesData = Matrix.builder().data(
    Month: ["Jan", "Feb", "Mar", "Apr", "May"],
    Sales: [100, 120, 90, 140, 110],
    Profit: [20, 25, 18, 30, 22]
).types([String, Number, Number])
  .build()

// Create a line chart for sales over time
def lineChart = LineChart.create(salesData)
    .setTitle("Monthly Sales")
    .addSeries("Sales", "Month", "Sales")
    .addSeries("Profit", "Month", "Profit")

// Create a pie chart for total sales by month
def pieChart = PieChart.create(salesData)
    .setTitle("Sales Distribution")
    .addSeries("Month", "Sales")

// Export both charts
lineChart.exportPng(new File("salesLineChart.png"))
pieChart.exportPng(new File("salesPieChart.png"))
```

## Best Practices

1. **Choose the Right Chart Type**: Select the appropriate chart type for your data and the story you want to tell.

2. **Keep It Simple**: Avoid cluttering your charts with too many series or data points.

3. **Use Meaningful Labels**: Always include clear titles and axis labels to make your charts self-explanatory.

4. **Consider Color Blindness**: Choose color schemes that are accessible to people with color vision deficiencies.

5. **Test Different Themes**: Experiment with different chart themes to find the one that best presents your data.

6. **Export in Vector Format**: For publication-quality charts, export to SVG or PDF rather than PNG.

## Conclusion

The matrix-xchart module provides a powerful and flexible way to create various types of charts from Matrix data. By leveraging the XChart library, it offers a wide range of chart types and customization options while maintaining a simple and consistent API.

In the next section, we'll explore the matrix-sql module, which provides functionality for interacting with databases.

Go to [previous section](7-matrix-json.md) | Go to [next section](9-matrix-sql.md) | Back to [outline](outline.md)