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
 * ARFF format specification:
 * - @RELATION defines the dataset name
 * - @ATTRIBUTE name type defines columns (NUMERIC, REAL, INTEGER, STRING, DATE, or {nominal values})
 * - @DATA marks the start of data rows
 * - Data rows are comma-separated values
 */
@CompileStatic
class MatrixArffWriter {

  private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

  /** Write to a File */
  static void write(Matrix matrix, File file) {
    validateMatrix(matrix)
    File output = ensureFileOutput(matrix, file)
    OutputStream outputStream = new FileOutputStream(output)
    OutputStreamWriter writer = null
    try {
      writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
      write(matrix, writer)
      writer.flush()
    } finally {
      if (writer != null) {
        writer.close()
      } else {
        outputStream.close()
      }
    }
  }

  /** Write to a Path */
  static void write(Matrix matrix, Path path) {
    write(matrix, path.toFile())
  }

  /** Write to an OutputStream */
  static void write(Matrix matrix, OutputStream output) {
    validateMatrix(matrix)
    OutputStreamWriter writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)
    write(matrix, writer)
    writer.flush()
  }

  /** Write to a Writer */
  static void write(Matrix matrix, Writer writer) {
    validateMatrix(matrix)
    PrintWriter pw = writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer)

    // Write relation name
    String relationName = matrix.matrixName ?: "matrix"
    pw.println("@RELATION ${escapeIdentifier(relationName)}")
    pw.println()

    // Collect column metadata
    List<String> columnNames = matrix.columnNames()
    List<ArffAttributeInfo> attributeInfos = []

    for (String colName : columnNames) {
      Class colType = matrix.type(colName)
      ArffAttributeInfo info = determineAttributeInfo(matrix, colName, colType)
      attributeInfos.add(info)

      // Write attribute declaration
      pw.println("@ATTRIBUTE ${escapeIdentifier(colName)} ${info.typeDeclaration}")
    }

    pw.println()
    pw.println("@DATA")

    // Write data rows
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

    pw.flush()
  }

  /**
   * Write with custom nominal value mappings for specific columns.
   * This allows explicit control over which columns should be treated as nominal
   * and what their allowed values are.
   */
  static void write(Matrix matrix, File file, Map<String, List<String>> nominalMappings) {
    validateMatrix(matrix)
    File output = ensureFileOutput(matrix, file)
    OutputStream outputStream = new FileOutputStream(output)
    OutputStreamWriter writer = null
    try {
      writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
      write(matrix, writer, nominalMappings)
      writer.flush()
    } finally {
      if (writer != null) {
        writer.close()
      } else {
        outputStream.close()
      }
    }
  }

  /** Write with custom nominal value mappings */
  static void write(Matrix matrix, Writer writer, Map<String, List<String>> nominalMappings) {
    validateMatrix(matrix)
    PrintWriter pw = writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer)

    // Write relation name
    String relationName = matrix.matrixName ?: "matrix"
    pw.println("@RELATION ${escapeIdentifier(relationName)}")
    pw.println()

    // Collect column metadata
    List<String> columnNames = matrix.columnNames()
    List<ArffAttributeInfo> attributeInfos = []

    for (String colName : columnNames) {
      ArffAttributeInfo info
      if (nominalMappings.containsKey(colName)) {
        // Use provided nominal values
        List<String> nominalValues = nominalMappings[colName]
        String typeDecl = "{${nominalValues.collect { escapeNominalValue(it) }.join(',')}}"
        info = new ArffAttributeInfo(ArffTypeDecl.NOMINAL, typeDecl, nominalValues)
      } else {
        Class colType = matrix.type(colName)
        info = determineAttributeInfo(matrix, colName, colType)
      }
      attributeInfos.add(info)

      // Write attribute declaration
      pw.println("@ATTRIBUTE ${escapeIdentifier(colName)} ${info.typeDeclaration}")
    }

    pw.println()
    pw.println("@DATA")

    // Write data rows
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

    pw.flush()
  }

  private static ArffAttributeInfo determineAttributeInfo(Matrix matrix, String colName, Class colType) {
    // Check for numeric types
    if (isNumericType(colType)) {
      return new ArffAttributeInfo(ArffTypeDecl.NUMERIC, "NUMERIC")
    }

    // Check for integer types
    if (isIntegerType(colType)) {
      return new ArffAttributeInfo(ArffTypeDecl.INTEGER, "INTEGER")
    }

    // Check for date types
    if (isDateType(colType)) {
      return new ArffAttributeInfo(ArffTypeDecl.DATE, "DATE '${DEFAULT_DATE_FORMAT}'")
    }

    // For String or Object types, check if it should be nominal (categorical)
    if (colType == String || colType == Object) {
      Set<String> uniqueValues = collectUniqueStringValues(matrix, colName)
      // Heuristic: if there are few unique values relative to row count, treat as nominal
      if (shouldBeNominal(uniqueValues, matrix.rowCount())) {
        List<String> sortedValues = uniqueValues.sort()
        String typeDecl = "{${sortedValues.collect { escapeNominalValue(it) }.join(',')}}"
        return new ArffAttributeInfo(ArffTypeDecl.NOMINAL, typeDecl, sortedValues)
      }
    }

    // Default to STRING
    return new ArffAttributeInfo(ArffTypeDecl.STRING, "STRING")
  }

  private static boolean isNumericType(Class type) {
    return type in [BigDecimal, Double, Float, Long, BigInteger, double.class, float.class, long.class, Number]
  }

  private static boolean isIntegerType(Class type) {
    return type in [Integer, Short, Byte, int.class, short.class, byte.class]
  }

  private static boolean isDateType(Class type) {
    return type in [Date, java.sql.Date, Timestamp,
                    LocalDate, LocalDateTime, Instant]
  }

  private static Set<String> collectUniqueStringValues(Matrix matrix, String colName) {
    Set<String> values = new LinkedHashSet<>()
    int rowCount = matrix.rowCount()
    for (int row = 0; row < rowCount; row++) {
      Object val = matrix[row, colName]
      if (val != null) {
        values.add(val.toString())
      }
    }
    return values
  }

  private static boolean shouldBeNominal(Set<String> uniqueValues, int rowCount) {
    // Treat as nominal if:
    // - There are unique values
    // - Number of unique values is much smaller than row count (< 10% or max 50 unique values)
    if (uniqueValues.isEmpty()) {
      return false
    }
    int uniqueCount = uniqueValues.size()
    return uniqueCount <= 50 && (rowCount < 10 || uniqueCount <= rowCount * 0.1)
  }

  private static String formatValue(Object value, ArffAttributeInfo info) {
    if (value == null) {
      return "?"
    }

    switch (info.type) {
      case ArffTypeDecl.NUMERIC:
      case ArffTypeDecl.REAL:
      case ArffTypeDecl.INTEGER:
        return value.toString()

      case ArffTypeDecl.DATE:
        return formatDate(value)

      case ArffTypeDecl.NOMINAL:
        return escapeNominalValue(value.toString())

      case ArffTypeDecl.STRING:
        return escapeStringValue(value.toString())

      default:
        return escapeStringValue(value.toString())
    }
  }

  private static String formatDate(Object value) {
    SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATE_FORMAT)
    if (value instanceof Date) {
      return "'" + sdf.format((Date) value) + "'"
    } else if (value instanceof LocalDate) {
      Date date = java.sql.Date.valueOf((LocalDate) value)
      return "'" + sdf.format(date) + "'"
    } else if (value instanceof LocalDateTime) {
      Date date = Timestamp.valueOf((LocalDateTime) value)
      return "'" + sdf.format(date) + "'"
    } else if (value instanceof Instant) {
      Date date = Date.from((Instant) value)
      return "'" + sdf.format(date) + "'"
    }
    return "'" + value.toString() + "'"
  }

  private static String escapeIdentifier(String name) {
    // Quote identifier if it contains spaces or special characters
    if (name.contains(' ') || name.contains(',') || name.contains('{') ||
        name.contains('}') || name.contains('%') || name.contains("'") ||
        name.contains('"')) {
      // Use single quotes and escape any internal single quotes
      return "'" + name.replace("\\", "\\\\").replace("'", "\\'") + "'"
    }
    return name
  }

  private static String escapeNominalValue(String value) {
    // For nominal values, escape if they contain special characters
    if (value.contains(',') || value.contains(' ') || value.contains("'") ||
        value.contains('"') || value.contains('{') || value.contains('}')) {
      return "'" + value.replace("'", "\\'") + "'"
    }
    return value
  }

  private static String escapeStringValue(String value) {
    // String values should be quoted
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"
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
    return output
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
    return baseName
  }
}

/** Enum representing ARFF type declarations */
enum ArffTypeDecl {
  NUMERIC,
  REAL,
  INTEGER,
  STRING,
  NOMINAL,
  DATE
}

/** Class holding attribute information for writing */
@CompileStatic
class ArffAttributeInfo {
  ArffTypeDecl type
  String typeDeclaration
  List<String> nominalValues

  ArffAttributeInfo(ArffTypeDecl type, String typeDeclaration, List<String> nominalValues = null) {
    this.type = type
    this.typeDeclaration = typeDeclaration
    this.nominalValues = nominalValues
  }
}
