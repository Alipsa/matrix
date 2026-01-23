package se.alipsa.matrix.json

import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix

/**
 * Exports Matrix data to JSON format with optional column formatters.
 *
 * @deprecated Use {@link JsonWriter} instead. This class will be removed in v2.0.
 * <p>Migration guide:</p>
 * <ul>
 *   <li>Instance API: {@code new JsonExporter(matrix).toJson()} → {@code JsonWriter.writeString(matrix)}</li>
 *   <li>Static API: {@code JsonExporter.toJson(matrix)} → {@code JsonWriter.writeString(matrix)}</li>
 *   <li>File export: {@code JsonExporter.toJsonFile(matrix, file)} → {@code JsonWriter.write(matrix, file)}</li>
 * </ul>
 *
 * @see JsonWriter
 * @see JsonImporter
 */
@Deprecated
class JsonExporter {

  Matrix table

  /**
   * @deprecated Use {@link JsonWriter#writeString(Matrix)} static methods instead
   */
  @Deprecated
  JsonExporter(Grid grid, List<String> columnNames) {
    if (grid == null || columnNames == null) {
      throw new IllegalArgumentException("Grid and columnNames cannot be null")
    }
    this.table = Matrix.builder()
        .columnNames(columnNames)
        .data(grid)
        .build()
  }

  /**
   * @deprecated Use {@link JsonWriter#writeString(Matrix)} static methods instead
   */
  @Deprecated
  JsonExporter(Matrix table) {
    if (table == null) {
      throw new IllegalArgumentException("Matrix table cannot be null")
    }
    this.table = table
  }

  /**
   * @deprecated Use {@link JsonWriter#writeString(Matrix, boolean)} instead
   */
  @Deprecated
  String toJson(boolean indent = false) {
    JsonWriter.writeString(table, indent)
  }

  /**
   * @deprecated Use {@link JsonWriter#writeString(Matrix, Map, boolean, String)} instead
   */
  @Deprecated
  String toJson(Map<String, Closure> columnFormatters) {
    JsonWriter.writeString(table, columnFormatters, false)
  }

  /**
   * @deprecated Use {@link JsonWriter#writeString(Matrix, String, boolean)} instead
   */
  @Deprecated
  String toJson(String dateFormat) {
    JsonWriter.writeString(table, dateFormat, false)
  }

  /**
   * @deprecated Use {@link JsonWriter#writeString(Matrix, Map, boolean, String)} instead
   */
  @Deprecated
  String toJson(Map<String, Closure> columnFormatters, boolean indent, String dateFormat = 'yyyy-MM-dd') {
    JsonWriter.writeString(table, columnFormatters, indent, dateFormat)
  }

  /**
   * Export a Matrix to JSON string (static convenience method).
   *
   * @param table the Matrix to export
   * @param indent whether to pretty print the JSON
   * @return JSON string representation
   * @deprecated Use {@link JsonWriter#writeString(Matrix, boolean)} instead
   */
  @Deprecated
  static String toJson(Matrix table, boolean indent = false) {
    JsonWriter.writeString(table, indent)
  }

  /**
   * Export a Matrix to a JSON file.
   *
   * @param table the Matrix to export
   * @param outputFile file to write JSON to
   * @param indent whether to pretty print the JSON
   * @throws IOException if writing fails
   * @deprecated Use {@link JsonWriter#write(Matrix, File, boolean)} instead
   */
  @Deprecated
  static void toJsonFile(Matrix table, File outputFile, boolean indent = false) throws IOException {
    JsonWriter.write(table, outputFile, indent)
  }
}
