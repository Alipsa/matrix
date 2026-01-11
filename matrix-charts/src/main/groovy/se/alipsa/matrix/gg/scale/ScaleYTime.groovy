package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Time-of-day scale for the y-axis.
 * Handles LocalTime values representing times within a 24-hour cycle (00:00:00 to 23:59:59).
 *
 * Usage:
 * - scale_y_time() - basic time y-axis
 * - scale_y_time(time_format: 'h:mm a') - 12-hour format with AM/PM
 * - scale_y_time(time_breaks: '1 hour') - breaks every hour
 */
@CompileStatic
class ScaleYTime extends ScaleXTime {

  ScaleYTime() {
    super()
    aesthetic = 'y'
    position = 'left'  // Always set to left for Y-axis
  }

  ScaleYTime(Map params) {
    super(params)
    aesthetic = 'y'
    // Set to left unless user explicitly specified a position
    if (!params.position) {
      position = 'left'
    }
  }
}
