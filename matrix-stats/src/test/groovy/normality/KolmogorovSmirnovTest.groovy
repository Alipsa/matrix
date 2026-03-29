package normality

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import groovy.transform.CompileStatic

import org.apache.commons.math3.distribution.NormalDistribution as ApacheNormalDistribution
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest as ApacheKolmogorovSmirnovTest
import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.normality.KolmogorovSmirnov

/**
 * Tests for the native Kolmogorov-Smirnov implementation.
 */
@CompileStatic
class KolmogorovSmirnovTest {

  private static final double D_TOLERANCE = 1e-12d
  // The one-sample implementation intentionally uses the asymptotic Kolmogorov distribution,
  // so it does not match Apache's exact/reference p-value as tightly as the statistic itself.
  private static final double ONE_SAMPLE_P_TOLERANCE = 0.025d
  private static final double TWO_SAMPLE_P_TOLERANCE = 0.01d

  @Test
  void testAgainstDistributionMatchesApacheReference() {
    List<Double> data = [-1.2d, -0.4d, -0.1d, 0.0d, 0.4d, 0.7d, 1.1d, 1.4d]
    def distribution = new se.alipsa.matrix.stats.distribution.NormalDistribution(0.0d, 1.0d)
    def result = KolmogorovSmirnov.testAgainstDistribution(data, distribution, 'Standard Normality')

    ApacheKolmogorovSmirnovTest apacheTest = new ApacheKolmogorovSmirnovTest()
    ApacheNormalDistribution apacheDistribution = new ApacheNormalDistribution(0.0d, 1.0d)
    double[] sample = data as double[]

    assertEquals(apacheTest.kolmogorovSmirnovStatistic(apacheDistribution, sample), result.dStatistic, D_TOLERANCE)
    assertEquals(apacheTest.kolmogorovSmirnovTest(apacheDistribution, sample), result.pValue, ONE_SAMPLE_P_TOLERANCE)
  }

  @Test
  void testTwoSampleMatchesApacheReference() {
    List<Double> sample1 = [0.12d, 0.18d, 0.21d, 0.25d, 0.33d, 0.41d, 0.48d]
    List<Double> sample2 = [0.08d, 0.15d, 0.19d, 0.27d, 0.31d, 0.36d, 0.52d]
    def result = KolmogorovSmirnov.twoSampleTest(sample1, sample2)

    ApacheKolmogorovSmirnovTest apacheTest = new ApacheKolmogorovSmirnovTest()
    double[] first = sample1 as double[]
    double[] second = sample2 as double[]

    assertEquals(apacheTest.kolmogorovSmirnovStatistic(first, second), result.dStatistic, D_TOLERANCE)
    assertEquals(apacheTest.kolmogorovSmirnovTest(first, second), result.pValue, TWO_SAMPLE_P_TOLERANCE)
  }

  @Test
  void testTwoSampleExactBranchHandlesBoundaryStatistic() {
    List<Double> sample1 = [1.0d, 3.0d, 4.0d, 7.0d, 9.0d]
    List<Double> sample2 = [2.0d, 5.0d, 6.0d, 8.0d, 10.0d]
    def result = KolmogorovSmirnov.twoSampleTest(sample1, sample2)

    ApacheKolmogorovSmirnovTest apacheTest = new ApacheKolmogorovSmirnovTest()
    double[] first = sample1 as double[]
    double[] second = sample2 as double[]

    assertEquals(apacheTest.kolmogorovSmirnovStatistic(first, second), result.dStatistic, D_TOLERANCE)
    assertEquals(apacheTest.kolmogorovSmirnovTest(first, second), result.pValue, TWO_SAMPLE_P_TOLERANCE)
  }

  @Test
  void testRejectsNullValues() {
    assertThrows(IllegalArgumentException) {
      KolmogorovSmirnov.twoSampleTest([1.0d, null, 3.0d], [1.0d, 2.0d, 3.0d])
    }
  }

  @Test
  void testPValueRangeForTwoSample() {
    def result = KolmogorovSmirnov.twoSampleTest([1.0d, 2.0d, 3.0d], [3.0d, 4.0d, 5.0d])

    assertTrue(result.pValue >= 0.0d)
    assertTrue(result.pValue <= 1.0d)
  }
}
