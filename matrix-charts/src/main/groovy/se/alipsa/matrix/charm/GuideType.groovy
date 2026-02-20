package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Enumeration of guide types for legend and axis rendering.
 */
@CompileStatic
enum GuideType {

  /** Standard discrete legend with color/shape/size keys. */
  LEGEND,

  /** Continuous color gradient bar. */
  COLORBAR,

  /** Stepped/binned color blocks. */
  COLORSTEPS,

  /** Suppress the guide entirely. */
  NONE,

  /** Standard axis guide with optional label rotation and overlap checking. */
  AXIS,

  /** Axis guide with logarithmic tick marks at three tiers. */
  AXIS_LOGTICKS,

  /** Axis guide for polar/theta coordinates. */
  AXIS_THETA,

  /** Stacked axis guides on the same side. */
  AXIS_STACK,

  /** Binned axis guide. */
  BINS,

  /** User-defined custom guide via closure. */
  CUSTOM

  private static final Map<String, GuideType> LOOKUP = [:]

  static {
    values().each { GuideType gt ->
      LOOKUP[gt.name().toLowerCase(Locale.ROOT)] = gt
    }
    // Common aliases
    LOOKUP['coloursteps'] = COLORSTEPS
    LOOKUP['colorsteps'] = COLORSTEPS
    LOOKUP['colour_steps'] = COLORSTEPS
    LOOKUP['color_steps'] = COLORSTEPS
    LOOKUP['color_bar'] = COLORBAR
    LOOKUP['colour_bar'] = COLORBAR
    LOOKUP['colourbar'] = COLORBAR
    LOOKUP['axis_log_ticks'] = AXIS_LOGTICKS
    LOOKUP['logticks'] = AXIS_LOGTICKS
  }

  /**
   * Parses a guide type from a string, supporting aliases.
   *
   * @param value string representation
   * @return parsed guide type or null if not recognized
   */
  static GuideType fromString(String value) {
    if (value == null || value.isBlank()) {
      return null
    }
    LOOKUP[value.trim().toLowerCase(Locale.ROOT)]
  }
}
