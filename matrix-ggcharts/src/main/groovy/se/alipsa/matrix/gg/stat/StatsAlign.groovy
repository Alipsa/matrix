package se.alipsa.matrix.gg.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.layer.StatType

/**
 * Alignment statistics for interpolating groups to common x-coordinates.
 * This stat is primarily used with geom_area() for stacked area charts.
 *
 * In ggplot2, stat_align() ensures that all groups in a stacked area chart
 * have y-values at the same x-coordinates by linearly interpolating where needed.
 * This enables smooth stacking even when groups have misaligned x-values.
 *
 * The stat:
 * <ul>
 *   <li>Creates a union of all x-values across all groups</li>
 *   <li>Interpolates y-values for each group at those common x-coordinates</li>
 *   <li>Uses linear interpolation between existing points</li>
 *   <li>Extrapolates at boundaries using nearest point value</li>
 * </ul>
 *
 * Usage:
 * <pre>
 * // Automatic with geom_area (default behavior)
 * ggplot(data, aes(x: 'time', y: 'value', fill: 'group')) +
 *   geom_area()  // Uses stat_align by default
 *
 * // Explicitly disable alignment
 * ggplot(data, aes(x: 'time', y: 'value', fill: 'group')) +
 *   geom_area(stat: 'identity')  // No interpolation
 *
 * // Use with other geoms (less common)
 * ggplot(data, aes('x', 'y', color: 'group')) +
 *   geom_line() +
 *   stat_align()
 * </pre>
 */
@CompileStatic
class StatsAlign extends Stats {

  /**
   * Create a stat_align specification.
   *
   * @param params stat parameters (currently unused, reserved for future interpolation options)
   */
  StatsAlign(Map params = [:]) {
    super(StatType.ALIGN, params)
  }
}
