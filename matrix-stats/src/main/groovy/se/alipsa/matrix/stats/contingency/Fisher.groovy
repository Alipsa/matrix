package se.alipsa.matrix.stats.contingency

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.HypergeometricDistribution
import se.alipsa.matrix.core.Matrix

/**
 * Fisher's exact test is a statistical significance test used in the analysis of contingency tables.
 * Although in practice it is employed when sample sizes are small, it is valid for all sample sizes.
 * It is named after its inventor, Ronald Fisher, and is one of a class of exact tests,
 * so called because the significance of the deviation from a null hypothesis (e.g., p-value) can be calculated exactly,
 * rather than relying on an approximation that becomes exact in the limit as the sample size grows to infinity,
 * as with many other statistical tests.
 *
 * <p>This implementation uses the hypergeometric distribution to calculate exact p-values for 2×2 contingency tables.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Test data: [[12, 5], [9, 11]]
 * def result = Fisher.test([[12, 5], [9, 11]])
 * println "p-value: ${result.pValue}"
 * println "odds ratio: ${result.oddsRatio}"
 * </pre>
 */
@CompileStatic
class Fisher {

  /**
   * Performs Fisher's exact test on a 2×2 contingency table.
   *
   * @param table A 2×2 contingency table as a List of Lists: [[a, b], [c, d]]
   * @param alternative The alternative hypothesis: "two.sided" (default), "greater", or "less"
   * @return FisherResult containing p-value, odds ratio, and confidence interval
   * @throws IllegalArgumentException if table is not 2×2 or contains negative values
   */
  static FisherResult test(List<List<Integer>> table, String alternative = "two.sided") {
    validateTable(table)

    int a = table[0][0]
    int b = table[0][1]
    int c = table[1][0]
    int d = table[1][1]

    return calculateFisherTest(a, b, c, d, alternative)
  }

  /**
   * Performs Fisher's exact test on a Matrix (2×2 contingency table).
   *
   * @param table A 2×2 Matrix
   * @param alternative The alternative hypothesis: "two.sided" (default), "greater", or "less"
   * @return FisherResult containing p-value, odds ratio, and confidence interval
   * @throws IllegalArgumentException if table is not 2×2 or contains negative values
   */
  static FisherResult test(Matrix table, String alternative = "two.sided") {
    if (table.rowCount() != 2 || table.columnCount() != 2) {
      throw new IllegalArgumentException("Fisher's exact test requires a 2×2 table (got ${table.rowCount()}×${table.columnCount()})")
    }

    int a = table.get(0, 0) as int
    int b = table.get(0, 1) as int
    int c = table.get(1, 0) as int
    int d = table.get(1, 1) as int

    return calculateFisherTest(a, b, c, d, alternative)
  }

  private static void validateTable(List<List<Integer>> table) {
    if (table == null || table.size() != 2) {
      throw new IllegalArgumentException("Fisher's exact test requires a 2×2 table (got ${table?.size() ?: 0} rows)")
    }
    if (table[0].size() != 2 || table[1].size() != 2) {
      throw new IllegalArgumentException("Fisher's exact test requires a 2×2 table (got ${table[0]?.size()}×${table[1]?.size()} columns)")
    }
    for (List<Integer> row : table) {
      for (Integer val : row) {
        if (val == null || val < 0) {
          throw new IllegalArgumentException("Table values must be non-negative integers")
        }
      }
    }
  }

  private static FisherResult calculateFisherTest(int a, int b, int c, int d, String alternative) {
    int n = a + b + c + d
    int rowSum1 = a + b
    int rowSum2 = c + d
    int colSum1 = a + c
    int colSum2 = b + d

    // Calculate odds ratio
    double oddsRatio = calculateOddsRatio(a, b, c, d)

    // Calculate p-value using hypergeometric distribution
    // The hypergeometric distribution models the probability of 'a' successes
    // in a sample of size rowSum1, drawn from a population of size n
    // with colSum1 successes
    HypergeometricDistribution dist = new HypergeometricDistribution(n, colSum1, rowSum1)

    double pValue
    switch (alternative.toLowerCase()) {
      case "greater":
        // P(X >= a)
        pValue = dist.upperCumulativeProbability(a)
        break
      case "less":
        // P(X <= a)
        pValue = dist.cumulativeProbability(a)
        break
      case "two.sided":
      default:
        // Two-sided: sum probabilities of tables as or more extreme
        pValue = calculateTwoSidedPValue(dist, a, rowSum1)
        break
    }

    // Calculate confidence interval for odds ratio (95% by default)
    double[] confInt = calculateConfidenceInterval(a, b, c, d, 0.95)

    return new FisherResult(
      pValue: pValue,
      oddsRatio: oddsRatio,
      confidenceInterval: confInt,
      alternative: alternative
    )
  }

