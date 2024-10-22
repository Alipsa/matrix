package se.alipsa.groovy.matrix.util;

import java.util.*;

/**
 * Simplifies Matrix creation making Map creation almost as simple as i Groovy
 */
public class Columns extends LinkedHashMap<String, List> {

  public Columns() {
  }

  @SafeVarargs
  public Columns(Map<String, List>... values) {
    for (Map<String, List> vals : values) {
      putAll(vals);
    }
  }

  public Columns add(String name, Object... values) {
    put(name, Arrays.asList(values));
    return this;
  }

  public Columns add(String name, List values) {
    put(name, values);
    return this;
  }
}
