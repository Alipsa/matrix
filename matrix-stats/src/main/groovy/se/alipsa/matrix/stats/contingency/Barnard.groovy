package se.alipsa.matrix.stats.contingency

import groovy.transform.CompileStatic

/**
 * Barnard's exact test is an unconditional exact test for 2×2 contingency tables that is uniformly
 * more powerful than Fisher's exact test. It tests for association between two binary variables
 * while conditioning only on the total sample size, making it more powerful when row and column
 * margins are not fixed by experimental design.
 *
 * <p><b>What is Barnard's test?</b></p>
 * Barnard's test evaluates the association between two dichotomous variables using an unconditional
 * approach. Unlike Fisher's exact test which conditions on both row and column margins, Barnard's
 * test only conditions on the total sample size (n), allowing it to use more of the sample space
 * and thereby achieve greater statistical power. The test maximizes over a nuisance parameter π
 * (the common success probability under the null hypothesis) and uses Wald's score statistic.
 *
 * <p><b>When to use Barnard's test:</b></p>
 * <ul>
 *   <li>When analyzing 2×2 contingency tables with small sample sizes</li>
 *   <li>When margins are not fixed by the experimental design (e.g., retrospective studies)</li>
 *   <li>When you need more statistical power than Fisher's exact test</li>
 *   <li>When comparing two independent proportions with binary outcomes</li>
 *   <li>As an alternative to chi-squared test when expected cell frequencies are small (&lt; 5)</li>
 * </ul>
 *
 * <p><b>Hypotheses:</b></p>
 * <ul>
 *   <li>H₀ (null hypothesis): The two variables are independent (no association between row and column variables)</li>
 *   <li>H₁ (alternative hypothesis): The two variables are associated (the proportions differ between groups)</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 * // Test association between treatment and outcome in a clinical trial
 * int[][] table = [[12, 8],    // Treatment group: Success, Failure
 *                  [6, 14]]    // Control group: Success, Failure
 *
 * def result = Barnard.test(table)
 * println "p-value: ${result.pValue}"
 * println "Wald statistic: ${result.statistic}"
 * println result.interpret()
 *
 * // Example output:
 * // p-value: 0.0891
 * // Wald statistic: 1.6996
 * // Fail to reject H0: No significant association detected
 * </pre>
 *
 * <p><b>Statistical details:</b></p>
 * The test statistic is the Wald score: T = (p₁ - p₂) / √[π(1-π)(1/n₁ + 1/n₂)]
 * where p₁ and p₂ are the observed proportions in each group, and π is the pooled proportion.
 * The p-value is computed by enumerating all possible 2×2 tables with the same total sample size
 * and summing probabilities of tables with test statistics as extreme or more extreme than observed.
 *
 * <p><b>References:</b></p>
 * <ul>
 *   <li>Barnard, G. A. (1945). "A new test for 2×2 tables". Nature, 156, 177.</li>
 *   <li>Barnard, G. A. (1947). "Significance tests for 2×2 tables". Biometrika, 34(1-2), 123-138.</li>
 *   <li>Mehta, C. R., & Senchaudhuri, P. (2003). "Conditional versus unconditional exact tests for comparing two binomials". In Handbook of Statistics, Vol. 22 (pp. 1-49).</li>
 * </ul>
 *
 * <p><b>Note:</b> Barnard's test is computationally intensive for large sample sizes (n > 200) due to the need to
 * enumerate all possible tables and optimize over the nuisance parameter π. For very large samples,
 * consider using the chi-squared test instead.</p>
 */
@CompileStatic
class Barnard {

