package se.alipsa.matrix.stats.timeseries

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.NormalDistribution

/**
 * The Turning Point test is a non-parametric statistical test for randomness (independence) in a time series
 * based on counting local maxima and minima. It is particularly useful for detecting cyclicity and
 * non-random patterns in sequential data.
 *
 * <p>A turning point occurs at position i when the value differs from both neighbors in the same direction:
 * either a local maximum (peak) where x[i-1] < x[i] > x[i+1], or a local minimum (trough) where
 * x[i-1] > x[i] < x[i+1]. Random sequences produce a predictable number of turning points on average.</p>
 *
 * <h3>When to Use</h3>
 * <ul>
 * <li>To test if a sequence is randomly ordered versus exhibiting systematic patterns</li>
 * <li>For detecting cyclical behavior in time series (business cycles, seasonal patterns)</li>
 * <li>As a simple preliminary randomness test before more complex analysis</li>
 * <li>When you want a distribution-free test (no normality assumption)</li>
 * <li>To identify over-smoothing (too few turning points) or excessive volatility (too many)</li>
 * </ul>
 *
 * <h3>Hypotheses</h3>
 * <ul>
 * <li><strong>H0 (null):</strong> The series is random (independent observations)</li>
 * <li><strong>H1 (alternative):</strong> The series is not random (exhibits trend or cyclicity)</li>
 * </ul>
 *
 * <h3>Test Interpretation</h3>
 * <ul>
 * <li><strong>Too few turning points:</strong> Suggests a trend or autocorrelation (smooth series)</li>
 * <li><strong>Too many turning points:</strong> Suggests excessive cyclicity or mean reversion</li>
 * <li><strong>Expected number:</strong> Random series have approximately 2(n-2)/3 turning points</li>
 * </ul>
 *
 * <h3>Test Statistics</h3>
 * <p>For a sequence of n observations:</p>
 * <ul>
 * <li>E[T] = 2(n - 2) / 3 (expected turning points under randomness)</li>
 * <li>Var[T] = (16n - 29) / 90 (variance under randomness)</li>
 * <li>Z = (T - E[T]) / √Var[T] ~ N(0,1) for large n</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Test a series for randomness
 * def data = [1, 3, 2, 4, 3, 5, 4, 6, 5, 7, 6, 8] as double[]
 * def result = TurningPoint.test(data)
 * println result.interpret()
 *
 * // Access detailed results
 * println "Peaks: ${result.peaks}, Troughs: ${result.troughs}"
 * println "Total turning points: ${result.turningPoints}"
 * println "Expected: ${result.expectedTurningPoints}"
 * </pre>
 *
 * <h3>Limitations</h3>
 * <ul>
 * <li>As noted by Kendall & Stuart: "reasonable for cyclicity but poor as a test against trend"</li>
 * <li>Less powerful than runs tests for detecting trends</li>
 * <li>Sensitive to tied values (equal consecutive observations)</li>
 * </ul>
 *
 * <h3>References</h3>
 * <ul>
 * <li>Kendall, M. G., & Stuart, A. (1976). The Advanced Theory of Statistics, Vol. 3:
 * Design and Analysis, and Time-Series, 4th Edition, Chapter 45.</li>
 * <li>Brockwell, P. J., & Davis, R. A. (2016). Introduction to Time Series and Forecasting,
 * 3rd Edition, Springer.</li>
 * <li>Mood, A. M. (1940). "The Distribution Theory of Runs", Annals of Mathematical Statistics, 11(4), 367-392.</li>
 * </ul>
 */
@CompileStatic
class TurningPoint {

