package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Lightweight row access helpers for LayerData metadata.
 */
@CompileStatic
class LayerDataRowAccess {

  private LayerDataRowAccess() {
    // Utility class
  }

  static void attach(LayerData datum, Matrix data, int rowIndex) {
    if (datum == null || data == null) {
      return
    }
    datum.meta.__data = data
    datum.meta.__rowIndex = rowIndex
  }

  static Object value(LayerData datum, String columnName) {
    if (datum == null || columnName == null) {
      return null
    }
    if (datum.meta?.__row instanceof Map) {
      Map<String, Object> row = datum.meta.__row as Map<String, Object>
      if (row.containsKey(columnName)) {
        return row[columnName]
      }
    }
    Matrix data = matrix(datum)
    Integer rowIndex = rowIndex(datum)
    if (data == null || rowIndex == null || rowIndex < 0 || rowIndex >= data.rowCount()) {
      return null
    }
    if (!data.columnNames().contains(columnName)) {
      return null
    }
    data[rowIndex, columnName]
  }

  static List<String> columns(LayerData datum) {
    Matrix data = matrix(datum)
    if (data != null) {
      return data.columnNames()
    }
    if (datum?.meta?.__row instanceof Map) {
      return (datum.meta.__row as Map<String, Object>).keySet().toList()
    }
    []
  }

  static Map<String, Object> rowMap(LayerData datum) {
    if (datum?.meta?.__row instanceof Map) {
      return datum.meta.__row as Map<String, Object>
    }
    Matrix data = matrix(datum)
    Integer rowIndex = rowIndex(datum)
    if (data == null || rowIndex == null || rowIndex < 0 || rowIndex >= data.rowCount()) {
      return [:]
    }
    Map<String, Object> row = [:]
    data.columnNames().each { String columnName ->
      row[columnName] = data[rowIndex, columnName]
    }
    datum.meta.__row = row
    row
  }

  private static Matrix matrix(LayerData datum) {
    datum?.meta?.__data instanceof Matrix ? datum.meta.__data as Matrix : null
  }

  private static Integer rowIndex(LayerData datum) {
    Object stored = datum?.meta?.__rowIndex
    if (stored instanceof Number) {
      return stored as int
    }
    datum?.rowIndex
  }
}
