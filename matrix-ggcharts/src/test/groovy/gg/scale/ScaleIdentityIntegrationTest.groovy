package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import testutil.Slow

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Integration (render) tests for all Scale*Identity types.
 * Consolidates the testWithChart() method from each individual identity scale file.
 * All tests are @Slow (full chart render pipeline).
 */
@Slow
class ScaleIdentityIntegrationTest {

  @Test
  void testAlphaChartRenders() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'transparency'])
        .rows([[1, 2, 0.3], [2, 4, 0.7], [3, 6, 1.0]])
        .build()

    def svg = (ggplot(data, aes(x: 'x', y: 'y', alpha: 'transparency')) +
        geom_point() +
        scale_alpha_identity()).render()

    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<svg'))
  }

  @Test
  void testColorChartRenders() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'color'])
        .rows([[1, 2, 'red'], [2, 4, 'blue'], [3, 6, 'green']])
        .build()

    def svg = (ggplot(data, aes(x: 'x', y: 'y', color: 'color')) +
        geom_point() +
        scale_color_identity()).render()

    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<svg'))
  }

  @Test
  void testColourAliasChartRenders() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'colour'])
        .rows([[1, 2, 'red'], [2, 4, 'blue']])
        .build()

    def svg = (ggplot(data, aes(x: 'x', y: 'y', color: 'colour')) +
        geom_point() +
        scale_colour_identity()).render()

    assertNotNull(svg)
  }

  @Test
  void testFillChartRenders() {
    def data = Matrix.builder()
        .columnNames(['category', 'value', 'fillColor'])
        .rows([['A', 10, 'red'], ['B', 20, 'blue'], ['C', 15, 'green']])
        .build()

    def svg = (ggplot(data, aes(x: 'category', y: 'value', fill: 'fillColor')) +
        geom_col() +
        scale_fill_identity()).render()

    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<svg'))
  }

  @Test
  void testLinetypeChartRenders() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'lineStyle'])
        .rows([[1, 2, 'solid'], [2, 4, 'dashed'], [3, 6, 'dotted']])
        .build()

    def svg = (ggplot(data, aes(x: 'x', y: 'y', linetype: 'lineStyle')) +
        geom_line() +
        scale_linetype_identity()).render()

    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<svg'))
  }

  @Test
  void testShapeChartRenders() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'shape'])
        .rows([[1, 2, 'circle'], [2, 4, 'square'], [3, 6, 'triangle']])
        .build()

    def svg = (ggplot(data, aes(x: 'x', y: 'y', shape: 'shape')) +
        geom_point() +
        scale_shape_identity()).render()

    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<svg'))
  }

  @Test
  void testSizeChartRenders() {
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'pointSize'])
        .rows([[1, 2, 3], [2, 4, 5], [3, 6, 8]])
        .build()

    def svg = (ggplot(data, aes(x: 'x', y: 'y', size: 'pointSize')) +
        geom_point() +
        scale_size_identity()).render()

    assertNotNull(svg)
    assertTrue(SvgWriter.toXml(svg).contains('<svg'))
  }
}
