package se.alipsa.matrix.parquet

import groovy.transform.PackageScope

import org.apache.parquet.filter2.predicate.FilterApi
import org.apache.parquet.filter2.predicate.FilterPredicate
import org.apache.parquet.io.api.Binary
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Immutable logical predicate; values are converted against the file schema before reading. */
final class ParquetPredicate {
  private static final Object NOT_PUSHABLE = new Object()
  private static final String IS_NOT_NULL = 'isNotNull'

  private static final String KEY_EQ = 'eq'
  private static final String KEY_NE = 'ne'
  private static final String KEY_LT = 'lt'
  private static final String KEY_LE = 'le'
  private static final String KEY_GT = 'gt'
  private static final String KEY_GE = 'ge'

  final String operation
  final String column
  private final Object value
  final List<ParquetPredicate> children

  @PackageScope
  ParquetPredicate(String operation, String column, Object value, List<ParquetPredicate> children = []) {
    if (column == null && (children.empty || children.any { it == null })) { throw new IllegalArgumentException('Predicate children cannot be null') }
    if (column != null && (column.empty || (!(operation in ['isNull', IS_NOT_NULL]) && value == null))) { throw new IllegalArgumentException('Use explicit null predicates for null comparisons') }
    this.operation = operation
    this.column = column
    this.value = value instanceof byte[] ? ((byte[]) value).clone() :
        value instanceof Number && !(value instanceof Double) && !(value instanceof Float) ? new BigDecimal(value.toString()) : value
    this.children = Collections.unmodifiableList(new ArrayList<>(children))
  }

  /** Returns the top-level columns required by this predicate. */
  Set<String> columns() {
    column == null ? children.collectMany { it.columns() }.toSet() : [column] as Set<String>
  }

  /** Evaluates the exact logical predicate; comparisons with null are false except null tests. */
  boolean test(Map<String, Object> row) {
    switch (operation) {
      case 'and' -> children.every { it.test(row) }
      case 'or' -> children.any { it.test(row) }
      case 'not' -> !children[0].test(row)
      case 'isNull' -> row[column] == null
      case IS_NOT_NULL -> row[column] != null
      default -> compare(row[column])
    }
  }

  private boolean compare(Object actual) {
    if (actual == null || value == null) { return false }
    int comparison
    if (actual instanceof Number && value instanceof Number) {
      if (Double.isNaN(actual.doubleValue()) || Double.isNaN(((Number) value).doubleValue())) {
        return operation == KEY_NE
      }
      boolean actualInfinite = (actual instanceof Double || actual instanceof Float) && Double.isInfinite(actual.doubleValue())
      boolean valueInfinite = (value instanceof Double || value instanceof Float) && Double.isInfinite(((Number) value).doubleValue())
      comparison = actualInfinite && valueInfinite ? Double.compare(actual.doubleValue(), ((Number) value).doubleValue()) :
          actualInfinite ? (actual.doubleValue() > 0 ? 1 : -1) : valueInfinite ? (((Number) value).doubleValue() > 0 ? -1 : 1) :
          new BigDecimal(actual.toString()) <=> new BigDecimal(value.toString())
    } else if (actual instanceof String && value instanceof String) {
      comparison = Arrays.compareUnsigned(actual.getBytes(java.nio.charset.StandardCharsets.UTF_8),
          ((String) value).getBytes(java.nio.charset.StandardCharsets.UTF_8))
    } else if (actual instanceof byte[] && value instanceof byte[]) {
      comparison = Arrays.compareUnsigned((byte[]) actual, (byte[]) value)
    } else {
      comparison = ((Comparable) actual) <=> value
    }
    switch (operation) {
      case KEY_EQ -> comparison == 0
      case KEY_NE -> comparison != 0
      case KEY_LT -> comparison < 0
      case KEY_LE -> comparison <= 0
      case KEY_GT -> comparison > 0
      case KEY_GE -> comparison >= 0
      default -> throw new IllegalArgumentException("Unknown predicate $operation")
    }
  }

