package se.alipsa.matrix.charm

/**
 * Shared deep-copy helper for values held in the free-form, mutable
 * DSL/value-object params and defaultAes maps used across the various
 * {@code *Spec} {@code copy()} methods (e.g. {@link GeomSpec}, {@link StatSpec},
 * {@link PositionSpec}, {@link Scale}). Unlike {@link Chart#deepFreezeValue},
 * the copies produced here remain mutable.
 */
class SpecCopyUtil {

  private SpecCopyUtil() { }

  /**
   * Deep-copies a guide specification.
   *
   * @param value guide specification to copy
   * @return copied guide specification
   */
  static GuideSpec deepCopyValue(GuideSpec value) {
    value?.copy()
  }

  /**
   * Deep-copies a map and all nested values.
   *
   * @param value map to copy
   * @return copied mutable map
   */
  static Map<Object, Object> deepCopyValue(Map<?, ?> value) {
    Map<Object, Object> copied = [:]
    value.each { Object key, Object nestedValue ->
      copied[key] = deepCopyValue(nestedValue)
    }
    copied
  }

  /**
   * Deep-copies a set and all nested values.
   *
   * @param value set to copy
   * @return copied mutable set
   */
  static Set<Object> deepCopyValue(Set<?> value) {
    Set<Object> copied = new LinkedHashSet<>()
    value.each { Object nestedValue ->
      copied << deepCopyValue(nestedValue)
    }
    copied
  }

  /**
   * Deep-copies a list and all nested values.
   *
   * @param value list to copy
   * @return copied mutable list
   */
  static List<Object> deepCopyValue(List<?> value) {
    value.collect { Object nestedValue -> deepCopyValue(nestedValue) }
  }

  /**
   * Deep-copies a generic collection and all nested values into a mutable list.
   *
   * @param value collection to copy
   * @return copied mutable list
   */
  static List<Object> deepCopyValue(Collection<?> value) {
    value.collect { Object nestedValue -> deepCopyValue(nestedValue) }
  }

  /**
   * Fallback dispatcher for dynamically typed callers. Known mutable value
   * types are routed to typed overloads; all other values are shared by design.
   *
   * @param value value to copy
   * @return a deep-copied mutable value, or the original immutable/shared value
   */
  static Object deepCopyValue(Object value) {
    // @CompileStatic instanceof narrowing dispatches each branch to its typed overload above.
    if (value instanceof GuideSpec) {
      return deepCopyValue(value)
    }
    if (value instanceof Map) {
      return deepCopyValue(value)
    }
    if (value instanceof Set) {
      return deepCopyValue(value)
    }
    if (value instanceof List) {
      return deepCopyValue(value)
    }
    if (value instanceof Collection) {
      return deepCopyValue(value)
    }
    value
  }

  /**
   * Deep-copies every value in a params/defaultAes map via {@link #deepCopyValue}.
   *
   * @param params map to copy
   * @return a new map with deep-copied values
   */
  static Map<String, Object> deepCopyParams(Map<String, Object> params) {
    Map<String, Object> copied = [:]
    params.each { String key, Object value ->
      copied[key] = deepCopyValue(value)
    }
    copied
  }

  /**
   * Copies a per-layer scale overrides map, dropping any null-valued entries
   * and deep-copying each remaining {@link Scale} via {@code Scale.copy()}.
   *
   * @param scales scale overrides map, possibly null or empty
   * @return a new mutable map of copied, non-null scales
   */
  static Map<String, Scale> copyScales(Map<String, Scale> scales) {
    if (!scales) {
      return [:]
    }
    scales.findAll { String k, Scale v -> v != null }
        .collectEntries { String k, Scale v -> [(k): v.copy()] } as Map<String, Scale>
  }

}
