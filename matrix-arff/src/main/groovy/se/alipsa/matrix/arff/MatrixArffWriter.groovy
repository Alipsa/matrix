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
 *
 * <p>ARFF format specification:</p>
 * <ul>
 *   <li>@RELATION defines the dataset name</li>
 *   <li>@ATTRIBUTE name type defines columns (NUMERIC, REAL, INTEGER, STRING, DATE, or {nominal values})</li>
 *   <li>@DATA marks the start of data rows</li>
 *   <li>Data rows are comma-separated values</li>
 * </ul>
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * Matrix m = Matrix.builder("iris")
 *   .data(sepalLength: [5.1, 4.9], sepalWidth: [3.5, 3.0])
 *   .build()
 *
 * // Write to file
 * MatrixArffWriter.write(m, new File("iris.arff"))
 *
 * // Write to String
 * String arff = MatrixArffWriter.writeString(m)
 *
 * // Write with custom nominal values
 * Map&lt;String, List&lt;String&gt;&gt; nominals = [species: ["setosa", "versicolor", "virginica"]]
 * MatrixArffWriter.write(m, file, nominals)
 * String arffStr = MatrixArffWriter.writeString(m, nominals)
 * </pre>
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

  /**
   * Write with custom nominal value mappings for specific columns.
   * This allows explicit control over which columns should be treated as nominal
   * and what their allowed values are.
   */
  static void write(Matrix matrix, File file, Map<String, List<String>> nominalMappings) {
    write(matrix, file, new ArffWriteOptions().nominalMappings(nominalMappings))
  }

  /** Write with custom nominal value mappings. */
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
    writeMatrix(matrix, pw, options)
    pw.flush()
  }

  /**
   * Write Matrix to a String in ARFF format.
   *
   * @param matrix the Matrix to write
   * @return ARFF formatted string
   * @throws IllegalArgumentException if matrix is null
   */
  static String writeString(Matrix matrix) {
    writeString(matrix, new ArffWriteOptions())
  }

  /**
   * Write Matrix to a String in ARFF format with custom nominal value mappings.
   *
   * @param matrix the Matrix to write
   * @param nominalMappings map of column names to lists of nominal values
   * @return ARFF formatted string
   * @throws IllegalArgumentException if matrix is null
   */
  static String writeString(Matrix matrix, Map<String, List<String>> nominalMappings) {
    writeString(matrix, new ArffWriteOptions().nominalMappings(nominalMappings))
  }

  /**
   * Write Matrix to a String in ARFF format using typed ARFF write options.
   *
   * @param matrix the Matrix to write
   * @param options ARFF schema and formatting options
   * @return ARFF formatted string
   * @throws IllegalArgumentException if matrix is null
   */
  static String writeString(Matrix matrix, ArffWriteOptions options) {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }
    StringWriter stringWriter = new StringWriter()
    write(matrix, stringWriter, options)
    stringWriter.toString()
  }

  private static void writeMatrix(Matrix matrix, PrintWriter pw, ArffWriteOptions options) {
    ArffWriteOptions resolvedOptions = options ?: new ArffWriteOptions()
    validateWriteOptions(matrix, resolvedOptions)

    String relationName = matrix.matrixName ?: "matrix"
    pw.println("@RELATION ${escapeIdentifier(relationName)}")
    pw.println()

    List<String> columnNames = matrix.columnNames()
    List<ArffAttributeInfo> attributeInfos = resolveAttributeInfos(matrix, columnNames, resolvedOptions)
    for (int i = 0; i < columnNames.size(); i++) {
      pw.println("@ATTRIBUTE ${escapeIdentifier(columnNames[i])} ${attributeInfos[i].typeDeclaration}")
    }

    pw.println()
    pw.println("@DATA")

    int rowCount = matrix.rowCount()
    for (int row = 0; row < rowCount; row++) {
      StringBuilder line = new StringBuilder()
      for (int col = 0; col < columnNames.size(); col++) {
        if (col > 0) {
          line.append(',')
        }
        Object value = matrix[row, col]
        line.append(formatValue(value, attributeInfos[col]))
      }
      pw.println(line.toString())
    }
  }

  private static List<ArffAttributeInfo> resolveAttributeInfos(Matrix matrix, List<String> columnNames, ArffWriteOptions options) {
    List<ArffAttributeInfo> attributeInfos = []
    for (String colName : columnNames) {
      attributeInfos << resolveAttributeInfo(matrix, colName, options)
    }
    attributeInfos
  }

  private static ArffAttributeInfo resolveAttributeInfo(Matrix matrix, String colName, ArffWriteOptions options) {
    Class colType = matrix.type(colName)
    ArffTypeDecl explicitType = options.attributeTypesByColumn[colName]
    if (explicitType == null && options.stringColumns.contains(colName)) {
      explicitType = ArffTypeDecl.STRING
    }
    if (explicitType == null && (options.nominalColumns.contains(colName) || options.nominalMappings.containsKey(colName))) {
      explicitType = ArffTypeDecl.NOMINAL
    }
    if (explicitType != null) {
      return createAttributeInfo(matrix, colName, colType, explicitType, options)
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

  private static ArffAttributeInfo createAttributeInfo(Matrix matrix, String colName, Class colType,
                                                       ArffTypeDecl type, ArffWriteOptions options) {
    return switch (type) {
      case ArffTypeDecl.NUMERIC -> new ArffAttributeInfo(ArffTypeDecl.NUMERIC, 'NUMERIC')
      case ArffTypeDecl.REAL -> new ArffAttributeInfo(ArffTypeDecl.REAL, 'REAL')
      case ArffTypeDecl.INTEGER -> new ArffAttributeInfo(ArffTypeDecl.INTEGER, 'INTEGER')
      case ArffTypeDecl.STRING -> new ArffAttributeInfo(ArffTypeDecl.STRING, 'STRING')
      case ArffTypeDecl.DATE -> createDateInfo(colName, options)
      case ArffTypeDecl.NOMINAL -> {
        List<String> nominalValues = nominalValuesForColumn(matrix, colName, colType, options)
        yield createNominalInfo(colName, nominalValues, options.nominalMappings.containsKey(colName))
      }
    }
  }

  private static ArffAttributeInfo createDateInfo(String colName, ArffWriteOptions options) {
    String dateFormat = resolveDateFormat(colName, options)
    new ArffAttributeInfo(ArffTypeDecl.DATE, "DATE '${escapeQuotedContent(dateFormat)}'", null, dateFormat)
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
        throw new IllegalArgumentException(
            "Column '$colName' is configured in stringColumns and attributeTypesByColumn=$type")
      }
      if ((options.nominalColumns.contains(colName) || options.nominalMappings.containsKey(colName)) && type != ArffTypeDecl.NOMINAL) {
        throw new IllegalArgumentException(
            "Column '$colName' has nominal configuration but attributeTypesByColumn=$type")
      }
      if (options.dateFormatsByColumn.containsKey(colName) && type != ArffTypeDecl.DATE) {
        throw new IllegalArgumentException(
            "Column '$colName' has dateFormatsByColumn configured but attributeTypesByColumn=$type")
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
        throw new IllegalArgumentException(
            "dateFormatsByColumn[$colName] requires a DATE column or attributeTypesByColumn[$colName]=DATE")
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
      Object val = matrix[row, colName]
      if (val != null) {
        values << val.toString()
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

  /**
   * Validate that the Matrix is not null and has columns.
   */
  private static void validateMatrix(Matrix matrix) {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }
    if (matrix.columnCount() == 0) {
      throw new IllegalArgumentException("Matrix must have at least one column")
    }
  }

  /**
   * Ensure the output File exists and is a regular file (not a directory).
   * If output is a directory, creates a file within it using the Matrix name.
   */
  private static File ensureFileOutput(Matrix matrix, File output) {
    if (output == null) {
      throw new IllegalArgumentException("File or directory cannot be null")
    }
    if (output.isDirectory()) {
      String fileName = safeFileName(matrix.matrixName) + '.arff'
      output = new File(output, fileName)
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

/** Enum representing ARFF type declarations. */
enum ArffTypeDecl {
  NUMERIC,
  REAL,
  INTEGER,
  STRING,
  NOMINAL,
  DATE
}

/** Class holding attribute information for writing. */
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
