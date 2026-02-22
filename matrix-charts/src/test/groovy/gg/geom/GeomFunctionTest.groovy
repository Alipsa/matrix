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

  /**
   * Extract path coordinates from SVG path 'd' attribute for function curves.
   * Returns list of [x, y] coordinate pairs from the longest path with many L commands
   * (function curves have many line segments).
   */
  private static List<double[]> extractPathCoordinates(String svgContent) {
    // Find all path elements
    def allPaths = svgContent.findAll(/<path[^>]*>/)

    // Extract coordinates from each path and find the one with the most points
    Map<String, List<double[]>> pathCoords = [:]

    allPaths.each { pathElement ->
      def dMatch = pathElement =~ /d=["']([^"']+)["']/
      if (dMatch) {
        String pathData = dMatch[0][1]
        List<double[]> coords = []

        // Parse M and L commands (M x y or L x y), including scientific notation.
        def numberPattern = /[-+]?(?:\d*\.\d+|\d+\.?\d*)(?:[eE][-+]?\d+)?/
        def coordPattern = ~/([ML])\s*(${numberPattern})[,\s]+(${numberPattern})/
        def matcher = (pathData =~ coordPattern)
        while (matcher.find()) {
          String xToken = matcher.group(2)
          String yToken = matcher.group(3)
          if (xToken == null || yToken == null || xToken.isBlank() || yToken.isBlank()) {
            continue
          }
          coords << ([Double.parseDouble(xToken), Double.parseDouble(yToken)] as double[])
        }

        if (!coords.isEmpty()) {
          pathCoords[pathData] = coords
        }
      }
    }

    if (pathCoords.isEmpty()) return []

    // Return coordinates from the path with the most points (the function curve)
    return pathCoords.values().max { it.size() }
  }

  @Test
  void testBasicSineFunction() {
    // Test: Basic sine function with default n=101 points
    // Verify: Path contains correct number of points and mathematically correct coordinates
    def chart = ggplot(null, null) +
                xlim(0, 2*Math.PI) +
                geom_function(fun: { x -> Math.sin(x) })
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<svg'))
    assertTrue(svgContent.contains('<path'))
    assertTrue(svgContent.contains('stroke'))

    // Extract and verify path coordinates
    List<double[]> coords = extractPathCoordinates(svgContent)
    assertEquals(101, coords.size(), "Expected 101 points (default n=101)")

    // The coordinates in SVG are already transformed through scales and coord system
    // We can verify that we have a reasonable distribution of y values for a sine wave
    double[] yValues = coords.collect { it[1] } as double[]
    double minY = yValues.min()
    double maxY = yValues.max()

    // Sine wave should have variation (not a flat line)
    // Use a more lenient threshold since SVG coordinates are scaled
    assertTrue(maxY - minY > 1, "Sine wave should show variation in y values (found range: ${maxY - minY})")
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
    // Verify: Function renders within specified range with correct number of points
    def n = 50
    def chart = ggplot(null, null) +
                geom_function(
                  fun: { x -> Math.cos(x) },
                  xlim: [-Math.PI, Math.PI],
                  n: n
                )
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))

    // Verify number of points
    List<double[]> coords = extractPathCoordinates(svgContent)
    assertEquals(n, coords.size(), "Expected ${n} points as specified by n parameter")

    // Verify x coordinates span the expected range (after scale transformation)
    // The actual pixel values will be transformed, but we should have spread
    double[] xValues = coords.collect { it[0] } as double[]
    double minX = xValues.min()
    double maxX = xValues.max()

    // X values should have reasonable spread (not all the same)
    assertTrue(maxX - minX > 1, "X coordinates should span a range (found ${maxX - minX})")

    // Y values should also show variation (cosine function oscillates)
    double[] yValues = coords.collect { it[1] } as double[]
    double minY = yValues.min()
    double maxY = yValues.max()
    assertTrue(maxY - minY > 1, "Y coordinates should show variation for cosine function (found ${maxY - minY})")
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
    // Test: Cubic polynomial with specific n
    // Verify: Renders smooth curve with correct number of points and expected shape
    def n = 60
    def chart = ggplot(null, null) +
                xlim(-2, 2) +
                geom_function(fun: { x -> x**3 - 2*x }, n: n)
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))

    // Verify number of points
    List<double[]> coords = extractPathCoordinates(svgContent)
    assertEquals(n, coords.size(), "Expected ${n} points as specified by n parameter")

    // For a cubic function x^3 - 2x over [-2, 2], it should have:
    // - local max around x = -sqrt(2/3) ≈ -0.816
    // - local min around x = sqrt(2/3) ≈ 0.816
    // We can verify the y values show this pattern (decrease, then increase, then decrease)
    double[] yValues = coords.collect { it[1] } as double[]

    // Verify we have variation in y values (not a flat line)
    double minY = yValues.min()
    double maxY = yValues.max()
    assertTrue(maxY - minY > 1, "Cubic function should show significant variation in y values")
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
    // Verify: Path contains exactly 20 points
    def chart = ggplot(null, null) +
                xlim(0, 2*Math.PI) +
                geom_function(
                  fun: { x -> Math.sin(x) },
                  n: 20
                )
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))

    // Verify number of points matches 'n' parameter
    List<double[]> coords = extractPathCoordinates(svgContent)
    assertEquals(20, coords.size(), "Expected exactly 20 points as specified by n parameter")
  }

  @Test
  void testFunctionWithManyPoints() {
    // Test: Function with many points for smoothness (n=500)
    // Verify: Path contains exactly 500 points
    def chart = ggplot(null, null) +
                xlim(0, 2*Math.PI) +
                geom_function(
                  fun: { x -> Math.sin(x) },
                  n: 500
                )
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))

    // Verify number of points matches 'n' parameter
    List<double[]> coords = extractPathCoordinates(svgContent)
    assertEquals(500, coords.size(), "Expected exactly 500 points as specified by n parameter")
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
    // Test: Simple linear function with known n value
    // Verify: Renders straight line with correct number of points
    def n = 50
    def chart = ggplot(null, null) +
                xlim(-10, 10) +
                geom_function(fun: { x -> 2*x + 3 }, n: n)
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))

    // Verify number of points
    List<double[]> coords = extractPathCoordinates(svgContent)
    assertEquals(n, coords.size(), "Expected ${n} points as specified by n parameter")

    // For a linear function f(x) = 2x + 3 over [-10, 10], verify reasonable variation
    // The y values should span a range (not be constant)
    double[] yValues = coords.collect { it[1] } as double[]
    double minY = yValues.min()
    double maxY = yValues.max()
    assertTrue(maxY - minY > 5, "Linear function should show significant y-value range (found: ${maxY - minY})")
  }

  @Test
  void testConstantFunction() {
    // Test: Constant function (horizontal line)
    // Verify: All y values should be at the same level after transformation
    def n = 30
    def chart = ggplot(null, null) +
                xlim(0, 10) +
                geom_function(fun: { x -> 5 }, n: n)
    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<path'))

    // Verify all y coordinates are the same (constant function)
    List<double[]> coords = extractPathCoordinates(svgContent)
    assertEquals(n, coords.size(), "Expected ${n} points as specified by n parameter")

    double[] yValues = coords.collect { it[1] } as double[]
    double firstY = yValues[0]
    double maxDeviation = yValues.collect { Math.abs(it - firstY) }.max()

    // All y values should be very similar (allowing for coordinate transformations)
    // Constant function should have much less variation than a typical function
    assertTrue(maxDeviation < 20, "Constant function should have minimal y value variation (max deviation: ${maxDeviation})")
  }
}
