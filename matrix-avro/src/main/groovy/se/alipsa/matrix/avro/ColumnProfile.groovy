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
