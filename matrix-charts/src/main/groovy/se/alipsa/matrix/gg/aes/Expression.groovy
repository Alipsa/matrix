package se.alipsa.matrix.gg.aes

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row

/**
 * Wrapper for closure-based expressions in aesthetic mappings.
 * Use expr() or closures in aes() to compute derived values from data.
 *
 * Example:
 * <pre>
 * // Using closure directly
 * aes(x: 'displ', y: { 1.0 / it.hwy })
 *
 * // Using expr() for clarity
 * aes(x: 'displ', y: expr { 1.0 / it.hwy })
 * </pre>
 *
 * The closure receives a Row object and should return a numeric value.
 */
@CompileStatic
class Expression {

  /** The closure that computes the value from row data */
  final Closure<Number> closure

  /** Optional name for the generated column */
  final String name

  Expression(Closure<Number> closure) {
    this(closure, null)
  }

  Expression(Closure<Number> closure, String name) {
    if (closure == null) {
      throw new IllegalArgumentException("Expression closure cannot be null")
    }
    this.closure = closure
    this.name = name ?: "_expr_${System.identityHashCode(closure)}"
  }

  /**
   * Evaluate the expression for a single row.
   */
  @CompileDynamic
  Number evaluate(Row row) {
    return closure.call(row) as Number
  }

  /**
   * Evaluate the expression for all rows in a matrix.
   * Returns a list of computed values.
   */
  List<Number> evaluateAll(Matrix data) {
    List<Number> results = new ArrayList<>(data.rowCount())
    for (Row row : data) {
      results.add(evaluate(row))
    }
    return results
  }

  /**
   * Add a computed column to the matrix based on this expression.
   * Returns the column name used.
   */
  String addToMatrix(Matrix data) {
    List<Number> values = evaluateAll(data)
    data.addColumn(name, values)
    return name
  }

  /**
   * Get the generated column name.
   */
  String getName() {
    return name
  }

  @Override
  String toString() {
    return "expr($name)"
  }

  /**
   * Static factory method.
   */
  static Expression of(Closure<Number> closure) {
    return new Expression(closure)
  }

  static Expression of(Closure<Number> closure, String name) {
    return new Expression(closure, name)
  }
}
