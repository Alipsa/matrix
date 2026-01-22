package se.alipsa.matrix.stats.timeseries

import groovy.transform.CompileStatic
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression

/**
 * The augmented Dickey–Fuller test (ADF) tests the null hypothesis that a unit root is present in a time series sample.
 * The alternative hypothesis is different depending on which version of the test is used,
 * but is usually stationarity or trend-stationarity.
 * It is an augmented version of the Dickey–Fuller test for a larger and more complicated set of time series models.
 *
 * The ADF test fits the regression:
 * Δy_t = α + βt + γy_{t-1} + δ₁Δy_{t-1} + ... + δₚΔy_{t-p} + ε_t
 *
 * The null hypothesis is H0: γ = 0 (unit root present)
 * The alternative is H1: γ < 0 (series is stationary)
 *
 * Reference: R's tseries::adf.test() and urca::ur.df()
 */
@CompileStatic
class Adf {

  /**
   * Performs the Augmented Dickey-Fuller test for a unit root.
   *
   * @param data The time series data
   * @param lags The number of lags to include (default: 0 for simple Dickey-Fuller)
   * @param type The type of test: "none" (no intercept/trend), "drift" (intercept only), or "trend" (intercept and trend)
   * @return AdfResult containing test statistic and conclusion
   */
  static AdfResult test(List<? extends Number> data, int lags = 0, String type = "drift") {
    validateInput(data, lags, type)

    int n = data.size()
    double[] y = data.collect { it.doubleValue() } as double[]

    // Check for constant series
    double yMin = y.min()
    double yMax = y.max()
    if (Math.abs(yMax - yMin) < 1e-10) {
      throw new IllegalArgumentException("Data has no variation (constant series). Cannot perform ADF test.")
    }

    // Calculate first differences
    double[] dy = new double[n - 1]
    for (int i = 1; i < n; i++) {
      dy[i - 1] = y[i] - y[i - 1]
    }

    // Prepare the regression data
    // We need at least lags + 2 observations after differencing
    int nObs = n - lags - 1
    if (nObs < 10) {
      throw new IllegalArgumentException("Insufficient observations after accounting for lags. Need at least ${lags + 11}, got ${n}")
    }

    // Build the design matrix and response vector
    double[] response = new double[nObs]
    double[][] predictors = buildDesignMatrix(y, dy, lags, type, nObs, n)

    // Fill the response vector (Δy_t)
    for (int i = 0; i < nObs; i++) {
      response[i] = dy[lags + i]
    }

    // Check for zero variance in response
    double responseMean = 0.0
    for (double r : response) {
      responseMean += r
    }
    responseMean /= nObs

    double responseVar = 0.0
    for (double r : response) {
      responseVar += (r - responseMean) * (r - responseMean)
    }
    if (responseVar < 1e-10) {
      throw new IllegalArgumentException("First differences have no variation. Cannot perform ADF test.")
    }

    try {
      // Perform OLS regression
      OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression()
      regression.setNoIntercept(true) // We manually add intercept to design matrix
      regression.newSampleData(response, predictors)

      // Get the coefficient for y_{t-1} (this is the γ coefficient)
      double[] coefficients = regression.estimateRegressionParameters()
      double[] standardErrors = regression.estimateRegressionParametersStandardErrors()

      // The coefficient index for y_{t-1} depends on the type
      int gammaIndex = getGammaIndex(type)

      double gammaCoef = coefficients[gammaIndex]
      double gammaSE = standardErrors[gammaIndex]

      // Calculate the t-statistic
      double tStatistic = gammaCoef / gammaSE

      // Determine stationarity based on critical values (approximate)
      // These are approximate critical values for the 5% level
      double criticalValue = getCriticalValue(type, nObs)

      return new AdfResult(
        statistic: tStatistic,
        lag: lags,
        type: type,
        sampleSize: n,
        effectiveSize: nObs,
        gammaCoefficient: gammaCoef,
        gammaStandardError: gammaSE,
        criticalValue: criticalValue
      )
    } catch (Exception e) {
      throw new RuntimeException(
        "Failed to perform ADF test. This may be due to perfect collinearity in the data " +
        "(e.g., perfect linear trend or constant differences). " +
        "Try using a different lag order or test type. Error: ${e.message}", e
      )
    }
  }

