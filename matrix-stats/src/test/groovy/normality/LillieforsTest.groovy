package normality

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.normality.Lilliefors

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for the Lilliefors normality test.
 * The Lilliefors test is a K-S test with parameters estimated from data.
 */
class LillieforsTest {

  private static final double TOLERANCE = 0.01

  @Test
  void testNormalLikeData() {
    // Data that is approximately normal
    List<Double> normalData = [
      1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0,
      6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0, 10.5, 11.0
    ]

    def result = Lilliefors.testNormality(normalData, 0.05)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.statistic >= 0 && result.statistic <= 1, 'Statistic should be in [0, 1]')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'P-value should be in [0, 1]')
    assertNotNull(result.evaluate(), 'Should have evaluation')
  }

  @Test
  void testValidation() {
    // Null data
    assertThrows(IllegalArgumentException) {
      Lilliefors.testNormality(null)
    }

    // Empty data
    assertThrows(IllegalArgumentException) {
      Lilliefors.testNormality([])
    }

    // Too few observations
    assertThrows(IllegalArgumentException) {
      Lilliefors.testNormality([1.0, 2.0, 3.0])
    }

    // Null values in data
    assertThrows(IllegalArgumentException) {
      Lilliefors.testNormality([1.0, null, 3.0, 4.0])
    }
  }

  @Test
  void testResultToString() {
    List<Double> data = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0]

    def result = Lilliefors.testNormality(data)

    String str = result.toString()
    assertTrue(str.contains('Lilliefors Normality Test'), 'Should contain test name')
    assertTrue(str.contains('Sample size'), 'Should contain sample size')
    assertTrue(str.contains('P-value'), 'Should contain p-value')
    assertTrue(str.contains('Test statistic'), 'Should contain test statistic')
  }

  @Test
  void testDifferentSampleSizes() {
    // Test with different sample sizes
    List<Double> small = (1..10).collect { it.doubleValue() }
    List<Double> medium = (1..50).collect { it.doubleValue() }
    List<Double> large = (1..100).collect { it.doubleValue() }

    def resultSmall = Lilliefors.testNormality(small)
    def resultMedium = Lilliefors.testNormality(medium)
    def resultLarge = Lilliefors.testNormality(large)

    assertNotNull(resultSmall, 'Small sample result should not be null')
    assertNotNull(resultMedium, 'Medium sample result should not be null')
    assertNotNull(resultLarge, 'Large sample result should not be null')

    assertEquals(10, resultSmall.sampleSize, 'Small sample size')
    assertEquals(50, resultMedium.sampleSize, 'Medium sample size')
    assertEquals(100, resultLarge.sampleSize, 'Large sample size')
  }

  @Test
  void testStandardNormalData() {
    // Data from standard normal distribution
    List<Double> data = [
      -0.56047565, -0.23017749, 1.55870831, 0.07050839, 0.12928774,
      1.71506499, 0.46091621, -1.26506123, -0.68685285, -0.44566197,
      1.22408180, 0.35981383, 0.40077145, 0.11068272, -0.55584113,
      1.78691314, 0.49785048, -1.96661716, 0.70135590, -0.47279141
    ]

    def result = Lilliefors.testNormality(data)

    assertNotNull(result, 'Result should not be null')
    assertNotNull(result.statistic, 'Statistic should not be null')
    assertTrue(result.statistic >= 0 && result.statistic <= 1, 'Statistic should be in [0, 1]')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'P-value should be in [0, 1]')
  }

  @Test
  void testSkewedData() {
    // Highly skewed data
    List<Double> data = [1.0, 1.1, 1.2, 1.3, 1.4, 2.0, 3.0, 5.0, 10.0, 20.0]

    def result = Lilliefors.testNormality(data)

    // Verify the test runs and produces valid output
    assertNotNull(result, 'Result should not be null')
    assertTrue(result.statistic > 0, 'Test statistic should be positive')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'P-value should be in [0, 1]')
  }

  @Test
  void testStatisticInRange() {
    // Test that the D statistic is always in [0, 1]
    List<Double> data = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0]

    def result = Lilliefors.testNormality(data)

    assertTrue(result.statistic >= 0, 'D statistic should be >= 0')
    assertTrue(result.statistic <= 1, 'D statistic should be <= 1')
  }

  @Test
  void testPValueRange() {
    // Test that p-value is always in [0, 1]
    List<List<Double>> dataSets = [
      [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0],
      [0.5, 1.5, 2.5, 3.5, 4.5, 5.5, 6.5, 7.5],
      [10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0],
      [-5.0, -3.0, -1.0, 1.0, 3.0, 5.0, 7.0, 9.0]
    ]

    for (List<Double> data : dataSets) {
      def result = Lilliefors.testNormality(data)
      assertTrue(result.pValue >= 0, "P-value should be >= 0 for data: ${data}")
      assertTrue(result.pValue <= 1, "P-value should be <= 1 for data: ${data}")
    }
  }

  @Test
  void testEvaluateConsistency() {
    List<Double> data = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0]

    def result = Lilliefors.testNormality(data, 0.05)
    String evaluation = result.evaluate()

    // Evaluation should be consistent with p-value and alpha
    if (result.pValue < 0.05) {
      assertTrue(evaluation.contains('Reject H0'), 'Should reject when p < alpha')
    } else {
      assertTrue(evaluation.contains('Fail to reject'), 'Should not reject when p >= alpha')
    }
  }
}
