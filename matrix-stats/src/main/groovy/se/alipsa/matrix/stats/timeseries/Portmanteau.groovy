package se.alipsa.matrix.stats.timeseries

import se.alipsa.matrix.core.Matrix

/**
 * A portmanteau test is a type of statistical hypothesis test in which the null hypothesis is well specified,
 * but the alternative hypothesis is more loosely specified.
 * Tests constructed in this context can have the property of being at least moderately powerful against a
 * wide range of departures from the null hypothesis. Thus, in applied statistics, a portmanteau test
 * provides a reasonable way of proceeding as a general check of a model's match to a dataset where there
 * are many different ways in which the model may depart from the underlying data generating process.
 * Use of such tests avoids having to be very specific about the particular type of departure being tested.
 */
class Portmanteau {

  /**
   * The Ljung–Box test (named for Greta M. Ljung and George E. P. Box) is a type of statistical test of whether
   * any of a group of autocorrelations of a time series are different from zero.
   * Instead of testing randomness at each distinct lag, it tests the "overall" randomness based on a number of
   * lags, and is therefore a portmanteau test.
   */
  Matrix ljungBox(Matrix table) {
    return null
  }
}
