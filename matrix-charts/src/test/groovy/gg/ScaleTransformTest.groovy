package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.*

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for transform scales: log10, sqrt, reverse
 */
class ScaleTransformTest extends BaseTest {

  // ============ ScaleXLog10 / ScaleYLog10 Tests ============

  @Test
  void testScaleXLog10Defaults() {
    ScaleXLog10 scale = new ScaleXLog10()
    assertEquals('x', scale.aesthetic)
    assertEquals('bottom', scale.position)
  }

  @Test
  void testScaleXLog10Train() {
    ScaleXLog10 scale = new ScaleXLog10()
    scale.range = [0, 100] as List<Number>
    scale.train([1, 10, 100, 1000])

    assertTrue(scale.isTrained())

    // Transform should work in log space
    // log10(1) = 0, log10(1000) = 3
    // With domain [0, 3] and range [0, 100], value 10 (log=1) should map to ~33
    def transformed = scale.transform(10)
    assertNotNull(transformed)
    // Due to expansion, the exact value may differ, but should be reasonable
    assertTrue((transformed as double) > 20 && (transformed as double) < 50)
  }

  @Test
  void testScaleXLog10Inverse() {
    ScaleXLog10 scale = new ScaleXLog10()
    scale.range = [0, 100] as List<Number>
    scale.expand = null  // Disable expansion for exact test
    scale.train([1, 10, 100, 1000])

    // Test transform and inverse are reciprocal
    [1, 10, 100, 1000].each { value ->
      def transformed = scale.transform(value)
      def inverted = scale.inverse(transformed)
      assertEquals(value as double, inverted as double, 0.01, "Inverse should recover original for $value")
    }
  }

  @Test
  void testScaleXLog10FiltersNonPositive() {
    ScaleXLog10 scale = new ScaleXLog10()
    scale.range = [0, 100] as List<Number>
    scale.train([-5, 0, 1, 10, 100])

    // Non-positive values should return null when transformed
    assertNull(scale.transform(-5))
    assertNull(scale.transform(0))
    assertNotNull(scale.transform(1))
    assertNotNull(scale.transform(10))
  }

