package se.alipsa.matrix.gg.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.layer.StatType

/**
 * Spoke statistics for converting angle/radius data to line segment endpoints.
 * This stat is used with geom_spoke() to create radial line segments.
 *
 * In ggplot2, stat_spoke() is typically used with angle and radius aesthetics.
 * The stat itself doesn't transform data - transformation happens in the geom.
 * This class exists for semantic consistency with ggplot2's API.
 *
 * Parameters:
 * <ul>
 *   <li><b>angle:</b> column name for angle values (in radians)</li>
 *   <li><b>radius:</b> column name for radius/length values</li>
 * </ul>
 *
 * Usage:
 * <pre>
 * // Basic usage with angle and radius columns
 * ggplot(data, aes('x', 'y')) +
 *   stat_spoke(angle: 'direction', radius: 'strength')
 *
 * // Equivalent to using geom_spoke directly
 * ggplot(data, aes('x', 'y')) +
 *   geom_spoke(aes(angle: 'direction', radius: 'strength'))
 * </pre>
 */
@CompileStatic
class StatsSpoke extends Stats {

  /**
   * Create a stat_spoke specification.
   *
   * @param params stat parameters: angle, radius
   */
  StatsSpoke(Map params = [:]) {
    super(StatType.SPOKE, params)
  }
}
