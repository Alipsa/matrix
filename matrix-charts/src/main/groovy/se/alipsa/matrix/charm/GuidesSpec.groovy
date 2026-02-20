package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Collection of guide specifications keyed by normalized aesthetic name.
 */
@CompileStatic
class GuidesSpec {

  /** Map of normalized aesthetic names to guide specs. */
  Map<String, GuideSpec> specs = [:]

  GuidesSpec() {}

  /**
   * Creates a guides spec from a map of aesthetic -> GuideSpec.
   *
   * @param entries aesthetic to guide spec mapping
   */
  GuidesSpec(Map<String, GuideSpec> entries) {
    if (entries) {
      entries.each { String key, GuideSpec value ->
        setSpec(key, value)
      }
    }
  }

  /**
   * Fetches the guide spec for the given aesthetic.
   *
   * @param aesthetic aesthetic name (e.g. 'color', 'fill', 'size')
   * @return guide spec or null
   */
  GuideSpec getSpec(String aesthetic) {
    if (aesthetic == null) {
      return null
    }
    specs[normalizeKey(aesthetic)]
  }

  /**
   * Sets the guide spec for the given aesthetic.
   *
   * @param aesthetic aesthetic name
   * @param spec guide spec
   */
  void setSpec(String aesthetic, GuideSpec spec) {
    if (aesthetic == null) {
      return
    }
    String key = normalizeKey(aesthetic)
    if (spec == null) {
      specs.remove(key)
    } else {
      specs[key] = spec
    }
  }

  /**
   * Returns a deep copy of this guides spec.
   *
   * @return copy
   */
  GuidesSpec copy() {
    GuidesSpec result = new GuidesSpec()
    specs.each { String key, GuideSpec value ->
      if (value != null) {
        result.specs[key] = value.copy()
      }
    }
    result
  }

  /**
   * Merges this guides spec with another, preferring entries from the other.
   *
   * @param other guides spec to merge
   * @return merged guides spec
   */
  GuidesSpec plus(GuidesSpec other) {
    GuidesSpec merged = copy()
    if (other?.specs) {
      other.specs.each { String key, GuideSpec value ->
        merged.specs[normalizeKey(key)] = value?.copy()
      }
    }
    merged
  }

  /**
   * Returns true if there are no guide specs.
   *
   * @return true if empty
   */
  boolean isEmpty() {
    specs.isEmpty()
  }

  private static String normalizeKey(String key) {
    if (key == null) {
      return null
    }
    key == 'colour' ? 'color' : key
  }
}