  // Only exact physical conversions are pushed down; unsafe expressions remain wholly residual.
  @PackageScope
  FilterPredicate pushdown(MessageType schema) {
    if (column == null) {
      List<FilterPredicate> predicates = children*.pushdown(schema)
      if (operation == 'and') {
        // Partial AND relies on always applying the residual and never pushing NOT.
        return predicates[0] == null ? predicates[1] : predicates[1] == null ? predicates[0] : FilterApi.and(predicates[0], predicates[1])
      }
      if (predicates.any { it == null }) { return null }
      return operation == 'or' ? FilterApi.or(predicates[0], predicates[1]) : null
    }
    TypeCheck.validate(this, schema)
    PrimitiveType field = schema.getType(column).asPrimitiveType()
    def logical = field.logicalTypeAnnotation
    if (operation in [KEY_NE, IS_NOT_NULL]) { return null }
    if (logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation &&
        field.primitiveTypeName in [PrimitiveType.PrimitiveTypeName.BINARY, PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY]) { return null }
    if (!(field.primitiveTypeName in [PrimitiveType.PrimitiveTypeName.INT32, PrimitiveType.PrimitiveTypeName.INT64, PrimitiveType.PrimitiveTypeName.BINARY])) { return null }
    if (logical instanceof LogicalTypeAnnotation.IntLogicalTypeAnnotation && !logical.isSigned()) { return null }
    Object physical = physicalValue(field)
    if (physical.is(NOT_PUSHABLE)) { return null }
    def col
    switch (field.primitiveTypeName) {
      case PrimitiveType.PrimitiveTypeName.INT32 -> {
        col = FilterApi.intColumn(column)
      }
      case PrimitiveType.PrimitiveTypeName.INT64 -> {
        col = FilterApi.longColumn(column)
      }
      case PrimitiveType.PrimitiveTypeName.BINARY -> {
        col = FilterApi.binaryColumn(column)
        physical = physical instanceof String ? Binary.fromString((String) physical) : physical instanceof byte[] ? Binary.fromConstantByteArray((byte[]) physical) : null
      }
      default -> col = null
    }
    buildPhysical(operation, col, (Comparable) physical)
  }

  @groovy.transform.CompileDynamic
  private static FilterPredicate buildPhysical(String operation, col, Comparable physical) {
    switch (operation) {
      case KEY_EQ, 'isNull' -> FilterApi.eq(col, physical)
      case KEY_LT -> FilterApi.lt(col, physical)
      case KEY_LE -> FilterApi.ltEq(col, physical)
      case KEY_GT -> FilterApi.gt(col, physical)
      case KEY_GE -> FilterApi.gtEq(col, physical)
      default -> null
    }
  }

  @PackageScope
  ParquetPredicate bind(MessageType schema, ZoneId zone) {
    if (column == null) { return new ParquetPredicate(operation, null, null, children*.bind(schema, zone)) }
    TypeCheck.validate(this, schema)
    PrimitiveType field = schema.getType(column).asPrimitiveType()
    Object normalized = value
    if ((field.primitiveTypeName == PrimitiveType.PrimitiveTypeName.INT96 ||
        (field.logicalTypeAnnotation instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation &&
        ((LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) field.logicalTypeAnnotation).isAdjustedToUTC())) && value instanceof LocalDateTime) {
      normalized = value.atZone(zone ?: ZoneId.systemDefault()).toInstant()
    }
    ParquetPredicate bound = new ParquetPredicate(operation, column, normalized)
    bound
  }

  private Object physicalValue(PrimitiveType field) {
    try { physicalValueExact(field) }
    catch (ArithmeticException | IllegalArgumentException ignored) { NOT_PUSHABLE }
  }

