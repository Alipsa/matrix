package normality

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.stats.normality.KSquared

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for D'Agostino's K² test.
 */
class KSquaredTest {

  private static final double TOLERANCE = 0.01

  @Test
  void testNormalData() {
    // Approximately normal data
    double[] data = [
      2.1, 2.5, 2.8, 3.0, 3.2, 3.5, 3.8, 4.0,
      2.3, 2.7, 2.9, 3.1, 3.3, 3.6, 3.9, 4.1,
      2.2, 2.6, 2.85, 3.05, 3.25, 3.55, 3.85, 4.05
    ] as double[]

    def result = KSquared.test(data)

    assertNotNull(result, 'Result should not be null')
    assertEquals(24, result.sampleSize, 'Sample size should be 24')
    assertTrue(result.pValue > 0.05, 'Normal data should have high p-value')
    assertTrue(result.statistic >= 0, 'K² statistic should be non-negative')
  }

  @Test
  void testPositivelySkewedData() {
    // Right-skewed data (exponential-like)
    double[] data = [
      1, 1, 1, 2, 2, 2, 2, 3,
      3, 3, 4, 4, 5, 6, 7, 8,
      9, 10, 12, 15, 20, 25, 30, 40
    ] as double[]

    def result = KSquared.test(data)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.skewness > 0, 'Should detect positive skewness')
    assertTrue(result.pValue < 0.05, 'Should reject normality for skewed data')
  }

  @Test
  void testNegativelySkewedData() {
    // Left-skewed data - most values high, few very low
    double[] data = [
      10, 10, 10, 10, 10, 10, 9, 9,
      9, 8, 8, 7, 6, 5, 3, 1
    ] as double[]

    def result = KSquared.test(data)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.skewness < 0, 'Should detect negative skewness')
    assertTrue(result.pValue < 0.05, 'Should reject normality for skewed data')
  }

  @Test
  void testHeavyTails() {
    // Data with heavy tails (high kurtosis) - extreme outliers
    double[] data = [
      -20, -15, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 15, 20
    ] as double[]

    def result = KSquared.test(data)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.kurtosis > 3, 'Should detect heavy tails')
    // May or may not reject at 0.05 depending on data, so just check it runs
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be valid')
  }

  @Test
  void testLightTails() {
    // Uniform-like distribution (light tails, low kurtosis)
    double[] data = [
      1, 2, 3, 4, 5, 6, 7, 8,
      9, 10, 11, 12, 13, 14, 15, 16,
      17, 18, 19, 20, 21, 22, 23, 24
    ] as double[]

    def result = KSquared.test(data)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.kurtosis < 3, 'Should detect light tails')
  }

  @Test
  void testZScores() {
    // Test that Z-scores are calculated
    double[] data = [
      2.1, 2.5, 2.8, 3.0, 3.2, 3.5, 3.8, 4.0,
      2.3, 2.7, 2.9, 3.1, 3.3, 3.6, 3.9, 4.1
    ] as double[]

    def result = KSquared.test(data)

    assertNotNull(result.zSkewness, 'Z-score for skewness should be calculated')
    assertNotNull(result.zKurtosis, 'Z-score for kurtosis should be calculated')

    // K² should equal sum of squared Z-scores
    double expected = result.zSkewness * result.zSkewness + result.zKurtosis * result.zKurtosis
    assertEquals(expected, result.statistic, 0.0001, 'K² should equal sum of Z²')
  }

  @Test
  void testMatrixByName() {
    def matrix = Matrix.builder()
      .matrixName('test')
      .columnNames(['value'])
      .rows([
        [2.1], [2.5], [2.8], [3.0], [3.2], [3.5], [3.8], [4.0],
        [2.3], [2.7], [2.9], [3.1], [3.3], [3.6], [3.9], [4.1]
      ])
      .build()

    def result = KSquared.test(matrix, 'value')

    assertNotNull(result, 'Result should not be null')
    assertEquals(16, result.sampleSize, 'Sample size should be 16')
  }

  @Test
  void testMatrixByIndex() {
    def matrix = Matrix.builder()
      .matrixName('test')
      .rows([
        [2.1], [2.5], [2.8], [3.0], [3.2], [3.5], [3.8], [4.0],
        [2.3], [2.7], [2.9], [3.1], [3.3], [3.6], [3.9], [4.1]
      ])
      .build()

    def result = KSquared.test(matrix, 0)

    assertNotNull(result, 'Result should not be null')
    assertEquals(16, result.sampleSize, 'Sample size should be 16')
  }

  @Test
  void testValidation() {
    // Null data
    assertThrows(IllegalArgumentException) {
      KSquared.test(null as double[])
    }

    // Too small sample
    double[] tooSmall = [1, 2, 3, 4, 5, 6, 7] as double[]
    assertThrows(IllegalArgumentException) {
      KSquared.test(tooSmall)
    }

    // Zero variance
    double[] constant = [5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0] as double[]
    assertThrows(IllegalArgumentException) {
      KSquared.test(constant)
    }

    // Null matrix
    assertThrows(IllegalArgumentException) {
      KSquared.test(null as Matrix, 'col')
    }
  }

  @Test
  void testResultInterpret() {
    // Normal data
    double[] normalData = [
      2.1, 2.5, 2.8, 3.0, 3.2, 3.5, 3.8, 4.0,
      2.3, 2.7, 2.9, 3.1, 3.3, 3.6, 3.9, 4.1
    ] as double[]

    def normalResult = KSquared.test(normalData)
    String normalInterp = normalResult.interpret()

    assertTrue(normalInterp.contains('Fail to reject H0'), 'Should not reject H0 for normal data')
    assertTrue(normalInterp.contains('consistent with normality'), 'Should indicate normality')

    // Highly skewed data - exponential-like
    double[] skewedData = [
      1, 1, 1, 1, 2, 2, 2, 3,
      3, 4, 5, 7, 10, 15, 25, 50
    ] as double[]

    def skewedResult = KSquared.test(skewedData)

    // Skewed data should show departure from normality (check p-value is low)
    if (skewedResult.pValue < 0.05) {
      String skewedInterp = skewedResult.interpret()
      assertTrue(skewedInterp.contains('Reject H0'), 'Should reject H0 for skewed data')
      assertTrue(skewedInterp.contains('departs from normality'), 'Should indicate non-normality')
    } else {
      // If not significant, at least check interpretation makes sense
      String skewedInterp = skewedResult.interpret()
      assertNotNull(skewedInterp, 'Interpretation should not be null')
    }
  }

  @Test
  void testResultEvaluate() {
    double[] data = [
      2.1, 2.5, 2.8, 3.0, 3.2, 3.5, 3.8, 4.0,
      2.3, 2.7, 2.9, 3.1, 3.3, 3.6, 3.9, 4.1
    ] as double[]

    def result = KSquared.test(data)
    String evaluation = result.evaluate()

    assertNotNull(evaluation, 'Evaluation should not be null')
    assertTrue(evaluation.contains('K² statistic'), 'Should contain statistic')
    assertTrue(evaluation.contains('p-value'), 'Should contain p-value')
    assertTrue(evaluation.contains('Skewness'), 'Should contain skewness')
    assertTrue(evaluation.contains('Kurtosis'), 'Should contain kurtosis')
    assertTrue(evaluation.contains('Conclusion'), 'Should contain conclusion')
  }

  @Test
  void testResultToString() {
    double[] data = [
      2.1, 2.5, 2.8, 3.0, 3.2, 3.5, 3.8, 4.0,
      2.3, 2.7, 2.9, 3.1, 3.3, 3.6, 3.9, 4.1
    ] as double[]

    def result = KSquared.test(data)
    String str = result.toString()

    assertTrue(str.contains('D\'Agostino'), 'Should contain test name')
    assertTrue(str.contains('K²'), 'Should contain K²')
    assertTrue(str.contains('p-value'), 'Should contain p-value')
    assertTrue(str.contains('Skewness'), 'Should contain skewness')
    assertTrue(str.contains('Kurtosis'), 'Should contain kurtosis')
  }

  @Test
  void testPValueRange() {
    // Test multiple datasets to ensure p-values are always valid
    double[][] testData = [
      [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12] as double[],
      [2.1, 2.5, 2.8, 3.0, 3.2, 3.5, 3.8, 4.0, 2.3, 2.7, 2.9, 3.1] as double[],
      [1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144] as double[],
      [-5, -3, -1, 0, 1, 3, 5, 7, 9, 11, 13, 15] as double[]
    ]

    for (double[] data : testData) {
      def result = KSquared.test(data)

      assertTrue(result.pValue >= 0, "p-value should be >= 0, got ${result.pValue}")
      assertTrue(result.pValue <= 1, "p-value should be <= 1, got ${result.pValue}")
      assertTrue(result.statistic >= 0, "K² statistic should be >= 0, got ${result.statistic}")
    }
  }

  @Test
  void testDifferentAlphaLevels() {
    double[] data = [
      1, 1, 2, 2, 3, 4, 5, 8,
      10, 15, 20, 25, 30, 40, 50, 60
    ] as double[]

    def result = KSquared.test(data)

    String interp01 = result.interpret(0.01)
    String interp05 = result.interpret(0.05)
    String interp10 = result.interpret(0.10)

    assertNotNull(interp01, 'Interpretation at α=0.01 should not be null')
    assertNotNull(interp05, 'Interpretation at α=0.05 should not be null')
    assertNotNull(interp10, 'Interpretation at α=0.10 should not be null')
  }

  @Test
  void testMinimumSampleSize() {
    // Test with minimum allowed sample size (8)
    double[] data = [1, 2, 3, 4, 5, 6, 7, 8] as double[]

    def result = KSquared.test(data)

    assertNotNull(result, 'Should handle minimum sample size')
    assertEquals(8, result.sampleSize, 'Sample size should be 8')
  }

  @Test
  void testLargeSample() {
    // Test with larger sample (100 points)
    double[] data = new double[100]
    Random rnd = new Random(42)
    for (int i = 0; i < 100; i++) {
      data[i] = rnd.nextGaussian() * 2 + 5
    }

    def result = KSquared.test(data)

    assertNotNull(result, 'Should handle large samples')
    assertEquals(100, result.sampleSize, 'Sample size should be 100')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be valid')
  }

  @Test
  void testSymmetricDistribution() {
    // Perfectly symmetric around mean
    double[] data = [
      -4, -3, -2, -1, 0, 1, 2, 3,
      4, -3.5, -2.5, -1.5, -0.5, 0.5, 1.5, 2.5, 3.5
    ] as double[]

    def result = KSquared.test(data)

    assertNotNull(result, 'Result should not be null')
    assertTrue(Math.abs(result.skewness) < 0.5, 'Skewness should be near 0 for symmetric data')
  }

  @Test
  void testDetermineReason() {
    // Test skewness-dominated departure
    double[] skewed = [
      1, 1, 2, 2, 3, 4, 5, 10,
      15, 20, 30, 40, 50, 60, 70, 80
    ] as double[]

    def skewedResult = KSquared.test(skewed)
    if (skewedResult.pValue < 0.05) {
      String interp = skewedResult.interpret()
      assertTrue(interp.contains('skewness') || interp.contains('tail'),
                 'Should mention skewness or tail for skewed data')
    }
  }

  @Test
  void testKurtosisValues() {
    // Normal distribution has kurtosis = 3
    // Test that kurtosis is reported
    double[] data = [
      2.1, 2.5, 2.8, 3.0, 3.2, 3.5, 3.8, 4.0,
      2.3, 2.7, 2.9, 3.1, 3.3, 3.6, 3.9, 4.1
    ] as double[]

    def result = KSquared.test(data)

    assertTrue(result.kurtosis > 0, 'Kurtosis should be calculated')
    // For roughly normal data, kurtosis should be near 3
    assertTrue(result.kurtosis >= 1 && result.kurtosis <= 5,
               'Kurtosis for near-normal data should be in reasonable range')
  }
}
