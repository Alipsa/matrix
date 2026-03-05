package se.alipsa.matrix.gg.aes

import groovy.transform.CompileDynamic
import se.alipsa.matrix.core.Matrix

/**
 * Proxy exposing validated, unquoted column name references for a Matrix.
 *
 * <p>Property access returns the column name when it exists, otherwise throws
 * an {@link IllegalArgumentException} with available columns.
 */
@CompileDynamic
class ColumnRef {

  private final Set<String> columns
  private final List<String> sortedColumns

  /**
   * Create a column reference proxy for a Matrix.
   *
   * @param data the matrix whose columns should be exposed
   */
  ColumnRef(Matrix data) {
    if (data == null) {
      throw new IllegalArgumentException('cols() requires a non-null Matrix')
    }
    this.columns = data.columnNames().toSet()
    this.sortedColumns = data.columnNames().sort()
  }

  /**
   * Resolve a property access to a validated column name.
   *
   * @param name the requested column/property name
   * @return the same name if it exists in the matrix
   * @throws IllegalArgumentException if the column does not exist
   */
  String propertyMissing(String name) {
    if (!columns.contains(name)) {
      throw new IllegalArgumentException(
          "Column '${name}' not found in matrix. Available: ${sortedColumns}")
    }
    name
  }
}
