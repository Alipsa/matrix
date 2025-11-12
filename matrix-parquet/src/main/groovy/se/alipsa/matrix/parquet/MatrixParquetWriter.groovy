package se.alipsa.matrix.parquet

import groovy.transform.CompileStatic
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.parquet.example.data.Group
import org.apache.parquet.example.data.simple.SimpleGroupFactory
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.example.ExampleParquetWriter
import org.apache.parquet.io.api.Binary
import org.apache.parquet.schema.GroupType
import org.apache.parquet.schema.LogicalTypeAnnotation
import org.apache.parquet.schema.MessageType
import org.apache.parquet.schema.PrimitiveType
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName
import org.apache.parquet.schema.Type
import org.apache.parquet.schema.Types

import se.alipsa.matrix.core.Matrix

import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.beans.Introspector
import java.beans.PropertyDescriptor

@CompileStatic
class MatrixParquetWriter {

  /**
   * Writes a Matrix to a Parquet file, optionally inferring precision and scale for BigDecimal columns.
   *
   * @param matrix the matrix to write
   * @param fileOrDir the target file or directory. If directory the file name will be based on the matrix name.
   * @param inferPrecisionAndScale whether to infer precision and scale for BigDecimal columns. If false,
   * BigDecimal columns default to double storage (which leads to some loss of precision).
   * Nested Groovy {@link List} and {@link Map} structures are preserved as Parquet LIST/MAP/STRUCT types.
   * @return the target file
   */
  static File write(Matrix matrix, File fileOrDir, boolean inferPrecisionAndScale = true) {
    MessageType schema
    if (inferPrecisionAndScale) {
      schema = buildSchema(matrix,true)
    } else {
      schema = buildSchema(matrix, false)
    }
    //println "Write, inferPrecisionAndScale = $inferPrecisionAndScale, schema = $schema"
    File file = determineTargetFile(matrix, fileOrDir)

    return writeInternal(matrix, file, schema)
  }

  /**
   * Writes a Matrix to a Parquet file using uniform decimal precision
   * and scale for all BigDecimal columns. This is useful if you don't
   * want to infer precision/scale but also don't want to store them as double
   * so you can set the precision and scale to a sufficiently large value (e.g., [38, 18] for typical financial calculations)
   * so there will be no loss of precision.
   *
   * @param matrix
   * @param fileOrDir
   * @param precision
   * @param scale
   * @return
   */
  static File write(Matrix matrix, File fileOrDir, int precision, int scale) {
    Map<String, int[]> decimalMeta = [:]
    matrix.columnNames().each { col ->
      if (matrix.type(col) == BigDecimal) {
        decimalMeta[col] = [precision, scale] as int[]
      }
    }
    return write(matrix, fileOrDir, decimalMeta)
  }

  /**
   * Writes a Matrix to a Parquet file using explicitly provided decimal precision and scale metadata.
   * This allows for precise control over how BigDecimal values are stored.
   * Note that precision affects storage but not the precision and
   * scale of the BigDecimal values themselves. This means that if the precision is too small to hold a value,
   * an exception will be thrown during writing. BigDecimal automatically adjust precision and scale. E.g.
   * if you specified precision as 5 and scale as two and then retrieve a value of 12.1
   * the precision of that value will be 3 and scale 1. Example usage:
   * <code><pre>
   * Matrix data = Dataset.mtcars()
   * File file = new File("mtcars.parquet")
   * MatrixParquetWriter.write(data, file, [wt: [4, 3]])
   * </pre></code>
   * @param matrix the matrix to write
   * @param fileOrDir the target file or directory. If directory the file name will be based on the matrix name.
   * @param decimalMeta a Map where keys are column names (String) and values are int arrays [precision, scale].
   * if set to null or there is no matching entry for a BigDecimal column, that column will default to double storage
   * @return the target file
   */
  static File write(Matrix matrix, File fileOrDir, Map<String, int[]> decimalMeta) {
    File file = determineTargetFile(matrix, fileOrDir)

    def schema = buildSchema(matrix, decimalMeta)
    return writeInternal(matrix, file, schema)
  }

