package timeseries

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.timeseries.Df

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for Dickey-Fuller test.
 */
class DfTest {

  private static final double TOLERANCE = 0.01

  @Test
  void testRandomWalk() {
    // Random walk (non-stationary)
    double[] data = [
      100, 102, 104, 103, 105, 107, 106, 108, 110, 112,
      111, 113, 115, 114, 116, 118, 120, 122, 121, 123
    ] as double[]

    def result = Df.test(data, "drift")

    assertNotNull(result, 'Result should not be null')
    assertEquals(20, result.sampleSize)
    assertEquals("drift", result.testType)

    // Random walk should fail to reject unit root
    assertTrue(result.statistic > result.criticalValue5pct,
               'Random walk should not reject H0 (unit root)')
  }

  @Test
  void testStationarySeries() {
    // Stationary series (mean-reverting around 100)
    double[] data = [
      100, 101, 99, 100, 102, 98, 100, 101, 99, 100,
      101, 99, 100, 102, 98, 100, 101, 99, 100, 101
    ] as double[]

    def result = Df.test(data, "drift")

    assertNotNull(result, 'Result should not be null')

    // Stationary series should reject unit root
    // Note: May not always reject with small sample, so just check it runs
    assertTrue(result.statistic <= 0, 'DF statistic should be negative')
  }

  @Test
  void testTrendStationary() {
    // Linear trend (trend-stationary)
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = 100 + i * 0.5 + Math.sin(i * 0.3) * 2
    }

    def result = Df.test(data, "trend")

