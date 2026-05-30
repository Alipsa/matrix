package chart

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.pict.BoxChart
import se.alipsa.matrix.pict.Plot

import javafx.scene.Group

class BoxChartTest {

  @Test
  void testBoxChartWithCategoryAndValueColumn() {
    def data = Matrix.builder().matrixName('BoxData').columns([
        group: ['A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
                'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B'],
        value: [10, 12, 14, 15, 13, 11, 16, 18, 12, 14,
                20, 22, 25, 28, 23, 21, 26, 30, 24, 22]
    ]).types([String, int]).build()

    def chart = BoxChart.create('Test Box Chart', data, 'group', 'value')
    assertNotNull(chart)
    assertEquals(2, chart.categorySeries.size())
    assertEquals(2, chart.valueSeries.size())
    assertEquals('group', chart.xAxisTitle)
    assertEquals('value', chart.yAxisTitle)
  }

  @Test
  void testBoxChartWithColumnNames() {
    def data = Matrix.builder().matrixName('BoxData').columns([
        seriesA: [10, 12, 14, 15, 13, 11, 16, 18, 12, 14],
        seriesB: [20, 22, 25, 28, 23, 21, 26, 30, 24, 22]
    ]).types([int, int]).build()

    def chart = BoxChart.create('Multi Column Box Chart', data, ['seriesA', 'seriesB'])
    assertNotNull(chart)
    assertEquals(2, chart.categorySeries.size())
    assertEquals(2, chart.valueSeries.size())
    assertIterableEquals(['seriesA', 'seriesB'], chart.valueSeriesNames)
  }

  @Test
  void testJfxBoxChartRendering() {
    Assumptions.assumeTrue(
        System.getenv('DISPLAY') != null || 'true' == System.getProperty('headless'),
        'No DISPLAY available; skipping JavaFX test. Run with -Pheadless=true for headless mode.'
    )
    def data = Matrix.builder().matrixName('BoxData').columns([
        group: ['A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
                'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B'],
        value: [10, 12, 14, 15, 13, 11, 16, 18, 12, 14,
                20, 22, 25, 28, 23, 21, 26, 30, 24, 22]
    ]).types([String, int]).build()

    def chart = BoxChart.create('JFX Box Chart', data, 'group', 'value')
    def jfxNode = Plot.jfx(chart)
    assertNotNull(jfxNode)
    assertTrue(jfxNode instanceof Group, "Expected javafx.scene.Group but got ${jfxNode.getClass().name}")
  }

  @Test
  void testBoxChartToPng() {
    def data = Matrix.builder().matrixName('BoxData').columns([
        group: ['A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A', 'A',
                'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B', 'B',
                'C', 'C', 'C', 'C', 'C', 'C', 'C', 'C', 'C', 'C'],
        value: [10, 12, 14, 15, 13, 11, 16, 18, 12, 14,
                20, 22, 25, 28, 23, 21, 26, 30, 24, 22,
                5, 8, 7, 9, 6, 50, 4, 7, 8, 6]
    ]).types([String, int]).build()

    def chart = BoxChart.create('PNG Box Chart', data, 'group', 'value')
    File file = File.createTempFile('BoxChart', '.png')
    try {
      Plot.png(chart, file)
      assertTrue(file.exists(), 'PNG file should exist')
      assertTrue(file.length() > 0, 'PNG file should not be empty')
    } finally {
      file.delete()
    }
  }

}
