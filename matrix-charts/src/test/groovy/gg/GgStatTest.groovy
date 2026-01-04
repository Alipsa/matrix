package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.stat.GgStat

import static org.junit.jupiter.api.Assertions.*

class GgStatTest {

  @Test
  void testParseFormulaSimpleLinear() {
    def result = GgStat.parseFormula('y ~ x')
    assertEquals(1, result.polyDegree)
    assertEquals('y', result.response)
    assertEquals('x', result.predictor)
  }

  @Test
  void testParseFormulaQuadratic() {
    def result = GgStat.parseFormula('y ~ poly(x, 2)')
    assertEquals(2, result.polyDegree)
    assertEquals('y', result.response)
    assertEquals('x', result.predictor)
  }

  @Test
  void testParseFormulaCubic() {
    def result = GgStat.parseFormula('y ~ poly(x, 3)')
    assertEquals(3, result.polyDegree)
    assertEquals('y', result.response)
    assertEquals('x', result.predictor)
  }

  @Test
  void testParseFormulaWithSpaces() {
    def result = GgStat.parseFormula('  y   ~   poly( x , 2 )  ')
    assertEquals(2, result.polyDegree)
    assertEquals('y', result.response)
    assertEquals('x', result.predictor)
  }

  @Test
  void testParseFormulaNull() {
    def result = GgStat.parseFormula(null)
    assertEquals(1, result.polyDegree)
    assertEquals('y', result.response)
    assertEquals('x', result.predictor)
  }

  @Test
  void testParseFormulaEmpty() {
    def result = GgStat.parseFormula('')
    assertEquals(1, result.polyDegree)
    assertEquals('y', result.response)
    assertEquals('x', result.predictor)
  }

  @Test
  void testParseFormulaWhitespaceOnly() {
    def result = GgStat.parseFormula('   ')
    assertEquals(1, result.polyDegree)
    assertEquals('y', result.response)
    assertEquals('x', result.predictor)
  }

  @Test
  void testParseFormulaNoTilde() {
    // Formula without ~ should treat the whole string as predictor
    def result = GgStat.parseFormula('x')
    assertEquals(1, result.polyDegree)
    assertEquals('y', result.response)  // Defaults to 'y'
    assertEquals('x', result.predictor)
  }

  @Test
  void testParseFormulaTildeOnly() {
    // Just ~ should fall back to defaults
    def result = GgStat.parseFormula('~')
    assertEquals(1, result.polyDegree)
    assertEquals('y', result.response)
    assertEquals('x', result.predictor)
  }

  @Test
  void testParseFormulaEmptyResponse() {
    def result = GgStat.parseFormula('~ x')
    assertEquals(1, result.polyDegree)
    assertEquals('y', result.response)  // Falls back to 'y'
    assertEquals('x', result.predictor)
  }

  @Test
  void testParseFormulaEmptyPredictor() {
    def result = GgStat.parseFormula('y ~')
    assertEquals(1, result.polyDegree)
    assertEquals('y', result.response)
    assertEquals('x', result.predictor)  // Falls back to 'x'
  }

  @Test
  void testParseFormulaInvalidDegreeZero() {
    def ex = assertThrows(IllegalArgumentException) {
      GgStat.parseFormula('y ~ poly(x, 0)')
    }
    assertTrue(ex.message.contains('at least 1'))
    assertTrue(ex.message.contains('0'))
  }

  @Test
  void testParseFormulaDifferentPredictor() {
    def result = GgStat.parseFormula('y ~ poly(z, 2)')
    assertEquals(2, result.polyDegree)
    assertEquals('y', result.response)
    assertEquals('z', result.predictor)
  }

  @Test
  void testParseFormulaDifferentResponse() {
    def result = GgStat.parseFormula('output ~ poly(input, 3)')
    assertEquals(3, result.polyDegree)
    assertEquals('output', result.response)
    assertEquals('input', result.predictor)
  }