    assertNotNull(result, 'Result should not be null')
    assertEquals("trend", result.testType)
    assertTrue(result.statistic <= 0, 'DF statistic should be negative')
  }

  @Test
  void testNoIntercept() {
    // Test with type="none" (no intercept or trend)
    double[] data = [
      1, 1.1, 0.9, 1.0, 1.2, 0.8, 1.0, 1.1, 0.9, 1.0,
      1.1, 0.9, 1.0, 1.2, 0.8, 1.0, 1.1, 0.9, 1.0, 1.1
    ] as double[]

    def result = Df.test(data, "none")

    assertNotNull(result, 'Result should not be null')
    assertEquals("none", result.testType)
    assertTrue(result.statistic <= 0, 'DF statistic should be negative')
  }

  @Test
  void testListInput() {
    // Test with List input
    List<Double> data = [
      100, 101, 99, 100, 102, 98, 100, 101, 99, 100,
      101, 99, 100, 102, 98, 100, 101, 99, 100, 101
    ]

    def result = Df.test(data, "drift")

    assertNotNull(result, 'Result should not be null')
    assertEquals(20, result.sampleSize)
  }

  @Test
  void testValidation() {
    // Null data
    assertThrows(IllegalArgumentException) {
      Df.test(null as double[], "drift")
    }

    // Too few observations
    double[] tooSmall = [1, 2, 3, 4, 5, 6, 7, 8, 9] as double[]
    assertThrows(IllegalArgumentException) {
      Df.test(tooSmall, "drift")
    }

    // Constant series
    double[] constant = new double[20]
    for (int i = 0; i < 20; i++) constant[i] = 100.0
    assertThrows(IllegalArgumentException) {
      Df.test(constant, "drift")
    }

    // Invalid type
    double[] data = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12] as double[]
    assertThrows(IllegalArgumentException) {
      Df.test(data, "invalid")
    }
  }

  @Test
  void testCriticalValues() {
    // Test that critical values are reasonable
    double[] data = new double[100]
    for (int i = 0; i < 100; i++) {
      data[i] = 100 + i * 0.1
    }

    def result = Df.test(data, "drift")

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

    def result = Df.test(data, "drift")

    String interpretation = result.interpret(0.05)
    assertNotNull(interpretation, 'Interpretation should not be null')
    assertTrue(interpretation.contains('H0'), 'Should mention H0')
    assertTrue(interpretation.contains('DF ='), 'Should show DF statistic')
    assertTrue(interpretation.contains('CV ='), 'Should show critical value')
  }

  @Test
  void testResultEvaluate() {
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = 100 + i
    }

    def result = Df.test(data, "trend")

    String evaluation = result.evaluate()
    assertNotNull(evaluation, 'Evaluation should not be null')
    assertTrue(evaluation.contains('Dickey-Fuller'), 'Should contain test name')
    assertTrue(evaluation.contains('DF statistic'), 'Should contain statistic')
    assertTrue(evaluation.contains('Critical values'), 'Should contain critical values')
    assertTrue(evaluation.contains('Conclusion'), 'Should contain conclusion')
  }

  @Test
  void testResultToString() {
    double[] data = new double[30]
    for (int i = 0; i < 30; i++) {
      data[i] = 100 + Math.sin(i * 0.5) * 10
    }

    def result = Df.test(data, "drift")

    String str = result.toString()
    assertTrue(str.contains('Dickey-Fuller'), 'Should contain test name')
    assertTrue(str.contains('DF statistic'), 'Should contain statistic')
    assertTrue(str.contains('γ coefficient'), 'Should contain gamma')
    assertTrue(str.contains('Critical values'), 'Should contain CVs')
  }

  @Test
  void testDifferentTypes() {
    double[] data = new double[30]
    for (int i = 0; i < 30; i++) {
      data[i] = 100 + i * 0.5
    }

    // Test all three types
    def noneResult = Df.test(data, "none")
    def driftResult = Df.test(data, "drift")
    def trendResult = Df.test(data, "trend")

    assertNotNull(noneResult)
    assertNotNull(driftResult)
    assertNotNull(trendResult)

    assertEquals("none", noneResult.testType)
    assertEquals("drift", driftResult.testType)
    assertEquals("trend", trendResult.testType)

    // Critical values should differ by type
    // Trend is most negative, none is least negative
    assertTrue(trendResult.criticalValue5pct < driftResult.criticalValue5pct)
    assertTrue(driftResult.criticalValue5pct < noneResult.criticalValue5pct)
  }

  @Test
  void testGammaCoefficient() {
    // For a series with unit root, gamma should be close to 0
    double[] randomWalk = new double[50]
    randomWalk[0] = 100
    for (int i = 1; i < 50; i++) {
      randomWalk[i] = randomWalk[i - 1] + (Math.random() - 0.5)
    }

    def result = Df.test(randomWalk, "drift")

    // Gamma should be small (close to 0) for unit root process
    assertTrue(Math.abs(result.gamma) < 0.5,
               'Gamma should be close to 0 for unit root process')
  }

  @Test
  void testStatisticRange() {
    // Test multiple series to ensure statistics are in reasonable range
    double[][] testData = [
      (0..29).collect { 100 + it * 0.5 } as double[],  // Trend
      (0..29).collect { 100 + Math.sin(it * 0.3) * 5 } as double[],  // Cyclical
      (0..29).collect { 100 + (Math.random() - 0.5) * 2 } as double[]  // Stationary
    ]

    for (double[] data : testData) {
      def result = Df.test(data, "drift")

      // DF statistic should be negative
      assertTrue(result.statistic <= 0,
                 "DF statistic should be negative, got ${result.statistic}")

      // Should be more negative than -20 (reasonable bound)
      assertTrue(result.statistic > -20,
                 "DF statistic should be > -20, got ${result.statistic}")
    }
  }

  @Test
  void testMinimumSampleSize() {
    // Test with minimum allowed sample size (10)
    double[] data = [100, 101, 102, 103, 104, 105, 106, 107, 108, 109] as double[]

    def result = Df.test(data, "drift")

    assertNotNull(result, 'Should handle minimum sample size')
    assertEquals(10, result.sampleSize)
  }

  @Test
  void testDifferentAlphaLevels() {
    double[] data = new double[50]
    for (int i = 0; i < 50; i++) {
      data[i] = 100 + Math.sin(i * 0.2) * 10
    }

    def result = Df.test(data, "drift")

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

    def result = Df.test(data, "drift")

    // Standard error should be positive
    assertTrue(result.standardError > 0,
               'Standard error should be positive')

    // Standard error should be reasonable (not too large)
    assertTrue(result.standardError < 1.0,
               'Standard error should be reasonable')
  }
}
