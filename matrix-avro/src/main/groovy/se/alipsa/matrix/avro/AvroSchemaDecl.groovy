package se.alipsa.matrix.avro

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.apache.avro.Schema
import se.alipsa.matrix.core.spi.OptionMaps

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/**
 * Typed declaration for explicit Avro schema overrides on Matrix columns.
 *
 * <p>Use the static factory methods to define a fixed decimal schema, force an
 * array or map value type, or force a record schema for record-like map columns.
 * Instances can be round-tripped through SPI option maps by calling {@link AvroSchemaDecl#toMap()}
 * and {@link AvroSchemaDecl#fromMap(java.util.Map)}.
 */
@CompileStatic
abstract class AvroSchemaDecl {

  private static final Map<Class<?>, AvroScalarTypeDecl> SCALAR_TYPE_BY_CLASS = [
      (String)        : AvroScalarTypeDecl.STRING,
      (Boolean)       : AvroScalarTypeDecl.BOOLEAN,
      (boolean.class) : AvroScalarTypeDecl.BOOLEAN,
      // Avro has no dedicated BYTE or SHORT scalar, so both widen to INT on write.
      (Integer)       : AvroScalarTypeDecl.INT,
      (int.class)     : AvroScalarTypeDecl.INT,
      (Short)         : AvroScalarTypeDecl.INT,
      (short.class)   : AvroScalarTypeDecl.INT,
      (Byte)          : AvroScalarTypeDecl.INT,
      (byte.class)    : AvroScalarTypeDecl.INT,
      (Long)          : AvroScalarTypeDecl.LONG,
      (long.class)    : AvroScalarTypeDecl.LONG,
      (BigInteger)    : AvroScalarTypeDecl.LONG,
      (Float)         : AvroScalarTypeDecl.FLOAT,
      (float.class)   : AvroScalarTypeDecl.FLOAT,
      (Double)        : AvroScalarTypeDecl.DOUBLE,
      (double.class)  : AvroScalarTypeDecl.DOUBLE,
      (byte[].class)  : AvroScalarTypeDecl.BYTES,
      (LocalDate)     : AvroScalarTypeDecl.DATE,
      (java.sql.Date) : AvroScalarTypeDecl.DATE,
      (LocalTime)     : AvroScalarTypeDecl.TIME_MILLIS,
      (java.sql.Time) : AvroScalarTypeDecl.TIME_MILLIS,
      (Instant)       : AvroScalarTypeDecl.TIMESTAMP_MILLIS,
      (Date)          : AvroScalarTypeDecl.TIMESTAMP_MILLIS,
      (LocalDateTime) : AvroScalarTypeDecl.LOCAL_TIMESTAMP_MICROS,
      (UUID)          : AvroScalarTypeDecl.UUID
  ].asImmutable()

  /**
   * Creates a scalar schema declaration.
   *
   * @param scalarType the scalar Avro type to use
   * @return a scalar schema declaration
   */
  static AvroSchemaDecl scalar(AvroScalarTypeDecl scalarType) {
    if (scalarType == null) {
      throw new IllegalArgumentException('scalarType must not be null')
    }
    new ScalarAvroSchemaDecl(scalarType)
  }

  /**
   * Creates a scalar schema declaration from a supported Java type.
   *
   * <p>Use {@link #decimal(int, int)} for {@link BigDecimal} instead of this method.
   * Use {@link #array(AvroSchemaDecl)}, {@link #map(AvroSchemaDecl)}, or
   * {@link #record(Map)} for nested collection types.
   *
   * @param javaType the Java type to map to an Avro scalar declaration
   * @return a scalar schema declaration
   */
  static AvroSchemaDecl type(Class<?> javaType) {
    if (javaType == null) {
      throw new IllegalArgumentException('javaType must not be null')
    }
    if (javaType == BigDecimal) {
      throw new IllegalArgumentException('Use AvroSchemaDecl.decimal(precision, scale) for BigDecimal declarations')
    }
    if (javaType == List || javaType == Map || javaType == Object || javaType == Number) {
      throw new IllegalArgumentException(
          "Use array(...), map(...), or record(...) declarations instead of type(${javaType.simpleName})"
      )
    }
    scalar(toScalarType(javaType))
  }

  /**
   * Creates a fixed decimal schema declaration.
   *
   * @param precision the decimal precision
   * @param scale the decimal scale
   * @return a decimal schema declaration
   */
  static AvroSchemaDecl decimal(int precision, int scale) {
    validateDecimal(precision, scale, 'decimal')
    new DecimalAvroSchemaDecl(precision, scale)
  }

  /**
   * Creates an array schema declaration.
   *
   * @param elementType the schema declaration for array elements
   * @return an array schema declaration
   */
  static AvroSchemaDecl array(AvroSchemaDecl elementType) {
    if (elementType == null) {
      throw new IllegalArgumentException('array elementType must not be null')
    }
    new ArrayAvroSchemaDecl(elementType)
  }

