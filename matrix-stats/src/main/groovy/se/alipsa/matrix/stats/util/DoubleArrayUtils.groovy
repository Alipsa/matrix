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
   * @throws IllegalArgumentException if source is null
   */
  static double[] copy(double[] source) {
    if (source == null) {
      throw new IllegalArgumentException('Source array cannot be null')
    }
    Arrays.copyOf(source, source.length)
  }

  /**
   * Deep-copies a two-dimensional double array, preserving ragged row lengths.
   *
   * @param source the array to copy
   * @return an independent copy of the outer array and every row
   * @throws IllegalArgumentException if source or any source row is null
   */
  static double[][] deepCopy(double[][] source) {
    if (source == null) {
      throw new IllegalArgumentException('Source matrix cannot be null')
    }
    double[][] copy = new double[source.length][]
    for (int i = 0; i < source.length; i++) {
      if (source[i] == null) {
        throw new IllegalArgumentException("Source matrix row ${i} cannot be null")
      }
      copy[i] = DoubleArrayUtils.copy(source[i])
    }
    copy
  }
}
