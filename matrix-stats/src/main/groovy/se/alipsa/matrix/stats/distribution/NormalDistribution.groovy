package se.alipsa.matrix.stats.distribution

import groovy.transform.CompileStatic

/**
 * Normal (Gaussian) distribution implementation with cumulative and inverse cumulative probability.
 */
@CompileStatic
@SuppressWarnings('DuplicateNumberLiteral')
class NormalDistribution implements ContinuousDistribution {

  private static final double SQRT_TWO = Math.sqrt(2.0d)

  private static final double[] A = [
      -3.969683028665376e+01,
      2.209460984245205e+02,
      -2.759285104469687e+02,
      1.383577518672690e+02,
      -3.066479806614716e+01,
      2.506628277459239e+00
  ] as double[]

  private static final double[] B = [
      -5.447609879822406e+01,
      1.615858368580409e+02,
      -1.556989798598866e+02,
      6.680131188771972e+01,
      -1.328068155288572e+01
  ] as double[]

  private static final double[] C = [
      -7.784894002430293e-03,
      -3.223964580411365e-01,
      -2.400758277161838e+00,
      -2.549732539343734e+00,
      4.374664141464968e+00,
      2.938163982698783e+00
  ] as double[]

  private static final double[] D = [
      7.784695709041462e-03,
      3.224671290700398e-01,
      2.445134137142996e+00,
      3.754408661907416e+00
  ] as double[]

  private static final double P_LOW = 0.02425d
  private static final double P_HIGH = 1.0d - P_LOW

  private final double mean
  private final double standardDeviation

  NormalDistribution(double mean = 0.0d, double standardDeviation = 1.0d) {
    if (standardDeviation <= 0.0d) {
      throw new IllegalArgumentException("Standard deviation must be positive, got: $standardDeviation")
    }
    this.mean = mean
    this.standardDeviation = standardDeviation
  }

  double getMean() {
    mean
  }

  double getStandardDeviation() {
    standardDeviation
  }

  @Override
  double cumulativeProbability(double x) {
    double standardized = (x - mean) / standardDeviation
    if (standardized == Double.NEGATIVE_INFINITY) {
      return 0.0d
    }
    if (standardized == Double.POSITIVE_INFINITY) {
      return 1.0d
    }

    double z = standardized / SQRT_TWO
    double gamma = SpecialFunctions.regularizedIncompleteGammaP(0.5d, z * z)
    if (z >= 0.0d) {
      return 0.5d * (1.0d + gamma)
    }
    return 0.5d * (1.0d - gamma)
  }

  double inverseCumulativeProbability(double p) {
    if (p < 0.0d || p > 1.0d) {
      throw new IllegalArgumentException("p must be between 0 and 1, got: $p")
    }
    if (p == 0.0d) {
      return Double.NEGATIVE_INFINITY
    }
    if (p == 1.0d) {
      return Double.POSITIVE_INFINITY
    }

    double x
    if (p < P_LOW) {
      double q = Math.sqrt(-2.0d * Math.log(p))
      x = polynomial(C, q) / polynomial([D[0], D[1], D[2], D[3], 1.0d] as double[], q)
    } else if (p <= P_HIGH) {
      double q = p - 0.5d
      double r = q * q
      x = q * polynomial(A, r) / polynomial([B[0], B[1], B[2], B[3], B[4], 1.0d] as double[], r)
    } else {
      double q = Math.sqrt(-2.0d * Math.log(1.0d - p))
      x = -polynomial(C, q) / polynomial([D[0], D[1], D[2], D[3], 1.0d] as double[], q)
    }

    // One Halley refinement step brings the approximation close to machine precision.
    double error = cumulativeProbability(mean + standardDeviation * x) - p
    double density = Math.exp(-0.5d * x * x) / Math.sqrt(2.0d * Math.PI)
    x -= error / density

    return mean + standardDeviation * x
  }

  private static double polynomial(double[] coefficients, double x) {
    double result = coefficients[0]
    for (int i = 1; i < coefficients.length; i++) {
      result = result * x + coefficients[i]
    }
    result
  }
}
