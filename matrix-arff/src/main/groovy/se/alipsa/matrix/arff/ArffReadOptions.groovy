package se.alipsa.matrix.arff

import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

/**
 * Typed options for ARFF read operations via the SPI.
 */
class ArffReadOptions {

  private static final String STRICT = 'strict'
  private static final String FAIL_ON_UNKNOWN_ATTRIBUTE_TYPE = 'failOnUnknownAttributeType'
  private static final String FAIL_ON_ROW_LENGTH_MISMATCH = 'failOnRowLengthMismatch'

  private String fallbackMatrixName = null
  private boolean strict = false
  private Boolean failOnUnknownAttributeType = null
  private Boolean failOnRowLengthMismatch = null

  String getFallbackMatrixName() {
    fallbackMatrixName
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

  ArffReadOptions fallbackMatrixName(String value) {
    this.fallbackMatrixName = value
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
    if (normalized.containsKey('fallbackmatrixname')) {
      String fallbackMatrixName = OptionMaps.stringValueOrNull(normalized.fallbackmatrixname)
      if (fallbackMatrixName != null) {
        result.fallbackMatrixName(fallbackMatrixName)
      }
    }
    if (normalized.containsKey(STRICT)) {
      result.strict(ArffOptionValues.booleanValue(normalized.strict, STRICT))
    }
    if (normalized.containsKey('failonunknownattributetype')) {
      result.failOnUnknownAttributeType(ArffOptionValues.booleanValue(normalized.failonunknownattributetype, FAIL_ON_UNKNOWN_ATTRIBUTE_TYPE))
    }
    if (normalized.containsKey('failonrowlengthmismatch')) {
      result.failOnRowLengthMismatch(ArffOptionValues.booleanValue(normalized.failonrowlengthmismatch, FAIL_ON_ROW_LENGTH_MISMATCH))
    }
    result
  }

  Map<String, ?> toMap() {
    Map<String, Object> result = [:]
    if (fallbackMatrixName != null) {
      result.fallbackMatrixName = fallbackMatrixName
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
        new OptionDescriptor('fallbackMatrixName', String, null, 'Fallback Matrix name when the ARFF file has no @RELATION'),
        new OptionDescriptor(STRICT, Boolean, false, 'Enable fail-fast validation for unknown attribute types and row length mismatches unless overridden by specific options'),
        new OptionDescriptor(FAIL_ON_UNKNOWN_ATTRIBUTE_TYPE, Boolean, STRICT, 'Fail when an unknown @ATTRIBUTE type is encountered instead of falling back to STRING'),
        new OptionDescriptor(FAIL_ON_ROW_LENGTH_MISMATCH, Boolean, STRICT, 'Fail when a dense @DATA row has more or fewer values than the declared attributes')
    ]
  }

}
