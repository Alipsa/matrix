package test.alipsa.matrix.xchart

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.AreaChart
import se.alipsa.matrix.xchart.BarChart
import se.alipsa.matrix.xchart.HeatmapChart
import se.alipsa.matrix.xchart.LineChart
import se.alipsa.matrix.xchart.PieChart
import se.alipsa.matrix.xchart.Plot
import se.alipsa.matrix.xchart.RadarChart

@CompileStatic
class BuilderApiTest {

  private static Matrix numericData() {
    Matrix.builder().data(x: [1, 2, 3, 4], first: [2, 4, 6, 8], second: [3, 6, 9, 12])
        .types(Number, Number, Number).build()
  }

  @Test
  void buildsCommonChartsWithDeferredConfiguration() {
    Matrix data = numericData()
    def line = LineChart.builder(data).title('Line').size(640, 480).x('x').y('first', 'second').build()
    assertEquals('Line', line.title)
    assertEquals(2, line.series.size())

    def bar = BarChart.builder(data).x('x').y('first', 'second').stacked().build()
    assertTrue(bar.style.stacked)

    def area = AreaChart.builder(data).x('x').y('first').build()
    assertEquals(185, area.getSeries('first').fillColor.alpha)
  }

  @Test
  void validatesBuilderMappings() {
    Matrix data = numericData()
    assertThrows(IllegalStateException) { LineChart.builder(data).y('first').build() }
    assertThrows(IllegalArgumentException) { PieChart.builder(data).xAxisTitle('x') }
    assertThrows(IllegalArgumentException) { RadarChart.builder(data).values() }
  }

  @Test
  void supportsHeatmapBuilderOverloadsAndExportFacade() {
    Matrix data = numericData()
    assertNotNull(HeatmapChart.builder(data).values('first', 2).build())
    assertNotNull(HeatmapChart.builder(data).values(['first', 'second']).build())
    ByteArrayOutputStream output = new ByteArrayOutputStream()
    Plot.svg(LineChart.builder(data).x('x').y('first').build(), output)
    assertTrue(output.size() > 0)
  }
}
