package se.alipsa.matrix.charm.render.coord

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Fixed aspect ratio coordinate transformation.
 * Ensures that the ratio between y and x scales is fixed,
 * so one unit on the y-axis has the same physical length as
 * {@code ratio} units on the x-axis.
 *
 * At the data level, this behaves identically to CartesianCoord
 * (pass-through with optional xlim/ylim clipping). The {@code ratio}
 * parameter is metadata consumed by the renderer when setting up
 * axis scale ranges to enforce the aspect ratio constraint.
 */
@CompileStatic
class FixedCoord {

  /**
   * Applies fixed coordinate transformation.
   * Delegates to CartesianCoord since the data-level behavior is identical.
   * The ratio parameter is consumed by the renderer during scale setup.
   *
   * @param coordSpec coordinate specification with optional ratio, xlim, ylim
   * @param data layer data to transform
   * @return transformed layer data
   */
  static List<LayerData> compute(CoordSpec coordSpec, List<LayerData> data) {
    CartesianCoord.compute(coordSpec, data)
  }
}
