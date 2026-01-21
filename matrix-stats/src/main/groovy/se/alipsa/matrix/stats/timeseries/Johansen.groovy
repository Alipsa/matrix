package se.alipsa.matrix.stats.timeseries

import groovy.transform.CompileStatic
import org.apache.commons.math3.linear.*

/**
 * The Johansen test, named after Søren Johansen, is a procedure for testing cointegration of several,
 * say k, I(1) time series.
 *
 * This test permits more than one cointegrating relationship so is more generally applicable than the
 * Engle–Granger test which is based on the Dickey–Fuller (or the augmented) test for unit roots in the
 * residuals from a single (estimated) cointegrating relationship.
 *
 * There are two types of Johansen test, either with trace or with eigenvalue,
 * and the inferences might be a little bit different.
 *
 * This implementation provides the trace test statistic:
 * Trace(r) = -T * Σ(i=r+1 to k) ln(1 - λ_i)
 *
 * where T is sample size, k is number of variables, r is the number of cointegrating relations under H0,
 * and λ_i are the eigenvalues.
 *
 * Example:
 * <pre>
 * // Test for cointegration between two series
 * def y1 = [1.0, 1.2, 1.5, 1.3, 1.6, ...] as double[]
 * def y2 = [2.0, 2.3, 2.8, 2.5, 3.0, ...] as double[]
 * def data = [y1, y2]
 * def result = Johansen.test(data, 1)  // Test with 1 lag
 * println result.toString()
 * </pre>
 *
 * Reference:
 * - Johansen, S. (1991). "Estimation and Hypothesis Testing of Cointegration Vectors"
 * - Johansen, S. (1995). "Likelihood-Based Inference in Cointegrated Vector Autoregressive Models"
 * - R's urca package (ca.jo function)
 */
@CompileStatic
class Johansen {

