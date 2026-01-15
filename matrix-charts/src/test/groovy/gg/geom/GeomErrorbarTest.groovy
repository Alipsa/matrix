package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.*

class GeomErrorbarTest {

  @Test
  void testStatSummaryErrorbarRender() {
    def data = Matrix.builder()
        .columnNames(['group', 'value'])
        .rows([
            ['A', 1],
            ['A', 2],
            ['A', 3],
            ['B', 2],
            ['B', 4],
            ['B', 6]
        ])
        .build()

    def chart = ggplot(data, aes('group', 'value')) +
        stat_summary('fun.data': 'mean_cl_normal', geom: 'errorbar', width: 0.4)

    def svg = chart.render()
    assertNotNull(svg)

    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<line'), "Should render error bar line segments")
  }
}
