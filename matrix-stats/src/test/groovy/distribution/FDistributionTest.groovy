package distribution

import org.junit.jupiter.api.Test
import se.alipsa.matrix.stats.distribution.FDistribution

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for FDistribution class.
 * Reference values computed using R's pf() and aov() functions.
 */
class FDistributionTest {

  private static final double TOLERANCE = 1e-3

  @Test
  void testConstructorValidation() {
    assertThrows(IllegalArgumentException) {
      new FDistribution(0, 10)
    }
    assertThrows(IllegalArgumentException) {
      new FDistribution(10, 0)
    }
    assertThrows(IllegalArgumentException) {
      new FDistribution(-5, 10)
    }
    assertThrows(IllegalArgumentException) {
      new FDistribution(10, -5)
    }
  }

  @Test
  void testCdfValidation() {
    def dist = new FDistribution(5, 10)
    assertThrows(IllegalArgumentException) {
      dist.cdf(-1.0)
    }
  }

  @Test
  void testCdfAgainstRReference() {
    // Test against R: pf(f, df1, df2)
    // R> pf(2.5, 5, 10)
    // [1] 0.897046
    def dist = new FDistribution(5, 10)
    assertEquals(0.897046, dist.cdf(2.5), TOLERANCE, 'f=2.5, df1=5, df2=10')

    // R> pf(0, 5, 10)
    // [1] 0
    assertEquals(0.0, dist.cdf(0), TOLERANCE, 'f=0 should give CDF=0')

    // R> pf(1.0, 10, 20)
    // [1] 0.5151053
    dist = new FDistribution(10, 20)
    assertEquals(0.5151053, dist.cdf(1.0), TOLERANCE, 'f=1.0, df1=10, df2=20')

    // R> pf(4.0, 3, 15)
    // [1] 0.9728625
    dist = new FDistribution(3, 15)
    assertEquals(0.9728625, dist.cdf(4.0), TOLERANCE, 'f=4.0, df1=3, df2=15')
  }

  @Test
  void testPValue() {
    // R> 1 - pf(3.0, 2, 10)
    // [1] 0.09539903
    def dist = new FDistribution(2, 10)
    assertEquals(0.09539903, dist.pValue(3.0), TOLERANCE, 'p-value for f=3.0, df1=2, df2=10')

    // R> 1 - pf(5.14, 2, 27)
    // [1] 0.01269826
    dist = new FDistribution(2, 27)
    assertEquals(0.01269826, dist.pValue(5.14), TOLERANCE, 'p-value for f=5.14, df1=2, df2=27')
  }

  @Test
  void testStaticPValueMethod() {
    // R> 1 - pf(4.26, 3, 16)
    // [1] 0.02194892
    assertEquals(0.02194892, FDistribution.pValue(4.26, 3, 16), TOLERANCE, 'static pValue method')
  }

  @Test
  void testOneWayAnovaFValue() {
    // R> group1 <- c(10, 12, 11, 13, 9)
    // R> group2 <- c(14, 15, 13, 16, 14)
    // R> group3 <- c(8, 9, 7, 10, 8)
    // R> data <- data.frame(value = c(group1, group2, group3),
    //                       group = factor(rep(1:3, each=5)))
    // R> summary(aov(value ~ group, data))
    //             Df Sum Sq Mean Sq F value   Pr(>F)
    // group        2  118.0  59.000   17.26 0.000368 ***
    // Residuals   12   41.0   3.417

    double[] group1 = [10, 12, 11, 13, 9] as double[]
    double[] group2 = [14, 15, 13, 16, 14] as double[]
    double[] group3 = [8, 9, 7, 10, 8] as double[]

    double fValue = FDistribution.oneWayAnovaFValue([group1, group2, group3])
    assertEquals(17.26, fValue, 0.01, 'F-value from one-way ANOVA')
  }

  @Test
  void testOneWayAnovaPValue() {
    // Using same data as above
    // R p-value: 0.000368
    double[] group1 = [10, 12, 11, 13, 9] as double[]
    double[] group2 = [14, 15, 13, 16, 14] as double[]
    double[] group3 = [8, 9, 7, 10, 8] as double[]

    double pValue = FDistribution.oneWayAnovaPValue([group1, group2, group3])
    assertEquals(0.000368, pValue, 1e-5, 'p-value from one-way ANOVA')
  }

