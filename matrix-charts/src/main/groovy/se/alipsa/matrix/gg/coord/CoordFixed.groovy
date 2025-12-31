package se.alipsa.matrix.gg.coord

import groovy.transform.CompileStatic

/**
 * Fixed aspect ratio coordinate system.
 * Ensures that the ratio between y and x scales is fixed,
 * so one unit on the y-axis has the same physical length as
 * ratio units on the x-axis.
 *
 * Usage:
 * - coord_fixed() - equal scaling (1:1 ratio)
 * - coord_fixed(ratio: 0.5) - y is scaled at half of x
 * - coord_fixed(ratio: 2) - y is scaled at twice x
 *
 * Example:
 * ggplot(data, aes(x: 'x', y: 'y')) +
 *     geom_point() +
 *     coord_fixed()  // ensures equal scaling on both axes
 */
@CompileStatic
class CoordFixed extends CoordCartesian {

  /**
   * Aspect ratio: y units per x unit.
   * Default 1.0 means one unit on y equals one unit on x in physical length.
   * A ratio of 0.5 means y is scaled at half of x.
   * A ratio of 2 means y is scaled at twice x.
   */
  double ratio = 1.0

  /**
   * Returns the configured aspect ratio (y units per x unit).
   * This can be used by rendering or transformation logic to
   * enforce a fixed aspect ratio compared to CoordCartesian.
   */
  double getRatio() {
    return ratio
  }

  CoordFixed() {}

  CoordFixed(double ratio) {
    this.ratio = ratio
  }

  CoordFixed(Map params) {
    super(params)
    if (params.ratio != null) this.ratio = params.ratio as double
  }
}
