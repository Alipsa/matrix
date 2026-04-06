package se.alipsa.matrix.stats

import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Native statistical utility functions used by the stats module.
 */
final class StatUtils {

  private StatUtils() {
  }

  /**
   * Computes the arithmetic mean for the supplied values.
   *
   * @param values the sample values
   * @return the arithmetic mean
   * @throws IllegalArgumentException if the array is null or empty
   */
  static double mean(double[] values) {
    validateNotEmpty(values, 'Mean')

    // Use compensated summation to reduce drift when these helpers are reused for larger samples.
    double sum = 0.0d
    double compensation = 0.0d
    for (double value : values) {
      double adjusted = value - compensation
      double nextSum = sum + adjusted
      compensation = (nextSum - sum) - adjusted
      sum = nextSum
    }
    sum / values.length
  }

  /**
   * Computes the arithmetic mean for the supplied values.
   *
   * @param values the sample values
   * @return the arithmetic mean
   * @throws IllegalArgumentException if the list is null or empty
   */
  static double mean(List<? extends Number> values) {
    mean(NumericConversion.toDoubleArray(values, 'values'))
  }

  /**
   * Computes the sample variance using Bessel's correction.
   *
   * @param values the sample values
   * @return the sample variance
   * @throws IllegalArgumentException if the array is null or has fewer than two observations
   */
  static double variance(double[] values) {
    variance(values, mean(values))
  }

  /**
   * Computes the sample variance using Bessel's correction.
   *
   * @param values the sample values
   * @return the sample variance
   * @throws IllegalArgumentException if the list is null or has fewer than two observations
   */
  static double variance(List<? extends Number> values) {
    variance(NumericConversion.toDoubleArray(values, 'values'))
  }

  /**
   * Computes the sample variance using a precomputed mean and Bessel's correction.
   *
   * @param values the sample values
   * @param mean the precomputed arithmetic mean
   * @return the sample variance
   * @throws IllegalArgumentException if the array is null or has fewer than two observations
   */
  static double variance(double[] values, double mean) {
    validateForVariance(values)

    double sumSquaredDeviation = 0.0d
    for (double value : values) {
      double deviation = value - mean
      sumSquaredDeviation += deviation * deviation
    }
    sumSquaredDeviation / (values.length - 1)
  }

  /**
   * Computes the sample variance using a precomputed mean and Bessel's correction.
   *
   * @param values the sample values
   * @param mean the precomputed arithmetic mean
   * @return the sample variance
   * @throws IllegalArgumentException if the list is null or has fewer than two observations
   */
  static double variance(List<? extends Number> values, Number mean) {
    variance(
      NumericConversion.toDoubleArray(values, 'values'),
      NumericConversion.toFiniteDouble(mean, 'mean')
    )
  }

  private static void validateNotEmpty(double[] values, String operation) {
    if (values == null || values.length == 0) {
      throw new IllegalArgumentException("${operation} requires at least one observation")
    }
  }

  private static void validateForVariance(double[] values) {
    if (values == null || values.length < 2) {
      throw new IllegalArgumentException("Variance requires at least two observations")
    }
  }
}
