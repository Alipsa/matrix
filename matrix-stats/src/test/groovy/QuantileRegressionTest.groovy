import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.regression.QuantileRegression

import static org.junit.jupiter.api.Assertions.*

class QuantileRegressionTest {

  @Test
  void testMedianRegression() {
    // Simple linear data: y = 2x + 1 with some noise
    def x = [1, 2, 3, 4, 5]
    def y = [3.1, 4.9, 7.2, 8.8, 11.1]

    def qr = new QuantileRegression(x, y, 0.5)

    assertNotNull(qr.slope)
    assertNotNull(qr.intercept)
    assertEquals(0.5, qr.tau as double, 0.001)

    // Slope should be close to 2
    assertEquals(2.0, qr.slope as double, 0.5)

    // Intercept should be close to 1
    assertEquals(1.0, qr.intercept as double, 1.0)

    // Test prediction
    BigDecimal pred = qr.predict(3)
    assertTrue(pred > 6.0 && pred < 8.0, "Prediction at x=3 should be around 7, got ${pred}")
  }

  @Test
  void testUpperQuartileRegression() {
    // Data with asymmetric noise - upper quartile should have higher intercept
    def x = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    def y = [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]

    def qr75 = new QuantileRegression(x, y, 0.75)
    def qr50 = new QuantileRegression(x, y, 0.5)

    assertNotNull(qr75.slope)
    assertNotNull(qr75.intercept)
    assertEquals(0.75, qr75.tau as double, 0.001)

    // For this perfect linear data, all quantiles should have same slope (≈2)
    assertEquals(2.0, qr75.slope as double, 0.1)
    assertEquals(2.0, qr50.slope as double, 0.1)

    // But with asymmetric noise, upper quantile would have different intercept
    // For perfect data, they should be similar
    assertEquals(qr50.intercept as double, qr75.intercept as double, 1.0)
  }

  @Test
  void testLowerQuartileRegression() {
    def x = [1, 2, 3, 4, 5]
    def y = [2, 4, 6, 8, 10]

    def qr25 = new QuantileRegression(x, y, 0.25)

    assertNotNull(qr25.slope)
    assertNotNull(qr25.intercept)
    assertEquals(0.25, qr25.tau as double, 0.001)

    // Slope should be close to 2
    assertEquals(2.0, qr25.slope as double, 0.1)
  }

  @Test
  void testExtremeTau() {
    def x = [1, 2, 3, 4, 5]
    def y = [2, 4, 6, 8, 10]

    // Test tau = 0.01 (1st percentile)
    def qr01 = new QuantileRegression(x, y, 0.01)
    assertNotNull(qr01.slope)
    assertNotNull(qr01.intercept)

    // Test tau = 0.99 (99th percentile)
    def qr99 = new QuantileRegression(x, y, 0.99)
    assertNotNull(qr99.slope)
    assertNotNull(qr99.intercept)

    // Both should produce reasonable results for this simple data
    assertTrue(qr01.slope > 0)
    assertTrue(qr99.slope > 0)
  }

  @Test
  void testPrediction() {
    def x = [1, 2, 3, 4, 5]
    def y = [2, 4, 6, 8, 10]

    def qr = new QuantileRegression(x, y, 0.5)

    // Test single prediction
    BigDecimal pred = qr.predict(3)
    assertNotNull(pred)
    assertEquals(6.0, pred as double, 1.0)

    // Test single prediction with rounding
    BigDecimal predRounded = qr.predict(3, 2)
    assertNotNull(predRounded)
    assertEquals(2, predRounded.scale())

    // Test list prediction
    List<BigDecimal> preds = qr.predict([1, 2, 3, 4, 5])
    assertEquals(5, preds.size())
    assertNotNull(preds[0])

    // Test list prediction with rounding
    List<BigDecimal> predsRounded = qr.predict([1, 2, 3], 2)
    assertEquals(3, predsRounded.size())
    predsRounded.each { p ->
      assertEquals(2, p.scale())
    }
  }

  @Test
  void testFromMatrix() {
    def data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([[1, 2], [2, 4], [3, 6], [4, 8], [5, 10]])
      .build()

    def qr = new QuantileRegression(data, 'x', 'y', 0.5)

    assertNotNull(qr.slope)
    assertNotNull(qr.intercept)
    assertEquals('x', qr.x)
    assertEquals('y', qr.y)

    // Slope should be close to 2
    assertEquals(2.0, qr.slope as double, 0.1)
  }

  @Test
  void testInvalidTauZero() {
    def x = [1, 2, 3, 4, 5]
    def y = [2, 4, 6, 8, 10]

    Exception exception = assertThrows(IllegalArgumentException.class, {
      new QuantileRegression(x, y, 0.0)
    })

    assertTrue(exception.message.contains("must be in the open interval (0, 1)"))
  }

  @Test
  void testInvalidTauOne() {
    def x = [1, 2, 3, 4, 5]
    def y = [2, 4, 6, 8, 10]

    Exception exception = assertThrows(IllegalArgumentException.class, {
      new QuantileRegression(x, y, 1.0)
    })

    assertTrue(exception.message.contains("must be in the open interval (0, 1)"))
  }

