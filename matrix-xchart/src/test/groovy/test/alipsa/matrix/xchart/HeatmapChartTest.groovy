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

import static org.junit.jupiter.api.Assertions.assertEquals
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
        .addSeries("Basic HeatMap",'c')
    hc.style.setPlotContentSize(1)
    hc.style.setShowValue(true)
    File file = new File("build/testHeatmap.png")
    hc.exportPng(file)
    assertTrue(file.exists())
    Matrix hm = hc.heatMapMatrix
    //println hm.content()

    // Construct a heatmap using XChart directly to compare
    HeatMapChart chart =
        new HeatMapChartBuilder().width(1000).height(600)
            .title(getClass().getSimpleName()).build()

    chart.getStyler().theme = new MatrixTheme()
    chart.getStyler().setPlotContentSize(1)
    chart.getStyler().setShowValue(true)


    int[] xData = [1, 2, 3, 4]
    int[] yData = [1, 2, 3]
    int[][] heatData = new int[xData.length][yData.length]
    def rows = []
    for (int i = 0; i < xData.length; i++) {
      def row = []
      for (int j = 0; j < yData.length; j++) {
        heatData[i][j] = data[i+j]
        rows << [i, j, data[i+j]]
      }
    }

    Matrix m = new MatrixBuilder().rows(rows).build()
    //println m.content()
    chart.addSeries("Basic HeatMap", xData, yData, heatData)
    file = new File("build/testHeatmap2.png")
    BitmapEncoder.saveBitmap(chart, file.absolutePath, BitmapEncoder.BitmapFormat.PNG)
    assertTrue(file.exists())
    assertEquals(m, hm, m.diff(hm))
  }
}
