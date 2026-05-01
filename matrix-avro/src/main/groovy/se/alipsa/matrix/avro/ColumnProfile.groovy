package se.alipsa.matrix.avro

import groovy.transform.PackageScope

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
  boolean sawDecimal = false
  int maxIntegerDigits = 0
  int maxScale = 0
  ColumnProfile(String name, Class<?> declaredType) {
    this.name = name
    this.declaredType = declaredType
  }
  int[] decimalMeta() {
    if (!sawDecimal) {
      return [10, 0] as int[]
    }
    int scale = Math.max(0, maxScale)
    int precision = Math.max(1, maxIntegerDigits + scale)
    [precision, scale] as int[]
  }

}
