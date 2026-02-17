package se.alipsa.matrix.charm.render.coord

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.util.Logger

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

  private static final Logger log = Logger.getLogger(FixedCoord)
  private static boolean ratioWarningLogged

  /**
   * Applies fixed coordinate transformation.
   * Delegates to CartesianCoord since the data-level behavior is identical.
   * The ratio parameter is currently informational in Charm.
   *
   * @param coordSpec coordinate specification with optional ratio, xlim, ylim
   * @param data layer data to transform
   * @return transformed layer data
   */
  static List<LayerData> compute(CoordSpec coordSpec, List<LayerData> data) {
    if (coordSpec?.ratio != null && !ratioWarningLogged) {
      log.warn("coord_fixed ratio is not yet applied in Charm render; using Cartesian data behavior only")
      ratioWarningLogged = true
    }
    CartesianCoord.compute(coordSpec, data)
  }
}