  private static File determineTargetFile(Matrix matrix, File fileOrDir) {
    String name = matrix.matrixName ?: 'matrix'
    File file
    if (fileOrDir.isDirectory()) {
      file = new File(fileOrDir, "${name}.parquet")
      println "Writing to ${file.absolutePath}"
    } else {
      file = fileOrDir
    }
    file
  }

  /**
   * Internal helper method that handles the core logic of writing matrix rows
   * to a Parquet file, given an already constructed schema.
   * @param matrix the matrix to write
   * @param file the target file
   * @param schema the Parquet MessageType schema
   * @return the target file
   */
  private static File writeInternal(Matrix matrix, File file, MessageType schema) {
    def conf = new Configuration()
    def extraMeta = new HashMap<String, String>()
    extraMeta.put("matrix.columnTypes", matrix.types().collect { it.name }.join(','))

    def writer = ExampleParquetWriter.builder(new Path(file.toURI()))
        .withConf(conf)
        .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
        .withType(schema)
        .withExtraMetaData(extraMeta)
        .build()

    def factory = new SimpleGroupFactory(schema)
    def rowCount = matrix.rowCount()
    def colNames = matrix.columnNames()

    (0..<rowCount).each { i ->
      def group = factory.newGroup()
      colNames.each { col ->
        def value = matrix[i, col]
        if (value != null) {
          def fieldType = schema.getType(col)
          writeValue(group, col, fieldType, value)
        }
      }
      writer.write(group)
    }

    writer.close()
    return file
  }

  /**
   * Builds the Parquet schema based on the matrix and an optional map of explicit decimal metadata.
   * This overload is the core schema builder.
   *
   * @param matrix the matrix
   * @param explicitDecimalMeta map of column name to [precision, scale] array for BigDecimal columns.
   * @return the Parquet MessageType schema.
   */
  static MessageType buildSchema(Matrix matrix, Map<String, int[]> explicitDecimalMeta) {
    def builder = Types.buildMessage()
    matrix.columnNames().each { col ->
      def type = matrix.type(col)
      def meta = explicitDecimalMeta?.get(col)
      //println "Building schema for column '$col' of type $type with decimal meta: $meta"
      builder.addField(buildParquetType(col, matrix.column(col), type, meta))
    }
    return builder.named("MatrixSchema")
  }

  /**
   * Builds the Parquet schema based on the matrix, inferring decimal precision/scale if requested.
   * This is for backward compatibility and delegates to the explicit map builder.
   * @param matrix the matrix
   * @param inferPrecisionAndScale if true, infers decimal precision and scale. If false, BigDecimal columns default to
   * double storage (which leads to some loss of precision).
   * @return the Parquet MessageType schema.
   */
  static MessageType buildSchema(Matrix matrix, boolean inferPrecisionAndScale = false) {
    Map<String, int[]> decimalMeta = null
    if (inferPrecisionAndScale) {
      decimalMeta = inferDecimalPrecisionAndScale(matrix)
    }
    return buildSchema(matrix, decimalMeta)
  }

  /**
   * Builds a Parquet {@link Type} for the supplied column, recursively handling nested collections and maps.
   *
   * @param name the field name in the Parquet schema
   * @param values the column values used to infer nested shapes
   * @param clazz the declared Matrix column type
   * @param decimalMeta optional decimal precision/scale metadata for {@link BigDecimal} columns
   * @return a Parquet {@link Type} describing the column
   */
  static Type buildParquetType(String name, List<?> values, Class clazz, int[] decimalMeta = null, boolean preferStructForMaps = false) {
    if (isListType(clazz, values)) {
      List<?> nestedValues = extractListElements(values)
      Type elementType = buildParquetType('element', nestedValues, inferClass(nestedValues, Object), null, true)
      if (elementType == null) {
        elementType = buildPrimitiveType('element', String, null, false)
      }
      return Types.optionalList().element(elementType).named(name)
    }

    if (isMapType(clazz, values)) {
      if (preferStructForMaps) {
        return buildStructType(name, values, clazz)
      }
      return buildMapType(name, values)
    }

    if (isStructType(clazz, values)) {
      return buildStructType(name, values, clazz)
    }

    Class<?> effectiveClass = inferClass(values, clazz)
    return buildPrimitiveType(name, effectiveClass, decimalMeta, false)
  }