  /**
   * Creates a map schema declaration.
   *
   * @param valueType the schema declaration for map values
   * @return a map schema declaration
   */
  static AvroSchemaDecl map(AvroSchemaDecl valueType) {
    if (valueType == null) {
      throw new IllegalArgumentException('map valueType must not be null')
    }
    new MapAvroSchemaDecl(valueType)
  }

  /**
   * Creates a record schema declaration using the default nested record name for the column.
   *
   * @param fields the record fields and their schema declarations
   * @return a record schema declaration
   */
  static AvroSchemaDecl record(Map<String, AvroSchemaDecl> fields) {
    record(null, fields)
  }

  /**
   * Creates a record schema declaration.
   *
   * @param recordName optional Avro record name override for the nested record
   * @param fields the record fields and their schema declarations
   * @return a record schema declaration
   */
  static AvroSchemaDecl record(String recordName, Map<String, AvroSchemaDecl> fields) {
    new RecordAvroSchemaDecl(recordName, validateFields(fields))
  }

  /**
   * Parses a schema declaration from an SPI-friendly nested map.
   *
   * @param value the map representation of a schema declaration
   * @return the parsed schema declaration
   */
  static AvroSchemaDecl fromMap(Map<String, ?> value) {
    if (value == null) {
      throw new IllegalArgumentException('Schema declaration must not be null')
    }
    Map<String, Object> normalized = normalizeNestedKeys(value)
    String kind = OptionMaps.stringValueOrNull(normalized.kind)
    if (kind == null) {
      throw new IllegalArgumentException("Schema declaration must include 'kind'")
    }
    switch (kind.toLowerCase(Locale.ROOT)) {
      case 'scalar' -> parseScalarDecl(normalized)
      case 'decimal' -> parseDecimalDecl(normalized)
      case 'array' -> parseArrayDecl(normalized)
      case 'map' -> parseMapDecl(normalized)
      case 'record' -> parseRecordDecl(normalized)
      default -> throw new IllegalArgumentException("Unsupported schema declaration kind '$kind'")
    }
  }

  /**
   * Converts this declaration to an SPI-friendly nested map.
   *
   * @return map representation of this schema declaration
   */
  abstract Map<String, ?> toMap()

  @PackageScope
  abstract Schema toAvroSchema(String defaultName, String namespace)

  /**
   * Parses the nested `columnSchemas` SPI option map used by {@link AvroWriteOptions}.
   */
  @PackageScope
  static Map<String, AvroSchemaDecl> columnSchemasValue(Object value, String optionName) {
    if (!(value instanceof Map)) {
      throw new IllegalArgumentException("$optionName must be a Map<String, AvroSchemaDecl> but was ${value?.class}")
    }
    Map<String, AvroSchemaDecl> result = [:]
    ((Map<?, ?>) value).each { key, item ->
      String columnName = requireName(String.valueOf(key), optionName)
      result[columnName] = schemaDeclValue(item, "$optionName[$columnName]")
    }
    result
  }

  /**
   * Serializes typed column schema declarations back to the SPI map shape used by {@link AvroWriteOptions}.
   */
  @PackageScope
  static Map<String, ?> columnSchemasToMap(Map<String, AvroSchemaDecl> value) {
    Map<String, Object> result = [:]
    value.each { String key, AvroSchemaDecl item ->
      result[key] = item.toMap()
    }
    result
  }

  private static AvroScalarTypeDecl toScalarType(Class<?> javaType) {
    AvroScalarTypeDecl scalarType = SCALAR_TYPE_BY_CLASS[javaType]
    if (scalarType != null) {
      return scalarType
    }
    throw new IllegalArgumentException("Unsupported scalar Java type '${javaType.name}' for explicit Avro schema control")
  }

  private static Map<String, Object> normalizeNestedKeys(Map<String, ?> value) {
    Map<String, Object> normalized = [:]
    value.each { key, item ->
      normalized[String.valueOf(key).toLowerCase(Locale.ROOT)] = item
    }
    normalized
  }

  private static AvroSchemaDecl schemaDeclValue(Object value, String optionName) {
    if (value instanceof AvroSchemaDecl) {
      return value as AvroSchemaDecl
    }
    if (value instanceof AvroScalarTypeDecl) {
      return scalar(value as AvroScalarTypeDecl)
    }
    if (value instanceof Map) {
      return fromMap(value as Map<String, ?>)
    }
    if (value != null) {
      String text = String.valueOf(value)
      try {
        return scalar(AvroScalarTypeDecl.valueOf(text.toUpperCase(Locale.ROOT)))
      } catch (IllegalArgumentException ignored) {
        // fall through
      }
    }
    throw new IllegalArgumentException(
        "$optionName must be an AvroSchemaDecl, AvroScalarTypeDecl, or declaration Map but was ${value?.class}"
    )
  }

