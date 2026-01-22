package normality

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.normality.ShapiroFrancia

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for the Shapiro-Francia normality test.
 */
class ShapiroFranciaTest {

  private static final double TOLERANCE = 0.01

  @Test
  void testNormalDistribution() {
    // Generate data from normal distribution
    List<Double> data = []
    Random rnd = new Random(42)
    for (int i = 0; i < 100; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = ShapiroFrancia.test(data)

    assertNotNull(result, 'Result should not be null')
    assertEquals(100, result.sampleSize, 'Sample size')
    assertTrue(result.W >= 0 && result.W <= 1, 'W\' should be in [0, 1]')
    assertTrue(result.pValue >= 0 && result.pValue <= 1, 'p-value should be in [0, 1]')

    // Normal data should have high W' value (close to 1)
    assertTrue(result.W > 0.9, 'Normal data should have W\' > 0.9')
  }

  @Test
  void testUniformDistribution() {
    // Uniform distribution is not normal
    List<Double> data = []
    Random rnd = new Random(123)
    for (int i = 0; i < 100; i++) {
      data.add(rnd.nextDouble())
    }

    def result = ShapiroFrancia.test(data)

    assertNotNull(result, 'Result should not be null')
    // Uniform should have lower W' value than normal
    assertTrue(result.W < 1.0, 'W\' should be < 1 for non-normal data')
  }

  @Test
  void testExponentialDistribution() {
    // Exponential distribution is highly non-normal
    List<Double> data = []
    Random rnd = new Random(456)
    for (int i = 0; i < 100; i++) {
      data.add(-Math.log(rnd.nextDouble()))  // Exponential(1)
    }

    def result = ShapiroFrancia.test(data)

    assertNotNull(result, 'Result should not be null')
    // Exponential should have lower W' than normal data
    // (We don't strictly require p < 0.05 as it depends on the specific sample)
    assertTrue(result.W < 0.99, 'Exponential data should have W\' < 0.99')
  }

  @Test
  void testSmallSample() {
    // Small sample of normal-ish data
    List<Double> data = [0.5, 1.2, -0.3, 0.8, -0.5, 0.2, 1.0, -0.8, 0.3, 1.5]

    def result = ShapiroFrancia.test(data)

    assertNotNull(result, 'Result should not be null')
    assertEquals(10, result.sampleSize, 'Sample size')
    assertTrue(result.W >= 0 && result.W <= 1, 'W\' should be in [0, 1]')
  }

  @Test
  void testValidation() {
    // Null data
    assertThrows(IllegalArgumentException) {
      ShapiroFrancia.test(null)
    }

    // Empty data
    assertThrows(IllegalArgumentException) {
      ShapiroFrancia.test([])
    }

    // Too few observations (< 5)
    assertThrows(IllegalArgumentException) {
      ShapiroFrancia.test([1.0, 2.0, 3.0, 4.0])
    }

    // Too many observations
    List<Double> tooManyData = (1..6000).collect { it.doubleValue() }
    assertThrows(IllegalArgumentException) {
      ShapiroFrancia.test(tooManyData)
    }
  }

  @Test
  void testConstantData() {
    // Constant data should be rejected (zero variance)
    List<Double> constantData = (1..50).collect { 5.0 }

    assertThrows(IllegalArgumentException) {
      ShapiroFrancia.test(constantData)
    }
  }

  @Test
  void testNearConstantData() {
    // Near-constant data (very small variance)
    List<Double> nearConstantData = (1..50).collect { 5.0 + it * 1e-12 }

    assertThrows(IllegalArgumentException) {
      ShapiroFrancia.test(nearConstantData)
    }
  }

  @Test
  void testArrayInput() {
    // Test with array input
    double[] data = new double[50]
    Random rnd = new Random(789)
    for (int i = 0; i < 50; i++) {
      data[i] = rnd.nextGaussian()
    }

    def result = ShapiroFrancia.test(data)

    assertNotNull(result, 'Result should not be null')
    assertEquals(50, result.sampleSize, 'Sample size')
  }

  @Test
  void testStatisticRange() {
    List<Double> data = []
    Random rnd = new Random(321)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = ShapiroFrancia.test(data)

    // W' must always be between 0 and 1
    assertTrue(result.W >= 0.0, 'W\' should be >= 0')
    assertTrue(result.W <= 1.0, 'W\' should be <= 1')
  }

  @Test
  void testResultToString() {
    List<Double> data = []
    Random rnd = new Random(654)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = ShapiroFrancia.test(data)

    String str = result.toString()
    assertTrue(str.contains('Shapiro-Francia'), 'Should contain test name')
    assertTrue(str.contains('Sample size'), 'Should contain sample size')
    assertTrue(str.contains('W\''), 'Should contain test statistic')
    assertTrue(str.contains('p-value'), 'Should contain p-value')
  }

  @Test
  void testInterpret() {
    List<Double> data = []
    Random rnd = new Random(987)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = ShapiroFrancia.test(data)

    String interpretation = result.interpret()
    assertNotNull(interpretation, 'Interpretation should not be null')
    assertTrue(interpretation.contains('H0'), 'Should mention null hypothesis')
    assertTrue(interpretation.contains('W\''), 'Should contain W\' value')
  }

  @Test
  void testInterpretWithDifferentAlpha() {
    List<Double> data = []
    Random rnd = new Random(147)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = ShapiroFrancia.test(data)

    String interp05 = result.interpret(0.05)
    String interp01 = result.interpret(0.01)

    assertNotNull(interp05, 'Interpretation at 5% should not be null')
    assertNotNull(interp01, 'Interpretation at 1% should not be null')
  }

  @Test
  void testEvaluate() {
    List<Double> data = []
    Random rnd = new Random(258)
    for (int i = 0; i < 50; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = ShapiroFrancia.test(data)

    String evaluation = result.evaluate()
    assertNotNull(evaluation, 'Evaluation should not be null')
    assertTrue(evaluation.contains('W\''), 'Should contain statistic info')
    assertTrue(evaluation.contains('Conclusion'), 'Should contain conclusion')
  }

  @Test
  void testMeanAndStdDev() {
    // Test with known mean and std dev
    List<Double> data = [1.0, 2.0, 3.0, 4.0, 5.0]

    def result = ShapiroFrancia.test(data)

    assertEquals(3.0, result.mean, TOLERANCE, 'Mean should be 3.0')
    // Sample std dev = sqrt(2.5) ≈ 1.581
    assertEquals(1.581, result.stdDev, 0.01, 'Std dev should be approximately 1.581')
  }

  @Test
  void testBimodalDistribution() {
    // Mixture of two normal distributions (bimodal)
    List<Double> data = []
    Random rnd = new Random(369)
    for (int i = 0; i < 50; i++) {
      if (rnd.nextBoolean()) {
        data.add(rnd.nextGaussian() - 2.0)  // First mode at -2
      } else {
        data.add(rnd.nextGaussian() + 2.0)  // Second mode at +2
      }
    }

    def result = ShapiroFrancia.test(data)

    assertNotNull(result, 'Result should not be null')
    // Bimodal should typically have lower W' than unimodal normal
    assertTrue(result.W < 1.0, 'Bimodal data should have W\' < 1')
  }

  @Test
  void testSkewedDistribution() {
    // Right-skewed data (chi-squared-like)
    List<Double> data = []
    Random rnd = new Random(741)
    for (int i = 0; i < 100; i++) {
      // Sum of squared normals ~ chi-squared
      double val = 0.0
      for (int j = 0; j < 3; j++) {
        double z = rnd.nextGaussian()
        val += z * z
      }
      data.add(val)
    }

    def result = ShapiroFrancia.test(data)

    assertNotNull(result, 'Result should not be null')
    // Skewed data should have lower W' than normal data
    // (We don't strictly require p < 0.05 as it depends on the specific sample)
    assertTrue(result.W < 0.99, 'Skewed data should have W\' < 0.99')
  }

  @Test
  void testLargeSample() {
    // Test with larger sample (where Shapiro-Francia is preferred over Shapiro-Wilk)
    List<Double> data = []
    Random rnd = new Random(852)
    for (int i = 0; i < 500; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = ShapiroFrancia.test(data)

    assertEquals(500, result.sampleSize, 'Sample size')
    assertNotNull(result, 'Should handle large samples')
    assertTrue(result.W > 0.9, 'Normal data should have high W\' even for large n')
  }

  @Test
  void testPValueRange() {
    // Test multiple random series to ensure p-values are always valid
    Random rnd = new Random(963)
    for (int trial = 0; trial < 10; trial++) {
      List<Double> data = []
      for (int i = 0; i < 50; i++) {
        data.add(rnd.nextGaussian())
      }

      def result = ShapiroFrancia.test(data)

      assertTrue(result.pValue >= 0, "p-value should be >= 0, got ${result.pValue}")
      assertTrue(result.pValue <= 1, "p-value should be <= 1, got ${result.pValue}")
    }
  }

  @Test
  void testNullValuesInData() {
    List<Double> data = [1.0, 2.0, null, 4.0, 5.0, 6.0]

    assertThrows(IllegalArgumentException) {
      ShapiroFrancia.test(data)
    }
  }

  @Test
  void testHighlyNonNormalData() {
    // Cauchy-like heavy-tailed data
    List<Double> data = []
    Random rnd = new Random(159)
    for (int i = 0; i < 100; i++) {
      // Ratio of normals ~ Cauchy
      double numerator = rnd.nextGaussian()
      double denominator = rnd.nextGaussian()
      if (Math.abs(denominator) > 0.1) {  // Avoid extreme values
        data.add(numerator / denominator)
      } else {
        i--  // Retry
      }
    }

    def result = ShapiroFrancia.test(data)

    assertNotNull(result, 'Result should not be null')
    // Heavy-tailed data should reject normality
    assertTrue(result.W < 0.95, 'Heavy-tailed data should have lower W\'')
  }

  @Test
  void testMinimalSample() {
    // Test with exactly 5 observations (minimum)
    List<Double> data = [1.0, 2.0, 3.0, 4.0, 5.0]

    def result = ShapiroFrancia.test(data)

    assertNotNull(result, 'Should handle minimal sample size')
    assertEquals(5, result.sampleSize, 'Sample size')
  }

  @Test
  void testMediumSample() {
    // Test with medium-sized sample
    List<Double> data = []
    Random rnd = new Random(357)
    for (int i = 0; i < 200; i++) {
      data.add(rnd.nextGaussian())
    }

    def result = ShapiroFrancia.test(data)

    assertEquals(200, result.sampleSize, 'Sample size')
    assertTrue(result.W > 0.95, 'Normal data should have high W\' for medium sample')
  }
}
