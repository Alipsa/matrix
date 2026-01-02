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
}
