package distribution

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import groovy.transform.CompileStatic

import org.apache.commons.math3.distribution.ChiSquaredDistribution as ApacheChiSquaredDistribution
import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.distribution.ChiSquaredDistribution

/**
 * Reference tests for the native chi-squared distribution implementation.
 */
@CompileStatic
@SuppressWarnings('DuplicateNumberLiteral')
class ChiSquaredDistributionTest {

  private static final double TOLERANCE = 1e-10

  @Test
  void testConstructorValidation() {
    assertThrows(IllegalArgumentException) {
      new ChiSquaredDistribution(0.0d)
    }
    assertThrows(IllegalArgumentException) {
      new ChiSquaredDistribution(-2.0d)
    }
  }

  @Test
  void testCumulativeProbabilityAgainstApache() {
    List<List<Double>> cases = [
        [1.0d, 0.25d],
        [1.0d, 3.84d],
        [2.0d, 5.99d],
        [5.0d, 9.24d],
        [12.0d, 18.55d]
    ]

    cases.each { List<Double> input ->
      double degreesOfFreedom = input[0]
      double x = input[1]
      ChiSquaredDistribution nativeDistribution = new ChiSquaredDistribution(degreesOfFreedom)
      ApacheChiSquaredDistribution apacheDistribution = new ApacheChiSquaredDistribution(degreesOfFreedom)

      assertEquals(
          apacheDistribution.cumulativeProbability(x),
          nativeDistribution.cumulativeProbability(x),
          TOLERANCE,
          "CDF mismatch for df=$degreesOfFreedom, x=$x"
      )
    }
  }

  @Test
  void testInverseCumulativeProbabilityAgainstApache() {
    List<List<Double>> cases = [
        [1.0d, 0.95d],
        [2.0d, 0.95d],
        [5.0d, 0.5d],
        [10.0d, 0.99d],
        [20.0d, 0.25d]
    ]

    cases.each { List<Double> input ->
      double degreesOfFreedom = input[0]
      double p = input[1]
      ChiSquaredDistribution nativeDistribution = new ChiSquaredDistribution(degreesOfFreedom)
      ApacheChiSquaredDistribution apacheDistribution = new ApacheChiSquaredDistribution(degreesOfFreedom)

      assertEquals(
          apacheDistribution.inverseCumulativeProbability(p),
          nativeDistribution.inverseCumulativeProbability(p),
          1e-9,
          "Inverse CDF mismatch for df=$degreesOfFreedom, p=$p"
      )
    }
  }

  @Test
  void testBoundaryProbabilities() {
    ChiSquaredDistribution distribution = new ChiSquaredDistribution(2.0d)
    assertEquals(0.0d, distribution.inverseCumulativeProbability(0.0d), TOLERANCE)
    assertTrue(Double.isInfinite(distribution.inverseCumulativeProbability(1.0d)))
  }
}