  @Test
  void testInvalidTauNegative() {
    def x = [1, 2, 3, 4, 5]
    def y = [2, 4, 6, 8, 10]

    Exception exception = assertThrows(IllegalArgumentException.class, {
      new QuantileRegression(x, y, -0.5)
    })

    assertTrue(exception.message.contains("must be in the open interval (0, 1)"))
  }

  @Test
  void testInvalidTauGreaterThanOne() {
    def x = [1, 2, 3, 4, 5]
    def y = [2, 4, 6, 8, 10]

    Exception exception = assertThrows(IllegalArgumentException.class, {
      new QuantileRegression(x, y, 1.5)
    })

    assertTrue(exception.message.contains("must be in the open interval (0, 1)"))
  }

  @Test
  void testInsufficientData() {
    def x = [1]
    def y = [2]

    Exception exception = assertThrows(IllegalArgumentException.class, {
      new QuantileRegression(x, y, 0.5)
    })

    assertTrue(exception.message.contains("Need at least 2 data points"))
  }

  @Test
  void testUnequalLengths() {
    def x = [1, 2, 3]
    def y = [2, 4]

    Exception exception = assertThrows(IllegalArgumentException.class, {
      new QuantileRegression(x, y, 0.5)
    })

    assertTrue(exception.message.contains("Must have equal number"))
  }

  @Test
  void testToString() {
    def x = [1, 2, 3, 4, 5]
    def y = [2, 4, 6, 8, 10]

    def qr = new QuantileRegression(x, y, 0.5)

    String str = qr.toString()
    assertNotNull(str)
    assertTrue(str.contains("Y"))
    assertTrue(str.contains("X"))
    assertTrue(str.contains("τ=0.5") || str.contains("τ=0.50"))
  }

  @Test
  void testGetSlopeWithDecimals() {
    def x = [1, 2, 3, 4, 5]
    def y = [2.123, 4.456, 6.789, 8.111, 10.222]

    def qr = new QuantileRegression(x, y, 0.5)

    // Test with rounding
    BigDecimal slope2 = qr.getSlope(2)
    assertEquals(2, slope2.scale())

    // Test without rounding (default)
    BigDecimal slopeDefault = qr.getSlope()
    assertNotNull(slopeDefault)

    // Test with -1 (no rounding)
    BigDecimal slopeNoRound = qr.getSlope(-1)
    assertEquals(slopeDefault, slopeNoRound)
  }

  @Test
  void testGetInterceptWithDecimals() {
    def x = [1, 2, 3, 4, 5]
    def y = [2.123, 4.456, 6.789, 8.111, 10.222]

    def qr = new QuantileRegression(x, y, 0.5)

    // Test with rounding
    BigDecimal intercept2 = qr.getIntercept(2)
    assertEquals(2, intercept2.scale())

    // Test without rounding (default)
    BigDecimal interceptDefault = qr.getIntercept()
    assertNotNull(interceptDefault)

    // Test with -1 (no rounding)
    BigDecimal interceptNoRound = qr.getIntercept(-1)
    assertEquals(interceptDefault, interceptNoRound)
  }

  @Test
  void testTwoPoints() {
    // With only two points, there should be a unique solution
    def x = [1, 5]
    def y = [2, 10]

    def qr = new QuantileRegression(x, y, 0.5)

    assertNotNull(qr.slope)
    assertNotNull(qr.intercept)

    // The line should pass through or near both points
    // slope = (10 - 2) / (5 - 1) = 2
    assertEquals(2.0, qr.slope as double, 0.1)

    // intercept = y - slope * x = 2 - 2*1 = 0
    assertEquals(0.0, qr.intercept as double, 0.5)
  }

  @Test
  void testCompareWithLmForSymmetricData() {
    // For symmetric data, median regression (tau=0.5) should approximate OLS
    def x = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    def y = [2, 4, 6, 8, 10, 12, 14, 16, 18, 20]

    def qr50 = new QuantileRegression(x, y, 0.5)

    // For perfect linear data, quantile regression should match OLS
    // y = 2x, so slope = 2, intercept = 0
    assertEquals(2.0, qr50.slope as double, 0.1)
    assertEquals(0.0, qr50.intercept as double, 0.5)
  }

  @Test
  void testAsymmetricOutliers() {
    // Create data with positive outliers - upper quantile should be higher
    def x = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    def y = [2, 4, 6, 8, 10, 12, 14, 16, 20, 25] // Last two points are outliers

    def qr25 = new QuantileRegression(x, y, 0.25)
    def qr50 = new QuantileRegression(x, y, 0.50)
    def qr75 = new QuantileRegression(x, y, 0.75)

    // All should have positive slopes
    assertTrue(qr25.slope > 0)
    assertTrue(qr50.slope > 0)
    assertTrue(qr75.slope > 0)

    // Upper quantile should have higher slope due to outliers
    assertTrue(qr75.slope >= qr50.slope,
      "Upper quantile should have higher slope. q75=${qr75.slope}, q50=${qr50.slope}")
  }
}
