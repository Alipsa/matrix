package se.alipsa.matrix.stats.timeseries

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.FDistribution

/**
 * The Chow test proposed by econometrician Gregory Chow in 1960, is a test of whether the true coefficients
 * in two linear regressions on different data sets are equal.
 *
 * In econometrics, it is most commonly used in time series analysis to test for the presence of a structural
 * break at a period which can be assumed to be known a priori (for instance, a major historical event such as a war).
 *
 * The test compares:
 * - H0: No structural break (coefficients are equal in both periods)
 * - H1: Structural break exists (coefficients differ)
 *
 * The F-statistic is:
 * F = [(RSS_full - (RSS_1 + RSS_2)) / k] / [(RSS_1 + RSS_2) / (n - 2k)]
 *
 * where RSS = residual sum of squares, k = number of parameters, n = total observations
 *
 * Example:
 * <pre>
 * def y = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10] as double[]
 * def X = [[1, 1], [1, 2], [1, 3], [1, 4], [1, 5], [1, 6], [1, 7], [1, 8], [1, 9], [1, 10]] as double[][]
 * def result = Chow.test(y, X, 5)  // Test for break at observation 5
 * println result.toString()
 * </pre>
 *
 * Reference:
 * - Chow, G. C. (1960). "Tests of Equality Between Sets of Coefficients in Two Linear Regressions"
 * - R's strucchange package
 */
@CompileStatic
class Chow {

  /**
   * Performs the Chow test for a structural break.
   *
   * @param y Response variable
   * @param X Design matrix (including intercept column if desired)
   * @param breakPoint Index where the break is suspected (must be > k and < n - k for valid test)
   * @return ChowResult containing test statistic, p-value, and conclusion
   */
  static ChowResult test(double[] y, double[][] X, int breakPoint) {
    if (y == null || X == null) {
      throw new IllegalArgumentException("Data cannot be null")
    }

    int n = y.length
    int k = X[0].length

    if (X.length != n) {
      throw new IllegalArgumentException("X and y must have the same number of observations (got ${X.length} and ${n})")
    }

    if (breakPoint <= k) {
      throw new IllegalArgumentException("Break point must be > ${k} (number of parameters) for valid test (got ${breakPoint})")
    }

    if (breakPoint >= n - k) {
      throw new IllegalArgumentException("Break point must be < ${n - k} (n - k) for valid test (got ${breakPoint})")
    }

    // Fit full model
    double[] betaFull = fitOLS(y, X)
    double rssFull = calculateRSS(y, X, betaFull)

    // Split data at break point
    double[] y1 = new double[breakPoint]
    double[][] X1 = new double[breakPoint][k]
    for (int i = 0; i < breakPoint; i++) {
      y1[i] = y[i]
      X1[i] = X[i]
    }

    double[] y2 = new double[n - breakPoint]
    double[][] X2 = new double[n - breakPoint][k]
    for (int i = 0; i < n - breakPoint; i++) {
      y2[i] = y[breakPoint + i]
      X2[i] = X[breakPoint + i]
    }

    // Fit sub-models
    double[] beta1 = fitOLS(y1, X1)
    double rss1 = calculateRSS(y1, X1, beta1)

    double[] beta2 = fitOLS(y2, X2)
    double rss2 = calculateRSS(y2, X2, beta2)

    // Calculate Chow F-statistic
    double numerator = (rssFull - (rss1 + rss2)) / k
    double denominator = (rss1 + rss2) / (n - 2 * k)

    if (denominator < 1e-10) {
      throw new IllegalArgumentException("Denominator too small - perfect fit in sub-models")
    }

    double fStatistic = numerator / denominator

    // Calculate p-value
    FDistribution fDist = new FDistribution(k, n - 2 * k)
    double pValue = 1.0 - fDist.cumulativeProbability(fStatistic)

    return new ChowResult(
      statistic: fStatistic,
      pValue: pValue,
      df1: k,
      df2: n - 2 * k,
      breakPoint: breakPoint,
      rssFull: rssFull,
      rss1: rss1,
      rss2: rss2,
      sampleSize: n,
      numParameters: k
    )
  }

  /**
   * Performs the Chow test with automatic conversion of List inputs.
   */
  static ChowResult test(List<? extends Number> y, List<List<? extends Number>> X, int breakPoint) {
    double[] yArray = y.collect { it.doubleValue() } as double[]
    double[][] XArray = X.collect { row -> row.collect { it.doubleValue() } as double[] } as double[][]
    return test(yArray, XArray, breakPoint)
  }

  /**
   * Fit OLS regression and return coefficients.
   */
  private static double[] fitOLS(double[] y, double[][] X) {
    int n = y.length
    int k = X[0].length

    // Calculate X'X
    double[][] XtX = new double[k][k]
    for (int i = 0; i < k; i++) {
      for (int j = 0; j < k; j++) {
        double sum = 0.0
        for (int m = 0; m < n; m++) {
          sum += X[m][i] * X[m][j]
        }
        XtX[i][j] = sum
      }
    }

    // Calculate X'y
    double[] Xty = new double[k]
    for (int i = 0; i < k; i++) {
      double sum = 0.0
      for (int m = 0; m < n; m++) {
        sum += X[m][i] * y[m]
      }
      Xty[i] = sum
    }

    // Solve for beta
    return solveLinearSystem(XtX, Xty)
  }

