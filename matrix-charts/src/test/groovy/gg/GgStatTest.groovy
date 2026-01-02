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
}
