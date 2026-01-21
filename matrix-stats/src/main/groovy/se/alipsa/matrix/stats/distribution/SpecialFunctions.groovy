package se.alipsa.matrix.stats.distribution

import groovy.transform.CompileStatic
import org.apache.commons.math3.special.Beta
import org.apache.commons.math3.special.Gamma

/**
 * Special mathematical functions used in statistical distributions.
 * This class provides wrappers around Apache Commons Math implementations
 * for improved numerical accuracy and reliability.
 */
@CompileStatic
class SpecialFunctions {

  /**
   * Computes the regularized incomplete beta function I_x(a, b).
   * This is the CDF of the beta distribution.
   *
   * <p>Uses Apache Commons Math's implementation which provides excellent
   * numerical accuracy across all parameter ranges.</p>
   *
   * @param x the integration limit (0 <= x <= 1)
   * @param a first shape parameter (a > 0)
   * @param b second shape parameter (b > 0)
   * @return the regularized incomplete beta function value
   * @throws IllegalArgumentException if x is not in [0,1] or a,b are not positive
   */
  static double regularizedIncompleteBeta(double x, double a, double b) {
    if (x < 0.0 || x > 1.0) {
      throw new IllegalArgumentException("x must be between 0 and 1, got: $x")
    }
    if (a <= 0.0 || b <= 0.0) {
      throw new IllegalArgumentException("a and b must be positive, got a=$a, b=$b")
    }

    // Use Apache Commons Math's implementation for accuracy
    return Beta.regularizedBeta(x, a, b)
  }

  /**
   * Computes the log of the gamma function using Lanczos approximation.
   * More accurate than direct computation for large values.
   *
   * <p>Uses Apache Commons Math's implementation which provides excellent
   * numerical accuracy.</p>
   *
   * @param x input value (x > 0)
   * @return log(Gamma(x))
   * @throws IllegalArgumentException if x <= 0
   */
  static double logGamma(double x) {
    if (x <= 0.0) {
      throw new IllegalArgumentException("x must be positive, got: $x")
    }

    // Use Apache Commons Math's implementation for accuracy
    return Gamma.logGamma(x)
  }

  /**
   * Computes the gamma function.
   *
   * <p>Uses Apache Commons Math's implementation which provides excellent
   * numerical accuracy.</p>
   *
   * @param x input value (x > 0)
   * @return Gamma(x)
   * @throws IllegalArgumentException if x <= 0
   */
  static double gamma(double x) {
    if (x <= 0.0) {
      throw new IllegalArgumentException("x must be positive, got: $x")
    }

    // Use Apache Commons Math's implementation for accuracy
    return Gamma.gamma(x)
  }
}
