package timeseries

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.timeseries.Chow

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for Chow test for structural breaks.
 */
class ChowTest {

  @Test
  void testNoStructuralBreak() {
    // Linear relationship with no break: y = 2 + 3x
    double[] y = new double[20]
    double[][] X = new double[20][2]

    for (int i = 0; i < 20; i++) {
      X[i][0] = 1.0  // Intercept
      X[i][1] = i + 1.0  // x
      y[i] = 2.0 + 3.0 * (i + 1.0) + (Math.random() - 0.5) * 0.1  // Very small noise
    }

    def result = Chow.test(y, X, 10)

    assertNotNull(result, 'Result should not be null')
    assertEquals(20, result.sampleSize)
    assertEquals(2, result.numParameters)
    assertEquals(10, result.breakPoint)

    // Should typically not reject H0 (no structural break)
    // With random noise, allow for occasional false positives
    assertTrue(result.pValue > 0.01, 'Should typically not detect break when none exists')
  }

  @Test
  void testWithStructuralBreak() {
    // Different relationships before and after break:
    // Before: y = 1 + 2x
    // After: y = 5 + 1x
    double[] y = new double[30]
    double[][] X = new double[30][2]

    for (int i = 0; i < 15; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      y[i] = 1.0 + 2.0 * (i + 1.0) + (Math.random() - 0.5) * 0.1
    }

    for (int i = 15; i < 30; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      y[i] = 5.0 + 1.0 * (i + 1.0) + (Math.random() - 0.5) * 0.1
    }

    def result = Chow.test(y, X, 15)

    assertNotNull(result, 'Result should not be null')
    assertEquals(30, result.sampleSize)
    assertEquals(15, result.breakPoint)

    // Should reject H0 (structural break exists)
    assertTrue(result.pValue < 0.05, 'Should detect clear structural break')
    assertTrue(result.statistic > 0, 'F-statistic should be positive')
  }

  @Test
  void testListInput() {
    List<Double> y = (1..20).collect { it * 2.0 + 1.0 + (Math.random() - 0.5) * 0.1 }
    List<List<Double>> X = (1..20).collect { [1.0, it as double] }

    def result = Chow.test(y, X, 10)

    assertNotNull(result, 'Result should not be null')
    assertEquals(20, result.sampleSize)
  }

  @Test
  void testValidation() {
    double[] y = (1..20).collect { it * 2.0 } as double[]
    double[][] X = (1..20).collect { [1.0, it] as double[] } as double[][]

    // Null data
    assertThrows(IllegalArgumentException) {
      Chow.test(null as double[], X, 10)
    }

    assertThrows(IllegalArgumentException) {
      Chow.test(y, null as double[][], 10)
    }

    // Mismatched dimensions
    double[] yShort = [1, 2, 3] as double[]
    assertThrows(IllegalArgumentException) {
      Chow.test(yShort, X, 10)
    }

    // Break point too early
    assertThrows(IllegalArgumentException) {
      Chow.test(y, X, 1)
    }

    // Break point too late
    assertThrows(IllegalArgumentException) {
      Chow.test(y, X, 19)
    }
  }

  @Test
  void testDifferentBreakPoints() {
    double[] y = new double[30]
    double[][] X = new double[30][2]

    for (int i = 0; i < 30; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      y[i] = 2.0 + 3.0 * (i + 1.0) + (Math.random() - 0.5) * 0.5
    }

    // Test at different break points
    def result10 = Chow.test(y, X, 10)
    def result15 = Chow.test(y, X, 15)
    def result20 = Chow.test(y, X, 20)

    assertEquals(10, result10.breakPoint)
    assertEquals(15, result15.breakPoint)
    assertEquals(20, result20.breakPoint)

    // All should show no break
    assertTrue(result10.pValue > 0.05)
    assertTrue(result15.pValue > 0.05)
    assertTrue(result20.pValue > 0.05)
  }

  @Test
  void testMultipleRegressors() {
    // Test with 3 predictors: intercept, x1, x2
    double[] y = new double[40]
    double[][] X = new double[40][3]

    for (int i = 0; i < 20; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      X[i][2] = (i + 1.0) * (i + 1.0)
      y[i] = 1.0 + 2.0 * X[i][1] + 0.5 * X[i][2] + (Math.random() - 0.5) * 0.5
    }

    for (int i = 20; i < 40; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      X[i][2] = (i + 1.0) * (i + 1.0)
      y[i] = 5.0 + 1.0 * X[i][1] + 1.5 * X[i][2] + (Math.random() - 0.5) * 0.5  // Different coefficients
    }

    def result = Chow.test(y, X, 20)

    assertNotNull(result, 'Result should not be null')
    assertEquals(3, result.numParameters)
    assertEquals(3, result.df1)  // k = 3
    assertEquals(34, result.df2)  // n - 2k = 40 - 6 = 34
  }

  @Test
  void testResultInterpret() {
    double[] y = new double[20]
    double[][] X = new double[20][2]

    for (int i = 0; i < 20; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      y[i] = 2.0 + 3.0 * (i + 1.0) + (Math.random() - 0.5) * 0.5
    }

    def result = Chow.test(y, X, 10)

    String interpretation = result.interpret(0.05)
    assertNotNull(interpretation, 'Interpretation should not be null')
    assertTrue(interpretation.contains('H0'), 'Should mention H0')
    assertTrue(interpretation.contains('F ='), 'Should show F statistic')
    assertTrue(interpretation.contains('p ='), 'Should show p-value')
  }

