package se.alipsa.matrix.json

import se.alipsa.matrix.core.Matrix
import groovy.json.*

import java.nio.file.Path
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

/**
 * Writes Matrix data to JSON format with optional column formatters.
 *
 * <p>Supports custom formatting for individual columns via closures and
 * automatic temporal data formatting with configurable date patterns.</p>
 *
 * <h3>Basic Usage</h3>
 * <pre>
 * Matrix m = Matrix.builder()
 *   .data(id: [1, 2], name: ['Alice', 'Bob'])
 *   .build()
 *
 * String json = JsonWriter.writeString(m)
 * // Output: [{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]
 *
 * String pretty = JsonWriter.writeString(m, true)
 * // Output: Pretty-printed with indentation
 *
 * // Write to file
 * JsonWriter.write(m, new File("output.json"))
 * </pre>
 *
 * <h3>Custom Column Formatting</h3>
 * <pre>
 * String json = JsonWriter.writeString(m, [
 *   'salary': {it * 10 + ' kr'},
 *   'date': {DateTimeFormatter.ofPattern('MM/dd/yy').format(it)}
 * ])
 * </pre>
 *
 * <h3>Date Formatting</h3>
 * <pre>
 * // All temporal columns formatted with custom pattern
 * String json = JsonWriter.writeString(m, 'MM/dd/yyyy')
 * </pre>
 *
 * @see JsonReader
 */
class JsonWriter {

  private JsonWriter() {
    // Only static methods
  }

