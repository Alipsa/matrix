package se.alipsa.matrix.stats.util

/**
 * Shared numeric conversion helpers for `matrix-stats`.
 * <p>
 * This utility centralizes finite-number validation and list-to-array conversion so
 * public facades such as linear algebra and interpolation do not duplicate the same
 * low-level numeric adapter logic.
 */
final class NumericConversion {

  private NumericConversion() {
  }

  /**
   * Convert a numeric vector into a dense {@code double[]} array.
   *
   * @param values the numeric vector
   * @param label the label used in validation messages
   * @return the dense numeric vector
   */
  static double[] toDoubleArray(List<? extends Number> values, String label = 'values') {
    if (values == null) {
      throw new IllegalArgumentException("${label.capitalize()} cannot be null")
    }
    if (values.isEmpty()) {
      throw new IllegalArgumentException("${label.capitalize()} must contain at least one value")
    }
    double[] numericValues = new double[values.size()]
    for (int i = 0; i < values.size(); i++) {
      numericValues[i] = toFiniteDouble(values[i], "${label} value at index ${i}")
    }
    numericValues
  }

  /**
   * Convert a value to a finite {@code double}.
   *
   * @param value the source value
   * @param label the label used in validation messages
   * @return the finite numeric value
   */
  static double toFiniteDouble(Object value, String label) {
    if (!(value instanceof Number)) {
      throw new IllegalArgumentException("${label.capitalize()} must be numeric")
    }
    double numericValue = value as double
    if (!Double.isFinite(numericValue)) {
      throw new IllegalArgumentException("${label.capitalize()} must be finite")
    }
    numericValue
  }
}
