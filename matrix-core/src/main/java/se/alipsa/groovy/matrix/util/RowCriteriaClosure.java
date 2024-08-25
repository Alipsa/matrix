package se.alipsa.groovy.matrix.util;

import groovy.lang.Closure;
import se.alipsa.groovy.matrix.Row;

import java.util.function.Predicate;

/**
 * A criteria closure that operates on a Row. Example:
 * <code><pre>
 * var subSet2 = table.subset(new RowCriteriaClosure(it ->
 *   it.getAt(0, Integer.class) > 1
 * ));
 * </pre></code>
 * This is equivalent to doing
 * <code><pre>
 * var subSet2 = new Closure&lt;Boolean&gt;(null) {
 *   public Boolean doCall(Row it) {
 *     return it.getAt(0, Integer.class) > 1
 *   }
 * }
 * </pre></code>
 */
public class RowCriteriaClosure extends Closure<Boolean> {

  Predicate<Row> predicate;

  public RowCriteriaClosure(Predicate<Row> predicate) {
    super(null);
    this.predicate = predicate;
  }

  public Boolean doCall(Row row) {
    return predicate.test(row);
  }
}
