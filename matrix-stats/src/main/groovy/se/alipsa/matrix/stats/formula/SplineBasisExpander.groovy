package se.alipsa.matrix.stats.formula

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Generates natural cubic spline basis functions for use in GAM smooth terms.
 * Uses equally-spaced interior knots and the truncated-power basis with the
 * natural constraint (linear beyond boundary knots).
 *
 * <p>The basis is centered and excludes the linear {@code x} term so
 * {@code y ~ x + s(x)} can combine an unpenalized linear effect with a
 * separate smooth component without duplicating columns.
 */
@SuppressWarnings('DuplicateNumberLiteral')
final class SplineBasisExpander {

  private SplineBasisExpander() {}

  /**
   * Generates a centered natural cubic spline basis matrix without the raw {@code x} term.
   *
   * <p>The basis does NOT include a constant column or the raw/centered {@code x}
   * column. The outer model supplies the intercept, and callers can add {@code x}
   * explicitly when they want a separate linear term.
   *
   * <p>Produces {@code df} centered nonlinear basis columns using a constrained
   * truncated-power representation with equally spaced interior knots.
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

    double xMin = Double.MAX_VALUE
    double xMax = -Double.MAX_VALUE
    for (double v : x) {
      if (v < xMin) {
        xMin = v
      }
      if (v > xMax) {
        xMax = v
      }
    }

    if (xMax == xMin) {
      return new double[n][df]
    }

    // Use one extra interior knot so the nonlinear part still has df columns
    // after excluding the linear x component from the returned basis.
    int nKnots = df + 3
    double[] knots = new double[nKnots]
    for (int i = 0; i < nKnots; i++) {
      knots[i] = xMin + (xMax - xMin) * i / (nKnots - 1)
    }

    double boundaryMax = knots[nKnots - 1]
    double referenceInterior = knots[nKnots - 2]
    double denom = boundaryMax - referenceInterior
    if (denom == 0.0d) {
      denom = 1.0d
    }

    double[][] basis = new double[n][df]
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < df; j++) {
        double knot = knots[j + 1]
        double basisAtKnot = (truncPow3(x[i] - knot) - truncPow3(x[i] - boundaryMax)) / (boundaryMax - knot)
        double basisAtReference =
          (truncPow3(x[i] - referenceInterior) - truncPow3(x[i] - boundaryMax)) / denom
        basis[i][j] = basisAtKnot - basisAtReference
      }
    }

    centerColumns(basis)
    basis
  }

  /**
   * Generates a centered natural cubic spline basis matrix without the raw {@code x} term.
   *
   * @param x the predictor values (length n)
   * @param df degrees of freedom (number of basis columns, must be >= 1)
   * @return an n x df matrix of basis function evaluations
   */
  static double[][] naturalCubicSplineBasis(List<? extends Number> x, int df) {
    naturalCubicSplineBasis(NumericConversion.toDoubleArray(x, 'x'), df)
  }

  private static double truncPow3(double v) {
    v > 0.0d ? v * v * v : 0.0d
  }

  private static void centerColumns(double[][] basis) {
    if (basis.length == 0 || basis[0].length == 0) {
      return
    }
    int rows = basis.length
    int columns = basis[0].length
    for (int column = 0; column < columns; column++) {
      double mean = 0.0d
      for (int row = 0; row < rows; row++) {
        mean += basis[row][column]
      }
      mean /= rows
      for (int row = 0; row < rows; row++) {
        basis[row][column] -= mean
      }
    }
  }
}
