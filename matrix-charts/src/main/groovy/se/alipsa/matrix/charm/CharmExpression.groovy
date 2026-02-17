package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Marker interface for expression types that can appear in Charm
 * aesthetic mappings.
 *
 * {@link ColumnExpr} extends this interface. GG expression types
 * ({@code Factor}, {@code CutWidth}, {@code Expression}, {@code AfterStat},
 * {@code AfterScale}, {@code Identity}) implement it to allow the Charm
 * pipeline to accept them without casting.
 */
@CompileStatic
interface CharmExpression {

  /**
   * Returns a human-readable description of this expression.
   *
   * @return descriptive string
   */
  String describe()
}
