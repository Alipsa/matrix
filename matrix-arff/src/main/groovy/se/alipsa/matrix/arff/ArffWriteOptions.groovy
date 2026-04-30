package se.alipsa.matrix.arff

import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

/**
 * Typed options for ARFF write operations via the SPI.
 */
class ArffWriteOptions {

  private static final int DEFAULT_NOMINAL_THRESHOLD = 50
  private static final String NOMINAL_MAPPINGS = 'nominalMappings'
  private static final String INFER_NOMINALS = 'inferNominals'
  private static final String NOMINAL_THRESHOLD = 'nominalThreshold'
  private static final String NOMINAL_COLUMNS = 'nominalColumns'
  private static final String STRING_COLUMNS = 'stringColumns'
  private static final String ATTRIBUTE_TYPES_BY_COLUMN = 'attributeTypesByColumn'
  private static final String DATE_FORMATS_BY_COLUMN = 'dateFormatsByColumn'

  private Map<String, List<String>> nominalMappings = [:]
  private boolean inferNominals = true
  private int nominalThreshold = DEFAULT_NOMINAL_THRESHOLD
  private Set<String> nominalColumns = [] as Set<String>
  private Set<String> stringColumns = [] as Set<String>
  private Map<String, ArffTypeDecl> attributeTypesByColumn = [:]
  private String dateFormat = null
  private Map<String, String> dateFormatsByColumn = [:]

  Map<String, List<String>> getNominalMappings() {
    nominalMappings.asImmutable()
  }

  boolean isInferNominals() {
    inferNominals
  }

  int getNominalThreshold() {
    nominalThreshold
  }

  Set<String> getNominalColumns() {
    nominalColumns.asImmutable()
  }

  Set<String> getStringColumns() {
    stringColumns.asImmutable()
  }

  Map<String, ArffTypeDecl> getAttributeTypesByColumn() {
    attributeTypesByColumn.asImmutable()
  }

  String getDateFormat() {
    dateFormat
  }

  Map<String, String> getDateFormatsByColumn() {
    dateFormatsByColumn.asImmutable()
  }

  ArffWriteOptions nominalMappings(Map<String, List<String>> value) {
    this.nominalMappings = value == null ? [:] : copyNominalMappings(value)
    this
  }

  ArffWriteOptions inferNominals(boolean value) {
    this.inferNominals = value
    this
  }

  ArffWriteOptions nominalThreshold(int value) {
    if (value < 0) {
      throw new IllegalArgumentException("nominalThreshold must be >= 0 but was $value")
    }
    this.nominalThreshold = value
    this
  }

  ArffWriteOptions nominalColumns(Collection<String> value) {
    this.nominalColumns = value == null ? [] as Set<String> : stringSet(value, NOMINAL_COLUMNS)
    this
  }

  ArffWriteOptions stringColumns(Collection<String> value) {
    this.stringColumns = value == null ? [] as Set<String> : stringSet(value, STRING_COLUMNS)
    this
  }

  ArffWriteOptions attributeTypesByColumn(Map<String, ArffTypeDecl> value) {
    this.attributeTypesByColumn = value == null ? [:] : copyAttributeTypes(value)
    this
  }

  ArffWriteOptions dateFormat(String value) {
    this.dateFormat = value
    this
  }

  ArffWriteOptions dateFormatsByColumn(Map<String, String> value) {
    this.dateFormatsByColumn = value == null ? [:] : copyStringMap(value, DATE_FORMATS_BY_COLUMN)
    this
  }

  static ArffWriteOptions fromMap(Map<String, ?> options) {
    ArffWriteOptions result = new ArffWriteOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)

    if (normalized.containsKey('nominalmappings')) {
      result.nominalMappings(nominalMappingsValue(normalized.nominalmappings, NOMINAL_MAPPINGS))
    }
    if (normalized.containsKey('infernominals')) {
      result.inferNominals(ArffOptionValues.booleanValue(normalized.infernominals, INFER_NOMINALS))
    }
    if (normalized.containsKey('nominalthreshold')) {
      result.nominalThreshold(intValue(normalized.nominalthreshold, NOMINAL_THRESHOLD))
    }
    if (normalized.containsKey('nominalcolumns')) {
      result.nominalColumns(stringCollectionValue(normalized.nominalcolumns, NOMINAL_COLUMNS))
    }
    if (normalized.containsKey('stringcolumns')) {
      result.stringColumns(stringCollectionValue(normalized.stringcolumns, STRING_COLUMNS))
    }
    if (normalized.containsKey('attributetypesbycolumn')) {
      result.attributeTypesByColumn(attributeTypesValue(normalized.attributetypesbycolumn, ATTRIBUTE_TYPES_BY_COLUMN))
    }
    if (normalized.containsKey('dateformat')) {
      result.dateFormat(OptionMaps.stringValueOrNull(normalized.dateformat))
    }
    if (normalized.containsKey('dateformatsbycolumn')) {
      result.dateFormatsByColumn(stringMapValue(normalized.dateformatsbycolumn, DATE_FORMATS_BY_COLUMN))
    }

