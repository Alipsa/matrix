[![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.alipsa.matrix/matrix-xchart/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.alipsa.matrix/matrix-xchart)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-xchart/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-xchart)
# Matrix-xchart

Matrix-xchart integrates the Matrix library with the [XChart library](https://knowm.org/open-source/xchart/)

The se.alipsa.matrix.xchart package contains factory classes for each chart type.

To use it add the following to your gradle build script (or equivalent for maven etc)
```groovy
implementation 'org.apache.groovy:groovy:4.0.28'
implementation 'se.alipsa.matrix:matrix-core:3.3.0'
implementation 'se.alipsa.matrix:matrix-xchart:0.2.1'
```
Here is an example usage for a Line Chart:

```groovy
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.LineChart

Matrix matrix = Matrix.builder()
  .data(
      X1: [0.0, 1.0, 2.0],
      Y1: [2.0, 1.0, 0.0],
      X2: [1.8, 1.5, 0.5],
      Y2: [0.0, 1.0, 1.5],
  ).types([Double] * 4)
  .matrixName("Lines")
  .build()

LineChart chart = LineChart.create(matrix, 600, 500)
    // add a series with a different series name
    .addSeries('First', 'X1', 'Y1')
    // add a series by specifying the column names
    .addSeries('X2', 'Y2')
// Write to a png file
chart.exportPng(new File("./build/testLineChart.png"))

// Write it to an output stream
try (FileOutputStream fos = new FileOutputStream("./build/testLineChart2.svg")) {
  // add an additional series to the chart with an optional series name
  // instead of specifying the column name, we provide the columns themselves
  chart.addSeries('Third', matrix.X1, matrix.Y2).exportSvg(fos)
}
```
See the [tests](https://github.com/Alipsa/matrix/tree/main/matrix-xchart/src/test/groovy/test/alipsa/matrix/xchart) for more examples.

You can easily export to png, svg or swing using one of the exportXXX methods but if you need something else you can always get the underlying XChart and use one of the encoders. E.g:
```groovy
VectorGraphicsEncoder.saveVectorGraphic(chart.xchart, file.absolutePath, VectorGraphicsEncoder.VectorGraphicsFormat.PDF)
```

The Chart types supported are:
- [AreaChart](https://github.com/Alipsa/matrix/blob/main/matrix-xchart/src/main/groovy/se/alipsa/matrix/xchart/AreaChart.groovy)
  -  An area chart or area graph displays graphically quantitative data. It is based on an XY chart. It is usually used to compare two or more quantities over time. 
- [BarChart](https://github.com/Alipsa/matrix/blob/main/matrix-xchart/src/main/groovy/se/alipsa/matrix/xchart/BarChart.groovy)
  - A bar chart or bar graph is a chart or graph that presents categorical data with rectangular bars with heights or lengths proportional to the values that they represent.
- [BoxChart](https://github.com/Alipsa/matrix/blob/main/matrix-xchart/src/main/groovy/se/alipsa/matrix/xchart/BoxChart.groovy)
  - A box chart or box plot is a standardized way of displaying the distribution of data based on a five-number summary ("minimum", first quartile (Q1), median, third quartile (Q3), and "maximum").
- [BubbleChart](https://github.com/Alipsa/matrix/blob/main/matrix-xchart/src/main/groovy/se/alipsa/matrix/xchart/BubbleChart.groovy)
  - A bubble chart is a scatter plot in which a third dimension of the data is shown through the size of markers.
- [CorrelationHeatmapChart](https://github.com/Alipsa/matrix/blob/main/matrix-xchart/src/main/groovy/se/alipsa/matrix/xchart/CorrelationHeatmapChart.groovy)
  - A correlation heatmap chart is a graphical representation of the correlation matrix, where the values are represented by colors. It is used to visualize the correlation between different variables in a dataset.
- [HeatmapChart](https://github.com/Alipsa/matrix/blob/main/matrix-xchart/src/main/groovy/se/alipsa/matrix/xchart/HeatmapChart.groovy)
  - A heatmap is a 2-dimensional data visualization technique that represents the magnitude of individual values within a dataset as a color.
- [HistogramChart](https://github.com/Alipsa/matrix/blob/main/matrix-xchart/src/main/groovy/se/alipsa/matrix/xchart/HistogramChart.groovy)
  - A visual representation of the distribution of quantitative data.
- [LineChart](https://github.com/Alipsa/matrix/blob/main/matrix-xchart/src/main/groovy/se/alipsa/matrix/xchart/LineChart.groovy)
  - a type of chart that displays information as a series of data points called 'markers' connected by straight line segments.
- [OhlcChart](https://github.com/Alipsa/matrix/blob/main/matrix-xchart/src/main/groovy/se/alipsa/matrix/xchart/OhlcChart.groovy)
  - An open-high-low-close chart (OHLC) is a type of chart typically used in technical analysis to illustrate movements in the price of a financial instrument over time. Each vertical line on the chart shows the price range (the highest and lowest prices) over one unit of time, e.g., one day or one hour. 
- [PieChart](https://github.com/Alipsa/matrix/blob/main/matrix-xchart/src/main/groovy/se/alipsa/matrix/xchart/PieChart.groovy)
  - A pie chart (or a circle chart) is a circular statistical graphic which is divided into slices to illustrate numerical proportion.
- [RadarChart](https://github.com/Alipsa/matrix/blob/main/matrix-xchart/src/main/groovy/se/alipsa/matrix/xchart/RadarChart.groovy)
  - A radar chart is a graphical method of displaying multivariate data in the form of a two-dimensional chart of three or more quantitative variables represented on axes starting from the same point.
- [ScatterChart](https://github.com/Alipsa/matrix/blob/main/matrix-xchart/src/main/groovy/se/alipsa/matrix/xchart/ScatterChart.groovy)
  - a type of plot or mathematical diagram using Cartesian coordinates to display values for typically two variables for a set of data. If the points are coded (color/shape/size), additional variables can be displayed.
- [StickChart](https://github.com/Alipsa/matrix/blob/main/matrix-xchart/src/main/groovy/se/alipsa/matrix/xchart/StickChart.groovy)
  -