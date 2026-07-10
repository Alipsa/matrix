package test.alipsa.matrix.xchart

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.HeatMapChart
import org.knowm.xchart.HeatMapChartBuilder

import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.stats.Normalize
import se.alipsa.matrix.xchart.HeatmapChart
import se.alipsa.matrix.xchart.MatrixTheme

class HeatmapChartTest {

  @Test
  void testHeatmap() {
    def data = [123, 210, 89, 87, 245, 187, 100, 110, 99, 59, 110, 101]
    Matrix matrix = Matrix.builder().data(
        'c': data
    ).types(Number)
    .build()

    def hc = HeatmapChart.create(matrix, 1000, 600)
        .setTitle(getClass().getSimpleName())
        .addSeries('Basic HeatMap', 'c', 4)

    File file = new File('build/testHeatmap.png')
    hc.exportPng(file)
    assertTrue(file.exists())
    Matrix hm = hc.heatMapMatrix.withMatrixName('matrix chart')
    // Construct a heatmap using XChart directly to compare
    HeatMapChart chart =
        new HeatMapChartBuilder().width(1000).height(600)
            .title(getClass().getSimpleName()).build()

    chart.getStyler().theme = new MatrixTheme()
    chart.getStyler().setPlotContentSize(1)
    chart.getStyler().setShowValue(true)

    int[] columnNums = [0, 1, 2, 3]
    int[] rowNums = [0, 1, 2]
    int[][] heatData = new int[columnNums.length][rowNums.length]
    def rows = []

    int idx = 0
    rowNums.each { int r ->
      def row = []
      columnNums.each { int c ->
        heatData[c][r] = data[idx]
        row << data[idx]
        idx++
      }
      rows << row
    }

    Matrix m = Matrix.builder('xchart').rows(rows).build()
    chart.addSeries('Basic HeatMap', columnNums, rowNums, heatData)
    def file2 = new File('build/testHeatmap2.png')
    BitmapEncoder.saveBitmap(chart, file2.absolutePath, BitmapEncoder.BitmapFormat.PNG)
    assertTrue(file2.exists())
    assertTrue(m.equals(hm, true, true), m.diff(hm))

    // Same thing, different way of constructing
    Matrix m2 = Matrix.builder('matrix chart3').rows([
        [123, 210, 89, 87],
        [245, 187, 100, 110],
        [99, 59, 110, 101]]
    ).types([Number] * 4)
        .build()
    def hmc = HeatmapChart.create(m2, 1000, 600)
    .addSeries('Basic HeatMap', [m2['c1'], m2['c2'], m2['c3'], m2['c4']])
    File file3 = new File('build/testHeatmap3.png')
    hmc.exportPng(file3)
    assertTrue(file3.exists())
    Matrix hmc3 = hmc.heatMapMatrix.withMatrixName('matrix chart3')
    assertTrue(hm.equals(hmc3, true, true, false), hm.diff(hmc3))
  }

  @Test
  void testVerifyDistribution() {
    def hmc = HeatmapChart.create(Dataset.mtcars())
        .addAllToSeriesBy('model')
        .setXLabel('Features')
        .setYLabel('Cars')
    // println hmc.heatMapMatrix.content()
    File file = new File('build/testVerifyDistribution.png')
    hmc.exportPng(file)
    assertTrue(file.exists())
  }

  @Test
  void testVectorHeatmapRejectsUnevenInput() {
    Matrix matrix = Matrix.builder()
        .data(c: [1, 2, 3, 4, 5])
        .types(Number)
        .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      HeatmapChart.create(matrix).addSeries('Uneven', 'c', 2)
    }

