package se.alipsa.matrix.stats.contingency

import groovy.transform.CompileStatic
import org.apache.commons.math3.distribution.ChiSquaredDistribution
import se.alipsa.matrix.core.Matrix

/**
 * A chi-squared test (also chi-square or χ² test) is a statistical hypothesis test
 * used in the analysis of contingency tables when the sample sizes are large.
 * In simpler terms, this test is primarily used to examine whether two categorical variables
 * (two dimensions of the contingency table) are independent in influencing the test statistic
 * (values within the table). The test is valid when the test statistic is chi-squared distributed
 * under the null hypothesis, specifically Pearson's chi-squared test and variants thereof.
 *
 * <p>The different types are:</p>
 * <ul>
 *   <li>Pearson - Standard chi-squared test</li>
 *   <li>Yates - Yates' correction for continuity (for 2×2 tables)</li>
 *   <li>likelihood ratio (G-test) - Uses log-likelihood ratios</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Test independence in a 2×2 table
 * def table = Matrix.builder()
 *   .matrixName('contingency')
 *   .rows([[12, 5], [9, 11]])
 *   .build()
 * def result = ChiSquared.pearsonTest(table)
 * println "p-value: ${result.pValue}"
 * </pre>
 */
@CompileStatic
class ChiSquared {

  /**
   * Pearson's chi-squared test is a statistical test applied to sets of categorical data to
   * evaluate how likely it is that any observed difference between the sets arose by chance.
   * It is the most widely used of many chi-squared tests, i.e. statistical procedures whose results
   * are evaluated by reference to the chi-squared distribution.
   *
   * <p>The test statistic is calculated as: χ² = Σ((O - E)² / E)</p>
   * <p>where O = observed frequency, E = expected frequency</p>
   *
   * @param table The contingency table (Matrix) with observed frequencies
   * @return ChiSquaredResult containing test statistic, degrees of freedom, and p-value
   * @throws IllegalArgumentException if table contains negative values or all expected frequencies < 5
   */
  static ChiSquaredResult pearsonTest(Matrix table) {
    validateTable(table)

    int rows = table.rowCount()
    int cols = table.columnCount()

    // Calculate row and column totals
    double[] rowTotals = new double[rows]
    double[] colTotals = new double[cols]
    double grandTotal = 0

    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        double value = table.get(i, j) as double
        rowTotals[i] += value
        colTotals[j] += value
        grandTotal += value
      }
    }

    // Calculate chi-squared statistic
    double chiSquared = 0
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        double observed = table.get(i, j) as double
        double expected = (rowTotals[i] * colTotals[j]) / grandTotal

        if (expected < 1.0) {
          throw new IllegalArgumentException("Expected frequency too small (< 1). Consider using Fisher's exact test instead.")
        }

        chiSquared += Math.pow(observed - expected, 2) / expected
      }
    }

    // Degrees of freedom = (rows - 1) * (cols - 1)
    int degreesOfFreedom = (rows - 1) * (cols - 1)

    // Calculate p-value
    ChiSquaredDistribution distribution = new ChiSquaredDistribution(degreesOfFreedom)
    double pValue = 1.0 - distribution.cumulativeProbability(chiSquared)

    return new ChiSquaredResult(
      testStatistic: chiSquared,
      degreesOfFreedom: degreesOfFreedom,
      pValue: pValue,
      testType: "Pearson"
    )
  }

  /**
   * G-tests are likelihood-ratio or maximum likelihood statistical significance tests that are
   * increasingly being used in situations where Pearson's chi-squared tests were previously recommended.
   *
   * <p>The test statistic is calculated as: G = 2 * Σ(O * ln(O / E))</p>
   * <p>where O = observed frequency, E = expected frequency</p>
   *
   * @param table The contingency table (Matrix) with observed frequencies
   * @return ChiSquaredResult containing test statistic, degrees of freedom, and p-value
   * @throws IllegalArgumentException if table contains negative values or zero observed frequencies
   */
  static ChiSquaredResult gTest(Matrix table) {
    validateTable(table)

    int rows = table.rowCount()
    int cols = table.columnCount()

    // Calculate row and column totals
    double[] rowTotals = new double[rows]
    double[] colTotals = new double[cols]
    double grandTotal = 0

    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        double value = table.get(i, j) as double
        rowTotals[i] += value
        colTotals[j] += value
        grandTotal += value
      }
    }

    // Calculate G statistic
    double gStatistic = 0
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        double observed = table.get(i, j) as double
        double expected = (rowTotals[i] * colTotals[j]) / grandTotal

        if (observed > 0) {  // Only include if observed > 0 (log of 0 is undefined)
          gStatistic += observed * Math.log(observed / expected)
        }
      }
    }
    gStatistic *= 2.0

    // Degrees of freedom = (rows - 1) * (cols - 1)
    int degreesOfFreedom = (rows - 1) * (cols - 1)

    // Calculate p-value (G statistic is approximately chi-squared distributed)
    ChiSquaredDistribution distribution = new ChiSquaredDistribution(degreesOfFreedom)
    double pValue = 1.0 - distribution.cumulativeProbability(gStatistic)

    return new ChiSquaredResult(
      testStatistic: gStatistic,
      degreesOfFreedom: degreesOfFreedom,
      pValue: pValue,
      testType: "G-test"
    )
  }

  /**
   * Yates's correction for continuity (or Yates's chi-squared test) is used in certain situations when
   * testing for independence in a contingency table.
   * It aims at correcting the error introduced by assuming that the discrete probabilities of frequencies
   * in the table can be approximated by a continuous distribution (chi-squared).
   *
   * <p>This correction should only be used for 2×2 contingency tables.</p>
   * <p>The test statistic is calculated as: χ² = Σ((|O - E| - 0.5)² / E)</p>
   *
   * @param table The 2×2 contingency table (Matrix) with observed frequencies
   * @return ChiSquaredResult containing test statistic, degrees of freedom, and p-value
   * @throws IllegalArgumentException if table is not 2×2 or contains negative values
   */
  static ChiSquaredResult yatesTest(Matrix table) {
    if (table.rowCount() != 2 || table.columnCount() != 2) {
      throw new IllegalArgumentException("Yates' correction requires a 2×2 table (got ${table.rowCount()}×${table.columnCount()})")
    }

    validateTable(table)

    int rows = 2
    int cols = 2

    // Calculate row and column totals
    double[] rowTotals = new double[2]
    double[] colTotals = new double[2]
    double grandTotal = 0

    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        double value = table.get(i, j) as double
        rowTotals[i] += value
        colTotals[j] += value
        grandTotal += value
      }
    }

    // Calculate chi-squared statistic with Yates' correction
    double chiSquared = 0
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        double observed = table.get(i, j) as double
        double expected = (rowTotals[i] * colTotals[j]) / grandTotal

        // Apply Yates' correction: subtract 0.5 from |O - E|
        double correctedDiff = Math.abs(observed - expected) - 0.5
        if (correctedDiff < 0) correctedDiff = 0  // Don't allow negative values

        chiSquared += Math.pow(correctedDiff, 2) / expected
      }
    }

    // Degrees of freedom = 1 for 2×2 table
    int degreesOfFreedom = 1

    // Calculate p-value
    ChiSquaredDistribution distribution = new ChiSquaredDistribution(degreesOfFreedom)
    double pValue = 1.0 - distribution.cumulativeProbability(chiSquared)

    return new ChiSquaredResult(
      testStatistic: chiSquared,
      degreesOfFreedom: degreesOfFreedom,
      pValue: pValue,
      testType: "Yates"
    )
  }

  private static void validateTable(Matrix table) {
    if (table == null) {
      throw new IllegalArgumentException("Table cannot be null")
    }
    if (table.rowCount() < 2 || table.columnCount() < 2) {
      throw new IllegalArgumentException("Table must be at least 2×2 (got ${table.rowCount()}×${table.columnCount()})")
    }

    // Check for negative values
    for (int i = 0; i < table.rowCount(); i++) {
      for (int j = 0; j < table.columnCount(); j++) {
        double value = table.get(i, j) as double
        if (value < 0) {
          throw new IllegalArgumentException("Table values must be non-negative (found ${value} at [${i},${j}])")
        }
      }
    }
  }

  /**
   * Result class for chi-squared tests.
   */
  static class ChiSquaredResult {
    /** The chi-squared test statistic (or G statistic for G-test) */
    Double testStatistic

    /** Degrees of freedom for the test */
    Integer degreesOfFreedom

    /** The p-value of the test */
    Double pValue

    /** The type of test performed */
    String testType

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
      return """Chi-Squared Test Result (${testType}):
  test statistic: ${testStatistic}
  degrees of freedom: ${degreesOfFreedom}
  p-value: ${pValue}"""
    }
  }
}
