[![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.alipsa.matrix/matrix-xchart/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.alipsa.matrix/matrix-xchart)
[![javadoc](https://javadoc.io/badge2/se.alipsa.matrix/matrix-xchart/javadoc.svg)](https://javadoc.io/doc/se.alipsa.matrix/matrix-xchart)
# Matrix-xchart

Matrix-xchart integrates the Matrix library with the [XChart library](https://knowm.org/open-source/xchart/)

The se.alipsa.matrix.xchart package contains factory classes for each chart type.

To use it add the following to your gradle build script (or equivalent for maven etc)
```groovy
implementation 'org.apache.groovy:groovy:4.0.26'
implementation 'se.alipsa.matrix:matrix-core:3.0.0'
implementation 'se.alipsa.matrix:matrix-xchart:0.1'
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