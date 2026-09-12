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
    DecimalColumnProfile result = new DecimalColumnProfile(0, 0, false)
    values.each { Number value ->
      result = result.include(value)
    }
    result
  }

  /**
   * Returns a profile that also accommodates the supplied decimal-compatible value.
   * Null values leave this profile unchanged, allowing callers to profile streams
   * without materializing every value.
   *
   * @param value decimal-compatible value to include, or null
   * @return a profile that can represent this profile's values and {@code value}
   */
  DecimalColumnProfile include(Number value) {
    if (value == null) {
      return this
    }
    BigDecimal decimal = value instanceof BigDecimal
        ? value as BigDecimal
        : new BigDecimal(value.toString())
    int integerDigits = Math.max(1, decimal.precision() - decimal.scale())
    int maxScale = Math.max(scale, decimal.scale())
    int maxIntegerDigits = Math.max(hasValues ? precision - scale : 0, integerDigits)
    new DecimalColumnProfile(maxIntegerDigits + maxScale, maxScale, true)
  }
}
