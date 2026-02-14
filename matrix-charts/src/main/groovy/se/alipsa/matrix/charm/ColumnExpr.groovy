package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Represents a typed column expression used in aesthetic mappings.
 */
@CompileStatic
interface ColumnExpr {

  String columnName()
}
