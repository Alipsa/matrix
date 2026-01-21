package se.alipsa.matrix.stats.contingency

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.ChiSquaredDistribution

/**
 * The Cochran–Mantel–Haenszel test (CMH) is a test used in the analysis of stratified or matched categorical data.
 * It allows an investigator to test the association between a binary predictor or treatment and a binary outcome
 * such as case or control status while taking into account the stratification.
 * Unlike the McNemar test which can only handle pairs, the CMH test handles arbitrary strata size.
 *
 * The test is particularly useful when:
 * - You have multiple 2×2 contingency tables (one for each stratum)
 * - You want to test for a common association while controlling for a confounding variable
 * - The stratifying variable might confound the relationship of interest
 *
 * Example:
 * <pre>
 * // Test treatment effect across multiple hospitals (strata)
 * // Stratum 1: Hospital A
 * int[][] stratum1 = [[12, 8],    // Treatment: Success, Failure
 *                     [6, 14]]    // Control: Success, Failure
 *
 * // Stratum 2: Hospital B
 * int[][] stratum2 = [[18, 12],
 *                     [8, 22]]
 *
 * List<int[][]> strata = [stratum1, stratum2]
 * def result = CochranMantelHaenszel.test(strata)
 * println result.toString()
 * </pre>
 *
 * Reference:
 * - Cochran, W. G. (1954). "Some methods for strengthening the common χ² tests"
 * - Mantel, N., & Haenszel, W. (1959). "Statistical aspects of the analysis of data from retrospective studies"
 *
 * Named after William G. Cochran, Nathan Mantel, and William Haenszel.
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