  /**
   * Builds the design matrix for the ADF regression.
   * Returns a matrix where each row corresponds to one observation.
   */
  private static double[][] buildDesignMatrix(double[] y, double[] dy, int lags, String type, int nObs, int n) {
    // Determine number of predictors
    int nPredictors = 1 // y_{t-1}
    if (type == "drift" || type == "trend") nPredictors++ // constant
    if (type == "trend") nPredictors++ // time trend
    nPredictors += lags // lagged differences

    double[][] X = new double[nObs][nPredictors]

    for (int i = 0; i < nObs; i++) {
      int col = 0

      // Add constant if drift or trend
      if (type == "drift" || type == "trend") {
        X[i][col++] = 1.0
      }

      // Add time trend if trend (centered to reduce collinearity)
      if (type == "trend") {
        // Center the time trend around its mean
        double t = (lags + i + 1) - (n / 2.0)
        X[i][col++] = t
      }

      // Add y_{t-1} (the level at time t-1)
      // When i=0, we want y at time lags (since response is dy at time lags+1)
      // So y_{t-1} for response dy[lags] is y[lags]
      X[i][col++] = y[lags + i]

      // Add lagged differences Δy_{t-1}, ..., Δy_{t-p}
      for (int lag = 1; lag <= lags; lag++) {
        // For i=0, lag=1: we want Δy_{t-1} where t is lags+1
        // So we want dy[lags+1-1] = dy[lags]... wait, that's wrong
        // Actually: response[i] = dy[lags + i]
        // This is Δy at time t = lags + i + 1 (since dy[k] = y[k+1] - y[k])
        // We want Δy_{t-lag} = dy[lags + i + 1 - 1 - lag] = dy[lags + i - lag]
        X[i][col++] = dy[lags + i - lag]
      }
    }

    return X
  }

  /**
   * Gets the index of the γ coefficient (y_{t-1}) in the regression parameters.
   */
  private static int getGammaIndex(String type) {
    if (type == "none") {
      return 0 // y_{t-1} is first
    } else if (type == "drift") {
      return 1 // constant, then y_{t-1}
    } else { // trend
      return 2 // constant, trend, then y_{t-1}
    }
  }

  /**
   * Gets approximate critical values for the ADF test at the 5% significance level.
   * These are rough approximations based on MacKinnon (1996).
   */
  private static double getCriticalValue(String type, int n) {
    // Approximate 5% critical values (more accurate for larger samples)
    if (type == "none") {
      return -1.95 // No intercept, no trend
    } else if (type == "drift") {
      // Use size-adjusted critical value
      if (n < 25) {
        return -3.00
      } else if (n < 50) {
        return -2.93
      } else if (n < 100) {
        return -2.89
      } else {
        return -2.86
      }
    } else { // trend
      // Use size-adjusted critical value
      if (n < 25) {
        return -3.60
      } else if (n < 50) {
        return -3.50
      } else if (n < 100) {
        return -3.45
      } else {
        return -3.41
      }
    }
  }

  private static void validateInput(List<? extends Number> data, int lags, String type) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null")
    }
    if (data.isEmpty()) {
      throw new IllegalArgumentException("Data cannot be empty")
    }
    if (data.size() < 12) {
      throw new IllegalArgumentException("ADF test requires at least 12 observations, got ${data.size()}")
    }
    if (lags < 0) {
      throw new IllegalArgumentException("Lags must be non-negative, got ${lags}")
    }
    if (lags > (data.size() / 3)) {
      throw new IllegalArgumentException("Too many lags (${lags}) for sample size (${data.size()}). Maximum recommended: ${data.size() / 3 as int}")
    }
    if (!(type in ['none', 'drift', 'trend'])) {
      throw new IllegalArgumentException("Type must be 'none', 'drift', or 'trend', got: ${type}")
    }
    // Check for non-null values
    for (Number value : data) {
      if (value == null) {
        throw new IllegalArgumentException("Data contains null values")
      }
    }
  }

  /**
   * Result class for the Augmented Dickey-Fuller test.
   */
  @CompileStatic
  static class AdfResult {
    /** The ADF test statistic (t-statistic for γ coefficient) */
    double statistic

    /** The number of lags used */
    int lag

    /** The type of test performed */
    String type

    /** The original sample size */
    int sampleSize

    /** The effective sample size after accounting for lags */
    int effectiveSize

    /** The estimated γ coefficient on y_{t-1} */
    double gammaCoefficient

    /** The standard error of the γ coefficient */
    double gammaStandardError

    /** The approximate 5% critical value */
    double criticalValue

    /**
     * Interprets the ADF test result.
     *
     * @return A string describing whether the series appears stationary
     */
    String interpret() {
      if (statistic < criticalValue) {
        return "Reject H0: Series appears to be stationary (no unit root)"
      } else {
        return "Fail to reject H0: Series appears to have a unit root (non-stationary)"
      }
    }

    /**
     * Evaluates the test result.
     *
     * @return A description of the test result with interpretation
     */
    String evaluate() {
      String conclusion = statistic < criticalValue ?
        "stationary (no unit root)" :
        "non-stationary (unit root present)"

      return String.format("ADF statistic: %.4f (critical value: %.2f at 5%% level)\nγ coefficient: %.6f (SE: %.6f)\nConclusion: Series appears %s",
                           statistic, criticalValue, gammaCoefficient, gammaStandardError, conclusion)
    }

    @Override
    String toString() {
      return """Augmented Dickey-Fuller Test
  Type: ${type}
  Lags: ${lag}
  Sample size: ${sampleSize} (effective: ${effectiveSize})
  ADF statistic: ${String.format("%.4f", statistic)}
  Critical value (5%%): ${String.format("%.2f", criticalValue)}
  γ coefficient: ${String.format("%.6f", gammaCoefficient)}
  Standard error: ${String.format("%.6f", gammaStandardError)}

  ${interpret()}"""
    }
  }
}