  @Test
  void testOneWayAnovaIdenticalGroups() {
    // Identical groups should give F≈0 and p≈1
    double[] group1 = [10, 12, 11, 13, 9] as double[]
    double[] group2 = [10, 12, 11, 13, 9] as double[]
    double[] group3 = [10, 12, 11, 13, 9] as double[]

    double fValue = FDistribution.oneWayAnovaFValue([group1, group2, group3])
    assertTrue(fValue < 0.0001, 'F-value should be ≈0 for identical groups')

    double pValue = FDistribution.oneWayAnovaPValue([group1, group2, group3])
    assertTrue(pValue > 0.99, 'p-value should be ≈1 for identical groups')
  }

  @Test
  void testOneWayAnovaTwoGroups() {
    // ANOVA with 2 groups should match t-test squared
    // R> group1 <- c(10, 12, 11, 13, 9)
    // R> group2 <- c(14, 15, 13, 16, 14)
    // R> data <- data.frame(value = c(group1, group2),
    //                       group = factor(rep(1:2, each=5)))
    // R> summary(aov(value ~ group, data))
    //             Df Sum Sq Mean Sq F value  Pr(>F)
    // group        1   40.0    40.0      18 0.00289 **
    // Residuals    8   17.8     2.225

    double[] group1 = [10, 12, 11, 13, 9] as double[]
    double[] group2 = [14, 15, 13, 16, 14] as double[]

    double fValue = FDistribution.oneWayAnovaFValue([group1, group2])
    assertEquals(18.0, fValue, 0.1, 'F-value for 2-group ANOVA')

    double pValue = FDistribution.oneWayAnovaPValue([group1, group2])
    assertEquals(0.00289, pValue, 1e-4, 'p-value for 2-group ANOVA')
  }

  @Test
  void testHighDegreesOfFreedom() {
    // R> pf(2.0, 100, 100)
    // [1] 0.9999899
    def dist = new FDistribution(100, 100)
    assertEquals(0.9999899, dist.cdf(2.0), TOLERANCE, 'high degrees of freedom')
  }

  @Test
  void testLowDegreesOfFreedom() {
    // R> pf(2.0, 1, 1)
    // [1] 0.788675
    def dist = new FDistribution(1, 1)
    assertEquals(0.788675, dist.cdf(2.0), TOLERANCE, 'df1=1, df2=1')

    // R> pf(3.0, 2, 3)
    // [1] 0.8401709
    dist = new FDistribution(2, 3)
    assertEquals(0.8401709, dist.cdf(3.0), TOLERANCE, 'df1=2, df2=3')
  }

  @Test
  void testExtremeValues() {
    def dist = new FDistribution(5, 10)

    // Very large F-value should give CDF ≈ 1
    assertTrue(dist.cdf(100.0) > 0.9999, 'very large F should give CDF ≈ 1')

    // Very small F-value should give CDF ≈ 0
    assertTrue(dist.cdf(0.01) < 0.01, 'very small F should give CDF ≈ 0')
  }

  @Test
  void testFractionalDegreesOfFreedom() {
    // ANOVA can sometimes produce fractional df in unbalanced designs
    // R> pf(2.5, 3.5, 10.5)
    // [1] 0.9094773
    def dist = new FDistribution(3.5, 10.5)
    assertEquals(0.9094773, dist.cdf(2.5), TOLERANCE, 'fractional degrees of freedom')
  }

  @Test
  void testOneWayAnovaMultipleGroups() {
    // Test with 4 groups
    // R> g1 <- c(5, 6, 7)
    // R> g2 <- c(8, 9, 10)
    // R> g3 <- c(11, 12, 13)
    // R> g4 <- c(14, 15, 16)
    // R> data <- data.frame(value = c(g1, g2, g3, g4),
    //                       group = factor(rep(1:4, each=3)))
    // R> summary(aov(value ~ group, data))
    //             Df Sum Sq Mean Sq F value Pr(>F)
    // group        3  165.0    55.0      99 1.68e-07 ***
    // Residuals    8    4.4     0.5556

    double[] g1 = [5, 6, 7] as double[]
    double[] g2 = [8, 9, 10] as double[]
    double[] g3 = [11, 12, 13] as double[]
    double[] g4 = [14, 15, 16] as double[]

    double fValue = FDistribution.oneWayAnovaFValue([g1, g2, g3, g4])
    assertEquals(99.0, fValue, 1.0, 'F-value for 4-group ANOVA')

    double pValue = FDistribution.oneWayAnovaPValue([g1, g2, g3, g4])
    assertTrue(pValue < 0.0001, 'p-value should be very small for distinct groups')
  }
}
