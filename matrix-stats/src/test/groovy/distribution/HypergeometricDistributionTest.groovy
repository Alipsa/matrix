package distribution

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

import groovy.transform.CompileStatic

import org.apache.commons.math3.distribution.HypergeometricDistribution as ApacheHypergeometricDistribution
import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.distribution.HypergeometricDistribution

/**
 * Reference tests for the native hypergeometric distribution implementation.
 */
@CompileStatic
@SuppressWarnings('DuplicateNumberLiteral')
class HypergeometricDistributionTest {

  private static final double TOLERANCE = 1e-12

  @Test
  void testConstructorValidation() {
    assertThrows(IllegalArgumentException) {
      new HypergeometricDistribution(-1, 1, 1)
    }
    assertThrows(IllegalArgumentException) {
      new HypergeometricDistribution(10, 11, 3)
    }
    assertThrows(IllegalArgumentException) {
      new HypergeometricDistribution(10, 5, 11)
    }
  }

  @Test
  void testProbabilityAndSupportAgainstApache() {
    HypergeometricDistribution nativeDistribution = new HypergeometricDistribution(37, 21, 17)
    ApacheHypergeometricDistribution apacheDistribution = new ApacheHypergeometricDistribution(37, 21, 17)

    assertEquals(apacheDistribution.supportLowerBound, nativeDistribution.supportLowerBound)
    assertEquals(apacheDistribution.supportUpperBound, nativeDistribution.supportUpperBound)

    (nativeDistribution.supportLowerBound..nativeDistribution.supportUpperBound).each { int x ->
      assertEquals(
          apacheDistribution.probability(x),
          nativeDistribution.probability(x),
          TOLERANCE,
          "PMF mismatch for x=$x"
      )
    }
  }

  @Test
  void testCumulativeProbabilitiesAgainstApache() {
    HypergeometricDistribution nativeDistribution = new HypergeometricDistribution(37, 21, 17)
    ApacheHypergeometricDistribution apacheDistribution = new ApacheHypergeometricDistribution(37, 21, 17)

    [7, 10, 12, 15].each { int x ->
      assertEquals(
          apacheDistribution.cumulativeProbability(x),
          nativeDistribution.cumulativeProbability(x),
          TOLERANCE,
          "Lower-tail mismatch for x=$x"
      )
      assertEquals(
          apacheDistribution.upperCumulativeProbability(x),
          nativeDistribution.upperCumulativeProbability(x),
          TOLERANCE,
          "Upper-tail mismatch for x=$x"
      )
    }
  }

  @Test
  void testIdiomaticNumberOverloadsReturnBigDecimal() {
    HypergeometricDistribution distribution = new HypergeometricDistribution(37, 21, 17)

    BigDecimal probability = distribution.probability(10)
    BigDecimal lowerTail = distribution.cumulativeProbability(10)
    BigDecimal upperTail = distribution.upperCumulativeProbability(10)

    assertEquals(distribution.probability(10), probability as double, TOLERANCE)
    assertEquals(distribution.cumulativeProbability(10), lowerTail as double, TOLERANCE)
    assertEquals(distribution.upperCumulativeProbability(10), upperTail as double, TOLERANCE)
  }

  @Test
  void testIdiomaticNumberOverloadsRejectNonIntegralInput() {
    HypergeometricDistribution distribution = new HypergeometricDistribution(37, 21, 17)

    IllegalArgumentException probabilityException = assertThrows(IllegalArgumentException) {
      distribution.probability(10.5G)
    }
    assertEquals('X must be an integer, got 10.5', probabilityException.message)

    IllegalArgumentException cumulativeException = assertThrows(IllegalArgumentException) {
      distribution.cumulativeProbability(10.5G)
    }
    assertEquals('X must be an integer, got 10.5', cumulativeException.message)
  }
}