    assertTrue(exception.message.contains('cannot be evenly divided'))
  }

  @Test
  void testVectorHeatmapRejectsInvalidColumnCount() {
    Matrix matrix = Matrix.builder()
        .data(c: [1, 2, 3, 4])
        .types(Number)
        .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      HeatmapChart.create(matrix).addSeries('Invalid', 'c', 0)
    }

    assertTrue(exception.message.contains('column count'))
  }

  @Test
  void testVectorHeatmapRejectsAutoDetectionWithoutSquareInput() {
    Matrix matrix = Matrix.builder()
        .data(c: [1, 2, 3, 4, 5])
        .types(Number)
        .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      HeatmapChart.create(matrix).addSeries('Auto', 'c')
    }

    assertTrue(exception.message.contains('no integer square root'))
  }

  @Test
  void testHeatmapRejectsEmptyColumnLists() {
    Matrix matrix = Matrix.builder().data(c: [1, 2]).types(Number).build()

    assertThrows(IllegalArgumentException) {
      HeatmapChart.create(matrix).addSeries('Empty', [])
    }

    assertThrows(IllegalArgumentException) {
      HeatmapChart.create(matrix).addSeries('Empty', [], [], [])
    }
  }

  @Test
  void testHeatmapRejectsNullColumnInList() {
    Matrix matrix = Matrix.builder().data(c: [1, 2]).types(Number).build()

    assertThrows(IllegalArgumentException) {
      HeatmapChart.create(matrix).addSeries('Null', [null, matrix['c']])
    }
  }

  @Test
  void testHeatmapRejectsMismatchedColumnLengths() {
    Matrix matrix = Matrix.builder()
        .data(a: [1, 2, 3], b: [4, 5, 6])
        .types([Number, Number])
        .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      HeatmapChart.create(matrix).addSeries('Mismatch', [matrix['a'], new Column('short', [4, 5], Number)])
    }

    assertTrue(exception.message.contains('equal lengths'))
  }

  @Test
  void testAddAllToSeriesByRejectsUnnamedMatrix() {
    Matrix matrix = Matrix.builder()
        .data(model: ['a', 'b'], value: [1, 2])
        .types([String, Number])
        .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      HeatmapChart.create(matrix).addAllToSeriesBy('model')
    }

    assertTrue(exception.message.contains('name'))
  }

  /**
   * mimics
   * heatmaply(
   *   normalize(mtcars),
   *   xlab = 'Features',
   *   ylab = 'Cars',
   *   main = 'Data Scaling'
   * )
   */
  @Test
  void testNormalizedHeatmap() {
    def mtcars = Normalize.minMaxNorm(Dataset.mtcars(), 5).withMatrixName('Data Scaling')
    def hmc = HeatmapChart.create(mtcars).addAllToSeriesBy('model')
    hmc.XLabel = 'Features'
    hmc.YLabel = 'Cars'
    hmc.style.showValue = false
    File file = new File('build/testNormalizedHeatmap.png')
    hmc.exportPng(file)
    assertTrue(file.exists())
  }

  @Test
  void testCustomLabelsMapToCorrectAxesForNonSquareGrid() {
    Matrix matrix = Matrix.builder().data(
        c1: [1, 2, 3],
        c2: [4, 5, 6],
        c3: [7, 8, 9],
        c4: [10, 11, 12]
    ).types([Number] * 4).build()

    // 4 columns, 3 rows: nCols != nRows so a label swap would misalign the grid
    List<String> columnLabels = ['w', 'x', 'y', 'z']
    List<String> rowLabels = ['r1', 'r2', 'r3']

    def hmc = HeatmapChart.create(matrix)
        .addSeries('Custom', columnLabels, rowLabels, [matrix['c1'], matrix['c2'], matrix['c3'], matrix['c4']])

    def series = hmc.getSeries('Custom')
    assertEquals(columnLabels, series.xData, 'xData should be the column labels (X-axis)')
    assertEquals(rowLabels, series.yData, 'yData should be the row labels (Y-axis)')

    Map<String, Number> cellByLabel = series.heatData.collectEntries { Number[] point ->
      String x = series.xData[point[0].intValue()] as String
      String y = series.yData[point[1].intValue()] as String
      [("$x,$y".toString()): point[2]]
    }
    assertEquals(1, cellByLabel['w,r1'])
    assertEquals(2, cellByLabel['w,r2'])
    assertEquals(3, cellByLabel['w,r3'])
    assertEquals(4, cellByLabel['x,r1'])
    assertEquals(10, cellByLabel['z,r1'])
    assertEquals(12, cellByLabel['z,r3'])
  }

}
