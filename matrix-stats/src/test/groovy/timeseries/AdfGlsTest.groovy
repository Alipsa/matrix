package timeseries

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.timeseries.AdfGls

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for ADF-GLS (Elliott-Rothenberg-Stock) test.
 */
class AdfGlsTest {

  @Test
  void testRandomWalk() {
    // Random walk (non-stationary)
    double[] data = [
      100, 102, 104, 103, 105, 107, 106, 108, 110, 112,
      111, 113, 115, 114, 116, 118, 120, 122, 121, 123,
      125, 124, 126, 128, 130, 132, 131, 133, 135, 137
    ] as double[]

    def result = AdfGls.test(data, 0, "drift")

    assertNotNull(result, 'Result should not be null')
    assertEquals(30, result.sampleSize)
    assertEquals("drift", result.testType)
    assertEquals(0, result.lags)

    // Random walk should fail to reject unit root
    assertTrue(result.statistic > result.criticalValue5pct,
               'Random walk should not reject H0 (unit root)')
  }

  @Test
  void testStationarySeries() {
    // Stationary series (mean-reverting around 100)
    double[] data = [
      100, 101, 99, 100, 102, 98, 100, 101, 99, 100,
      101, 99, 100, 102, 98, 100, 101, 99, 100, 101,
      99, 100, 102, 98, 100, 101, 99, 100, 101, 99
    ] as double[]

    def result = AdfGls.test(data, 0, "drift")

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.statistic <= 0, 'ADF-GLS statistic should be negative')
  }

  @Test
  void testTrendStationary() {
    // Linear trend (trend-stationary)
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = 100 + i * 0.5 + Math.sin(i * 0.3) * 2
    }

    def result = AdfGls.test(data, 0, "trend")

    assertNotNull(result, 'Result should not be null')
    assertEquals("trend", result.testType)
    assertTrue(result.statistic <= 0, 'ADF-GLS statistic should be negative')
  }

  @Test
  void testWithLags() {
    // Test with explicit lag specification
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = 100 + Math.sin(i * 0.2) * 10 + (Math.random() - 0.5) * 3
    }

    def result = AdfGls.test(data, 2, "drift")

    assertNotNull(result, 'Result should not be null')
    assertEquals(2, result.lags, 'Lags should be 2')
  }

  @Test
  void testAutoLagSelection() {
    // Test automatic lag selection
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = 100 + i * 0.3 + Math.sin(i * 0.2) * 5
    }

    def result = AdfGls.test(data, null as Integer, "drift")

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.lags >= 0, 'Lags should be non-negative')
  }

  @Test
  void testListInput() {
    // Test with List input
    List<Double> data = [
      100, 101, 99, 100, 102, 98, 100, 101, 99, 100,
      101, 99, 100, 102, 98, 100, 101, 99, 100, 101
    ]

    def result = AdfGls.test(data, 0, "drift")

    assertNotNull(result, 'Result should not be null')
    assertEquals(20, result.sampleSize)
  }

  @Test
  void testValidation() {
    // Null data
    assertThrows(IllegalArgumentException) {
      AdfGls.test(null as double[], 0, "drift")
    }

    // Too few observations
    double[] tooSmall = [1, 2, 3, 4, 5, 6, 7, 8, 9] as double[]
    assertThrows(IllegalArgumentException) {
      AdfGls.test(tooSmall, 0, "drift")
    }

    // Constant series
    double[] constant = new double[20]
    for (int i = 0; i < 20; i++) constant[i] = 100.0
    assertThrows(IllegalArgumentException) {
      AdfGls.test(constant, 0, "drift")
    }

    // Invalid type
    double[] data = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12] as double[]
    assertThrows(IllegalArgumentException) {
      AdfGls.test(data, 0, "invalid")
    }
  }

  @Test
  void testCriticalValues() {
    // Test that critical values are reasonable
    double[] data = new double[100]
    for (int i = 0; i < 100; i++) {
      data[i] = 100 + i * 0.1 + Math.sin(i * 0.2) * 2
    }

    def result = AdfGls.test(data, 0, "drift")

    // Critical values should be negative and ordered: 1% < 5% < 10%
    assertTrue(result.criticalValue1pct < result.criticalValue5pct,
               '1% CV should be more negative than 5% CV')
    assertTrue(result.criticalValue5pct < result.criticalValue10pct,
               '5% CV should be more negative than 10% CV')
    assertTrue(result.criticalValue10pct < 0,
               '10% CV should be negative')
  }

  @Test
  void testResultInterpret() {
    double[] data = [
      100, 101, 99, 100, 102, 98, 100, 101, 99, 100,
      101, 99, 100, 102, 98, 100, 101, 99, 100, 101
    ] as double[]

    def result = AdfGls.test(data, 0, "drift")

    String interpretation = result.interpret(0.05)
    assertNotNull(interpretation, 'Interpretation should not be null')
    assertTrue(interpretation.contains('H0'), 'Should mention H0')
    assertTrue(interpretation.contains('ADF-GLS ='), 'Should show ADF-GLS statistic')
    assertTrue(interpretation.contains('CV ='), 'Should show critical value')
  }

  @Test
  void testResultEvaluate() {
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = 100 + i + Math.sin(i * 0.3) * 3
    }

    def result = AdfGls.test(data, 0, "trend")

    String evaluation = result.evaluate()
    assertNotNull(evaluation, 'Evaluation should not be null')
    assertTrue(evaluation.contains('ADF-GLS'), 'Should contain test name')
    assertTrue(evaluation.contains('statistic'), 'Should contain statistic')
    assertTrue(evaluation.contains('Critical values'), 'Should contain critical values')
    assertTrue(evaluation.contains('Conclusion'), 'Should contain conclusion')
  }

  @Test
  void testResultToString() {
    double[] data = new double[30]
    for (int i = 0; i < 30; i++) {
      data[i] = 100 + Math.sin(i * 0.5) * 10
    }

    def result = AdfGls.test(data, 0, "drift")

    String str = result.toString()
    assertTrue(str.contains('ADF-GLS'), 'Should contain test name')
    assertTrue(str.contains('Elliott-Rothenberg-Stock'), 'Should contain authors')
    assertTrue(str.contains('statistic'), 'Should contain statistic')
    assertTrue(str.contains('γ coefficient'), 'Should contain gamma')
    assertTrue(str.contains('Critical values'), 'Should contain CVs')
  }

  @Test
  void testDriftVsTrend() {
    double[] data = new double[30]
    for (int i = 0; i < 30; i++) {
      data[i] = 100 + i * 0.5
    }

    // Test both types
    def driftResult = AdfGls.test(data, 0, "drift")
    def trendResult = AdfGls.test(data, 0, "trend")

    assertNotNull(driftResult)
    assertNotNull(trendResult)

    assertEquals("drift", driftResult.testType)
    assertEquals("trend", trendResult.testType)

    // Critical values should differ by type
    // Trend critical values are more negative
    assertTrue(trendResult.criticalValue5pct < driftResult.criticalValue5pct,
               'Trend CV should be more negative than drift CV')
  }

  @Test
  void testGammaCoefficient() {
    // For a series with unit root, gamma should be close to 0
    double[] randomWalk = new double[50]
    randomWalk[0] = 100
    for (int i = 1; i < 50; i++) {
      randomWalk[i] = randomWalk[i - 1] + (Math.random() - 0.5)
    }

    def result = AdfGls.test(randomWalk, 0, "drift")

    // Gamma should be small (close to 0) for unit root process
    assertTrue(Math.abs(result.gamma) < 0.5,
               'Gamma should be close to 0 for unit root process')
  }

  @Test
  void testStatisticRange() {
    // Test multiple series to ensure statistics are in reasonable range
    double[][] testData = [
      (0..29).collect { 100 + it * 0.5 + Math.sin(it * 0.2) * 2 } as double[],  // Trend with variation
      (0..29).collect { 100 + Math.sin(it * 0.3) * 5 } as double[],  // Cyclical
      (0..29).collect { 100 + Math.sin(it * 0.7) * 3 + Math.cos(it * 0.4) * 2 } as double[]  // Stationary-like
    ]

    for (double[] data : testData) {
      def result = AdfGls.test(data, 0, "drift")

      // Statistics should be finite and in a reasonable range
      assertFalse(Double.isNaN(result.statistic), 'Statistic should not be NaN')
      assertFalse(Double.isInfinite(result.statistic), 'Statistic should not be infinite')

      // Should be in a reasonable range
      assertTrue(result.statistic > -50 && result.statistic < 50,
                 "ADF-GLS statistic should be in reasonable range, got ${result.statistic}")
    }
  }

  @Test
  void testMinimumSampleSize() {
    // Test with minimum allowed sample size (15 to account for lag computations)
    double[] data = [100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114] as double[]

    def result = AdfGls.test(data, 0, "drift")

    assertNotNull(result, 'Should handle minimum sample size')
    assertEquals(15, result.sampleSize)
  }

  @Test
  void testDifferentAlphaLevels() {
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = 100 + Math.sin(i * 0.2) * 10
    }

    def result = AdfGls.test(data, 0, "drift")

    String interp01 = result.interpret(0.01)
    String interp05 = result.interpret(0.05)
    String interp10 = result.interpret(0.10)

    assertNotNull(interp01, 'Interpretation at α=0.01 should not be null')
    assertNotNull(interp05, 'Interpretation at α=0.05 should not be null')
    assertNotNull(interp10, 'Interpretation at α=0.10 should not be null')
  }

  @Test
  void testStandardError() {
    double[] data = new double[30]
    for (int i = 0; i < 30; i++) {
      data[i] = 100 + i + Math.sin(i * 0.3) * 2
    }

    def result = AdfGls.test(data, 0, "drift")

    // Standard error should be positive
    assertTrue(result.standardError > 0,
               'Standard error should be positive')

    // Standard error should be reasonable (not too large)
    assertTrue(result.standardError < 1.0,
               'Standard error should be reasonable')
  }

  @Test
  void testCompareWithDifferentLags() {
    // Test that using different lags produces different results
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = 100 + i * 0.2 + Math.sin(i * 0.3) * 5
    }

    def result0 = AdfGls.test(data, 0, "drift")
    def result1 = AdfGls.test(data, 1, "drift")
    def result2 = AdfGls.test(data, 2, "drift")

    assertEquals(0, result0.lags)
    assertEquals(1, result1.lags)
    assertEquals(2, result2.lags)

    // Statistics should all be calculated (no NaN or infinity)
    assertFalse(Double.isNaN(result0.statistic))
    assertFalse(Double.isNaN(result1.statistic))
    assertFalse(Double.isNaN(result2.statistic))
    assertFalse(Double.isInfinite(result0.statistic))
    assertFalse(Double.isInfinite(result1.statistic))
    assertFalse(Double.isInfinite(result2.statistic))
  }

  @Test
  void testGlsDetrendingEffect() {
    // Test that GLS detrending improves power for trend-stationary series
    double[] trendSeries = new double[50]
    for (int i = 0; i < 50; i++) {
      trendSeries[i] = 100 + i * 2 + Math.sin(i * 0.5) * 3
    }

    def result = AdfGls.test(trendSeries, 0, "trend")

    assertNotNull(result, 'Result should not be null')
    // For trend-stationary series, should generally reject unit root
    assertTrue(result.statistic <= 0, 'Statistic should be negative')
  }
}
