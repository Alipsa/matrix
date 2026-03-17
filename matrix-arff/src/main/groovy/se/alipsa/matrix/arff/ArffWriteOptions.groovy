package se.alipsa.matrix.arff

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

import java.util.Locale

/**
 * Typed options for ARFF write operations via the SPI.
 */
@CompileStatic
class ArffWriteOptions {

  private static final int DEFAULT_NOMINAL_THRESHOLD = 50

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
    this.nominalColumns = value == null ? [] as Set<String> : stringSet(value, 'nominalColumns')
    this
  }

  ArffWriteOptions stringColumns(Collection<String> value) {
    this.stringColumns = value == null ? [] as Set<String> : stringSet(value, 'stringColumns')
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
    this.dateFormatsByColumn = value == null ? [:] : copyStringMap(value, 'dateFormatsByColumn')
    this
  }

  static ArffWriteOptions fromMap(Map<String, ?> options) {
    ArffWriteOptions result = new ArffWriteOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)

    if (normalized.containsKey('nominalmappings')) {
      result.nominalMappings(nominalMappingsValue(normalized.nominalmappings, 'nominalMappings'))
    }
    if (normalized.containsKey('infernominals')) {
      result.inferNominals(booleanValue(normalized.infernominals, 'inferNominals'))
    }
    if (normalized.containsKey('nominalthreshold')) {
      result.nominalThreshold(intValue(normalized.nominalthreshold, 'nominalThreshold'))
    }
    if (normalized.containsKey('nominalcolumns')) {
      result.nominalColumns(stringCollectionValue(normalized.nominalcolumns, 'nominalColumns'))
    }
    if (normalized.containsKey('stringcolumns')) {
      result.stringColumns(stringCollectionValue(normalized.stringcolumns, 'stringColumns'))
    }
    if (normalized.containsKey('attributetypesbycolumn')) {
      result.attributeTypesByColumn(attributeTypesValue(normalized.attributetypesbycolumn, 'attributeTypesByColumn'))
    }
    if (normalized.containsKey('dateformat')) {
      result.dateFormat(OptionMaps.stringValueOrNull(normalized.dateformat))
    }
    if (normalized.containsKey('dateformatsbycolumn')) {
      result.dateFormatsByColumn(stringMapValue(normalized.dateformatsbycolumn, 'dateFormatsByColumn'))
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
        new OptionDescriptor('nominalMappings', Map, null, 'Map of column names to explicit nominal values'),
        new OptionDescriptor('inferNominals', Boolean, true, 'Whether String/Object columns should be auto-detected as nominal using nominalThreshold and, for 10+ rows, a 10% row-count heuristic'),
        new OptionDescriptor('nominalThreshold', Integer, DEFAULT_NOMINAL_THRESHOLD, 'Maximum distinct values allowed before nominal inference falls back to STRING'),
        new OptionDescriptor('nominalColumns', Collection, null, 'Columns that should always be written as nominal'),
        new OptionDescriptor('stringColumns', Collection, null, 'Columns that should always be written as STRING'),
        new OptionDescriptor('attributeTypesByColumn', Map, null, 'Map of column names to ARFF type declarations such as STRING, NOMINAL, DATE, NUMERIC, INTEGER'),
        new OptionDescriptor('dateFormat', String, null, 'Global DATE format override for DATE attributes'),
        new OptionDescriptor('dateFormatsByColumn', Map, null, 'Per-column DATE format overrides')
    ]
  }

  private static Map<String, List<String>> nominalMappingsValue(Object value, String name) {
    if (!(value instanceof Map)) {
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
    if (!(value instanceof Map)) {
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
    if (!(value instanceof Map)) {
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
    if (value instanceof Collection) {
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
      result[key] = stringList(item, "nominalMappings[$key]")
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
        throw new IllegalArgumentException("attributeTypesByColumn[$key] must not be null")
      }
      result[key] = item
    }
    result
  }

  private static Set<String> stringSet(Collection<String> value, String name) {
    LinkedHashSet<String> result = [] as LinkedHashSet<String>
    value.each { String item ->
      result << requireString(item, name)
    }
    result
  }

  private static List<String> stringList(Object value, String name) {
    if (!(value instanceof Collection)) {
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

  private static int intValue(Object value, String name) {
    try {
      return new BigDecimal(String.valueOf(value)).intValueExact()
    } catch (Exception e) {
      throw new IllegalArgumentException("$name must be an integer but was $value", e)
    }
  }

  private static ArffTypeDecl attributeTypeValue(Object value, String name) {
    if (value instanceof ArffTypeDecl) {
      return (ArffTypeDecl) value
    }
    if (value instanceof CharSequence) {
      try {
        return ArffTypeDecl.valueOf(value.toString().trim().toUpperCase(Locale.ROOT))
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("$name must be one of ${ArffTypeDecl.values().toList()} but was $value", e)
      }
    }
    throw new IllegalArgumentException("$name must be an ArffTypeDecl or String but was ${value?.class}")
  }
}
