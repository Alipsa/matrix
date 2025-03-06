package test.alipsa.matrix.xchart

import org.junit.jupiter.api.Test
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.HeatMapChart
import org.knowm.xchart.HeatMapChartBuilder
import org.knowm.xchart.style.Styler
import org.knowm.xchart.style.theme.MatlabTheme
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixBuilder
import se.alipsa.matrix.xchart.HeatmapChart
import se.alipsa.matrix.xchart.MatrixTheme

import static org.junit.jupiter.api.Assertions.assertTrue

class HeatmapChartTest {

  @Test
  void testHeatmap() {
    def data = [123, 210, 89, 87, 245, 187, 100, 110, 99, 59, 110, 101]
    Matrix matrix = new MatrixBuilder().data(
        'c': data
    ).types(Number)
    .build()

    def hc = HeatmapChart.create(matrix, 1000, 600)
        .setTitle(getClass().getSimpleName())
        .addSeries("Basic HeatMap",'c', 4)

    File file = new File("build/testHeatmap.png")
    hc.exportPng(file)
    assertTrue(file.exists())
    Matrix hm = hc.heatMapMatrix.withMatrixName("matrix chart")
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
    for (int r in rowNums) {
      def row = []
      for (int c in columnNums) {
        heatData[c][r] = data[idx]
        row << data[idx]
        idx++
      }
      rows << row
    }

    Matrix m = new MatrixBuilder('xchart').rows(rows).build()
    chart.addSeries("Basic HeatMap", columnNums, rowNums, heatData)
    def file2 = new File("build/testHeatmap2.png")
    BitmapEncoder.saveBitmap(chart, file2.absolutePath, BitmapEncoder.BitmapFormat.PNG)
    assertTrue(file2.exists())
    assertTrue(m.equals(hm,true, true), m.diff(hm))

    // Same thing, different way of constructing
    Matrix m2 = new MatrixBuilder('matrix chart3').rows([
        [123, 210, 89, 87],
        [245, 187, 100, 110],
        [99, 59, 110, 101]]
    ).types([Number]*4)
        .build()
    def hmc = HeatmapChart.create(m2, 1000, 600)
    .addSeries("Basic HeatMap", m2['c1'], m2['c2'], m2['c3'], m2['c4'])
    File file3 = new File("build/testHeatmap3.png")
    hmc.exportPng(file3)
    assertTrue(file3.exists())
    Matrix hmc3 = hmc.heatMapMatrix.withMatrixName("matrix chart3")
    assertTrue(hm.equals(hmc3,true, true, false), hm.diff(hmc3))
  }
}
