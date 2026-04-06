package se.alipsa.matrix.stats.timeseries

import se.alipsa.matrix.stats.distribution.FDistribution
import se.alipsa.matrix.stats.util.NumericConversion

/**
 * The Chow test, proposed by econometrician Gregory Chow in 1960, tests whether the coefficients
 * in two linear regressions on different subsets of data are equal. It is the standard test for
 * detecting structural breaks in time series when the break point is known a priori.
 *
 * <p>In econometrics and time series analysis, structural breaks represent fundamental changes in the
 * data-generating process, such as policy changes, regime shifts, economic crises, or technological innovations.
 * The Chow test determines whether such events significantly altered the relationship between variables.</p>
 *
 * <h3>When to Use</h3>
 * <ul>
 * <li>When testing for structural change at a known point in time (policy change, crisis event)</li>
 * <li>To validate whether a regression model is stable across different time periods</li>
 * <li>Before pooling data from different periods in econometric analysis</li>
 * <li>When you suspect regime changes in economic or financial relationships</li>
 * <li>To test stability of estimated relationships across subsamples</li>
 * </ul>
 *
 * <h3>Hypotheses</h3>
 * <ul>
 * <li><strong>H0 (null):</strong> No structural break; coefficients are equal in both periods (β₁ = β₂)</li>
 * <li><strong>H1 (alternative):</strong> Structural break exists; coefficients differ across periods (β₁ ≠ β₂)</li>
 * </ul>
 *
 * <h3>Test Statistic</h3>
 * <p>F = [(RSS_full - (RSS_1 + RSS_2)) / k] / [(RSS_1 + RSS_2) / (n - 2k)]</p>
 * <p>where RSS = residual sum of squares, k = number of parameters, n = total observations</p>
 * <p>Under H0, the F-statistic follows an F(k, n-2k) distribution.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Test for structural break at a known point (e.g., policy change)
 * def y = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10] as double[]
 * def X = [[1, 1], [1, 2], [1, 3], [1, 4], [1, 5],
 *          [1, 6], [1, 7], [1, 8], [1, 9], [1, 10]] as double[][]
 *
 * // Test for break at observation 5 (after index 4)
 * def result = Chow.test(y, X, 5)
 * println result.interpret()
 *
 * // Detailed evaluation
 * println result.evaluate(0.05)
 * </pre>
 *
 * <h3>References</h3>
 * <ul>
 * <li>Chow, G. C. (1960). "Tests of Equality Between Sets of Coefficients in Two Linear Regressions",
 * Econometrica, 28(3), 591-605.</li>
 * <li>Greene, W. H. (2018). Econometric Analysis, 8th Edition, Chapter 6.</li>
 * <li>R's strucchange package: sctest() function</li>
 * <li>Stata's chow command</li>
 * </ul>
 */
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral', 'ParameterName', 'VariableName'])
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
    double[] betaFull = TimeSeriesUtils.fitOLS(y, X)
    double rssFull = TimeSeriesUtils.calculateRSS(y, X, betaFull)

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
    double[] beta1 = TimeSeriesUtils.fitOLS(y1, X1)
    double rss1 = TimeSeriesUtils.calculateRSS(y1, X1, beta1)

    double[] beta2 = TimeSeriesUtils.fitOLS(y2, X2)
    double rss2 = TimeSeriesUtils.calculateRSS(y2, X2, beta2)

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
      statistic: BigDecimal.valueOf(fStatistic),
      pValue: BigDecimal.valueOf(pValue),
      df1: k,
      df2: n - 2 * k,
      breakPoint: breakPoint,
      rssFull: BigDecimal.valueOf(rssFull),
      rss1: BigDecimal.valueOf(rss1),
      rss2: BigDecimal.valueOf(rss2),
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
   * Result class for Chow test.
   */
  static class ChowResult {
    /** The Chow F-statistic */
    BigDecimal statistic

    /** The p-value */
    BigDecimal pValue

    /** Degrees of freedom (numerator) */
    int df1

    /** Degrees of freedom (denominator) */
    int df2

    /** Break point index */
    int breakPoint

    /** RSS for full model */
    BigDecimal rssFull

    /** RSS for first sub-model */
    BigDecimal rss1

    /** RSS for second sub-model */
    BigDecimal rss2

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
    String interpret(Number alpha = 0.05) {
      BigDecimal alphaValue = NumericConversion.toAlpha(alpha)
      if (pValue < alphaValue) {
        return "Reject H0: Structural break detected at observation ${breakPoint} (F = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      } else {
        return "Fail to reject H0: No evidence of structural break at observation ${breakPoint} (F = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      }
    }

    /**
     * Evaluates the test result with detailed information.
     */
    String evaluate(Number alpha = 0.05) {
      BigDecimal alphaValue = NumericConversion.toAlpha(alpha)
      String conclusion = pValue < alphaValue ? "structural break present" : "no structural break detected"

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
        conclusion, (alphaValue * 100) as double
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
