package se.alipsa.matrix.arff

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

import java.util.Locale

/**
 * Typed options for ARFF read operations via the SPI.
 */
@CompileStatic
class ArffReadOptions {

  private String matrixName = null
  private boolean strict = false
  private Boolean failOnUnknownAttributeType = null
  private Boolean failOnRowLengthMismatch = null

  String getMatrixName() {
    matrixName
  }

  boolean isStrict() {
    strict
  }

  boolean isFailOnUnknownAttributeType() {
    failOnUnknownAttributeType == null ? strict : failOnUnknownAttributeType.booleanValue()
  }

  boolean isFailOnRowLengthMismatch() {
    failOnRowLengthMismatch == null ? strict : failOnRowLengthMismatch.booleanValue()
  }

  ArffReadOptions matrixName(String value) {
    this.matrixName = value
    this
  }

  ArffReadOptions strict(boolean value) {
    this.strict = value
    this
  }

  ArffReadOptions failOnUnknownAttributeType(boolean value) {
    this.failOnUnknownAttributeType = value
    this
  }

  ArffReadOptions failOnRowLengthMismatch(boolean value) {
    this.failOnRowLengthMismatch = value
    this
  }

  static ArffReadOptions fromMap(Map<String, ?> options) {
    ArffReadOptions result = new ArffReadOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    if (normalized.containsKey('matrixname')) {
      String matrixName = OptionMaps.stringValueOrNull(normalized.matrixname)
      if (matrixName != null) {
        result.matrixName(matrixName)
      }
    }
    if (normalized.containsKey('strict')) {
      result.strict(booleanValue(normalized.strict, 'strict'))
    }
    if (normalized.containsKey('failonunknownattributetype')) {
      result.failOnUnknownAttributeType(booleanValue(normalized.failonunknownattributetype, 'failOnUnknownAttributeType'))
    }
    if (normalized.containsKey('failonrowlengthmismatch')) {
      result.failOnRowLengthMismatch(booleanValue(normalized.failonrowlengthmismatch, 'failOnRowLengthMismatch'))
    }
    result
  }

  Map<String, ?> toMap() {
    Map<String, Object> result = [:]
    if (matrixName != null) {
      result.matrixName = matrixName
    }
    if (strict) {
      result.strict = true
    }
    if (failOnUnknownAttributeType != null) {
      result.failOnUnknownAttributeType = failOnUnknownAttributeType
    }
    if (failOnRowLengthMismatch != null) {
      result.failOnRowLengthMismatch = failOnRowLengthMismatch
    }
    result
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('matrixName', String, null, 'Fallback Matrix name when the ARFF file has no @RELATION'),
        new OptionDescriptor('strict', Boolean, false, 'Enable fail-fast validation for unknown attribute types and row length mismatches unless overridden by specific options'),
        new OptionDescriptor('failOnUnknownAttributeType', Boolean, 'strict', 'Fail when an unknown @ATTRIBUTE type is encountered instead of falling back to STRING'),
        new OptionDescriptor('failOnRowLengthMismatch', Boolean, 'strict', 'Fail when a dense @DATA row has more or fewer values than the declared attributes')
    ]
  }

  private static boolean booleanValue(Object value, String name) {
    if (value instanceof Boolean) {
      return (Boolean) value
    }
    if (value instanceof CharSequence) {
      String normalized = value.toString().trim().toLowerCase(Locale.ROOT)
      if (normalized == 'true') {
        return true
      }
      if (normalized == 'false') {
        return false
      }
    }
    throw new IllegalArgumentException("$name must be a boolean but was ${value?.class}")
  }
}