    result
  }

  Map<String, ?> toMap() {
    Map<String, Object> result = [:]
    if (!nominalMappings.isEmpty()) {
      result.nominalMappings = nominalMappings
    }
    if (!inferNominals) {
      result.inferNominals = false
    }
    if (nominalThreshold != DEFAULT_NOMINAL_THRESHOLD) {
      result.nominalThreshold = nominalThreshold
    }
    if (!nominalColumns.isEmpty()) {
      result.nominalColumns = nominalColumns
    }
    if (!stringColumns.isEmpty()) {
      result.stringColumns = stringColumns
    }
    if (!attributeTypesByColumn.isEmpty()) {
      result.attributeTypesByColumn = attributeTypesByColumn
    }
    if (dateFormat != null) {
      result.dateFormat = dateFormat
    }
    if (!dateFormatsByColumn.isEmpty()) {
      result.dateFormatsByColumn = dateFormatsByColumn
    }
    result
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor(NOMINAL_MAPPINGS, Map, null, 'Map of column names to explicit nominal values'),
        new OptionDescriptor(INFER_NOMINALS, Boolean, true, 'Whether String/Object columns should be auto-detected as nominal using nominalThreshold and, for 10+ rows, a 10% row-count heuristic'),
        new OptionDescriptor(NOMINAL_THRESHOLD, Integer, DEFAULT_NOMINAL_THRESHOLD, 'Maximum distinct values allowed before nominal inference falls back to STRING'),
        new OptionDescriptor(NOMINAL_COLUMNS, Collection, null, 'Columns that should always be written as nominal'),
        new OptionDescriptor(STRING_COLUMNS, Collection, null, 'Columns that should always be written as STRING'),
        new OptionDescriptor(ATTRIBUTE_TYPES_BY_COLUMN, Map, null, 'Map of column names to ARFF type declarations such as STRING, NOMINAL, DATE, NUMERIC, INTEGER'),
        new OptionDescriptor('dateFormat', String, null, 'Global DATE format override for DATE attributes'),
        new OptionDescriptor(DATE_FORMATS_BY_COLUMN, Map, null, 'Per-column DATE format overrides')
    ]
  }

  private static Map<String, List<String>> nominalMappingsValue(Object value, String name) {
    if (!Map.isInstance(value)) {
      throw new IllegalArgumentException("$name must be a Map<String, List<String>> but was ${value?.class}")
    }
    Map<String, List<String>> result = [:]
    ((Map<?, ?>) value).each { key, item ->
      String columnName = String.valueOf(key)
      result[columnName] = stringList(item, "$name[$columnName]")
    }
    result
  }

  private static Map<String, String> stringMapValue(Object value, String name) {
    if (!Map.isInstance(value)) {
      throw new IllegalArgumentException("$name must be a Map<String, String> but was ${value?.class}")
    }
    Map<String, String> result = [:]
    ((Map<?, ?>) value).each { key, item ->
      String columnName = String.valueOf(key)
      result[columnName] = requireString(item, "$name[$columnName]")
    }
    result
  }

  private static Map<String, ArffTypeDecl> attributeTypesValue(Object value, String name) {
    if (!Map.isInstance(value)) {
      throw new IllegalArgumentException("$name must be a Map<String, ArffTypeDecl> but was ${value?.class}")
    }
    Map<String, ArffTypeDecl> result = [:]
    ((Map<?, ?>) value).each { key, item ->
      String columnName = String.valueOf(key)
      result[columnName] = attributeTypeValue(item, "$name[$columnName]")
    }
    result
  }

  private static Collection<String> stringCollectionValue(Object value, String name) {
    if (Collection.isInstance(value)) {
      return ((Collection<?>) value).collect { Object item -> requireString(item, name) } as List<String>
    }
    if (value != null && value.getClass().array) {
      return (value as List<?>).collect { Object item -> requireString(item, name) } as List<String>
    }
    throw new IllegalArgumentException("$name must be a Collection<String> but was ${value?.class}")
  }

  private static Map<String, List<String>> copyNominalMappings(Map<String, List<String>> value) {
    Map<String, List<String>> result = [:]
    value.each { String key, List<String> item ->
      result[key] = stringList(item, "$NOMINAL_MAPPINGS[$key]")
    }
    result
  }

  private static Map<String, String> copyStringMap(Map<String, String> value, String name) {
    Map<String, String> result = [:]
    value.each { String key, String item ->
      result[key] = requireString(item, "$name[$key]")
    }
    result
  }

  private static Map<String, ArffTypeDecl> copyAttributeTypes(Map<String, ArffTypeDecl> value) {
    Map<String, ArffTypeDecl> result = [:]
    value.each { String key, ArffTypeDecl item ->
      if (item == null) {
        throw new IllegalArgumentException("$ATTRIBUTE_TYPES_BY_COLUMN[$key] must not be null")
      }
      result[key] = item
    }
    result
  }

  private static Set<String> stringSet(Collection<String> value, String name) {
    Set<String> result = new LinkedHashSet<String>()
    value.each { String item ->
      result << requireString(item, name)
    }
    result
  }

  private static List<String> stringList(Object value, String name) {
    if (!Collection.isInstance(value)) {
      throw new IllegalArgumentException("$name must be a Collection<String> but was ${value?.class}")
    }
    List<String> result = []
    ((Collection<?>) value).each { Object item ->
      result << requireString(item, name)
    }
    result
  }

  private static String requireString(Object value, String name) {
    String stringValue = OptionMaps.stringValueOrNull(value)
    if (stringValue == null) {
      throw new IllegalArgumentException("$name must not contain null values")
    }
    stringValue
  }

  private static int intValue(Object value, String name) {
    try {
      return new BigDecimal(String.valueOf(value)).intValueExact()
    } catch (Exception e) {
      throw new IllegalArgumentException("$name must be an integer but was $value", e)
    }
  }

  private static ArffTypeDecl attributeTypeValue(Object value, String name) {
    if (ArffTypeDecl.isInstance(value)) {
      return (ArffTypeDecl) value
    }
    if (CharSequence.isInstance(value)) {
      try {
        return ArffTypeDecl.valueOf(value.toString().trim().toUpperCase(java.util.Locale.ROOT))
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("$name must be one of ${ArffTypeDecl.values().toList()} but was $value", e)
      }
    }
    throw new IllegalArgumentException("$name must be an ArffTypeDecl or String but was ${value?.class}")
  }

}
