package se.alipsa.groovy.stats.contingency

import se.alipsa.groovy.matrix.Matrix

/**
 * A chi-squared test (also chi-square or χ2 test) is a statistical hypothesis test
 * used in the analysis of contingency tables when the sample sizes are large.
 * In simpler terms, this test is primarily used to examine whether two categorical variables
 * (two dimensions of the contingency table) are independent in influencing the test statistic
 * (values within the table).[1] The test is valid when the test statistic is chi-squared distributed
 * under the null hypothesis, specifically Pearson's chi-squared test and variants thereof.
 * The different types are
 * <ul>
 *   <li>Pearson</li>
 *   <li>Yates</li>
 *   <li>likelihood ratio (G-test)</li>
 *   <li>portmanteau test in time series</li>
 * </ul>
 */
class ChiSquared {

  /**
   * Pearson's chi-squared test is a statistical test applied to sets of categorical data to
   * evaluate how likely it is that any observed difference between the sets arose by chance.
   * It is the most widely used of many chi-squared tests, i.e.statistical procedures whose results
   * are evaluated by reference to the chi-squared distribution.
   *
   * @param table
   * @return
   */
  static Matrix pearsonTest(Matrix table) {
    return null
  }

  /**
   * G-tests are likelihood-ratio or maximum likelihood statistical significance tests that are
   * increasingly being used in situations where Pearson's chi-squared tests were previously recommended.
   *
   * @param table
   * @return
   */
  static Matrix gTest(Matrix table) {
    return null
  }

  /**
   * Yates's correction for continuity (or Yates's chi-squared test) is used in certain situations when
   * testing for independence in a contingency table.
   * It aims at correcting the error introduced by assuming that the discrete probabilities of frequencies
   * in the table can be approximated by a continuous distribution (chi-squared).
   *
   * @param table
   * @return
   */
  static Matrix yatesTest(Matrix table) {
    return null
  }
}
