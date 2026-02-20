package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * CSS attribute configuration for charm-rendered SVG output.
 */
@CompileStatic
class CssAttributesSpec {

  /** Master toggle for CSS class/id/data-* emission. */
  boolean enabled = false

  /** Whether geom elements receive gg-style CSS classes (for example {@code gg-point}). */
  boolean includeClasses = true

  /** Whether geom elements receive generated IDs. */
  boolean includeIds = true

  /** Whether geom elements receive {@code data-*} attributes for interactivity. */
  boolean includeDataAttributes = false

  /** Optional preferred ID prefix. */
  String chartIdPrefix

  /** Fallback ID prefix when {@link #chartIdPrefix} is absent/invalid. */
  String idPrefix = 'gg'

  /**
   * Returns a defensive copy.
   *
   * @return copied spec
   */
  CssAttributesSpec copy() {
    new CssAttributesSpec(
        enabled: enabled,
        includeClasses: includeClasses,
        includeIds: includeIds,
        includeDataAttributes: includeDataAttributes,
        chartIdPrefix: chartIdPrefix,
        idPrefix: idPrefix
    )
  }
}
