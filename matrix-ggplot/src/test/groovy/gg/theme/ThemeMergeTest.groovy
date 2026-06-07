package gg.theme

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.*

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

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

  @Test
  void testThemeElementsAcceptNonStringParamKeys() {
    def text = element_text([(new StringBuilder('color')): 'red', (new StringBuilder('size')): 12])
    def line = element_line([(new StringBuilder('color')): 'blue', (new StringBuilder('linewidth')): 2])

    assertEquals('red', text.color)
    assertEquals(12, text.size)
    assertEquals('blue', line.color)
    assertEquals(2, line.size)
  }
}
