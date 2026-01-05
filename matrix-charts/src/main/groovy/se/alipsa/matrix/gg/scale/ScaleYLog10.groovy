package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Log10-transformed continuous scale for the y-axis.
 * Maps values using log10 transformation, useful for data spanning multiple orders of magnitude.
 *
 * Usage:
 * - scale_y_log10() - basic log10 y-axis
 * - scale_y_log10(limits: [1, 1000]) - with explicit limits (in data space)
 *
 * Note: Values <= 0 are filtered out since log10 is undefined for non-positive numbers.
 */
@CompileStatic
class ScaleYLog10 extends ScaleXLog10 {

  ScaleYLog10() {
    aesthetic = 'y'
  }

  ScaleYLog10(Map params) {
    super(params)
    aesthetic = 'y'
  }
}