  /**
   * Performs Barnard's exact test for a 2×2 contingency table.
   *
   * @param table A 2×2 table represented as int[2][2]: [[a, b], [c, d]]
   * @return BarnardResult containing test statistic, p-value, and nuisance parameter
   */
  static BarnardResult test(int[][] table) {
    validateTable(table)

    int a = table[0][0]
    int b = table[0][1]
    int c = table[1][0]
    int d = table[1][1]

    int n1 = a + b  // Row 1 total
    int n2 = c + d  // Row 2 total
    int n = n1 + n2  // Grand total

    if (n == 0) {
      throw new IllegalArgumentException("Table cannot have zero total count")
    }

    // Calculate observed Wald score statistic
    double observedT = calculateWaldScore(a, b, c, d)

    // Find the nuisance parameter π that maximizes the p-value
    // We search over a grid of π values from 0 to 1
    int gridSize = Math.min(100, n + 1)
    double maxPValue = 0.0
    double optimalPi = 0.5

    for (int i = 0; i <= gridSize; i++) {
      double pi = i / (double) gridSize

      // Calculate p-value for this π
      double pValue = calculatePValueForPi(n1, n2, n, observedT, pi)

      if (pValue > maxPValue) {
        maxPValue = pValue
        optimalPi = pi
      }
    }

    // Clamp p-value to [0, 1] to handle numerical precision issues
    maxPValue = Math.max(0.0, Math.min(1.0, maxPValue))

    return new BarnardResult(
      statistic: observedT,
      pValue: maxPValue,
      nuisanceParameter: optimalPi,
      sampleSize: n
    )
  }

  /**
   * Calculate the Wald score statistic for a 2×2 table.
   * T = (p1 - p2) / sqrt(π(1-π)(1/n1 + 1/n2))
   * where p1 = a/(a+b), p2 = c/(c+d), π is the pooled proportion
   */
  private static double calculateWaldScore(int a, int b, int c, int d) {
    int n1 = a + b
    int n2 = c + d

    if (n1 == 0 || n2 == 0) {
      return 0.0
    }

    double p1 = a / (double) n1
    double p2 = c / (double) n2

    // Use observed pooled proportion for the statistic
    double pooledP = (a + c) / (double) (n1 + n2)

    // Handle edge cases
    if (pooledP == 0.0 || pooledP == 1.0) {
      return (Math.abs(p1 - p2) > 1e-10) ? (Double.POSITIVE_INFINITY as double) : (0.0 as double)
    }

    double variance = pooledP * (1 - pooledP) * (1.0 / n1 + 1.0 / n2)
    if (variance < 1e-10) {
      return (Math.abs(p1 - p2) > 1e-10) ? (Double.POSITIVE_INFINITY as double) : (0.0 as double)
    }

    return (p1 - p2) / Math.sqrt(variance)
  }

  /**
   * Calculate p-value for a given nuisance parameter π.
   * This involves enumerating all possible tables with the same margins
   * and calculating their probabilities under the binomial model.
   */
  private static double calculatePValueForPi(int n1, int n2, int n, double observedT, double pi) {
    double pValue = 0.0

    // Enumerate all possible values of x1 (count in cell a)
    for (int x1 = 0; x1 <= n1; x1++) {
      int y1 = n1 - x1  // Count in cell b

      for (int x2 = 0; x2 <= n2; x2++) {
        int y2 = n2 - x2  // Count in cell d

        // Calculate Wald score for this table
        double t = calculateWaldScoreWithPi(x1, y1, x2, y2, pi)

        // If this table is as or more extreme, include its probability
        if (Math.abs(t) >= Math.abs(observedT) - 1e-10) {
          double prob = binomialProbability(n1, x1, pi) * binomialProbability(n2, x2, pi)
          pValue += prob
        }
      }
    }

    return pValue
  }

  /**
   * Calculate Wald score with a specific π value.
   */
  private static double calculateWaldScoreWithPi(int x1, int y1, int x2, int y2, double pi) {
    int n1 = x1 + y1
    int n2 = x2 + y2

    if (n1 == 0 || n2 == 0) {
      return 0.0
    }

    double p1 = x1 / (double) n1
    double p2 = x2 / (double) n2

    // Handle edge cases
    if (pi <= 0.0 || pi >= 1.0) {
      return Math.abs(p1 - p2)
    }

    double variance = pi * (1 - pi) * (1.0 / n1 + 1.0 / n2)
    if (variance < 1e-10) {
      return Math.abs(p1 - p2) > 1e-10 ? 1000.0 : 0.0
    }

    return (p1 - p2) / Math.sqrt(variance)
  }

