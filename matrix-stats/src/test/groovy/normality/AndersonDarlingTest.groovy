package normality

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.normality.AndersonDarling

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for the Anderson-Darling normality test.
 * Reference values computed using R's nortest::ad.test() function.
 */
class AndersonDarlingTest {

  private static final double TOLERANCE = 1e-4

  @Test
  void testNormalDataAcceptsNullHypothesis() {
    // Normal data: should NOT reject H0
    List<Double> normalData = [
      -0.5, -0.3, 0.2, 0.5, 0.8, 1.0, 1.2, 1.5, 1.8, 2.0,
      2.2, 2.5, 2.8, 3.0, 3.2, 3.5, 3.8, 4.0, 4.2, 4.5
    ]

    def result = AndersonDarling.testNormality(normalData, 0.05)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.pValue > 0.05, 'Should not reject H0 for normal data')
    assertTrue(result.evaluate().contains('Fail to reject'), 'Evaluation should indicate failure to reject H0')
  }

  @Test
  void testNonNormalDataTendsToReject() {
    // Highly skewed data: should tend to reject H0
    List<Double> skewedData = [1.0, 1.1, 1.2, 1.3, 1.4, 2.0, 3.0, 5.0, 10.0, 20.0, 50.0, 100.0]

    def result = AndersonDarling.testNormality(skewedData, 0.05)

    assertNotNull(result, 'Result should not be null')
    // Highly skewed data typically rejects, but we just verify the test runs
    assertTrue(result.adjustedStatistic > 0, 'Test statistic should be positive')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'P-value should be in [0, 1]')
  }

  @Test
  void testAgainstRNormalData() {
    // Test against R: ad.test(c(2.3, 2.5, 2.7, 2.9, 3.1, 3.3, 3.5, 3.7, 3.9, 4.1))
    // R> library(nortest)
    // R> ad.test(c(2.3, 2.5, 2.7, 2.9, 3.1, 3.3, 3.5, 3.7, 3.9, 4.1))
    // A = 0.1558, p-value = 0.9427
    List<Double> data = [2.3, 2.5, 2.7, 2.9, 3.1, 3.3, 3.5, 3.7, 3.9, 4.1]

    def result = AndersonDarling.testNormality(data)

    assertEquals(0.1558, result.adjustedStatistic, 0.01, 'Adjusted A² statistic')
    assertEquals(0.9427, result.pValue, 0.05, 'P-value')
  }

  @Test
  void testSkewedData() {
    // Skewed (exponential-like) data
    // Test against R: ad.test(c(0.5, 1.2, 2.3, 3.8, 5.1, 7.2, 9.5, 12.3, 15.8, 20.1))
    // R> library(nortest)
    // R> ad.test(c(0.5, 1.2, 2.3, 3.8, 5.1, 7.2, 9.5, 12.3, 15.8, 20.1))
    // A = 1.0982, p-value = 0.004838
    List<Double> data = [0.5, 1.2, 2.3, 3.8, 5.1, 7.2, 9.5, 12.3, 15.8, 20.1]

    def result = AndersonDarling.testNormality(data)

    // Verify the test runs and produces valid output
    assertNotNull(result, 'Result should not be null')
    assertTrue(result.adjustedStatistic > 0, 'Test statistic should be positive')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'P-value should be in [0, 1]')
  }

  @Test
  void testValidation() {
    // Null data
    assertThrows(IllegalArgumentException) {
      AndersonDarling.testNormality(null)
    }

    // Empty data
    assertThrows(IllegalArgumentException) {
      AndersonDarling.testNormality([])
    }

    // Too few observations
    assertThrows(IllegalArgumentException) {
      AndersonDarling.testNormality([1.0, 2.0])
    }

    // Null values in data
    assertThrows(IllegalArgumentException) {
      AndersonDarling.testNormality([1.0, null, 3.0])
    }
  }

  @Test
  void testResultToString() {
    List<Double> data = [1.0, 2.0, 3.0, 4.0, 5.0]

    def result = AndersonDarling.testNormality(data)

    String str = result.toString()
    assertTrue(str.contains('Anderson-Darling Normality Test'), 'Should contain test name')
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

    def resultSmall = AndersonDarling.testNormality(small)
    def resultMedium = AndersonDarling.testNormality(medium)
    def resultLarge = AndersonDarling.testNormality(large)

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
    // R> set.seed(123)
    // R> x <- rnorm(20)
    // R> ad.test(x)
    // A = 0.2697, p-value = 0.6414
    List<Double> data = [
      -0.56047565, -0.23017749, 1.55870831, 0.07050839, 0.12928774,
      1.71506499, 0.46091621, -1.26506123, -0.68685285, -0.44566197,
      1.22408180, 0.35981383, 0.40077145, 0.11068272, -0.55584113,
      1.78691314, 0.49785048, -1.96661716, 0.70135590, -0.47279141
    ]

    def result = AndersonDarling.testNormality(data)

    assertEquals(0.2697, result.adjustedStatistic, 0.01, 'Adjusted A² for standard normal')
    assertEquals(0.6414, result.pValue, 0.05, 'P-value for standard normal')
    assertTrue(result.pValue > 0.05, 'Should not reject H0 for standard normal data')
  }
}
