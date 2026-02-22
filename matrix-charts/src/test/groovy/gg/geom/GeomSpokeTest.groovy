package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

import testutil.Slow

/**
 * Tests for GeomSpoke - radial line segments.
 */
@Slow
class GeomSpokeTest {

  @Test
  void testBasicSpokes() {
    // Wind rose style data - direction and strength
    def data = Matrix.builder()
        .data([
            x: [0, 0, 0, 0],
            y: [0, 0, 0, 0],
            angle: [0, Math.PI / 2, Math.PI, 3 * Math.PI / 2],
            radius: [1, 1.5, 0.8, 1.2]
        ])
        .types(Integer, Integer, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_spoke()

    Svg svg = chart.render()
    assertNotNull(svg, 'Basic spokes should render')
  }

  @Test
  void testSpokesWithColor() {
    def data = Matrix.builder()
        .data([
            x: [0, 0],
            y: [0, 0],
            angle: [0, Math.PI],
            radius: [1, 1]
        ])
        .types(Integer, Integer, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_spoke(color: 'blue', linewidth: 2)

    Svg svg = chart.render()
    assertNotNull(svg, 'Spokes with color should render')
  }

  @Test
  void testWindRosePattern() {
    // 8 directions like a compass
    def angles = (0..7).collect { i -> i * 2 * Math.PI / 8 }
    def strengths = [1.0, 1.5, 0.8, 1.2, 0.9, 1.3, 1.1, 0.7]

    def data = Matrix.builder()
        .data([
            x: [0] * 8,
            y: [0] * 8,
            angle: angles,
            radius: strengths
        ])
        .types(Integer, Integer, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_spoke(color: 'darkblue', linewidth: 2) +
        coord_fixed()

    Svg svg = chart.render()
    assertNotNull(svg, 'Wind rose pattern should render')
  }

  @Test
  void testSpokesWithAlpha() {
    def data = Matrix.builder()
        .data([
            x: [0, 0, 0],
            y: [0, 0, 0],
            angle: [0, Math.PI / 3, 2 * Math.PI / 3],
            radius: [1, 1, 1]
        ])
        .types(Integer, Integer, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_spoke(alpha: 0.5)

    Svg svg = chart.render()
    assertNotNull(svg, 'Spokes with alpha should render')
  }

  @Test
  void testSpokesWithLinetype() {
    def data = Matrix.builder()
        .data([
            x: [0, 0],
            y: [0, 0],
            angle: [0, Math.PI / 2],
            radius: [1, 1]
        ])
        .types(Integer, Integer, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_spoke(linetype: 'dashed')

    Svg svg = chart.render()
    assertNotNull(svg, 'Spokes with linetype should render')
  }

  @Test
  void testMultipleOrigins() {
    // Spokes from different points
    def data = Matrix.builder()
        .data([
            x: [0, 1, 2],
            y: [0, 1, 0],
            angle: [Math.PI / 4, Math.PI / 2, 3 * Math.PI / 4],
            radius: [0.5, 0.7, 0.6]
        ])
        .types(Integer, Integer, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_spoke(color: 'red')

    Svg svg = chart.render()
    assertNotNull(svg, 'Multiple origin spokes should render')
  }

  @Test
  void testEmptyData() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'angle', 'radius')
        .types(Integer, Integer, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_spoke()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should render without error on empty data')
  }

  @Test
  void testDefaultRadius() {
    // Test without radius column - should use default
    def data = Matrix.builder()
        .data([
            x: [0, 0],
            y: [0, 0],
            angle: [0, Math.PI / 2]
        ])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_spoke(radius: 2.0)

    Svg svg = chart.render()
    assertNotNull(svg, 'Spokes with default radius should render')
  }

  @Test
  void testStatSpoke() {
    // Test stat_spoke() factory method
    def data = Matrix.builder()
        .data([
            x: [0, 0, 0, 0],
            y: [0, 0, 0, 0],
            direction: [0, Math.PI / 2, Math.PI, 3 * Math.PI / 2],
            speed: [1.0, 1.5, 0.8, 1.2]
        ])
        .types(Integer, Integer, BigDecimal, BigDecimal)
        .build()

    // Using stat_spoke with custom column names
    def chart = ggplot(data, aes('x', 'y')) +
        geom_spoke() +
        stat_spoke(angle: 'direction', radius: 'speed')

    Svg svg = chart.render()
    assertNotNull(svg, 'stat_spoke() should work with custom column names')
  }

  @Test
  void testStatSpokeFactory() {
    // Verify stat_spoke factory creates correct instance
    def stat = stat_spoke(angle: 'theta', radius: 'r')
    assertNotNull(stat, 'stat_spoke should create instance')
    assertEquals('theta', stat.params.angle, 'Should preserve angle parameter')
    assertEquals('r', stat.params.radius, 'Should preserve radius parameter')
  }
}