  /**
   * Performs the Johansen cointegration test.
   *
   * @param data List of time series (each series is a double array)
   * @param lags Number of lags in the VAR model
   * @param type Type of deterministic component ('none', 'const', 'trend')
   * @return JohansenResult containing test statistics and eigenvalues
   */
  static JohansenResult test(List<double[]> data, int lags = 1, String type = 'const') {
    if (data == null || data.isEmpty()) {
      throw new IllegalArgumentException("Data cannot be null or empty")
    }

    if (lags < 1) {
      throw new IllegalArgumentException("Lags must be at least 1 (got ${lags})")
    }

    if (!(type in ['none', 'const', 'trend'])) {
      throw new IllegalArgumentException("Type must be 'none', 'const', or 'trend' (got '${type}')")
    }

    int k = data.size()  // Number of variables
    int n = data[0].length  // Number of observations

    // Validate all series have same length
    for (int i = 1; i < k; i++) {
      if (data[i].length != n) {
        throw new IllegalArgumentException("All series must have the same length")
      }
    }

    if (n < lags + 20) {
      throw new IllegalArgumentException("Need at least ${lags + 20} observations (got ${n})")
    }

    // Form the VAR model: ΔY_t = Π Y_{t-1} + Γ_1 ΔY_{t-1} + ... + Γ_{p-1} ΔY_{t-p+1} + μ + ε_t
    // We use the two-step approach:
    // 1. Regress ΔY_t and Y_{t-1} on lagged differences and deterministic terms
    // 2. Compute moment matrices from residuals
    // 3. Solve eigenvalue problem

    int effectiveN = n - lags

    // Create differenced data
    double[][] deltaY = new double[effectiveN][k]
    double[][] laggedY = new double[effectiveN][k]

    for (int t = 0; t < effectiveN; t++) {
      int actualT = t + lags
      for (int i = 0; i < k; i++) {
        deltaY[t][i] = data[i][actualT] - data[i][actualT - 1]
        laggedY[t][i] = data[i][actualT - 1]
      }
    }

    // Build design matrix with short-run dynamics only (lagged differences + deterministic)
    int numRegressors = (lags > 1 ? k * (lags - 1) : 0) + (type == 'const' ? 1 : 0) + (type == 'trend' ? 1 : 0)

    RealMatrix R0, R1  // Residuals

    if (numRegressors > 0) {
      double[][] Z = new double[effectiveN][numRegressors]

      for (int t = 0; t < effectiveN; t++) {
        int col = 0
        int actualT = t + lags

        // Lagged differences
        if (lags > 1) {
          for (int lag = 1; lag < lags; lag++) {
            for (int i = 0; i < k; i++) {
              Z[t][col++] = data[i][actualT - lag] - data[i][actualT - lag - 1]
            }
          }
        }

        // Deterministic terms
        if (type == 'const') {
          Z[t][col++] = 1.0
        }
        if (type == 'trend') {
          Z[t][col++] = t + 1
        }
      }

      RealMatrix ZMatrix = MatrixUtils.createRealMatrix(Z)
      RealMatrix deltaYMatrix = MatrixUtils.createRealMatrix(deltaY)
      RealMatrix laggedYMatrix = MatrixUtils.createRealMatrix(laggedY)

      // Compute projection matrix: M = I - Z(Z'Z)^{-1}Z'
      RealMatrix ZtZ = ZMatrix.transpose().multiply(ZMatrix)
      DecompositionSolver solver = new LUDecomposition(ZtZ).getSolver()

      if (!solver.isNonSingular()) {
        throw new IllegalArgumentException("Singular matrix in short-run regression - cannot perform Johansen test")
      }

      RealMatrix ZtZinv = solver.getInverse()
      RealMatrix projZ = ZMatrix.multiply(ZtZinv).multiply(ZMatrix.transpose())

      // Compute residuals: R0 = M * ΔY, R1 = M * Y_{t-1}
      R0 = deltaYMatrix.subtract(projZ.multiply(deltaYMatrix))
      R1 = laggedYMatrix.subtract(projZ.multiply(laggedYMatrix))
    } else {
      // No regressors - residuals are just the original data
      R0 = MatrixUtils.createRealMatrix(deltaY)
      R1 = MatrixUtils.createRealMatrix(laggedY)
    }

    // Compute moment matrices from residuals
    RealMatrix S00 = R0.transpose().multiply(R0).scalarMultiply(1.0 / effectiveN)
    RealMatrix S11 = R1.transpose().multiply(R1).scalarMultiply(1.0 / effectiveN)
    RealMatrix S01 = R0.transpose().multiply(R1).scalarMultiply(1.0 / effectiveN)

    // Solve generalized eigenvalue problem: |λ S11 - S10' S00^{-1} S01| = 0
    // Equivalent to eigenvalues of S11^{-1} S10' S00^{-1} S01

    RealMatrix S00Inv = new LUDecomposition(S00).getSolver().getInverse()
    RealMatrix S11Inv = new LUDecomposition(S11).getSolver().getInverse()

    RealMatrix product = S11Inv.multiply(S01.transpose()).multiply(S00Inv).multiply(S01)

    EigenDecomposition eigen = new EigenDecomposition(product)
    double[] eigenvalues = eigen.getRealEigenvalues()

    // Sort eigenvalues in descending order
    Arrays.sort(eigenvalues)
    double[] sortedEigenvalues = new double[eigenvalues.length]
    for (int i = 0; i < eigenvalues.length; i++) {
      sortedEigenvalues[i] = eigenvalues[eigenvalues.length - 1 - i]
    }

    // Compute trace statistics for different values of r
    double[] traceStats = new double[k]
    for (int r = 0; r < k; r++) {
      double sum = 0.0
      for (int i = r; i < k; i++) {
        sum += Math.log(1.0 - sortedEigenvalues[i])
      }
      traceStats[r] = -effectiveN * sum
    }

    // Critical values (5% significance) from Johansen tables
    // These are approximate values for comparison
    double[][] criticalValues = getCriticalValues(type)

    return new JohansenResult(
      eigenvalues: sortedEigenvalues,
      traceStatistics: traceStats,
      criticalValues5pct: criticalValues,
      sampleSize: effectiveN,
      numVariables: k,
      lags: lags,
      type: type
    )
  }

