package formula

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.formula.Formula
import se.alipsa.matrix.stats.formula.FormulaParseException
import se.alipsa.matrix.stats.formula.dsl.GroovyFormulaDsl
import se.alipsa.matrix.stats.formula.dsl.TermRef

/**
 * Tests for the Groovy-native formula DSL core syntax.
 */
@SuppressWarnings(['DuplicateStringLiteral'])
class GroovyFormulaDslTest {

  @Test
  void testBuildsAdditiveFormula() {
    assertDslEquals('y ~ x + z') {
      y | x + z
    }
  }

  @Test
  void testBuildsNoInterceptFormula() {
    assertDslEquals('y ~ 0 + x') {
      y | noIntercept + x
    }
  }

  @Test
  void testNoInterceptParticipatesInPlusChaining() {
    assertDslEquals('y ~ 0 + x + group') {
      y | noIntercept + x + group
    }
  }

  @Test
  void testBuildsInteractionWithOperator() {
    assertDslEquals('y ~ x + group + x:group') {
      y | x + group + (x % group)
    }
  }

  @Test
  void testBuildsInteractionWithHelper() {
    assertDslEquals('y ~ x + group + x:group') {
      y | x + group + interaction(x, group)
    }
  }

  @Test
  void testBuildsThreeWayInteractionWithHelper() {
    assertDslEquals('y ~ x:group:z') {
      y | interaction(x, group, z)
    }
  }

  @Test
  void testBuildsPowerExpansion() {
    assertDslEquals('y ~ (a + b + c)^2') {
      y | (a + b + c) ** 2
    }
  }

  @Test
  void testRendersSingleTermPowerExpansionWithoutParens() {
    def spec = GroovyFormulaDsl.evaluate {
      y | x ** 2
    }

    assertEquals('y ~ x^2', spec.render())
  }

  @Test
  void testBuildsAllWithRemoval() {
    assertDslEquals('y ~ . - id') {
      y | all - id
    }
  }

  @Test
  void testBuildsQuotedColumnNames() {
    assertDslEquals('y ~ `gross margin` + `unit price`') {
      y | col('gross margin') + col('unit price')
    }
  }

  @Test
  void testBuildsTransformHelpers() {
    assertDslEquals('y ~ log(x) + sqrt(z) + exp(w)') {
      y | log(x) + sqrt(z) + exp(w)
    }
  }

  @Test
  void testBuildsIExpressionTerm() {
    assertDslEquals('y ~ I(x + 1)') {
      y | I { x + 1 }
    }
  }

  @Test
  void testBuildsIExpressionWithLeftNumericLiteral() {
    assertDslEquals('y ~ I(1 + x)') {
      y | I { 1 + x }
    }
  }

  @Test
  void testBuildsPolynomialHelper() {
    assertDslEquals('y ~ poly(x, 3)') {
      y | poly(x, 3)
    }
  }

  @Test
  void testBuildsSmoothHelper() {
    assertDslEquals('y ~ s(time, 6)') {
      y | smooth(time, 6)
    }
  }

  @Test
  void testBuildsSmoothAlias() {
    assertDslEquals('y ~ s(time, 6)') {
      y | s(time, 6)
    }
  }

  @Test
  void testOperatorPrecedenceGroupsRightHandSideAdditionBeforeFormulaSeparator() {
    assertDslEquals('y ~ x + group') {
      y | x + group
    }
  }

  @Test
  void testOperatorPrecedenceGroupsNoInterceptAdditionBeforeFormulaSeparator() {
    assertDslEquals('y ~ 0 + x') {
      y | noIntercept + x
    }
  }

  @Test
  void testRejectsNullFormulaClosure() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Formula.build(null)
    }

    assertTrue(exception.message.contains('Formula closure cannot be null'))
  }

  @Test
  void testRejectsClosureThatDoesNotReturnFormulaSpec() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Formula.build {
        x + z
      }
    }

    assertTrue(exception.message.contains('Formula closure must return a Groovy formula expression'))
  }

  @Test
  void testRejectsEmptyInteraction() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Formula.build {
        y | interaction()
      }
    }

    assertTrue(exception.message.contains('interaction(...) requires at least two terms'))
  }

  @Test
  void testRejectsOneArgumentInteraction() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Formula.build {
        y | interaction(x)
      }
    }

    assertTrue(exception.message.contains('interaction(...) requires at least two terms'))
  }

  @Test
  void testRejectsUnsupportedZeroPlusTermSyntax() {
    MissingMethodException exception = assertThrows(MissingMethodException) {
      Formula.build {
        y | 0 + x
      }
    }

    assertEquals('plus', exception.method)
    assertEquals(Integer, exception.type)
    assertEquals(TermRef, exception.arguments[0].class)
  }

  @Test
  void testRejectsTransformedResponseInDsl() {
    FormulaParseException exception = assertThrows(FormulaParseException) {
      Formula.build {
        log(y) | x
      }
    }

    assertEquals('Transformed responses are not supported: log(y) | x. Move transforms to the predictor side.', exception.message)
  }

  @Test
  void testRejectsNonVariableResponseInDslUsingPipeSyntax() {
    FormulaParseException exception = assertThrows(FormulaParseException) {
      Formula.build {
        (x + y) | z
      }
    }

    assertEquals('Response must be a single variable in the Groovy formula DSL: x + y | z', exception.message)
  }

  @Test
  void testRejectsMalformedIExpressionClosure() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Formula.build {
        y | I { null }
      }
    }

    assertEquals('I { ... } must return an arithmetic expression such as x + 1', exception.message)
  }

  @Test
  void testRejectsUnsupportedArithmeticOperatorInIExpression() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      Formula.build {
        y | I { x % 2 }
      }
    }

    assertEquals("Unsupported arithmetic operator '%' in I { ... }", exception.message)
  }

  @Test
  void testDslCanBeEvaluatedDirectlyForRenderedFormula() {
    def spec = GroovyFormulaDsl.evaluate {
      y | x + group + interaction(x, group)
    }

    assertEquals('y ~ x + group + x:group', spec.render())
  }

  private static void assertDslEquals(String equivalentFormula, Closure<?> dsl) {
    assertEquals(Formula.normalize(equivalentFormula).asFormulaString(), Formula.build(dsl).asFormulaString())
  }
}
