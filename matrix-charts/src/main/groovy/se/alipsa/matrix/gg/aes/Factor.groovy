package se.alipsa.matrix.gg.aes

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

import java.util.concurrent.atomic.AtomicInteger

/**
 * Wrapper for categorical (factor) values in aesthetic mappings.
 * Converts values to Strings and adds a discrete column to the data.
 */
@CompileStatic
class Factor {

  private static final AtomicInteger NAME_COUNTER = new AtomicInteger(0)

  final Object value
  final String name

  Factor(Object value) {
    this(value, null)
  }

  Factor(Object value, String name) {
    this.value = value
    this.name = name ?: ".factor.${NAME_COUNTER.incrementAndGet()}"
  }

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

  @Override
  String toString() {
    return "factor(${value})"
  }
}
