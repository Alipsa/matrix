package regression

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileDynamic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.formula.ModelFrame
import se.alipsa.matrix.stats.formula.ModelFrameResult
import se.alipsa.matrix.stats.regression.FitRegistry
import se.alipsa.matrix.stats.regression.FitResult
import se.alipsa.matrix.stats.regression.GamOptions

@CompileDynamic
class GamMethodTest {

  @Test
  void testGamWithSmoothTerms() {
    // Nonlinear relationship: y = sin(x) + noise
    List<List> rows = (0..<30).collect { int i ->
      BigDecimal x = i * 0.2
      BigDecimal y = (Math.sin(x as double) as BigDecimal) + (i % 5 == 0 ? 0.1 : -0.05)
      [x, y]
    }
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows(rows)
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ s(x)', data).evaluate()
    FitResult result = FitRegistry.instance().get('gam').fit(frame)

    assertEquals(30, result.fittedValues.length)
    assertEquals(30, result.residuals.length)
    // GAM should fit nonlinear data well
    assertTrue(result.rSquared > 0.8, "R-squared should be > 0.8 for sine data, got ${result.rSquared}")
  }

  @Test
  void testGamWithMixedTerms() {
    // y = 2*z + sin(x)
    List<List> rows = (0..<30).collect { int i ->
      BigDecimal x = i * 0.2
      BigDecimal z = i * 0.5
      BigDecimal y = (2.0 * z) + (Math.sin(x as double) as BigDecimal)
      [x, z, y]
    }
    Matrix data = Matrix.builder()
      .columnNames(['x', 'z', 'y'])
      .rows(rows)
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    // Mix of smooth and linear terms
    ModelFrameResult frame = ModelFrame.of('y ~ s(x) + z', data).evaluate()
    FitResult result = FitRegistry.instance().get('gam').fit(frame)

    // Should have intercept + 4 spline basis + z = 6 coefficients
    assertEquals(frame.data.columnCount() + 1, result.coefficients.length) // +1 for intercept
    assertEquals(result.coefficients.length, result.standardErrors.length)
    assertEquals(frame.response.size(), result.fittedValues.length)
    assertEquals(frame.response.size(), result.residuals.length)
    assertTrue(result.rSquared > 0.95)
  }

  @Test
  void testGamCustomLambda() {
    List<List> rows = (1..20).collect { int i ->
      [i as BigDecimal, (i * i) as BigDecimal]
    }
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows(rows)
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ s(x)', data).evaluate()
    FitResult lowPenalty = FitRegistry.instance().get('gam').fit(frame, new GamOptions(0.001d))
    FitResult highPenalty = FitRegistry.instance().get('gam').fit(frame, new GamOptions(500.0d))

    // Higher penalty -> smoother -> lower R-squared
    assertTrue(lowPenalty.rSquared >= highPenalty.rSquared,
      "Lower lambda should give equal or higher R-squared: ${lowPenalty.rSquared} vs ${highPenalty.rSquared}")
  }

  @Test
  void testGamSupportsLinearAndSmoothForSameVariable() {
    List<List> rows = (0..<30).collect { int i ->
      BigDecimal x = i * 0.2
      BigDecimal y = (3.0 * x) + (Math.sin(x as double) as BigDecimal)
      [x, y]
    }
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows(rows)
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ x + s(x)', data).evaluate()
    FitResult result = FitRegistry.instance().get('gam').fit(frame)

    assertEquals(frame.data.columnCount() + 1, result.coefficients.length)
    assertTrue(result.rSquared > 0.95d, "Model should fit combined linear and smooth effects, got ${result.rSquared}")
  }

  @Test
  void testGamRejectsWeights() {
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y', 'w'])
      .rows([
        [1.0, 2.0, 1.0],
        [2.0, 5.0, 2.0],
        [3.0, 10.0, 1.0],
        [4.0, 17.0, 2.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ s(x)', data).weights('w').evaluate()
    UnsupportedOperationException ex = assertThrows(UnsupportedOperationException) {
      FitRegistry.instance().get('gam').fit(frame)
    }

    assertTrue(ex.message.contains('weights'))
  }

  @Test
  void testGamRejectsOffsets() {
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y', 'off'])
      .rows([
        [1.0, 2.0, 0.1],
        [2.0, 5.0, 0.2],
        [3.0, 10.0, 0.3],
        [4.0, 17.0, 0.4],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ s(x)', data).offset('off').evaluate()
    UnsupportedOperationException ex = assertThrows(UnsupportedOperationException) {
      FitRegistry.instance().get('gam').fit(frame)
    }

    assertTrue(ex.message.contains('offsets'))
  }

  @Test
  void testGamOptionsAcceptNumberLambda() {
    GamOptions options = new GamOptions(1.5G)

    assertEquals(1.5d, options.lambda, 1e-10d)
  }

}
