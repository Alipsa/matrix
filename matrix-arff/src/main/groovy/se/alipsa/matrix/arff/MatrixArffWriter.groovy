package se.alipsa.matrix.arff

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix

import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Writes Matrix objects to ARFF (Attribute-Relation File Format) files.
 */
@CompileStatic
class MatrixArffWriter {

  private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

  /** Write to a File. */
  static void write(Matrix matrix, File file) {
    write(matrix, file, new ArffWriteOptions())
  }

  /** Write to a Path. */
  static void write(Matrix matrix, Path path) {
    write(matrix, path, new ArffWriteOptions())
  }

  /** Write to an OutputStream. */
  static void write(Matrix matrix, OutputStream output) {
    write(matrix, output, new ArffWriteOptions())
  }

  /** Write to a Writer. */
  static void write(Matrix matrix, Writer writer) {
    write(matrix, writer, new ArffWriteOptions())
  }

  /** Write with explicit nominal value mappings. */
  static void write(Matrix matrix, File file, Map<String, List<String>> nominalMappings) {
    write(matrix, file, new ArffWriteOptions().nominalMappings(nominalMappings))
  }

  /** Write with explicit nominal value mappings. */
  static void write(Matrix matrix, Writer writer, Map<String, List<String>> nominalMappings) {
    write(matrix, writer, new ArffWriteOptions().nominalMappings(nominalMappings))
  }

  /** Write to a File using typed ARFF write options. */
  static void write(Matrix matrix, File file, ArffWriteOptions options) {
    validateMatrix(matrix)
    File output = ensureFileOutput(matrix, file)
    OutputStream outputStream = new FileOutputStream(output)
    OutputStreamWriter writer = null
    try {
      writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
      write(matrix, writer, options)
      writer.flush()
    } finally {
      if (writer != null) {
        writer.close()
      } else {
        outputStream.close()
      }
    }
  }

  /** Write to a Path using typed ARFF write options. */
  static void write(Matrix matrix, Path path, ArffWriteOptions options) {
    write(matrix, path.toFile(), options)
  }