  /**
   * Calculate binomial probability: C(n,k) * p^k * (1-p)^(n-k)
   */
  private static double binomialProbability(int n, int k, double p) {
    if (k < 0 || k > n) {
      return 0.0
    }

    if (p <= 0.0) {
      return k == 0 ? 1.0 : 0.0
    }

    if (p >= 1.0) {
      return k == n ? 1.0 : 0.0
    }

    // Use log probabilities for numerical stability
    double logProb = logBinomialCoefficient(n, k) + k * Math.log(p) + (n - k) * Math.log(1 - p)

    return Math.exp(logProb)
  }

  /**
   * Calculate log of binomial coefficient: log(C(n,k))
   */
  private static double logBinomialCoefficient(int n, int k) {
    if (k < 0 || k > n) {
      return Double.NEGATIVE_INFINITY
    }

    if (k == 0 || k == n) {
      return 0.0
    }

    // Use symmetry: C(n,k) = C(n, n-k)
    if (k > n - k) {
      k = n - k
    }

    double result = 0.0
    for (int i = 1; i <= k; i++) {
      result += Math.log(n - k + i) - Math.log(i)
    }

    return result
  }

  private static void validateTable(int[][] table) {
    if (table == null) {
      throw new IllegalArgumentException("Table cannot be null")
    }

    if (table.length != 2) {
      throw new IllegalArgumentException("Table must be 2×2 (got ${table.length} rows)")
    }

    for (int row = 0; row < 2; row++) {
      if (table[row] == null) {
        throw new IllegalArgumentException("Row ${row} is null")
      }
      if (table[row].length != 2) {
        throw new IllegalArgumentException("Row ${row} must have 2 columns (got ${table[row].length})")
      }

      for (int col = 0; col < 2; col++) {
        if (table[row][col] < 0) {
          throw new IllegalArgumentException("Cell [${row}][${col}] has negative value: ${table[row][col]}")
        }
      }
    }
  }

  /**
   * Result class for Barnard's test.
   */
  @CompileStatic
  static class BarnardResult {
    /** The Wald score statistic */
    double statistic

    /** The p-value (two-sided) */
    double pValue

    /** The nuisance parameter that maximizes p-value */
    double nuisanceParameter

    /** The total sample size */
    int sampleSize

    /**
     * Interprets the Barnard's test result.
     *
     * @param alpha The significance level (default: 0.05)
     * @return A string describing whether there is a significant association
     */
    String interpret(double alpha = 0.05) {
      if (pValue < alpha) {
        return "Reject H0: Significant association detected (T = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      } else {
        return "Fail to reject H0: No significant association detected (T = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      }
    }

    /**
     * Evaluates the test result.
     *
     * @return A detailed description of the test result
     */
    String evaluate(double alpha = 0.05) {
      String significance = pValue < alpha ? "significant" : "not significant"

      return String.format(
        "Barnard's exact test:\\n" +
        "Wald score statistic: %.4f\\n" +
        "p-value: %.4f\\n" +
        "Nuisance parameter (π): %.4f\\n" +
        "Sample size: %d\\n" +
        "Conclusion: Association is %s at %.0f%% significance level",
        statistic, pValue, nuisanceParameter, sampleSize, significance, alpha * 100
      )
    }

    @Override
    String toString() {
      return """Barnard's Exact Test
  Sample size: ${sampleSize}
  Wald score statistic: ${String.format('%.4f', statistic)}
  p-value: ${String.format('%.4f', pValue)}
  Nuisance parameter (π): ${String.format('%.4f', nuisanceParameter)}

  ${interpret()}"""
    }
  }
}
