package se.alipsa.matrix.gg.aes

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Proxy exposing validated, unquoted column name references for a Matrix.
 *
 * <p>Property access returns the column name when it exists, otherwise throws
 * an {@link IllegalArgumentException} with available columns.
 */
@CompileStatic
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
    List<String> columnNames = data.columnNames().toList()
    this.columns = columnNames.toSet()
    this.sortedColumns = columnNames.sort(false)
  }

  /**
   * Resolve property reads to validated column names first.
   *
   * <p>This handles names that would otherwise be intercepted by Groovy/Object
   * properties (for example {@code class} or {@code metaClass}).
   *
  * @param name the requested column/property name
  * @return the same name if it exists in the matrix
  * @throws IllegalArgumentException if the column does not exist
  */
  @CompileDynamic
  Object getProperty(String name) {
    if (this.@columns.contains(name)) {
      return name
    }
    throw new IllegalArgumentException(
        "Column '${name}' not found in matrix. Available: ${this.@sortedColumns}")
  }
}
