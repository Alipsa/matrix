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
    this.name = name ?: ".expr.${System.identityHashCode(closure)}"
  }

  /**
   * Evaluate the expression for a single row.
   * Returns null if the closure returns null or a non-numeric value.
   * @throws RuntimeException if the closure throws an exception
   */
  @CompileDynamic
  Number evaluate(Row row) {
    try {
      def result = closure.call(row)
      if (result == null) {
        return null
      }
      if (result instanceof Number) {
        return (Number) result
      }
      // Try to convert string to number
      String str = result.toString()
      if (str.contains('.')) {
        return new BigDecimal(str)
      }
      return new BigInteger(str)
    } catch (Exception e) {
      throw new RuntimeException("Expression evaluation failed for row: ${e.message}", e)
    }
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
   * If a column with the same name already exists, generates a unique name.
   * Returns the column name used.
   */
  String addToMatrix(Matrix data) {
    List<Number> values = evaluateAll(data)
    String colName = name
    // Check if column already exists and generate unique name if needed
    if (data.columnNames().contains(colName)) {
      int suffix = 1
      while (data.columnNames().contains("${colName}_${suffix}")) {
        suffix++
      }
      colName = "${colName}_${suffix}"
    }
    data.addColumn(colName, values)
    return colName
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
