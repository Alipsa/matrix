package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Marker object that signals a per-layer scale reset in a gg chart composition.
 *
 * <p>When added to a chart with {@code +}, subsequent scales for the specified
 * aesthetic will be attached to the preceding layer rather than the chart-level
 * scale spec. This enables multiple layers to use different scales for the same
 * aesthetic (e.g. different color palettes).</p>
 *
 * <p>Usage: {@code ggplot(...) + geom_point(...) + new_scale_color() + geom_line(...) + scale_color_manual(...)}</p>
 */
@CompileStatic
class NewScaleMarker {

  /** The aesthetic being reset (e.g. 'color', 'fill'). */
  final String aesthetic

  /**
   * Creates a new scale marker.
   *
   * @param aesthetic the aesthetic to reset
   */
  NewScaleMarker(String aesthetic) {
    this.aesthetic = aesthetic
  }
}
