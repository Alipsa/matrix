package se.alipsa.matrix.arff

import groovy.transform.Immutable
import groovy.transform.PackageScope

import se.alipsa.matrix.core.Matrix

import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Writes Matrix objects to ARFF (Attribute-Relation File Format) files.
 */
class MatrixArffWriter {

  private static final String LINE_SEPARATOR = System.lineSeparator()
  private static final List<String> BOOLEAN_NOMINAL_VALUES = ['false', 'true'].asImmutable()
  private static final String MATRIX_NULL_MESSAGE = 'Matrix cannot be null'
  private static final String DEFAULT_MATRIX_BASE_NAME = 'matrix'
  private static final String COMMA = ','
  private static final String QUESTION_MARK = '?'
  private static final String OPEN_BRACE = '{'
  private static final String CLOSE_BRACE = '}'
  private static final String BACKSLASH = '\\'
  private static final String UNDERSCORE = '_'
  private static final String DOT = '.'
  private static final String DOUBLE_DOT = '..'
  private static final String NESTED_INDENT = '  '

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
    if (file == null) {
      throw new IllegalArgumentException('File or directory cannot be null')
    }
    write(matrix, file.toPath(), options)
  }

  /** Write to a Path using typed ARFF write options. */
  static void write(Matrix matrix, Path path, ArffWriteOptions options) {
    if (path == null) {
      throw new IllegalArgumentException('Path cannot be null')
    }
    ArffWriteOptions resolved = options ?: new ArffWriteOptions()
    WriteSchema schema = resolveSchema(matrix, resolved)
    validateEncodability(matrix, schema, resolved.charset)
    Path target = Files.isDirectory(path) ? path.resolve(safeFileName(matrix.matrixName) + '.arff') : path
    ensureParentDirectories(target)
    Files.newOutputStream(target).withCloseable { OutputStream output ->
      encodedWriter(output, resolved.charset).withCloseable { Writer writer ->
        printMatrix(matrix, schema, writer)
      }
    }
  }

  /** Write to an OutputStream using typed ARFF write options; the caller retains ownership of the stream. */
  static void write(Matrix matrix, OutputStream output, ArffWriteOptions options) {
    ArffWriteOptions resolved = options ?: new ArffWriteOptions()
    WriteSchema schema = resolveSchema(matrix, resolved)
    validateEncodability(matrix, schema, resolved.charset)
    encodedWriter(output, resolved.charset).withCloseable { Writer writer ->
      printMatrix(matrix, schema, writer)
    }
  }

  /**
   * Write using typed options. The caller retains ownership of the writer.
   * If writer is a PrintWriter, it is flushed and its error state checked before and after writing.
   */
  static void write(Matrix matrix, Writer writer, ArffWriteOptions options) {
    if (writer instanceof PrintWriter && writer.checkError()) {
      throw new IllegalArgumentException('The supplied PrintWriter is already in an error state from an earlier write')
    }
    printMatrix(matrix, resolveSchema(matrix, options ?: new ArffWriteOptions()), writer)
    writer.flush()
    if (writer instanceof PrintWriter && writer.checkError()) {
      throw new IOException('Failed to write ARFF output (PrintWriter reported an error)')
    }
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

  @Immutable(knownImmutableClasses = [ArffAttributeInfo])
  private static class WriteSchema {
    String relationName
    List<String> columnNames
    List<Integer> columnIndexes
    List<ArffAttributeInfo> attributeInfos
    String weightColumn
    Integer weightColumnIndex
  }

  private static WriteSchema resolveSchema(Matrix matrix, ArffWriteOptions options) {
    validateMatrix(matrix)
    validateWriteOptions(matrix, options)

    // Resolve the whole schema before printing so a validation failure never leaves a partial document behind
    String weightColumn = options.instanceWeightColumn
    List<String> columnNames = matrix.columnNames().findAll { String name -> name != weightColumn }
    List<Integer> columnIndexes = columnNames.collect { String name -> matrix.columnIndex(name) }
    Integer weightColumnIndex = weightColumn == null ? null : matrix.columnIndex(weightColumn)
    List<ArffAttributeInfo> attributeInfos = columnNames.collect { String colName ->
      resolveAttributeInfo(matrix, colName, options)
    }

    columnNames.eachWithIndex { String name, int index -> validateCells(matrix, name, attributeInfos[index]) }
    new WriteSchema(matrix.matrixName ?: DEFAULT_MATRIX_BASE_NAME, columnNames, columnIndexes,
        attributeInfos, weightColumn, weightColumnIndex)
  }

  private static void printMatrix(Matrix matrix, WriteSchema schema, Writer writer) {
    writeLine(writer, "@RELATION ${escapeIdentifier(schema.relationName)}")
    writeLine(writer, '')
    schema.columnNames.eachWithIndex { String name, int index ->
      writeAttributeDeclaration(writer, name, schema.attributeInfos[index], '')
    }
    writeLine(writer, '')
    writeLine(writer, '@DATA')
    for (int row = 0; row < matrix.rowCount(); row++) {
      writeLine(writer, formatRow(matrix, row, schema.columnIndexes, schema.attributeInfos,
          schema.weightColumnIndex, schema.weightColumn))
    }
  }

  /** Finish even stateful encoders without closing a caller-owned byte stream. */
  private static Writer encodedWriter(OutputStream output, Charset charset) {
    OutputStream nonClosing = new FilterOutputStream(output) {
      @Override
      void write(byte[] bytes, int offset, int length) {
        out.write(bytes, offset, length)
      }

      @Override
      void close() {
        flush()
      }
    }
    new OutputStreamWriter(nonClosing, charset.newEncoder()
        .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT))
  }

  private static void writeLine(Writer writer, String line) {
    writer.write(line)
    writer.write(LINE_SEPARATOR)
  }

  private static void writeAttributeDeclaration(Writer writer, String colName, ArffAttributeInfo info, String indent) {
    String identifier = escapeIdentifier(colName)
    writeLine(writer, "${indent}@ATTRIBUTE $identifier ${info.typeDeclaration}")
    if (info.type == ArffTypeDecl.RELATIONAL) {
      String nestedIndent = indent + NESTED_INDENT
      for (int i = 0; i < info.relationalColumnNames.size(); i++) {
        writeAttributeDeclaration(writer, info.relationalColumnNames[i], info.relationalInfos[i], nestedIndent)
      }
      writeLine(writer, "${indent}@END $identifier")
    }
  }

  private static void validateEncodability(Matrix matrix, WriteSchema schema, Charset charset) {
    ArffEncoding encoding = new ArffEncoding(charset)
    encoding.check(schema.relationName, "Relation name '${schema.relationName}'")
    checkDeclarations(schema.columnNames, schema.attributeInfos, '', encoding)
    checkCellEncoding(matrix, schema.columnIndexes, schema.attributeInfos, '', encoding)
  }

  private static void checkDeclarations(List<String> names, List<ArffAttributeInfo> infos,
                                        String prefix, ArffEncoding encoding) {
    names.eachWithIndex { String name, int index ->
      ArffAttributeInfo info = infos[index]
      String path = prefix + name
      encoding.check(name, "Attribute name '$path'")
      encoding.check(info.typeDeclaration, "Declaration of column '$path'")
      if (info.type == ArffTypeDecl.RELATIONAL) {
        checkDeclarations(info.relationalColumnNames, info.relationalInfos, path + DOT, encoding)
      }
    }
  }

  private static void checkCellEncoding(Matrix matrix, List<Integer> indexes, List<ArffAttributeInfo> infos,
                                        String prefix, ArffEncoding encoding) {
    infos.eachWithIndex { ArffAttributeInfo info, int index ->
      boolean date = info.type == ArffTypeDecl.DATE
      if (info.type != ArffTypeDecl.STRING && !date && info.type != ArffTypeDecl.RELATIONAL) {
        return
      }
      String path = prefix + matrix.columnNames()[indexes[index]]
      int rows = matrix.rowCount()
      for (int row = 0; row < rows; row++) {
        Object value = matrix[row, indexes[index]]
        if (value == null) {
          continue
        }
        if (info.type == ArffTypeDecl.RELATIONAL) {
          checkCellEncoding(value as Matrix, info.relationalColumnIndexes, info.relationalInfos,
              "$path[$row].", encoding)
        } else if (!date || value instanceof CharSequence) {
          encoding.checkCell(value.toString(), path, row)
        } else if (info.dateMode == ArffDateMode.WEKA) {
          encoding.checkCell(formatDate(value, info), path, row)
        }
      }
    }
  }

  private static void validateCells(Matrix matrix, String colName, ArffAttributeInfo info) {
    if (info.type == ArffTypeDecl.STRING || info.type == ArffTypeDecl.RELATIONAL) {
      // STRING accepts every value; relational contents were validated while resolving their shared schema.
      return
    }
    int columnIndex = matrix.columnIndex(colName)
    int rowCount = matrix.rowCount()
    Set<String> allowed = info.type == ArffTypeDecl.NOMINAL ? info.nominalValues as Set<String> : null
    for (int row = 0; row < rowCount; row++) {
      Object value = matrix[row, columnIndex]
      if (value == null) {
        continue
      }
      boolean valid = switch (info.type) {
        case ArffTypeDecl.NOMINAL -> allowed.contains(value.toString())
        case ArffTypeDecl.NUMERIC, ArffTypeDecl.REAL, ArffTypeDecl.INTEGER -> validNumericCell(value, info.type)
        case ArffTypeDecl.DATE -> value instanceof Date || isDateType(value.class) || (value instanceof CharSequence &&
            ArffDateFormats.parsesCompletely(info.dateFormatter, value.toString()))
        default -> true
      }
      if (!valid) {
        throw new IllegalArgumentException("Column '$colName' row $row cannot be written as ${info.type}: $value; " +
            'use STRING or convert the column first (use null for missing numeric values)')
      }
    }
  }

  private static boolean validNumericCell(Object value, ArffTypeDecl type) {
    if (isNonFinite(value)) {
      return true
    }
    if (!(value instanceof Number) && !(value instanceof CharSequence)) {
      return false
    }
    if (value instanceof CharSequence) {
      CharSequence text = value
      for (int i = 0; i < text.length(); i++) {
        if (text.charAt(i) > 0x7F) {
          return false
        }
      }
    } else if (type != ArffTypeDecl.INTEGER || value instanceof Integer || value instanceof Long ||
        value instanceof Short || value instanceof Byte || value instanceof BigInteger) {
      return true
    }
    try {
      BigDecimal number = new BigDecimal(value.toString())
      type != ArffTypeDecl.INTEGER || number.stripTrailingZeros().scale() <= 0
    } catch (NumberFormatException ignored) {
      false
    }
  }

  /** One data row: the attribute values, then `,{w}` when {@code weightColumn} is set and the cell is not null. */
  private static String formatRow(Matrix matrix, int row, List<Integer> columnIndexes, List<ArffAttributeInfo> infos,
                                  Integer weightColumnIndex, String weightColumn) {
    StringBuilder line = new StringBuilder()
    for (int col = 0; col < columnIndexes.size(); col++) {
      if (col > 0) {
        line.append(COMMA)
      }
      line.append(formatValue(matrix[row, columnIndexes[col]], infos[col]))
    }
    if (weightColumnIndex != null) {
      Object weight = matrix[row, weightColumnIndex]
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
    ArffTypeDecl explicit = explicitTypeForColumn(colName, options)
    if (explicit != null) {
      return createAttributeInfo(matrix, colName, colType, explicit, options)
    }
    resolveInferredAttributeInfo([matrix], colName, options, resolveDateFormat(colName, options))
  }

  /**
   * Schema for a relational column across all matrices that hold it (one at the top level; several when the column
   * itself sits inside a relational value).
   */
  private static ArffAttributeInfo createRelationalInfo(List<Matrix> parts, String colName, ArffWriteOptions options) {
    List<Matrix> nested = []
    for (Matrix part : parts) {
      int rowCount = part.rowCount()
      for (int row = 0; row < rowCount; row++) {
        Object value = part[row, colName]
        if (value == null) {
          continue
        }
        if (!(value instanceof Matrix)) {
          throw new IllegalArgumentException(
              "Column '$colName' is written as RELATIONAL but row $row holds ${value.class.simpleName}")
        }
        nested.add(value)
      }
    }
    if (nested.isEmpty()) {
      throw new IllegalArgumentException("Column '$colName' cannot be written as RELATIONAL because it has no non-null values")
    }
    nested.each { Matrix part -> validateMatrix(part) }
    List<String> allNames = nested[0].columnNames()
    List<Class> allTypes = nested[0].types()
    for (Matrix part : nested) {
      if (part.columnNames() != allNames) {
        throw new IllegalArgumentException(
            "All values of relational column '$colName' must have the same columns: expected $allNames but found ${part.columnNames()}")
      }
      if (part.types() != allTypes) {
        throw new IllegalArgumentException(
            "All values of relational column '$colName' must have the same column types: expected $allTypes but found ${part.types()}")
      }
    }
    String weightColumn = options.instanceWeightColumn
    String nestedWeightColumn = allNames.contains(weightColumn) ? weightColumn : null
    List<String> nestedNames = allNames.findAll { String name -> name != nestedWeightColumn }
    List<Integer> nestedIndexes = nestedNames.collect { String name -> nested[0].columnIndex(name) }
    Integer nestedWeightColumnIndex = nestedWeightColumn == null ? null : nested[0].columnIndex(nestedWeightColumn)
    if (nestedNames.isEmpty()) {
      // Weka rejects a relational declaration without attributes (and so does the reader)
      throw new IllegalArgumentException(
          "Relational column '$colName' must have at least one column besides instanceWeightColumn '$weightColumn'")
    }
    if (nestedWeightColumn != null) {
      nested.each { Matrix part -> validateWeightCells(part, nestedWeightColumn) }
    }
    List<ArffAttributeInfo> nestedInfos = nestedNames.collect { String nestedName ->
      resolveInferredAttributeInfo(
          nested, nestedName, options, options.dateFormat ?: ArffDateFormats.DEFAULT_PATTERN)
    }
    nested.each { Matrix part ->
      nestedNames.eachWithIndex { String name, int index -> validateCells(part, name, nestedInfos[index]) }
    }
    new ArffAttributeInfo(nestedNames, nestedIndexes, nestedInfos, nestedWeightColumnIndex, nestedWeightColumn)
  }

  /** Infer one attribute over one or more matrices; explicit top-level type options are handled by the caller. */
  private static ArffAttributeInfo resolveInferredAttributeInfo(List<Matrix> parts, String colName,
                                                                ArffWriteOptions options, String dateFormat) {
    Class colType = parts[0].type(colName)
    if (colType == Matrix) {
      return createRelationalInfo(parts, colName, options)
    }
    if (isIntegerType(colType)) {
      return createAttributeInfo(parts[0], colName, colType, ArffTypeDecl.INTEGER, options)
    }
    if (isNumericType(colType)) {
      return createAttributeInfo(parts[0], colName, colType, ArffTypeDecl.NUMERIC, options)
    }
    if (isDateType(colType)) {
      return createDateInfo(dateFormat, options.dateMode)
    }
    if (isBooleanType(colType)) {
      return createNominalInfo(colName, BOOLEAN_NOMINAL_VALUES, false)
    }
    if (options.inferNominals && (colType == String || colType == Object)) {
      Set<String> uniqueValues = collectUniqueStringValues(parts, colName, options.nominalThreshold)
      int rowCount = parts.sum { Matrix part -> part.rowCount() } as int
      if (shouldBeNominal(uniqueValues, rowCount, options.nominalThreshold)) {
        return createNominalInfo(colName, uniqueValues as List<String>, false)
      }
    }
    createAttributeInfo(parts[0], colName, colType, ArffTypeDecl.STRING, options)
  }

  private static ArffAttributeInfo createDateInfo(String dateFormat, ArffDateMode dateMode) {
    new ArffAttributeInfo(ArffTypeDecl.DATE, "DATE '${ArffEscapes.escape(dateFormat)}'", null, dateFormat, dateMode)
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
      case ArffTypeDecl.DATE -> createDateInfo(resolveDateFormat(colName, options), options.dateMode)
      case ArffTypeDecl.RELATIONAL -> createRelationalInfo([matrix], colName, options)
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
    if (isBooleanType(colType)) {
      return BOOLEAN_NOMINAL_VALUES
    }
    if (colType != String && colType != Object) {
      throw new IllegalArgumentException(
          "Column '$colName' cannot be written as NOMINAL without nominalMappings because its type is ${colType?.simpleName}")
    }
    collectUniqueStringValues([matrix], colName, Integer.MAX_VALUE) as List<String>
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
    validateWeightCells(matrix, weightColumn)
  }

  private static void validateWeightCells(Matrix matrix, String weightColumn) {
    int rowCount = matrix.rowCount()
    for (int row = 0; row < rowCount; row++) {
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

  private static boolean isBooleanType(Class type) {
    type == Boolean || type == boolean.class
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

  private static Set<String> collectUniqueStringValues(List<Matrix> parts, String colName, int limit) {
    Set<String> values = [] as LinkedHashSet<String>
    for (Matrix part : parts) {
      int rowCount = part.rowCount()
      for (int row = 0; row < rowCount; row++) {
        Object value = part[row, colName]
        if (value != null) {
          values.add(value.toString())
          if (values.size() > limit) {
            return values
          }
        }
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
      case ArffTypeDecl.RELATIONAL -> formatRelational(value, info)
      default -> throw new IllegalArgumentException("Unsupported ArffTypeDecl: ${info.type}")
    }
  }

  private static String formatRelational(Object value, ArffAttributeInfo info) {
    if (!(value instanceof Matrix)) {
      throw new IllegalArgumentException("Relational value must be a Matrix but was ${value.class.simpleName}")
    }
    List<String> rows = []
    int rowCount = value.rowCount()
    for (int row = 0; row < rowCount; row++) {
      rows.add(formatRow(value, row, info.relationalColumnIndexes, info.relationalInfos,
          info.relationalWeightColumnIndex, info.relationalWeightColumn))
    }
    ArffEscapes.quote(rows.join('\n'))
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
    SimpleDateFormat sdf = info.dateFormatter ?: ArffDateFormats.create(ArffDateFormats.DEFAULT_PATTERN, info.dateMode)
    if (value instanceof Date) {
      return ArffEscapes.quote(sdf.format(value))
    }
    if (value instanceof LocalDate) {
      Date date = Date.from(value.atStartOfDay(ArffDateFormats.zone(info.dateMode)).toInstant())
      return ArffEscapes.quote(sdf.format(date))
    }
    if (value instanceof LocalDateTime) {
      Date date = Date.from(value.atZone(ArffDateFormats.zone(info.dateMode)).toInstant())
      return ArffEscapes.quote(sdf.format(date))
    }
    if (value instanceof Instant) {
      Date date = Date.from(value)
      return ArffEscapes.quote(sdf.format(date))
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
    List<String> names = matrix.columnNames()
    Set<String> duplicates = names.findAll { String name -> names.count(name) > 1 } as Set<String>
    if (!duplicates.isEmpty()) {
      throw new IllegalArgumentException("ARFF attribute names must be unique but the Matrix has duplicate column names: $duplicates")
    }
    if (matrix.columnCount() == 0) {
      throw new IllegalArgumentException('Matrix must have at least one column')
    }
  }

  /** Create parent directories when this path has a parent; single-name relative paths need no action. */
  @PackageScope
  static void ensureParentDirectories(Path path) {
    if (path.parent != null) {
      Files.createDirectories(path.parent)
    }
  }

  private static String safeFileName(String name) {
    String baseName = name?.trim()
    if (baseName == null || baseName.isEmpty()) {
      return DEFAULT_MATRIX_BASE_NAME
    }
    baseName = baseName.replace(BACKSLASH, UNDERSCORE).replace('/', UNDERSCORE)
    baseName = baseName.replace(DOUBLE_DOT, UNDERSCORE)
    baseName = baseName.replaceAll(/[^A-Za-z0-9._-]/, UNDERSCORE)
    if (baseName.isEmpty() || baseName == DOT || baseName == DOUBLE_DOT) {
      return DEFAULT_MATRIX_BASE_NAME
    }
    baseName
  }
}

/** Resolved ARFF schema information for one written column. */
class ArffAttributeInfo {
  private static final String RELATIONAL_DECLARATION = 'RELATIONAL'

  ArffTypeDecl type
  String typeDeclaration
  List<String> nominalValues
  String dateFormat
  SimpleDateFormat dateFormatter
  ArffDateMode dateMode
  List<String> relationalColumnNames
  List<Integer> relationalColumnIndexes
  List<ArffAttributeInfo> relationalInfos
  /** Nested weight-column index, or null. */
  Integer relationalWeightColumnIndex
  /** Nested column written as `{w}` after each nested row, or null. */
  String relationalWeightColumn

  ArffAttributeInfo(ArffTypeDecl type, String typeDeclaration, List<String> nominalValues = null, String dateFormat = null,
                    ArffDateMode dateMode = ArffDateMode.UTC) {
    this.type = type
    this.typeDeclaration = typeDeclaration
    this.nominalValues = nominalValues
    this.dateFormat = dateFormat
    this.dateMode = dateMode
    this.dateFormatter = dateFormat == null ? null : ArffDateFormats.create(dateFormat, dateMode)
  }

  /** Constructor for a RELATIONAL attribute with its sub-relation schema. */
  ArffAttributeInfo(List<String> relationalColumnNames, List<Integer> relationalColumnIndexes,
                    List<ArffAttributeInfo> relationalInfos, Integer relationalWeightColumnIndex,
                    String relationalWeightColumn) {
    this.type = ArffTypeDecl.RELATIONAL
    this.typeDeclaration = RELATIONAL_DECLARATION
    this.relationalColumnNames = relationalColumnNames
    this.relationalColumnIndexes = relationalColumnIndexes
    this.relationalInfos = relationalInfos
    this.relationalWeightColumnIndex = relationalWeightColumnIndex
    this.relationalWeightColumn = relationalWeightColumn
  }
}
