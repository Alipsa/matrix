package se.alipsa.matrix.arff

import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Typed options for ARFF read operations via the SPI.
 */
class ArffReadOptions {

  private static final String STRICT = 'strict'
  private static final String FAIL_ON_UNKNOWN_ATTRIBUTE_TYPE = 'failOnUnknownAttributeType'
  private static final String FAIL_ON_ROW_LENGTH_MISMATCH = 'failOnRowLengthMismatch'
  private static final String FAIL_ON_UNDECLARED_NOMINAL_VALUE = 'failOnUndeclaredNominalValue'
  private static final String OMITTED_STRING_FALLBACK = 'omittedStringFallback'
  private static final String INSTANCE_WEIGHT_COLUMN = 'instanceWeightColumn'
  private static final String INTEGER_TYPE = 'integerType'
  private static final String CHARSET = 'charset'
  private static final String DATE_MODE = 'dateMode'

  private String fallbackMatrixName = null
  private boolean strict = false
  private Boolean failOnUnknownAttributeType = null
  private Boolean failOnRowLengthMismatch = null
  private Boolean failOnUndeclaredNominalValue = null
  private String omittedStringFallback = null
  private String instanceWeightColumn = null
  private Class<? extends Number> integerType = Integer
  private Charset charset = StandardCharsets.UTF_8
  private ArffDateMode dateMode = ArffDateMode.UTC

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

  boolean isFailOnUndeclaredNominalValue() {
    failOnUndeclaredNominalValue == null ? strict : failOnUndeclaredNominalValue.booleanValue()
  }

  /** Value of a STRING cell omitted from a sparse row when the column has no explicit value at all; null by default. */
  String getOmittedStringFallback() {
    omittedStringFallback
  }

  /** Name of the NUMERIC column that receives ARFF instance weights, or null when weights are discarded. */
  String getInstanceWeightColumn() {
    instanceWeightColumn
  }

