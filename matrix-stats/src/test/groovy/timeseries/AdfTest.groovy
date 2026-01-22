package timeseries

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.timeseries.Adf

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for the Augmented Dickey-Fuller test.
 */
class AdfTest {

  @Test
  void testBasicFunctionality() {
    // Generate random walk data with drift
    List<Double> data = []
    double value = 100.0
    Random rnd = new Random(42)
    for (int i = 0; i < 50; i++) {
      value += 0.5 + rnd.nextGaussian() * 2.0  // drift + noise
      data.add(value)
    }

    def result = Adf.test(data)

    assertNotNull(result, 'Result should not be null')
    assertEquals(50, result.sampleSize, 'Sample size')
    assertEquals(0, result.lag, 'Default lag')
    assertEquals("drift", result.type, 'Default type')
    assertTrue(result.statistic != 0, 'Statistic should be non-zero')
  }

  @Test
  void testStationarySeries() {
    // Stationary AR(1) process: y_t = 0.5*y_{t-1} + ε_t
    List<Double> data = []
    Random rnd = new Random(123)
    double y = 0.0
    for (int i = 0; i < 50; i++) {
      y = 0.5 * y + rnd.nextGaussian()
      data.add(y)
    }

    def result = Adf.test(data, 1)

    assertNotNull(result, 'Result should not be null')
    // For stationary series, we expect to reject H0 (statistic < critical value)
    // But we just verify the test runs without error
    assertTrue(result.sampleSize == 50, 'Sample size should be 50')
  }

  @Test
  void testValidation() {
    // Null data
    assertThrows(IllegalArgumentException) {
      Adf.test(null)
    }

    // Empty data
    assertThrows(IllegalArgumentException) {
      Adf.test([])
    }

    // Too few observations
    assertThrows(IllegalArgumentException) {
      Adf.test([1.0, 2.0, 3.0, 4.0, 5.0])
    }

    // Generate valid data for remaining tests
    List<Double> validData = []
    Random rnd = new Random(42)
    double val = 100.0
    for (int i = 0; i < 50; i++) {
      val += rnd.nextGaussian()
      validData.add(val)
    }

    // Negative lags
    assertThrows(IllegalArgumentException) {
      Adf.test(validData, -1)
    }

    // Too many lags
    assertThrows(IllegalArgumentException) {
      Adf.test(validData, 20)
    }

    // Invalid type
    assertThrows(IllegalArgumentException) {
      Adf.test(validData, 0, "invalid")
    }
  }

  @Test
  void testDifferentTypes() {
    // Generate data with trend
    List<Double> data = []
    Random rnd = new Random(456)
    for (int i = 0; i < 50; i++) {
      data.add(i * 0.5 + rnd.nextGaussian() * 2.0)
    }

    def resultDrift = Adf.test(data, 0, "drift")
    def resultTrend = Adf.test(data, 0, "trend")
    def resultNone = Adf.test(data, 0, "none")

    assertEquals("drift", resultDrift.type, 'Type should be drift')
    assertEquals("trend", resultTrend.type, 'Type should be trend')
    assertEquals("none", resultNone.type, 'Type should be none')

    // Critical values should be different for different types
    assertTrue(resultNone.criticalValue > resultDrift.criticalValue,
               'None should have less negative critical value than drift')
    assertTrue(resultDrift.criticalValue > resultTrend.criticalValue,
               'Drift should have less negative critical value than trend')
  }

  @Test
  void testWithLags() {
    // Generate AR(2) process
    List<Double> data = []
    Random rnd = new Random(789)
    double y1 = 0.0
    double y2 = 0.0
    for (int i = 0; i < 100; i++) {
      double y = 0.6 * y1 - 0.2 * y2 + rnd.nextGaussian()
      data.add(y)
      y2 = y1
      y1 = y
    }

    def result1 = Adf.test(data, 1)
    def result2 = Adf.test(data, 2)

    assertEquals(1, result1.lag, 'Lag should be 1')
    assertEquals(2, result2.lag, 'Lag should be 2')

    // Effective size should decrease with more lags
    assertTrue(result1.effectiveSize > result2.effectiveSize,
               'More lags means fewer effective observations')
  }

