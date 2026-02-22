package se.alipsa.matrix.charm.render.coord

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Fixed aspect ratio coordinate transformation.
 *
 * At the data level, this behaves identically to CartesianCoord
 * (pass-through with optional xlim/ylim clamping).
 *
 * In the current Charm render pipeline, {@code ratio} is carried in
 * {@link CoordSpec} but is not applied to adjust panel dimensions or
 * scale ranges yet.
 */
@CompileStatic
class FixedCoord {

  /**
   * Applies fixed coordinate transformation.
   * Delegates to CartesianCoord since the data-level behavior is identical.
   * Ratio handling is applied during positional scale training.
   *
   * @param coordSpec coordinate specification with optional ratio, xlim, ylim
   * @param data layer data to transform
   * @return transformed layer data
   */
  static List<LayerData> compute(CoordSpec coordSpec, List<LayerData> data) {
    CartesianCoord.compute(coordSpec, data)
  }
}