  @Test
  void testLog10ChartRender() {
    // Data with exponential growth
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 1],
            [10, 10],
            [100, 100],
            [1000, 1000],
            [10000, 10000]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 4) +
        geom_line() +
        scale_x_log10() +
        scale_y_log10() +
        labs(title: 'Log10 Scales')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/log10_scale_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testLog10BreaksGeneration() {
    ScaleXLog10 scale = new ScaleXLog10()
    scale.range = [0, 100] as List<Number>
    scale.train([1, 1000])

    List breaks = scale.getComputedBreaks()
    assertNotNull(breaks)
    assertTrue(breaks.size() > 0)
    // Should contain powers of 10 (check using doubles for comparison)
    List<Double> breakValues = breaks.collect { (it as Number).doubleValue() }
    assertTrue(breakValues.any { Math.abs(it - 1.0) < 0.01 }, "Should contain 1")
    assertTrue(breakValues.any { Math.abs(it - 10.0) < 0.01 }, "Should contain 10")
    assertTrue(breakValues.any { Math.abs(it - 100.0) < 0.01 }, "Should contain 100")
  }

  // ============ ScaleXSqrt / ScaleYSqrt Tests ============

  @Test
  void testScaleXSqrtDefaults() {
    ScaleXSqrt scale = new ScaleXSqrt()
    assertEquals('x', scale.aesthetic)
    assertEquals('bottom', scale.position)
  }

  @Test
  void testScaleXSqrtTrain() {
    ScaleXSqrt scale = new ScaleXSqrt()
    scale.range = [0, 100] as List<Number>
    scale.train([0, 4, 16, 100])

    assertTrue(scale.isTrained())

    // Transform should work in sqrt space
    // sqrt(0) = 0, sqrt(100) = 10
    // With domain [0, 10] and range [0, 100], value 4 (sqrt=2) should map to 20
    def transformed = scale.transform(4)
    assertNotNull(transformed)
  }

  @Test
  void testScaleXSqrtInverse() {
    ScaleXSqrt scale = new ScaleXSqrt()
    scale.range = [0, 100] as List<Number>
    scale.expand = null  // Disable expansion for exact test
    scale.train([0, 4, 16, 100])

    // Test transform and inverse are reciprocal
    [0, 4, 16, 100].each { value ->
      def transformed = scale.transform(value)
      def inverted = scale.inverse(transformed)
      assertEquals(value as double, inverted as double, 0.01, "Inverse should recover original for $value")
    }
  }

  @Test
  void testScaleXSqrtFiltersNegative() {
    ScaleXSqrt scale = new ScaleXSqrt()
    scale.range = [0, 100] as List<Number>
    scale.train([-5, 0, 4, 16])

    // Negative values should return null when transformed
    assertNull(scale.transform(-5))
    // Zero and positive should work
    assertNotNull(scale.transform(0))
    assertNotNull(scale.transform(4))
  }

  @Test
  void testSqrtChartRender() {
    // Count-like data (benefits from sqrt transformation)
    def data = Matrix.builder()
        .columnNames('x', 'count')
        .rows([
            [1, 1],
            [2, 4],
            [3, 9],
            [4, 16],
            [5, 25],
            [6, 36],
            [7, 49],
            [8, 64],
            [9, 81],
            [10, 100]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'count')) +
        geom_point(size: 4) +
        geom_line() +
        scale_y_sqrt() +
        labs(title: 'Sqrt Y Scale (Count Data)')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/sqrt_scale_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============ ScaleXReverse / ScaleYReverse Tests ============

  @Test
  void testScaleXReverseDefaults() {
    ScaleXReverse scale = new ScaleXReverse()
    assertEquals('x', scale.aesthetic)
    assertEquals('bottom', scale.position)
  }

  @Test
  void testScaleXReverseTransform() {
    ScaleXReverse scale = new ScaleXReverse()
    scale.range = [0, 100]
    scale.expand = null  // Disable expansion for exact test
    scale.train([0, 10, 20, 30])

    assertTrue(scale.isTrained())

    // In reversed scale, low values should map to high range values
    // Domain [0, 30] -> Range [0, 100] reversed
    // So value 0 should map to 100, and value 30 should map to 0
    def transformedLow = scale.transform(0) as double
    def transformedHigh = scale.transform(30) as double

    assertEquals(100.0, transformedLow, 0.1, "Low value should map to high end of range")
    assertEquals(0.0, transformedHigh, 0.1, "High value should map to low end of range")
  }

  @Test
  void testScaleXReverseInverse() {
    ScaleXReverse scale = new ScaleXReverse()
    scale.range = [0, 100] as List<Number>
    scale.expand = null  // Disable expansion for exact test
    scale.train([0, 10, 20, 30])

    // Test transform and inverse are reciprocal
    [0, 10, 20, 30].each { value ->
      def transformed = scale.transform(value)
      def inverted = scale.inverse(transformed)
      assertEquals(value as double, inverted as double, 0.01, "Inverse should recover original for $value")
    }
  }

  @Test
  void testReverseChartRender() {
    def data = Matrix.builder()
        .columnNames('rank', 'score')
        .rows([
            [1, 95],
            [2, 88],
            [3, 82],
            [4, 75],
            [5, 70]
        ])
        .types(Integer, Integer)
        .build()

    // Rank 1 should be on the right (highest position)
    def chart = ggplot(data, aes(x: 'rank', y: 'score')) +
        geom_point(size: 5) +
        geom_line() +
        scale_x_reverse() +
        labs(title: 'Reversed X Scale (Rank)')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/reverse_x_scale_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testReverseYChartRender() {
    def data = Matrix.builder()
        .columnNames('depth', 'value')
        .rows([
            [0, 10],
            [10, 15],
            [20, 12],
            [30, 18],
            [40, 20]
        ])
        .types(Integer, Integer)
        .build()

    // Depth increasing downward (geological style)
    def chart = ggplot(data, aes(x: 'value', y: 'depth')) +
        geom_point(size: 5) +
        geom_path() +
        scale_y_reverse() +
        labs(title: 'Reversed Y Scale (Depth)', x: 'Value', y: 'Depth (m)')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/reverse_y_scale_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============ Factory Method Tests ============

  @Test
  void testFactoryMethods() {
    // Log10 scales
    assertNotNull(scale_x_log10())
    assertTrue(scale_x_log10() instanceof ScaleXLog10)
    assertNotNull(scale_y_log10())
    assertTrue(scale_y_log10() instanceof ScaleYLog10)

    // Sqrt scales
    assertNotNull(scale_x_sqrt())
    assertTrue(scale_x_sqrt() instanceof ScaleXSqrt)
    assertNotNull(scale_y_sqrt())
    assertTrue(scale_y_sqrt() instanceof ScaleYSqrt)

    // Reverse scales
    assertNotNull(scale_x_reverse())
    assertTrue(scale_x_reverse() instanceof ScaleXReverse)
    assertNotNull(scale_y_reverse())
    assertTrue(scale_y_reverse() instanceof ScaleYReverse)
  }

  @Test
  void testFactoryMethodsWithParams() {
    def log10 = scale_x_log10(name: 'Log Scale')
    assertEquals('Log Scale', log10.name)

    def sqrt = scale_y_sqrt(limits: [0, 100])
    BaseTest.assertIterableEquals([0, 100], sqrt.limits)

    def reverse = scale_x_reverse(position: 'top')
    assertEquals('top', reverse.position)
  }

  // ============ Combined Visualization Tests ============

  @Test
  void testCompareNormalVsLog() {
    // Create data that demonstrates the value of log scaling
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 1],
            [2, 2],
            [5, 5],
            [10, 10],
            [50, 50],
            [100, 100],
            [500, 500],
            [1000, 1000]
        ])
        .types(Integer, Integer)
        .build()

    // Normal linear scale
    def normalChart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 4) +
        geom_line() +
        labs(title: 'Linear Scale')

    // Log10 scale
    def logChart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 4) +
        geom_line() +
        scale_x_log10() +
        scale_y_log10() +
        labs(title: 'Log10 Scale')

    Svg normalSvg = normalChart.render()
    Svg logSvg = logChart.render()

    assertNotNull(normalSvg)
    assertNotNull(logSvg)

    write(normalSvg, new File('build/compare_linear.svg'))
    write(logSvg, new File('build/compare_log10.svg'))
  }

  @Test
  void testCombinedTransforms() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 100],
            [4, 81],
            [9, 64],
            [16, 49],
            [25, 36],
            [36, 25],
            [49, 16],
            [64, 9],
            [81, 4],
            [100, 1]
        ])
        .types(Integer, Integer)
        .build()

    // X: sqrt, Y: reverse
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 4, color: 'steelblue') +
        geom_line(color: 'gray') +
        scale_x_sqrt() +
        scale_y_reverse() +
        labs(title: 'Sqrt X + Reverse Y')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/combined_transforms.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }
}
