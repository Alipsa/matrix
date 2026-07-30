package test.alipsa.matrix.xchart

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.xchart.AreaChart
import se.alipsa.matrix.xchart.BarChart
import se.alipsa.matrix.xchart.BoxChart
import se.alipsa.matrix.xchart.BubbleChart
import se.alipsa.matrix.xchart.CorrelationHeatmapChart
import se.alipsa.matrix.xchart.HeatmapChart
import se.alipsa.matrix.xchart.HistogramChart
import se.alipsa.matrix.xchart.LineChart
import se.alipsa.matrix.xchart.OhlcChart
import se.alipsa.matrix.xchart.PieChart
import se.alipsa.matrix.xchart.Plot
import se.alipsa.matrix.xchart.RadarChart
import se.alipsa.matrix.xchart.ScatterChart
import se.alipsa.matrix.xchart.StickChart

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
    assertEquals(640, line.getXChart().width)
    assertEquals(480, line.getXChart().height)

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
    assertEquals(1, HeatmapChart.builder(data).values('first', 2).build().series.size())
    assertEquals(1, HeatmapChart.builder(data).values(['first', 'second']).build().series.size())
    ByteArrayOutputStream output = new ByteArrayOutputStream()
    def chart = LineChart.builder(data).x('x').y('first').build()
    Plot.svg(chart, output)
    assertTrue(output.size() > 0)
    ByteArrayOutputStream png = new ByteArrayOutputStream()
    ByteArrayOutputStream pdf = new ByteArrayOutputStream()
    Plot.png(chart, png)
    Plot.pdf(chart, pdf)
    assertTrue(png.size() > 0)
    assertTrue(pdf.size() > 0)
  }

  @Test
  void buildsRemainingChartTypes() {
    Matrix data = numericData()
    assertEquals(1, ScatterChart.builder(data).x('x').y('first').build().series.size())
    assertEquals(1, StickChart.builder(data).x('x').y('first').build().series.size())
    assertEquals(2, BoxChart.builder(data).y('first', 'second').build().series.size())
    assertEquals(1, BubbleChart.builder(data).x('x').y('first').size('second').build().series.size())
    assertEquals(1, HistogramChart.builder(data).x('first').buckets(2).build().series.size())
    def pie = PieChart.builder(data).x('x').y('first').donut().build()
    assertEquals(4, pie.series.size())
    assertEquals(org.knowm.xchart.PieSeries.PieSeriesRenderStyle.Donut, pie.style.defaultSeriesRenderStyle)
    assertEquals(1, CorrelationHeatmapChart.builder(data).seriesName('Correlation').columns('first', 'second').build().series.size())
  }

  @Test
  void supportsRadarAndOhlcMappings() {
    Matrix radarData = Matrix.builder().data(name: ['A', 'B'], speed: [0.2, 0.4], power: [0.3, 0.6])
        .types(String, Number, Number).build()
    List<String> radii = ['speed', 'power']
    assertNotNull(RadarChart.builder(radarData).label('name').values(*radii).build())

    Date today = new Date()
    Date tomorrow = new Date(today.time + 86_400_000L)
    Matrix ohlcData = Matrix.builder().data(date: [today, tomorrow], open: [1, 2], high: [2, 3], low: [0, 1], close: [1, 2])
        .types(Date, Number, Number, Number, Number).build()
    assertNotNull(OhlcChart.builder(ohlcData).date('date').open('open').high('high').low('low').close('close').build())
  }
}
