package timeseries

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.timeseries.DurbinWatson

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for the Durbin-Watson test.
 */
class DurbinWatsonTest {

  private static final double TOLERANCE = 0.01

  @Test
  void testNoAutocorrelation() {
    // Random residuals with no autocorrelation
    // DW should be close to 2
    List<Double> residuals = [0.5, -0.3, 0.8, -0.2, 0.1, -0.6, 0.4, -0.1, 0.3, -0.4]

    def result = DurbinWatson.test(residuals)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.statistic >= 0 && result.statistic <= 4, 'DW statistic should be in [0, 4]')
    assertEquals(10, result.sampleSize, 'Sample size')
  }

  @Test
  void testPositiveAutocorrelation() {
    // Residuals with positive autocorrelation
    // DW should be < 2
    List<Double> residuals = [1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9]

    def result = DurbinWatson.test(residuals)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.statistic < 2.0, 'DW statistic should be < 2 for positive autocorrelation')
    assertTrue(result.autocorrelation > 0, 'Autocorrelation should be positive')
  }

  @Test
  void testNegativeAutocorrelation() {
    // Residuals with negative autocorrelation (alternating pattern)
    // DW should be > 2
    List<Double> residuals = [1.0, -1.0, 1.0, -1.0, 1.0, -1.0, 1.0, -1.0, 1.0, -1.0]

    def result = DurbinWatson.test(residuals)

    assertNotNull(result, 'Result should not be null')
    assertTrue(result.statistic > 2.0, 'DW statistic should be > 2 for negative autocorrelation')
    assertTrue(result.autocorrelation < 0, 'Autocorrelation should be negative')
  }

  @Test
  void testValidation() {
    // Null residuals
    assertThrows(IllegalArgumentException) {
      DurbinWatson.test(null)
    }

    // Empty residuals
    assertThrows(IllegalArgumentException) {
      DurbinWatson.test([])
    }

    // Too few observations
    assertThrows(IllegalArgumentException) {
      DurbinWatson.test([1.0, 2.0])
    }

    // Null values in residuals
    assertThrows(IllegalArgumentException) {
      DurbinWatson.test([1.0, null, 3.0, 4.0])
    }
  }

  @Test
  void testAlternativeHypothesis() {
    List<Double> residuals = [1.0, 1.1, 1.2, 1.3, 1.4, 1.5]

    // Test with different alternatives
    def resultTwoSided = DurbinWatson.test(residuals, "two.sided")
    def resultGreater = DurbinWatson.test(residuals, "greater")
    def resultLess = DurbinWatson.test(residuals, "less")

    assertEquals("two.sided", resultTwoSided.alternative, 'Two-sided alternative')
    assertEquals("greater", resultGreater.alternative, 'Greater alternative')
    assertEquals("less", resultLess.alternative, 'Less alternative')

    // Invalid alternative
    assertThrows(IllegalArgumentException) {
      DurbinWatson.test(residuals, "invalid")
    }
  }

  @Test
  void testResultToString() {
    List<Double> residuals = [1.0, 2.0, 3.0, 4.0, 5.0]

    def result = DurbinWatson.test(residuals)

    String str = result.toString()
    assertTrue(str.contains('Durbin-Watson Test'), 'Should contain test name')
    assertTrue(str.contains('Sample size'), 'Should contain sample size')
    assertTrue(str.contains('Test statistic'), 'Should contain test statistic')
    assertTrue(str.contains('Autocorrelation'), 'Should contain autocorrelation')
  }

  @Test
  void testInterpretation() {
    List<Double> residuals = [1.0, 1.1, 1.2, 1.3, 1.4, 1.5]

    def result = DurbinWatson.test(residuals)

    String interpretation = result.interpret()
    assertNotNull(interpretation, 'Interpretation should not be null')
    assertTrue(interpretation.length() > 0, 'Interpretation should not be empty')
  }

  @Test
  void testEvaluate() {
    List<Double> residuals = [1.0, 1.1, 1.2, 1.3, 1.4, 1.5]

    def result = DurbinWatson.test(residuals)

    String evaluation = result.evaluate()
    assertNotNull(evaluation, 'Evaluation should not be null')
    assertTrue(evaluation.contains('Durbin-Watson statistic'), 'Should contain statistic info')
    assertTrue(evaluation.contains('autocorrelation'), 'Should contain autocorrelation info')
  }

  @Test
  void testStatisticFormula() {
    // Test with known values
    // Residuals: [1, 2, 3]
    // Numerator: (2-1)² + (3-2)² = 1 + 1 = 2
    // Denominator: 1² + 2² + 3² = 1 + 4 + 9 = 14
    // DW = 2/14 = 0.142857...
    List<Double> residuals = [1.0, 2.0, 3.0]

    def result = DurbinWatson.test(residuals)

    assertEquals(2.0 / 14.0, result.statistic, 0.001, 'DW statistic calculation')
  }

  @Test
  void testAutocorrelationFormula() {
    // DW = 2 should give ρ ≈ 0
    List<Double> residuals = [0.5, -0.3, 0.8, -0.2, 0.1, -0.6, 0.4, -0.1]

    def result = DurbinWatson.test(residuals)

    // ρ ≈ 1 - (DW / 2)
    double expectedRho = 1.0 - (result.statistic / 2.0)
    assertEquals(expectedRho, result.autocorrelation, 0.001, 'Autocorrelation calculation')
  }

  @Test
  void testZeroResiduals() {
    // All zeros should handle gracefully (though not a realistic scenario)
    List<Double> residuals = [0.0, 0.0, 0.0, 0.0]

    // This will cause division by zero - the result will be NaN or Infinity
    def result = DurbinWatson.test(residuals)
    assertNotNull(result, 'Result should not be null even with zero residuals')
  }
}
