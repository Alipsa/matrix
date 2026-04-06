package se.alipsa.matrix.stats.kde

import se.alipsa.matrix.stats.interpolation.Interpolation
import se.alipsa.matrix.stats.util.NumericConversion

/**
 * Bandwidth selection methods for kernel density estimation.
 *
 * The bandwidth (h) controls the smoothness of the density estimate:
 * - Smaller bandwidth: more detail, but may be too noisy
 * - Larger bandwidth: smoother, but may obscure important features
 *
 * This class provides automatic bandwidth selection using common rules of thumb.
 */
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral'])
class BandwidthSelector {

  /**
   * Silverman's rule of thumb for bandwidth selection.
   *
   * This is the default method used by R's density() function.
   * Formula: h = 0.9 * min(σ, IQR/1.34) * n^(-1/5)
   *
   * Reference: Silverman, B. W. (1986). Density Estimation for Statistics and Data Analysis.
   *
   * @param data numeric data points
   * @return the estimated optimal bandwidth
   */
  static BigDecimal silverman(List<? extends Number> data) {
    double[] sorted = validateAndSort(data)
    int n = sorted.length
    double std = computeStd(sorted)
    double iqr = computeIQR(sorted)

    // Use robust scale estimate: min of std and IQR/1.34
    double scale = Math.min(std, iqr / 1.34)

    // Handle edge case where scale is 0 (all identical values)
    if (scale <= 0) {
      scale = std > 0 ? std : 1.0d
    }

    BigDecimal.valueOf(0.9 * scale * Math.pow(n, -0.2))
  }

  /**
   * Scott's rule for bandwidth selection.
   *
   * Formula: h = 3.49 * σ * n^(-1/3)
   *
   * Reference: Scott, D. W. (1992). Multivariate Density Estimation.
   *
   * @param data numeric data points
   * @return the estimated optimal bandwidth
   */
  static BigDecimal scott(List<? extends Number> data) {
    double[] sorted = validateAndSort(data)
    int n = sorted.length
    double std = computeStd(sorted)

    // Handle edge case where std is 0
    if (std <= 0) {
      std = 1.0
    }

    BigDecimal.valueOf(3.49 * std * Math.pow(n, -1.0 / 3.0))
  }

  static double silvermanValue(double[] data) {
    validateDenseData(data)
    silverman(data.collect { double value -> value } as List<Double>) as double
  }

  static double scottValue(double[] data) {
    validateDenseData(data)
    scott(data.collect { double value -> value } as List<Double>) as double
  }

  /**
   * Compute the sample standard deviation.
   *
   * @param data the data array
   * @return the sample standard deviation (using n-1 denominator)
   */
  private static double computeStd(double[] data) {
    int n = data.length
    double mean = 0.0
    for (double d : data) {
      mean += d
    }
    mean /= n

    double variance = 0.0
    for (double d : data) {
      double diff = d - mean
      variance += diff * diff
    }
    variance /= (n - 1)  // Sample variance (Bessel's correction)

    Math.sqrt(variance)
  }

  /**
   * Compute the interquartile range (IQR = Q3 - Q1).
   *
   * @param data sorted array of data points
   * @return the interquartile range
   */
  private static double computeIQR(double[] data) {
    // Simple quartile calculation using linear interpolation
    double q1 = percentile(data, 0.25)
    double q3 = percentile(data, 0.75)
    q3 - q1
  }

  /**
   * Compute a percentile value using linear interpolation.
   *
   * @param sortedData sorted array of data points
   * @param p the percentile (0.0 to 1.0)
   * @return the interpolated percentile value
   */
  private static double percentile(double[] sortedData, double p) {
    Interpolation.linear(sortedData.collect { double value -> value } as List<Double>, p * (sortedData.length - 1)) as double
  }

  private static double[] validateAndSort(List<? extends Number> data) {
    double[] values = NumericConversion.toDoubleArray(data, 'data')
    validateDenseData(values)
    Arrays.sort(values)
    values
  }

  private static void validateDenseData(double[] data) {
    if (data == null || data.length < 2) {
      throw new IllegalArgumentException('Data must contain at least 2 values')
    }
  }

}