  /**
   * Calculate residual sum of squares.
   */
  private static double calculateRSS(double[] y, double[][] X, double[] beta) {
    int n = y.length
    double rss = 0.0

    for (int i = 0; i < n; i++) {
      double fitted = 0.0
      for (int j = 0; j < beta.length; j++) {
        fitted += X[i][j] * beta[j]
      }
      double residual = y[i] - fitted
      rss += residual * residual
    }

    return rss
  }

  /**
   * Solve linear system Ax = b using Gaussian elimination.
   */
  private static double[] solveLinearSystem(double[][] A, double[] b) {
    int n = b.length
    double[][] augmented = new double[n][n + 1]

    // Create augmented matrix
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        augmented[i][j] = A[i][j]
      }
      augmented[i][n] = b[i]
    }

    // Forward elimination with partial pivoting
    for (int k = 0; k < n; k++) {
      // Find pivot
      int maxRow = k
      double maxVal = Math.abs(augmented[k][k])
      for (int i = k + 1; i < n; i++) {
        if (Math.abs(augmented[i][k]) > maxVal) {
          maxVal = Math.abs(augmented[i][k])
          maxRow = i
        }
      }

      // Swap rows
      if (maxRow != k) {
        double[] temp = augmented[k]
        augmented[k] = augmented[maxRow]
        augmented[maxRow] = temp
      }

      // Check for singular matrix
      if (Math.abs(augmented[k][k]) < 1e-14) {
        throw new IllegalArgumentException("Singular matrix - cannot solve linear system")
      }

      // Eliminate column
      for (int i = k + 1; i < n; i++) {
        double factor = augmented[i][k] / augmented[k][k]
        for (int j = k; j < n + 1; j++) {
          augmented[i][j] -= factor * augmented[k][j]
        }
      }
    }

    // Back substitution
    double[] x = new double[n]
    for (int i = n - 1; i >= 0; i--) {
      double sum = augmented[i][n]
      for (int j = i + 1; j < n; j++) {
        sum -= augmented[i][j] * x[j]
      }
      x[i] = sum / augmented[i][i]
    }

    return x
  }

  /**
   * Result class for Chow test.
   */
  @CompileStatic
  static class ChowResult {
    /** The Chow F-statistic */
    double statistic

    /** The p-value */
    double pValue

    /** Degrees of freedom (numerator) */
    int df1

    /** Degrees of freedom (denominator) */
    int df2

    /** Break point index */
    int breakPoint

    /** RSS for full model */
    double rssFull

    /** RSS for first sub-model */
    double rss1

    /** RSS for second sub-model */
    double rss2

    /** Sample size */
    int sampleSize

    /** Number of parameters */
    int numParameters

    /**
     * Interprets the test result.
     *
     * @param alpha Significance level (default 0.05)
     * @return Interpretation string
     */
    String interpret(double alpha = 0.05) {
      if (pValue < alpha) {
        return "Reject H0: Structural break detected at observation ${breakPoint} (F = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      } else {
        return "Fail to reject H0: No evidence of structural break at observation ${breakPoint} (F = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      }
    }

    /**
     * Evaluates the test result with detailed information.
     */
    String evaluate(double alpha = 0.05) {
      String conclusion = pValue < alpha ? "structural break present" : "no structural break detected"

      return String.format(
        "Chow test:\\n" +
        "Break point: %d\\n" +
        "F-statistic: %.4f\\n" +
        "p-value: %.4f\\n" +
        "Degrees of freedom: (%d, %d)\\n" +
        "RSS full model: %.4f\\n" +
        "RSS sub-models: %.4f + %.4f = %.4f\\n" +
        "Sample size: %d\\n" +
        "Number of parameters: %d\\n" +
        "Conclusion: %s at %.0f%% significance level",
        breakPoint, statistic, pValue, df1, df2,
        rssFull, rss1, rss2, rss1 + rss2,
        sampleSize, numParameters,
        conclusion, alpha * 100
      )
    }

    @Override
    String toString() {
      return """Chow Test for Structural Break
  Break point: ${breakPoint}
  Sample size: ${sampleSize}
  Parameters: ${numParameters}
  F-statistic: ${String.format('%.4f', statistic)}
  p-value: ${String.format('%.4f', pValue)}
  Degrees of freedom: (${df1}, ${df2})
  RSS full model: ${String.format('%.4f', rssFull)}
  RSS sub-models: ${String.format('%.4f', rss1)} + ${String.format('%.4f', rss2)} = ${String.format('%.4f', rss1 + rss2)}

  ${interpret()}"""
    }
  }
}
