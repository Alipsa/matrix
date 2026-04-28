package se.alipsa.matrix.json

import groovy.transform.CompileStatic

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row

import java.nio.file.Path
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

/**
 * Writes Matrix data to JSON format with optional column formatters.
 *
 * <p>Supports custom formatting for individual columns via closures and
 * automatic temporal data formatting with configurable date patterns.</p>
 *
 * <h3>Fluent API (recommended)</h3>
 * <pre>
 * JsonWriter.write(matrix).to(file)
 * JsonWriter.write(matrix).indent().to(file)
 * JsonWriter.write(matrix).dateFormat('MM/dd/yyyy').to(file)
 * JsonWriter.write(matrix).formatter('salary') { it * 10 }.to(file)
 * String json = JsonWriter.write(matrix).asString()
 * </pre>
 *
 * @see JsonReader
 */
@CompileStatic
class JsonWriter {

  private static final JsonFactory FACTORY = new JsonFactory()

  private JsonWriter() {
  }

  /**
   * Entry point for the fluent JSON writing API.
   *
   * <pre>
   * JsonWriter.write(matrix).to(file)
   * JsonWriter.write(matrix).indent().to(file)
   * JsonWriter.write(matrix).dateFormat('MM/dd/yyyy').to(file)
   * String json = JsonWriter.write(matrix).asString()
   * </pre>
   *
   * @param matrix the matrix to write
   * @return a new {@link WriteBuilder}
   */
  static WriteBuilder write(Matrix matrix) {
    new WriteBuilder(matrix)
  }

  /**
   * Write a Matrix to a JSON string.
   *
   * @param matrix the Matrix to write
   * @param indent whether to pretty print the JSON
   * @return JSON string representation
   * @deprecated Use {@code JsonWriter.write(matrix).asString()} or {@code JsonWriter.write(matrix).indent().asString()} instead
   */
  @Deprecated
  static String writeString(Matrix matrix, boolean indent = false) {
    write(matrix).indent(indent).asString()
  }

  /**
   * Write a Matrix to a JSON string with custom column formatters.
   *
   * @param matrix the Matrix to write
   * @param columnFormatters map of column names to formatting closures
   * @param indent whether to pretty print the JSON
   * @param dateFormat date format pattern for temporal columns (default: yyyy-MM-dd)
   * @return JSON string representation
   * @deprecated Use the fluent API: {@code JsonWriter.write(matrix).columnFormatters(formatters).asString()} instead
   */
  @Deprecated
  static String writeString(Matrix matrix, Map<String, Closure> columnFormatters, boolean indent = false, String dateFormat = 'yyyy-MM-dd') {
    WriteBuilder builder = write(matrix).indent(indent).dateFormat(dateFormat)
    if (columnFormatters != null) {
      builder.columnFormatters(columnFormatters)
    }
    builder.asString()
  }

  /**
   * Write a Matrix to a JSON string with custom date formatting.
   *
   * @param matrix the Matrix to write
   * @param dateFormat date format pattern for all temporal columns
   * @param indent whether to pretty print the JSON
   * @return JSON string representation
   * @deprecated Use {@code JsonWriter.write(matrix).dateFormat(pattern).asString()} instead
   */
  @Deprecated
  static String writeString(Matrix matrix, String dateFormat, boolean indent = false) {
    write(matrix).indent(indent).dateFormat(dateFormat).asString()
  }

  /**
   * Write a Matrix to a JSON file.
   *
   * @param matrix the Matrix to write
   * @param outputFile file to write JSON to
   * @param indent whether to pretty print the JSON
   * @throws IOException if writing fails
   * @deprecated Use {@code JsonWriter.write(matrix).to(file)} instead
   */
  @Deprecated
  static void write(Matrix matrix, File outputFile, boolean indent = false) throws IOException {
    write(matrix).indent(indent).to(outputFile)
  }

  /**
   * Write a Matrix to a JSON file specified by Path.
   *
   * @param matrix the Matrix to write
   * @param outputPath path to write JSON to
   * @param indent whether to pretty print the JSON
   * @throws IOException if writing fails
   * @deprecated Use {@code JsonWriter.write(matrix).to(path)} instead
   */
  @Deprecated
  static void write(Matrix matrix, Path outputPath, boolean indent = false) throws IOException {
    write(matrix).indent(indent).to(outputPath)
  }

  /**
   * Write a Matrix to a JSON file specified by String path.
   *
   * @param matrix the Matrix to write
   * @param outputPath file path to write JSON to
   * @param indent whether to pretty print the JSON
   * @throws IOException if writing fails
   * @deprecated Use {@code JsonWriter.write(matrix).to(filePath)} instead
   */
  @Deprecated
  static void write(Matrix matrix, String outputPath, boolean indent = false) throws IOException {
    write(matrix).indent(indent).to(outputPath)
  }