  private static Map<String, AvroSchemaDecl> recordFieldsValue(Object value, String optionName) {
    if (!(value instanceof Map)) {
      throw new IllegalArgumentException("$optionName must be a Map<String, AvroSchemaDecl> but was ${value?.class}")
    }
    Map<String, AvroSchemaDecl> result = [:]
    ((Map<?, ?>) value).each { key, item ->
      String fieldName = requireName(String.valueOf(key), optionName)
      result[fieldName] = schemaDeclValue(item, "$optionName[$fieldName]")
    }
    if (result.isEmpty()) {
      throw new IllegalArgumentException("$optionName must contain at least one field")
    }
    result
  }

  private static Map<String, AvroSchemaDecl> validateFields(Map<String, AvroSchemaDecl> fields) {
    if (fields == null || fields.isEmpty()) {
      throw new IllegalArgumentException('record fields must contain at least one field')
    }
    Map<String, AvroSchemaDecl> result = [:]
    fields.each { String key, AvroSchemaDecl value ->
      String fieldName = requireName(key, 'record fields')
      if (value == null) {
        throw new IllegalArgumentException("record fields[$fieldName] must not be null")
      }
      result[fieldName] = value
    }
    result
  }

  private static String requireName(String value, String optionName) {
    String trimmed = value?.trim()
    if (trimmed == null || trimmed.isEmpty()) {
      throw new IllegalArgumentException("$optionName must not contain blank names")
    }
    trimmed
  }

  private static AvroScalarTypeDecl scalarTypeValue(Object value, String optionName) {
    if (value instanceof AvroScalarTypeDecl) {
      return value as AvroScalarTypeDecl
    }
    String text = OptionMaps.stringValueOrNull(value)
    if (text == null) {
      throw new IllegalArgumentException("$optionName must not be null")
    }
    try {
      return AvroScalarTypeDecl.valueOf(text.toUpperCase(Locale.ROOT))
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unsupported $optionName value '$text'", e)
    }
  }

  private static int intValue(Object value, String optionName) {
    try {
      return new BigDecimal(String.valueOf(value)).intValueExact()
    } catch (Exception e) {
      throw new IllegalArgumentException("$optionName must be an integer but was $value", e)
    }
  }

  private static void validateDecimal(int precision, int scale, String optionName) {
    if (precision <= 0) {
      throw new IllegalArgumentException("$optionName precision must be > 0 but was $precision")
    }
    if (scale < 0) {
      throw new IllegalArgumentException("$optionName scale must be >= 0 but was $scale")
    }
    if (scale > precision) {
      throw new IllegalArgumentException("$optionName scale must be <= precision but was $scale > $precision")
    }
  }

  private static void ensureOnlyKeys(Map<String, Object> value, String optionName, Set<String> allowedKeys) {
    List<String> unexpected = value.keySet().findAll { String key -> !allowedKeys.contains(key) }.sort()
    if (!unexpected.isEmpty()) {
      throw new IllegalArgumentException(
          "$optionName declaration does not support keys ${unexpected}; allowed keys are ${allowedKeys.sort()}"
      )
    }
  }

  private static AvroSchemaDecl parseScalarDecl(Map<String, Object> normalized) {
    ensureOnlyKeys(normalized, 'scalar', ['kind', 'scalartype'] as Set<String>)
    scalar(scalarTypeValue(normalized.scalartype, 'scalar.scalarType'))
  }

  private static AvroSchemaDecl parseDecimalDecl(Map<String, Object> normalized) {
    ensureOnlyKeys(normalized, 'decimal', ['kind', 'precision', 'scale'] as Set<String>)
    decimal(intValue(normalized.precision, 'decimal.precision'),
        intValue(normalized.scale, 'decimal.scale'))
  }

  private static AvroSchemaDecl parseArrayDecl(Map<String, Object> normalized) {
    ensureOnlyKeys(normalized, 'array', ['kind', 'elementtype'] as Set<String>)
    array(schemaDeclValue(normalized.elementtype, 'array.elementType'))
  }

  private static AvroSchemaDecl parseMapDecl(Map<String, Object> normalized) {
    ensureOnlyKeys(normalized, 'map', ['kind', 'valuetype'] as Set<String>)
    map(schemaDeclValue(normalized.valuetype, 'map.valueType'))
  }

  private static AvroSchemaDecl parseRecordDecl(Map<String, Object> normalized) {
    ensureOnlyKeys(normalized, 'record', ['kind', 'recordname', 'fields'] as Set<String>)
    record(OptionMaps.stringValueOrNull(normalized.recordname), recordFieldsValue(normalized.fields, 'record.fields'))
  }
}
