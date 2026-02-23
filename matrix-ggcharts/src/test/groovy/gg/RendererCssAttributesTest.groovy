package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for CSS attributes in renderer components (axes, grid, legend).
 */
class RendererCssAttributesTest {

  @Test
  void testAxisRendererCssClasses() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    // Axis components should have CSS classes (independent of css_attributes config)
    assertTrue(svgString.contains('gg-axis-line') || svgString.contains('charm-axis-line'),
        "SVG should contain axis line CSS class")
    assertTrue(svgString.contains('gg-axis-tick') || svgString.contains('charm-axis-tick'),
        "SVG should contain axis tick CSS class")
    assertTrue(svgString.contains('gg-axis-label') || svgString.contains('charm-axis-label'),
        "SVG should contain axis label CSS class")
  }

  @Test
  void testGridRendererCssClasses() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3], [3, 4]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        theme_minimal()  // theme_minimal shows grid lines

    Svg svg = chart.render()
    String svgString = svg.toXml()

    // Grid lines should have CSS classes (charm-grid for delegated charts, gg-grid-major for legacy)
    assertTrue(svgString.contains('gg-grid-major') ||
               svgString.contains('gg-grid-minor') ||
               svgString.contains('charm-grid'),
        "SVG should contain grid CSS classes")
  }

  @Test
  void testLegendRendererCssClasses() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
          [1, 2, 'A'],
          [2, 3, 'B'],
          [3, 4, 'C']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'group')) +
        geom_point()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    // Legend components should have CSS classes
    assertTrue(svgString.contains('gg-legend-'),
        "SVG should contain legend CSS classes")
  }

  @Test
  void testLegendKeysCssClass() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
          [1, 2, 'A'],
          [2, 3, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'group')) +
        geom_point()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertTrue(svgString.contains('gg-legend-key'),
        "SVG should contain gg-legend-key CSS class for legend symbols")
  }

  @Test
  void testLegendLabelsCssClass() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
          [1, 2, 'A'],
          [2, 3, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'group')) +
        geom_point()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertTrue(svgString.contains('gg-legend-label'),
        "SVG should contain gg-legend-label CSS class for legend labels")
  }

  @Test
  void testContinuousLegendColorbar() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
          [1, 2, 10],
          [2, 3, 20],
          [3, 4, 30]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    assertTrue(svgString.contains('gg-legend-colorbar'),
        "SVG should contain gg-legend-colorbar CSS class for continuous legends")
  }

  @Test
  void testAxisRendererInFlippedCoordinates() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 2], [2, 3]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        coord_flip()

    Svg svg = chart.render()
    String svgString = svg.toXml()

    // Even with flipped coordinates, axis CSS classes should be present
    assertTrue(svgString.contains('gg-axis-line') || svgString.contains('charm-axis-line'),
        "Flipped chart should contain axis line CSS class")
    assertTrue(svgString.contains('gg-axis-tick') || svgString.contains('charm-axis-tick'),
        "Flipped chart should contain axis tick CSS class")
  }
}
