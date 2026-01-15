package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for GeomCurve - curved line segments.
 */
class GeomCurveTest {

  @Test
  void testBasicCurve() {
    def data = Matrix.builder()
        .data([
            x: [1],
            y: [1],
            xend: [5],
            yend: [5]
        ])
        .types(Integer, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_curve()

    Svg svg = chart.render()
    assertNotNull(svg, 'Basic curve should render')
  }

  @Test
  void testCurveWithCurvature() {
    def data = Matrix.builder()
        .data([
            x: [0, 0, 0],
            y: [0, 1, 2],
            xend: [5, 5, 5],
            yend: [0, 1, 2]
        ])
        .types(Integer, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_curve(curvature: 1.0, color: 'blue', linewidth: 2)

    Svg svg = chart.render()
    assertNotNull(svg, 'Curve with custom curvature should render')
  }

  @Test
  void testNegativeCurvature() {
    def data = Matrix.builder()
        .data([
            x: [1, 1],
            y: [1, 3],
            xend: [5, 5],
            yend: [1, 3]
        ])
        .types(Integer, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_curve(curvature: -0.5)

    Svg svg = chart.render()
    assertNotNull(svg, 'Curve with negative curvature should render')
  }

  @Test
  void testStraightCurve() {
    // Curvature of 0 should be like a straight line
    def data = Matrix.builder()
        .data([
            x: [1],
            y: [1],
            xend: [5],
            yend: [5]
        ])
        .types(Integer, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_curve(curvature: 0)

    Svg svg = chart.render()
    assertNotNull(svg, 'Straight curve should render')
  }

  @Test
  void testMultipleCurves() {
    def data = Matrix.builder()
        .data([
            x: [1, 2, 3],
            y: [1, 2, 1],
            xend: [2, 3, 4],
            yend: [2, 1, 2]
        ])
        .types(Integer, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_curve(color: 'red')

    Svg svg = chart.render()
    assertNotNull(svg, 'Multiple curves should render')
  }

  @Test
  void testCurveWithAlpha() {
    def data = Matrix.builder()
        .data([
            x: [1],
            y: [1],
            xend: [5],
            yend: [5]
        ])
        .types(Integer, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_curve(alpha: 0.3)

    Svg svg = chart.render()
    assertNotNull(svg, 'Curve with alpha should render')
  }

  @Test
  void testCurveWithLinetype() {
    def data = Matrix.builder()
        .data([
            x: [1, 1],
            y: [1, 2],
            xend: [5, 5],
            yend: [1, 2]
        ])
        .types(Integer, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_curve(linetype: 'dashed', curvature: 0.8)

    Svg svg = chart.render()
    assertNotNull(svg, 'Curve with dashed linetype should render')
  }

  @Test
  void testEmptyData() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'xend', 'yend')
        .types(Integer, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_curve()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should render without error on empty data')
  }

  @Test
  void testFlowDiagram() {
    // Simulating a simple flow diagram with curved connections
    def data = Matrix.builder()
        .data([
            x: [1, 1, 2, 2],
            y: [1, 2, 1, 2],
            xend: [2, 2, 3, 3],
            yend: [1, 2, 1.5, 1.5]
        ])
        .types(Integer, Integer, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point(size: 5) +
        geom_curve(curvature: 0.3, color: 'gray', linewidth: 1.5)

    Svg svg = chart.render()
    assertNotNull(svg, 'Flow diagram with curves should render')
  }

  @Test
  void testHighCurvature() {
    def data = Matrix.builder()
        .data([
            x: [1],
            y: [1],
            xend: [3],
            yend: [1]
        ])
        .types(Integer, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_curve(curvature: 2.0, color: 'purple')

    Svg svg = chart.render()
    assertNotNull(svg, 'Curve with high curvature should render')
  }
}
