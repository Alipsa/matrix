package distribution

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

import groovy.transform.CompileStatic

import org.apache.commons.math3.distribution.NormalDistribution as ApacheNormalDistribution
import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.distribution.NormalDistribution

/**
 * Reference tests for the native normal distribution implementation.
 */
@CompileStatic
@SuppressWarnings('DuplicateNumberLiteral')
class NormalDistributionTest {

  private static final double TOLERANCE = 1e-10

  @Test
  void testConstructorValidation() {
    assertThrows(IllegalArgumentException) {
      new NormalDistribution(0.0d, 0.0d)
    }
    assertThrows(IllegalArgumentException) {
      new NormalDistribution(0.0d, -1.0d)
    }
  }

  @Test
  void testCumulativeProbabilityAgainstApache() {
    List<List<Double>> cases = [
        [0.0d, 1.0d, -2.5d],
        [0.0d, 1.0d, 0.0d],
        [0.0d, 1.0d, 1.96d],
        [10.0d, 2.5d, 9.3d],
        [-3.0d, 0.75d, -1.2d]
    ]

    cases.each { List<Double> input ->
      double mean = input[0]
      double standardDeviation = input[1]
      double x = input[2]
      NormalDistribution nativeDistribution = new NormalDistribution(mean, standardDeviation)
      ApacheNormalDistribution apacheDistribution = new ApacheNormalDistribution(mean, standardDeviation)

      assertEquals(
          apacheDistribution.cumulativeProbability(x),
          nativeDistribution.cumulativeProbability(x),
          TOLERANCE,
          "CDF mismatch for mean=$mean, sd=$standardDeviation, x=$x"
      )
    }
  }

  @Test
  void testInverseCumulativeProbabilityAgainstApache() {
    List<List<Double>> cases = [
        [0.0d, 1.0d, 0.001d],
        [0.0d, 1.0d, 0.025d],
        [0.0d, 1.0d, 0.5d],
        [0.0d, 1.0d, 0.975d],
        [12.0d, 3.0d, 0.9d],
        [-5.0d, 2.0d, 0.2d]
    ]

    cases.each { List<Double> input ->
      double mean = input[0]
      double standardDeviation = input[1]
      double p = input[2]
      NormalDistribution nativeDistribution = new NormalDistribution(mean, standardDeviation)
      ApacheNormalDistribution apacheDistribution = new ApacheNormalDistribution(mean, standardDeviation)

      assertEquals(
          apacheDistribution.inverseCumulativeProbability(p),
          nativeDistribution.inverseCumulativeProbability(p),
          1e-9,
          "Inverse CDF mismatch for mean=$mean, sd=$standardDeviation, p=$p"
      )
    }
  }

  @Test
  void testRoundTripProbability() {
    NormalDistribution distribution = new NormalDistribution(2.0d, 1.5d)

    [0.01d, 0.1d, 0.5d, 0.9d, 0.99d].each { double p ->
      double x = distribution.inverseCumulativeProbability(p)
      assertEquals(p, distribution.cumulativeProbability(x), 1e-10, "Round-trip mismatch for p=$p")
    }
  }
}
