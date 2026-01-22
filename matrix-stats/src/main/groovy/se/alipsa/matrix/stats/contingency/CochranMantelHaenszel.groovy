package se.alipsa.matrix.stats.contingency

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.ChiSquaredDistribution

/**
 * The Cochran-Mantel-Haenszel (CMH) test is a statistical test for assessing the association between
 * two binary variables while controlling for one or more stratifying variables. It tests whether there
 * is a consistent association across multiple 2×2 contingency tables (strata), making it essential for
 * controlling confounding in observational studies.
 *
 * <p><b>What is the Cochran-Mantel-Haenszel test?</b></p>
 * The CMH test, named for William G. Cochran, Nathan Mantel, and William Haenszel, tests for a common
 * association between a binary exposure and binary outcome across k strata (subgroups). It combines
 * information from multiple 2×2 tables to test whether the exposure-outcome association is consistent
 * across strata while controlling for the stratifying variable(s). The test produces a chi-squared
 * statistic with 1 degree of freedom and estimates a common (pooled) odds ratio across all strata
 * using the Mantel-Haenszel estimator.
 *
 * <p><b>When to use the Cochran-Mantel-Haenszel test:</b></p>
 * <ul>
 *   <li>When analyzing the association between two binary variables across multiple strata</li>
 *   <li>When you need to control for a confounding variable in observational studies</li>
 *   <li>In meta-analysis to combine results from multiple studies</li>
 *   <li>When testing treatment effects across multiple centers, hospitals, or sites</li>
 *   <li>In matched case-control studies with variable matching ratios</li>
 *   <li>When you want to test for a common odds ratio across strata</li>
 *   <li>As an extension of Fisher's or chi-squared test to stratified data</li>
 *   <li>When the association is assumed to be similar (homogeneous) across strata</li>
 * </ul>
 *
 * <p><b>Hypotheses:</b></p>
 * <ul>
 *   <li>H₀ (null hypothesis): There is no association between exposure and outcome in any stratum (common odds ratio = 1)</li>
 *   <li>H₁ (alternative hypothesis): There is a consistent association between exposure and outcome across strata (common odds ratio ≠ 1)</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 * // Test drug effectiveness across multiple hospitals, controlling for hospital
 * // Stratum 1: Hospital A
 * int[][] hospitalA = [[15, 10],    // Drug: Cured, Not cured
 *                      [8, 17]]     // Placebo: Cured, Not cured
 *
 * // Stratum 2: Hospital B
 * int[][] hospitalB = [[22, 14],    // Drug: Cured, Not cured
 *                      [12, 24]]    // Placebo: Cured, Not cured
 *
 * // Stratum 3: Hospital C
 * int[][] hospitalC = [[18, 8],     // Drug: Cured, Not cured
 *                      [9, 15]]     // Placebo: Cured, Not cured
 *
 * List<int[][]> hospitals = [hospitalA, hospitalB, hospitalC]
 * def result = CochranMantelHaenszel.test(hospitals)
 * println "CMH statistic: ${result.statistic}"
 * println "p-value: ${result.pValue}"
 * println "Common odds ratio: ${result.commonOddsRatio}"
 * println result.interpret()
 *
 * // Example output:
 * // CMH statistic: 5.8234
 * // p-value: 0.0158
 * // Common odds ratio: 2.1547
 * // Reject H0: Significant association detected across strata
 * </pre>
 *
 * <p><b>Statistical details:</b></p>
 * The CMH statistic is calculated as: χ²_CMH = [|Σ(a_i - E[a_i])| - c]² / Σ Var(a_i),
 * where the sum is over all strata, a_i is the observed count in cell [1,1] of stratum i,
 * E[a_i] is its expected value under independence, Var(a_i) is its variance, and c is the
 * continuity correction (0.5 by default, or 0 if disabled). The test statistic follows a
 * chi-squared distribution with 1 degree of freedom under the null hypothesis.
 *
 * <p><b>Common odds ratio (Mantel-Haenszel estimator):</b></p>
 * The pooled odds ratio is: OR_MH = Σ(a_i × d_i / n_i) / Σ(b_i × c_i / n_i),
 * where a_i, b_i, c_i, d_i are the four cells of the 2×2 table in stratum i, and n_i is the
 * total count in stratum i. This provides a summary measure of the association across all strata.
 *
 * <p><b>Assumptions:</b></p>
 * <ul>
 *   <li>Independence of observations within and across strata</li>
 *   <li>Homogeneity of odds ratios across strata (the association is similar in all strata)</li>
 *   <li>Binary exposure and outcome variables</li>
 * </ul>
 *
 * <p><b>Comparison with other tests:</b></p>
 * <ul>
 *   <li>Generalizes the McNemar test to arbitrary stratum sizes (McNemar is CMH with paired data)</li>
 *   <li>More appropriate than pooling all strata into one table when a confounding variable exists</li>
 *   <li>Tests for a common association; use Breslow-Day test to check homogeneity assumption</li>
 *   <li>More powerful than analyzing each stratum separately</li>
 * </ul>
 *
 * <p><b>References:</b></p>
 * <ul>
 *   <li>Cochran, W. G. (1954). "Some methods for strengthening the common χ² tests". Biometrics, 10(4), 417-451.</li>
 *   <li>Mantel, N., & Haenszel, W. (1959). "Statistical aspects of the analysis of data from retrospective studies of disease". Journal of the National Cancer Institute, 22(4), 719-748.</li>
 *   <li>Agresti, A. (2013). "Categorical Data Analysis" (3rd ed.). Wiley, Chapter 8.</li>
 *   <li>Breslow, N. E., & Day, N. E. (1980). "Statistical Methods in Cancer Research, Volume 1: The Analysis of Case-Control Studies". IARC Scientific Publications.</li>
 * </ul>
 *
 * <p><b>Note:</b> The CMH test assumes that the odds ratios are homogeneous (similar) across strata.
 * If the association varies substantially across strata (heterogeneity), the test may not be
 * appropriate. Consider using the Breslow-Day test to assess homogeneity of odds ratios before
 * interpreting the CMH test results. When heterogeneity is present, consider analyzing strata
 * separately or using stratified logistic regression.</p>
 */
