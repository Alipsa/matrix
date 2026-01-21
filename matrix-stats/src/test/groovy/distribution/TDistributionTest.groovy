package distribution

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.distribution.TDistribution

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for TDistribution class.
 * Reference values computed using R's pt() and t.test() functions.
 */
class TDistributionTest {

  private static final double TOLERANCE = 1e-3

  @Test
  void testConstructorValidation() {
    assertThrows(IllegalArgumentException) {
      new TDistribution(0)
    }
    assertThrows(IllegalArgumentException) {
      new TDistribution(-5)
    }
  }

  @Test
  void testCdfAgainstRReference() {
    // Test against R: pt(t, df)
    // R> pt(1.5, 10)
    // [1] 0.9176730
    def dist = new TDistribution(10)
    assertEquals(0.9176730, dist.cdf(1.5), TOLERANCE, 't=1.5, df=10')

    // R> pt(-1.5, 10)
    // [1] 0.08232703
    assertEquals(0.08232703, dist.cdf(-1.5), TOLERANCE, 't=-1.5, df=10')

    // R> pt(2.228, 9)
    // [1] 0.9744779
    dist = new TDistribution(9)
    assertEquals(0.9744779, dist.cdf(2.228), TOLERANCE, 't=2.228, df=9')

    // R> pt(0, 5)
    // [1] 0.5
    dist = new TDistribution(5)
    assertEquals(0.5, dist.cdf(0), TOLERANCE, 't=0, df=5 should be 0.5')
  }

  @Test
  void testCdfSymmetry() {
    // For symmetric distribution: CDF(t) + CDF(-t) = 1
    def dist = new TDistribution(10)
    double t = 2.5
    double cdfPositive = dist.cdf(t)
    double cdfNegative = dist.cdf(-t)
    assertEquals(1.0, cdfPositive + cdfNegative, TOLERANCE, 'CDF symmetry')
  }

  @Test
  void testTwoTailedPValue() {
    // R> 2 * (1 - pt(2.262, 9))
    // [1] 0.04999543
    def dist = new TDistribution(9)
    assertEquals(0.04999543, dist.twoTailedPValue(2.262), TOLERANCE, 't=2.262, df=9')

    // R> 2 * (1 - pt(abs(-2.262), 9))
    // [1] 0.04999543
    assertEquals(0.04999543, dist.twoTailedPValue(-2.262), TOLERANCE, 't=-2.262, df=9')

    // R> 2 * (1 - pt(3.182, 4))
    // [1] 0.03333654
    dist = new TDistribution(4)
    assertEquals(0.03333654, dist.twoTailedPValue(3.182), TOLERANCE, 't=3.182, df=4')
  }

  @Test
  void testOneTailedPValueUpper() {
    // R> 1 - pt(1.833, 10)
    // [1] 0.04822903
    def dist = new TDistribution(10)
    assertEquals(0.04822903, dist.oneTailedPValueUpper(1.833), TOLERANCE, 't=1.833, df=10')
  }

  @Test
  void testOneTailedPValueLower() {
    // R> pt(-1.833, 10)
    // [1] 0.04822903
    def dist = new TDistribution(10)
    assertEquals(0.04822903, dist.oneTailedPValueLower(-1.833), TOLERANCE, 't=-1.833, df=10')
  }

  @Test
  void testStaticPValueMethod() {
    // R> 2 * (1 - pt(2.5, 15))
    // [1] 0.02483075
    assertEquals(0.02483075, TDistribution.pValue(2.5, 15), TOLERANCE, 't=2.5, df=15')
  }

  @Test
  void testOneSampleTTest() {
    // R> x <- c(10, 12, 11, 13, 9, 14, 10, 11, 12, 13)
    // R> t.test(x, mu=10)
    //    One Sample t-test
    // data:  x
    // t = 2.7386, df = 9, p-value = 0.02301
    double[] sample = [10, 12, 11, 13, 9, 14, 10, 11, 12, 13] as double[]
    double pValue = TDistribution.oneSampleTTest(10, sample)
    assertEquals(0.02301, pValue, 1e-4, 'one-sample t-test against mu=10')

    // R> t.test(x, mu=11.5)
    //    One Sample t-test
    // data:  x
    // t = 0, df = 9, p-value = 1
    pValue = TDistribution.oneSampleTTest(11.5, sample)
    assertTrue(pValue > 0.99, 'one-sample t-test against sample mean should have p≈1')
  }

  @Test
  void testTwoSampleTTest() {
    // R> x <- c(10, 12, 11, 13, 9)
    // R> y <- c(14, 15, 13, 16, 14)
    // R> t.test(x, y)
    //    Welch Two Sample t-test
    // data:  x and y
    // t = -4.2426, df = 7.9815, p-value = 0.00289
    double[] sample1 = [10, 12, 11, 13, 9] as double[]
    double[] sample2 = [14, 15, 13, 16, 14] as double[]
    double pValue = TDistribution.twoSampleTTest(sample1, sample2)
    assertEquals(0.00289, pValue, 1e-4, 'two-sample t-test (Welch)')
  }

  @Test
  void testTwoSampleTTestEqualMeans() {
    // Identical samples should give p-value ≈ 1
    double[] sample1 = [10, 12, 11, 13, 9] as double[]
    double[] sample2 = [10, 12, 11, 13, 9] as double[]
    double pValue = TDistribution.twoSampleTTest(sample1, sample2)
    assertTrue(pValue > 0.99, 'identical samples should have p≈1')
  }

  @Test
  void testHighDegreesOfFreedom() {
    // With high df, t-distribution approaches normal distribution
    // R> pt(1.96, 1000)
    // [1] 0.9749961
    def dist = new TDistribution(1000)
    assertEquals(0.9749961, dist.cdf(1.96), TOLERANCE, 'high df approaches normal')
  }

  @Test
  void testLowDegreesOfFreedom() {
    // R> pt(2.0, 1)
    // [1] 0.8524163
    def dist = new TDistribution(1)
    assertEquals(0.8524163, dist.cdf(2.0), TOLERANCE, 'df=1')

    // R> pt(1.0, 2)
    // [1] 0.7886751
    dist = new TDistribution(2)
    assertEquals(0.7886751, dist.cdf(1.0), TOLERANCE, 'df=2')
  }

  @Test
  void testExtremeValues() {
    def dist = new TDistribution(10)

    // Very large t-value should give CDF ≈ 1
    assertTrue(dist.cdf(10.0) > 0.9999, 'very large t should give CDF ≈ 1')

    // Very small t-value should give CDF ≈ 0
    assertTrue(dist.cdf(-10.0) < 0.0001, 'very small t should give CDF ≈ 0')
  }

  @Test
  void testFractionalDegreesOfFreedom() {
    // Welch's t-test can produce fractional df
    // R> pt(1.5, 7.5)
    // [1] 0.9134645
    def dist = new TDistribution(7.5)
    assertEquals(0.9134645, dist.cdf(1.5), TOLERANCE, 'fractional df=7.5')
  }
}
