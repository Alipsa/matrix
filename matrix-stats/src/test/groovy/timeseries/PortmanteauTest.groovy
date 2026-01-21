package timeseries

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.timeseries.Portmanteau

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for the Ljung-Box portmanteau test.
 */
class PortmanteauTest {

  @Test
  void testWhiteNoise() {
    // White noise should show no significant autocorrelation
    List<Double> data = []
    Random rnd = new Random(42)
    for (int i = 0; i < 100; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = Portmanteau.ljungBox(data)

    assertNotNull(result, 'Result should not be null')
    assertEquals(100, result.sampleSize, 'Sample size')
    assertTrue(result.lags > 0, 'Should have positive lags')
    assertTrue(result.statistic >= 0, 'Q statistic should be non-negative')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be in [0, 1]')

    // White noise should typically have high p-value (fail to reject H0)
    // But we don't assert this strictly as it depends on random data
  }

  @Test
  void testAR1Process() {
    // AR(1) process with φ = 0.7 should show significant autocorrelation
    List<Double> data = []
    Random rnd = new Random(123)
    double y = 0.0
    for (int i = 0; i < 100; i++) {
      y = 0.7 * y + rnd.nextGaussian()
      data.add(y)
    }

    def result = Portmanteau.ljungBox(data, 10)

    assertNotNull(result, 'Result should not be null')
    assertEquals(10, result.lags, 'Should test 10 lags')
    assertEquals(10, result.degreesOfFreedom, 'df should equal lags for raw data')

    // AR(1) should show significant autocorrelation at lag 1
    assertTrue(Math.abs(result.getAutocorrelation(1)) > 0.1,
               'AR(1) should have non-trivial autocorrelation at lag 1')
  }

  @Test
  void testCustomLags() {
    List<Double> data = []
    Random rnd = new Random(456)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result5 = Portmanteau.ljungBox(data, 5)
    def result8 = Portmanteau.ljungBox(data, 8)

    assertEquals(5, result5.lags, 'Should test 5 lags')
    assertEquals(8, result8.lags, 'Should test 8 lags')
    assertEquals(5, result5.autocorrelations.size(), 'Should have 5 autocorrelations')
    assertEquals(8, result8.autocorrelations.size(), 'Should have 8 autocorrelations')
  }

  @Test
  void testAutoLagSelection() {
    List<Double> data = []
    Random rnd = new Random(789)
    for (int i = 0; i < 100; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = Portmanteau.ljungBox(data)

    assertTrue(result.lags > 0, 'Auto-selected lags should be positive')
    assertTrue(result.lags <= 20, 'Auto-selected lags should be reasonable')
    // For n=100, should be min(10, 100/5) = 10
    assertEquals(10, result.lags, 'For n=100, should select 10 lags')
  }

  @Test
  void testWithFitDf() {
    // Test with model fit degrees of freedom (e.g., ARMA(1,1) has fitdf=2)
    List<Double> data = []
    Random rnd = new Random(321)
    double y = 0.0
    for (int i = 0; i < 100; i++) {
      y = 0.5 * y + rnd.nextGaussian()
      data.add(y)
    }

    def result = Portmanteau.ljungBox(data, 10, 2)

    assertEquals(10, result.lags, 'Should test 10 lags')
    assertEquals(2, result.fitdf, 'Should have 2 fit df')
    assertEquals(8, result.degreesOfFreedom, 'df should be lags - fitdf = 10 - 2')
  }

  @Test
  void testValidation() {
    // Null data
    assertThrows(IllegalArgumentException) {
      Portmanteau.ljungBox(null)
    }

    // Empty data
    assertThrows(IllegalArgumentException) {
      Portmanteau.ljungBox([])
    }

    // Too few observations
    assertThrows(IllegalArgumentException) {
      Portmanteau.ljungBox([1.0, 2.0, 3.0, 4.0, 5.0])
    }

    // Generate valid data for remaining tests
    List<Double> validData = []
    Random rnd = new Random(42)
    for (int i = 0; i < 50; i++) {
      validData.add(rnd.nextGaussian())
    }

    // Invalid lags (zero or negative)
    assertThrows(IllegalArgumentException) {
      Portmanteau.ljungBox(validData, 0)
    }
    assertThrows(IllegalArgumentException) {
      Portmanteau.ljungBox(validData, -1)
    }

    // Too many lags
    assertThrows(IllegalArgumentException) {
      Portmanteau.ljungBox(validData, 50)
    }

    // Invalid fitdf (negative)
    assertThrows(IllegalArgumentException) {
      Portmanteau.ljungBox(validData, 5, -1)
    }

    // fitdf >= lags (would give non-positive df)
    assertThrows(IllegalArgumentException) {
      Portmanteau.ljungBox(validData, 5, 5)
    }
    assertThrows(IllegalArgumentException) {
      Portmanteau.ljungBox(validData, 5, 10)
    }
  }

  @Test
  void testConstantSeries() {
    // Constant series should be rejected
    List<Double> constantData = (1..50).collect { 5.0 }

    assertThrows(IllegalArgumentException) {
      Portmanteau.ljungBox(constantData)
    }
  }

  @Test
  void testResultToString() {
    List<Double> data = []
    Random rnd = new Random(654)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = Portmanteau.ljungBox(data, 5)

    String str = result.toString()
    assertTrue(str.contains('Ljung-Box'), 'Should contain test name')
    assertTrue(str.contains('Sample size'), 'Should contain sample size')
    assertTrue(str.contains('Q statistic'), 'Should contain test statistic')
    assertTrue(str.contains('p-value'), 'Should contain p-value')
    assertTrue(str.contains('Autocorrelations'), 'Should contain autocorrelations')
    assertTrue(str.contains('Lag'), 'Should show lag numbers')
  }

  @Test
  void testInterpret() {
    List<Double> data = []
    Random rnd = new Random(987)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = Portmanteau.ljungBox(data)

    String interpretation = result.interpret()
    assertNotNull(interpretation, 'Interpretation should not be null')
    assertTrue(interpretation.contains('H0'), 'Should mention null hypothesis')
  }

  @Test
  void testEvaluate() {
    List<Double> data = []
    Random rnd = new Random(147)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = Portmanteau.ljungBox(data)

    String evaluation = result.evaluate()
    assertNotNull(evaluation, 'Evaluation should not be null')
    assertTrue(evaluation.contains('Q statistic'), 'Should contain statistic info')
    assertTrue(evaluation.contains('Conclusion'), 'Should contain conclusion')
    assertTrue(evaluation.contains('significance'), 'Should mention significance level')
  }

  @Test
  void testGetAutocorrelation() {
    List<Double> data = []
    Random rnd = new Random(258)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = Portmanteau.ljungBox(data, 10)

    // Valid lags
    for (int lag = 1; lag <= 10; lag++) {
      double rho = result.getAutocorrelation(lag)
      assertTrue(rho >= -1 && rho <= 1, "Autocorrelation at lag ${lag} should be in [-1, 1]")
    }

    // Invalid lag (too small)
    assertThrows(IllegalArgumentException) {
      result.getAutocorrelation(0)
    }

    // Invalid lag (too large)
    assertThrows(IllegalArgumentException) {
      result.getAutocorrelation(11)
    }
  }

  @Test
  void testAutocorrelationProperties() {
    List<Double> data = []
    Random rnd = new Random(369)
    for (int i = 0; i < 100; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = Portmanteau.ljungBox(data, 20)

    // All autocorrelations should be in [-1, 1]
    for (int i = 0; i < result.autocorrelations.size(); i++) {
      double rho = result.autocorrelations[i]
      assertTrue(rho >= -1 && rho <= 1,
                 "Autocorrelation at lag ${i + 1} should be in [-1, 1], got ${rho}")
    }
  }

  @Test
  void testStrongAutocorrelation() {
    // Create series with strong positive autocorrelation
    List<Double> data = []
    Random rnd = new Random(741)
    double y = 0.0
    for (int i = 0; i < 100; i++) {
      y = 0.9 * y + rnd.nextGaussian() * 0.1  // Strong AR(1) with small noise
      data.add(y)
    }

    def result = Portmanteau.ljungBox(data, 10)

    // Should detect strong autocorrelation
    assertTrue(result.statistic > 10, 'Should have large Q statistic for strong autocorrelation')
    assertTrue(result.pValue < 0.05, 'Should reject H0 with strong autocorrelation')
  }

  @Test
  void testPValueRange() {
    // Test multiple random series to ensure p-values are always valid
    Random rnd = new Random(852)
    for (int trial = 0; trial < 10; trial++) {
      List<Double> data = []
      for (int i = 0; i < 50; i++) {
        data.add(rnd.nextGaussian())
      }

      def result = Portmanteau.ljungBox(data, 5)

      assertTrue(result.pValue >= 0, "p-value should be >= 0, got ${result.pValue}")
      assertTrue(result.pValue <= 1, "p-value should be <= 1, got ${result.pValue}")
    }
  }

  @Test
  void testDifferentAlpha() {
    List<Double> data = []
    Random rnd = new Random(963)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = Portmanteau.ljungBox(data)

    // Test different significance levels
    String interp05 = result.interpret(0.05)
    String interp01 = result.interpret(0.01)

    assertNotNull(interp05, 'Interpretation at 5% should not be null')
    assertNotNull(interp01, 'Interpretation at 1% should not be null')
  }

  @Test
  void testLongSeries() {
    // Test with a longer series
    List<Double> data = []
    Random rnd = new Random(159)
    for (int i = 0; i < 500; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = Portmanteau.ljungBox(data, 20)

    assertEquals(500, result.sampleSize, 'Sample size')
    assertEquals(20, result.lags, 'Should test 20 lags')
    assertNotNull(result, 'Should handle long series')
  }
}