  private static PrimitiveType buildPrimitiveType(String name, Class clazz, int[] decimalMeta = null, boolean required = false) {
    if (clazz == BigDecimal) {
      if (decimalMeta != null && decimalMeta.length == 2) {
        // int precision = decimalMeta[0] ?: 10 // BUGGY: what if 0 is passed? (though inference prevents this)
        // int scale = decimalMeta[1] ?: 2     // BUGGY: 0 ?: 2 becomes 2

        int precision = decimalMeta[0]
        int scale = decimalMeta[1]

        // Add safeguards for invalid (0 or less) precision, which can be passed manually
        // The inference method already ensures precision is at least 1.
        if (precision <= 0) {
          precision = 10 // Fallback to a default precision
        }
        // Add safeguards for invalid scale: must be non-negative and not greater than precision
        if (scale < 0) {
          scale = 0 // Fallback to a default scale
        }
        if (scale > precision) {
          scale = precision // Clamp scale to precision
        }

        def builder = required ? Types.required(PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY) : Types.optional(PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY)
        .length(minBytesForPrecision(precision))
        .as(LogicalTypeAnnotation.decimalType(scale, precision))
        return builder.named(name)
      } else {
        // fallback to double (discouraged, but allowed for backward compat)
        def builder = required ? Types.required(PrimitiveTypeName.DOUBLE) : Types.optional(PrimitiveTypeName.DOUBLE)
        return builder.named(name)
      }
    }

    def primitive
    def logical = null

    switch (clazz) {
      case Integer, int -> primitive = PrimitiveTypeName.INT32
      case Long, long, BigInteger -> primitive = PrimitiveTypeName.INT64
      case Float, float -> primitive = PrimitiveTypeName.FLOAT
      case Double, double -> primitive = PrimitiveTypeName.DOUBLE
      case Boolean, boolean -> primitive = PrimitiveTypeName.BOOLEAN
      case LocalDate, java.sql.Date -> {
        primitive = PrimitiveTypeName.INT32
        logical = LogicalTypeAnnotation.dateType()
      }
      case LocalDateTime, Timestamp -> {
        primitive = PrimitiveTypeName.INT64
        // Use LogicalTypeAnnotation.TimeUnit.MICROS for better precision, but current code used MILLIS
        // Sticking to MILLIS to avoid breaking existing behavior, but writing microseconds in _writeInternal
        logical = LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS)
      }
      case Time -> {
        primitive = PrimitiveTypeName.INT32
        // Time in milliseconds, but writing seconds in _writeInternal
        logical = LogicalTypeAnnotation.timeType(true, LogicalTypeAnnotation.TimeUnit.MILLIS)
      }
      default -> primitive = PrimitiveTypeName.BINARY
    }

