package se.alipsa.matrix.stats.distribution

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Chi-squared distribution implementation backed by the regularized incomplete gamma function.
 */
@SuppressWarnings('DuplicateNumberLiteral')
class ChiSquaredDistribution implements ContinuousDistribution {

  private static final int MAX_INVERSE_ITERATIONS = 80

  private final BigDecimal degreesOfFreedom

  ChiSquaredDistribution(Number degreesOfFreedom) {
    BigDecimal normalizedDegreesOfFreedom = NumericConversion.toBigDecimal(degreesOfFreedom, 'degreesOfFreedom')
    if (normalizedDegreesOfFreedom <= 0.0d) {
      throw new IllegalArgumentException("Degrees of freedom must be positive, got: $degreesOfFreedom")
    }
    this.degreesOfFreedom = normalizedDegreesOfFreedom
  }

  BigDecimal getDegreesOfFreedom() {
    degreesOfFreedom
  }

  BigDecimal cumulativeProbability(Number x) {
    BigDecimal.valueOf(cumulativeProbabilityValue(NumericConversion.toFiniteDouble(x, 'x')))
  }

  @Override
  @Deprecated
  double cumulativeProbability(double x) {
    cumulativeProbabilityValue(x)
  }

  BigDecimal inverseCumulativeProbability(Number p) {
    BigDecimal.valueOf(inverseCumulativeProbabilityValue(NumericConversion.toFiniteDouble(p, 'p')))
  }

  @Deprecated
  double inverseCumulativeProbability(double p) {
    inverseCumulativeProbabilityValue(p)
  }

  private double cumulativeProbabilityValue(double x) {
    if (x <= 0.0d) {
      return 0.0d
    }
    return SpecialFunctions.regularizedIncompleteGammaP((degreesOfFreedom as double) / 2.0d, x / 2.0d)
  }

  private double inverseCumulativeProbabilityValue(double p) {
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
    double degrees = degreesOfFreedom as double
    double high = Math.max(guess, degrees)
    if (high <= 0.0d || !Double.isFinite(high)) {
      high = degrees
    }

    while (cumulativeProbabilityValue(high) < p) {
      low = high
      high *= 2.0d
      if (high > Double.MAX_VALUE / 4.0d) {
        break
      }
    }

    for (int i = 0; i < MAX_INVERSE_ITERATIONS; i++) {
      double mid = 0.5d * (low + high)
      double cdf = cumulativeProbabilityValue(mid)
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
    double degrees = degreesOfFreedom as double
    double a = 2.0d / (9.0d * degrees)
    double term = 1.0d - a + z * Math.sqrt(a)
    if (term <= 0.0d) {
      return degrees * 0.25d
    }
    return degrees * term * term * term
  }
}
