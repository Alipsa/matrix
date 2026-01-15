package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for GeomPolygon - filled polygon geometry.
 */
class GeomPolygonTest {

  @Test
  void testBasicTriangle() {
    // Create a simple triangle
    def data = Matrix.builder()
        .data([
            x: [0, 1, 0.5],
            y: [0, 0, 1]
        ])
        .types(BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_polygon()

    Svg svg = chart.render()
    assertNotNull(svg, 'Triangle polygon should render')
  }

  @Test
  void testPolygonWithFillColor() {
    def data = Matrix.builder()
        .data([
            x: [0, 1, 1, 0],
            y: [0, 0, 1, 1]
        ])
        .types(BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_polygon(fill: 'blue', color: 'red', linewidth: 2)

    Svg svg = chart.render()
    assertNotNull(svg, 'Styled polygon should render')
  }

  @Test
  void testMultiplePolygonsByGroup() {
    def data = Matrix.builder()
        .data([
            x: [0, 1, 0.5, 2, 3, 2.5],
            y: [0, 0, 1, 0, 0, 1],
            group: ['A', 'A', 'A', 'B', 'B', 'B']
        ])
        .types(BigDecimal, BigDecimal, String)
        .build()

    def chart = ggplot(data, aes('x', 'y', group: 'group', fill: 'group')) +
        geom_polygon(alpha: 0.5)

    Svg svg = chart.render()
    assertNotNull(svg, 'Multiple grouped polygons should render')
  }

  @Test
  void testPentagon() {
    // Create a pentagon
    def angles = (0..4).collect { i -> (i * 2 * Math.PI / 5) - Math.PI / 2 }
    def xs = angles.collect { Math.cos(it) }
    def ys = angles.collect { Math.sin(it) }

    def data = Matrix.builder()
        .data([x: xs, y: ys])
        .types(BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_polygon(fill: 'lightblue', color: 'navy')

    Svg svg = chart.render()
    assertNotNull(svg, 'Pentagon should render')
  }

  @Test
  void testPolygonWithAlpha() {
    def data = Matrix.builder()
        .data([
            x: [0, 1, 1, 0],
            y: [0, 0, 1, 1]
        ])
        .types(BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_polygon(fill: 'red', alpha: 0.3)

    Svg svg = chart.render()
    assertNotNull(svg, 'Polygon with alpha should render')
  }

  @Test
  void testInsufficientPoints() {
    // Polygon needs at least 3 points
    def data = Matrix.builder()
        .data([
            x: [0, 1],
            y: [0, 0]
        ])
        .types(BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_polygon()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should render without error even with insufficient points')
  }

  @Test
  void testPolygonWithDashedBorder() {
    def data = Matrix.builder()
        .data([
            x: [0, 1, 1, 0],
            y: [0, 0, 1, 1]
        ])
        .types(BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_polygon(fill: 'yellow', color: 'black', linetype: 'dashed')

    Svg svg = chart.render()
    assertNotNull(svg, 'Polygon with dashed border should render')
  }
}
