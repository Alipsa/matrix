package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Date scale for the y-axis.
 * Handles Date, LocalDate, LocalDateTime, and other temporal types.
 *
 * Usage:
 * - scale_y_date() - basic date y-axis
 * - scale_y_date(date_labels: '%Y-%m') - custom date format
 * - scale_y_date(date_breaks: '1 month') - breaks every month
 */
@CompileStatic
class ScaleYDate extends ScaleXDate {

  ScaleYDate() {
    aesthetic = 'y'
  }

  ScaleYDate(Map params) {
    super(params)
    aesthetic = 'y'
  }
}