    def builder = required ? Types.required(primitive) : Types.optional(primitive)
    if (logical != null) {
      builder = builder.as(logical)
    }
    return builder.named(name)
  }

  private static Type buildMapType(String name, List<?> values) {
    def mapBuilder = Types.optionalMap()
    List<Map> maps = values.findAll { it instanceof Map } as List<Map>

    Object keySample = findFirstNonNull(maps.collectMany { it?.keySet() ?: [] } as List)
    Class<?> keyClass = inferClass([keySample], keySample?.class ?: String)
    PrimitiveType keyType = buildPrimitiveType('key', keyClass ?: String, null, true)
    mapBuilder.key(keyType)

    List<Object> valueSamples = maps.collectMany { it?.values() ?: [] } as List<Object>
    Set<Class<?>> valueClasses = valueSamples.findAll { it != null }.collect { it.class }.toSet()
    if (valueClasses.size() > 1) {
      return buildStructType(name, values, Map)
    }
    Type valueType
    if (valueSamples.isEmpty()) {
      valueType = buildPrimitiveType('value', String, null, false)
    } else {
      valueType = buildParquetType('value', valueSamples, inferClass(valueSamples, Object), null)
    }
    mapBuilder.value(valueType)
    return mapBuilder.named(name)
  }

  private static Type buildStructType(String name, List<?> values, Class<?> declaredType) {
    Object sample = findFirstNonNull(values)
    if (sample == null) {
      return Types.optionalGroup().named(name)
    }

    def groupBuilder = Types.optionalGroup()
    Map<String, List<Object>> fieldValues = new LinkedHashMap<>()
    Map<String, Class<?>> fieldTypes = new LinkedHashMap<>()

    if (sample instanceof Map || Map.isAssignableFrom(declaredType)) {
      Set<String> keys = new LinkedHashSet<>()
      values.each { val ->
        if (val instanceof Map mapVal) {
          mapVal.keySet().each { key -> keys.add(String.valueOf(key)) }
        }
      }
      keys.each { key ->
        List<Object> collected = []
        values.each { val ->
          if (val instanceof Map mapVal) {
            collected.add(mapVal.get(key))
          } else {
            collected.add(null)
          }
        }
        fieldValues[key] = collected
        fieldTypes[key] = inferClass(collected, Object)
      }
    } else {
      def beanInfo = Introspector.getBeanInfo(sample.class, Object)
      List<PropertyDescriptor> descriptors = beanInfo.propertyDescriptors.findAll { it.readMethod != null }
      descriptors.each { PropertyDescriptor pd ->
        List<Object> collected = []
        values.each { val ->
          if (val == null) {
            collected.add(null)
          } else {
            def method = pd.readMethod
            if (!method.accessible) {
              method.accessible = true
            }
            collected.add(method.invoke(val))
          }
        }
        fieldValues[pd.name] = collected
        fieldTypes[pd.name] = pd.propertyType
      }
    }

    fieldValues.each { fieldName, collected ->
      Type fieldType = buildParquetType(fieldName, collected, fieldTypes[fieldName] ?: Object, null)
      groupBuilder.addField(fieldType)
    }
    return groupBuilder.named(name)
  }

  private static List<?> extractListElements(List<?> values) {
    List<Object> elements = []
    values.each { val ->
      if (val instanceof Collection coll) {
        coll.each { elements.add(it) }
      }
    }
    return elements
  }

  private static boolean isListType(Class<?> declaredType, List<?> values) {
    if (declaredType != null && Collection.isAssignableFrom(declaredType)) {
      return true
    }
    return values.any { it instanceof Collection }
  }

  private static boolean isMapType(Class<?> declaredType, List<?> values) {
    if (declaredType != null && Map.isAssignableFrom(declaredType)) {
      return true
    }
    return values.any { it instanceof Map }
  }

  private static boolean isStructType(Class<?> declaredType, List<?> values) {
    if (isMapType(declaredType, values)) {
      return true
    }
    Class<?> effective = inferClass(values, declaredType)
    return effective != null && !isPrimitiveLike(effective) && !Collection.isAssignableFrom(effective)
  }

  private static Class<?> inferClass(List<?> values, Class<?> declaredType) {
    Object sample = findFirstNonNull(values)
    if (sample != null) {
      Class<?> clazz = sample.class
      return clazz.isPrimitive() ? convertPrimitive(clazz) : clazz
    }
    if (declaredType != null && declaredType != Object) {
      return declaredType.isPrimitive() ? convertPrimitive(declaredType) : declaredType
    }
    return Object
  }

  private static Object findFirstNonNull(List<?> values) {
    for (def val : values) {
      if (val != null) {
        return val
      }
    }
    return null
  }

  private static Class<?> convertPrimitive(Class<?> clazz) {
    if (!clazz.isPrimitive()) {
      return clazz
    }
    if (clazz == int) return Integer
    if (clazz == long) return Long
    if (clazz == double) return Double
    if (clazz == float) return Float
    if (clazz == boolean) return Boolean
    if (clazz == byte) return Byte
    if (clazz == short) return Short
    if (clazz == char) return Character
    return clazz
  }

  private static boolean isPrimitiveLike(Class<?> clazz) {
    if (clazz == null) {
      return false
    }
    if (clazz.isPrimitive()) {
      return true
    }
    if (Number.isAssignableFrom(clazz)) {
      return true
    }
    if (CharSequence.isAssignableFrom(clazz)) {
      return true
    }
    if (Boolean.isAssignableFrom(clazz)) {
      return true
    }
    if (Date.isAssignableFrom(clazz)) {
      return true
    }
    if (clazz in [LocalDate, LocalDateTime, Timestamp, Time]) {
      return true
    }
    return false
  }

  private static void writeValue(Group group, String fieldName, Type fieldType, Object value) {
    if (value == null) {
      return
    }
    if (fieldType.isPrimitive()) {
      writePrimitiveValue(group, fieldName, fieldType.asPrimitiveType(), value)
      return
    }

    GroupType groupType = fieldType.asGroupType()
    def logical = groupType.logicalTypeAnnotation
    if (logical instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation) {
      writeList(group, fieldName, groupType, value)
    } else if (logical instanceof LogicalTypeAnnotation.MapLogicalTypeAnnotation) {
      writeMap(group, fieldName, groupType, value)
    } else {
      writeStruct(group, fieldName, groupType, value)
    }
  }

  private static void writePrimitiveValue(Group group, String fieldName, PrimitiveType field, Object value) {
    switch (value.class) {
      case Integer, int -> group.append(fieldName, (int) value)
      case Long, long, BigInteger -> group.append(fieldName, ((Number) value).longValue())
      case Float, float -> group.append(fieldName, ((Number) value).floatValue())
      case Double, double -> group.append(fieldName, ((Number) value).doubleValue())
      case BigDecimal -> {
        def logical = field.getLogicalTypeAnnotation()
        if (field.primitiveTypeName == PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY &&
            logical instanceof LogicalTypeAnnotation.DecimalLogicalTypeAnnotation) {
          def bd = (BigDecimal) value
          def scale = logical.scale
          def unscaled = bd.setScale(scale, BigDecimal.ROUND_HALF_UP).unscaledValue()
          def bytes = unscaled.toByteArray()
          def size = field.typeLength
          def padded = new byte[size]
          System.arraycopy(bytes, 0, padded, size - bytes.length, bytes.length)
          group.add(fieldName, Binary.fromConstantByteArray(padded))
        } else {
          group.append(fieldName, ((BigDecimal) value).doubleValue())
        }
      }
      case Boolean, boolean -> group.append(fieldName, (boolean) value)
      case LocalDate -> group.append(fieldName, ((LocalDate) value).toEpochDay().intValue())
      case java.sql.Date -> group.append(fieldName, ((java.sql.Date) value).toLocalDate().toEpochDay().intValue())
      case Time -> group.append(fieldName, ((Time) value).toLocalTime().toSecondOfDay())
      case LocalDateTime -> {
        def micros = ((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() * 1000
        group.append(fieldName, (long) micros)
      }
      case Timestamp -> {
        def micros = ((Timestamp) value).toInstant().toEpochMilli() * 1000
        group.append(fieldName, (long) micros)
      }
      case Date -> group.append(fieldName, ((Date) value).time)
      default -> group.append(fieldName, value.toString())
    }
  }

  private static void writeList(Group group, String fieldName, GroupType groupType, Object value) {
    if (!(value instanceof Collection)) {
      throw new IllegalArgumentException("Expected a Collection for list field '$fieldName' but got ${value.class}")
    }
    Collection<?> collection = (Collection<?>) value
    if (collection.isEmpty()) {
      group.addGroup(fieldName)
      return
    }
    Group listGroup = group.addGroup(fieldName)
    GroupType repeatedType = groupType.getType(0).asGroupType()
    Type elementType = repeatedType.getType(0)
    collection.each { Object element ->
      Group entry = listGroup.addGroup('list')
      writeValue(entry, elementType.name, elementType, element)
    }
  }

  private static void writeMap(Group group, String fieldName, GroupType groupType, Object value) {
    if (!(value instanceof Map)) {
      throw new IllegalArgumentException("Expected a Map for field '$fieldName' but got ${value.class}")
    }
    Map<?, ?> mapValue = (Map<?, ?>) value
    Group mapGroup = group.addGroup(fieldName)
    GroupType keyValueType = groupType.getType(0).asGroupType()
    PrimitiveType keyPrimitive = keyValueType.getType(0).asPrimitiveType()
    Type valueType = keyValueType.getFieldCount() > 1 ? keyValueType.getType(1) : null
    mapValue.each { Object k, Object v ->
      Group kvGroup = mapGroup.addGroup('key_value')
      writePrimitiveValue(kvGroup, keyPrimitive.name, keyPrimitive, k)
      if (valueType != null && v != null) {
        writeValue(kvGroup, valueType.name, valueType, v)
      }
    }
  }

  private static void writeStruct(Group group, String fieldName, GroupType groupType, Object value) {
    Map<String, Object> structValues = toStructMap(value)
    Group structGroup = group.addGroup(fieldName)
    groupType.fields.each { Type field ->
      def childValue = structValues.get(field.name)
      if (childValue != null) {
        writeValue(structGroup, field.name, field, childValue)
      }
    }
  }

  private static Map<String, Object> toStructMap(Object value) {
    if (value instanceof Map mapValue) {
      LinkedHashMap<String, Object> result = new LinkedHashMap<>()
      mapValue.each { k, v -> result[String.valueOf(k)] = v }
      return result
    }
    LinkedHashMap<String, Object> map = new LinkedHashMap<>()
    def beanInfo = Introspector.getBeanInfo(value.class, Object)
    beanInfo.propertyDescriptors.findAll { it.readMethod != null }.each { PropertyDescriptor pd ->
      def method = pd.readMethod
      if (!method.accessible) {
        method.accessible = true
      }
      map[pd.name] = method.invoke(value)
    }
    return map
  }

  /**
   * Infers the required maximum precision and scale for all BigDecimal columns in the matrix.
   * It performs the calculation in a single pass:
   * 1. It tracks the running maximum scale (S).
   * 2. It tracks the running maximum number of digits to the LEFT of the decimal (L).
   * 3. The final precision P = L + S.
   * This guarantees that the resulting schema is large enough to contain all values.
   * @param matrix the matrix
   * @return a Map of column name to [precision, scale] array.
   */
  static Map<String, int[]> inferDecimalPrecisionAndScale(Matrix matrix) {
    Map<String, int[]> result = [:] // columnName -> [precision, scale]

    matrix.columnNames().each { col ->
      if (matrix.type(col) == BigDecimal) {
        int maxScale = 0
        int maxLeftDigits = 0 // Max digits to the left of the decimal

        (0..<matrix.rowCount()).each { i ->
          def value = matrix[i, col]
          if (value instanceof BigDecimal) {
            int valScale = value.scale()
            maxScale = Math.max(maxScale, valScale)

            // Calculate digits to the left of the decimal
            // precision() - scale() works for most numbers (e.g., 123.45 -> p=5, s=2 -> 3)
            // but fails for numbers < 1 (e.g., 0.45 -> p=2, s=2 -> 0)
            // We must ensure at least one digit (for the '0')
            int leftDigits = Math.max(1, value.precision() - value.scale())
            maxLeftDigits = Math.max(maxLeftDigits, leftDigits)
          }
        }

        // Total precision = max digits left + max digits right (max scale)
        int maxPrecision = maxLeftDigits + maxScale

        // Ensure precision is at least 1 (e.g., for a column of all 0s)
        maxPrecision = Math.max(1, maxPrecision)

        result[col] = [maxPrecision, maxScale] as int[]
      }
    }
    //println "Inferred decimal precision/scale: $result"
    return result
  }


  static int minBytesForPrecision(int precision) {
    // According to the Parquet spec
    def t = (Math.log(Math.pow(10, precision)) / Math.log(2) + 1) as double
    return Math.ceil( t / 8d) as int
  }
}

