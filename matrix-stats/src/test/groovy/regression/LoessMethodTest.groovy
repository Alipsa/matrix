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
  void testDirectQuadraticLoessAgainstR() {
    List<BigDecimal> y = [2.1, 3.9, 6.2, 7.8, 10.1, 12.2, 13.8, 16.1, 18.2, 19.9, 22.1, 24.0]
    Matrix data = Matrix.builder().columnNames(['x', 'y'])
      .rows((0..<12).collect { int i -> [i + 1, y[i]] }).types([Integer, BigDecimal]).build()
    // R 4.6.1: predict(loess(y ~ x, span=s, degree=2, surface="direct"))[1:4].
    [0.5d: [2.06271888d, 4.03179696d, 5.99890548d, 7.97789131d],
     0.75d: [2.05362005d, 4.02989285d, 6.02134370d, 8.02866459d]].each { double span, List<Double> expected ->
      def result = new LoessMethod().fit(ModelFrame.of('y ~ x', data).evaluate(), new LoessOptions(span))
      expected.eachWithIndex { double value, int i -> assertEquals(value, result.fittedValues[i], 1e-6d) }
    }
  }

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
    assertTrue(maxResidual < 1.0, 'Residuals should be bounded for smooth data')
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
    assertEquals(0, result.coefficients.length)
    assertEquals(0, result.standardErrors.length)
    assertTrue(result.predictorNames.isEmpty())
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

  @Test
  void testLoessUsesNearestNeighborsForUnevenSpacing() {
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([
        [0.0, 0.0],
        [0.1, 0.1],
        [0.2, 0.2],
        [10.0, 100.0],
        [10.1, 101.0],
      ])
      .types([BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ x', data).evaluate()
    FitResult result = FitRegistry.instance().get('loess').fit(frame, new LoessOptions(0.6d))

    assertEquals(5, result.fittedValues.length)
    assertTrue(result.fittedValues[2] < 1.0d,
      "Fit near x=0.2 should be driven by nearby points, got ${result.fittedValues[2]}")
  }

  @Test
  void testLoessRejectsOffsets() {
    Matrix data = Matrix.builder()
      .columnNames(['x', 'y', 'off'])
      .rows([
        [1.0, 3.0, 0.1],
        [2.0, 5.0, 0.2],
        [3.0, 7.0, 0.3],
      ])
      .types([BigDecimal, BigDecimal, BigDecimal])
      .build()

    ModelFrameResult frame = ModelFrame.of('y ~ x', data).offset('off').evaluate()
    UnsupportedOperationException ex = assertThrows(UnsupportedOperationException) {
      FitRegistry.instance().get('loess').fit(frame)
    }

    assertTrue(ex.message.contains('offsets'))
  }

  @Test
  void testLoessOptionsAcceptNumberSpan() {
    LoessOptions options = new LoessOptions(0.3G, 2)

    assertEquals(0.3d, options.span, 1e-10d)
    assertEquals(2, options.degree)
  }


  @Test
  void fractionalSpanAndBoundaryTiesMatchRDirectSurface() {
    Matrix data = Matrix.builder().columns([x: [1,1,2,2,3,3,4,4,5,5,6,6],
      y: [2.1,1.9,4.1,3.8,6.2,5.9,8.1,7.8,10.2,9.9,12.1,11.8]]).types(Integer, BigDecimal).build()
    // R 4.6.1 loess(y~x,span=.61,degree=2,control=loess.control(surface='direct'))$fitted.
    def fit = new LoessMethod().fit(ModelFrame.of('y~x', data).evaluate(), new LoessOptions(0.61, 2))
    assertArrayEquals([2d,2d,3.95d,3.95d,6.05d,6.05d,7.95d,7.95d,10.05d,10.05d,11.95d,11.95d] as double[], fit.fittedValues, 1e-9)
  }
}
