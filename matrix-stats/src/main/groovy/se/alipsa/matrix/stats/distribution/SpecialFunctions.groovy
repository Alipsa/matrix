package se.alipsa.matrix.stats.distribution

import static java.math.BigDecimal.*
import static se.alipsa.matrix.ext.NumberExtension.*

import se.alipsa.matrix.stats.util.NumericConversion

import java.math.MathContext
import java.math.RoundingMode

/**
 * Special mathematical functions used in statistical distributions.
 * Implementation based on algorithms from Numerical Recipes and NIST.
 *
 * <p>This implementation provides excellent numerical accuracy (1e-10 or better)
 * and is self-contained with no external dependencies.</p>
 */
@SuppressWarnings('DuplicateNumberLiteral')
class SpecialFunctions {

  private static final double EPSILON = 1e-14
  private static final double GAMMA_TINY = 1e-300
  private static final int MAX_ITERATIONS = 200
  private static final MathContext BETA_MC = new MathContext(50, RoundingMode.HALF_EVEN)
  private static final BigDecimal BETA_EPSILON = new BigDecimal('1e-30')
  private static final BigDecimal BETA_TINY = new BigDecimal('1e-40')

  /**
   * Computes the regularized incomplete beta function I_x(a, b).
   * This is the CDF of the beta distribution.
   *
   * <p>Uses continued fraction representation with Lentz's algorithm
   * for stable and accurate computation.</p>
   *
   * @param x the integration limit (0 <= x <= 1)
   * @param a first shape parameter (a > 0)
   * @param b second shape parameter (b > 0)
   * @return the regularized incomplete beta function value
   * @throws IllegalArgumentException if x is not in [0,1] or a,b are not positive
   * @deprecated Use {@link #regularizedIncompleteBeta(BigDecimal, BigDecimal, BigDecimal)} for idiomatic groovy
   */
  @Deprecated
  static double regularizedIncompleteBeta(double x, double a, double b) {
    if (x < 0.0d || x > 1.0d) {
      throw new IllegalArgumentException("x must be between 0 and 1, got: $x")
    }
    if (a <= 0.0d || b <= 0.0d) {
      throw new IllegalArgumentException("a and b must be positive, got a=$a, b=$b")
    }

    if (x == 0.0d) {
      return 0.0d
    }
    if (x == 1.0d) {
      return 1.0d
    }

    // Use symmetry relation for faster convergence when x > (a+1)/(a+b+2)
    if (x > (a + 1.0) / (a + b + 2.0)) {
      return 1.0d - regularizedIncompleteBeta(1.0d - x, b, a)
    }

    // Compute using continued fraction
    double bt = Math.exp(
        logGamma(a + b) - logGamma(a) - logGamma(b) +
        a * Math.log(x) + b * Math.log(1.0d - x)
    )

    return bt * betaContinuedFraction(x, a, b) / a
  }

  /**
   * BigDecimal-friendly incomplete beta function.
   *
   * @param x the evaluation point
   * @param a the first shape parameter
   * @param b the second shape parameter
   * @return the regularized incomplete beta value
   */
  static BigDecimal regularizedIncompleteBeta(Number x, Number a, Number b) {
    BigDecimal.valueOf(regularizedIncompleteBeta(
      NumericConversion.toFiniteDouble(x, 'x'),
      NumericConversion.toFiniteDouble(a, 'a'),
      NumericConversion.toFiniteDouble(b, 'b')
    ))
  }

  static BigDecimal regularizedIncompleteBeta(BigDecimal x, BigDecimal a, BigDecimal b) {
    if (x < ZERO || x > ONE) {
      throw new IllegalArgumentException("x must be between 0 and 1, got: $x")
    }
    if (a <= ZERO || b <= ZERO) {
      throw new IllegalArgumentException("a and b must be positive, got a=$a, b=$b")
    }

    if (x == ZERO) {
      return ZERO
    }
    if (x == ONE) {
      return ONE
    }

    // Use symmetry relation for faster convergence when x > (a+1)/(a+b+2)
    BigDecimal threshold = div(a + 1.0, a + b + 2.0)
    if (x > threshold) {
      return 1.0 - regularizedIncompleteBeta(1.0 - x, b, a)
    }

    // Compute using continued fraction
    BigDecimal bt = (
        logGamma(a + b) - logGamma(a) - logGamma(b) +
            a * x.log() + b * (1.0 - x).log()
    ).exp()

    div(bt * betaContinuedFraction(x, a, b), a)
  }

