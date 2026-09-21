package se.alipsa.matrix.gg.aes

import groovy.transform.CompileDynamic

import se.alipsa.matrix.charm.CharmExpression
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row

import java.util.concurrent.atomic.AtomicInteger

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
 * The closure receives a Row object and may return a number, string or boolean.
 */
@SuppressWarnings(['ReturnNullFromCatchBlock', 'ThrowRuntimeException'])
class Expression implements CharmExpression {

  private static final AtomicInteger NAME_COUNTER = new AtomicInteger(0)

  /** The closure that computes the value from row data */
  final Closure closure

  /** Optional name for the generated column */
  final String name

  Expression(Closure closure) {
    this(closure, null)
  }

  Expression(Closure closure, String name) {
    if (closure == null) {
      throw new IllegalArgumentException('Expression closure cannot be null')
    }
    this.closure = closure
    this.name = name ?: ".expr.${NAME_COUNTER.incrementAndGet()}"
  }

  /**
   * Evaluate the expression for a single row.
   * Numbers, booleans and other objects are preserved. Numeric text is converted to a number
   * so that numeric string expressions train continuous scales correctly.
   *
   * @param row the row to evaluate
   * @return the closure result, or null when the closure returns null
   * @throws RuntimeException if the closure throws an exception
   */
  @CompileDynamic
  @SuppressWarnings('UnnecessaryToString')
  Object evaluate(Row row) {
    try {
      def result = closure.call(row)
      if (result == null || result instanceof Number || result instanceof Boolean) {
        return result
      }
      if (result instanceof CharSequence) {
        return coerceNumericText(result.toString())
      }
      result
    } catch (Exception e) {
      throw new RuntimeException("Expression evaluation failed for row: ${e.message}", e)
    }
  }

  private static Object coerceNumericText(String text) {
    String trimmed = text.trim()
    if (trimmed.isEmpty()) {
      return text
    }
    try {
      trimmed.contains('.') || trimmed.contains('e') || trimmed.contains('E')
          ? new BigDecimal(trimmed) : new BigInteger(trimmed)
    } catch (NumberFormatException ignored) {
      text
    }
  }

  /**
   * Evaluate the expression for all rows in a matrix.
   * Returns a list of computed values.
   */
  List<Object> evaluateAll(Matrix data) {
    List<Object> results = new ArrayList<>(data.rowCount())
    for (Row row : data) {
      results.add(evaluate(row))
    }
    results
  }

  /**
   * Add a computed column to the matrix based on this expression.
   * If a column with the same name already exists, generates a unique name.
   * Returns the column name used.
   */
  String addToMatrix(Matrix data) {
    List<Object> values = evaluateAll(data)
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
    colName
  }

  /**
   * Get the generated column name.
   */
  String getName() {
    name
  }

  @Override
  String toString() {
    return "expr($name)"
  }

  /**
   * Static factory method.
   */
  static Expression of(Closure closure) {
    new Expression(closure)
  }

  static Expression of(Closure closure, String name) {
    new Expression(closure, name)
  }

  @Override
  String describe() {
    "expr(${name})"
  }
}
