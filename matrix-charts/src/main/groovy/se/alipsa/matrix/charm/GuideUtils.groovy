package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Utility methods for normalizing guide configuration values.
 */
@CompileStatic
final class GuideUtils {

  private GuideUtils() {
    // Utility class
  }

  /**
   * Coerces a guide value into a normalized {@link GuideSpec}.
   *
   * @param value guide value
   * @param aesthetic aesthetic/context name used in validation errors
   * @return normalized guide spec, or null if value is null
   */
  static GuideSpec coerceGuide(Object value, String aesthetic) {
    if (value == null) {
      return null
    }
    if (value instanceof GuideSpec) {
      return (value as GuideSpec).copy()
    }
    if (value instanceof GuideType) {
      return new GuideSpec(value as GuideType)
    }
    if (value instanceof CharSequence) {
      GuideType type = GuideType.fromString(value.toString())
      if (type == null) {
        throw new CharmValidationException("Unknown guide type '${value}' for '${aesthetic}'")
      }
      return new GuideSpec(type)
    }
    if (value == false || value == Boolean.FALSE) {
      return GuideSpec.none()
    }
    throw new CharmValidationException(
        "Unsupported guide value type '${value.getClass().name}' for '${aesthetic}'. " +
            "Use GuideSpec, GuideType, String, or false."
    )
  }
}