  /**
   * Continued fraction representation of the incomplete beta function.
   * Uses the modified Lentz's algorithm for numerical stability.
   * @deprecated Use {@link #betaContinuedFraction(BigDecimal, BigDecimal, BigDecimal)} for idiomatic groovy
   */
  @Deprecated
  private static double betaContinuedFraction(double x, double a, double b) {
    double qab = a + b
    double qap = a + 1.0d
    double qam = a - 1.0d
    double c = 1.0d
    double d = 1.0d - qab * x / qap
    if (Math.abs(d) < 1e-30) {
      d = 1e-30
    }
    d = 1.0d / d
    double h = d

    for (int m = 1; m <= MAX_ITERATIONS; m++) {
      int m2 = 2 * m
      double aa = m * (b - m) * x / ((qam + m2) * (a + m2))
      d = 1.0d + aa * d
      if (Math.abs(d) < 1e-30) {
      d = 1e-30
    }
      c = 1.0d + aa / c
      if (Math.abs(c) < 1e-30) {
        c = 1e-30
      }
      d = 1.0d / d
      h *= d * c

      aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2))
      d = 1.0 + aa * d
      if (Math.abs(d) < 1e-30) {
      d = 1e-30
    }
      c = 1.0 + aa / c
      if (Math.abs(c) < 1e-30) {
        c = 1e-30
      }
      d = 1.0 / d
      double delta = d * c
      h *= delta

