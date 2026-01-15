package gg.guide

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GuideAxisStackTest {

  @Test
  void testGuideAxisStackFactory() {
    def stacked = guide_axis_stack(guide_axis(), guide_axis(angle: 45))
    assertEquals('axis_stack', stacked.type)
    assertNotNull(stacked.params.first)
    assertNotNull(stacked.params.additional)
  }

  @Test
  void testGuideAxisStackVarargs() {
    def stacked = guide_axis_stack(guide_axis(), guide_axis(angle: 45), guide_axis(angle: 90))
    assertEquals('axis_stack', stacked.type)
    assertEquals(2, (stacked.params.additional as List).size())
  }

  @Test
  void testGuideAxisStackWithSpacing() {
    def stacked = guide_axis_stack(guide_axis(), [spacing: 10, additional: [guide_axis()]])
    assertEquals('axis_stack', stacked.type)
    assertEquals(10, stacked.params.spacing)
  }

  @Test
  void testGuideAxisStackRendering() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_continuous(guide: guide_axis_stack(guide_axis(), guide_axis(angle: 45)))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'))
    assertTrue(content.contains('<text'))
    assertTrue(content.contains('<line'))
  }

  @Test
  void testGuideAxisStackMultipleLayers() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_continuous(guide: guide_axis_stack(
          guide_axis(),
          guide_axis(angle: 45),
          guide_axis(angle: 90)
        ))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'))
    // Should render successfully with multiple layers
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideAxisStackYAxis() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_y_continuous(guide: guide_axis_stack(guide_axis(), guide_axis(angle: -45)))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="y-axis"'))
  }

  @Test
  void testGuideAxisStackCustomSpacing() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_continuous(guide: guide_axis_stack(
          guide_axis(),
          [additional: [guide_axis(angle: 45)], spacing: 20]
        ))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'))
  }

  @Test
  void testGuideAxisStackWithScatter() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 5], [2, 10], [3, 15]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        scale_x_continuous(guide: guide_axis_stack(guide_axis(), guide_axis(angle: 30)))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'))  // Points
    assertTrue(content.contains('id="x-axis"'))
  }

  @Test
  void testGuideAxisStackWithBar() {
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([['A', 10], ['B', 20], ['C', 15]])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'value')) +
        geom_col() +
        scale_x_discrete(guide: guide_axis_stack(guide_axis(), guide_axis(angle: 45)))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'))
    assertTrue(content.contains('<rect'))  // Bars
  }

  @Test
  void testGuideAxisStackBothAxes() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_continuous(guide: guide_axis_stack(guide_axis(), guide_axis(angle: 45))) +
        scale_y_continuous(guide: guide_axis_stack(guide_axis(), guide_axis(angle: -30)))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'))
    assertTrue(content.contains('id="y-axis"'))
  }

  @Test
  void testGuideAxisStackWithLogTicks() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 10], [10, 100], [100, 1000]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_log10(guide: guide_axis_stack(guide_axis_logticks(), guide_axis()))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'))
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideAxisStackTwoLayers() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 4], [3, 6]])
        .types(Integer, Integer)
        .build()

    // Simple two-layer stack
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_continuous(guide: guide_axis_stack(guide_axis(), guide_axis()))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'))
  }

  @Test
  void testGuideAxisStackWithDifferentAngles() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        scale_x_continuous(guide: guide_axis_stack(
          guide_axis(),
          guide_axis(angle: 30),
          guide_axis(angle: 60)
        ))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'))

    // Check for rotate transforms with multiple patterns to handle different SVG formatting:
    // - "rotate(30," - angle with comma-separated center point
    // - "rotate(30 " - angle with space-separated center point
    // - "rotate(30)" - angle only, no center point
    // This prevents false positives (e.g., matching 130 or 230) while handling format variations
    assertTrue(content.contains('transform="rotate(30,') || content.contains('transform="rotate(30 ') || content.contains('transform="rotate(30)"'))
    assertTrue(content.contains('transform="rotate(60,') || content.contains('transform="rotate(60 ') || content.contains('transform="rotate(60)"'))
  }
}
