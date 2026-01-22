package se.alipsa.matrix.parquet

import groovy.transform.CompileStatic
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.example.data.Group
import org.apache.parquet.hadoop.ParquetFileReader
import org.apache.parquet.hadoop.ParquetReader
import org.apache.parquet.hadoop.example.GroupReadSupport
import org.apache.parquet.schema.GroupType
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Type
import se.alipsa.matrix.core.Matrix

import java.sql.Time
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads Apache Parquet files into {@link Matrix} objects.
 *
 * <p>This class provides static methods for deserializing Parquet files back into Matrix data,
 * reconstructing column types, nested structures (Lists, Maps), and preserving BigDecimal precision.</p>
 *
 * <h3>Basic Usage</h3>
 * <pre>{@code
 * // Read a Parquet file (matrix name derived from filename)
 * Matrix data = MatrixParquetReader.read(new File("sales.parquet"))
 *
 * // Read with a custom matrix name
 * Matrix data = MatrixParquetReader.read(new File("sales.parquet"), "quarterly_sales")
 * }</pre>
 *
 * <h3>Type Preservation</h3>
 * <p>When reading files written by {@link MatrixParquetWriter}, column types are automatically
 * restored from the {@link #METADATA_COLUMN_TYPES} metadata. This includes:</p>
 * <ul>
 *   <li>Primitives: Integer, Long, Float, Double, Boolean</li>
 *   <li>Numeric: BigDecimal (with original precision), BigInteger</li>
 *   <li>Temporal: LocalDate, LocalDateTime, java.sql.Date, java.sql.Time, java.sql.Timestamp</li>
 *   <li>Text: String</li>
 *   <li>Nested: List, Map (reconstructed from Parquet LIST/MAP/STRUCT types)</li>
 * </ul>
 *
 * <h3>Reading External Parquet Files</h3>
 * <p>Files not created by MatrixParquetWriter can still be read. Column types will be
 * inferred from the Parquet schema's logical type annotations.</p>
 *
 * <h3>Timezone Handling</h3>
 * <p>UTC timestamps are converted to {@link LocalDateTime} using the system default
 * timezone. To use a different timezone, use the {@link #read(File, ZoneId)} method:</p>
 * <pre>{@code
 * // Read using a specific timezone
 * Matrix data = MatrixParquetReader.read(file, ZoneId.of("America/New_York"))
 *
 * // Read with custom name and timezone
 * Matrix data = MatrixParquetReader.read(file, "myData", ZoneId.of("Europe/London"))
 * }</pre>
 *
 * <h3>Limitations</h3>
 * <ul>
 *   <li>The entire file is loaded into memory (no streaming support)</li>
 * </ul>
 *
 * @see MatrixParquetWriter
 * @see Matrix
 */
@CompileStatic
class MatrixParquetReader {

  /** Metadata key for storing Matrix column types in Parquet file */
  static final String METADATA_COLUMN_TYPES = "matrix.columnTypes"

  /** Parquet schema field name for repeated list entries */
  private static final String FIELD_LIST = 'list'

  /** Parquet schema field name for map key-value pairs */
  private static final String FIELD_KEY_VALUE = 'key_value'

  /** Thread-local storage for timezone used during read operations */
  private static final ThreadLocal<ZoneId> ZONE_ID_HOLDER = new ThreadLocal<>()

  /** Cache for Class.forName() results to avoid repeated reflection calls */
  private static final Map<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>()

  /**
   * Gets the current timezone for timestamp conversion.
   * Returns the thread-local value if set, otherwise the system default.
   */
  private static ZoneId getZoneId() {
    ZoneId zoneId = ZONE_ID_HOLDER.get()
    return zoneId != null ? zoneId : ZoneId.systemDefault()
  }

  /**
   * Gets a cached Class object for the given class name.
   * Uses Class.forName() and caches the result for subsequent calls.
   */
  private static Class<?> getCachedClass(String className) {
    return CLASS_CACHE.computeIfAbsent(className) { name ->
      Class.forName(name)
    }
  }

  /**
   * Validates the file parameter before reading.
   *
   * @param file the file to validate
   * @throws IllegalArgumentException if file is null, does not exist, or is a directory
   */
  private static void validateFile(File file) {
    if (file == null) {
      throw new IllegalArgumentException("File cannot be null")
    }
    if (!file.exists()) {
      throw new IllegalArgumentException("File does not exist: ${file.absolutePath}")
    }
    if (file.isDirectory()) {
      throw new IllegalArgumentException("Expected a file but got a directory: ${file.absolutePath}")
    }
  }

  /**
   * Read the Parquet file into a {@link Matrix} using the supplied name.
   *
   * @param file the Parquet file to read
   * @param matrixName the name to apply to the resulting matrix
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if file is null, does not exist, or is a directory
   */
  static Matrix read(File file, String matrixName) {
    validateFile(file)
    read(file).withMatrixName(matrixName)
  }

  /**
   * Read the Parquet file into a {@link Matrix} with a specific timezone for timestamp conversion.
   *
   * <p>By default, UTC timestamps are converted to {@link LocalDateTime} using the system
   * default timezone. Use this method to specify a different timezone for the conversion.</p>
   *
   * @param file the Parquet file to read
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if file is null, does not exist, is a directory, or zoneId is null
   */
  static Matrix read(File file, ZoneId zoneId) {
    if (zoneId == null) {
      throw new IllegalArgumentException("ZoneId cannot be null")
    }
    try {
      ZONE_ID_HOLDER.set(zoneId)
      return read(file)
    } finally {
      ZONE_ID_HOLDER.remove()
    }
  }

  /**
   * Read the Parquet file into a {@link Matrix} with a specific timezone and matrix name.
   *
   * @param file the Parquet file to read
   * @param matrixName the name to apply to the resulting matrix
   * @param zoneId the timezone to use for converting UTC timestamps to LocalDateTime values
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if file is null, does not exist, is a directory, or zoneId is null
   */
  static Matrix read(File file, String matrixName, ZoneId zoneId) {
    if (zoneId == null) {
      throw new IllegalArgumentException("ZoneId cannot be null")
    }
    try {
      ZONE_ID_HOLDER.set(zoneId)
      return read(file, matrixName)
    } finally {
      ZONE_ID_HOLDER.remove()
    }
  }

  /**
   * Read the Parquet file into a {@link Matrix}, reconstructing nested LIST/MAP/STRUCT values.
   *
   * @param file the Parquet file to read
   * @return a matrix populated with the file contents
   * @throws IllegalArgumentException if file is null, does not exist, or is a directory
   */
  static Matrix read(File file) {
    validateFile(file)
    def path = new Path(file.toURI())
    def conf = new Configuration()
    // Open Parquet file metadata
    def footer = ParquetFileReader.readFooter(conf, path)
    def keyValueMetaData = footer.getFileMetaData().getKeyValueMetaData()
    def typeString = keyValueMetaData.get(METADATA_COLUMN_TYPES)

    //println("typeString: $typeString")
    List<Class> fieldTypes
    if (typeString != null) {
      fieldTypes = parseTypeString(typeString)
    }

    def reader = ParquetReader.builder(new GroupReadSupport(), path).build()

    String matrixName
    if (file.name.contains('.')) {
      matrixName = file.name.substring(0, file.name.lastIndexOf('.'))
    } else {
      matrixName = file.name
    }

    Group row = reader.read()
    if (row == null) {
      return Matrix.builder(matrixName).build()
    }

    def schema = row.getType()
    List<String> fieldNames = schema.fields.collect { it.name }
    if (fieldTypes == null) {
      fieldTypes = extractFieldTypes(schema)
    }

    def builder = Matrix.builder(matrixName)
    .columnNames(fieldNames)
    .types(fieldTypes)

    while (row != null) {
      def rowData = []
      fieldNames.eachWithIndex { name, i ->
        def type = fieldTypes[i]
        def field = schema.getType(name)
        def value = readValue(row, name, field, type)
        rowData << value
      }
      builder.addRow(rowData as Object[])
      row = reader.read()
    }
    return builder.build()
  }

  static List<Class> parseTypeString(String typeString) {
    return typeString.split(',').collect { className ->
      return getCachedClass(className.trim())
    }
  }

  private static Class getJavaType(PrimitiveType.PrimitiveTypeName typeName) {
    switch (typeName) {
      case PrimitiveTypeName.INT32: return Integer
      case PrimitiveTypeName.INT64: return Long
      case PrimitiveTypeName.FLOAT: return Float
      case PrimitiveTypeName.DOUBLE: return Double
      case PrimitiveTypeName.BOOLEAN: return Boolean
      default: return String
    }
  }

  static List<Class> extractFieldTypes(GroupType schema) {
    schema.fields.collect { field ->
      def logical = field.getLogicalTypeAnnotation()
      if (logical instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation) {
        return List
      }
      if (logical instanceof LogicalTypeAnnotation.MapLogicalTypeAnnotation) {
        return Map
      }

      if (!field.isPrimitive()) {
        return Map
      }

      def primitive = field.asPrimitiveType().primitiveTypeName

      if (logical != null) {
        if (logical == LogicalTypeAnnotation.dateType()) return LocalDate
        if (logical instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) return LocalDateTime
        if (logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) return BigDecimal
      }

      return getJavaType(primitive)
    }
  }

  private static Object readValue(Group group, String fieldName, Type fieldType, Class expectedType) {
    if (group.getFieldRepetitionCount(fieldName) == 0) {
      return null
    }

    if (fieldType.isPrimitive()) {
      return readPrimitive(group, fieldName, fieldType.asPrimitiveType(), expectedType)
    }

    GroupType groupType = fieldType.asGroupType()
    def logical = groupType.logicalTypeAnnotation
    if (logical instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation) {
      return readList(group, fieldName, groupType)
    }
    if (logical instanceof LogicalTypeAnnotation.MapLogicalTypeAnnotation) {
      return readMap(group, fieldName, groupType)
    }
    return readStruct(group, fieldName, groupType)
  }

  private static Object readPrimitive(Group group, String fieldName, PrimitiveType fieldType, Class expectedType) {
    def logical = fieldType.logicalTypeAnnotation
    switch (fieldType.primitiveTypeName) {
      case PrimitiveTypeName.INT32:
        if (logical == LogicalTypeAnnotation.dateType()) {
          return LocalDate.ofEpochDay(group.getInteger(fieldName, 0))
        }
        if (logical instanceof LogicalTypeAnnotation.TimeLogicalTypeAnnotation) {
          int millis = group.getInteger(fieldName, 0)
          return Time.valueOf(LocalTime.ofNanoOfDay(millis * 1_000_000L))
        }
        if (expectedType == java.sql.Date) {
          return java.sql.Date.valueOf(LocalDate.ofEpochDay(group.getInteger(fieldName, 0)))
        }
        return group.getInteger(fieldName, 0)
      case PrimitiveTypeName.INT64:
        if (logical instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) {
          def micros = group.getLong(fieldName, 0)
          def millis = (long) (micros / 1000)
          if (expectedType == java.sql.Timestamp) {
            return new java.sql.Timestamp(millis)
          }
          return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), getZoneId())
        }
        def longValue = group.getLong(fieldName, 0)
        if (expectedType == BigInteger) {
          return BigInteger.valueOf(longValue)
        }
        return longValue
      case PrimitiveTypeName.FLOAT:
        return group.getFloat(fieldName, 0)
      case PrimitiveTypeName.DOUBLE:
        return group.getDouble(fieldName, 0)
      case PrimitiveTypeName.BOOLEAN:
        return group.getBoolean(fieldName, 0)
      case PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY:
      case PrimitiveTypeName.BINARY:
        if (logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) {
          def scale = logical.scale
          def binary = group.getBinary(fieldName, 0)
          def unscaled = new BigInteger(binary.getBytes())
          return new BigDecimal(unscaled, scale)
        }
        if (expectedType == BigDecimal) {
          return BigDecimal.valueOf(group.getDouble(fieldName, 0))
        }
        return group.getBinary(fieldName, 0).toStringUsingUTF8()
      default:
        return group.getString(fieldName, 0)
    }
  }

  private static List readList(Group group, String fieldName, GroupType groupType) {
    List result = []
    Group listGroup = group.getGroup(fieldName, 0)
    GroupType repeatedType = groupType.getType(0).asGroupType()
    Type elementType = repeatedType.getType(0)
    int elementCount = listGroup.getFieldRepetitionCount(FIELD_LIST)
    for (int j = 0; j < elementCount; j++) {
      Group elementGroup = listGroup.getGroup(FIELD_LIST, j)
      def element = readValue(elementGroup, elementType.name, elementType, null)
      result << element
    }
    return result
  }

  private static Map readMap(Group group, String fieldName, GroupType groupType) {
    LinkedHashMap result = new LinkedHashMap()
    Group mapGroup = group.getGroup(fieldName, 0)
    GroupType keyValueType = groupType.getType(0).asGroupType()
    Type keyType = keyValueType.getType(0)
    Type valueType = keyValueType.getFieldCount() > 1 ? keyValueType.getType(1) : null
    int keyValueCount = mapGroup.getFieldRepetitionCount(FIELD_KEY_VALUE)
    for (int j = 0; j < keyValueCount; j++) {
      Group kvGroup = mapGroup.getGroup(FIELD_KEY_VALUE, j)
      def key = readValue(kvGroup, keyType.name, keyType, null)
      def value = valueType == null ? null : readValue(kvGroup, valueType.name, valueType, null)
      result[key] = value
    }
    return result
  }

  private static Map readStruct(Group group, String fieldName, GroupType groupType) {
    if (groupType.fields.isEmpty()) {
      return [:]
    }
    Group structGroup = group.getGroup(fieldName, 0)
    LinkedHashMap<String, Object> result = new LinkedHashMap<>()
    groupType.fields.each { Type field ->
      def value = readValue(structGroup, field.name, field, null)
      result[field.name] = value
    }
    return result
  }
}
