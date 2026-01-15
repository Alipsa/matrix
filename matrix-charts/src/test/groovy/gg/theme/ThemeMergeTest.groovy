package gg.theme

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Theme merging regression tests.
 */
class ThemeMergeTest {

  /**
   * Ensure plot title tweaks don't drop default panel backgrounds.
   */
  @Test
  void testPlotTitleThemeKeepsDefaultPanelBackground() {
    Matrix data = Matrix.builder()
        .data([x: [1, 2, 3], y: [2, 4, 6]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        ggtitle('Centered title') +
        theme(plotTitle: element_text(hjust: 0.5))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('fill="#EBEBEB"'),
        'Expected default panel background when only plotTitle is customized')
  }
}
