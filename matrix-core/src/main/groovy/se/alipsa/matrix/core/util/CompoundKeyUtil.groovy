package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic

/**
 * Helper methods for compound list keys used in grouped and indexed lookups.
 */
@CompileStatic
class CompoundKeyUtil {

  /**
   * Create a reusable mutable probe for a fixed-width compound key.
   *
   * @param size the number of key levels
   * @return a mutable probe initialized with null placeholders
   */
  static List<Object> createProbe(int size) {
    List<Object> probe = new ArrayList<>(size)
    for (int i = 0; i < size; i++) {
      probe.add(null)
    }
    probe
  }

  /**
   * Look up the row bucket for the current probe values, creating a new immutable key only
   * when a distinct compound key is encountered for the first time.
   *
   * @param map the map keyed by immutable compound-key lists
   * @param probe the reusable mutable probe containing the current key values
   * @return the mutable row-index bucket for the current compound key
   */
  static List<Integer> getOrCreateRowBucket(Map<List<?>, List<Integer>> map, List<Object> probe) {
    List<Integer> bucket = map.get(probe)
    if (bucket != null) {
      return bucket
    }
    List<?> immutableKey = Collections.unmodifiableList(new ArrayList<>(probe))
    bucket = []
    map[immutableKey] = bucket
    bucket
  }

}
