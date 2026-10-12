package regression

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.formula.ModelFrame
import se.alipsa.matrix.stats.formula.ModelFrameResult
import se.alipsa.matrix.stats.regression.FitRegistry
import se.alipsa.matrix.stats.regression.FitResult

@CompileStatic
class LmMethodTest {

  @Test
  void testAliasedCoefficientsFollowInputOrder() {
    List<BigDecimal> y = [3.1, 4.9, 7.2, 8.8, 11.1, 13.0]
    List<Integer> z = [1, 0, 1, 0, 1, 1]
    Matrix data = Matrix.builder().columnNames(['y', 'x', 'x2', 'z'])
      .rows((0..<6).collect { int i -> [y[i], i + 1, 2 * (i + 1), z[i]] } as List<List>)
      .types([BigDecimal, Integer, Integer, Integer]).build()
    FitResult result = FitRegistry.instance().get('lm').fit(ModelFrame.of('y ~ x + x2 + z', data).evaluate())
    // R 4.6.1: coef(lm(y ~ x + x2 + z)); x2=2*x.
    assertEquals([false, false, true, false], result.aliased)
    assertNull(result.coefficientList[2])
    assertNull(result.standardErrorList[2])
    assertEquals(0.921641791d, result.coefficients[0], 1e-8d)
    assertEquals(1.976119403d, result.coefficients[1], 1e-8d)
    assertEquals(0.267910448d, result.coefficients[3], 1e-8d)
    assertTrue(result.fittedValues.every { double value -> Double.isFinite(value) })
  }

  @Test
  void testLmSimpleRegression() {
    // y = 2x + 1 with some noise
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([
        [1.0, 3.1],
        [2.0, 4.9],
        [3.0, 7.2],
        [4.0, 8.8],
        [5.0, 11.1],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ x', data).evaluate()
    FitResult result = FitRegistry.instance().get('lm').fit(frame)

    // intercept + x = 2 coefficients
    assertEquals(2, result.coefficients.length)
    assertEquals(2, result.predictorNames.size())
    assertEquals('(Intercept)', result.predictorNames[0])
    assertEquals('x', result.predictorNames[1])
    // intercept ~ 1.0, slope ~ 2.0
    assertEquals(1.0, result.coefficients[0], 0.5)
    assertEquals(2.0, result.coefficients[1], 0.3)
    // R-squared should be high
    assertTrue(result.rSquared > 0.95)
    // fitted values and residuals have correct length
    assertEquals(5, result.fittedValues.length)
    assertEquals(5, result.residuals.length)
  }

  @Test
  void testLmNoIntercept() {
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([
        [1.0, 2.0],
        [2.0, 4.0],
        [3.0, 6.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    // y ~ x - 1 suppresses intercept
    ModelFrameResult frame = ModelFrame.of('y ~ x - 1', data).evaluate()
    FitResult result = FitRegistry.instance().get('lm').fit(frame)

    // no intercept: only 1 coefficient
    assertEquals(1, result.coefficients.length)
    assertEquals('x', result.predictorNames[0])
    assertEquals(2.0, result.coefficients[0], 0.01)
  }

  @Test
  void testLmMultipleRegression() {
    Matrix data = Matrix.builder()
      .columnNames(['x1', 'x2', 'y'])
      .rows([
        [1.0, 2.0, 5.0],
        [2.0, 1.0, 5.0],
        [3.0, 3.0, 9.0],
        [4.0, 2.0, 8.0],
        [5.0, 4.0, 13.0],
        [6.0, 1.0, 9.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ x1 + x2', data).evaluate()
    FitResult result = FitRegistry.instance().get('lm').fit(frame)

    // intercept + x1 + x2 = 3 coefficients
    assertEquals(3, result.coefficients.length)
    assertEquals('(Intercept)', result.predictorNames[0])
    assertEquals(6, result.fittedValues.length)
    assertEquals(6, result.residuals.length)
    assertEquals(3, result.standardErrors.length)
    assertTrue(result.rSquared > 0.8)
  }

  @Test
  void testLmRejectsWeights() {
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y', 'w'])
      .rows([
        [1.0, 3.0, 1.0],
        [2.0, 5.0, 2.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ x', data).weights('w').evaluate()

    UnsupportedOperationException ex = assertThrows(UnsupportedOperationException) {
      FitRegistry.instance().get('lm').fit(frame)
    }
    assertTrue(ex.message.contains('weights'))
  }

  @Test
  void testLmRejectsOffsets() {
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y', 'off'])
      .rows([
        [1.0, 3.0, 0.1],
        [2.0, 5.0, 0.2],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ x', data).offset('off').evaluate()

    UnsupportedOperationException ex = assertThrows(UnsupportedOperationException) {
      FitRegistry.instance().get('lm').fit(frame)
    }
    assertTrue(ex.message.contains('offsets'))
  }

}
