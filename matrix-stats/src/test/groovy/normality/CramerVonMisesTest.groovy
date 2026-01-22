package normality

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.normality.CramerVonMises

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for the Cramér-von Mises normality test.
 */
class CramerVonMisesTest {

  private static final double TOLERANCE = 0.01

  @Test
  void testNormalDistribution() {
    // Generate data from normal distribution
    List<Double> data = []
    Random rnd = new Random(42)
    for (int i = 0; i < 100; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = CramerVonMises.testNormality(data)

    assertNotNull(result, 'Result should not be null')
    assertEquals(100, result.sampleSize, 'Sample size')
    assertTrue(result.statistic >= 0, 'W²* should be non-negative')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be in [0, 1]')

    // Normal data should typically not reject H0
    // (though we don't strictly assert this due to random variation)
  }

  @Test
  void testUniformDistribution() {
    // Uniform distribution is not normal - should reject H0
    List<Double> data = []
    Random rnd = new Random(123)
    for (int i = 0; i < 100; i++) {
      data.add(rnd.nextDouble())
    }

    def result = CramerVonMises.testNormality(data)

    assertNotNull(result, 'Result should not be null')
    // Uniform distribution should show significant departure from normality
    assertTrue(result.statistic > 0, 'Should have positive statistic')
  }

  @Test
  void testExponentialDistribution() {
    // Exponential distribution is highly non-normal
    List<Double> data = []
    Random rnd = new Random(456)
    for (int i = 0; i < 100; i++) {
      data.add(-Math.log(rnd.nextDouble()))  // Exponential(1)
    }

    def result = CramerVonMises.testNormality(data)

    assertNotNull(result, 'Result should not be null')
    // Exponential should strongly reject normality
    assertTrue(result.statistic > result.criticalValue,
               'Exponential data should reject normality')
  }

  @Test
  void testSmallSample() {
    // Small sample of normal data
    List<Double> data = [0.5, 1.2, -0.3, 0.8, -0.5, 0.2, 1.0, -0.8, 0.3, 1.5]

    def result = CramerVonMises.testNormality(data, 0.05)

    assertNotNull(result, 'Result should not be null')
    assertEquals(10, result.sampleSize, 'Sample size')
    assertTrue(result.statistic >= 0, 'Statistic should be non-negative')
  }

  @Test
  void testValidation() {
    // Null data
    assertThrows(IllegalArgumentException) {
      CramerVonMises.testNormality(null)
    }

    // Empty data
    assertThrows(IllegalArgumentException) {
      CramerVonMises.testNormality([])
    }

    // Too few observations
    assertThrows(IllegalArgumentException) {
      CramerVonMises.testNormality([1.0, 2.0])
    }

    // Invalid alpha (too small)
    assertThrows(IllegalArgumentException) {
      CramerVonMises.testNormality([1.0, 2.0, 3.0, 4.0, 5.0], 0.0)
    }

    // Invalid alpha (too large)
    assertThrows(IllegalArgumentException) {
      CramerVonMises.testNormality([1.0, 2.0, 3.0, 4.0, 5.0], 1.0)
    }

    // Invalid alpha (negative)
    assertThrows(IllegalArgumentException) {
      CramerVonMises.testNormality([1.0, 2.0, 3.0, 4.0, 5.0], -0.05)
    }
  }

  @Test
  void testConstantData() {
    // Constant data should be rejected
    List<Double> constantData = (1..50).collect { 5.0 }

    assertThrows(IllegalArgumentException) {
      CramerVonMises.testNormality(constantData)
    }
  }

  @Test
  void testDifferentAlphaLevels() {
    List<Double> data = []
    Random rnd = new Random(789)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result05 = CramerVonMises.testNormality(data, 0.05)
    def result01 = CramerVonMises.testNormality(data, 0.01)

    assertEquals(0.05, result05.alpha, TOLERANCE, 'Alpha should be 0.05')
    assertEquals(0.01, result01.alpha, TOLERANCE, 'Alpha should be 0.01')

    // Critical values should be different
    assertTrue(result01.criticalValue > result05.criticalValue,
               'Lower alpha should have higher critical value')
  }

  @Test
  void testStatisticProperties() {
    List<Double> data = []
    Random rnd = new Random(321)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = CramerVonMises.testNormality(data)

    // Modified statistic should be larger than raw statistic
    assertTrue(result.statistic >= result.rawStatistic,
               'Modified W²* should be >= raw W²')

    // Both should be non-negative
    assertTrue(result.statistic >= 0, 'Modified statistic should be non-negative')
    assertTrue(result.rawStatistic >= 0, 'Raw statistic should be non-negative')
  }

  @Test
  void testResultToString() {
    List<Double> data = []
    Random rnd = new Random(654)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = CramerVonMises.testNormality(data)

    String str = result.toString()
    assertTrue(str.contains('Cramér-von Mises'), 'Should contain test name')
    assertTrue(str.contains('Sample size'), 'Should contain sample size')
    assertTrue(str.contains('W²*'), 'Should contain test statistic')
    assertTrue(str.contains('p-value'), 'Should contain p-value')
  }

  @Test
  void testInterpret() {
    List<Double> data = []
    Random rnd = new Random(987)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = CramerVonMises.testNormality(data)

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

    def result = CramerVonMises.testNormality(data)

    String evaluation = result.evaluate()
    assertNotNull(evaluation, 'Evaluation should not be null')
    assertTrue(evaluation.contains('W²*'), 'Should contain statistic info')
    assertTrue(evaluation.contains('Conclusion'), 'Should contain conclusion')
  }

  @Test
  void testMeanAndStdDev() {
    // Test with known mean and std dev
    List<Double> data = [1.0, 2.0, 3.0, 4.0, 5.0]

    def result = CramerVonMises.testNormality(data)

    assertEquals(3.0, result.mean, TOLERANCE, 'Mean should be 3.0')
    // Sample std dev = sqrt(2.5) ≈ 1.581
    assertEquals(1.581, result.stdDev, 0.01, 'Std dev should be approximately 1.581')
  }

  @Test
  void testCriticalValues() {
    List<Double> data = []
    Random rnd = new Random(258)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    // Test various alpha levels
    def result25 = CramerVonMises.testNormality(data, 0.25)
    def result10 = CramerVonMises.testNormality(data, 0.10)
    def result05 = CramerVonMises.testNormality(data, 0.05)
    def result01 = CramerVonMises.testNormality(data, 0.01)

    // Critical values should increase as alpha decreases
    assertTrue(result25.criticalValue < result10.criticalValue,
               '25% CV < 10% CV')
    assertTrue(result10.criticalValue < result05.criticalValue,
               '10% CV < 5% CV')
    assertTrue(result05.criticalValue < result01.criticalValue,
               '5% CV < 1% CV')
  }

  @Test
  void testPValueCalculation() {
    List<Double> data = []
    Random rnd = new Random(369)
    for (int i = 0; i < 100; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = CramerVonMises.testNormality(data)

    // p-value should be valid
    assertTrue(result.pValue >= 0, 'p-value should be >= 0')
    assertTrue(result.pValue <= 1, 'p-value should be <= 1')

    // Relationship between p-value and critical value
    if (result.statistic < result.criticalValue) {
      // Should not reject, p-value should be >= alpha
      assertTrue(result.pValue >= result.alpha,
                 'When not rejecting, p-value should be >= alpha')
    }
  }

  @Test
  void testBimodalDistribution() {
    // Mixture of two normal distributions (bimodal)
    List<Double> data = []
    Random rnd = new Random(741)
    for (int i = 0; i < 50; i++) {
      if (rnd.nextBoolean()) {
        data.add(rnd.nextGaussian() - 2.0)  // First mode at -2
      } else {
        data.add(rnd.nextGaussian() + 2.0)  // Second mode at +2
      }
    }

    def result = CramerVonMises.testNormality(data)

    assertNotNull(result, 'Result should not be null')
    // Bimodal should typically reject normality
    // (though we don't strictly assert this due to random variation)
  }

  @Test
  void testLargeSample() {
    // Test with larger sample
    List<Double> data = []
    Random rnd = new Random(852)
    for (int i = 0; i < 500; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = CramerVonMises.testNormality(data)

    assertEquals(500, result.sampleSize, 'Sample size')
    assertNotNull(result, 'Should handle large samples')
    assertTrue(result.statistic >= 0, 'Statistic should be non-negative')
  }

  @Test
  void testSkewedDistribution() {
    // Right-skewed data (chi-squared-like)
    List<Double> data = []
    Random rnd = new Random(963)
    for (int i = 0; i < 100; i++) {
      // Sum of squared normals ~ chi-squared
      double val = 0.0
      for (int j = 0; j < 3; j++) {
        double z = rnd.nextGaussian()
        val += z * z
      }
      data.add(val)
    }

    def result = CramerVonMises.testNormality(data)

    assertNotNull(result, 'Result should not be null')
    // Skewed data should typically reject normality
    assertTrue(result.statistic > 0, 'Should have positive statistic')
  }

  @Test
  void testNullValuesInData() {
    List<Double> data = [1.0, 2.0, null, 4.0, 5.0]

    assertThrows(IllegalArgumentException) {
      CramerVonMises.testNormality(data)
    }
  }
}