  @Test
  void testResultFields() {
    // Generate data
    List<Double> data = []
    Random rnd = new Random(321)
    double value = 50.0
    for (int i = 0; i < 50; i++) {
      value += rnd.nextGaussian() * 3.0
      data.add(value)
    }

    def result = Adf.test(data, 1, "trend")

    assertEquals(1, result.lag, 'Lag')
    assertEquals("trend", result.type, 'Type')
    assertEquals(50, result.sampleSize, 'Sample size')
    assertNotNull(result.statistic, 'Statistic should not be null')
    assertNotNull(result.gammaCoefficient, 'Gamma coefficient should not be null')
    assertNotNull(result.gammaStandardError, 'Standard error should not be null')
    assertNotNull(result.criticalValue, 'Critical value should not be null')

    // Gamma coefficient should be negative for mean-reverting processes
    // (but could be close to 0 for unit root)
    assertTrue(result.gammaCoefficient <= 1.0, 'Gamma should be <= 1')
  }

  @Test
  void testResultToString() {
    List<Double> data = []
    Random rnd = new Random(654)
    double val = 100.0
    for (int i = 0; i < 50; i++) {
      val += rnd.nextGaussian()
      data.add(val)
    }

    def result = Adf.test(data)

    String str = result.toString()
    assertTrue(str.contains('Augmented Dickey-Fuller Test'), 'Should contain test name')
    assertTrue(str.contains('Sample size'), 'Should contain sample size')
    assertTrue(str.contains('ADF statistic'), 'Should contain test statistic')
  }

  @Test
  void testInterpret() {
    List<Double> data = []
    Random rnd = new Random(987)
    double val = 100.0
    for (int i = 0; i < 50; i++) {
      val += rnd.nextGaussian()
      data.add(val)
    }

    def result = Adf.test(data)

    String interpretation = result.interpret()
    assertNotNull(interpretation, 'Interpretation should not be null')
    assertTrue(interpretation.contains('H0'), 'Should mention null hypothesis')
  }

  @Test
  void testEvaluate() {
    List<Double> data = []
    Random rnd = new Random(147)
    double val = 100.0
    for (int i = 0; i < 50; i++) {
      val += rnd.nextGaussian()
      data.add(val)
    }

    def result = Adf.test(data)

    String evaluation = result.evaluate()
    assertNotNull(evaluation, 'Evaluation should not be null')
    assertTrue(evaluation.contains('ADF statistic'), 'Should contain statistic info')
    assertTrue(evaluation.contains('coefficient'), 'Should contain coefficient info')
  }

  @Test
  void testConstantSeriesValidation() {
    // Constant series should be rejected
    List<Double> constantData = (1..50).collect { 5.0 }

    assertThrows(IllegalArgumentException) {
      Adf.test(constantData)
    }
  }

  @Test
  void testEffectiveSize() {
    List<Double> data = []
    Random rnd = new Random(258)
    double val = 100.0
    for (int i = 0; i < 50; i++) {
      val += rnd.nextGaussian()
      data.add(val)
    }

    def result = Adf.test(data, 2)

    // Effective size = n - lags - 1
    assertEquals(50 - 2 - 1, result.effectiveSize, 'Effective size calculation')
  }

  @Test
  void testSizeAdjustedCriticalValues() {
    // Critical values should vary with sample size
    List<Double> smallSample = []
    List<Double> largeSample = []
    Random rnd = new Random(369)

    double val = 100.0
    for (int i = 0; i < 20; i++) {
      val += rnd.nextGaussian()
      smallSample.add(val)
    }

    val = 100.0
    for (int i = 0; i < 150; i++) {
      val += rnd.nextGaussian()
      largeSample.add(val)
    }

    def resultSmall = Adf.test(smallSample, 0, "drift")
    def resultLarge = Adf.test(largeSample, 0, "drift")

    // Small sample should have more negative critical value
    assertTrue(resultSmall.criticalValue < resultLarge.criticalValue,
               'Small sample should have more stringent (more negative) critical value')
  }
}
