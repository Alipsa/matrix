package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for GeomErrorbarh - horizontal error bars.
 */
class GeomErrorbarhTest {

  @Test
  void testBasicHorizontalErrorBars() {
    def data = Matrix.builder()
        .data([
            x: [1.0, 2.0, 3.0],
            y: [1, 2, 3],
            xmin: [0.5, 1.5, 2.5],
            xmax: [1.5, 2.5, 3.5]
        ])
        .types(BigDecimal, Integer, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_errorbarh()

    Svg svg = chart.render()
    assertNotNull(svg, 'Basic horizontal error bars should render')
  }

  @Test
  void testErrorbarhWithHeight() {
    def data = Matrix.builder()
        .data([
            x: [1.0, 2.0, 3.0],
            y: [1, 2, 3],
            xmin: [0.5, 1.5, 2.5],
            xmax: [1.5, 2.5, 3.5]
        ])
        .types(BigDecimal, Integer, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_errorbarh(height: 0.5, color: 'blue', linewidth: 2)

    Svg svg = chart.render()
    assertNotNull(svg, 'Horizontal error bars with custom height should render')
  }

  @Test
  void testErrorbarhWithDiscreteY() {
    def data = Matrix.builder()
        .data([
            x: [2.0, 3.0, 2.5],
            category: ['A', 'B', 'C'],
            xmin: [1.0, 2.0, 1.5],
            xmax: [3.0, 4.0, 3.5]
        ])
        .types(BigDecimal, String, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'category')) +
        geom_errorbarh()

    Svg svg = chart.render()
    assertNotNull(svg, 'Horizontal error bars with discrete y should render')
  }

  @Test
  void testErrorbarhWithPoints() {
    def data = Matrix.builder()
        .data([
            y: [1, 2, 3],
            x: [1.0, 2.0, 3.0],
            xmin: [0.5, 1.5, 2.5],
            xmax: [1.5, 2.5, 3.5]
        ])
        .types(Integer, BigDecimal, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_point() +
        geom_errorbarh()

    Svg svg = chart.render()
    assertNotNull(svg, 'Horizontal error bars with points should render')
  }

  @Test
  void testErrorbarhWithAlpha() {
    def data = Matrix.builder()
        .data([
            x: [1.0, 2.0, 3.0],
            y: [1, 2, 3],
            xmin: [0.5, 1.5, 2.5],
            xmax: [1.5, 2.5, 3.5]
        ])
        .types(BigDecimal, Integer, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_errorbarh(alpha: 0.5, color: 'red')

    Svg svg = chart.render()
    assertNotNull(svg, 'Horizontal error bars with alpha should render')
  }

  @Test
  void testErrorbarhWithLinetype() {
    def data = Matrix.builder()
        .data([
            x: [1.0, 2.0, 3.0],
            y: [1, 2, 3],
            xmin: [0.5, 1.5, 2.5],
            xmax: [1.5, 2.5, 3.5]
        ])
        .types(BigDecimal, Integer, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_errorbarh(linetype: 'dashed')

    Svg svg = chart.render()
    assertNotNull(svg, 'Horizontal error bars with dashed linetype should render')
  }

  @Test
  void testEmptyData() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'xmin', 'xmax')
        .types(BigDecimal, Integer, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_errorbarh()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should render without error on empty data')
  }

  @Test
  void testConfidenceIntervalVisualization() {
    // Simulating horizontal confidence intervals
    def data = Matrix.builder()
        .data([
            method: ['Method A', 'Method B', 'Method C'],
            estimate: [2.5, 3.0, 2.8],
            lower: [2.0, 2.5, 2.2],
            upper: [3.0, 3.5, 3.4],
            xmin: [2.0, 2.5, 2.2],
            xmax: [3.0, 3.5, 3.4]
        ])
        .types(String, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes('estimate', 'method')) +
        geom_point(size: 3) +
        geom_errorbarh(height: 0.2) +
        labs(x: 'Estimate', y: 'Method')

    Svg svg = chart.render()
    assertNotNull(svg, 'Confidence interval visualization should render')
  }
}