  /** Write to an OutputStream using typed ARFF write options. */
  static void write(Matrix matrix, OutputStream output, ArffWriteOptions options) {
    validateMatrix(matrix)
    OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)
    write(matrix, writer, options)
    writer.flush()
  }

  /** Write to a Writer using typed ARFF write options. */
  static void write(Matrix matrix, Writer writer, ArffWriteOptions options) {
    validateMatrix(matrix)
    PrintWriter pw = writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer)
    writeMatrix(matrix, pw, options ?: new ArffWriteOptions())
    pw.flush()
  }

  /** Write Matrix to a String in ARFF format. */
  static String writeString(Matrix matrix) {
    writeString(matrix, new ArffWriteOptions())
  }

  /** Write Matrix to a String in ARFF format with explicit nominal mappings. */
  static String writeString(Matrix matrix, Map<String, List<String>> nominalMappings) {
    writeString(matrix, new ArffWriteOptions().nominalMappings(nominalMappings))
  }

  /** Write Matrix to a String in ARFF format using typed ARFF write options. */
  static String writeString(Matrix matrix, ArffWriteOptions options) {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }
    StringWriter writer = new StringWriter()
    write(matrix, writer, options)
    writer.toString()
  }

  private static void writeMatrix(Matrix matrix, PrintWriter pw, ArffWriteOptions options) {
    validateWriteOptions(matrix, options)

    String relationName = matrix.matrixName ?: 'matrix'
    pw.println("@RELATION ${escapeIdentifier(relationName)}")
    pw.println()

    List<String> columnNames = matrix.columnNames()
    List<ArffAttributeInfo> attributeInfos = []
    for (String colName : columnNames) {
      ArffAttributeInfo info = resolveAttributeInfo(matrix, colName, options)
      attributeInfos << info
      pw.println("@ATTRIBUTE ${escapeIdentifier(colName)} ${info.typeDeclaration}")
    }

    pw.println()
    pw.println('@DATA')

    int rowCount = matrix.rowCount()
    for (int row = 0; row < rowCount; row++) {
      StringBuilder line = new StringBuilder()
      for (int col = 0; col < columnNames.size(); col++) {
        if (col > 0) {
          line.append(',')
        }
        line.append(formatValue(matrix[row, col], attributeInfos[col]))
      }
      pw.println(line.toString())
    }
  }

  private static ArffAttributeInfo resolveAttributeInfo(Matrix matrix, String colName, ArffWriteOptions options) {
    Class colType = matrix.type(colName)
    ArffTypeDecl type = explicitTypeForColumn(colName, options)
    if (type != null) {
      return createAttributeInfo(matrix, colName, colType, type, options)
    }
    if (isIntegerType(colType)) {
      return createAttributeInfo(matrix, colName, colType, ArffTypeDecl.INTEGER, options)
    }
    if (isNumericType(colType)) {
      return createAttributeInfo(matrix, colName, colType, ArffTypeDecl.NUMERIC, options)
    }
    if (isDateType(colType)) {
      return createAttributeInfo(matrix, colName, colType, ArffTypeDecl.DATE, options)
    }
    if (options.inferNominals && (colType == String || colType == Object)) {
      LinkedHashSet<String> uniqueValues = collectUniqueStringValues(matrix, colName)
      if (shouldBeNominal(uniqueValues, matrix.rowCount(), options.nominalThreshold)) {
        return createNominalInfo(colName, uniqueValues as List<String>, false)
      }
    }
    createAttributeInfo(matrix, colName, colType, ArffTypeDecl.STRING, options)
  }

  private static ArffTypeDecl explicitTypeForColumn(String colName, ArffWriteOptions options) {
    if (options.attributeTypesByColumn.containsKey(colName)) {
      return options.attributeTypesByColumn[colName]
    }
    if (options.stringColumns.contains(colName)) {
      return ArffTypeDecl.STRING
    }
    if (options.nominalColumns.contains(colName) || options.nominalMappings.containsKey(colName)) {
      return ArffTypeDecl.NOMINAL
    }
    null
  }

  private static ArffAttributeInfo createAttributeInfo(Matrix matrix, String colName, Class colType,
                                                       ArffTypeDecl type, ArffWriteOptions options) {
    return switch (type) {
      case ArffTypeDecl.NUMERIC -> new ArffAttributeInfo(ArffTypeDecl.NUMERIC, 'NUMERIC')
      case ArffTypeDecl.REAL -> new ArffAttributeInfo(ArffTypeDecl.REAL, 'REAL')
      case ArffTypeDecl.INTEGER -> new ArffAttributeInfo(ArffTypeDecl.INTEGER, 'INTEGER')
      case ArffTypeDecl.STRING -> new ArffAttributeInfo(ArffTypeDecl.STRING, 'STRING')
      case ArffTypeDecl.DATE -> {
        String dateFormat = resolveDateFormat(colName, options)
        yield new ArffAttributeInfo(ArffTypeDecl.DATE, "DATE '${escapeQuotedContent(dateFormat)}'", null, dateFormat)
      }
      case ArffTypeDecl.NOMINAL -> {
        List<String> nominalValues = nominalValuesForColumn(matrix, colName, colType, options)
        yield createNominalInfo(colName, nominalValues, options.nominalMappings.containsKey(colName))
      }
      default -> throw new IllegalArgumentException("Unsupported ArffTypeDecl: $type")
    }
  }

  private static List<String> nominalValuesForColumn(Matrix matrix, String colName, Class colType, ArffWriteOptions options) {
    if (options.nominalMappings.containsKey(colName)) {
      return options.nominalMappings[colName]
    }
    if (colType != String && colType != Object) {
      throw new IllegalArgumentException(
          "Column '$colName' cannot be written as NOMINAL without nominalMappings because its type is ${colType?.simpleName}")
    }
    collectUniqueStringValues(matrix, colName) as List<String>
  }

  private static ArffAttributeInfo createNominalInfo(String colName, List<String> nominalValues, boolean explicitValues) {
    if (nominalValues == null || nominalValues.isEmpty()) {
      String guidance = explicitValues
          ? "nominalMappings for column '$colName' must contain at least one value"
          : "Cannot write column '$colName' as NOMINAL because it has no non-null values. Provide nominalMappings to define the allowed nominal values."
      throw new IllegalArgumentException(guidance)
    }
    validateNominalValues(colName, nominalValues)
    String typeDecl = "{${nominalValues.collect { String value -> escapeNominalValue(value) }.join(',')}}"
    new ArffAttributeInfo(ArffTypeDecl.NOMINAL, typeDecl, nominalValues)
  }

  private static String resolveDateFormat(String colName, ArffWriteOptions options) {
    options.dateFormatsByColumn[colName] ?: options.dateFormat ?: DEFAULT_DATE_FORMAT
  }

  private static void validateWriteOptions(Matrix matrix, ArffWriteOptions options) {
    Set<String> matrixColumns = matrix.columnNames() as Set<String>
    validateColumnsExist('nominalMappings', options.nominalMappings.keySet(), matrixColumns)
    validateColumnsExist('nominalColumns', options.nominalColumns, matrixColumns)
    validateColumnsExist('stringColumns', options.stringColumns, matrixColumns)
    validateColumnsExist('attributeTypesByColumn', options.attributeTypesByColumn.keySet(), matrixColumns)
    validateColumnsExist('dateFormatsByColumn', options.dateFormatsByColumn.keySet(), matrixColumns)

    Set<String> overlappingColumns = options.nominalColumns.intersect(options.stringColumns) as Set<String>
    if (!overlappingColumns.isEmpty()) {
      throw new IllegalArgumentException("Columns cannot be configured as both nominalColumns and stringColumns: ${overlappingColumns}")
    }

    for (Map.Entry<String, ArffTypeDecl> entry : options.attributeTypesByColumn.entrySet()) {
      String colName = entry.key
      ArffTypeDecl type = entry.value
      if (options.stringColumns.contains(colName) && type != ArffTypeDecl.STRING) {
        throw new IllegalArgumentException("Column '$colName' is configured in stringColumns and attributeTypesByColumn=$type")
      }
      if ((options.nominalColumns.contains(colName) || options.nominalMappings.containsKey(colName)) && type != ArffTypeDecl.NOMINAL) {
        throw new IllegalArgumentException("Column '$colName' has nominal configuration but attributeTypesByColumn=$type")
      }
      if (options.dateFormatsByColumn.containsKey(colName) && type != ArffTypeDecl.DATE) {
        throw new IllegalArgumentException("Column '$colName' has dateFormatsByColumn configured but attributeTypesByColumn=$type")
      }
    }

    for (String colName : options.dateFormatsByColumn.keySet()) {
      if (options.stringColumns.contains(colName)) {
        throw new IllegalArgumentException("Column '$colName' cannot be configured in both stringColumns and dateFormatsByColumn")
      }
      if (options.nominalColumns.contains(colName) || options.nominalMappings.containsKey(colName)) {
        throw new IllegalArgumentException("Column '$colName' cannot be configured as nominal and also use dateFormatsByColumn")
      }
      if (!options.attributeTypesByColumn.containsKey(colName) && !isDateType(matrix.type(colName))) {
        throw new IllegalArgumentException("dateFormatsByColumn[$colName] requires a DATE column or attributeTypesByColumn[$colName]=DATE")
      }
    }

    for (Map.Entry<String, List<String>> entry : options.nominalMappings.entrySet()) {
      validateNominalValues(entry.key, entry.value)
    }
  }

  private static void validateColumnsExist(String optionName, Collection<String> configuredColumns, Set<String> matrixColumns) {
    Set<String> unknownColumns = configuredColumns.findAll { String colName -> !matrixColumns.contains(colName) } as Set<String>
    if (!unknownColumns.isEmpty()) {
      throw new IllegalArgumentException("$optionName references unknown columns: ${unknownColumns}")
    }
  }

  private static void validateNominalValues(String colName, List<String> nominalValues) {
    Set<String> seen = [] as Set<String>
    for (String nominalValue : nominalValues) {
      if (nominalValue == null) {
        throw new IllegalArgumentException("Nominal values for column '$colName' must not contain null")
      }
      if (!seen.add(nominalValue)) {
        throw new IllegalArgumentException("Nominal values for column '$colName' must not contain duplicates: $nominalValue")
      }
    }
  }

  private static boolean isNumericType(Class type) {
    type in [BigDecimal, Double, Float, Long, BigInteger, double.class, float.class, long.class, Number]
  }

  private static boolean isIntegerType(Class type) {
    type in [Integer, Short, Byte, int.class, short.class, byte.class]
  }

  private static boolean isDateType(Class type) {
    type in [Date, java.sql.Date, Timestamp, LocalDate, LocalDateTime, Instant]
  }

  private static LinkedHashSet<String> collectUniqueStringValues(Matrix matrix, String colName) {
    LinkedHashSet<String> values = [] as LinkedHashSet<String>
    int rowCount = matrix.rowCount()
    for (int row = 0; row < rowCount; row++) {
      Object value = matrix[row, colName]
      if (value != null) {
        values << value.toString()
      }
    }
    values
  }

  private static boolean shouldBeNominal(Set<String> uniqueValues, int rowCount, int nominalThreshold) {
    if (uniqueValues.isEmpty()) {
      return false
    }
    int uniqueCount = uniqueValues.size()
    uniqueCount <= nominalThreshold && (rowCount < 10 || uniqueCount <= rowCount * 0.1)
  }

  private static String formatValue(Object value, ArffAttributeInfo info) {
    if (value == null) {
      return '?'
    }
    return switch (info.type) {
      case ArffTypeDecl.NUMERIC, ArffTypeDecl.REAL, ArffTypeDecl.INTEGER -> value.toString()
      case ArffTypeDecl.DATE -> formatDate(value, info)
      case ArffTypeDecl.NOMINAL -> escapeNominalValue(value.toString())
      case ArffTypeDecl.STRING -> escapeStringValue(value.toString())
      default -> throw new IllegalArgumentException("Unsupported ArffTypeDecl: ${info.type}")
    }
  }

  private static String formatDate(Object value, ArffAttributeInfo info) {
    SimpleDateFormat sdf = new SimpleDateFormat(info.dateFormat ?: DEFAULT_DATE_FORMAT)
    if (value instanceof Date) {
      return "'${sdf.format((Date) value)}'"
    }
    if (value instanceof LocalDate) {
      Date date = java.sql.Date.valueOf((LocalDate) value)
      return "'${sdf.format(date)}'"
    }
    if (value instanceof LocalDateTime) {
      Date date = Timestamp.valueOf((LocalDateTime) value)
      return "'${sdf.format(date)}'"
    }
    if (value instanceof Instant) {
      Date date = Date.from((Instant) value)
      return "'${sdf.format(date)}'"
    }
    "'${escapeQuotedContent(value.toString())}'"
  }

  private static String escapeIdentifier(String name) {
    if (name.contains(' ') || name.contains(',') || name.contains('{') ||
        name.contains('}') || name.contains('%') || name.contains("'") ||
        name.contains('"')) {
      return "'${escapeQuotedContent(name)}'"
    }
    name
  }

  private static String escapeNominalValue(String value) {
    if (value.contains(',') || value.contains(' ') || value.contains("'") ||
        value.contains('"') || value.contains('{') || value.contains('}')) {
      return "'${escapeQuotedContent(value)}'"
    }
    value
  }

  private static String escapeStringValue(String value) {
    "'${escapeQuotedContent(value)}'"
  }

  private static String escapeQuotedContent(String value) {
    value.replace("\\", "\\\\").replace("'", "\\'")
  }

  private static void validateMatrix(Matrix matrix) {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }
    if (matrix.columnCount() == 0) {
      throw new IllegalArgumentException("Matrix must have at least one column")
    }
  }

  private static File ensureFileOutput(Matrix matrix, File output) {
    if (output == null) {
      throw new IllegalArgumentException("File or directory cannot be null")
    }
    if (output.isDirectory()) {
      output = new File(output, safeFileName(matrix.matrixName) + '.arff')
    }
    if (output.parentFile != null && !output.parentFile.exists()) {
      if (!output.parentFile.mkdirs() && !output.parentFile.exists()) {
        throw new IllegalArgumentException("Failed to create directory: ${output.parentFile.absolutePath}")
      }
    }
    output
  }

  private static String safeFileName(String name) {
    String baseName = name?.trim()
    if (baseName == null || baseName.isEmpty()) {
      return 'matrix'
    }
    baseName = baseName.replace('\\', '_').replace('/', '_')
    baseName = baseName.replace('..', '_')
    baseName = baseName.replaceAll(/[^A-Za-z0-9._-]/, '_')
    if (baseName.isEmpty() || baseName == '.' || baseName == '..') {
      return 'matrix'
    }
    baseName
  }
}

@CompileStatic
class ArffAttributeInfo {
  ArffTypeDecl type
  String typeDeclaration
  List<String> nominalValues
  String dateFormat

  ArffAttributeInfo(ArffTypeDecl type, String typeDeclaration, List<String> nominalValues = null, String dateFormat = null) {
    this.type = type
    this.typeDeclaration = typeDeclaration
    this.nominalValues = nominalValues
    this.dateFormat = dateFormat
  }
}
