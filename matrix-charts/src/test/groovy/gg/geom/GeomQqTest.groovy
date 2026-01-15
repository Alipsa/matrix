package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.geom.GeomQq
import se.alipsa.matrix.gg.geom.GeomQqLine
import se.alipsa.matrix.gg.layer.StatType

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static se.alipsa.matrix.gg.GgPlot.*

class GeomQqTest {

  @Test
  void testGeomQqDefaults() {
    GeomQq geom = new GeomQq()

    assertEquals(StatType.QQ, geom.defaultStat)
    assertEquals(['x'], geom.requiredAes)
  }

  @Test
  void testGeomQqLineDefaults() {
    GeomQqLine geom = new GeomQqLine()

    assertEquals(StatType.QQ_LINE, geom.defaultStat)
    assertEquals(['x'], geom.requiredAes)
  }

  @Test
  void testQqRender() {
    def data = Matrix.builder()
        .columnNames('value')
        .rows([[1], [2], [3], [4]])
        .types(Integer)
        .build()

    Svg svg = (ggplot(data, aes('value')) + geom_qq() + geom_qq_line()).render()

    assertNotNull(svg)
  }
}
