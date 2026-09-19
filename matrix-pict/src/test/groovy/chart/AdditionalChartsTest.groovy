package chart

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.pict.CorrelationHeatmapChart
import se.alipsa.matrix.pict.HeatmapChart
import se.alipsa.matrix.pict.Plot
import se.alipsa.matrix.pict.RadarChart
import se.alipsa.matrix.stats.Correlation

import java.awt.Color

/** Tests the public models and Charm rendering paths for the additional pict charts. */
class AdditionalChartsTest {

  private static Matrix measures() {
    Matrix.builder().columns([
        name: ['a', 'b', 'c'], a: [1, 2, 3], b: [3, 2, 1], c: [2, 4, 6]
    ]).types([String, Integer, Integer, Integer]).build()
  }

  @Test
  void heatmapBuildsColumnMajorValuesAndRendersTiles() {
    HeatmapChart chart = HeatmapChart.builder(measures()).rowLabels('name').columns('a', 'b').build()
    assertTrue(chart.values[0] == [1, 2, 3])
    assertEquals(['a', 'b'], chart.columnLabels)
    Svg svg = Plot.svg(chart)
    assertEquals(6, elementsWithClass(svg, 'charm-tile').size())
    assertEquals(6, elementsWithClass(svg, 'charm-text').size())
  }

  @Test
  void heatmapTreatsNonFiniteValuesAsNa() {
    Matrix data = Matrix.builder().columns([v: [1.0d, Double.NaN, 3.0d]]).types([Double]).build()
    HeatmapChart chart = HeatmapChart.builder(data).columns('v').build()
    assertNull(chart.values[0][1])
    List<String> fills = elementsWithClass(Plot.svg(chart), 'charm-tile')*.getAttribute('fill')*.toString()
    assertTrue(fills.contains('#999999'))
  }

  @Test
  void correlationUsesFixedDivergingDomain() {
    CorrelationHeatmapChart chart = CorrelationHeatmapChart.builder(measures()).columns('a', 'b').method(Correlation.PEARSON).build()
    assertTrue(chart.values[0] == [1.00, -1.00])
    assertEquals([-1.00, 1.00], chart.fillLimits)
    assertEquals(4, elementsWithClass(Plot.svg(chart), 'charm-tile').size())
  }

  @Test
  void radarNormalizesAndRendersOnePolygonPerRow() {
    RadarChart chart = RadarChart.builder(measures()).label('name').values('a', 'b', 'c')
        .normalize(true).seriesColors([a: Color.RED, b: Color.BLUE]).build()
    assertTrue(chart.seriesValues[0] == [0.0, 1.0, 0.0])
    Svg svg = Plot.svg(chart)
    assertEquals(3, elementsWithClass(svg, 'charm-polygon').size())
    assertEquals(3, elementsWithClass(svg, 'charm-segment').size())
    assertEquals(RadarChart.DEFAULT_RINGS, elementsWithClass(svg, 'charm-path').size())
  }

  @Test
  void radarRejectsIncompleteColumnsAndOutOfRangeScale() {
    Matrix incomplete = Matrix.builder().columns([n: ['x'], a: [1], b: [null], c: [3]])
        .types([String, Integer, Integer, Integer]).build()
    IllegalArgumentException values = assertThrows(IllegalArgumentException) {
      RadarChart.builder(incomplete).label('n').values('a', 'b', 'c').build()
    }
    assertTrue(values.message.contains('complete numeric'))
    IllegalArgumentException scale = assertThrows(IllegalArgumentException) {
      RadarChart.builder(measures()).label('name').values('a', 'b', 'c').yAxisScale(0, 2, 1).build()
    }
    assertTrue(scale.message.contains('exceeds'))
  }

  private static List elementsWithClass(Svg svg, String cssClass) {
    svg.descendants().findAll { it.getAttribute('class')?.toString()?.split(' ')?.contains(cssClass) }
  }
}