  /**
   * Write a Matrix to a JSON string.
   *
   * @param matrix the Matrix to write
   * @param indent whether to pretty print the JSON
   * @return JSON string representation
   */
  static String writeString(Matrix matrix, boolean indent = false) {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }
    writeString(matrix, [:], indent)
  }

  /**
   * Write a Matrix to a JSON string with custom column formatters.
   *
   * @param matrix the Matrix to write
   * @param columnFormatters map of column names to formatting closures
   * @param indent whether to pretty print the JSON
   * @param dateFormat date format pattern for temporal columns (default: yyyy-MM-dd)
   * @return JSON string representation
   */
  static String writeString(Matrix matrix, Map<String, Closure> columnFormatters, boolean indent = false, String dateFormat = 'yyyy-MM-dd') {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }

    // Validate that all formatter keys refer to existing columns before cloning/applying
    if (columnFormatters != null && !columnFormatters.isEmpty()) {
      def columnNames = matrix.columnNames()
      def invalidFormatters = columnFormatters.keySet().findAll { !columnNames.contains(it) }
      if (!invalidFormatters.isEmpty()) {
        throw new IllegalArgumentException(
            "Column formatter(s) defined for non-existent column(s): ${invalidFormatters}. " +
            "Available columns: ${columnNames}"
        )
      }
    }

    // Only clone if we need to apply formatters (avoids unnecessary memory allocation)
    def t = columnFormatters.isEmpty() ? matrix : matrix.clone()
    if (!columnFormatters.isEmpty()) {
      columnFormatters.each { k, v ->
        t.apply(k, v)
      }
    }
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormat)

    def jsonGenerator = new JsonGenerator.Options()
        .dateFormat(dateFormat)
        .addConverter(new JsonGenerator.Converter() {
          @Override
          boolean handles(Class<?> type) {
            return TemporalAccessor.isAssignableFrom(type)
          }
          @Override
          Object convert(Object date, String key) {
            dtf.format(date as TemporalAccessor)
          }
        })
        .build()

    // Build JSON by collecting row maps and letting the generator handle serialization
    def rowMaps = []
    for (def row : t) {
      rowMaps.add(row.toMap())
    }
    String json = jsonGenerator.toJson(rowMaps)
    return indent ? JsonOutput.prettyPrint(json) : json
  }

  /**
   * Write a Matrix to a JSON string with custom date formatting.
   *
   * @param matrix the Matrix to write
   * @param dateFormat date format pattern for all temporal columns
   * @param indent whether to pretty print the JSON
   * @return JSON string representation
   */
  static String writeString(Matrix matrix, String dateFormat, boolean indent = false) {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }
    writeString(matrix, [:], indent, dateFormat)
  }

  /**
   * Write a Matrix to a JSON file.
   *
   * @param matrix the Matrix to write
   * @param outputFile file to write JSON to
   * @param indent whether to pretty print the JSON
   * @throws IOException if writing fails
   */
  static void write(Matrix matrix, File outputFile, boolean indent = false) throws IOException {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }
    if (outputFile == null) {
      throw new IllegalArgumentException("Output file cannot be null")
    }

    if (outputFile.isDirectory()) {
      throw new IOException("Output file '${outputFile.absolutePath}' is a directory, cannot write JSON data")
    }

    File parent = outputFile.getParentFile()
    if (parent != null && !parent.exists()) {
      if (!parent.mkdirs()) {
        throw new IOException("Failed to create parent directory '${parent.absolutePath}' for output file '${outputFile.absolutePath}'")
      }
    }

    if (outputFile.exists() && !outputFile.canWrite()) {
      throw new IOException("Output file '${outputFile.absolutePath}' is not writable")
    }

    // Let Groovy's file.text setter handle file creation - it will create the file if needed
    // and overwrite if it exists, avoiding TOCTOU race conditions
    outputFile.text = writeString(matrix, indent)
  }

  /**
   * Write a Matrix to a JSON file specified by Path.
   *
   * @param matrix the Matrix to write
   * @param outputPath path to write JSON to
   * @param indent whether to pretty print the JSON
   * @throws IOException if writing fails
   */
  static void write(Matrix matrix, Path outputPath, boolean indent = false) throws IOException {
    write(matrix, outputPath.toFile(), indent)
  }

  /**
   * Write a Matrix to a JSON file specified by String path.
   *
   * @param matrix the Matrix to write
   * @param outputPath file path to write JSON to
   * @param indent whether to pretty print the JSON
   * @throws IOException if writing fails
   */
  static void write(Matrix matrix, String outputPath, boolean indent = false) throws IOException {
    write(matrix, new File(outputPath), indent)
  }

  /**
   * Write a Matrix to a Writer as JSON.
   *
   * @param matrix the Matrix to write
   * @param writer the Writer to write JSON to
   * @param indent whether to pretty print the JSON
   * @throws IOException if writing fails
   */
  static void write(Matrix matrix, Writer writer, boolean indent = false) throws IOException {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }
    if (writer == null) {
      throw new IllegalArgumentException("Writer cannot be null")
    }
    writer.write(writeString(matrix, indent))
    writer.flush()
  }

  /**
   * Write a Matrix to a Writer as JSON with custom formatting.
   *
   * @param matrix the Matrix to write
   * @param writer the Writer to write JSON to
   * @param columnFormatters map of column names to formatting closures
   * @param indent whether to pretty print the JSON
   * @param dateFormat date format pattern for temporal columns (default: yyyy-MM-dd)
   * @throws IOException if writing fails
   */
  static void write(Matrix matrix, Writer writer, Map<String, Closure> columnFormatters, boolean indent = false, String dateFormat = 'yyyy-MM-dd') throws IOException {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }
    if (writer == null) {
      throw new IllegalArgumentException("Writer cannot be null")
    }
    writer.write(writeString(matrix, columnFormatters, indent, dateFormat))
    writer.flush()
  }

  /**
   * Write a Matrix to a JSON file with custom formatting.
   *
   * @param matrix the Matrix to write
   * @param outputFile file to write JSON to
   * @param columnFormatters map of column names to formatting closures
   * @param indent whether to pretty print the JSON
   * @param dateFormat date format pattern for temporal columns (default: yyyy-MM-dd)
   * @throws IOException if writing fails
   */
  static void write(Matrix matrix, File outputFile, Map<String, Closure> columnFormatters, boolean indent = false, String dateFormat = 'yyyy-MM-dd') throws IOException {
    if (matrix == null) {
      throw new IllegalArgumentException("Matrix cannot be null")
    }
    if (outputFile == null) {
      throw new IllegalArgumentException("Output file cannot be null")
    }
    outputFile.text = writeString(matrix, columnFormatters, indent, dateFormat)
  }
}
