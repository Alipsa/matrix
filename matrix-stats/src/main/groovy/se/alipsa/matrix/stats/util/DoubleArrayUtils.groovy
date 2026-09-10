package se.alipsa.matrix.stats.util

/**
 * Copy helpers for primitive double arrays.
 */
final class DoubleArrayUtils {

  private DoubleArrayUtils() {
  }

  /**
   * Copies a one-dimensional double array.
   *
   * @param source the array to copy
   * @return an independent copy
   */
  static double[] copy(double[] source) {
    Arrays.copyOf(source, source.length)
  }

  /**
   * Deep-copies a two-dimensional double array, preserving ragged row lengths.
   *
   * @param source the array to copy
   * @return an independent copy of the outer array and every row
   */
  static double[][] deepCopy(double[][] source) {
    double[][] copy = new double[source.length][]
    for (int i = 0; i < source.length; i++) {
      copy[i] = DoubleArrayUtils.copy(source[i])
    }
    copy
  }
}
