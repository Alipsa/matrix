package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Square root-transformed continuous scale for the y-axis.
 * Maps values using sqrt transformation, useful for count data or to reduce right skew.
 *
 * Usage:
 * - scale_y_sqrt() - basic sqrt y-axis
 * - scale_y_sqrt(limits: [0, 100]) - with explicit limits (in data space)
 *
 * Note: Values < 0 are filtered out since sqrt is undefined for negative numbers.
 */
@CompileStatic
class ScaleYSqrt extends ScaleXSqrt {

  ScaleYSqrt() {
    aesthetic = 'y'
  }

  ScaleYSqrt(Map params) {
    super(params)
    aesthetic = 'y'
  }
}
