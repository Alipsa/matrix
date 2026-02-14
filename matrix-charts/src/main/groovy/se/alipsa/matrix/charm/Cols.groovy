package se.alipsa.matrix.charm

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Column-reference namespace used by Charm DSL (`col['name']`, `col.name`).
 */
@CompileStatic
class Cols {

  /**
   * Returns a column reference using bracket syntax.
   * This is the static-safe form for users running under @CompileStatic.
   *
   * @param columnName the matrix column name
   * @return a typed column expression
   */
  ColumnExpr getAt(String columnName) {
    new ColumnRef(columnName)
  }

  /**
   * Dynamic property sugar for DSL usage (`col.name`).
   *
   * @param propertyName the requested property/column
   * @return a typed column expression
   */
  @CompileDynamic
  ColumnExpr getProperty(String propertyName) {
    propertyMissing(propertyName)
  }

  /**
   * Dynamic property resolution hook used by DSL closures.
   *
   * @param propertyName requested property/column
   * @return a typed column expression
   */
  @CompileDynamic
  ColumnExpr propertyMissing(String propertyName) {
    new ColumnRef(propertyName)
  }
}
