package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

/**
 * Generates natural cubic spline basis functions for use in GAM smooth terms.
 * Uses equally-spaced interior knots and the truncated-power basis with the
 * natural constraint (linear beyond boundary knots).
 *
 * <p>The basis is intercept-free and centered to avoid non-identifiability
 * when the outer model includes its own intercept column.
 */
@CompileStatic
@SuppressWarnings('DuplicateNumberLiteral')
final class SplineBasisExpander {

  private SplineBasisExpander() {}

  /**
   * Generates an intercept-free, centered natural cubic spline basis matrix.
   *
   * <p>The basis does NOT include a constant column or a raw x column — those
   * are supplied by the outer model (intercept from the formula, raw x as a
   * separate linear term if desired). This avoids non-identifiability when the
   * model already includes an intercept.
   *
   * <p>Produces {@code df} centered basis columns using the constrained
   * truncated-power representation: one centered-x column plus {@code df - 1}
   * constrained spline columns.
   *
   * @param x the predictor values (length n)
   * @param df degrees of freedom (number of basis columns, must be >= 1)
   * @return an n x df matrix of basis function evaluations
   */
  static double[][] naturalCubicSplineBasis(double[] x, int df) {
    if (df < 1) {
      throw new IllegalArgumentException("df must be >= 1, got ${df}")
    }

    int n = x.length

    // Center x to avoid collinearity with a model intercept
    double xMean = 0.0d
    for (double v : x) {
      xMean += v
    }
    xMean /= n

    double xMin = Double.MAX_VALUE
    double xMax = -Double.MAX_VALUE
    for (double v : x) {
      if (v < xMin) xMin = v
      if (v > xMax) xMax = v
    }

    if (df == 1) {
      // Single centered-x column
      double[][] basis = new double[n][1]
      for (int i = 0; i < n; i++) {
        basis[i][0] = x[i] - xMean
      }
      return basis
    }

    // nKnots = df + 1 (boundary + interior); produces df columns:
    //   column 0 = centered x
    //   columns 1..df-1 = constrained truncated-power spline functions
    int nKnots = df + 1
    double[] knots = new double[nKnots]
    for (int i = 0; i < nKnots; i++) {
      knots[i] = xMin + (xMax - xMin) * i / (nKnots - 1)
    }

    int nInterior = nKnots - 2
    double kLast = knots[nKnots - 1]
    double kSecondLast = knots[nKnots - 2]
    double denom = kLast - kSecondLast
    if (denom == 0.0d) {
      denom = 1.0d
    }

    double[][] basis = new double[n][df]
    for (int i = 0; i < n; i++) {
      // Column 0: centered x
      basis[i][0] = x[i] - xMean

      // Columns 1..df-1: constrained truncated-power basis
      for (int j = 0; j < nInterior; j++) {
        double kj = knots[j + 1] // interior knots
        double d_j = (truncPow3(x[i] - kj) - truncPow3(x[i] - kLast)) / denom
        double d_last = (truncPow3(x[i] - kSecondLast) - truncPow3(x[i] - kLast)) / denom
        basis[i][j + 1] = d_j - d_last
      }
    }

    basis
  }

  private static double truncPow3(double v) {
    v > 0.0d ? v * v * v : 0.0d
  }
}
