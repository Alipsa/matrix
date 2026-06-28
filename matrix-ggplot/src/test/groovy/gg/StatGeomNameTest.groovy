package gg

import static org.junit.jupiter.api.Assertions.assertInstanceOf
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.aes
import static se.alipsa.matrix.gg.GgPlot.ggplot
import static se.alipsa.matrix.gg.GgPlot.stat_summary

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Line
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.geom.GeomLine

class StatGeomNameTest {

  private final Matrix data = Matrix.builder()
      .columnNames(['group', 'value'])
      .rows([
          ['A', 1],
          ['A', 3],
          ['B', 2],
          ['B', 6],
          ['C', 4],
          ['C', 8],
      ])
      .types([String, int])
      .build()

  @Test
  void testStatSummaryLineGeomRenders() {
    Svg svg = (ggplot(data, aes(x: 'group', y: 'value')) +
        stat_summary(geom: 'line', fun: { List<Number> values -> values.average() })).render()

    assertNotNull(svg)
    def lines = svg.descendants().findAll { it instanceof Line }
    assertTrue(lines.size() > 0, 'stat_summary with geom line should render line elements')
  }

  @Test
  void testMixedCaseStatGeomNameResolves() {
    GgChart lower = ggplot(data, aes(x: 'group', y: 'value')) +
        stat_summary(geom: 'line', fun: 'mean')
    GgChart mixed = ggplot(data, aes(x: 'group', y: 'value')) +
        stat_summary(geom: 'Line', fun: 'mean')

    assertInstanceOf(GeomLine, lower.layers.last().geom)
    assertInstanceOf(GeomLine, mixed.layers.last().geom)
  }

  @Test
  void testUnknownStatGeomNameThrowsWithKnownNames() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      ggplot(data, aes(x: 'group', y: 'value')) +
          stat_summary(geom: 'not_a_geom', fun: 'mean')
    }

    assertTrue(exception.message.contains("Unknown geom name: 'not_a_geom'"))
    assertTrue(exception.message.contains('Known names:'))
  }
}
