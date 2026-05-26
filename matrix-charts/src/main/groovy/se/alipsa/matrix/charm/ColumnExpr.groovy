package se.alipsa.matrix.charm

/**
 * Represents a typed column expression used in aesthetic mappings.
 */
interface ColumnExpr extends CharmExpression {

  String columnName()

  @Override
  default String describe() {
    "column(${columnName()})"
  }

}
