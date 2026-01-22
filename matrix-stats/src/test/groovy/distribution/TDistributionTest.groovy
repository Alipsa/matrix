package distribution

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.distribution.TDistribution

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for TDistribution class.
 * Reference values from Apache Commons Math 3.6.1 (used only for test comparisons).
 *
 * Note: The production implementation uses a custom self-contained implementation
 * with no external dependencies, achieving 1e-10 precision or better.
 * Apache Commons Math is used only in tests for reference value comparisons.
 */
class TDistributionTest {

  private static final double TOLERANCE = 1e-10

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
  void testCdfAgainstApacheCommonsMath() {
    // Values verified against Apache Commons Math 3.6.1
    def dist = new TDistribution(10)
    assertEquals(0.9177463367772797, dist.cdf(1.5), TOLERANCE, 't=1.5, df=10')
    assertEquals(0.08225366322272028, dist.cdf(-1.5), TOLERANCE, 't=-1.5, df=10')

    dist = new TDistribution(9)
    assertEquals(0.9735654327994362, dist.cdf(2.228), TOLERANCE, 't=2.228, df=9')

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
    // Values from Apache Commons Math 3.6.1
    def dist = new TDistribution(9)
    assertEquals(0.05001284550245466, dist.twoTailedPValue(2.262), TOLERANCE, 't=2.262, df=9')
    assertEquals(0.05001284550245466, dist.twoTailedPValue(-2.262), TOLERANCE, 't=-2.262, df=9')

    dist = new TDistribution(4)
    assertEquals(0.03347112132993746, dist.twoTailedPValue(3.182), TOLERANCE, 't=3.182, df=4')
  }

  @Test
  void testOneTailedPValueUpper() {
    // Values from Apache Commons Math 3.6.1
    def dist = new TDistribution(10)
    assertEquals(0.04835006287863486, dist.oneTailedPValueUpper(1.833), TOLERANCE, 't=1.833, df=10')
  }

  @Test
  void testOneTailedPValueLower() {
    // Values from Apache Commons Math 3.6.1
    def dist = new TDistribution(10)
    assertEquals(0.04835006287863485, dist.oneTailedPValueLower(-1.833), TOLERANCE, 't=-1.833, df=10')
  }

  @Test
  void testStaticPValueMethod() {
    // Values from Apache Commons Math 3.6.1
    assertEquals(0.024505803246513747, TDistribution.pValue(2.5, 15), TOLERANCE, 't=2.5, df=15')
  }

  @Test
  void testOneSampleTTest() {
    // Sample: mean=11.5, sd=1.58, n=10, t=(11.5-10)/(1.58/sqrt(10))=3.0
    // p-value from Apache Commons Math for t=3, df=9
    double[] sample = [10, 12, 11, 13, 9, 14, 10, 11, 12, 13] as double[]
    double pValue = TDistribution.oneSampleTTest(10, sample)
    assertEquals(0.014956363910414217, pValue, TOLERANCE, 'one-sample t-test against mu=10')

    // t.test(x, mu=11.5) - testing against sample mean should give p≈1
    pValue = TDistribution.oneSampleTTest(11.5, sample)
    assertTrue(pValue > 0.99, 'one-sample t-test against sample mean should have p≈1')
  }

  @Test
  void testTwoSampleTTest() {
    // Welch Two Sample t-test
    // t = -3.9001, df = 7.2746 (Welch-Satterthwaite)
    double[] sample1 = [10, 12, 11, 13, 9] as double[]
    double[] sample2 = [14, 15, 13, 16, 14] as double[]
    double pValue = TDistribution.twoSampleTTest(sample1, sample2)
    assertEquals(0.005470215157182601, pValue, TOLERANCE, 'two-sample t-test (Welch)')
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
    // Values from Apache Commons Math 3.6.1
    def dist = new TDistribution(1000)
    assertEquals(0.9748634075221323, dist.cdf(1.96), TOLERANCE, 'high df approaches normal')
  }

  @Test
  void testLowDegreesOfFreedom() {
    // Values from Apache Commons Math 3.6.1
    def dist = new TDistribution(1)
    assertEquals(0.8524163823495667, dist.cdf(2.0), TOLERANCE, 'df=1')

    dist = new TDistribution(2)
    assertEquals(0.7886751345948129, dist.cdf(1.0), TOLERANCE, 'df=2')
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
    // Values from Apache Commons Math 3.6.1
    def dist = new TDistribution(7.5)
    assertEquals(0.9127597147067676, dist.cdf(1.5), TOLERANCE, 'fractional df=7.5')
  }
}
