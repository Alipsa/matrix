package se.alipsa.matrix.avro

import groovy.transform.PackageScope

/**
 * Shared predicates for classifying numeric values during schema inference and conversion.
 */
@PackageScope
final class NumericKinds {

  private NumericKinds() {
  }

  /**
   * @param value the value to classify (may be null)
   * @return true for Byte, Short, Integer, Long, and BigInteger values
   */
  static boolean isIntegral(Object value) {
    Byte.isInstance(value) || Short.isInstance(value) || Integer.isInstance(value)
        || Long.isInstance(value) || BigInteger.isInstance(value)
  }

  /**
   * @param value the value to classify (may be null)
   * @return true for Byte, Short, Integer, and Long values, which convert to long directly
   */
  static boolean isDirectLong(Object value) {
    Byte.isInstance(value) || Short.isInstance(value) || Integer.isInstance(value) || Long.isInstance(value)
  }

  /**
   * @param value the value to classify (may be null)
   * @return true for Byte, Short, and Integer values, which convert to int directly
   */
  static boolean isDirectInt(Object value) {
    Byte.isInstance(value) || Short.isInstance(value) || Integer.isInstance(value)
  }

  /**
   * @param value the integral value to classify
   * @return true when the value requires a 64-bit integral schema (Long or BigInteger,
   * or an integral value outside the 32-bit range)
   */
  static boolean needsLongStorage(Number value) {
    Long.isInstance(value) || BigInteger.isInstance(value)
        || (isIntegral(value) && (value.longValue() < Integer.MIN_VALUE || value.longValue() > Integer.MAX_VALUE))
  }

  /**
   * @param value the floating value to classify
   * @return true for {@link Double} or {@link Float} values that are NaN or infinite
   */
  static boolean isNonFiniteFloating(Number value) {
    (Double.isInstance(value) && !Double.isFinite((Double) value)) ||
        (Float.isInstance(value) && !Float.isFinite((Float) value))
  }

  /**
   * Converts a finite number to {@link BigDecimal}.
   *
   * <p>Callers must reject non-finite floating values before calling this method.
   *
   * @param value the finite value to convert
   * @return the corresponding BigDecimal
   */
  static BigDecimal toBigDecimal(Number value) {
    BigDecimal.isInstance(value) ? (BigDecimal) value : new BigDecimal(value.toString())
  }
}
