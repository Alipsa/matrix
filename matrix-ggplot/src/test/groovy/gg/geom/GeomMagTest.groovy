package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

import testutil.Slow

/**
 * Tests for GeomMag - magnitude visualization for vector fields.
 */
@Slow
class GeomMagTest {

  @Test
  void testBasicMagnitude() {
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3, 4],
            y: [2, 4, 3, 5],
            magnitude: [0.5, 1.0, 1.5, 2.0]
        ])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag()

    Svg svg = chart.render()
    assertNotNull(svg, 'Basic magnitude visualization should render')

    // Verify points were rendered
    def circles = svg.descendants().findAll { it instanceof Circle }
    assertTrue(circles.size() > 0, 'Should render magnitude points')
  }

  @Test
  void testMagColumnName() {
    // Test using 'mag' instead of 'magnitude'
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3],
            y: [2, 4, 3],
            mag: [0.5, 1.0, 1.5]
        ])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should work with mag column name')
  }

  @Test
  void testExplicitMagnitudeColumn() {
    // Test explicitly specifying magnitude column via params
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3],
            y: [2, 4, 3],
            strength: [0.5, 1.0, 1.5]
        ])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag(magnitude: 'strength')

    Svg svg = chart.render()
    assertNotNull(svg, 'Should work with custom magnitude column')
  }

  @Test
  void testNegativeMagnitude() {
    // Negative magnitudes should be clamped to zero
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3, 4],
            y: [2, 4, 3, 5],
            magnitude: [-0.5, 1.0, -1.5, 2.0]
        ])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should handle negative magnitudes gracefully')

    def circles = svg.descendants().findAll { it instanceof Circle }
    assertTrue(circles.size() > 0, 'Should still render points')
  }

  @Test
  void testNullMagnitude() {
    // Null magnitudes should be skipped or use default behavior
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3, 4],
            y: [2, 4, 3, 5],
            magnitude: [1.0, null, 1.5, 2.0]
        ])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should handle null magnitudes gracefully')
  }

  @Test
  void testDisableAutoScaling() {
    // Test with scaleToMagnitude disabled
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3],
            y: [2, 4, 3],
            magnitude: [0.5, 1.0, 1.5]
        ])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag(scaleToMagnitude: false, size: 5, color: 'red')

    Svg svg = chart.render()
    assertNotNull(svg, 'Should render with auto-scaling disabled')
  }

  @Test
  void testExplicitSizeMapping() {
    // Explicit size mapping should override auto-scaling
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3],
            y: [2, 4, 3],
            magnitude: [0.5, 1.0, 1.5],
            point_size: [3, 5, 7]
        ])
        .types(Integer, Integer, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', size: 'point_size')) +
        geom_mag()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should work with explicit size mapping')
  }

  @Test
  void testExplicitColorMapping() {
    // Explicit color mapping should override auto-scaling
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3],
            y: [2, 4, 3],
            magnitude: [0.5, 1.0, 1.5],
            category: ['A', 'B', 'C']
        ])
        .types(Integer, Integer, BigDecimal, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'category')) +
        geom_mag()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should work with explicit color mapping')
  }

  @Test
  void testWithColorGradient() {
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3, 4, 5],
            y: [2, 4, 3, 5, 4],
            magnitude: [0.5, 1.0, 1.5, 2.0, 2.5]
        ])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag() +
        scale_color_gradient(low: 'blue', high: 'red')

    Svg svg = chart.render()
    assertNotNull(svg, 'Should work with color gradient scale')
  }

  @Test
  void testWithSizeScale() {
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3, 4],
            y: [2, 4, 3, 5],
            magnitude: [0.5, 1.0, 1.5, 2.0]
        ])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag() +
        scale_size_continuous(range: [1, 10])

    Svg svg = chart.render()
    assertNotNull(svg, 'Should work with size scale')
  }

  @Test
  void testCustomShape() {
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3],
            y: [2, 4, 3],
            magnitude: [0.5, 1.0, 1.5]
        ])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag(shape: 'square', color: 'darkgreen')

    Svg svg = chart.render()
    assertNotNull(svg, 'Should work with custom shape')
  }

  @Test
  void testWithAlpha() {
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3],
            y: [2, 4, 3],
            magnitude: [0.5, 1.0, 1.5]
        ])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag(alpha: 0.6)

    Svg svg = chart.render()
    assertNotNull(svg, 'Should work with alpha transparency')
  }

  @Test
  void testEmptyData() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'magnitude')
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should render without error on empty data')
  }

  @Test
  void testFactoryMethodNoArgs() {
    def geom = geom_mag()
    assertNotNull(geom, 'Factory method should create instance')
    assertEquals('steelblue', geom.color, 'Should have default color')
  }

  @Test
  void testFactoryMethodWithMapping() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .types(Integer, Integer)
        .build()
    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag(aes(x: 'x', y: 'y'))
    assertNotNull(chart, 'Factory method with mapping should work')
  }

  @Test
  void testFactoryMethodWithParams() {
    def geom = geom_mag(color: 'red', size: 5, shape: 'diamond')
    assertNotNull(geom, 'Factory method with params should create instance')
    assertEquals('red', geom.color, 'Should preserve color parameter')
    assertEquals(5 as BigDecimal, geom.size, 'Should preserve size parameter')
    assertEquals('diamond', geom.shape, 'Should preserve shape parameter')
  }

  @Test
  void testSizeMultiplier() {
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3],
            y: [2, 4, 3],
            magnitude: [0.5, 1.0, 1.5]
        ])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag(sizeMultiplier: 2.0)

    Svg svg = chart.render()
    assertNotNull(svg, 'Should work with custom size multiplier')
  }

  @Test
  void testWindFieldVisualization() {
    // Realistic wind field data
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3, 1, 2, 3, 1, 2, 3],
            y: [1, 1, 1, 2, 2, 2, 3, 3, 3],
            magnitude: [2.5, 5.1, 3.2, 4.8, 6.2, 5.5, 3.1, 4.3, 2.9]
        ])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag() +
        labs(title: 'Wind Speed Field')

    Svg svg = chart.render()
    assertNotNull(svg, 'Wind field visualization should render')

    def circles = svg.descendants().findAll { it instanceof Circle }
    assertEquals(9, circles.size(), 'Should render all 9 points')
  }

  @Test
  void testNoMagnitudeColumn() {
    // Test behavior when no magnitude column exists
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3],
            y: [2, 4, 3]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should render even without magnitude column')

    // Should render as regular points with default size
    def circles = svg.descendants().findAll { it instanceof Circle }
    assertTrue(circles.size() > 0, 'Should render points with default size')
  }

  @Test
  void testZeroMagnitude() {
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3, 4],
            y: [2, 4, 3, 5],
            magnitude: [0, 0.5, 1.0, 0]
        ])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_mag()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should handle zero magnitude values')
  }
}
