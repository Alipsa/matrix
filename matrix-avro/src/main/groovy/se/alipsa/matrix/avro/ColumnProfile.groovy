package se.alipsa.matrix.avro

import groovy.transform.PackageScope

import se.alipsa.matrix.core.util.DecimalColumnProfile

/**
 * Inferred column characteristics used when building Avro schemas.
 */
@PackageScope
final class ColumnProfile {

  final String name
  final Class<?> declaredType
  Class<?> effectiveType
  Class<?> listElemClass
  Class<?> mapValueClass
  boolean recordLike = false
  boolean recordSeen = false
  Map recordSample
  Set<String> recordKeys
  DecimalColumnProfile decimalProfile
  NestedNumericProfile listNumericProfile
  NestedNumericProfile mapValueNumericProfile
  Map<String, Class<?>> recordFieldClasses = [:]
  Map<String, NestedNumericProfile> recordNumericProfiles = [:]
  boolean forceDecimal = false
  boolean inferPrecisionAndScale = false
  boolean listHasNonNumeric = false
  boolean mapValuesHaveNonNumeric = false
  Map<String, Boolean> recordHasNonNumeric = [:]
  ColumnProfile(String name, Class<?> declaredType) {
    this.name = name
    this.declaredType = declaredType
  }
  int[] decimalMeta() {
    if (decimalProfile == null || !decimalProfile.hasValues) {
      return [10, 0] as int[]
    }
    [decimalProfile.precision, decimalProfile.scale] as int[]
  }

}

/**
 * Incremental numeric characteristics for one nested collection position.
 */
@PackageScope
final class NestedNumericProfile {

  DecimalColumnProfile decimalProfile
  boolean hasBigInteger
  boolean hasBigDecimal
  boolean hasFloating
  boolean needsLong

  void include(Number value, BigDecimal decimal) {
    decimalProfile = decimalProfile == null
        ? DecimalColumnProfile.profile([decimal])
        : decimalProfile.include(decimal)
    hasBigInteger |= BigInteger.isInstance(value)
    hasBigDecimal |= BigDecimal.isInstance(value)
    hasFloating |= Double.isInstance(value) || Float.isInstance(value)
    needsLong |= Long.isInstance(value) || (isIntegral(value) &&
        (value.longValue() < Integer.MIN_VALUE || value.longValue() > Integer.MAX_VALUE))
  }

  Class<?> schemaClass() {
    if (hasBigInteger && !hasBigDecimal && !hasFloating) {
      return BigInteger
    }
    if (hasBigInteger || hasBigDecimal) {
      return BigDecimal
    }
    if (hasFloating) {
      return Double
    }
    needsLong ? Long : Integer
  }

  private static boolean isIntegral(Number value) {
    Byte.isInstance(value) || Short.isInstance(value) || Integer.isInstance(value) ||
        Long.isInstance(value) || BigInteger.isInstance(value)
  }
}
