package formula

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertInstanceOf
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.formula.Formula
import se.alipsa.matrix.stats.formula.FormulaExpression
import se.alipsa.matrix.stats.formula.FormulaParseException
import se.alipsa.matrix.stats.formula.ParsedFormula

/**
 * Tests for formula parsing, normalization, and update helpers.
 */
@CompileStatic
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral'])
class FormulaTest {

  @Test
  void testParsesTransformsAndQuotedNames() {
    ParsedFormula parsed = Formula.parse('`gross margin` ~ log(x) + sqrt(y) + exp(z) + poly(w, 2) + I(a + b)')

    assertEquals('`gross margin`', parsed.response.asFormulaString())
    assertEquals('log(x) + sqrt(y) + exp(z) + poly(w, 2) + I(a + b)', parsed.predictors.asFormulaString())

    def normalized = parsed.normalize()
    assertEquals('`gross margin` ~ 1 + I(a + b) + exp(z) + log(x) + poly(w, 2) + sqrt(y)', normalized.asFormulaString())
  }

  @Test
  void testNormalizesInteractionsAndInterceptControl() {
    def normalized = Formula.normalize('y ~ 0 + x * z')

    assertEquals('y ~ 0 + x + z + x:z', normalized.asFormulaString())
  }

  @Test
  void testNormalizesPowerExpansionAndSubtraction() {
    def normalized = Formula.normalize('y ~ (a + b + c)^2 - b:c')

    assertEquals('y ~ 1 + a + b + c + a:b + a:c', normalized.asFormulaString())
  }

  @Test
  void testNormalizesNestingOperator() {
    def normalized = Formula.normalize('y ~ a / b')

    assertEquals('y ~ 1 + a + a:b', normalized.asFormulaString())
  }

  @Test
  void testPreservesDotForLaterModelFrameExpansion() {
    def normalized = Formula.normalize('y ~ . + x')

    assertEquals('y ~ 1 + . + x', normalized.asFormulaString())
  }

  @Test
  void testDefaultsPredictorOnlyFormulaToResponse() {
    def normalized = Formula.normalize('x + z', 'y')

    assertEquals('y ~ 1 + x + z', normalized.asFormulaString())
  }

  @Test
  void testUpdatesFormulaWithDotSubstitution() {
    ParsedFormula updated = Formula.update('y ~ x + z', '. ~ . - z + w')

    assertEquals('y ~ x + z - z + w', updated.toString())
    assertEquals('y ~ 1 + w + x', updated.normalize().asFormulaString())
  }

  @Test
  void testRejectsMultipleResponses() {
    FormulaParseException exception = assertThrows(FormulaParseException) {
      Formula.normalize('y1 + y2 ~ x')
    }

    assertTrue(exception.message.contains('Multiple responses are not supported'))
  }

  @Test
  void testRejectsMissingFormulaClosingParenthesisWithLocation() {
    FormulaParseException exception = assertThrows(FormulaParseException) {
      Formula.parse('y ~ log(x')
    }

    assertTrue(exception.message.contains("Expected ')'"))
    assertTrue(exception.message.contains('position'))
  }

  @Test
  void testProducesStructuredAst() {
    ParsedFormula parsed = Formula.parse('y ~ x:z')

    FormulaExpression.Binary predictors = assertInstanceOf(FormulaExpression.Binary, parsed.predictors)
    assertEquals(':', predictors.operator)
    assertEquals('x', predictors.left.asFormulaString())
    assertEquals('z', predictors.right.asFormulaString())
  }
}
