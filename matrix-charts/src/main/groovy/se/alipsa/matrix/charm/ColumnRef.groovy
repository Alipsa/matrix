package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Column-reference implementation for aesthetic mappings.
 */
@CompileStatic
class ColumnRef implements ColumnExpr {

  private final String name

  /**
   * Creates a new column reference.
   *
   * @param name the matrix column name
   */
  ColumnRef(String name) {
    String value = name?.trim()
    if (!value) {
      throw new IllegalArgumentException('Column name cannot be blank')
    }
    this.name = value
  }

  /**
   * Returns the referenced column name.
   *
   * @return the column name
   */
  @Override
  String columnName() {
    name
  }

  /**
   * Returns the column reference as string.
   *
   * @return the column name
   */
  @Override
  String toString() {
    name
  }
}
