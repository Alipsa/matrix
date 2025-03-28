package test.alipsa.matrix.xchart

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.BoxChart

class BoxChartTest {

  @Test
  void testBoxChart() {
    Matrix matrix = Matrix.builder(). data(
        'aaa': [40, 30, 20, 60, 50],
        'bbb': [-20, -10, -30, -15, -25],
        'ccc': [50, -20, 10, 5, 1]
    ).types([Number]*3)
    .matrixName("Box chart")
    .build()

    def bc = BoxChart.create(matrix)
    .addSeries('aaa')
    .addSeries('BBB', matrix.bbb)
    .addSeries(matrix['ccc'])

    File file = new File('build/testBoxChart.png')
    bc.exportPng(file)
    assert file.exists()
  }
}
