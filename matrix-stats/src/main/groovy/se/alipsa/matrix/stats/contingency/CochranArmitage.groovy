package se.alipsa.matrix.stats.contingency

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.NormalDistribution

/**
 * The Cochran–Armitage test for trend, named for William Cochran and Peter Armitage,
 * is used in categorical data analysis when the aim is to assess for the presence of an
 * association between a variable with two categories and an ordinal variable with k categories.
 * It modifies the Pearson chi-squared test to incorporate a suspected ordering in the effects
 * of the k categories of the second variable.
 *
 * The test is particularly useful for detecting dose-response relationships or trends across
 * ordered categories (e.g., low, medium, high exposure levels).
 *
 * The test statistic follows a standard normal distribution under the null hypothesis of no trend.
 *
 * Example:
 * <pre>
 * // Test for trend in disease rates across exposure levels (low, medium, high)
 * int[] cases = [10, 15, 25]      // Disease cases at each level
 * int[] controls = [90, 85, 75]   // Controls at each level
 * double[] scores = [0, 1, 2]     // Ordinal scores for exposure levels
 *
 * def result = CochranArmitage.test(cases, controls, scores)
 * println result.toString()
 * </pre>
 *
 * Reference:
 * - Cochran, W. G. (1954). "Some methods for strengthening the common χ² tests"
 * - Armitage, P. (1955). "Tests for linear trends in proportions and frequencies"
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
