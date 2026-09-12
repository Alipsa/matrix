package se.alipsa.matrix.arff

import se.alipsa.matrix.core.Matrix

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Writes Matrix objects to ARFF (Attribute-Relation File Format) files.
 */
class MatrixArffWriter {

  private static final String MATRIX_NULL_MESSAGE = 'Matrix cannot be null'
  private static final String DEFAULT_MATRIX_BASE_NAME = 'matrix'
  private static final String COMMA = ','
  private static final String QUESTION_MARK = '?'
  private static final String OPEN_BRACE = '{'
  private static final String CLOSE_BRACE = '}'
  private static final String BACKSLASH = '\\'
  private static final String UNDERSCORE = '_'
  private static final String DOUBLE_DOT = '..'

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
    // validate before the file is created so an invalid configuration leaves no empty or partial file behind
    validateWriteOptions(matrix, options ?: new ArffWriteOptions())
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
      throw new IllegalArgumentException(MATRIX_NULL_MESSAGE)
    }
    StringWriter writer = new StringWriter()
    write(matrix, writer, options)
    writer.toString()
  }

  private static void writeMatrix(Matrix matrix, PrintWriter pw, ArffWriteOptions options) {
    validateWriteOptions(matrix, options)

    // Resolve the whole schema before printing so a validation failure never leaves a partial document behind
    String weightColumn = options.instanceWeightColumn
    List<String> columnNames = matrix.columnNames().findAll { String name -> name != weightColumn }
    List<ArffAttributeInfo> attributeInfos = columnNames.collect { String colName ->
      resolveAttributeInfo(matrix, colName, options)
    }

    String relationName = matrix.matrixName ?: DEFAULT_MATRIX_BASE_NAME
    pw.println("@RELATION ${escapeIdentifier(relationName)}")
    pw.println()

    for (int i = 0; i < columnNames.size(); i++) {
      pw.println("@ATTRIBUTE ${escapeIdentifier(columnNames[i])} ${attributeInfos[i].typeDeclaration}")
    }

    pw.println()
    pw.println('@DATA')

    int rowCount = matrix.rowCount()
    for (int row = 0; row < rowCount; row++) {
      pw.println(formatRow(matrix, row, columnNames, attributeInfos, weightColumn))
    }
  }

  /** One data row: the attribute values, then `,{w}` when {@code weightColumn} is set and the cell is not null. */
  private static String formatRow(Matrix matrix, int row, List<String> columnNames, List<ArffAttributeInfo> infos,
                                  String weightColumn) {
    StringBuilder line = new StringBuilder()
    for (int col = 0; col < columnNames.size(); col++) {
      if (col > 0) {
        line.append(COMMA)
      }
      line.append(formatValue(matrix[row, columnNames[col]], infos[col]))
    }
    if (weightColumn != null) {
      Object weight = matrix[row, weightColumn]
      if (weight != null) {
        line.append(COMMA).append(OPEN_BRACE).append(formatWeight(weight, weightColumn, row)).append(CLOSE_BRACE)
      }
    }
    line.toString()
  }

  private static String formatWeight(Object weight, String weightColumn, int row) {
    if (!(weight instanceof Number) || isNonFinite(weight)) {
      throw new IllegalArgumentException(
          "instanceWeightColumn '$weightColumn' must hold finite numbers but row $row holds ${weight.class.simpleName} $weight")
    }
    weight.toString()
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
      Set<String> uniqueValues = collectUniqueStringValues(matrix, colName)
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
        yield new ArffAttributeInfo(ArffTypeDecl.DATE, "DATE '${ArffEscapes.escape(dateFormat)}'", null, dateFormat)
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
    String typeDecl = "{${nominalValues.collect { String value -> escapeNominalValue(value) }.join(COMMA)}}"
    new ArffAttributeInfo(ArffTypeDecl.NOMINAL, typeDecl, nominalValues)
  }

  private static String resolveDateFormat(String colName, ArffWriteOptions options) {
    options.dateFormatsByColumn[colName] ?: options.dateFormat ?: ArffDateFormats.DEFAULT_PATTERN
  }

  private static void validateWriteOptions(Matrix matrix, ArffWriteOptions options) {
    Set<String> matrixColumns = matrix.columnNames() as Set<String>
    validateColumnsExist('nominalMappings', options.nominalMappings.keySet(), matrixColumns)
    validateColumnsExist('nominalColumns', options.nominalColumns, matrixColumns)
    validateColumnsExist('stringColumns', options.stringColumns, matrixColumns)
    validateColumnsExist('attributeTypesByColumn', options.attributeTypesByColumn.keySet(), matrixColumns)
    validateColumnsExist('dateFormatsByColumn', options.dateFormatsByColumn.keySet(), matrixColumns)
    validateWeightColumn(matrix, options)

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

  private static void validateWeightColumn(Matrix matrix, ArffWriteOptions options) {
    String weightColumn = options.instanceWeightColumn
    if (weightColumn == null) {
      return
    }
    List<String> matrixColumns = matrix.columnNames()
    if (!matrixColumns.contains(weightColumn)) {
      throw new IllegalArgumentException("instanceWeightColumn '$weightColumn' references an unknown column")
    }
    if (matrixColumns.size() == 1) {
      throw new IllegalArgumentException("instanceWeightColumn '$weightColumn' cannot be the only column")
    }
    boolean configuredElsewhere = options.nominalMappings.containsKey(weightColumn) ||
        options.nominalColumns.contains(weightColumn) || options.stringColumns.contains(weightColumn) ||
        options.attributeTypesByColumn.containsKey(weightColumn) || options.dateFormatsByColumn.containsKey(weightColumn)
    if (configuredElsewhere) {
      throw new IllegalArgumentException("instanceWeightColumn '$weightColumn' cannot also be configured as an attribute")
    }
    Class weightType = matrix.type(weightColumn)
    if (weightType != Object && !isNumericType(weightType) && !isIntegerType(weightType)) {
      throw new IllegalArgumentException(
          "instanceWeightColumn '$weightColumn' must be numeric or Object but its declared type is ${weightType.simpleName}")
    }
    for (int row = 0; row < matrix.rowCount(); row++) {
      Object weight = matrix[row, weightColumn]
      if (weight != null) {
        formatWeight(weight, weightColumn, row)
      }
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

  private static Set<String> collectUniqueStringValues(Matrix matrix, String colName) {
    Set<String> values = [] as LinkedHashSet<String>
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
      return QUESTION_MARK
    }
    return switch (info.type) {
      case ArffTypeDecl.NUMERIC, ArffTypeDecl.REAL, ArffTypeDecl.INTEGER -> isNonFinite(value) ? QUESTION_MARK : value.toString()
      case ArffTypeDecl.DATE -> formatDate(value, info)
      case ArffTypeDecl.NOMINAL -> escapeNominalValue(value.toString())
      case ArffTypeDecl.STRING -> escapeStringValue(value.toString())
      default -> throw new IllegalArgumentException("Unsupported ArffTypeDecl: ${info.type}")
    }
  }

  /** Weka has no representation for NaN or infinity (NaN is its internal missing marker), so they are written as `?`. */
  private static boolean isNonFinite(Object value) {
    if (value instanceof Double) {
      return value.isNaN() || value.isInfinite()
    }
    if (value instanceof Float) {
      return value.isNaN() || value.isInfinite()
    }
    false
  }

  private static String formatDate(Object value, ArffAttributeInfo info) {
    SimpleDateFormat sdf = info.dateFormatter ?: ArffDateFormats.create(ArffDateFormats.DEFAULT_PATTERN)
    if (value instanceof Date) {
      return "'${sdf.format((Date) value)}'"
    }
    if (value instanceof LocalDate) {
      Date date = Date.from(value.atStartOfDay(ZoneOffset.UTC).toInstant())
      return "'${sdf.format(date)}'"
    }
    if (value instanceof LocalDateTime) {
      Date date = Date.from(value.toInstant(ZoneOffset.UTC))
      return "'${sdf.format(date)}'"
    }
    if (value instanceof Instant) {
      Date date = Date.from((Instant) value)
      return "'${sdf.format(date)}'"
    }
    ArffEscapes.quote(value.toString())
  }

  private static String escapeIdentifier(String name) {
    ArffEscapes.quoteIfNeeded(name)
  }

  private static String escapeNominalValue(String value) {
    ArffEscapes.quoteIfNeeded(value)
  }

  private static String escapeStringValue(String value) {
    ArffEscapes.quote(value)
  }

  private static void validateMatrix(Matrix matrix) {
    if (matrix == null) {
      throw new IllegalArgumentException(MATRIX_NULL_MESSAGE)
    }
    if (matrix.columnCount() == 0) {
      throw new IllegalArgumentException('Matrix must have at least one column')
    }
  }

  private static File ensureFileOutput(Matrix matrix, File output) {
    if (output == null) {
      throw new IllegalArgumentException('File or directory cannot be null')
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
      return DEFAULT_MATRIX_BASE_NAME
    }
    baseName = baseName.replace(BACKSLASH, UNDERSCORE).replace('/', UNDERSCORE)
    baseName = baseName.replace(DOUBLE_DOT, UNDERSCORE)
    baseName = baseName.replaceAll(/[^A-Za-z0-9._-]/, UNDERSCORE)
    if (baseName.isEmpty() || baseName == '.' || baseName == DOUBLE_DOT) {
      return DEFAULT_MATRIX_BASE_NAME
    }
    baseName
  }
}

/** Resolved ARFF schema information for one written column. */
class ArffAttributeInfo {
  ArffTypeDecl type
  String typeDeclaration
  List<String> nominalValues
  String dateFormat
  SimpleDateFormat dateFormatter

  ArffAttributeInfo(ArffTypeDecl type, String typeDeclaration, List<String> nominalValues = null, String dateFormat = null) {
    this.type = type
    this.typeDeclaration = typeDeclaration
    this.nominalValues = nominalValues
    this.dateFormat = dateFormat
    this.dateFormatter = dateFormat == null ? null : ArffDateFormats.create(dateFormat)
  }
}
