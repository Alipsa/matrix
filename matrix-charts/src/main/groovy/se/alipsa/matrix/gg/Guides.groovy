package se.alipsa.matrix.gg

import groovy.transform.CompileStatic

/**
 * Collection of guide specifications keyed by aesthetic name.
 */
@CompileStatic
class Guides {

  /** Map of normalized aesthetic names to guide specs. */
  Map<String, Object> specs = [:]

  Guides() {}

  /**
   * Create a guide collection from a map of aesthetic -> guide.
   *
   * @param params map with keys like 'color', 'fill', 'shape', 'size'
   */
  Guides(Map params) {
    if (params) {
      params.each { key, value ->
        String normalized = normalizeKey(key as String)
        specs[normalized] = value
      }
    }
  }

  /**
   * Merge guides, preferring values from the provided guide set.
   *
   * @param other guide set to merge
   * @return merged guide collection
   */
  Guides plus(Guides other) {
    Guides merged = new Guides()
    merged.specs.putAll(this.specs)
    if (other?.specs) {
      merged.specs.putAll(other.specs)
    }
    return merged
  }

  /**
   * Fetch a guide spec for the given aesthetic.
   *
   * @param aesthetic aesthetic name
   * @return guide spec or null
   */
  Object getSpec(String aesthetic) {
    if (aesthetic == null) {
      return null
    }
    return specs[normalizeKey(aesthetic)]
  }

  private static String normalizeKey(String key) {
    if (key == null) {
      return null
    }
    return key == 'colour' ? 'color' : key
  }
}
