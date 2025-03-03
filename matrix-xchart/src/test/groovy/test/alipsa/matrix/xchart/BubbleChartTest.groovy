package test.alipsa.matrix.xchart

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixBuilder
import se.alipsa.matrix.xchart.BubbleChart

class BubbleChartTest {

  @Test
  void testBubbleChart() {

    Matrix matrix = new MatrixBuilder().data(
        xData: [1.5, 2.6, 3.3, 4.9, 5.5, 6.3, 1, 2.0, 3.0, 4.0, 5, 6],
        yData: [10, 4, 7, 7.7, 7, 5.5, 10, 4, 7, 1, 7, 9],
        values: [17, 40, 50, 51, 26, 20, 66, 35, 80, 27, 29, 44]
    ).types([Number]*3)
    .build()

    Matrix m2 = new MatrixBuilder().data(
        xData: [1, 2.0, 3.0, 4.0, 5, 6, 1.5, 2.6, 3.3, 4.9, 5.5, 6.3],
        yData: [1, 2, 3, 4, 5, 6, 10, 8.5, 4, 1, 4.7, 9],
        values: [37, 35, 80, 27, 29, 44, 57, 40, 50, 33, 26, 20]
    ).types([Number]*3)
    .build()

    def bc = BubbleChart.create(matrix)
      .addSeries('xData', 'yData', 'values')
      .addSeries('B', m2.xData, m2.yData, m2.values, 50)

    File file = new File('build/testBubbleChart.png')
    bc.exportPng(file)
    assert file.exists()
  }
}