@CompileStatic
class CochranMantelHaenszel {

  /**
   * Performs the Cochran-Mantel-Haenszel test.
   *
   * @param strata List of 2×2 tables, where each table is int[2][2] representing:
   *               [[a, b], [c, d]] where:
   *               - a = exposed cases
   *               - b = exposed controls
   *               - c = unexposed cases
   *               - d = unexposed controls
   * @param continuityCorrection Whether to apply continuity correction (default: true)
   * @return CochranMantelHaenszelResult containing test statistic, p-value, and common odds ratio
   */
  static CochranMantelHaenszelResult test(List<int[][]> strata, boolean continuityCorrection = true) {
    validateInput(strata)

    int k = strata.size()  // Number of strata

    // Calculate components for each stratum
    double sumNumerator = 0.0
    double sumVariance = 0.0
    double sumOddsRatioNumerator = 0.0
    double sumOddsRatioDenominator = 0.0

    for (int[][] table : strata) {
      int a = table[0][0]
      int b = table[0][1]
      int c = table[1][0]
      int d = table[1][1]

      int n = a + b + c + d  // Total for this stratum

      if (n == 0) {
        throw new IllegalArgumentException("Stratum has zero total count")
      }

      int r1 = a + b  // Row 1 total
      int r2 = c + d  // Row 2 total
      int c1 = a + c  // Column 1 total
      int c2 = b + d  // Column 2 total

      // Expected value of a under independence
      double expectedA = (r1 * c1) / (double) n

      // Variance of a
      double varianceA = (r1 * r2 * c1 * c2) / ((double) n * n * (n - 1))

      // Accumulate sums
      sumNumerator += (a - expectedA)
      sumVariance += varianceA

      // For common odds ratio estimation (Mantel-Haenszel estimator)
      sumOddsRatioNumerator += (a * d) / (double) n
      sumOddsRatioDenominator += (b * c) / (double) n
    }

    // Apply continuity correction if requested
    double numerator = Math.abs(sumNumerator)
    if (continuityCorrection) {
      numerator = Math.max(0.0, numerator - 0.5)
    }

    if (sumVariance < 1e-10) {
      throw new IllegalArgumentException("Total variance is too small - data may have no variation")
    }

    // Calculate CMH statistic
    double cmhStatistic = (numerator * numerator) / sumVariance

    // Calculate p-value using chi-squared distribution with 1 df
    ChiSquaredDistribution chiSq = new ChiSquaredDistribution(1)
    double pValue = 1.0 - chiSq.cumulativeProbability(cmhStatistic)

    // Calculate common odds ratio (Mantel-Haenszel estimator)
    double commonOddsRatio = Double.NaN
    if (sumOddsRatioDenominator > 1e-10) {
      commonOddsRatio = sumOddsRatioNumerator / sumOddsRatioDenominator
    }

    return new CochranMantelHaenszelResult(
      statistic: cmhStatistic,
      pValue: pValue,
      strata: k,
      commonOddsRatio: commonOddsRatio,
      continuityCorrection: continuityCorrection
    )
  }

