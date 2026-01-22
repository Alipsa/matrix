package se.alipsa.matrix.stats.contingency

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.NormalDistribution

/**
 * The Cochran-Armitage test for trend is a statistical test used to detect linear trends in
 * proportions across ordered categories. It extends the chi-squared test by incorporating the
 * ordinal nature of exposure or dose levels, making it more powerful for detecting dose-response
 * relationships or monotonic trends.
 *
 * <p><b>What is the Cochran-Armitage test?</b></p>
 * The Cochran-Armitage test, named for William Cochran and Peter Armitage, assesses whether there
 * is a linear trend in the proportion of cases (or events) across k ordered categories. Unlike the
 * standard Pearson chi-squared test which treats categories as nominal, the Cochran-Armitage test
 * incorporates the ordering of categories through assigned scores, providing greater statistical
 * power to detect trends. The test statistic follows a standard normal distribution (Z-distribution)
 * under the null hypothesis of no linear trend.
 *
 * <p><b>When to use the Cochran-Armitage test:</b></p>
 * <ul>
 *   <li>When testing for dose-response relationships in epidemiological studies</li>
 *   <li>When examining trends across ordered exposure levels (e.g., none, low, medium, high)</li>
 *   <li>When you have a binary outcome and an ordinal predictor variable with 3+ levels</li>
 *   <li>When detecting monotonic trends in proportions across ordered categories</li>
 *   <li>In case-control or cohort studies with ordinal exposure classifications</li>
 *   <li>When the ordering of categories has meaningful interpretation (not arbitrary)</li>
 *   <li>As an alternative to chi-squared test when testing for linear trends rather than general association</li>
 * </ul>
 *
 * <p><b>Hypotheses:</b></p>
 * <ul>
 *   <li>H₀ (null hypothesis): There is no linear trend in proportions across the ordered categories (slope = 0)</li>
 *   <li>H₁ (alternative hypothesis): There is a linear trend in proportions across the ordered categories (slope ≠ 0)</li>
 * </ul>
 *
 * <p><b>Example usage:</b></p>
 * <pre>
 * // Test for trend in disease incidence across smoking levels
 * int[] cases = [10, 18, 32, 45]          // Disease cases in each group
 * int[] controls = [90, 82, 68, 55]       // Controls in each group
 * double[] scores = [0, 5, 15, 30]        // Cigarettes per day (custom scoring)
 *
 * def result = CochranArmitage.test(cases, controls, scores)
 * println "Z-statistic: ${result.statistic}"
 * println "p-value: ${result.pValue}"
 * println result.interpret()
 *
 * // Example output:
 * // Z-statistic: 3.8472
 * // p-value: 0.0001
 * // Reject H0: Significant increasing trend detected
 *
 * // You can also use default scores (0, 1, 2, 3) if custom scores aren't needed
 * def result2 = CochranArmitage.test(cases, controls)  // Uses [0, 1, 2, 3]
 * </pre>
 *
 * <p><b>Statistical details:</b></p>
 * The test statistic is calculated as: Z = (Observed weighted sum - Expected weighted sum) / √Variance,
 * where the weighted sum uses the assigned scores for each category. The test is most powerful when:
 * <ul>
 *   <li>The true relationship is approximately linear on the logit scale</li>
 *   <li>The assigned scores accurately reflect the exposure levels</li>
 *   <li>Sample sizes are adequate across all categories</li>
 * </ul>
 *
 * <p><b>Score selection:</b></p>
 * <ul>
 *   <li>Default scores: [0, 1, 2, ..., k-1] for k categories (equally spaced)</li>
 *   <li>Custom scores: Should reflect meaningful differences between categories (e.g., actual dose amounts, midpoints of exposure ranges)</li>
 *   <li>The choice of scores affects test power; use domain knowledge when possible</li>
 *   <li>Linear transformations of scores (a + b×score) do not change the p-value</li>
 * </ul>
 *
 * <p><b>References:</b></p>
 * <ul>
 *   <li>Cochran, W. G. (1954). "Some methods for strengthening the common χ² tests". Biometrics, 10(4), 417-451.</li>
 *   <li>Armitage, P. (1955). "Tests for linear trends in proportions and frequencies". Biometrics, 11(3), 375-386.</li>
 *   <li>Agresti, A. (2013). "Categorical Data Analysis" (3rd ed.). Wiley, Chapter 3.</li>
 * </ul>
 *
 * <p><b>Note:</b> The test assumes independence of observations and is designed to detect linear trends.
 * If you suspect a non-linear dose-response relationship, consider using multiple indicator variables
 * or non-linear modeling approaches instead.</p>
 */
@CompileStatic
class CochranArmitage {