  /**
   * Gets approximate critical values for the trace test at 5% significance level.
   * Values from Osterwald-Lenum (1992).
   */
  private static double[][] getCriticalValues(String type) {
    // Critical values depend on type and number of cointegrating relations
    // Format: [r][k] where r is cointegration rank, k is number of variables
    // These are for 'const' case (most common)

    if (type == 'const') {
      return [
        [2.98, 15.41, 29.68, 47.21, 68.52],  // r = 0
        [0.00, 3.76, 15.41, 29.68, 47.21],   // r = 1
        [0.00, 0.00, 3.76, 15.41, 29.68],    // r = 2
        [0.00, 0.00, 0.00, 3.76, 15.41],     // r = 3
        [0.00, 0.00, 0.00, 0.00, 3.76]       // r = 4
      ] as double[][]
    } else if (type == 'trend') {
      return [
        [10.49, 22.76, 39.06, 59.14, 83.20],
        [0.00, 6.50, 22.76, 39.06, 59.14],
        [0.00, 0.00, 6.50, 22.76, 39.06],
        [0.00, 0.00, 0.00, 6.50, 22.76],
        [0.00, 0.00, 0.00, 0.00, 6.50]
      ] as double[][]
    } else {  // 'none'
      return [
        [2.71, 13.33, 26.79, 44.49, 66.23],
        [0.00, 2.71, 13.33, 26.79, 44.49],
        [0.00, 0.00, 2.71, 13.33, 26.79],
        [0.00, 0.00, 0.00, 2.71, 13.33],
        [0.00, 0.00, 0.00, 0.00, 2.71]
      ] as double[][]
    }
  }

  /**
   * Result class for Johansen cointegration test.
   */
  @CompileStatic
  static class JohansenResult {
    /** Eigenvalues (sorted in descending order) */
    double[] eigenvalues

    /** Trace statistics for different cointegration ranks */
    double[] traceStatistics

    /** Critical values at 5% significance level [r][k-1] */
    double[][] criticalValues5pct

    /** Effective sample size */
    int sampleSize

    /** Number of variables */
    int numVariables

    /** Number of lags */
    int lags

    /** Type of deterministic component */
    String type

    /**
     * Interprets the test result.
     *
     * @return Interpretation string
     */
    String interpret() {
      StringBuilder sb = new StringBuilder()
      sb.append("Johansen Cointegration Test Results:\n")

      int cointRank = 0
      for (int r = 0; r < numVariables; r++) {
        double critVal = r < criticalValues5pct.length && numVariables - 1 < criticalValues5pct[r].length ?
                         criticalValues5pct[r][numVariables - 1] : Double.NaN

        if (traceStatistics[r] > critVal) {
          cointRank = r + 1
          sb.append("H0: r = ${r} REJECTED (Trace = ${String.format('%.4f', traceStatistics[r])}, Critical = ${String.format('%.4f', critVal)})\n")
        } else {
          sb.append("H0: r = ${r} NOT REJECTED (Trace = ${String.format('%.4f', traceStatistics[r])}, Critical = ${String.format('%.4f', critVal)})\n")
          break
        }
      }

      sb.append("\nConclusion: Evidence suggests ${cointRank} cointegrating relationship(s)")
      return sb.toString()
    }

    @Override
    String toString() {
      return """Johansen Cointegration Test
  Number of variables: ${numVariables}
  Sample size: ${sampleSize}
  Lags: ${lags}
  Type: ${type}

  Eigenvalues: ${eigenvalues.collect { String.format('%.4f', it) }.join(', ')}

  ${interpret()}"""
    }
  }
}