  @Test
  void testParseFormulaHighDegree() {
    def result = GgStat.parseFormula('y ~ poly(x, 10)')
    assertEquals(10, result.polyDegree)
  }

  @Test
  void testSummaryMeanClNormal() {
    def data = Matrix.builder()
        .columnNames(['group', 'value'])
        .rows([
            ['A', 1],
            ['A', 2],
            ['A', 3],
            ['B', 2],
            ['B', 4],
            ['B', 6]
        ])
        .build()
    def aes = new Aes(x: 'group', y: 'value')

    def result = GgStat.summary(data, aes, ['fun.data': 'mean_cl_normal'])

    assertTrue(result.columnNames().containsAll(['group', 'value', 'ymin', 'ymax']))
    assertEquals(2, result.rowCount())

    def means = result['value'] as List<Number>
    def ymin = result['ymin'] as List<Number>
    def ymax = result['ymax'] as List<Number>

    assertEquals(2.0d, means[0] as double, 0.0001d)
    assertEquals(4.0d, means[1] as double, 0.0001d)
    assertTrue(ymin[0] <= means[0] && ymax[0] >= means[0])
    assertTrue(ymin[1] <= means[1] && ymax[1] >= means[1])
  }

  @Test
  void testSummaryMedianHiLow() {
    def data = Matrix.builder()
        .columnNames(['group', 'value'])
        .rows([
            ['A', 1],
            ['A', 2],
            ['A', 3],
            ['A', 4],
            ['B', 10],
            ['B', 20],
            ['B', 30],
            ['B', 40]
        ])
        .build()
    def aes = new Aes(x: 'group', y: 'value')

    def result = GgStat.summary(data, aes, [
        'fun.data': 'median_hilow',
        'fun.args': ['conf.int': 0.5d]
    ])

    assertTrue(result.columnNames().containsAll(['group', 'value', 'ymin', 'ymax']))
    assertEquals(2, result.rowCount())

    def medians = result['value'] as List<Number>
    def ymin = result['ymin'] as List<Number>
    def ymax = result['ymax'] as List<Number>

    assertEquals(2.5d, medians[0] as double, 0.0001d)
    assertEquals(25.0d, medians[1] as double, 0.0001d)
    assertEquals(1.75d, ymin[0] as double, 0.0001d)
    assertEquals(17.5d, ymin[1] as double, 0.0001d)
    assertEquals(3.25d, ymax[0] as double, 0.0001d)
    assertEquals(32.5d, ymax[1] as double, 0.0001d)
  }

