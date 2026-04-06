package se.alipsa.matrix.stats.solver

import groovy.transform.CompileStatic

/**
 * Bracketing Brent-Dekker root solver for one-dimensional continuous functions.
 *
 * <p>This is a low-level numerical kernel, so it intentionally uses primitive doubles
 * rather than BigDecimal to preserve the algorithm's expected floating-point behavior
 * and avoid allocation inside the iteration loop.</p>
 */
@SuppressWarnings('DuplicateNumberLiteral')
final class BrentSolver {

  private BrentSolver() {
  }

  /**
   * Finds a root of a continuous function within a bracketing interval.
   *
   * @param function objective function
   * @param min lower bracket endpoint
   * @param max upper bracket endpoint
   * @param relativeAccuracy relative convergence threshold used for interpolation steps
   * @param absoluteAccuracy absolute convergence threshold for the root value
   * @param maxIterations maximum iteration count
   * @return solver result containing the root and iteration metadata
   */
  static SolverResult solve(
      UnivariateObjective function,
      double min,
      double max,
      double relativeAccuracy,
      double absoluteAccuracy,
      int maxIterations
  ) {
    if (function == null) {
      throw new IllegalArgumentException("Function cannot be null")
    }
    if (min >= max) {
      throw new IllegalArgumentException("min ($min) must be smaller than max ($max)")
    }
    if (relativeAccuracy <= 0.0d || absoluteAccuracy <= 0.0d) {
      throw new IllegalArgumentException("Accuracies must be positive")
    }
    if (maxIterations < 1) {
      throw new IllegalArgumentException("maxIterations must be positive")
    }

    double a = min
    double b = max
    double fa = function.value(a)
    double fb = function.value(b)
    int evaluations = 2

    if (fa == 0.0d) {
      return new SolverResult(root: a, evaluations: evaluations, iterations: 0, lowerBound: a, upperBound: a)
    }
    if (fb == 0.0d) {
      return new SolverResult(root: b, evaluations: evaluations, iterations: 0, lowerBound: b, upperBound: b)
    }
    if (fa * fb > 0.0d) {
      throw new IllegalArgumentException("Function values at the interval endpoints must bracket a root")
    }

    double c = a
    double fc = fa
    double d = b - a
    double e = d

    for (int iteration = 1; iteration <= maxIterations; iteration++) {
      if (fb * fc > 0.0d) {
        c = a
        fc = fa
        d = b - a
        e = d
      }

      if (Math.abs(fc) < Math.abs(fb)) {
        a = b
        b = c
        c = a
        fa = fb
        fb = fc
        fc = fa
      }

      double tolerance = 2.0d * relativeAccuracy * Math.abs(b) + absoluteAccuracy
      double midpoint = 0.5d * (c - b)
      if (Math.abs(midpoint) <= tolerance || fb == 0.0d) {
        return new SolverResult(
            root: b,
            evaluations: evaluations,
            iterations: iteration,
            lowerBound: Math.min(b, c),
            upperBound: Math.max(b, c)
        )
      }

      if (Math.abs(e) >= tolerance && Math.abs(fa) > Math.abs(fb)) {
        double s = fb / fa
        double p
        double q
        if (a == c) {
          p = 2.0d * midpoint * s
          q = 1.0d - s
        } else {
          double qRatio = fa / fc
          double r = fb / fc
          p = s * (2.0d * midpoint * qRatio * (qRatio - r) - (b - a) * (r - 1.0d))
          q = (qRatio - 1.0d) * (r - 1.0d) * (s - 1.0d)
          if (p > 0.0d) {
            q = -q
          }
          p = Math.abs(p)
        }

        if (a == c) {
          if (p < Math.min(3.0d * midpoint * q - Math.abs(tolerance * q), Math.abs(e * q))) {
            e = d
            d = p / q
          } else {
            d = midpoint
            e = midpoint
          }
        } else if (p < Math.min(3.0d * midpoint * q - Math.abs(tolerance * q), Math.abs(e * q))) {
          e = d
          d = p / q
        } else {
          d = midpoint
          e = midpoint
        }
      } else {
        d = midpoint
        e = midpoint
      }

      a = b
      fa = fb
      if (Math.abs(d) > tolerance) {
        b += d
      } else {
        b += Math.copySign(tolerance, midpoint)
      }
      fb = function.value(b)
      evaluations++
    }

    throw new IllegalStateException("Brent solver failed to converge after $maxIterations iterations")
  }

  @CompileStatic
  static class SolverResult {
    /** Root value returned by the solver. */
    double root
    /** Number of function evaluations performed. */
    int evaluations
    /** Number of Brent iterations performed. */
    int iterations
    /** Lower bound of the final bracketing interval. */
    double lowerBound
    /** Upper bound of the final bracketing interval. */
    double upperBound
  }
}
