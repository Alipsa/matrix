package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.*

class GgBarColorTest {

  @Test
  void testGrey70FillNormalized() {
    def data = Matrix.builder()
        .data(group: ['A', 'B', 'A'])
        .build()

    def chart = ggplot(data, aes('group')) + geom_bar(fill: 'grey70')

    def svg = chart.render()
    assertNotNull(svg)

    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('fill="#b3b3b3"'), "Expected grey70 to map to #b3b3b3")
  }
}