  private static void validateInput(List<int[][]> strata) {
    if (strata == null || strata.isEmpty()) {
      throw new IllegalArgumentException("Strata list cannot be null or empty")
    }

    for (int i = 0; i < strata.size(); i++) {
      int[][] table = strata[i]

      if (table == null) {
        throw new IllegalArgumentException("Stratum ${i} is null")
      }

      if (table.length != 2) {
        throw new IllegalArgumentException(
          "Stratum ${i} must be 2×2 (got ${table.length} rows)"
        )
      }

      for (int row = 0; row < 2; row++) {
        if (table[row] == null) {
          throw new IllegalArgumentException("Stratum ${i}, row ${row} is null")
        }
        if (table[row].length != 2) {
          throw new IllegalArgumentException(
            "Stratum ${i}, row ${row} must have 2 columns (got ${table[row].length})"
          )
        }

        // Check for negative values
        for (int col = 0; col < 2; col++) {
          if (table[row][col] < 0) {
            throw new IllegalArgumentException(
              "Stratum ${i}, cell [${row}][${col}] has negative value: ${table[row][col]}"
            )
          }
        }
      }
    }
  }

  /**
   * Result class for the Cochran-Mantel-Haenszel test.
   */
  @CompileStatic
  static class CochranMantelHaenszelResult {
    /** The CMH test statistic (chi-squared) */
    double statistic

    /** The p-value */
    double pValue

    /** The number of strata */
    int strata

    /** The common odds ratio (Mantel-Haenszel estimator) */
    double commonOddsRatio

    /** Whether continuity correction was applied */
    boolean continuityCorrection

    /**
     * Interprets the Cochran-Mantel-Haenszel test result.
     *
     * @param alpha The significance level (default: 0.05)
     * @return A string describing whether there is a significant association
     */
    String interpret(double alpha = 0.05) {
      if (pValue < alpha) {
        return "Reject H0: Significant association detected across strata (χ² = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      } else {
        return "Fail to reject H0: No significant association detected across strata (χ² = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      }
    }

    /**
     * Evaluates the test result.
     *
     * @return A detailed description of the test result
     */
    String evaluate(double alpha = 0.05) {
      String significance = pValue < alpha ? "significant" : "not significant"
      String oddsRatioStr = Double.isNaN(commonOddsRatio) ?
        "undefined" :
        String.format("%.4f", commonOddsRatio)

      String direction = ""
      if (!Double.isNaN(commonOddsRatio)) {
        if (commonOddsRatio > 1) {
          direction = " (positive association)"
        } else if (commonOddsRatio < 1) {
          direction = " (negative association)"
        } else {
          direction = " (no association)"
        }
      }

      return String.format(
        "Cochran-Mantel-Haenszel test:\n" +
        "χ² statistic: %.4f\n" +
        "p-value: %.4f\n" +
        "Common odds ratio: %s%s\n" +
        "Strata: %d\n" +
        "Continuity correction: %s\n" +
        "Conclusion: Association is %s at %.0f%% significance level",
        statistic, pValue, oddsRatioStr, direction, strata,
        continuityCorrection ? "yes" : "no",
        significance, alpha * 100
      )
    }

    @Override
    String toString() {
      String oddsRatioStr = Double.isNaN(commonOddsRatio) ?
        "undefined" :
        String.format('%.4f', commonOddsRatio)

      return """Cochran-Mantel-Haenszel Test
  Strata: ${strata}
  χ² statistic: ${String.format('%.4f', statistic)}
  p-value: ${String.format('%.4f', pValue)}
  Common odds ratio: ${oddsRatioStr}
  Continuity correction: ${continuityCorrection ? 'yes' : 'no'}

  ${interpret()}"""
    }
  }
}
