package se.alipsa.matrix.stats.distribution

import groovy.transform.CompileStatic

/**
 * Special mathematical functions used in statistical distributions.
 * Implementation based on algorithms from Numerical Recipes and NIST.
 */
@CompileStatic
class SpecialFunctions {

  private static final double EPSILON = 1e-14
  private static final int MAX_ITERATIONS = 200

  /**
   * Computes the regularized incomplete beta function I_x(a, b).
   * This is the CDF of the beta distribution.
   *
   * @param x the integration limit (0 <= x <= 1)
   * @param a first shape parameter (a > 0)
   * @param b second shape parameter (b > 0)
   * @return the regularized incomplete beta function value
   */
  static double regularizedIncompleteBeta(double x, double a, double b) {
    if (x < 0.0 || x > 1.0) {
      throw new IllegalArgumentException("x must be between 0 and 1, got: $x")
    }
    if (a <= 0.0 || b <= 0.0) {
      throw new IllegalArgumentException("a and b must be positive, got a=$a, b=$b")
    }

    if (x == 0.0) return 0.0
    if (x == 1.0) return 1.0

    // Use symmetry relation for faster convergence when x > (a+1)/(a+b+2)
    if (x > (a + 1.0) / (a + b + 2.0)) {
      return 1.0 - regularizedIncompleteBeta(1.0 - x, b, a)
    }

    // Compute using continued fraction
    double bt = Math.exp(
        logGamma(a + b) - logGamma(a) - logGamma(b) +
        a * Math.log(x) + b * Math.log(1.0 - x)
    )

    return bt * betaContinuedFraction(x, a, b) / a
  }

  /**
   * Continued fraction representation of the incomplete beta function.
   * Uses the modified Lentz's algorithm.
   */
  private static double betaContinuedFraction(double x, double a, double b) {
    double qab = a + b
    double qap = a + 1.0
    double qam = a - 1.0
    double c = 1.0
    double d = 1.0 - qab * x / qap
    if (Math.abs(d) < 1e-30) d = 1e-30
    d = 1.0 / d
    double h = d

    for (int m = 1; m <= MAX_ITERATIONS; m++) {
      int m2 = 2 * m
      double aa = m * (b - m) * x / ((qam + m2) * (a + m2))
      d = 1.0 + aa * d
      if (Math.abs(d) < 1e-30) d = 1e-30
      c = 1.0 + aa / c
      if (Math.abs(c) < 1e-30) c = 1e-30
      d = 1.0 / d
      h *= d * c

      aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
      d = 1.0 + aa * d
      if (Math.abs(d) < 1e-30) d = 1e-30
      c = 1.0 + aa / c
      if (Math.abs(c) < 1e-30) c = 1e-30
      d = 1.0 / d
      double delta = d * c
      h *= delta

      if (Math.abs(delta - 1.0) < EPSILON) {
        return h
      }
    }
    throw new RuntimeException("Beta continued fraction failed to converge after $MAX_ITERATIONS iterations")
  }

  /**
   * Computes the log of the gamma function using Lanczos approximation.
   * More accurate than direct computation for large values.
   *
   * @param x input value (x > 0)
   * @return log(Gamma(x))
   */
  static double logGamma(double x) {
    if (x <= 0.0) {
      throw new IllegalArgumentException("x must be positive, got: $x")
    }

    // Lanczos coefficients for g=7
    double[] coef = [
        0.99999999999980993,
        676.5203681218851,
        -1259.1392167224028,
        771.32342877765313,
        -176.61502916214059,
        12.507343278686905,
        -0.13857109526572012,
        9.9843695780195716e-6,
        1.5056327351493116e-7
    ] as double[]

    if (x < 0.5) {
      // Use reflection formula
      return Math.log(Math.PI / Math.sin(Math.PI * x)) - logGamma(1.0 - x)
    }

    x -= 1.0
    double a = coef[0]
    double t = x + 7.5
    for (int i = 1; i < coef.length; i++) {
      a += coef[i] / (x + i)
    }

    return 0.5 * Math.log(2.0 * Math.PI) + (x + 0.5) * Math.log(t) - t + Math.log(a)
  }

  /**
   * Computes the gamma function.
   *
   * @param x input value
   * @return Gamma(x)
   */
  static double gamma(double x) {
    return Math.exp(logGamma(x))
  }
}
