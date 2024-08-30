package se.alipsa.groovy.matrix.util;

import groovy.lang.Closure;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A convenience class translating a lambda expression (bi function) into a Closure
 * example
 * <code><pre>
 * Closure closure = new ValueTwoArgClosure&lt;Long, Double, Integer&gt;((x,y) -> (long) (x / y * 2));
 * </pre></code>
 * @param <T> the return type
 * @param <V1> the type of the first param to act on
 * @param <V2> the type of the second param to act on
 */
public class ValueTwoArgClosure<T, V1, V2> extends Closure<T> {
  BiFunction<V1, V2, T> function;

  public ValueTwoArgClosure(BiFunction<V1, V2, T> function) {
    super(null);
    this.function = function;
  }

  public T doCall(V1 o1, V2 o2) {
    return function.apply(o1, o2);
  }
}
