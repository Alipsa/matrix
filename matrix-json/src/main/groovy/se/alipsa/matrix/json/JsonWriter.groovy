package se.alipsa.matrix.json

import tools.jackson.core.JsonGenerator
import tools.jackson.core.StreamWriteFeature
import tools.jackson.core.json.JsonFactory
import tools.jackson.databind.ObjectWriter
import tools.jackson.databind.json.JsonMapper

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row

import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

/**
 * Writes Matrix data to JSON format with optional column formatters.
 *
 * <p>Supports custom formatting for individual columns via closures and
 * automatic temporal data formatting with configurable date patterns. {@link LocalDate} and
 * {@link LocalDateTime} use their configured patterns; {@code LocalTime}, {@code Instant},
 * {@code ZonedDateTime}, {@code OffsetDateTime}, {@code OffsetTime}, {@code YearMonth},
 * {@code Year}, and {@code MonthDay} use ISO-8601 {@code toString()} values. Any other
 * {@link TemporalAccessor}, including {@code Month}, {@code DayOfWeek}, and chronology dates,
 * uses its {@code toString()} value. {@link Date} and {@link java.sql.Timestamp} are ISO-8601 instants;
 * {@link java.sql.Date} and {@link java.sql.Time} use {@code toString()}. Non-finite floating
 * point values are written as JSON {@code null}.</p>
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
class JsonWriter {

  private static final JsonFactory FACTORY = JsonFactory.builder()
      .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
      .build()
  private static final JsonMapper MAPPER = JsonMapper.builder(FACTORY).build()

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
   * @param dateFormat date format pattern for LocalDate columns (default: yyyy-MM-dd)
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
   * @param dateFormat date format pattern for LocalDate columns
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
   * @throws IOException if writing fails. The writer is flushed before this method returns but
   * remains open; the caller is responsible for closing it.
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
   * @throws IOException if writing fails. The writer is flushed before this method returns but
   * remains open; the caller is responsible for closing it.
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
   * @param dateFormat date format pattern for LocalDate columns (default: yyyy-MM-dd)
   * @throws IOException if writing fails. The writer is flushed before this method returns but
   * remains open; the caller is responsible for closing it.
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
   * @param dateFormat date format pattern for LocalDate columns (default: yyyy-MM-dd)
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
   * @param dateFormat date format pattern for LocalDate columns (default: yyyy-MM-dd)
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
   * @param dateFormat date format pattern for LocalDate columns (default: yyyy-MM-dd)
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
   *
   * <p>LocalDate and LocalDateTime use their configured patterns. LocalTime, Instant,
   * ZonedDateTime, OffsetDateTime, OffsetTime, YearMonth, Year, and MonthDay use ISO-8601
   * {@code toString()} values; every other TemporalAccessor uses {@code toString()}.</p>
   */
  private static void writeValue(JsonGenerator gen, Object value, DateTimeFormatter dateFormatter,
      DateTimeFormatter dateTimeFormatter) {
    if (value == null) {
      gen.writeNull()
    } else if (value instanceof LocalDate) {
      gen.writeString(dateFormatter.format((LocalDate) value))
    } else if (value instanceof LocalDateTime) {
      gen.writeString(dateTimeFormatter == null ? value.toString() : dateTimeFormatter.format((LocalDateTime) value))
    } else if (value instanceof TemporalAccessor) {
      gen.writeString(value.toString())
    } else if (value instanceof java.sql.Date || value instanceof java.sql.Time) {
      gen.writeString(value.toString())
    } else if (value instanceof Date) {
      gen.writeString(((Date) value).toInstant().toString())
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
      if (Double.isFinite((double) value)) {
        gen.writeNumber((double) value)
      } else {
        gen.writeNull()
      }
    } else if (value instanceof Float) {
      if (Float.isFinite((float) value)) {
        gen.writeNumber((float) value)
      } else {
        gen.writeNull()
      }
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
    private String dateTimeFormatValue = null
    private Map<String, Closure> columnFormattersValue = [:]

    private WriteBuilder(Matrix matrix) {
      if (matrix == null) {
        throw new IllegalArgumentException('Matrix cannot be null')
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
     * Set the date format pattern for LocalDate values.
     *
     * @param pattern date format pattern (e.g. 'MM/dd/yyyy')
     * @return this builder for chaining
     */
    WriteBuilder dateFormat(String pattern) {
      JsonWriteOptions.validatePattern('dateFormat', pattern)
      this.dateFormatValue = pattern
      this
    }

    /**
     * Set the date-time format pattern for LocalDateTime values.
     *
     * <p>A null pattern writes LocalDateTime values using their ISO-8601 {@code toString()} value.</p>
     *
     * @param pattern date-time format pattern, or null for ISO-8601 output
     * @return this builder for chaining
     */
    WriteBuilder dateTimeFormat(String pattern) {
      JsonWriteOptions.validatePattern('dateTimeFormat', pattern)
      this.dateTimeFormatValue = pattern
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
      JsonWriteOptions.validateColumnFormatters([(columnName): formatter])
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
      JsonWriteOptions.validateColumnFormatters(formatters)
      this.columnFormattersValue = formatters ?: [:]
      this
    }

    /**
     * Write JSON to a File.
     *
     * @param file the output file
     * @throws IOException if writing fails. The writer is flushed before this method returns but
     * remains open; the caller is responsible for closing it.
     */
    void to(File file) throws IOException {
      if (file == null) {
        throw new IllegalArgumentException('Output file cannot be null')
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
        throw new IllegalArgumentException('Writer cannot be null')
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
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(dateFormatValue)
      DateTimeFormatter dateTimeFormatter = dateTimeFormatValue == null ? null : DateTimeFormatter.ofPattern(dateTimeFormatValue)

      ObjectWriter objectWriter = indentValue
          ? MAPPER.writer().withDefaultPrettyPrinter()
          : MAPPER.writer()
      JsonGenerator gen = objectWriter.createGenerator(writer)
      gen.withCloseable {
        gen.writeStartArray()
        List<String> colNames = t.columnNames()
        int colCount = colNames.size()
        if (colCount == 0) {
          // Zero-column matrices (e.g. produced by JsonReader from JSON like [{}, {}]) still
          // carry a row count; Matrix.rows() is empty in that case, so iterate by rowCount()
          // instead of relying on the Row iterator, writing one empty object per row.
          int rowCount = t.rowCount()
          for (int r = 0; r < rowCount; r++) {
            gen.writeStartObject()
            gen.writeEndObject()
          }
        } else {
          for (Row row : t) {
            gen.writeStartObject()
            for (int i = 0; i < colCount; i++) {
              gen.writeName(colNames[i])
              writeValue(gen, row[i], dateFormatter, dateTimeFormatter)
            }
            gen.writeEndObject()
          }
        }
        gen.writeEndArray()
      }
    }
  }
}
