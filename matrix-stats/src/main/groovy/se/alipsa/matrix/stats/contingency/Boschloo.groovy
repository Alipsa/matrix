package se.alipsa.matrix.stats.contingency

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Boschloo's exact test is an unconditional exact test for 2×2 contingency tables that provides
 * uniformly greater statistical power than Fisher's exact test. It tests for association between
 * two binary variables by conditioning only on the total sample size, using Fisher's exact p-value
 * as the test statistic.
 *
 * <p><b>What is Boschloo's test?</b></p>
 * Boschloo's test is a more powerful alternative to Fisher's exact test that addresses the conservatism
 * of Fisher's test by using an unconditional approach. For each possible value of the nuisance parameter
 * π (the common success probability under the null hypothesis), the test computes the probability of
 * obtaining a table with a Fisher's exact p-value as extreme or more extreme than the observed table.
 * The final p-value is the maximum over all values of π, ensuring proper error rate control while
 * maximizing statistical power.
 *
 * <p><b>When to use Boschloo's test:</b></p>
 * <ul>
 *   <li>When analyzing 2×2 contingency tables with small to moderate sample sizes</li>
 *   <li>When margins are not fixed by experimental design (e.g., case-control studies, observational studies)</li>
 *   <li>When you need an exact test with greater power than Fisher's exact test</li>
 *   <li>When comparing two independent proportions and you want to avoid the conservatism of Fisher's test</li>
 *   <li>As a gold standard exact test for 2×2 tables when computational resources permit</li>
 *   <li>When expected cell frequencies are too small for chi-squared test validity (&lt; 5)</li>
 * </ul>
 *
 * <p><b>Hypotheses:</b></p>
 * <ul>
 *   <li>H₀ (null hypothesis): The two proportions are equal (p₁ = p₂), or equivalently, the two variables are independent</li>
 *   <li>H₁ (alternative hypothesis): The two proportions differ (p₁ ≠ p₂), indicating association between the variables</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 * // Test effectiveness of a new drug versus placebo
 * int[][] table = [[18, 12],    // Drug group: Improved, Not improved
 *                  [8, 22]]     // Placebo group: Improved, Not improved
 *
 * def result = Boschloo.test(table)
 * println "Boschloo p-value: ${result.pValue}"
 * println "Fisher p-value: ${result.fisherPValue}"
 * println "Optimal π: ${result.nuisanceParameter}"
 * println result.interpret()
 *
 * // Example output:
 * // Boschloo p-value: 0.0234
 * // Fisher p-value: 0.0312
 * // Optimal π: 0.4333
 * // Reject H0: Significant association detected
 * </pre>
 *
 * <p><b>Statistical details:</b></p>
 * The test uses Fisher's exact test p-value as an ordering statistic. For a given nuisance parameter π,
 * the Boschloo p-value is: P_π(Fisher p-value ≤ observed Fisher p-value). The final p-value is:
 * max_π P_π(Fisher p-value ≤ observed Fisher p-value), where the maximization is over all possible
 * values of π ∈ [0, 1]. This ensures the test maintains the nominal Type I error rate while being
 * less conservative than Fisher's exact test.
 *
 * <p><b>Comparison with other tests:</b></p>
 * <ul>
 *   <li>More powerful than Fisher's exact test (less conservative)</li>
 *   <li>Similar power to Barnard's test but uses different test statistic</li>
 *   <li>Exact test (maintains correct Type I error rate for any sample size)</li>
 *   <li>More appropriate than chi-squared test for small sample sizes</li>
 * </ul>
 *
 * <p><b>References:</b></p>
 * <ul>
 *   <li>Boschloo, R. D. (1970). "Raised conditional level of significance for the 2 × 2-table when testing the equality of two probabilities". Statistica Neerlandica, 24(1), 1-9.</li>
 *   <li>Lydersen, S., Fagerland, M. W., & Laake, P. (2009). "Recommended tests for association in 2×2 tables". Statistics in Medicine, 28(7), 1159-1175.</li>
 *   <li>Suissa, S., & Shuster, J. J. (1985). "Exact unconditional sample sizes for the 2×2 binomial trial". Journal of the Royal Statistical Society: Series A, 148(4), 317-327.</li>
 * </ul>
 *
 * <p><b>Note:</b> Boschloo's test is computationally intensive for large sample sizes (n > 200) as it
 * requires enumerating all possible 2×2 tables and computing Fisher's exact test for each combination
 * at multiple values of the nuisance parameter. For very large samples, consider using the chi-squared
 * test or Barnard's test instead.</p>
 */
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral', 'VariableName'])
class Boschloo {

  /**
   * Performs Boschloo's exact test for a 2×2 contingency table.
   *
   * @param table A 2×2 table represented as int[2][2]: [[a, b], [c, d]]
   * @return BoschlooResult containing p-value and nuisance parameter
   */
  static BoschlooResult test(int[][] table) {
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

    double observedFisherP = 0
    // The observed table is encountered during the single enumeration; compute its threshold first.
    double[] factorial = new double[n + 1]
    (1..n).each { int i -> factorial[i] = factorial[i - 1] + Math.log(i) }
    observedFisherP = UnconditionalExact.fisher(a, n1, c, n2, factorial)
    double[] optimum = UnconditionalExact.maximise(n1, n2) { int x1, int x2, double[] logs ->
      double candidate
      if (x1 == a && x2 == c) {
        candidate = observedFisherP
      } else {
        candidate = UnconditionalExact.fisher(x1, n1, x2, n2, logs)
      }
      candidate <= observedFisherP * (1 + 1e-7)
    }
    double maxPValue = optimum[0]
    double optimalPi = optimum[1]

    new BoschlooResult(
      pValue: BigDecimal.valueOf(maxPValue),
      fisherPValue: BigDecimal.valueOf(observedFisherP),
      nuisanceParameter: BigDecimal.valueOf(optimalPi),
      sampleSize: n
    )
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
   * Result class for Boschloo's test.
   */
  static class BoschlooResult {
    /** The p-value (two-sided) */
    BigDecimal pValue

    /** Fisher's exact p-value for the observed table */
    BigDecimal fisherPValue

    /** The nuisance parameter that maximizes p-value */
    BigDecimal nuisanceParameter

    /** The total sample size */
    int sampleSize

    /**
     * Interprets the Boschloo test result.
     *
     * @param alpha The significance level (default: 0.05)
     * @return A string describing whether there is a significant association
     */
    String interpret(Number alpha = 0.05) {
      BigDecimal normalizedAlpha = NumericConversion.toAlpha(alpha)
      if (pValue < normalizedAlpha) {
        return "Reject H0: Significant association detected (p = ${String.format('%.4f', pValue)})"
      }
      return "Fail to reject H0: No significant association detected (p = ${String.format('%.4f', pValue)})"
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
        "Boschloo's exact test:\\n" +
        'p-value: %.4f\\n' +
        "Fisher's p-value: %.4f\\n" +
        'Nuisance parameter (π): %.4f\\n' +
        'Sample size: %d\\n' +
        'Conclusion: Association is %s at %.0f%% significance level',
        pValue, fisherPValue, nuisanceParameter, sampleSize, significance, normalizedAlpha * 100
      )
    }

    @Override
    String toString() {
      """Boschloo's Exact Test
  Sample size: ${sampleSize}
  p-value: ${String.format('%.4f', pValue)}
  Fisher's p-value: ${String.format('%.4f', fisherPValue)}
  Nuisance parameter (π): ${String.format('%.4f', nuisanceParameter)}

  ${interpret()}"""
    }
  }
}
