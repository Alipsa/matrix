package gg

import org.junit.jupiter.api.Test
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
    // Formula without ~ should use the whole string as predictor
    def result = GgStat.parseFormula('x')
    assertEquals(1, result.polyDegree)
    assertEquals('x', result.response)  // First part becomes response
    assertEquals('x', result.predictor)  // Falls back to 'x'
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
}
