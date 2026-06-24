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
   * Deep-copies a single params/defaultAes map value. Maps, Lists, Sets, and
   * other Collections are recursively copied into new mutable containers;
   * {@link GuideSpec} values delegate to their own {@code copy()}; all other
   * values (including Closures and other non-collection objects) are
   * returned as-is since they are effectively immutable or shared by design.
   *
   * @param value value to copy
   * @return a deep-copied, still-mutable value
   */
  static Object deepCopyValue(Object value) {
    if (value instanceof GuideSpec) {
      return value.copy()
    }
    if (value instanceof Map) {
      Map<Object, Object> copied = [:]
      value.each { Object k, Object v ->
        copied[k] = deepCopyValue(v)
      }
      return copied
    }
    if (value instanceof Set) {
      Set<Object> copied = new LinkedHashSet<>()
      value.each { Object v ->
        copied << deepCopyValue(v)
      }
      return copied
    }
    if (value instanceof List) {
      return value.collect { Object v -> deepCopyValue(v) }
    }
    if (value instanceof Collection) {
      List<Object> copied = []
      value.each { Object v ->
        copied << deepCopyValue(v)
      }
      return copied
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

}
