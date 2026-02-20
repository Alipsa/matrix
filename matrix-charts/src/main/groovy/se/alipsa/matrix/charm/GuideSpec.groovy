package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Specification for a single guide (legend, colorbar, axis, etc.).
 */
@CompileStatic
class GuideSpec {

  /** The guide type. */
  GuideType type

  /** Optional parameters for guide customization. */
  Map<String, Object> params = [:]

  /**
   * Creates a guide spec with the given type and optional parameters.
   *
   * @param type guide type
   * @param params optional configuration
   */
  GuideSpec(GuideType type, Map<String, Object> params = [:]) {
    this.type = type
    if (params) {
      this.params.putAll(params)
    }
  }

  /** Creates a standard discrete legend guide. */
  static GuideSpec legend(Map<String, Object> params = [:]) {
    new GuideSpec(GuideType.LEGEND, params)
  }

  /** Creates a continuous colorbar guide. */
  static GuideSpec colorbar(Map<String, Object> params = [:]) {
    new GuideSpec(GuideType.COLORBAR, params)
  }

  /** Creates a stepped/binned color guide. */
  static GuideSpec colorsteps(Map<String, Object> params = [:]) {
    new GuideSpec(GuideType.COLORSTEPS, params)
  }

  /** Creates a guide that suppresses the legend. */
  static GuideSpec none() {
    new GuideSpec(GuideType.NONE)
  }

  /** Creates a standard axis guide. */
  static GuideSpec axis(Map<String, Object> params = [:]) {
    new GuideSpec(GuideType.AXIS, params)
  }

  /** Creates a logarithmic tick axis guide. */
  static GuideSpec axisLogticks(Map<String, Object> params = [:]) {
    new GuideSpec(GuideType.AXIS_LOGTICKS, params)
  }

  /** Creates a polar/theta axis guide. */
  static GuideSpec axisTheta(Map<String, Object> params = [:]) {
    new GuideSpec(GuideType.AXIS_THETA, params)
  }

  /** Creates a stacked axis guide. */
  static GuideSpec axisStack(Map<String, Object> params = [:]) {
    new GuideSpec(GuideType.AXIS_STACK, params)
  }

  /** Creates a binned axis guide. */
  static GuideSpec bins(Map<String, Object> params = [:]) {
    new GuideSpec(GuideType.BINS, params)
  }

  /** Creates a custom guide with a closure renderer. */
  static GuideSpec custom(Closure renderClosure, Map<String, Object> params = [:]) {
    Map<String, Object> merged = new LinkedHashMap<>(params)
    merged['renderClosure'] = renderClosure
    new GuideSpec(GuideType.CUSTOM, merged)
  }

  /**
   * Returns a deep copy of this guide spec.
   * Note: closures (e.g. renderClosure for custom guides) are shared by reference
   * since they are effectively immutable in their captured state.
   *
   * @return copy
   */
  GuideSpec copy() {
    Map<String, Object> copiedParams = new LinkedHashMap<>()
    params.each { String key, Object value ->
      copiedParams.put(key, deepCopy(value))
    }
    new GuideSpec(type, copiedParams)
  }

  /**
   * Recursively deep-copies Map and List values.
   * Non-collection values (including closures) are returned as-is.
   */
  private static Object deepCopy(Object value) {
    if (value instanceof Map) {
      Map<Object, Object> result = [:]
      (value as Map).each { Object k, Object v ->
        result[k] = deepCopy(v)
      }
      return result
    }
    if (value instanceof List) {
      List<Object> result = []
      (value as List).each { Object element ->
        result << deepCopy(element)
      }
      return result
    }
    return value
  }
}
