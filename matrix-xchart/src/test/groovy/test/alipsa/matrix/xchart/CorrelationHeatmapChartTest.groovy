package test.alipsa.matrix.xchart

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.CorrelationHeatmapChart

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

class CorrelationHeatmapChartTest {

  @Test
  void testCorrelationHeatmap() {
    Matrix whisky = Matrix.builder().data(this.class.getResource('/ScotchWhisky01.csv')).build()
    assertNotNull(whisky, "Failed to load csv file")
    File chartFile = new File("build/correlationHeatmap.svg")
    def chart = CorrelationHeatmapChart.create(whisky, 800, 600)
        .setTitle("Correlation Heatmap of Scotch Whisky Data")
        .addSeries("Correlation", whisky.columnNames() - 'Distillery' as List<String>)
    chart.exportSvg(chartFile)
    assertTrue(chartFile.exists(), "SVG file should be created")
  }
}
