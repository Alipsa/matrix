package se.alipsa.matrix.core.util;

import java.util.*;

public class CollectionUtils {


  /**
   * Convenience method to create a list almost as simple as in Groovy ;)
   *
   * @param values the values that the list should contain
   * @param <T> the type of list to return
   * @return a List&lt;T&gt; of the values specified
   */
  public static <T> List<T> c(T... values) {
    return Arrays.asList(values);
  }

  /**
   * Convenience method to create a list almost as simple as in Groovy ;)
   *
   * @param values the values that the list should contain
   * @return a List&lt;?&gt; of the values specified
   */
  public static List cg(Object... values) {
    return Arrays.asList(values);
  }

  public static <T> List<T> cr(T o, int numRepeats) {
    List<T> list = new ArrayList<>();
    for (int i = 0; i < numRepeats; i++) {
      list.add(o);
    }
    return list;
  }

  public static LinkedHashMap<String, List> m(String name, Object... values) {
    LinkedHashMap<String, List> map = new LinkedHashMap<>();
    map.put(name, Arrays.asList(values));
    return map;
  }

  public static LinkedHashMap<String, List> m(String name, List values) {
    LinkedHashMap<String, List> map = new LinkedHashMap<>();
    map.put(name, values);
    return map;
  }


  public static List r(int from, int to) {
    List<Integer> list = new ArrayList<>();
    for (int i = from; i <= to; i++) {
      list.add(i);
    }
    return list;
  }

  public static <K, V> LinkedHashMap<K, V> lhm(K k1, V v1) {
    LinkedHashMap<K, V> map = new LinkedHashMap<>();
    map.put(k1, v1);
    return map;
  }

  public static <K, V> LinkedHashMap<K, V> lhm(K k1, V v1, K k2, V v2) {
    LinkedHashMap<K, V> map = new LinkedHashMap<>();
    map.put(k1, v1);
    map.put(k2, v2);
    return map;
  }

  public static <K, V> LinkedHashMap<K, V> lhm(K k1, V v1, K k2, V v2, K k3, V v3) {
    LinkedHashMap<K, V> map = new LinkedHashMap<>();
    map.put(k1, v1);
    map.put(k2, v2);
    map.put(k3, v3);
    return map;
  }
}