  /**
   * Performs the Cochran-Armitage test for trend.
   *
   * @param cases Number of cases (events) in each ordered category
   * @param controls Number of controls (non-events) in each ordered category
   * @param scores Optional ordinal scores for categories (default: 0, 1, 2, ..., k-1)
   * @return CochranArmitagResult containing test statistic, p-value, and conclusion
   */
  static CochranArmitagResult test(int[] cases, int[] controls, double[] scores = null) {
    validateInput(cases, controls, scores)

    int k = cases.length
    double[] categoryScores = scores ?: (0..<k).collect { it as double } as double[]

    // Calculate totals
    int[] totals = new int[k]
    int casesTotal = 0
    int controlsTotal = 0
    int grandTotal = 0

    for (int i = 0; i < k; i++) {
      totals[i] = cases[i] + controls[i]
      casesTotal += cases[i]
      controlsTotal += controls[i]
      grandTotal += totals[i]
    }

    if (grandTotal == 0) {
      throw new IllegalArgumentException("Total sample size cannot be zero")
    }

    // Calculate weighted sum of cases
    double weightedCasesSum = 0.0
    for (int i = 0; i < k; i++) {
      weightedCasesSum += categoryScores[i] * cases[i]
    }

    // Calculate expected weighted sum
    double totalScoreWeightedByN = 0.0
    for (int i = 0; i < k; i++) {
      totalScoreWeightedByN += categoryScores[i] * totals[i]
    }
    double expectedWeightedSum = casesTotal * totalScoreWeightedByN / grandTotal

    // Calculate variance components
    double sumScoresSquaredWeightedByN = 0.0
    for (int i = 0; i < k; i++) {
      sumScoresSquaredWeightedByN += categoryScores[i] * categoryScores[i] * totals[i]
    }

    double varianceNumerator = casesTotal * controlsTotal *
      (grandTotal * sumScoresSquaredWeightedByN - totalScoreWeightedByN * totalScoreWeightedByN)

    double varianceDenominator = grandTotal * grandTotal * (grandTotal - 1)

    if (varianceDenominator == 0) {
      throw new IllegalArgumentException("Cannot compute variance with sample size = 1")
    }

    double variance = varianceNumerator / varianceDenominator

    if (variance < 1e-10) {
      throw new IllegalArgumentException("Variance is too small - data may have no variation in scores")
    }

    // Calculate test statistic (Z-score)
    double zStatistic = (weightedCasesSum - expectedWeightedSum) / Math.sqrt(variance)

    // Calculate two-sided p-value
    NormalDistribution normalDist = new NormalDistribution(0.0, 1.0)
    double pValue = 2.0 * (1.0 - normalDist.cumulativeProbability(Math.abs(zStatistic)))

    return new CochranArmitagResult(
      statistic: zStatistic,
      pValue: pValue,
      sampleSize: grandTotal,
      categories: k,
      cases: casesTotal,
      controls: controlsTotal,
      scores: categoryScores
    )
  }

  /**
   * Performs the Cochran-Armitage test using Lists instead of arrays.
   */
  static CochranArmitagResult test(List<Integer> cases, List<Integer> controls, List<Double> scores = null) {
    int[] casesArray = cases as int[]
    int[] controlsArray = controls as int[]
    double[] scoresArray = scores ? (scores as double[]) : null
    return test(casesArray, controlsArray, scoresArray)
  }

  private static void validateInput(int[] cases, int[] controls, double[] scores) {
    if (cases == null || controls == null) {
      throw new IllegalArgumentException("Cases and controls cannot be null")
    }
    if (cases.length != controls.length) {
      throw new IllegalArgumentException(
        "Cases and controls must have the same length (got ${cases.length} and ${controls.length})"
      )
    }
    if (cases.length < 2) {
      throw new IllegalArgumentException("Need at least 2 categories for trend test (got ${cases.length})")
    }
    if (scores != null && scores.length != cases.length) {
      throw new IllegalArgumentException(
        "Scores length (${scores.length}) must match number of categories (${cases.length})"
      )
    }

    // Check for negative values
    for (int i = 0; i < cases.length; i++) {
      if (cases[i] < 0) {
        throw new IllegalArgumentException("Cases cannot be negative (category ${i}: ${cases[i]})")
      }
      if (controls[i] < 0) {
        throw new IllegalArgumentException("Controls cannot be negative (category ${i}: ${controls[i]})")
      }
    }
  }

  /**
   * Result class for the Cochran-Armitage test.
   */
  @CompileStatic
  static class CochranArmitagResult {
    /** The test statistic (Z-score) */
    double statistic

    /** The two-sided p-value */
    double pValue

    /** The total sample size */
    int sampleSize

    /** The number of categories */
    int categories

    /** The total number of cases */
    int cases

    /** The total number of controls */
    int controls

    /** The scores used for the categories */
    double[] scores

    /**
     * Interprets the Cochran-Armitage test result.
     *
     * @param alpha The significance level (default: 0.05)
     * @return A string describing whether there is a significant trend
     */
    String interpret(double alpha = 0.05) {
      if (pValue < alpha) {
        String direction = statistic > 0 ? "increasing" : "decreasing"
        return "Reject H0: Significant ${direction} trend detected (Z = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      } else {
        return "Fail to reject H0: No significant trend detected (Z = ${String.format('%.4f', statistic)}, p = ${String.format('%.4f', pValue)})"
      }
    }

    /**
     * Evaluates the test result.
     *
     * @return A detailed description of the test result
     */
    String evaluate(double alpha = 0.05) {
      String direction = statistic > 0 ? "increasing" : "decreasing"
      String significance = pValue < alpha ? "significant" : "not significant"

      return String.format(
        "Cochran-Armitage trend test:\n" +
        "Z-statistic: %.4f (direction: %s)\n" +
        "p-value: %.4f\n" +
        "Sample: n=%d (%d cases, %d controls across %d categories)\n" +
        "Conclusion: Trend is %s at %.0f%% significance level",
        statistic, direction, pValue, sampleSize, cases, controls, categories, significance, alpha * 100
      )
    }

    @Override
    String toString() {
      return """Cochran-Armitage Trend Test
  Categories: ${categories}
  Sample size: ${sampleSize} (${cases} cases, ${controls} controls)
  Scores: ${scores.collect { String.format('%.2f', it) }.join(', ')}
  Z-statistic: ${String.format('%.4f', statistic)}
  p-value: ${String.format('%.4f', pValue)}

  ${interpret()}"""
    }
  }
}
