package regression

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.regression.LogisticRegression

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for the LogisticRegression class.
 */
class LogisticRegressionTest {

  private static final double TOLERANCE = 0.1

  @Test
  void testBasicLogisticRegression() {
    // Simple separable data
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
    List<Integer> y = [0, 0, 0, 1, 1, 1]

    def lr = new LogisticRegression(x, y)

    assertNotNull(lr, 'LogisticRegression should not be null')
    assertNotNull(lr.slope, 'Slope should not be null')
    assertNotNull(lr.intercept, 'Intercept should not be null')

    // For this data, slope should be positive (increasing x increases P(Y=1))
    assertTrue(lr.slope > 0, 'Slope should be positive for increasing pattern')
  }

  @Test
  void testPredictProbability() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
    List<Integer> y = [0, 0, 0, 1, 1, 1]

    def lr = new LogisticRegression(x, y)

    // Probability at low x should be closer to 0
    BigDecimal prob1 = lr.predictProbability(1.0)
    assertTrue(prob1 < 0.5, 'Probability at x=1 should be < 0.5')

    // Probability at high x should be closer to 1
    BigDecimal prob6 = lr.predictProbability(6.0)
    assertTrue(prob6 > 0.5, 'Probability at x=6 should be > 0.5')

    // Probability should be between 0 and 1
    assertTrue(prob1 >= 0 && prob1 <= 1, 'Probability should be in [0, 1]')
    assertTrue(prob6 >= 0 && prob6 <= 1, 'Probability should be in [0, 1]')
  }

  @Test
  void testPredictClass() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
    List<Integer> y = [0, 0, 0, 1, 1, 1]

    def lr = new LogisticRegression(x, y)

    // Predict class at low and high x
    int class1 = lr.predict(1.0)
    int class6 = lr.predict(6.0)

    // Classes should be 0 or 1
    assertTrue(class1 == 0 || class1 == 1, 'Class should be 0 or 1')
    assertTrue(class6 == 0 || class6 == 1, 'Class should be 0 or 1')

    // For this data, class at x=1 should be 0, at x=6 should be 1
    assertEquals(0, class1, 'Class at x=1 should be 0')
    assertEquals(1, class6, 'Class at x=6 should be 1')
  }

  @Test
  void testPredictList() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
    List<Integer> y = [0, 0, 0, 1, 1, 1]

    def lr = new LogisticRegression(x, y)

    // Predict for multiple values
    List<BigDecimal> probs = lr.predictProbability([1.0, 3.5, 6.0])
    assertEquals(3, probs.size(), 'Should have 3 probabilities')

    List<Integer> classes = lr.predict([1.0, 3.5, 6.0])
    assertEquals(3, classes.size(), 'Should have 3 classes')
  }

  @Test
  void testThreshold() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
    List<Integer> y = [0, 0, 0, 1, 1, 1]

    def lr = new LogisticRegression(x, y)

    // Default threshold is 0.5
    assertEquals(0.5, lr.threshold, 0.001, 'Default threshold should be 0.5')

    // Change threshold
    lr.setThreshold(0.7)
    assertEquals(0.7, lr.threshold, 0.001, 'Threshold should be updated to 0.7')

    // Invalid threshold
    assertThrows(IllegalArgumentException) {
      lr.setThreshold(0.0)
    }
    assertThrows(IllegalArgumentException) {
      lr.setThreshold(1.0)
    }
  }

  @Test
  void testValidation() {
    // Mismatched sizes
    assertThrows(IllegalArgumentException) {
      new LogisticRegression([1.0, 2.0], [0, 1, 0])
    }

    // Too few data points
    assertThrows(IllegalArgumentException) {
      new LogisticRegression([1.0, 2.0], [0, 1])
    }

    // Non-binary y values
    assertThrows(IllegalArgumentException) {
      new LogisticRegression([1.0, 2.0, 3.0], [0, 1, 2])
    }

    assertThrows(IllegalArgumentException) {
      new LogisticRegression([1.0, 2.0, 3.0], [0.5, 0.3, 0.8])
    }
  }

  @Test
  void testGetters() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
    List<Integer> y = [0, 0, 0, 1, 1, 1]

    def lr = new LogisticRegression(x, y)

    // Test getters with rounding
    BigDecimal slope2 = lr.getSlope(2)
    assertEquals(2, slope2.scale(), 'Slope should be rounded to 2 decimals')

    BigDecimal intercept2 = lr.getIntercept(2)
    assertEquals(2, intercept2.scale(), 'Intercept should be rounded to 2 decimals')

    // Test getters without rounding
    BigDecimal slopeNoRound = lr.getSlope(-1)
    assertEquals(lr.slope, slopeNoRound, 'Slope without rounding should equal raw slope')
  }

  @Test
  void testLogOdds() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
    List<Integer> y = [0, 0, 0, 1, 1, 1]

    def lr = new LogisticRegression(x, y)

    // Log-odds should be β0 + β1*x
    BigDecimal logOdds3 = lr.logOdds(3.0)
    BigDecimal expected = lr.intercept + lr.slope * 3.0
    assertEquals(expected, logOdds3, 'Log-odds should equal β0 + β1*x')
  }

  @Test
  void testToString() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
    List<Integer> y = [0, 0, 0, 1, 1, 1]

    def lr = new LogisticRegression(x, y)

    String str = lr.toString()
    assertTrue(str.contains('P('), 'String should contain probability notation')
    assertTrue(str.contains('exp'), 'String should contain exponential function')
    assertTrue(str.contains('Y'), 'String should contain dependent variable name')
  }

  @Test
  void testPerfectSeparation() {
    // Data with perfect separation
    List<Double> x = [1.0, 2.0, 10.0, 11.0]
    List<Integer> y = [0, 0, 1, 1]

    // Should still work (optimizer might have trouble, but should not crash)
    def lr = new LogisticRegression(x, y)

    assertNotNull(lr, 'Should handle perfect separation')
    assertTrue(lr.slope > 0, 'Slope should be positive')
  }

  @Test
  void testPredictProbabilityRounding() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
    List<Integer> y = [0, 0, 0, 1, 1, 1]

    def lr = new LogisticRegression(x, y)

    BigDecimal prob = lr.predictProbability(3.5, 3)
    assertEquals(3, prob.scale(), 'Probability should be rounded to 3 decimals')
  }

  @Test
  void testSigmoidProperties() {
    List<Double> x = [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
    List<Integer> y = [0, 0, 0, 0, 1, 1, 1, 1]

    def lr = new LogisticRegression(x, y)

    // Probabilities should increase monotonically with x (for positive slope)
    BigDecimal prob1 = lr.predictProbability(1.0)
    BigDecimal prob4 = lr.predictProbability(4.0)
    BigDecimal prob8 = lr.predictProbability(8.0)

    assertTrue(prob1 < prob4, 'Probability should increase with x')
    assertTrue(prob4 < prob8, 'Probability should increase with x')
  }
}