  /**
   * Write a Matrix to a Writer as JSON.
   *
   * @param matrix the Matrix to write
   * @param writer the Writer to write JSON to
   * @param indent whether to pretty print the JSON
   * @throws IOException if writing fails
   * @deprecated Use {@code JsonWriter.write(matrix).to(writer)} instead
   */
  @Deprecated
  static void write(Matrix matrix, Writer writer, boolean indent = false) throws IOException {
    write(matrix).indent(indent).to(writer)
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
   * @deprecated Use the fluent API: {@code JsonWriter.write(matrix).columnFormatters(formatters).to(writer)} instead
   */
  @Deprecated
  static void write(Matrix matrix, Writer writer, Map<String, Closure> columnFormatters, boolean indent = false, String dateFormat = 'yyyy-MM-dd') throws IOException {
    WriteBuilder builder = write(matrix).indent(indent).dateFormat(dateFormat)
    if (columnFormatters != null) {
      builder.columnFormatters(columnFormatters)
    }
    builder.to(writer)
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
   * @deprecated Use the fluent API: {@code JsonWriter.write(matrix).columnFormatters(formatters).to(file)} instead
   */
  @Deprecated
  static void write(Matrix matrix, File outputFile, Map<String, Closure> columnFormatters, boolean indent = false, String dateFormat = 'yyyy-MM-dd') throws IOException {
    WriteBuilder builder = write(matrix).indent(indent).dateFormat(dateFormat)
    if (columnFormatters != null) {
      builder.columnFormatters(columnFormatters)
    }
    builder.to(outputFile)
  }

  /**
   * Write a Matrix to a JSON file specified by Path with custom formatting.
   *
   * @param matrix the Matrix to write
   * @param outputPath path to write JSON to
   * @param columnFormatters map of column names to formatting closures
   * @param indent whether to pretty print the JSON
   * @param dateFormat date format pattern for temporal columns (default: yyyy-MM-dd)
   * @throws IOException if writing fails
   * @deprecated Use the fluent API: {@code JsonWriter.write(matrix).columnFormatters(formatters).to(path)} instead
   */
  @Deprecated
  static void write(Matrix matrix, Path outputPath, Map<String, Closure> columnFormatters, boolean indent = false, String dateFormat = 'yyyy-MM-dd') throws IOException {
    WriteBuilder builder = write(matrix).indent(indent).dateFormat(dateFormat)
    if (columnFormatters != null) {
      builder.columnFormatters(columnFormatters)
    }
    builder.to(outputPath)
  }

  /**
   * Write a Matrix to a JSON file specified by String path with custom formatting.
   *
   * @param matrix the Matrix to write
   * @param outputPath file path to write JSON to
   * @param columnFormatters map of column names to formatting closures
   * @param indent whether to pretty print the JSON
   * @param dateFormat date format pattern for temporal columns (default: yyyy-MM-dd)
   * @throws IOException if writing fails
   * @deprecated Use the fluent API: {@code JsonWriter.write(matrix).columnFormatters(formatters).to(filePath)} instead
   */
  @Deprecated
  static void write(Matrix matrix, String outputPath, Map<String, Closure> columnFormatters, boolean indent = false, String dateFormat = 'yyyy-MM-dd') throws IOException {
    WriteBuilder builder = write(matrix).indent(indent).dateFormat(dateFormat)
    if (columnFormatters != null) {
      builder.columnFormatters(columnFormatters)
    }
    builder.to(outputPath)
  }

  /**
   * Write a single value to the Jackson JsonGenerator with appropriate type handling.
   */
  private static void writeValue(JsonGenerator gen, Object value, DateTimeFormatter dtf) {
    if (value == null) {
      gen.writeNull()
    } else if (value instanceof TemporalAccessor) {
      gen.writeString(dtf.format((TemporalAccessor) value))
    } else if (value instanceof Boolean) {
      gen.writeBoolean((boolean) value)
    } else if (value instanceof Integer) {
      gen.writeNumber((int) value)
    } else if (value instanceof Long) {
      gen.writeNumber((long) value)
    } else if (value instanceof BigDecimal) {
      gen.writeNumber((BigDecimal) value)
    } else if (value instanceof BigInteger) {
      gen.writeNumber((BigInteger) value)
    } else if (value instanceof Double) {
      gen.writeNumber((double) value)
    } else if (value instanceof Float) {
      gen.writeNumber((float) value)
    } else if (value instanceof Number) {
      gen.writeNumber(value.toString())
    } else {
      gen.writeString(value.toString())
    }
  }

  /**
   * Fluent builder for writing Matrix data to JSON.
   *
   * <p>Obtained via {@link JsonWriter#write(Matrix)}. Configure options
   * with chained method calls, then invoke a terminal method to perform the write.</p>
   *
   * <h3>Examples</h3>
   * <pre>
   * // Simple write
   * JsonWriter.write(matrix).to(file)
   *
   * // Pretty-printed with date format
   * JsonWriter.write(matrix).indent().dateFormat('MM/dd/yyyy').to(file)
   *
   * // With column formatters
   * JsonWriter.write(matrix).formatter('salary') { it * 10 }.to(file)
   *
   * // As string
   * String json = JsonWriter.write(matrix).asString()
   * </pre>
   */
  static class WriteBuilder {

    private final Matrix matrix
    private boolean indentValue = false
    private String dateFormatValue = 'yyyy-MM-dd'
    private Map<String, Closure> columnFormattersValue = [:]

    private WriteBuilder(Matrix matrix) {
      if (matrix == null) {
        throw new IllegalArgumentException("Matrix cannot be null")
      }
      this.matrix = matrix
    }

    /**
     * Enable pretty-printing (indent = true).
     *
     * @return this builder for chaining
     */
    WriteBuilder indent() {
      this.indentValue = true
      this
    }

    /**
     * Set whether to pretty-print the output.
     *
     * @param value true to indent, false for compact output
     * @return this builder for chaining
     */
    WriteBuilder indent(boolean value) {
      this.indentValue = value
      this
    }

    /**
     * Set the date format pattern for temporal columns.
     *
     * @param pattern date format pattern (e.g. 'MM/dd/yyyy')
     * @return this builder for chaining
     */
    WriteBuilder dateFormat(String pattern) {
      this.dateFormatValue = pattern
      this
    }

    /**
     * Add a formatter for a single column. Can be called multiple times for different columns.
     *
     * @param columnName the column to format
     * @param formatter closure applied to each value in the column
     * @return this builder for chaining
     */
    WriteBuilder formatter(String columnName, Closure formatter) {
      this.columnFormattersValue[columnName] = formatter
      this
    }

    /**
     * Set all column formatters at once.
     *
     * @param formatters map of column names to formatting closures
     * @return this builder for chaining
     */
    WriteBuilder columnFormatters(Map<String, Closure> formatters) {
      this.columnFormattersValue = formatters ?: [:]
      this
    }

    /**
     * Write JSON to a File.
     *
     * @param file the output file
     * @throws IOException if writing fails
     */
    void to(File file) throws IOException {
      if (file == null) {
        throw new IllegalArgumentException("Output file cannot be null")
      }
      if (file.isDirectory()) {
        throw new IOException("Output file '${file.absolutePath}' is a directory, cannot write JSON data")
      }
      File parent = file.getParentFile()
      if (parent != null && !parent.exists()) {
        if (!parent.mkdirs()) {
          throw new IOException("Failed to create parent directory '${parent.absolutePath}' for output file '${file.absolutePath}'")
        }
      }
      if (file.exists() && !file.canWrite()) {
        throw new IOException("Output file '${file.absolutePath}' is not writable")
      }
      file.withWriter('UTF-8') { Writer w -> writeTo(w) }
    }

    /**
     * Write JSON to a Path.
     *
     * @param path the output path
     * @throws IOException if writing fails
     */
    void to(Path path) throws IOException {
      to(path.toFile())
    }

    /**
     * Write JSON to a file specified by path string.
     *
     * @param filePath the output file path
     * @throws IOException if writing fails
     */
    void to(String filePath) throws IOException {
      to(new File(filePath))
    }

    /**
     * Write JSON to a Writer.
     *
     * @param writer the output writer
     * @throws IOException if writing fails
     */
    void to(Writer writer) throws IOException {
      if (writer == null) {
        throw new IllegalArgumentException("Writer cannot be null")
      }
      writeTo(writer)
      writer.flush()
    }

    /**
     * Write JSON to a String.
     *
     * @return JSON string representation
     */
    String asString() {
      StringWriter sw = new StringWriter()
      writeTo(sw)
      sw.toString()
    }

    private void writeTo(Writer writer) {
      if (!columnFormattersValue.isEmpty()) {
        List<String> colNames = matrix.columnNames()
        Set<String> invalidFormatters = columnFormattersValue.keySet().findAll { String it -> !colNames.contains(it) } as Set<String>
        if (!invalidFormatters.isEmpty()) {
          throw new IllegalArgumentException(
              "Column formatter(s) defined for non-existent column(s): ${invalidFormatters}. " +
              "Available columns: ${colNames}"
          )
        }
      }

      Matrix t = columnFormattersValue.isEmpty() ? matrix : (matrix.clone() as Matrix)
      if (!columnFormattersValue.isEmpty()) {
        columnFormattersValue.each { String k, Closure v ->
          t.apply(k, v)
        }
      }
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormatValue)

      JsonGenerator gen = FACTORY.createGenerator(writer)
      if (indentValue) {
        gen.useDefaultPrettyPrinter()
      }
      gen.withCloseable {
        gen.writeStartArray()
        List<String> colNames = t.columnNames()
        int colCount = colNames.size()
        for (Row row : t) {
          gen.writeStartObject()
          for (int i = 0; i < colCount; i++) {
            gen.writeFieldName(colNames[i])
            writeValue(gen, row[i], dtf)
          }
          gen.writeEndObject()
        }
        gen.writeEndArray()
      }
    }
  }
}
