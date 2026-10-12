package se.alipsa.matrix.stats.util

/** Scale-relative checks for numerical variation and residual denominators. */
final class NumericTolerance {

  private static final double RELATIVE_TOLERANCE = 1e-14d

  private NumericTolerance() {
  }

  /**
   * Tests whether a value is negligible compared with a scale in the same units.
   * @param value the magnitude to test
   * @param scale the reference magnitude (zero means only exact zero is negligible)
   * @return whether the magnitude is within numerical roundoff
   */
  static boolean isNegligible(Number value, Number scale) {
    Math.abs(value as double) <= RELATIVE_TOLERANCE * Math.abs(scale as double)
  }

  /**
   * Finds the largest absolute value in a primitive numerical vector.
   * @param values the vector
   * @return its maximum magnitude, or zero for an empty vector
   */
  static double maxMagnitude(double[] values) {
    double maximum = 0.0d
    values.each { double value -> maximum = Math.max(maximum, Math.abs(value)) }
    maximum
  }
}
