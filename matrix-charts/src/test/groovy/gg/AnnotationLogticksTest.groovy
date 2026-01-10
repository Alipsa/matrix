package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.layer.Layer

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Test annotation_logticks() functionality.
 */
class AnnotationLogticksTest {

  @Test
  void testLogticksOnXAxis() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [10, 2], [100, 3], [1000, 4]])
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        scale_x_log10() +
        annotation_logticks(sides: 'b')

    assertNotNull(chart)
    assertEquals(2, chart.layers.size())

    // Verify the logticks layer
    def logticksLayer = chart.layers[1]
    assertNotNull(logticksLayer)
    assertNotNull(logticksLayer.geom)
    assertEquals('GeomLogticks', logticksLayer.geom.class.simpleName)
    assertFalse(logticksLayer.inheritAes)

    // Render to ensure no errors
    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<svg'))
    assertTrue(svgContent.contains('</svg>'))
  }

  @Test
  void testLogticksOnYAxis() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 10], [3, 100], [4, 1000]])
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        scale_y_log10() +
        annotation_logticks(sides: 'l')

    assertNotNull(chart)
    assertEquals(2, chart.layers.size())

    // Render to ensure no errors
    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<svg'))
  }

  @Test
  void testLogticksBothAxes() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [10, 10], [100, 100]])
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        scale_x_log10() +
        scale_y_log10() +
        annotation_logticks(sides: 'bl')

    assertNotNull(chart)
    assertEquals(2, chart.layers.size())

    // Render to ensure no errors
    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<line'))
  }

  @Test
  void testLogticksDifferentSides() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [10, 10], [100, 100]])
        .build()

    // Test top side
    def chartTop = ggplot(data, aes('x', 'y')) +
        geom_point() +
        scale_x_log10() +
        annotation_logticks(sides: 't')

    Svg svgTop = chartTop.render()
    assertNotNull(svgTop)

    // Test right side
    def chartRight = ggplot(data, aes('x', 'y')) +
        geom_point() +
        scale_y_log10() +
        annotation_logticks(sides: 'r')

    Svg svgRight = chartRight.render()
    assertNotNull(svgRight)

    // Test all sides
    def chartAll = ggplot(data, aes('x', 'y')) +
        geom_point() +
        scale_x_log10() +
        scale_y_log10() +
        annotation_logticks(sides: 'trbl')

    Svg svgAll = chartAll.render()
    assertNotNull(svgAll)
    String svgContent = SvgWriter.toXml(svgAll)
    assertTrue(svgContent.contains('<line'))
  }

  @Test
  void testLogticksCustomLengths() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [10, 2], [100, 3], [1000, 4]])
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        scale_x_log10() +
        annotation_logticks(
            sides: 'b',
            short: 2.0,
            mid: 4.0,
            long: 6.0
        )

    assertNotNull(chart)

    // Render to ensure custom lengths are accepted
    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testLogticksNoLogScale() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [3, 3], [4, 4]])
        .build()

    // Without log scales, logticks should render but produce no ticks
    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        annotation_logticks(sides: 'bl')

    assertNotNull(chart)

    // Render should complete without errors
    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testLogticksCustomColor() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [10, 2], [100, 3]])
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        scale_x_log10() +
        annotation_logticks(
            sides: 'b',
            colour: 'red',
            linewidth: 1.5,
            alpha: 0.7
        )

    assertNotNull(chart)

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<line'))
  }

  @Test
  void testLogticksLayer() {
    Layer layer = annotation_logticks(sides: 'b', base: 10)

    assertNotNull(layer)
    assertNotNull(layer.geom)
    assertEquals('GeomLogticks', layer.geom.class.simpleName)
    assertNull(layer.data)
    assertNull(layer.aes)
    assertFalse(layer.inheritAes)
  }

  @Test
  void testLogticksBase2() {
    // Test base-2 logarithmic scale
    // Base-2 should only generate major ticks (no intermediate/minor ticks)
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 2], [4, 3], [8, 4], [16, 5]])
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        scale_x_log10() +  // Note: Using log10 scale for now
        annotation_logticks(sides: 'b', base: 2)

    assertNotNull(chart)
    assertEquals(2, chart.layers.size())

    // Render to ensure base-2 works without errors
    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<line'))
  }

  @Test
  void testLogticksBase3() {
    // Test base-3 logarithmic scale
    // Base-3 should only generate major ticks (no intermediate/minor ticks)
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [3, 2], [9, 3], [27, 4]])
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        scale_x_log10() +
        annotation_logticks(sides: 'b', base: 3)

    assertNotNull(chart)

    // Render to ensure base-3 works without errors
    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<line'))
  }

  @Test
  void testLogticksBase5() {
    // Test base-5 logarithmic scale
    // Base-5 should generate major, intermediate (at 2), and minor ticks (at 3, 4)
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [5, 2], [25, 3], [125, 4]])
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        scale_x_log10() +
        annotation_logticks(sides: 'b', base: 5)

    assertNotNull(chart)

    // Render to ensure base-5 works with intermediate/minor ticks
    Svg svg = chart.render()
    assertNotNull(svg)
    String svgContent = SvgWriter.toXml(svg)
    assertTrue(svgContent.contains('<line'))
  }
}
