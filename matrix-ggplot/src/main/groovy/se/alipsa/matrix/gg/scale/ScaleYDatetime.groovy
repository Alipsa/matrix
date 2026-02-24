package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * DateTime scale for the y-axis.
 * Similar to ScaleXDatetime but for the y-axis.
 *
 * Usage:
 * - scale_y_datetime() - basic datetime y-axis
 * - scale_y_datetime(date_labels: 'yyyy-MM-dd HH:mm') - custom datetime format
 * - scale_y_datetime(date_breaks: '1 hour') - breaks every hour
 */
@CompileStatic
class ScaleYDatetime extends ScaleXDatetime {

  ScaleYDatetime() {
    aesthetic = 'y'
  }

  ScaleYDatetime(Map params) {
    super(params)
    aesthetic = 'y'
  }
}
