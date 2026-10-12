package se.alipsa.matrix.stats.timeseries

import groovy.transform.PackageScope

import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.stats.util.LeastSquaresKernel
import se.alipsa.matrix.stats.util.NumericConversion
import se.alipsa.matrix.stats.util.NumericTolerance

/**
 * The ADF-GLS test (or DF-GLS test) is a test for a unit root in a time series.
 * It was developed by Elliott, Rothenberg and Stock (ERS) in 1996 as a modification of the
 * augmented Dickey-Fuller test (ADF).
 *
 * <p>The ADF-GLS test uses generalized least squares (GLS) detrending prior to running the ADF test.
 * This modification provides improved power when there is an uncertain trend under the alternative hypothesis,
 * making it more powerful than the standard ADF test, especially in small samples.</p>
 *
 * <h3>When to Use</h3>
 * <ul>
 * <li>When testing for unit roots in economic or financial time series</li>
 * <li>When you need more statistical power than the standard ADF test</li>
 * <li>When the series may contain a deterministic trend or drift</li>
 * <li>Particularly useful for small to moderate sample sizes (n < 200)</li>
 * </ul>
 *
 * <h3>Hypotheses</h3>
 * <ul>
 * <li><strong>H0 (null):</strong> The series has a unit root (non-stationary, γ = 0)</li>
 * <li><strong>H1 (alternative):</strong> The series is stationary (γ < 0)</li>
 * </ul>
 *
 * <h3>Test Procedure</h3>
 * <ol>
 * <li>GLS-detrend the data with parameter α (α = 1 - 7/T for drift, α = 1 - 13.5/T for trend)</li>
 * <li>Run ADF regression on the detrended data: Δỹ_t = γỹ_{t-1} + δ₁Δỹ_{t-1} + ... + δₚΔỹ_{t-p} + ε_t</li>
 * <li>Compare t-statistic against ADF-GLS specific critical values</li>
 * </ol>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Test for unit root with drift (default)
 * def timeSeries = [100, 102, 101, 105, 107, 106, 110, 112] as double[]
 * def result = AdfGls.test(timeSeries)
 * println result.interpret()
 *
 * // Test with trend and specified lags
 * def result2 = AdfGls.test(timeSeries, 2, "trend")
 * println result2.evaluate()
 * </pre>
 *
 * <h3>References</h3>
 * <ul>
 * <li>Elliott, G., Rothenberg, T.J., & Stock, J.H. (1996). "Efficient Tests for an Autoregressive Unit Root",
 * Econometrica, 64(4), 813-836.</li>
 * <li>R's urca package: ur.ers() function</li>
 * <li>Stata's dfuller command with -gls option</li>
 * </ul>
 */
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral', 'ParameterName', 'VariableName'])
class AdfGls {
  private static final Logger log = Logger.getLogger(AdfGls)

  /**
   * Performs the ADF-GLS test for a unit root.
   *
   * @param data The time series data
   * @param lags The number of lags to include (default: auto-selected using modified AIC)
   * @param type The type of test: "drift" (constant only) or "trend" (constant and trend)
   * @return AdfGlsResult containing test statistic and conclusion
   */
  @SuppressWarnings('MethodSize')
  static AdfGlsResult test(double[] data, Integer lags = null, String type = 'drift') {
    if (data == null) {
      throw new IllegalArgumentException('Data cannot be null')
    }
    if (data.length < 10) {
      throw new IllegalArgumentException("Data must have at least 10 observations (got ${data.length})")
    }

    if (type != 'drift' && type != 'trend') {
      throw new IllegalArgumentException("Type must be 'drift' or 'trend' (got '${type}')")
    }

    int n = data.length

    if (!TimeSeriesUtils.isFinite(data)) {
      throw new IllegalArgumentException(TimeSeriesUtils.NON_FINITE_DATA_MESSAGE)
    }
    if (!TimeSeriesUtils.hasVariation(data)) {
      throw new IllegalArgumentException(TimeSeriesUtils.CONSTANT_SERIES_MESSAGE)
    }

    // Auto-select lags if not provided
    int p = lags != null ? lags : selectLags(data, type)

    // GLS detrending parameter: α = 1 + c/n
    // c = -7 for drift, c = -13.5 for trend
    double c = type == 'drift' ? -7.0 : -13.5
    double alpha = 1.0 + c / n

    // GLS-detrend the data
    double[] detrended = glsDetrend(data, alpha, type)
    if (NumericTolerance.isNegligible(NumericTolerance.maxMagnitude(detrended), NumericTolerance.maxMagnitude(data))) {
      throw new IllegalArgumentException('GLS detrended data are essentially constant')
    }

    // Run ADF-type regression on detrended data
    // Δỹ_t = γỹ_{t-1} + δ₁Δỹ_{t-1} + ... + δₚΔỹ_{t-p} + ε_t

    // Calculate first differences of detrended data
    double[] dy = new double[n - 1]
    for (int i = 1; i < n; i++) {
      dy[i - 1] = detrended[i] - detrended[i - 1]
    }

    // Prepare regression
    int nObs = n - p - 1
    if (nObs < 10) {
      throw new IllegalArgumentException('Insufficient observations after accounting for lags')
    }

    // Build design matrix: [ỹ_{t-1}, Δỹ_{t-1}, ..., Δỹ_{t-p}]
    double[][] X = new double[nObs][p + 1]
    double[] response = new double[nObs]

    for (int i = 0; i < nObs; i++) {
      int t = p + i
      // Response: Δỹ_t
      response[i] = dy[t]

      // Predictor: ỹ_{t-1}
      X[i][0] = detrended[t]

      // Lagged differences: Δỹ_{t-1}, ..., Δỹ_{t-p}
      for (int j = 1; j <= p; j++) {
        X[i][j] = dy[t - j]
      }
    }

    LeastSquaresKernel.OlsComputation fit = LeastSquaresKernel.fitMultipleLinearRegression(response, X)
    double gamma = fit.coefficients()[0]
    double gammaSE = fit.standardErrors()[0]

    // Calculate t-statistic (ADF-GLS statistic)
    double tStatistic = gamma / gammaSE

    // Get critical values
    double cv1pct = getCriticalValue(type, n, 0.01)
    double cv5pct = getCriticalValue(type, n, 0.05)
    double cv10pct = getCriticalValue(type, n, 0.10)

    new AdfGlsResult(
      statistic: BigDecimal.valueOf(tStatistic),
      gamma: BigDecimal.valueOf(gamma),
      standardError: BigDecimal.valueOf(gammaSE),
      lags: p,
      sampleSize: n,
      testType: type,
      criticalValue1pct: BigDecimal.valueOf(cv1pct),
      criticalValue5pct: BigDecimal.valueOf(cv5pct),
      criticalValue10pct: BigDecimal.valueOf(cv10pct)
    )
  }

