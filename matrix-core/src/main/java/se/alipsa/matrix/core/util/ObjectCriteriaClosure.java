package se.alipsa.matrix.core.util;

import groovy.lang.Closure;

import java.util.function.Predicate;

/**
 * A generic criteria closure where the predicate value should be cast
 * to the actual type. Example:
 * <code>
 * var subSet = table.subset("place", new ObjectCriteriaClosure(it ->
 *         ((Integer)it) > 1
 *     ));
 * </code>
 */
public class ObjectCriteriaClosure extends Closure<Boolean> {

  Predicate<Object> predicate;

  public ObjectCriteriaClosure(Predicate<Object> predicate) {
    super(null);
    this.predicate = predicate;
  }

  public Boolean doCall(Object o) {
    return predicate.test(o);
  }
}
