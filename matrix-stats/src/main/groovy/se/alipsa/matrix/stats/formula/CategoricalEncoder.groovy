package se.alipsa.matrix.stats.formula

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix

/**
 * Encodes categorical columns using configurable contrasts.
 */
@CompileStatic
final class CategoricalEncoder {

  private final Matrix data

  CategoricalEncoder(Matrix data) {
    this.data = data
  }

  /**
   * Encodes a categorical column using the specified contrast type.
   *
   * @param columnName the column to encode
   * @param contrastType the contrast type to apply
   * @return a map from generated column names to their value lists
   */
  Map<String, List<BigDecimal>> encode(String columnName, ContrastType contrastType) {
    List<Object> values = (0..<data.rowCount()).collect { int i -> data[i, columnName] }
    List<Object> uniqueValues = (values.toUnique() as List<Object>).sort() as List<Object>

    if (uniqueValues.size() <= 1) {
      return [:]
    }

    Map<String, List<BigDecimal>> result = [:]

    switch (contrastType) {
      case ContrastType.TREATMENT -> {
        // First level is reference, omitted
        for (int k = 1; k < uniqueValues.size(); k++) {
          Object level = uniqueValues[k]
          String indicatorName = "${columnName}_${level}"
          List<BigDecimal> indicator = values.collect { Object val ->
            val == level ? 1.0 as BigDecimal : 0.0 as BigDecimal
          }
          result[indicatorName] = indicator
        }
      }
      case ContrastType.SUM -> {
        // Last level is omitted
        Object omittedLevel = uniqueValues[-1]
        for (int k = 0; k < uniqueValues.size() - 1; k++) {
          Object level = uniqueValues[k]
          String indicatorName = "${columnName}_${level}"
          List<BigDecimal> indicator = values.collect { Object val ->
            if (val == level) {
              return 1.0 as BigDecimal
            }
            if (val == omittedLevel) {
              return -1.0 as BigDecimal
            }
            0.0 as BigDecimal
          }
          result[indicatorName] = indicator
        }
      }
      case ContrastType.DEVIATION -> {
        // Last level is omitted, with fractional negative weight
        Object omittedLevel = uniqueValues[-1]
        int levelCount = uniqueValues.size()
        BigDecimal omittedValue = (-1.0 / (levelCount - 1)) as BigDecimal
        for (int k = 0; k < levelCount - 1; k++) {
          Object level = uniqueValues[k]
          String indicatorName = "${columnName}_${level}"
          List<BigDecimal> indicator = values.collect { Object val ->
            if (val == level) {
              return 1.0 as BigDecimal
            }
            if (val == omittedLevel) {
              return omittedValue
            }
            0.0 as BigDecimal
          }
          result[indicatorName] = indicator
        }
      }
    }

    result
  }

  /**
   * Returns the sorted unique levels of a categorical column.
   *
   * @param columnName the column name
   * @return sorted unique values, or empty list if the column is not found
   */
  List<String> levels(String columnName) {
    int colIdx = data.columnIndex(columnName)
    if (colIdx < 0) {
      return []
    }
    List<Object> values = (0..<data.rowCount()).collect { int i -> data[i, columnName] }
    (values.toUnique() as List<Object>).sort().collect { Object val -> val.toString() }
  }

  /**
   * Determines whether a column should be treated as categorical.
   *
   * <p>Non-{@link Number} columns are treated as categorical by default.
   * String, Boolean, and enum columns are always categorical.
   *
   * @param columnName the column name
   * @return true if the column should be treated as categorical
   */
  boolean isCategorical(String columnName) {
    int colIdx = data.columnIndex(columnName)
    if (colIdx < 0) {
      return false
    }
    Class colType = data.type(columnName)
    !Number.isAssignableFrom(colType)
  }
}