  /**
   * Performs the ADF-GLS test on a List of numbers.
   */
  static AdfGlsResult test(List<? extends Number> data, Integer lags = null, String type = 'drift') {
    double[] array = data*.doubleValue() as double[]
    test(array, lags, type)
  }

  /**
   * GLS detrending of the data.
   * Transforms data using quasi-differencing: z_t = y_t - α*y_{t-1}
   * Then regresses on transformed deterministic terms and removes fitted values.
   */
  private static double[] glsDetrend(double[] data, double alpha, String type) {
    int n = data.length

    // Quasi-difference the data and deterministic terms
    double[] z = new double[n]
    z[0] = data[0]
    for (int i = 1; i < n; i++) {
      z[i] = data[i] - alpha * data[i - 1]
    }

    // Build quasi-differenced deterministic terms
    double[][] X = new double[n][type == 'drift' ? 1 : 2]

    // Constant term
    X[0][0] = 1.0
    for (int i = 1; i < n; i++) {
      X[i][0] = 1.0 - alpha
    }

    // Trend term (if needed)
    if (type == 'trend') {
      X[0][1] = 1.0
      for (int i = 1; i < n; i++) {
        X[i][1] = (i + 1.0) - alpha * i
      }
    }

    // Perform GLS regression: z = X*β + ε
    double[] beta = TimeSeriesUtils.solveLinearSystem(X, z)

    // Compute fitted values on original deterministic terms
    double[] fitted = new double[n]
    for (int i = 0; i < n; i++) {
      fitted[i] = beta[0]  // Constant
      if (type == 'trend') {
        fitted[i] += beta[1] * (i + 1.0)  // Trend
      }
    }

    // Detrended series
    double[] detrended = new double[n]
    for (int i = 0; i < n; i++) {
      detrended[i] = data[i] - fitted[i]
    }

    detrended
  }

  /**
   * Select optimal number of lags using modified AIC (MAIC).
   * Following Ng and Perron (2001) recommendation.
   */
  @PackageScope
  static int selectLags(double[] data, String type) {
    int n = data.length
    int maxLags = Math.min(12, (int) Math.floor(12.0 * Math.pow(n / 100.0, 0.25)))

    double bestMAIC = Double.POSITIVE_INFINITY
    int bestLag = 0

    maxLags = Math.min(maxLags, (n - 12).intdiv(2))
    double[] detrended = glsDetrend(data, 1 + (type == 'drift' ? -7.0d : -13.5d) / n, type)
    int count = n - maxLags - 1
    for (int p = 0; p <= maxLags; p++) {
      try {
        double[][] design = new double[count][p + 1]
        double[] response = new double[count]
        double laggedSquares = 0
        for (int i = 0; i < count; i++) {
          int t = maxLags + i
          response[i] = detrended[t + 1] - detrended[t]
          design[i][0] = detrended[t]
          laggedSquares += detrended[t] * detrended[t]
          for (int j = 1; j <= p; j++) {
            design[i][j] = detrended[t - j + 1] - detrended[t - j]
          }
        }
        LeastSquaresKernel.OlsComputation fit = LeastSquaresKernel.fitMultipleLinearRegression(response, design)
        double sigma2 = fit.residualSumOfSquares() / count
        double gamma = fit.coefficients()[0]
        double tau = gamma * gamma * laggedSquares / sigma2
        double maic = Math.log(sigma2) + 2.0d * (tau + p) / count

        if (maic < bestMAIC) {
          bestMAIC = maic
          bestLag = p
        }
      } catch (IllegalArgumentException e) {
        if (isNumericalFailure(e)) {
          log.warn("Numerical failure for ADF-GLS lag $p during MAIC selection: ${e.message}")
        } else {
          log.debug("Skipping lag $p during ADF-GLS MAIC selection: ${e.message}")
        }
      } catch (Exception e) {
        log.error("Potential bug while selecting lag $p for ADF-GLS: ${e.message}", e)
        throw e
      }
    }

    if (bestMAIC == Double.POSITIVE_INFINITY) {
      throw new IllegalArgumentException("Unable to select ADF-GLS lag: all candidate lags 0..${maxLags} failed")
    }

    bestLag
  }

