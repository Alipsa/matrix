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
  private static final String OMITTED_STRING_FALLBACK = 'omittedStringFallback'

  private String fallbackMatrixName = null
  private boolean strict = false
  private Boolean failOnUnknownAttributeType = null
  private Boolean failOnRowLengthMismatch = null
  private String omittedStringFallback = null

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

  /** Value of a STRING cell omitted from a sparse row when the column has no explicit value at all; null by default. */
  String getOmittedStringFallback() {
    omittedStringFallback
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

  /**
   * A STRING attribute omitted from a sparse row takes the first explicit value in that column (Weka's string
   * dictionary index 0). When the column never has an explicit value there is no such entry: the cell is null by
   * default, or this value when set — {@code '0'} reproduces the raw value Weka holds and the string liac-arff yields.
   */
  ArffReadOptions omittedStringFallback(String value) {
    this.omittedStringFallback = value
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
    if (normalized.containsKey('omittedstringfallback')) {
      result.omittedStringFallback(OptionMaps.stringValueOrNull(normalized.omittedstringfallback))
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
    if (omittedStringFallback != null) {
      result.omittedStringFallback = omittedStringFallback
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
        new OptionDescriptor(FAIL_ON_ROW_LENGTH_MISMATCH, Boolean, STRICT, 'Fail when a dense @DATA row has more or fewer values than the declared attributes'),
        new OptionDescriptor(OMITTED_STRING_FALLBACK, String, null, 'Value for a STRING attribute omitted from sparse rows when the column has no explicit value to resolve to (Weka dictionary index 0); null when unset, \'0\' matches Weka\'s raw value and liac-arff')
    ]
  }

}
