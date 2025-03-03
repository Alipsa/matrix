package test.alipsa.matrix.xchart

import org.junit.jupiter.api.Test
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.HeatMapChart
import org.knowm.xchart.HeatMapChartBuilder
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.MatrixBuilder
import se.alipsa.matrix.xchart.HeatmapChart

import static org.junit.jupiter.api.Assertions.assertTrue

class HeatmapChartTest {

  @Test
  void testHeatmap() {
    Matrix matrix = new MatrixBuilder().data(
        'red': [123, 210, 89, 87],
        'green': [245, 187, 100, 110],
        'blue': [99, 59, 110, 101]
    ).types([Number]*3)
    .build()

    println matrix.content()

    def hc = HeatmapChart.create(matrix)
      .addSeries('heatmap','red', 'green', 'blue')
    File file = new File("build/testHeatmap.png")
    hc.exportPng(file)
    assertTrue(file.exists())
  }

  @Test
  void testRandomHeatmap() {
    HeatMapChart chart =
        new HeatMapChartBuilder().width(1000).height(600).title(getClass().getSimpleName()).build();

    chart.getStyler().setPlotContentSize(1);
    chart.getStyler().setShowValue(true);

    int[] xData = [1, 2, 3, 4]
    int[] yData = [1, 2, 3]
    int[][] heatData = new int[xData.length][yData.length]
    Random random = new Random();
    for (int i = 0; i < xData.length; i++) {
      for (int j = 0; j < yData.length; j++) {
        heatData[i][j] = random.nextInt(1000)
      }
    }
    chart.addSeries("Basic HeatMap", xData, yData, heatData)
    File file = new File("build/testRandomHeatmap.png")
    BitmapEncoder.saveBitmap(chart, file.absolutePath, BitmapEncoder.BitmapFormat.PNG)
    assertTrue(file.exists())
  }
}
