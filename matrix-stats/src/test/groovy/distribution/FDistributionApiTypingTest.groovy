package distribution

import static org.junit.jupiter.api.Assertions.assertEquals

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.distribution.FDistribution

@CompileStatic
class FDistributionApiTypingTest {

  @Test
  void testPrimitiveGroupTypedOverloads() {
    double[] group1 = [10, 12, 11, 13, 9] as double[]
    double[] group2 = [14, 15, 13, 16, 14] as double[]
    double[] group3 = [8, 9, 7, 10, 8] as double[]
    List<double[]> groups = [group1, group2, group3]

    BigDecimal fValue = FDistribution.oneWayAnovaFValueArrays(groups)
    BigDecimal pValue = FDistribution.oneWayAnovaPValueArrays(groups)

    assertEquals(26.63d, fValue as double, 0.01d)
    assertEquals(3.867e-5d, pValue as double, 1e-6d)
  }

  @Test
  void testNumericListTypedOverloads() {
    Collection<List<BigDecimal>> groups = [
      [10.0, 12.0, 11.0, 13.0, 9.0],
      [14.0, 15.0, 13.0, 16.0, 14.0],
      [8.0, 9.0, 7.0, 10.0, 8.0]
    ]

    BigDecimal fValue = FDistribution.oneWayAnovaFValueFromLists(groups)
    BigDecimal pValue = FDistribution.oneWayAnovaPValueFromLists(groups)

    assertEquals(26.63d, fValue as double, 0.01d)
    assertEquals(3.867e-5d, pValue as double, 1e-6d)
  }
}
