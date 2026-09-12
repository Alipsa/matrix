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
  DecimalColumnProfile listDecimalProfile
  DecimalColumnProfile mapValueDecimalProfile
  Map<String, Class<?>> recordFieldClasses = [:]
  Map<String, DecimalColumnProfile> recordDecimalProfiles = [:]
  boolean forceDecimal = false
  boolean inferPrecisionAndScale = false
  List<Number> listNumbers = []
  boolean listHasNonNumeric = false
  List<Number> mapValueNumbers = []
  boolean mapValuesHaveNonNumeric = false
  Map<String, List<Number>> recordNumbers = [:]
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