  @Test
  void testSmoothDegreeWithFormulaNoPoly() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 1], [2, 4], [3, 9], [4, 16]])
        .build()
    def aes = new Aes(x: 'x', y: 'y')

    def result = GgStat.smooth(data, aes, [degree: 2, formula: 'y ~ x', se: false, n: 5])

    assertNotNull(result)
    assertEquals(['x', 'y'], result.columnNames())
    assertTrue(result.rowCount() > 0)
  }

  @Test
  void testCountWithFillGrouping() {
    def data = Matrix.builder()
        .columnNames(['class', 'drv'])
        .rows([
            ['compact', 'f'],
            ['compact', 'f'],
            ['compact', 'r'],
            ['suv', 'f'],
            ['suv', '4']
        ])
        .build()

    def aes = new Aes(x: 'class', fill: 'drv')
    def result = GgStat.count(data, aes)

    assertTrue(result.columnNames().containsAll(['class', 'drv', 'count', 'percent']))
    assertEquals(4, result.rowCount())

    Map<String, Integer> countByKey = [:]
    Map<String, BigDecimal> percentByKey = [:]
    List<Object> classes = result['class'] as List<Object>
    List<Object> drvs = result['drv'] as List<Object>
    List<Integer> counts = result['count'] as List<Integer>
    List<BigDecimal> percents = result['percent'] as List<BigDecimal>
    for (int i = 0; i < result.rowCount(); i++) {
      String key = "${classes[i]}|${drvs[i]}"
      countByKey[key] = counts[i]
      percentByKey[key] = percents[i]
    }

    BigDecimal sixtySix = new BigDecimal('66.67')
    BigDecimal thirtyThree = new BigDecimal('33.33')
    BigDecimal fifty = new BigDecimal('50.00')
    assertEquals(2, countByKey['compact|f'])
    assertEquals(1, countByKey['compact|r'])
    assertEquals(1, countByKey['suv|f'])
    assertEquals(1, countByKey['suv|4'])
    assertEquals(sixtySix, percentByKey['compact|f'])
    assertEquals(thirtyThree, percentByKey['compact|r'])
    assertEquals(fifty, percentByKey['suv|f'])
    assertEquals(fifty, percentByKey['suv|4'])
  }

  @Test
  void testDensityBasic() {
    def data = Matrix.builder()
        .columnNames(['value'])
        .rows([[1.2], [2.3], [1.8], [2.1], [3.5], [2.9], [2.0], [2.5], [1.9], [2.2]])
        .types(Double)
        .build()

    def aes = new Aes(x: 'value')
    def result = GgStat.density(data, aes)

    // Should have x and density columns
    assertTrue(result.columnNames().containsAll(['x', 'density']),
        "Result should have x and density columns, got: ${result.columnNames()}")

    // Should have default 512 evaluation points
    assertEquals(512, result.rowCount(), "Should have 512 evaluation points by default")

    // All density values should be non-negative
    List<Double> densities = result['density'] as List<Double>
    densities.each { d ->
      assertTrue(d >= 0, "Density should be non-negative")
    }

    // X values should be increasing
    List<Double> xVals = result['x'] as List<Double>
    for (int i = 1; i < xVals.size(); i++) {
      assertTrue(xVals[i] > xVals[i-1], "X values should be increasing")
    }
  }

  @Test
  void testDensityWithKernel() {
    def data = Matrix.builder()
        .columnNames(['value'])
        .rows([[1.0], [2.0], [3.0], [4.0], [5.0]])
        .types(Double)
        .build()

    def aes = new Aes(x: 'value')

    // Test with different kernels
    ['gaussian', 'epanechnikov', 'uniform', 'triangular'].each { kernel ->
      def result = GgStat.density(data, aes, [kernel: kernel, n: 100])
      assertEquals(100, result.rowCount(), "Should have 100 points for kernel: $kernel")
      assertTrue(result.columnNames().containsAll(['x', 'density']))
    }
  }

  @Test
  void testDensityWithAdjust() {
    def data = Matrix.builder()
        .columnNames(['value'])
        .rows([[1.0], [2.0], [3.0], [4.0], [5.0]])
        .types(Double)
        .build()

    def aes = new Aes(x: 'value')

    // Higher adjust = smoother density (lower peak)
    def result1 = GgStat.density(data, aes, [adjust: 0.5, n: 100])
    def result2 = GgStat.density(data, aes, [adjust: 2.0, n: 100])

    double max1 = (result1['density'] as List<Double>).max()
    double max2 = (result2['density'] as List<Double>).max()

    // Lower adjust should give higher peak (more detail)
    assertTrue(max1 > max2, "Lower adjust should give higher peak density")
  }

  @Test
  void testDensityGrouped() {
    def data = Matrix.builder()
        .columnNames(['value', 'group'])
        .rows([
            [1.0, 'A'], [2.0, 'A'], [3.0, 'A'], [2.5, 'A'], [1.5, 'A'],
            [5.0, 'B'], [6.0, 'B'], [7.0, 'B'], [6.5, 'B'], [5.5, 'B']
        ])
        .types(Double, String)
        .build()

    def aes = new Aes(x: 'value', color: 'group')
    def result = GgStat.density(data, aes, [n: 50])

    // Should have x, density, and group columns
    assertTrue(result.columnNames().containsAll(['x', 'density', 'group']),
        "Grouped result should have x, density, group columns")

    // Should have 50 points per group = 100 total
    assertEquals(100, result.rowCount(), "Should have 50 points per group")

    // Check that both groups are present
    def groups = result['group'] as List<Object>
    assertTrue(groups.contains('A'))
    assertTrue(groups.contains('B'))
  }

  @Test
  void testDensityWithCustomRange() {
    def data = Matrix.builder()
        .columnNames(['value'])
        .rows([[1.0], [2.0], [3.0], [4.0], [5.0]])
        .types(Double)
        .build()

    def aes = new Aes(x: 'value')
    def result = GgStat.density(data, aes, [from: 0.0, to: 6.0, n: 61])

    List<Double> xVals = result['x'] as List<Double>
    assertEquals(0.0, xVals[0], 0.001, "Should start at 'from' value")
    assertEquals(6.0, xVals[xVals.size()-1], 0.001, "Should end at 'to' value")
  }

  @Test
  void testDensityEmptyData() {
    def data = Matrix.builder()
        .columnNames(['value'])
        .rows([])
        .types(Double)
        .build()

    def aes = new Aes(x: 'value')
    def result = GgStat.density(data, aes)

    assertEquals(0, result.rowCount(), "Empty data should return empty result")
  }

  @Test
  void testDensityInsufficientData() {
    def data = Matrix.builder()
        .columnNames(['value'])
        .rows([[1.0]])
        .types(Double)
        .build()

    def aes = new Aes(x: 'value')
    def result = GgStat.density(data, aes)

    assertEquals(0, result.rowCount(), "Single point should return empty result")
  }

  @Test
  void testDensityRequiresXAesthetic() {
    def data = Matrix.builder()
        .columnNames(['value'])
        .rows([[1.0], [2.0]])
        .types(Double)
        .build()

    def aes = new Aes(y: 'value')  // Only y, no x

    def ex = assertThrows(IllegalArgumentException) {
      GgStat.density(data, aes)
    }
    assertTrue(ex.message.contains('x aesthetic'))
  }

  // ===== Tests for quantileType7 behavior (tested indirectly through boxplot) =====

  /**
   * Test quantile Type 7 calculation with known values.
   * Type 7 uses linear interpolation: quantile = (1-g)*x[j] + g*x[j+1]
   * where j = floor((n-1)*p + 1) and g is the fractional part.
   */
  @Test
  void testBoxplotQuartilesType7() {
    // Test data: 1, 2, 3, ..., 10
    // For n=10, Type 7 quantiles:
    // Q1 (p=0.25): h = (10-1)*0.25 + 1 = 3.25, j=3, g=0.25 → (1-0.25)*3 + 0.25*4 = 3.25
    // Q2 (p=0.5):  h = (10-1)*0.5 + 1 = 5.5, j=5, g=0.5 → (1-0.5)*5 + 0.5*6 = 5.5
    // Q3 (p=0.75): h = (10-1)*0.75 + 1 = 7.75, j=7, g=0.75 → (1-0.75)*7 + 0.75*8 = 7.75
    def data = Matrix.builder()
        .columnNames(['group', 'value'])
        .rows([
            ['A', 1], ['A', 2], ['A', 3], ['A', 4], ['A', 5],
            ['A', 6], ['A', 7], ['A', 8], ['A', 9], ['A', 10]
        ])
        .build()

    def aes = new Aes(x: 'group', y: 'value')
    def result = GgStat.boxplot(data, aes)

    assertEquals(1, result.rowCount())
    assertEquals(3.25d, result['lower'][0] as double, 0.0001d, 'Q1 should use Type 7 interpolation')
    assertEquals(5.5d, result['middle'][0] as double, 0.0001d, 'Median should use Type 7 interpolation')
    assertEquals(7.75d, result['upper'][0] as double, 0.0001d, 'Q3 should use Type 7 interpolation')
  }

  /**
   * Test quantile Type 7 with a single value (edge case).
   */
  @Test
  void testBoxplotSingleValue() {
    def data = Matrix.builder()
        .columnNames(['group', 'value'])
        .rows([['A', 5]])
        .build()

    def aes = new Aes(x: 'group', y: 'value')
    def result = GgStat.boxplot(data, aes)

    assertEquals(1, result.rowCount())
    // Single value: all quartiles should equal that value
    assertEquals(5d, result['lower'][0] as double, 0.0001d)
    assertEquals(5d, result['middle'][0] as double, 0.0001d)
    assertEquals(5d, result['upper'][0] as double, 0.0001d)
  }

  /**
   * Test quantile Type 7 with two values (interpolation edge case).
   */
  @Test
  void testBoxplotTwoValues() {
    def data = Matrix.builder()
        .columnNames(['group', 'value'])
        .rows([['A', 0], ['A', 10]])
        .build()

    def aes = new Aes(x: 'group', y: 'value')
    def result = GgStat.boxplot(data, aes)

    assertEquals(1, result.rowCount())
    // For n=2, Type 7: h = (2-1)*p + 1
    // Q1: h = 1.25, j=1, g=0.25 → 0 + 0.25*(10-0) = 2.5
    // Q2: h = 1.5, j=1, g=0.5 → 0 + 0.5*(10-0) = 5.0
    // Q3: h = 1.75, j=1, g=0.75 → 0 + 0.75*(10-0) = 7.5
    assertEquals(2.5d, result['lower'][0] as double, 0.0001d)
    assertEquals(5.0d, result['middle'][0] as double, 0.0001d)
    assertEquals(7.5d, result['upper'][0] as double, 0.0001d)
  }

  // ===== Tests for resolution behavior (tested indirectly through boxplot) =====

  /**
   * Test that xresolution is computed correctly for evenly spaced numeric x values.
   */
  @Test
  void testBoxplotXresolutionEvenlySpaced() {
    // X values: 1, 2, 3 (minimum difference = 1)
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([
            [1, 10], [1, 20],
            [2, 15], [2, 25],
            [3, 12], [3, 22]
        ])
        .build()

    def aes = new Aes(x: 'x', y: 'y')
    def result = GgStat.boxplot(data, aes)

    assertEquals(3, result.rowCount())
    // Each row should have xresolution = 1 (minimum spacing between x values)
    result.each { row ->
      assertEquals(1.0d, row['xresolution'] as double, 0.0001d,
          'xresolution should be minimum spacing between x values')
    }
  }

  /**
   * Test xresolution with irregular spacing.
   */
  @Test
  void testBoxplotXresolutionIrregularSpacing() {
    // X values: 0, 2, 10 (minimum difference = 2)
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([
            [0, 10], [0, 20],
            [2, 15], [2, 25],
            [10, 12], [10, 22]
        ])
        .build()

    def aes = new Aes(x: 'x', y: 'y')
    def result = GgStat.boxplot(data, aes)

    assertEquals(3, result.rowCount())
    // xresolution should be 2 (minimum positive difference)
    result.each { row ->
      assertEquals(2.0d, row['xresolution'] as double, 0.0001d,
          'xresolution should be minimum positive spacing')
    }
  }

  /**
   * Test xresolution with single x value (defaults to 1).
   */
  @Test
  void testBoxplotXresolutionSingleX() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([
            [5, 10], [5, 20], [5, 30]
        ])
        .build()

    def aes = new Aes(x: 'x', y: 'y')
    def result = GgStat.boxplot(data, aes)

    assertEquals(1, result.rowCount())
    // With single x value, resolution defaults to 1
    assertEquals(1.0d, result['xresolution'][0] as double, 0.0001d,
        'xresolution should default to 1 for single x value')
  }
}
