package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.geom.GeomFreqpoly
import se.alipsa.matrix.gg.layer.StatType

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GeomFreqpolyTest {

  @Test
  void testDefaults() {
    GeomFreqpoly geom = new GeomFreqpoly()

    assertEquals(StatType.BIN, geom.defaultStat)
    assertEquals(['x'], geom.requiredAes)
    assertEquals('black', geom.color)
    assertEquals(1, geom.size)
  }

  @Test
  void testRenderProducesSvg() {
    def data = Matrix.builder()
        .columnNames('value')
        .rows([
            [1], [2], [3], [4], [5],
            [6], [7], [8], [9], [10]
        ])
        .types(Integer)
        .build()

    Svg svg = (ggplot(data, aes('value')) + geom_freqpoly()).render()
    String content = SvgWriter.toXml(svg)

    assertNotNull(content)
    assertTrue(content.contains('<svg'))
  }
}
