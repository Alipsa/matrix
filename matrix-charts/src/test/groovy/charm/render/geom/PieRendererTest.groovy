package charm.render.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertEquals
import static se.alipsa.matrix.charm.Charts.plot

import testutil.Slow

@Slow
class PieRendererTest {

  @Test
  void testPieRendersOneSlicePerPositiveDatum() {
    Matrix data = Matrix.builder()
        .columnNames(['category', 'value', 'fill'])
        .rows([
            ['A', 30, '#ff0000'],
            ['B', 20, '#00ff00'],
            ['C', 50, '#0000ff']
        ])
        .build()

    Chart chart = plot(data) {
      mapping(x: 'category', y: 'value', fill: 'fill')
      layer(CharmGeomType.PIE, [:])
      theme {
        legendPosition = 'none'
      }
    }.build()

    assertEquals(3, countClass(chart.render(), 'charm-pie'))
  }

  @Test
  void testPieReturnsNoSlicesForEmptyOrNonPositiveInput() {
    Matrix empty = Matrix.builder()
        .columnNames(['category', 'value'])
        .rows([])
        .build()
    Chart emptyChart = plot(empty) {
      mapping(x: 'category', y: 'value')
      layer(CharmGeomType.PIE, [:])
    }.build()
    assertEquals(0, countClass(emptyChart.render(), 'charm-pie'))

    Matrix nonPositive = Matrix.builder()
        .columnNames(['category', 'value'])
        .rows([
            ['A', 0],
            ['B', -10],
            ['C', 0]
        ])
        .build()
    Chart nonPositiveChart = plot(nonPositive) {
      mapping(x: 'category', y: 'value')
      layer(CharmGeomType.PIE, [:])
    }.build()
    assertEquals(0, countClass(nonPositiveChart.render(), 'charm-pie'))
  }

  private static int countClass(Svg svg, String className) {
    String xml = SvgWriter.toXml(svg)
    String token = "class=\"${className}\""
    int idx = 0
    int count = 0
    while (true) {
      idx = xml.indexOf(token, idx)
      if (idx < 0) {
        break
      }
      count++
      idx += token.length()
    }
    count
  }
}