  private static boolean isNumericalFailure(IllegalArgumentException e) {
    String message = e.message ?: ''
    message.contains('Singular matrix') || message.contains('Non-finite value')
  }

  /**
   * Get critical values for ADF-GLS test.
   * Based on Elliott, Rothenberg, and Stock (1996) and updated tables.
   */
  private static double getCriticalValue(String type, int n, double significance) {
    int index = significance == 0.01d ? 0 : significance == 0.05d ? 1 : 2
    if (type == 'drift') {
      return ([-2.5658d - 1.96d / n - 10.04d / (n * (double) n),
               -1.9393d - 0.398d / n, -1.6156d - 0.181d / n] as double[])[index]
    }
    // urca 1.3.4 ur.ers uses these ERS Table 1 rows without interpolation.
    List<List<BigDecimal>> rows = [[-3.77, -3.19, -2.89], [-3.58, -3.03, -2.74],
                                  [-3.46, -2.93, -2.64], [-3.48, -2.89, -2.57]]
    rows[n < 50 ? 0 : n < 100 ? 1 : n <= 200 ? 2 : 3][index].doubleValue()
  }

  /**
   * Result class for ADF-GLS test.
   */
  static class AdfGlsResult {
    /** The ADF-GLS test statistic (t-statistic for γ) */
    BigDecimal statistic

    /** The estimated γ coefficient */
    BigDecimal gamma

    /** Standard error of γ */
    BigDecimal standardError

    /** Number of lags used */
    int lags

    /** Sample size */
    int sampleSize

    /** Test type (drift/trend) */
    String testType

    /** Critical value at 1% significance */
    BigDecimal criticalValue1pct

    /** Critical value at 5% significance */
    BigDecimal criticalValue5pct

    /** Critical value at 10% significance */
    BigDecimal criticalValue10pct

    /**
     * Interprets the test result.
     *
     * @param alpha Significance level (default 0.05)
     * @return Interpretation string
     */
    String interpret(Number alpha = 0.05) {
      BigDecimal alphaValue = NumericConversion.toAlpha(alpha)
      BigDecimal cv = alphaValue == 0.01 ? criticalValue1pct :
        alphaValue == 0.10 ? criticalValue10pct : criticalValue5pct

      if (statistic < cv) {
        return "Reject H0: Series appears stationary (ADF-GLS = ${String.format('%.4f', statistic)}, CV = ${String.format('%.4f', cv)})"
      }
      return "Fail to reject H0: Unit root likely present, series appears non-stationary (ADF-GLS = ${String.format('%.4f', statistic)}, CV = ${String.format('%.4f', cv)})"
    }

    /**
     * Evaluates the test result with detailed information.
     */
    String evaluate(Number alpha = 0.05) {
      BigDecimal alphaValue = NumericConversion.toAlpha(alpha)
      String conclusion = statistic < criticalValue5pct ? 'stationary' : 'non-stationary (unit root present)'

      String.format(
        'ADF-GLS test:\\n' +
        'Test type: %s\\n' +
        'Lags: %d\\n' +
        'ADF-GLS statistic: %.4f\\n' +
        'Critical values: 1%% = %.4f, 5%% = %.4f, 10%% = %.4f\\n' +
        'Sample size: %d\\n' +
        'Conclusion: Series appears %s at %.0f%% significance level',
        testType, lags, statistic, criticalValue1pct, criticalValue5pct, criticalValue10pct,
        sampleSize, conclusion, (alphaValue * 100) as double
      )
    }

    @Override
    String toString() {
      """ADF-GLS Test (Elliott-Rothenberg-Stock)
  Type: ${testType}
  Lags: ${lags}
  Sample size: ${sampleSize}
  ADF-GLS statistic: ${String.format('%.4f', statistic)}
  γ coefficient: ${String.format('%.6f', gamma)}
  Standard error: ${String.format('%.6f', standardError)}
  Critical values:
    1%: ${String.format('%.4f', criticalValue1pct)}
    5%: ${String.format('%.4f', criticalValue5pct)}
   10%: ${String.format('%.4f', criticalValue10pct)}

  ${interpret()}"""
    }
  }
}
