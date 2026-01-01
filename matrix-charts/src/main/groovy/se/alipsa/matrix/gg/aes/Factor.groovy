package se.alipsa.matrix.gg.aes

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Wrapper for categorical (factor) values in aesthetic mappings.
 * Converts values to Strings and adds a discrete column to the data.
 */
@CompileStatic
class Factor {
  private static final String DEFAULT_NAME = 'factor'

  final Object value
  final String name

  /**
   * Create a factor wrapper for a constant value or column reference.
   *
   * @param value constant value, list, or column name reference
   */
  Factor(Object value) {
    this(value, null)
  }

  /**
   * Create a factor wrapper with an explicit column name.
   *
   * @param value constant value, list, or column name reference
   * @param name explicit column name to use when adding to a matrix
   */
  Factor(Object value, String name) {
    this.value = value
    this.name = name ?: defaultName(value)
  }

  /**
   * Add a discrete factor column to the matrix, returning the final column name.
   * If the desired name already exists, a numeric suffix is appended to avoid collisions.
   *
   * @param data matrix to append the factor column to
   * @return the column name used in the matrix
   */
  String addToMatrix(Matrix data) {
    List<?> values = buildValues(data)
    String colName = name
    if (data.columnNames().contains(colName)) {
      int suffix = 1
      while (data.columnNames().contains("${colName}_${suffix}")) {
        suffix++
      }
      colName = "${colName}_${suffix}"
    }
    data.addColumn(colName, values.collect { it?.toString() })
    return colName
  }

  /**
   * Build the values list for the factor column.
   * Accepts a list (must match row count), a column name, or a constant value
   * that is repeated for each row.
   *
   * @param data matrix providing row count and columns
   * @return list of values aligned with the matrix row count
   */
  private List<?> buildValues(Matrix data) {
    if (value instanceof List) {
      List<?> list = value as List<?>
      if (data.rowCount() > 0 && list.size() != data.rowCount()) {
        throw new IllegalArgumentException("Factor list size (${list.size()}) does not match matrix row count (${data.rowCount()})")
      }
      return list
    }
    if (value instanceof CharSequence && data.columnNames().contains(value.toString())) {
      return data[value.toString()] as List<?>
    }
    int rows = data.rowCount()
    List<Object> result = new ArrayList<>(rows)
    for (int i = 0; i < rows; i++) {
      result.add(value)
    }
    return result
  }

  /**
   * Return the ggplot-style representation of this factor.
   *
   * @return a string like "factor(value)"
   */
  @Override
  String toString() {
    return "factor(${value})"
  }

  private static String defaultName(Object value) {
    if (value == null) {
      return "${DEFAULT_NAME}_null"
    }
    if (value instanceof CharSequence) {
      String candidate = value.toString().trim()
      if (candidate.isEmpty()) {
        return "${DEFAULT_NAME}_blank"
      }
      return "${DEFAULT_NAME}_${sanitizeName(candidate)}"
    }
    return "${DEFAULT_NAME}_const_${sanitizeName(value.toString())}"
  }

  private static String sanitizeName(String candidate) {
    return candidate.replaceAll(/[^A-Za-z0-9_]+/, '_')
  }
}