  @SuppressWarnings('AbcMetric') // Exhaustive exact physical conversion with residual fallback.
  private Object physicalValueExact(PrimitiveType field) {
    if (value == null) { return null }
    def logical = field.logicalTypeAnnotation
    if (logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) {
      BigDecimal decimal = new BigDecimal(value.toString()).setScale(logical.scale, java.math.RoundingMode.UNNECESSARY)
      if (decimal.precision() > logical.precision) { throw new IllegalArgumentException("Decimal predicate overflows '${field.name}'") }
      BigInteger unscaled = decimal.unscaledValue()
      return field.primitiveTypeName == PrimitiveType.PrimitiveTypeName.INT32 ? (Object) unscaled.intValueExact() :
          field.primitiveTypeName == PrimitiveType.PrimitiveTypeName.INT64 ? (Object) unscaled.longValueExact() : unscaled
    }
    if (value instanceof LocalDate) { return Math.toIntExact(value.toEpochDay()) }
    if (logical instanceof LogicalTypeAnnotation.TimeLogicalTypeAnnotation) {
      long nanos = ((LocalTime) value).toNanoOfDay()
      long divisor = logical.unit == LogicalTypeAnnotation.TimeUnit.MILLIS ? 1_000_000L : logical.unit == LogicalTypeAnnotation.TimeUnit.MICROS ? 1000L : 1L
      if (nanos % divisor != 0) { throw new IllegalArgumentException("Lossy TIME predicate '${field.name}'") }
      long units = nanos.intdiv(divisor)
      return field.primitiveTypeName == PrimitiveType.PrimitiveTypeName.INT32 ? (Object) Math.toIntExact(units) : (Object) units
    }
    if (logical instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) {
      Instant instant = value instanceof Instant ? (Instant) value : ((LocalDateTime) value).toInstant(java.time.ZoneOffset.UTC)
      long divisor = logical.unit == LogicalTypeAnnotation.TimeUnit.MILLIS ? 1_000_000L : logical.unit == LogicalTypeAnnotation.TimeUnit.MICROS ? 1000L : 1L
      if (instant.nano % divisor != 0) { throw new IllegalArgumentException("Lossy TIMESTAMP predicate '${field.name}'") }
      return TemporalUnits.epochUnits(instant, logical.unit)
    }
    if (field.primitiveTypeName == PrimitiveType.PrimitiveTypeName.INT32) { return new BigDecimal(value.toString()).intValueExact() }
    if (field.primitiveTypeName == PrimitiveType.PrimitiveTypeName.INT64) { return new BigDecimal(value.toString()).longValueExact() }
    value
  }

  private static class TypeCheck {
    static void validate(ParquetPredicate predicate, MessageType schema) {
      if (!schema.containsField(predicate.column)) {
        throw new IllegalArgumentException("Unknown predicate column '${predicate.column}'")
      }
      def field = schema.getType(predicate.column)
      if (!field.primitive || field.repetition == org.apache.parquet.schema.Type.Repetition.REPEATED) {
        throw new IllegalArgumentException("Repeated/nested predicate '${predicate.column}' is unsupported")
      }
      if (predicate.operation in ['isNull', IS_NOT_NULL]) { return }
      Class inferred = MatrixParquetReader.extractFieldTypes(new MessageType('predicate', [field]))[0]
      Object value = predicate.value
      boolean compatible = Number.isAssignableFrom(inferred) ? value instanceof Number :
          inferred == java.sql.Time ? value instanceof LocalTime :
          inferred == LocalDateTime ? (value instanceof LocalDateTime ||
              (value instanceof Instant && (field.asPrimitiveType().primitiveTypeName == PrimitiveType.PrimitiveTypeName.INT96 ||
              ((LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) field.logicalTypeAnnotation).isAdjustedToUTC()))) : inferred.isInstance(value)
      if (!compatible) { throw new IllegalArgumentException("Incompatible predicate value for '${predicate.column}': expected $inferred") }
    }
  }

  @PackageScope
  void validate(MessageType schema) {
    if (column != null) { TypeCheck.validate(this, schema) }
    children.each { it.validate(schema) }
  }
}