      if (Math.abs(delta - 1.0d) < EPSILON) {
        return h
      }
    }
    throw new IllegalStateException("Beta continued fraction failed to converge after $MAX_ITERATIONS iterations")
  }

  private static BigDecimal betaContinuedFraction(BigDecimal x, BigDecimal a, BigDecimal b) {
    BigDecimal qab = a + b
    BigDecimal qap = a + ONE
    BigDecimal qam = a - ONE
    BigDecimal c = ONE
    BigDecimal d = ONE - div(qab * x, qap)
    if (d.abs() < BETA_TINY) {
      d = BETA_TINY
    }
    d = div(ONE, d)
    BigDecimal h = d

    for (int m = 1; m <= MAX_ITERATIONS; m++) {
      int m2 = 2 * m
      BigDecimal aa = div(m * (b - m) * x, (qam + m2) * (a + m2))
      d = ONE + aa * d
      if (d.abs() < BETA_TINY) {
      d = BETA_TINY
    }
      c = ONE + div(aa, c)
      if (c.abs() < BETA_TINY) {
        c = BETA_TINY
      }
      d = div(ONE, d)
      h *= d * c

      aa = div(-(a + m) * (qab + m) * x, (a + m2) * (qap + m2))
      d = ONE + aa * d
      if (d.abs() < BETA_TINY) {
      d = BETA_TINY
    }
      c = ONE + div(aa, c)
      if (c.abs() < BETA_TINY) {
        c = BETA_TINY
      }
      d = div(ONE, d)
      BigDecimal delta = d * c
      h *= delta

      if ((delta - ONE).abs() < BETA_EPSILON) {
        return h
      }
    }
    throw new IllegalStateException("Beta continued fraction failed to converge after $MAX_ITERATIONS iterations")
  }

  private static BigDecimal div(BigDecimal numerator, BigDecimal denominator) {
    numerator.divide(denominator, BETA_MC)
  }

  /**
   * Computes the log of the gamma function using Lanczos approximation.
   * More accurate than direct computation for large values.
   *
   * <p>Uses Lanczos approximation with g=7 and n=9 terms for excellent
   * numerical accuracy across all positive inputs.</p>
   *
   * @param x input value (x > 0)
   * @return log(Gamma(x))
   * @throws IllegalArgumentException if x <= 0
   * @deprecated Use {@link #logGamma(BigDecimal)} for idiomatic groovy
   */
  @Deprecated
  static double logGamma(double x) {
    if (x <= 0.0) {
      throw new IllegalArgumentException("x must be positive, got: $x")
    }

    // Lanczos coefficients for g=7, n=9
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

    if (x < 0.5d) {
      // Use reflection formula
      return Math.log(Math.PI / Math.sin(Math.PI * x) as double) - logGamma(1.0d - x)
    }

    x -= 1.0
    double a = coef[0]
    double t = x + 7.5
    for (int i = 1; i < coef.length; i++) {
      a += coef[i] / (x + i)
    }

    return 0.5 * Math.log(2.0 * Math.PI) + (x + 0.5) * Math.log(t) - t + Math.log(a)
  }

  static BigDecimal logGamma(BigDecimal x) {
    if (x <= 0.0) {
      throw new IllegalArgumentException("x must be positive, got: $x")
    }

    // Lanczos coefficients for g=7, n=9
    BigDecimal[] coef = [
        0.99999999999980993,
        676.5203681218851,
        -1259.1392167224028,
        771.32342877765313,
        -176.61502916214059,
        12.507343278686905,
        -0.13857109526572012,
        9.9843695780195716e-6,
        1.5056327351493116e-7
    ] as BigDecimal[]

    if (x < 0.5) {
      // Use reflection formula
      return (PI / (PI * x).sin()).log() - logGamma(1.0 - x)
    }

    x -= 1.0
    BigDecimal a = coef[0]
    BigDecimal t = x + 7.5
    for (int i = 1; i < coef.length; i++) {
      a += coef[i] / (x + i)
    }

    return 0.5 * (2.0 * PI).log() + (x + 0.5) * t.log() - t + a.log()
  }

  /**
   * Computes the gamma function.
   *
   * @param x input value (x > 0)
   * @return Gamma(x)
   * @throws IllegalArgumentException if x <= 0
   * @deprecated Use {@link #gamma(BigDecimal)} for idiomatic groovy
   */
  @Deprecated
  static double gamma(double x) {
    if (x <= 0.0d) {
      throw new IllegalArgumentException("x must be positive, got: $x")
    }

    return Math.exp(logGamma(x))
  }

  static BigDecimal gamma(BigDecimal x) {
    if (x <= 0.0) {
      throw new IllegalArgumentException("x must be positive, got: $x")
    }
    return logGamma(x).exp()
  }

  /**
   * Computes the regularized lower incomplete gamma function P(a, x).
   *
   * @param a shape parameter (a > 0)
   * @param x integration limit (x >= 0)
   * @return the regularized lower incomplete gamma function
   * @throws IllegalArgumentException if a <= 0 or x < 0
   */
  static double regularizedIncompleteGammaP(double a, double x) {
    if (a <= 0.0d) {
      throw new IllegalArgumentException("a must be positive, got: $a")
    }
    if (x < 0.0d) {
      throw new IllegalArgumentException("x must be non-negative, got: $x")
    }
    if (x == 0.0d) {
      return 0.0d
    }
    if (Double.isInfinite(x)) {
      return 1.0d
    }
    if (x < a + 1.0d) {
      return gammaSeries(a, x)
    }
    return 1.0d - regularizedIncompleteGammaQ(a, x)
  }

  /**
   * BigDecimal-friendly lower regularized incomplete gamma function.
   *
   * @param a the shape parameter
   * @param x the evaluation point
   * @return the regularized incomplete gamma value
   */
  static BigDecimal regularizedIncompleteGammaP(Number a, Number x) {
    BigDecimal.valueOf(regularizedIncompleteGammaP(
      NumericConversion.toFiniteDouble(a, 'a'),
      NumericConversion.toFiniteDouble(x, 'x')
    ))
  }

  /**
   * Computes the regularized upper incomplete gamma function Q(a, x).
   *
   * @param a shape parameter (a > 0)
   * @param x integration limit (x >= 0)
   * @return the regularized upper incomplete gamma function
   * @throws IllegalArgumentException if a <= 0 or x < 0
   */
  static double regularizedIncompleteGammaQ(double a, double x) {
    if (a <= 0.0d) {
      throw new IllegalArgumentException("a must be positive, got: $a")
    }
    if (x < 0.0d) {
      throw new IllegalArgumentException("x must be non-negative, got: $x")
    }
    if (x == 0.0d) {
      return 1.0d
    }
    if (Double.isInfinite(x)) {
      return 0.0d
    }
    if (x < a + 1.0d) {
      return 1.0d - gammaSeries(a, x)
    }
    return gammaContinuedFraction(a, x)
  }

  /**
   * BigDecimal-friendly upper regularized incomplete gamma function.
   *
   * @param a the shape parameter
   * @param x the evaluation point
   * @return the upper-tail regularized incomplete gamma value
   */
  static BigDecimal regularizedIncompleteGammaQ(Number a, Number x) {
    BigDecimal.valueOf(regularizedIncompleteGammaQ(
      NumericConversion.toFiniteDouble(a, 'a'),
      NumericConversion.toFiniteDouble(x, 'x')
    ))
  }

  private static double gammaSeries(double a, double x) {
    double gln = logGamma(a)
    double sum = 1.0d / a
    double delta = sum
    double ap = a

    for (int n = 1; n <= MAX_ITERATIONS; n++) {
      ap += 1.0d
      delta *= x / ap
      sum += delta

      if (Math.abs(delta) < Math.abs(sum) * EPSILON) {
        return sum * Math.exp(-x + a * Math.log(x) - gln)
      }
    }

    throw new IllegalStateException("Gamma series failed to converge after $MAX_ITERATIONS iterations")
  }

  private static double gammaContinuedFraction(double a, double x) {
    double gln = logGamma(a)
    double b = x + 1.0d - a
    double c = 1.0d / GAMMA_TINY
    double d = 1.0d / b
    double h = d

    for (int i = 1; i <= MAX_ITERATIONS; i++) {
      double an = -i * (i - a)
      b += 2.0d
      d = an * d + b
      if (Math.abs(d) < GAMMA_TINY) {
        d = GAMMA_TINY
      }
      c = b + an / c
      if (Math.abs(c) < GAMMA_TINY) {
        c = GAMMA_TINY
      }
      d = 1.0d / d
      double delta = d * c
      h *= delta

      if (Math.abs(delta - 1.0d) < EPSILON) {
        return Math.exp(-x + a * Math.log(x) - gln) * h
      }
    }

    throw new IllegalStateException("Gamma continued fraction failed to converge after $MAX_ITERATIONS iterations")
  }
}
