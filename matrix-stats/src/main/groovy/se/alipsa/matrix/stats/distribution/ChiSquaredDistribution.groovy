package se.alipsa.matrix.stats.distribution

/**
 * Chi-squared distribution implementation backed by the regularized incomplete gamma function.
 */
@SuppressWarnings('DuplicateNumberLiteral')
class ChiSquaredDistribution implements ContinuousDistribution {

  private static final int MAX_INVERSE_ITERATIONS = 80

  private final double degreesOfFreedom

  ChiSquaredDistribution(double degreesOfFreedom) {
    if (degreesOfFreedom <= 0.0d) {
      throw new IllegalArgumentException("Degrees of freedom must be positive, got: $degreesOfFreedom")
    }
    this.degreesOfFreedom = degreesOfFreedom
  }

  double getDegreesOfFreedom() {
    degreesOfFreedom
  }

  @Override
  double cumulativeProbability(double x) {
    if (x <= 0.0d) {
      return 0.0d
    }
    return SpecialFunctions.regularizedIncompleteGammaP(degreesOfFreedom / 2.0d, x / 2.0d)
  }

  double inverseCumulativeProbability(double p) {
    if (p < 0.0d || p > 1.0d) {
      throw new IllegalArgumentException("p must be between 0 and 1, got: $p")
    }
    if (p == 0.0d) {
      return 0.0d
    }
    if (p == 1.0d) {
      return Double.POSITIVE_INFINITY
    }

    double guess = initialQuantileGuess(p)
    double low = 0.0d
    double high = Math.max(guess, degreesOfFreedom)
    if (high <= 0.0d || !Double.isFinite(high)) {
      high = degreesOfFreedom
    }

    while (cumulativeProbability(high) < p) {
      low = high
      high *= 2.0d
      if (high > Double.MAX_VALUE / 4.0d) {
        break
      }
    }

    for (int i = 0; i < MAX_INVERSE_ITERATIONS; i++) {
      double mid = 0.5d * (low + high)
      double cdf = cumulativeProbability(mid)
      if (Math.abs(cdf - p) < 1e-12d || high - low < 1e-12d * Math.max(1.0d, mid)) {
        return mid
      }
      if (cdf < p) {
        low = mid
      } else {
        high = mid
      }
    }

    return 0.5d * (low + high)
  }

  private double initialQuantileGuess(double p) {
    NormalDistribution standardNormal = new NormalDistribution()
    double z = standardNormal.inverseCumulativeProbability(p)
    double a = 2.0d / (9.0d * degreesOfFreedom)
    double term = 1.0d - a + z * Math.sqrt(a)
    if (term <= 0.0d) {
      return degreesOfFreedom * 0.25d
    }
    return degreesOfFreedom * term * term * term
  }
}
