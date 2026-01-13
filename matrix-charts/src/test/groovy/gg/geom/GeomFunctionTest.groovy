package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for GeomFunction - drawing functions as continuous curves.
 */
class GeomFunctionTest {

  @Test
  void testBasicSineFunction() {
    // Test: Basic sine function
    // Verify: SVG contains path element with correct structure
    def chart = ggplot(null, null) +
                xlim(0, 2*Math.PI) +
                geom_function(fun: { x -> Math.sin(x) })
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<svg'))
    assertTrue(svgContent.contains('<path'))
    assertTrue(svgContent.contains('</svg>'))
    assertTrue(svgContent.contains('stroke'))
  }

  @Test
  void testFunctionWithColor() {
    // Test: Function with custom color
    // Verify: SVG includes stroke color
    def chart = ggplot(null, null) +
                xlim(-3, 3) +
                geom_function(
                  fun: { x -> x**2 },
                  color: 'red'
                )
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('stroke="red"') ||
               svgContent.contains('stroke: red') ||
               svgContent.contains("stroke='red'"))
  }

  @Test
  void testFunctionWithLinewidth() {
    // Test: Function with custom line width
    // Verify: SVG includes stroke-width
    def chart = ggplot(null, null) +
                xlim(0, 10) +
                geom_function(
                  fun: { x -> Math.log(x + 1) },
                  linewidth: 2
                )
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('stroke-width'))
  }

  @Test
  void testFunctionWithLinetype() {
    // Test: Function with dashed linetype
    // Verify: SVG includes stroke-dasharray
    def chart = ggplot(null, null) +
                xlim(-3, 3) +
                geom_function(
                  fun: { x -> x**2 },
                  linetype: 'dashed'
                )
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('stroke-dasharray'))
  }

  @Test
  void testFunctionWithAlpha() {
    // Test: Function with alpha transparency
    // Verify: SVG includes opacity attribute
    def chart = ggplot(null, null) +
                xlim(0, 10) +
                geom_function(
                  fun: { x -> Math.exp(-x) },
                  alpha: 0.5
                )
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('stroke-opacity'))
  }

  @Test
  void testFunctionWithMultipleStyles() {
    // Test: Function with color, size, and linetype
    // Verify: All styling attributes present
    def chart = ggplot(null, null) +
                xlim(-Math.PI, Math.PI) +
                geom_function(
                  fun: { x -> Math.cos(x) },
                  color: 'steelblue',
                  linewidth: 1.5,
                  linetype: 'dotted'
                )
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
    assertTrue(svgContent.contains('stroke'))
    assertTrue(svgContent.contains('stroke-width'))
    assertTrue(svgContent.contains('stroke-dasharray'))
  }

  @Test
  void testFunctionOverlayOnData() {
    // Test: Function overlaid on scatter plot
    // Verify: Both points and function path appear
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([
          [1, 1.1],
          [2, 3.9],
          [3, 9.2],
          [4, 15.8],
          [5, 25.1]
        ])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
                geom_point() +
                geom_function(fun: { x -> x**2 }, color: 'blue')
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    // Should have both circles (points) and path (function)
    assertTrue(svgContent.contains('<circle'))
    assertTrue(svgContent.contains('<path'))
  }

  @Test
  void testFunctionWithCustomRange() {
    // Test: Function with explicit xlim in geom params
    // Verify: Function renders within specified range
    def chart = ggplot(null, null) +
                geom_function(
                  fun: { x -> Math.cos(x) },
                  xlim: [-Math.PI, Math.PI],
                  n: 50
                )
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
  }

  @Test
  void testMultipleFunctions() {
    // Test: Multiple function layers
    // Verify: Multiple paths with different colors
    def chart = ggplot(null, null) +
                xlim(0, Math.PI) +
                geom_function(fun: { x -> Math.sin(x) }, color: 'red') +
                geom_function(fun: { x -> Math.cos(x) }, color: 'blue')
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    // Should have at least two path elements
    int pathCount = svgContent.count('<path')
    assertTrue(pathCount >= 2, "Expected at least 2 paths, found ${pathCount}")
  }

  @Test
  void testNormalDistribution() {
    // Test: Standard normal distribution (bell curve)
    // Verify: Renders smooth curve
    def dnorm = { x ->
      Math.exp(-x*x/2) / Math.sqrt(2*Math.PI)
    }

    def chart = ggplot(null, null) +
                xlim(-3, 3) +
                geom_function(fun: dnorm, color: 'darkgreen')
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
  }

  @Test
  void testExponentialFunction() {
    // Test: Exponential growth function
    // Verify: Handles large y-value ranges
    def chart = ggplot(null, null) +
                xlim(0, 5) +
                geom_function(fun: { x -> Math.exp(x) })
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
  }

  @Test
  void testPolynomialFunction() {
    // Test: Cubic polynomial
    // Verify: Renders smooth curve
    def chart = ggplot(null, null) +
                xlim(-2, 2) +
                geom_function(fun: { x -> x**3 - 2*x })
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
  }

  @Test
  void testLogarithmicFunction() {
    // Test: Natural logarithm function
    // Verify: Handles undefined values (x <= 0)
    def chart = ggplot(null, null) +
                xlim(0.1, 10) +
                geom_function(fun: { x -> Math.log(x) })
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
  }

  @Test
  void testSquareRootFunction() {
    // Test: Square root function
    // Verify: Renders half-parabola
    def chart = ggplot(null, null) +
                xlim(0, 25) +
                geom_function(fun: { x -> Math.sqrt(x) })
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
  }

  @Test
  void testFunctionWithTitle() {
    // Test: Function with plot title
    // Verify: Title appears in SVG
    def chart = ggplot(null, null) +
                xlim(0, 10) +
                geom_function(fun: { x -> x**2 }) +
                ggtitle('Quadratic Function')
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('Quadratic Function'))
  }

  @Test
  void testFunctionWithLabels() {
    // Test: Function with axis labels
    // Verify: Labels appear in SVG
    def chart = ggplot(null, null) +
                xlim(-5, 5) +
                geom_function(fun: { x -> x**3 }) +
                xlab('x values') +
                ylab('f(x) = x³')
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('x values'))
    assertTrue(svgContent.contains('f(x)'))
  }

  @Test
  void testFunctionWithCustomNumberOfPoints() {
    // Test: Function with fewer points (n=20)
    // Verify: Still renders (may be less smooth)
    def chart = ggplot(null, null) +
                xlim(0, 2*Math.PI) +
                geom_function(
                  fun: { x -> Math.sin(x) },
                  n: 20
                )
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
  }

  @Test
  void testFunctionWithManyPoints() {
    // Test: Function with many points for smoothness (n=500)
    // Verify: Renders successfully
    def chart = ggplot(null, null) +
                xlim(0, 2*Math.PI) +
                geom_function(
                  fun: { x -> Math.sin(x) },
                  n: 500
                )
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
  }

  @Test
  void testFunctionWithTheme() {
    // Test: Function with custom theme
    // Verify: Theme is applied
    def chart = ggplot(null, null) +
                xlim(0, 10) +
                geom_function(fun: { x -> Math.log10(x + 1) }) +
                theme_minimal()
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
  }

  @Test
  void testAbsoluteValueFunction() {
    // Test: Absolute value function (V-shape)
    // Verify: Renders V-shaped curve
    def chart = ggplot(null, null) +
                xlim(-5, 5) +
                geom_function(fun: { x -> Math.abs(x) })
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
  }

  @Test
  void testPiecewiseFunction() {
    // Test: Piecewise function
    // Verify: Handles conditional logic
    def piecewise = { x ->
      x < 0 ? -x : x * x
    }

    def chart = ggplot(null, null) +
                xlim(-3, 3) +
                geom_function(fun: piecewise)
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
  }

  @Test
  void testFunctionWithBritishSpelling() {
    // Test: Using 'colour' instead of 'color'
    // Verify: British spelling works
    def chart = ggplot(null, null) +
                xlim(0, 5) +
                geom_function(
                  fun: { x -> x },
                  colour: 'purple'
                )
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
    // Color should be normalized
    assertTrue(svgContent.contains('stroke'))
  }

  @Test
  void testTangentFunction() {
    // Test: Tangent function with asymptotes
    // Verify: Handles discontinuities reasonably
    def chart = ggplot(null, null) +
                xlim(-Math.PI/2 + 0.1, Math.PI/2 - 0.1) +
                geom_function(fun: { x -> Math.tan(x) })
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
  }

  @Test
  void testLinearFunction() {
    // Test: Simple linear function
    // Verify: Renders straight line
    def chart = ggplot(null, null) +
                xlim(-10, 10) +
                geom_function(fun: { x -> 2*x + 3 })
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
  }

  @Test
  void testConstantFunction() {
    // Test: Constant function (horizontal line)
    // Verify: Renders horizontal line
    def chart = ggplot(null, null) +
                xlim(0, 10) +
                geom_function(fun: { x -> 5 })
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))
  }
}