  /**
   * Performs the Turning Point test for randomness.
   *
   * @param data The time series data
   * @return TurningPointResult containing test statistic, p-value, and conclusion
   */
  static TurningPointResult test(double[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null")
    }

    int n = data.length

    if (n < 3) {
      throw new IllegalArgumentException("Need at least 3 observations for turning point test (got ${n})")
    }

    // Count turning points
    int turningPoints = 0
    int peaks = 0
    int troughs = 0

    for (int i = 1; i < n - 1; i++) {
      boolean isPeak = data[i - 1] < data[i] && data[i] > data[i + 1]
      boolean isTrough = data[i - 1] > data[i] && data[i] < data[i + 1]

      if (isPeak) {
        turningPoints++
        peaks++
      } else if (isTrough) {
        turningPoints++
        troughs++
      }
    }

    // Calculate expected value and variance under H0 (randomness)
    double expectedTurningPoints = 2.0 * (n - 2) / 3.0
    double variance = (16.0 * n - 29.0) / 90.0
    double stdDev = Math.sqrt(variance)

    // Calculate test statistic (standardized)
    double zStatistic = (turningPoints - expectedTurningPoints) / stdDev

    // Calculate p-value (two-sided test)
    NormalDistribution normal = new NormalDistribution(0, 1)
    double pValue = 2.0 * (1.0 - normal.cumulativeProbability(Math.abs(zStatistic)))

    return new TurningPointResult(
      statistic: zStatistic,
      pValue: pValue,
      turningPoints: turningPoints,
      expectedTurningPoints: expectedTurningPoints,
      variance: variance,
      peaks: peaks,
      troughs: troughs,
      sampleSize: n,
      possibleTurningPoints: n - 2
    )
  }

  /**
   * Performs the Turning Point test with List input.
   */
  static TurningPointResult test(List<? extends Number> data) {
    double[] array = data.collect { it.doubleValue() } as double[]
    return test(array)
  }

  /**
   * Result class for Turning Point test.
   */
  @CompileStatic
  static class TurningPointResult {
    /** The Z-statistic (standardized test statistic) */
    double statistic

    /** The p-value */
    double pValue

    /** Observed number of turning points */
    int turningPoints

    /** Expected number of turning points under H0 */
    double expectedTurningPoints

    /** Variance of turning points under H0 */
    double variance

    /** Number of peaks (local maxima) */
    int peaks

    /** Number of troughs (local minima) */
    int troughs

    /** Sample size */
    int sampleSize

    /** Maximum possible turning points (n - 2) */
    int possibleTurningPoints

    /**
     * Interprets the test result.
     *
     * @param alpha Significance level (default 0.05)
     * @return Interpretation string
     */
    String interpret(double alpha = 0.05) {
      if (pValue < alpha) {
        String direction = turningPoints > expectedTurningPoints ? "more" : "fewer"
        return "Reject H0: Data shows ${direction} turning points than expected under randomness (Z = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      } else {
        return "Fail to reject H0: Data is consistent with randomness (Z = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      }
    }

    /**
     * Evaluates the test result with detailed information.
     */
    String evaluate(double alpha = 0.05) {
      String conclusion = pValue < alpha ? "data is not random" : "data is consistent with randomness"
      String direction = turningPoints > expectedTurningPoints ? "cyclicity" : "trend"

      return String.format(
        "Turning Point test:\\n" +
        "Sample size: %d\\n" +
        "Turning points observed: %d (peaks: %d, troughs: %d)\\n" +
        "Expected under H0: %.4f\\n" +
        "Variance: %.4f\\n" +
        "Z-statistic: %.4f\\n" +
        "p-value: %.4f\\n" +
        "Conclusion: %s at %.0f%% significance level\\n" +
        "Note: %s turning points may suggest %s",
        sampleSize, turningPoints, peaks, troughs,
        expectedTurningPoints, variance,
        statistic, pValue,
        conclusion, alpha * 100,
        turningPoints > expectedTurningPoints ? "More" : "Fewer",
        direction
      )
    }

    @Override
    String toString() {
      return """Turning Point Test
  Sample size: ${sampleSize}
  Turning points: ${turningPoints} (peaks: ${peaks}, troughs: ${troughs})
  Expected: ${String.format('%.4f', expectedTurningPoints)}
  Variance: ${String.format('%.4f', variance)}
  Z-statistic: ${String.format('%.4f', statistic)}
  p-value: ${String.format('%.4f', pValue)}

  ${interpret()}"""
    }
  }
}
