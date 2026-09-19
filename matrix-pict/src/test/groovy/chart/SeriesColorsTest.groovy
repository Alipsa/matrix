package chart

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.pict.Histogram
import se.alipsa.matrix.pict.LineChart
import se.alipsa.matrix.pict.Plot

import java.awt.Color

/** Tests per-series colour overrides and their default-palette fallback. */
class SeriesColorsTest {

  private static Matrix data() {
    Matrix.builder().columns([x: [1, 2], north: [2, 3], south: [3, 2]]).types([Integer, Integer, Integer]).build()
  }

  @Test
  void mapOverridesAndPartialMapKeepsDefault() {
    LineChart chart = LineChart.builder(data()).x('x').y('north', 'south').seriesColors([north: Color.RED]).build()
    List<String> strokes = classes(Plot.svg(chart), 'charm-line')*.getAttribute('stroke')*.toString()*.toLowerCase()
    assertTrue(strokes.contains('#ff0000'))
    assertTrue(strokes.contains('#d62728'))
  }

  @Test
  void histogramHasStableSeriesNameAndUsesLiteralColour() {
    Histogram chart = Histogram.builder(data()).x('north').seriesColors(Color.BLUE).build()
    assertEquals(['north'], chart.valueSeriesNames)
    assertEquals(['#0000ff'], classes(Plot.svg(chart), 'charm-histogram')*.getAttribute('fill')*.toString().unique())
    assertEquals([Histogram.DEFAULT_SERIES_NAME], Histogram.create([1, 2, 3]).valueSeriesNames)
  }

  private static List classes(Svg svg, String cssClass) {
    svg.descendants().findAll { it.getAttribute('class')?.toString()?.split(' ')?.contains(cssClass) }
  }
}