  @Test
  void testResultEvaluate() {
    double[] y = new double[20]
    double[][] X = new double[20][2]

    for (int i = 0; i < 20; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      y[i] = 2.0 + 3.0 * (i + 1.0) + (Math.random() - 0.5) * 0.5
    }

    def result = Chow.test(y, X, 10)

    String evaluation = result.evaluate()
    assertNotNull(evaluation, 'Evaluation should not be null')
    assertTrue(evaluation.contains('Chow test'), 'Should contain test name')
    assertTrue(evaluation.contains('F-statistic'), 'Should contain statistic')
    assertTrue(evaluation.contains('p-value'), 'Should contain p-value')
    assertTrue(evaluation.contains('RSS'), 'Should contain RSS values')
    assertTrue(evaluation.contains('Conclusion'), 'Should contain conclusion')
  }

  @Test
  void testResultToString() {
    double[] y = new double[20]
    double[][] X = new double[20][2]

    for (int i = 0; i < 20; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      y[i] = 2.0 + 3.0 * (i + 1.0) + (Math.random() - 0.5) * 0.5
    }

    def result = Chow.test(y, X, 10)

    String str = result.toString()
    assertTrue(str.contains('Chow Test'), 'Should contain test name')
    assertTrue(str.contains('Break point'), 'Should contain break point')
    assertTrue(str.contains('F-statistic'), 'Should contain statistic')
    assertTrue(str.contains('Degrees of freedom'), 'Should contain df')
  }

  @Test
  void testRSSValues() {
    double[] y = new double[20]
    double[][] X = new double[20][2]

    for (int i = 0; i < 20; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      y[i] = 2.0 + 3.0 * (i + 1.0) + (Math.random() - 0.5) * 0.5
    }

    def result = Chow.test(y, X, 10)

    // RSS values should be non-negative
    assertTrue(result.rssFull >= 0, 'RSS full should be non-negative')
    assertTrue(result.rss1 >= 0, 'RSS1 should be non-negative')
    assertTrue(result.rss2 >= 0, 'RSS2 should be non-negative')

    // For no break, sum of sub-RSS should be less than or equal to full RSS
    // (sub-models can fit better since they have same number of parameters per observation)
    double sumSubRSS = result.rss1 + result.rss2
    assertTrue(sumSubRSS <= result.rssFull * 1.5,
               'Sub-RSS sum should be reasonable relative to full RSS')
  }

  @Test
  void testDegreesOfFreedom() {
    double[] y = new double[30]
    double[][] X = new double[30][2]

    for (int i = 0; i < 30; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      y[i] = 2.0 + 3.0 * (i + 1.0) + (Math.random() - 0.5) * 0.5
    }

    def result = Chow.test(y, X, 15)

    // df1 should equal k
    assertEquals(2, result.df1, 'df1 should equal number of parameters')

    // df2 should equal n - 2k
    assertEquals(26, result.df2, 'df2 should equal n - 2k')
  }

  @Test
  void testDifferentAlphaLevels() {
    double[] y = new double[20]
    double[][] X = new double[20][2]

    for (int i = 0; i < 20; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      y[i] = 2.0 + 3.0 * (i + 1.0) + (Math.random() - 0.5) * 0.5
    }

    def result = Chow.test(y, X, 10)

    String interp01 = result.interpret(0.01)
    String interp05 = result.interpret(0.05)
    String interp10 = result.interpret(0.10)

    assertNotNull(interp01, 'Interpretation at α=0.01 should not be null')
    assertNotNull(interp05, 'Interpretation at α=0.05 should not be null')
    assertNotNull(interp10, 'Interpretation at α=0.10 should not be null')
  }

  @Test
  void testPValueRange() {
    double[] y = new double[20]
    double[][] X = new double[20][2]

    for (int i = 0; i < 20; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      y[i] = 2.0 + 3.0 * (i + 1.0) + (Math.random() - 0.5) * 0.5
    }

    def result = Chow.test(y, X, 10)

    // p-value should be in [0, 1]
    assertTrue(result.pValue >= 0, 'p-value should be >= 0')
    assertTrue(result.pValue <= 1, 'p-value should be <= 1')
  }

  @Test
  void testFStatisticPositive() {
    double[] y = new double[20]
    double[][] X = new double[20][2]

    for (int i = 0; i < 20; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      y[i] = 2.0 + 3.0 * (i + 1.0) + (Math.random() - 0.5) * 0.5
    }

    def result = Chow.test(y, X, 10)

    // F-statistic should always be positive
    assertTrue(result.statistic >= 0, 'F-statistic should be non-negative')
  }

  @Test
  void testBreakPointSensitivity() {
    // Test that the test is sensitive to the location of the break
    double[] y = new double[30]
    double[][] X = new double[30][2]

    // Introduce a break at position 15
    for (int i = 0; i < 15; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      y[i] = 1.0 + 2.0 * (i + 1.0) + (Math.random() - 0.5) * 0.1
    }

    for (int i = 15; i < 30; i++) {
      X[i][0] = 1.0
      X[i][1] = i + 1.0
      y[i] = 10.0 + 1.0 * (i + 1.0) + (Math.random() - 0.5) * 0.1
    }

    def resultCorrect = Chow.test(y, X, 15)  // Test at actual break
    def resultWrong = Chow.test(y, X, 10)    // Test at wrong location

    // Should be more significant when testing at correct location
    assertTrue(resultCorrect.pValue < resultWrong.pValue,
               'Test should be more significant at correct break point')
  }

  @Test
  void testSimpleLinearRegression() {
    // Simple example: y = x with noise
    double[] y = (1..20).collect { it + (Math.random() - 0.5) * 0.5 } as double[]
    double[][] X = (1..20).collect { [1.0, it] as double[] } as double[][]

    def result = Chow.test(y, X, 10)

    assertNotNull(result)
    assertTrue(result.statistic >= 0)
    assertTrue(result.pValue >= 0 && result.pValue <= 1)
  }
}
