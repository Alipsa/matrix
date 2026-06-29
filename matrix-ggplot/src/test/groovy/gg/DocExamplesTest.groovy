package gg

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.*

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Line
import se.alipsa.groovy.svg.Path
import se.alipsa.groovy.svg.Rect
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Text
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart

class DocExamplesTest {

  @Test
  void testCookbookSegmentChartWithAesXendYend() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'xend', 'yend'])
        .rows([[1, 1, 2, 3], [2, 2, 3, 4], [3, 3, 4, 2]])
        .build()

    Svg svg = (ggplot(data, aes(x: 'x', y: 'y', xend: 'xend', yend: 'yend')) +
        geom_segment(linewidth: 1.5) +
        labs(title: 'Segment endpoints')).render()

    assertNotNull(svg)
    assertTrue(svg.descendants().findAll { it instanceof Line }.size() > 0)
  }

  @Test
  void testCookbookErrorBarsWithAesYminYmax() {
    def data = Matrix.builder()
        .columnNames(['group', 'mean', 'lower', 'upper'])
        .rows([['A', 10, 8, 12], ['B', 14, 11, 17], ['C', 9, 7, 11]])
        .build()

    Svg svg = (ggplot(data, aes(x: 'group', y: 'mean', ymin: 'lower', ymax: 'upper')) +
        geom_errorbar(width: 0.25) +
        geom_point(size: 3) +
        labs(title: 'Interval estimate')).render()

    assertNotNull(svg)
    assertTrue(svg.descendants().findAll { it instanceof Line }.size() > 0)
  }

  @Test
  void testCookbookSeparateLegendTitlesPerAesthetic() {
    GgChart chart = separateLegendTitleChart()
    Svg svg = chart.render()

    assertNotNull(svg)
    assertTrue(svg.descendants().findAll { it instanceof Rect }.size() > 0)
    assertEquals('Source', chart.labels.legendTitles['color'])
    assertEquals('Kind', chart.labels.legendTitles['fill'])
    String text = svgText(svg)
    assertTrue(text.contains('Source'), text)
    assertTrue(text.contains('Kind'), text)
  }

  @Test
  void testCookbookStatSummaryWithNamedGeom() {
    def data = Matrix.builder()
        .columnNames(['group', 'value'])
        .rows([
            ['A', 1], ['A', 3],
            ['B', 2], ['B', 6],
            ['C', 4], ['C', 8]
        ])
        .build()

    Svg svg = (ggplot(data, aes(x: 'group', y: 'value')) +
        stat_summary(geom: 'line', fun: { List<Number> values -> values.average() }) +
        geom_point(alpha: 0.6)).render()

    assertNotNull(svg)
    assertTrue(svg.descendants().findAll { it instanceof Line }.size() > 0)
  }

  @Test
  void testTutorialPositionalRangeAesthetics() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'xend', 'yend', 'lower', 'upper'])
        .rows([
            [1, 10, 2, 13, 8, 12],
            [2, 14, 3, 16, 11, 17],
            [3, 9, 4, 11, 7, 11]
        ])
        .build()

    Svg svg = (ggplot(data, aes(x: 'x', y: 'y')) +
        geom_segment(mapping: aes(xend: 'xend', yend: 'yend'), linewidth: 1.2) +
        geom_errorbar(mapping: aes(ymin: 'lower', ymax: 'upper'), width: 0.2) +
        geom_point(size: 3)).render()

    assertNotNull(svg)
    assertTrue(svg.descendants().findAll { it instanceof Line }.size() > 0)
  }

  @Test
  void testTutorialLabelsAndLegendTitles() {
    GgChart chart = separateLegendTitleChart()
    Svg svg = chart.render()

    assertNotNull(svg)
    assertEquals('Source', chart.labels.legendTitles['color'])
    assertEquals('Kind', chart.labels.legendTitles['fill'])
    String text = svgText(svg)
    assertTrue(text.contains('Grouped results'), text)
    assertTrue(text.contains('Source'), text)
    assertTrue(text.contains('Kind'), text)
  }

  @Test
  void testReadmeRibbonRangeAesthetics() {
    def forecast = Matrix.builder()
        .columnNames(['week', 'value', 'low', 'high'])
        .rows([[1, 12, 10, 14], [2, 16, 13, 19], [3, 15, 12, 18], [4, 20, 17, 23]])
        .build()

    Svg svg = (ggplot(forecast, aes(x: 'week', y: 'value', ymin: 'low', ymax: 'high')) +
        geom_ribbon(fill: '#9ecae1', alpha: 0.45) +
        geom_line()).render()

    assertNotNull(svg)
    assertTrue(svg.descendants().findAll { it instanceof Path }.size() > 0)
  }

  private static GgChart separateLegendTitleChart() {
    def data = Matrix.builder()
        .columnNames(['category', 'value', 'kind', 'source'])
        .rows([
            ['A', 10, 'baseline', 'observed'],
            ['B', 14, 'target', 'model'],
            ['C', 9, 'baseline', 'observed']
        ])
        .build()

    ggplot(data, aes(x: 'category', y: 'value')) +
        geom_col(aes(fill: 'kind')) +
        geom_point(mapping: aes(color: 'source'), size: 4) +
        labs(
            title: 'Grouped results',
            x: 'Category',
            y: 'Value',
            color: 'Source',
            fill: 'Kind'
        )
  }

  private static String svgText(Svg svg) {
    svg.descendants()
        .findAll { it instanceof Text }
        .collect { (it as Text).content }
        .join(' ')
  }
}
