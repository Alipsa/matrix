package se.alipsa.matrix.stats.timeseries

import groovy.transform.CompileStatic

/**
 * The Durbin–Watson statistic is a test statistic used to detect the presence of autocorrelation at lag 1 in
 * the residuals (prediction errors) from a regression analysis. It is named after James Durbin and Geoffrey Watson.
 * The small sample distribution of this ratio was derived by John von Neumann (von Neumann, 1941).
 * Durbin and Watson (1950, 1951) applied this statistic to the residuals from least squares regressions,
 * and developed bounds tests for the null hypothesis that the errors are serially uncorrelated against the
 * alternative that they follow a first order autoregressive process.
 *
 * The test statistic is:
 * DW = Σ(e_t - e_{t-1})² / Σe_t²
 *
 * where e_t are the residuals.
 *
 * The test statistic ranges from 0 to 4:
 * - DW ≈ 2: No autocorrelation
 * - DW < 2: Positive autocorrelation
 * - DW > 2: Negative autocorrelation
 *
 * Reference: R's lmtest::dwtest()
 */
@CompileStatic
class DurbinWatson {

  /**
   * Performs the Durbin-Watson test on residuals.
   *
   * @param residuals The residuals from a regression analysis
   * @return DurbinWatsonResult containing test statistic and interpretation
   * @throws IllegalArgumentException if residuals is null, empty, or has fewer than 3 observations
   */
  static DurbinWatsonResult test(List<? extends Number> residuals) {
    validateInput(residuals)

    int n = residuals.size()
    double[] res = residuals.collect { it.doubleValue() } as double[]

    // Calculate the Durbin-Watson statistic
    double numerator = 0.0
    for (int i = 1; i < n; i++) {
      double diff = res[i] - res[i - 1]
      numerator += diff * diff
    }

    double denominator = 0.0
    for (int i = 0; i < n; i++) {
      denominator += res[i] * res[i]
    }

    double dwStatistic = numerator / denominator

    // Calculate the first-order autocorrelation coefficient
    // ρ ≈ 1 - (DW / 2)
    double autocorrelation = 1.0 - (dwStatistic / 2.0)

    return new DurbinWatsonResult(
      statistic: dwStatistic,
      autocorrelation: autocorrelation,
      sampleSize: n
    )
  }

  /**
   * Performs the Durbin-Watson test with alternative hypothesis specification.
   *
   * @param residuals The residuals from a regression analysis
   * @param alternative The alternative hypothesis: "two.sided", "greater" (negative autocorrelation), or "less" (positive autocorrelation)
   * @return DurbinWatsonResult containing test statistic and interpretation
   */
  static DurbinWatsonResult test(List<? extends Number> residuals, String alternative) {
    validateAlternative(alternative)
    DurbinWatsonResult result = test(residuals)
    result.alternative = alternative
    return result
  }

  private static void validateInput(List<? extends Number> residuals) {
    if (residuals == null) {
      throw new IllegalArgumentException("Residuals cannot be null")
    }
    if (residuals.isEmpty()) {
      throw new IllegalArgumentException("Residuals cannot be empty")
    }
    if (residuals.size() < 3) {
      throw new IllegalArgumentException("Durbin-Watson test requires at least 3 observations, got ${residuals.size()}")
    }
    // Check for non-null values
    for (Number value : residuals) {
      if (value == null) {
        throw new IllegalArgumentException("Residuals contains null values")
      }
    }
  }

  private static void validateAlternative(String alternative) {
    if (!(alternative in ['two.sided', 'greater', 'less'])) {
      throw new IllegalArgumentException("Alternative must be 'two.sided', 'greater', or 'less', got: ${alternative}")
    }
  }

  /**
   * Result class for the Durbin-Watson test.
   */
  @CompileStatic
  static class DurbinWatsonResult {
    /** The Durbin-Watson test statistic */
    double statistic

    /** The estimated first-order autocorrelation coefficient */
    double autocorrelation

    /** The sample size */
    int sampleSize

    /** The alternative hypothesis */
    String alternative = "two.sided"

    /**
     * Interprets the Durbin-Watson statistic.
     *
     * @return A string describing the autocorrelation pattern
     */
    String interpret() {
      if (statistic < 1.5) {
        return "Strong positive autocorrelation (DW < 1.5)"
      } else if (statistic < 1.8) {
        return "Moderate positive autocorrelation (1.5 ≤ DW < 1.8)"
      } else if (statistic < 2.2) {
        return "No significant autocorrelation (1.8 ≤ DW ≤ 2.2)"
      } else if (statistic < 2.5) {
        return "Moderate negative autocorrelation (2.2 < DW ≤ 2.5)"
      } else {
        return "Strong negative autocorrelation (DW > 2.5)"
      }
    }

    /**
     * Evaluates the test result.
     *
     * @return A description of the test result with interpretation
     */
    String evaluate() {
      String autocorrType
      if (autocorrelation > 0.3) {
        autocorrType = "positive"
      } else if (autocorrelation < -0.3) {
        autocorrType = "negative"
      } else {
        autocorrType = "negligible"
      }

      return String.format("Durbin-Watson statistic: %.4f (ρ ≈ %.4f, %s autocorrelation)\n%s",
                           statistic, autocorrelation, autocorrType, interpret())
    }

    @Override
    String toString() {
      return """Durbin-Watson Test
  Sample size: ${sampleSize}
  Test statistic (DW): ${String.format("%.4f", statistic)}
  Autocorrelation (ρ): ${String.format("%.4f", autocorrelation)}
  Alternative: ${alternative}

  ${interpret()}"""
    }
  }
}
