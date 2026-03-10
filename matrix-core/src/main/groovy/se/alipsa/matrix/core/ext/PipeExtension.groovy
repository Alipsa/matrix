package se.alipsa.matrix.core.ext

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.GroupedMatrix
import se.alipsa.matrix.core.Matrix

/**
 * Extension methods that add the {@code |} (pipe) operator to Matrix, Column,
 * and GroupedMatrix. Each method delegates to the instance {@code pipe(Closure)}
 * method, providing syntactic sugar for left-to-right pipeline chains.
 *
 * <p>Registered in {@code META-INF/groovy/org.codehaus.groovy.runtime.ExtensionModule}.</p>
 */
@CompileStatic
class PipeExtension {

  /**
   * Operator overload for {@code |} on {@link Matrix} — syntactic sugar for
   * {@link Matrix#pipe(Closure)}.
   *
   * <pre>
   * matrix | { it.selectColumns('a') } | { it.orderBy('a') }
   * </pre>
   *
   * @param self the matrix instance
   * @param transform a closure that receives the matrix and returns any value
   * @return the result of {@code self.pipe(transform)}
   */
  static Object or(Matrix self, Closure transform) {
    self.pipe(transform)
  }

  /**
   * Operator overload for {@code |} on {@link Column} — syntactic sugar for
   * {@link Column#pipe(Closure)}.
   *
   * <pre>
   * column | { it.removeNulls() } | { it.cumsum() }
   * </pre>
   *
   * @param self the column instance
   * @param transform a closure that receives the column and returns any value
   * @return the result of {@code self.pipe(transform)}
   */
  static Object or(Column self, Closure transform) {
    self.pipe(transform)
  }

  /**
   * Set-union operator for {@link Column} with a {@code Collection} argument.
   * Reproduces the standard Groovy {@code |} set-union semantics: returns
   * a new {@code Set} containing all unique elements from both operands.
   *
   * @param self the column instance
   * @param right the collection to union with
   * @return a new set containing the union of both collections
   */
  static Set or(Column self, Collection right) {
    Set result = new LinkedHashSet(self)
    result.addAll(right)
    result
  }

  /**
   * Operator overload for {@code |} on {@link GroupedMatrix} — syntactic sugar for
   * {@link GroupedMatrix#pipe(Closure)}.
   *
   * <pre>
   * Stat.groupBy(m, 'dept') | { it.agg(salary: { Stat.sum(it) }) }
   * </pre>
   *
   * @param self the grouped matrix instance
   * @param transform a closure that receives the grouped matrix and returns any value
   * @return the result of {@code self.pipe(transform)}
   */
  static Object or(GroupedMatrix self, Closure transform) {
    self.pipe(transform)
  }
}