  /** How DATE values are parsed; {@link ArffDateMode#UTC} by default. */
  ArffDateMode getDateMode() {
    dateMode
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

  ArffReadOptions failOnUndeclaredNominalValue(boolean value) {
    this.failOnUndeclaredNominalValue = value
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

  /**
   * Store ARFF instance weights ({@code {w}} after a data row) in a NUMERIC column with this name; rows without a
   * weight get {@code 1}. Null (the default) parses and discards weights. The name is reserved in every relation of
   * the file, including the sub-relations of relational attributes (their nested rows may be weighted as well), so an
   * {@code @ATTRIBUTE} with this name at any depth is rejected; choose a name that no attribute in the file uses.
   */
  ArffReadOptions instanceWeightColumn(String value) {
    this.instanceWeightColumn = value
    this
  }

  /**
   * Choose {@link ArffDateMode#WEKA} to parse DATE values with the JVM's default time zone and locale exactly
   * as Weka does on the same machine; null resets to the default {@link ArffDateMode#UTC}.
   */
  ArffReadOptions dateMode(ArffDateMode value) {
    this.dateMode = value == null ? ArffDateMode.UTC : value
    this
  }

  /** Java type of INTEGER attributes; Integer by default. */
  Class<? extends Number> getIntegerType() {
    integerType
  }

  /** Accept Integer (default), Long, or BigDecimal; null resets to Integer. */
  ArffReadOptions integerType(Class<? extends Number> value) {
    if (value != null && !(value in [Integer, Long, BigDecimal])) {
      throw new IllegalArgumentException('integerType must be Integer, Long or BigDecimal')
    }
    integerType = value ?: Integer
    this
  }

  /** Character encoding for byte streams; UTF-8 by default. */
  Charset getCharset() {
    charset
  }

  /** Set the stream charset; null resets to UTF-8. Writer/String entry points do not use this option. */
  ArffReadOptions charset(Charset value) {
    charset = value ?: StandardCharsets.UTF_8
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
    if (normalized.containsKey('failonundeclarednominalvalue')) {
      result.failOnUndeclaredNominalValue(ArffOptionValues.booleanValue(normalized.failonundeclarednominalvalue, FAIL_ON_UNDECLARED_NOMINAL_VALUE))
    }
    if (normalized.containsKey('omittedstringfallback')) {
      result.omittedStringFallback(OptionMaps.stringValueOrNull(normalized.omittedstringfallback))
    }
    if (normalized.containsKey('instanceweightcolumn')) {
      result.instanceWeightColumn(OptionMaps.stringValueOrNull(normalized.instanceweightcolumn))
    }
    if (normalized.containsKey('datemode')) {
      result.dateMode(ArffOptionValues.enumValue(normalized.datemode, ArffDateMode, DATE_MODE))
    }
    if (normalized.containsKey('integertype')) {
      result.integerType(ArffOptionValues.integerTypeValue(normalized.integertype))
    }
    if (normalized.containsKey(CHARSET)) {
      result.charset(ArffOptionValues.charsetValue(normalized[CHARSET]))
    }
    result
  }

  Map<String, ?> toMap() {
    Map<String, Object> result = [:]
    if (integerType != Integer) {
      result[INTEGER_TYPE] = integerType
    }
    if (charset != StandardCharsets.UTF_8) {
      result[CHARSET] = charset
    }
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
    if (failOnUndeclaredNominalValue != null) {
      result.failOnUndeclaredNominalValue = failOnUndeclaredNominalValue
    }
    if (omittedStringFallback != null) {
      result.omittedStringFallback = omittedStringFallback
    }
    if (instanceWeightColumn != null) {
      result.instanceWeightColumn = instanceWeightColumn
    }
    if (dateMode != ArffDateMode.UTC) {
      result.dateMode = dateMode
    }
    result
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor(INTEGER_TYPE, Class, Integer, 'INTEGER Java type: Integer, Long or BigDecimal (Class or case-insensitive simple/fully-qualified name)'),
        new OptionDescriptor(CHARSET, Charset, StandardCharsets.UTF_8, 'Byte-stream charset (Charset or name); malformed/unmappable text is rejected'),
        new OptionDescriptor('fallbackMatrixName', String, null, 'Fallback Matrix name when the ARFF file has no @RELATION'),
        new OptionDescriptor(STRICT, Boolean, false, 'Enable fail-fast validation for unknown attribute types, row length mismatches and undeclared nominal values unless overridden by specific options'),
        new OptionDescriptor(FAIL_ON_UNKNOWN_ATTRIBUTE_TYPE, Boolean, STRICT, 'Fail when an unknown @ATTRIBUTE type is encountered instead of falling back to STRING'),
        new OptionDescriptor(FAIL_ON_ROW_LENGTH_MISMATCH, Boolean, STRICT, 'Fail when a dense @DATA row has more or fewer values than the declared attributes'),
        new OptionDescriptor(FAIL_ON_UNDECLARED_NOMINAL_VALUE, Boolean, STRICT, 'Fail when a nominal data value is not in the attribute declaration, as Weka does'),
        new OptionDescriptor(OMITTED_STRING_FALLBACK, String, null, 'Value for a STRING attribute omitted from sparse rows when the column has no explicit value to resolve to (Weka dictionary index 0); null when unset, \'0\' matches Weka\'s raw value and liac-arff'),
        new OptionDescriptor(INSTANCE_WEIGHT_COLUMN, String, null, 'Name of a NUMERIC column that receives ARFF instance weights ({w} after a row, 1 when absent) in every relation including relational sub-relations; weights are discarded when unset'),
        new OptionDescriptor(DATE_MODE, ArffDateMode, ArffDateMode.UTC, 'UTC (machine-independent, whole value must match) or WEKA (JVM default time zone and locale, trailing text ignored, as weka.core.Attribute) for parsing DATE values')
    ]
  }

}
