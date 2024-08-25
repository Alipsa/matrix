package se.alipsa.groovy.matrix.util;

import groovy.lang.Closure;

import java.util.function.Function;

/**
 * A convenience class translating a lambda expression (function) into a Closure
 * example
 * <code><pre>
 * Closure closure = new ValueClosure<Integer, Integer>(it -> it * 2)
 * </pre></code>
 * @param <T> the return type
 * @param <V> the type to act on
 */
public class ValueClosure<T, V> extends Closure<T> {
  Function<V, T> function;

  public ValueClosure(Function<V, T> function) {
    super(null);
    this.function = function;
  }

  public T doCall(V o) {
    return function.apply(o);
  }
}
