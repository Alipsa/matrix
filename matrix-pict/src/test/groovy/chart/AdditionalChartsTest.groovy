package chart

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.pict.CharmBridge
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
  void heatmapFormatsValuesAndUsesAnExplicitLabelColour() {
    Matrix data = Matrix.builder().columns([v: [0.30000000000000004d, 1.123456789d]]).types([Double]).build()
    HeatmapChart chart = HeatmapChart.builder(data).columns('v').valueDecimals(2).labelColor(Color.BLACK).build()

    List labels = elementsWithClass(Plot.svg(chart), 'charm-text')
    assertEquals(['0.30', '1.12'], labels*.content)
    assertTrue(labels.every { it.getAttribute('fill') == '#000000' })
  }

  @Test
  void heatmapDefaultsToNaturalValueScaleAndContrastLabels() {
    Matrix data = Matrix.builder().columns([v: [3, 4]]).types([Integer]).build()
    HeatmapChart chart = HeatmapChart.builder(data).columns('v').build()

    List labels = elementsWithClass(Plot.svg(chart), 'charm-text')
    assertEquals(['3', '4'], labels*.content)
    assertTrue(labels.every { it.getAttribute('fill') in ['#000000', '#ffffff'] })
    assertTrue(labels*.getAttribute('fill').contains('#ffffff'))
  }

  @Test
  void heatmapUsesAUsefulDefaultLegendTitle() {
    HeatmapChart singleColumn = HeatmapChart.builder(measures()).columns('a').build()
    HeatmapChart multipleColumns = HeatmapChart.builder(measures()).columns('a', 'b').build()

    assertEquals('a', CharmBridge.convert(singleColumn).labels.guides['fill'])
    assertEquals('value', CharmBridge.convert(multipleColumns).labels.guides['fill'])
  }

  @Test
  void heatmapRejectsUnsupportedSeriesColours() {
    assertThrows(IllegalArgumentException) {
      HeatmapChart.builder(measures()).seriesColors(Color.RED)
    }
    assertThrows(IllegalArgumentException) {
      HeatmapChart.builder(measures()).seriesColors([Color.RED])
    }
    assertThrows(IllegalArgumentException) {
      HeatmapChart.builder(measures()).seriesColors([a: Color.RED])
    }
  }

  @Test
  void correlationUsesFixedDivergingDomain() {
    CorrelationHeatmapChart chart = CorrelationHeatmapChart.builder(measures()).columns('a', 'b').method(Correlation.PEARSON).build()
    assertTrue(chart.values[0] == [1.00, -1.00])
    assertEquals([-1.00, 1.00], chart.fillLimits)
    assertEquals(2, chart.valueDecimals)
    assertEquals(4, elementsWithClass(Plot.svg(chart), 'charm-tile').size())
  }

  @Test
  void correlationTwoColourGradientDoesNotRestoreTheDefaultMidpoint() {
    CorrelationHeatmapChart chart = CorrelationHeatmapChart.builder(measures())
        .columns('a', 'b').colors(Color.RED, Color.BLUE).build()

    assertEquals(Color.RED, chart.lowColor)
    assertNull(chart.midColor)
    assertEquals(Color.BLUE, chart.highColor)
    assertNull(chart.midpoint)
  }

  @Test
  void correlationLeavesUndefinedValuesAsNa() {
    Matrix data = Matrix.builder().columns([constant: [1, 1, 1], changing: [1, 2, 3]])
        .types([Integer, Integer]).build()
    CorrelationHeatmapChart chart = CorrelationHeatmapChart.builder(data).columns('constant', 'changing').build()

    assertNull(chart.values[0][0])
    assertNull(chart.values[0][1])
    assertNull(chart.values[1][0])
    assertEquals(1.00, chart.values[1][1])
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
  void radarRendersDuplicateSeriesLabelsWithoutMutatingTheChart() {
    Matrix data = Matrix.builder().columns([
        name: ['a', 'a', 'c'], x: [1, 2, 3], y: [2, 3, 1], z: [3, 1, 2]
    ]).types([String, Integer, Integer, Integer]).build()
    RadarChart chart = RadarChart.builder(data).label('name').values('x', 'y', 'z')
        .seriesColors([a: Color.RED]).build()

    List firstFills = elementsWithClass(Plot.svg(chart), 'charm-polygon')*.getAttribute('fill')*.toString()
    List secondFills = elementsWithClass(Plot.svg(chart), 'charm-polygon')*.getAttribute('fill')*.toString()
    assertEquals(['a', 'a', 'c'], chart.seriesLabels)
    assertEquals(firstFills, secondFills)
    assertFalse(secondFills.contains('#999999'))
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

  @Test
  void radarFillAlphaKeepsPolygonOutlineVisible() {
    RadarChart chart = RadarChart.builder(measures()).label('name').values('a', 'b', 'c').fillAlpha(0).build()
    List polygons = elementsWithClass(Plot.svg(chart), 'charm-polygon')

    assertEquals(3, polygons.size())
    assertEquals(['0'], polygons*.getAttribute('fill-opacity')*.toString().unique())
    assertTrue(polygons.every { it.getAttribute('opacity') == null })
    assertTrue(polygons.every { it.getAttribute('stroke')?.toString()?.startsWith('#') })
  }

  private static List elementsWithClass(Svg svg, String cssClass) {
    svg.descendants().findAll { it.getAttribute('class')?.toString()?.split(' ')?.contains(cssClass) }
  }
}