  private static double calculateOddsRatio(int a, int b, int c, int d) {
    if (b == 0 || c == 0) {
      // Avoid division by zero - add 0.5 to all cells (Haldane-Anscombe correction)
      return ((a + 0.5) * (d + 0.5)) / ((b + 0.5) * (c + 0.5))
    }
    return (a * d) / (b * c) as double
  }

  private static double calculateTwoSidedPValue(HypergeometricDistribution dist, int observed, int sampleSize) {
    double observedProb = dist.probability(observed)
    double pValue = 0.0

    // Sum probabilities for all tables with probability <= observed probability
    for (int k = dist.getSupportLowerBound(); k <= dist.getSupportUpperBound(); k++) {
      double prob = dist.probability(k)
      if (prob <= observedProb + 1e-10) {  // Small epsilon for floating point comparison
        pValue += prob
      }
    }

    return Math.min(pValue, 1.0)  // Ensure p-value doesn't exceed 1 due to rounding
  }

  private static double[] calculateConfidenceInterval(int a, int b, int c, int d, double confidenceLevel) {
    // Calculate confidence interval for odds ratio using the hypergeometric distribution
    // This is an approximation - exact calculation is complex

    if (a == 0 || b == 0 || c == 0 || d == 0) {
      // Use log odds ratio method for zero cells
      double alpha = 1.0 - confidenceLevel
      double logOddsRatio = Math.log(calculateOddsRatio(a, b, c, d))
      double se = Math.sqrt(1.0/(a+0.5) + 1.0/(b+0.5) + 1.0/(c+0.5) + 1.0/(d+0.5))
      double z = getZScore(alpha / 2.0)

      double lower = Math.exp(logOddsRatio - z * se)
      double upper = Math.exp(logOddsRatio + z * se)
      return [lower, upper] as double[]
    }

    // Normal approximation for non-zero cells
    double logOddsRatio = Math.log((a * d) / (b * c) as double)
    double se = Math.sqrt(1.0/a + 1.0/b + 1.0/c + 1.0/d)
    double alpha = 1.0 - confidenceLevel
    double z = getZScore(alpha / 2.0)

    double lower = Math.exp(logOddsRatio - z * se)
    double upper = Math.exp(logOddsRatio + z * se)

    return [lower, upper] as double[]
  }

  private static double getZScore(double alpha) {
    // Approximate z-score for common confidence levels
    // For 95% CI: alpha/2 = 0.025, z ≈ 1.96
    if (Math.abs(alpha - 0.025) < 0.001) return 1.96
    if (Math.abs(alpha - 0.05) < 0.001) return 1.645
    if (Math.abs(alpha - 0.005) < 0.001) return 2.576

    // Default approximation
    return 1.96
  }

  /**
   * Result class for Fisher's exact test.
   */
  static class FisherResult {
    /** The p-value of the test */
    Double pValue

    /** The estimated odds ratio */
    Double oddsRatio

    /** 95% confidence interval for the odds ratio [lower, upper] */
    double[] confidenceInterval

    /** The alternative hypothesis used */
    String alternative

    /**
     * Evaluates whether to reject the null hypothesis at the given significance level.
     *
     * @param alpha Significance level (default 0.05)
     * @return true if null hypothesis should be rejected (p-value < alpha)
     */
    boolean evaluate(double alpha = 0.05) {
      return pValue < alpha
    }

    @Override
    String toString() {
      return """Fisher's Exact Test Result:
  p-value: ${pValue}
  odds ratio: ${oddsRatio}
  ${confidenceInterval ? sprintf('95%% CI: [%.4f, %.4f]', confidenceInterval[0], confidenceInterval[1]) : '95% CI: N/A'}
  alternative: ${alternative}"""
    }
  }
}
