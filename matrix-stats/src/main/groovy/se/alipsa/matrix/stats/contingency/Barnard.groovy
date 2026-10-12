package se.alipsa.matrix.stats.contingency

import se.alipsa.matrix.stats.util.NumericConversion

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
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral'])
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
      throw new IllegalArgumentException('Table cannot have zero total count')
    }

    // Calculate observed Wald score statistic
    double observedT = calculateWaldScore(a, b, c, d)

    double[] optimum = UnconditionalExact.maximise(n1, n2) { int x1, int x2, double[] factorial ->
      Math.abs(calculateWaldScore(x1, n1 - x1, x2, n2 - x2)) >= Math.abs(observedT) - 1e-10
    }
    double maxPValue = optimum[0]
    double optimalPi = optimum[1]

    new BarnardResult(
      statistic: BigDecimal.valueOf(observedT),
      pValue: BigDecimal.valueOf(maxPValue),
      nuisanceParameter: BigDecimal.valueOf(optimalPi),
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
    if (variance <= 0) {
      return (Math.abs(p1 - p2) > 1e-10) ? (Double.POSITIVE_INFINITY as double) : (0.0 as double)
    }

    (p1 - p2) / Math.sqrt(variance)
  }

  private static void validateTable(int[][] table) {
    if (table == null) {
      throw new IllegalArgumentException('Table cannot be null')
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
  static class BarnardResult {
    /** The Wald score statistic */
    BigDecimal statistic

    /** The p-value (two-sided) */
    BigDecimal pValue

    /** The nuisance parameter that maximizes p-value */
    BigDecimal nuisanceParameter

    /** The total sample size */
    int sampleSize

    /**
     * Interprets the Barnard's test result.
     *
     * @param alpha The significance level (default: 0.05)
     * @return A string describing whether there is a significant association
     */
    String interpret(Number alpha = 0.05) {
      BigDecimal normalizedAlpha = NumericConversion.toAlpha(alpha)
      if (pValue < normalizedAlpha) {
        return "Reject H0: Significant association detected (T = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      }
      return "Fail to reject H0: No significant association detected (T = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
    }

    /**
     * Evaluates the test result.
     *
     * @return A detailed description of the test result
     */
    String evaluate(Number alpha = 0.05) {
      BigDecimal normalizedAlpha = NumericConversion.toAlpha(alpha)
      String significance = pValue < normalizedAlpha ? 'significant' : 'not significant'

      String.format(
        "Barnard's exact test:\\n" +
        'Wald score statistic: %.4f\\n' +
        'p-value: %.4f\\n' +
        'Nuisance parameter (π): %.4f\\n' +
        'Sample size: %d\\n' +
        'Conclusion: Association is %s at %.0f%% significance level',
        statistic, pValue, nuisanceParameter, sampleSize, significance, normalizedAlpha * 100
      )
    }

    @Override
    String toString() {
      """Barnard's Exact Test
  Sample size: ${sampleSize}
  Wald score statistic: ${String.format('%.4f', statistic)}
  p-value: ${String.format('%.4f', pValue)}
  Nuisance parameter (π): ${String.format('%.4f', nuisanceParameter)}

  ${interpret()}"""
    }
  }
}
