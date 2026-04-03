package regression

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileDynamic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.formula.ModelFrame
import se.alipsa.matrix.stats.formula.ModelFrameResult
import se.alipsa.matrix.stats.regression.FitRegistry
import se.alipsa.matrix.stats.regression.FitResult
import se.alipsa.matrix.stats.regression.LoessMethod
import se.alipsa.matrix.stats.regression.LoessOptions

@CompileDynamic
class LoessMethodTest {

  @Test
  void testLoessSmooth() {
    // sin curve with noise — loess should smooth it
    List<List> rows = (0..<50).collect { int i ->
      BigDecimal x = i * 0.1
      BigDecimal y = (Math.sin(x as double) as BigDecimal) + (i % 3 == 0 ? 0.2 : -0.1)
      [x, y]
    }
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows(rows)
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ x', data).evaluate()
    FitResult result = FitRegistry.instance().get('loess').fit(frame, new LoessOptions(0.3d))

    assertEquals(50, result.fittedValues.length)
    assertEquals(50, result.residuals.length)
    // fitted values should be smoother than raw — check that residuals exist
    double maxResidual = 0.0
    for (double r : result.residuals) {
      maxResidual = Math.max(maxResidual, Math.abs(r))
    }
    assertTrue(maxResidual < 1.0, "Residuals should be bounded for smooth data")
  }

  @Test
  void testLoessDefaultSpan() {
    List<List> rows = (1..20).collect { int i ->
      [i as BigDecimal, (2.0 * i + 1.0) as BigDecimal]
    }
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows(rows)
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ x', data).evaluate()
    // no params -> default span = 0.75
    FitResult result = FitRegistry.instance().get('loess').fit(frame)

    assertEquals(20, result.fittedValues.length)
    // linear data: loess should fit well
    assertTrue(result.rSquared > 0.99)
  }

  @Test
  void testLoessRespectsWeights() {
    // Point at x=1 is an outlier (y=10 vs trend ~4-12). Heavy weight on that
    // point should pull the fitted value at x=1 closer to 10 compared to
    // uniform weights which would smooth it out.
    List<List> rows = [
      [1.0, 10.0, 10.0], // outlier, heavily weighted
      [2.0, 4.0, 0.1],
      [3.0, 6.0, 0.1],
      [4.0, 8.0, 0.1],
      [5.0, 10.0, 0.1],
      [6.0, 12.0, 0.1],
    ]
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y', 'w'])
      .rows(rows)
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    // Weighted fit
    ModelFrameResult weightedFrame = ModelFrame.of('y ~ x', data).weights('w').evaluate()
    FitResult weighted = (FitRegistry.instance().get('loess') as LoessMethod).fit(weightedFrame, new LoessOptions(0.75d))

    // Unweighted fit (no weights column)
    Matrix unwData = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows(rows.collect { [it[0], it[1]] })
      .types([BigDecimal, BigDecimal])
      .build()
    ModelFrameResult unwFrame = ModelFrame.of('y ~ x', unwData).evaluate()
    FitResult unweighted = (FitRegistry.instance().get('loess') as LoessMethod).fit(unwFrame, new LoessOptions(0.75d))

    assertEquals(6, weighted.fittedValues.length)
    // Weighted fit at x=1 should be closer to 10 than unweighted
    assertTrue(Math.abs(weighted.fittedValues[0] - 10.0) < Math.abs(unweighted.fittedValues[0] - 10.0),
      "Weighted fit at x=1 (${weighted.fittedValues[0]}) should be closer to 10 than unweighted (${unweighted.fittedValues[0]})")
  }

  @Test
  void testLoessRejectsMultiplePredictors() {
    Matrix data = Matrix.builder()
      .columnNames(['x1', 'x2', 'y'])
      .rows([
        [1.0, 2.0, 3.0],
        [2.0, 3.0, 5.0],
        [3.0, 4.0, 7.0],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ x1 + x2', data).evaluate()
    assertThrows(IllegalArgumentException) {
      FitRegistry.instance().get('loess').fit(frame)
    }
  }
}
