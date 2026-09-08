package se.alipsa.matrix.core.util

/**
 * Precision and scale inferred from the decimal values in a column.
 *
 * <pre>{@code
 * DecimalColumnProfile profile = DecimalColumnProfile.profile([123.4g, 0.001g])
 * assert profile.precision == 6
 * assert profile.scale == 3
 * }</pre>
 */
final class DecimalColumnProfile {

  final int precision
  final int scale
  final boolean hasValues

  private DecimalColumnProfile(int precision, int scale, boolean hasValues) {
    this.precision = precision
    this.scale = scale
    this.hasValues = hasValues
  }

  /**
   * Profiles decimal-compatible values as a precision and scale pair capable of holding
   * every supplied value. Null values are ignored.
   *
   * @param values decimal-compatible column values
   * @return the inferred profile, or precision and scale zero when no values are present
   */
  static DecimalColumnProfile profile(Iterable<? extends Number> values) {
    int maxIntegerDigits = 0
    int maxScale = 0
    boolean hasValues = false
    values.each { Number value ->
      if (value != null) {
        hasValues = true
        BigDecimal decimal = value instanceof BigDecimal
            ? value as BigDecimal
            : new BigDecimal(value.toString())
        int integerDigits = Math.max(1, decimal.precision() - decimal.scale())
        maxIntegerDigits = Math.max(maxIntegerDigits, integerDigits)
        maxScale = Math.max(maxScale, decimal.scale())
      }
    }
    new DecimalColumnProfile(maxIntegerDigits + maxScale, maxScale, hasValues)
  }
}
