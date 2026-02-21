package se.alipsa.matrix.charm.render.coord

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmCoordType
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.util.Logger

/**
 * Central dispatch hub for coordinate transformations.
 * Routes coord computation to the appropriate coord class based on the chart's coord type.
 *
 * Unlike StatEngine/PositionEngine which operate per-layer, the coordinate engine
 * operates at the chart level since the coordinate system is shared across all layers.
 */
@CompileStatic
class CoordEngine {

  private static final Logger log = Logger.getLogger(CoordEngine)

  /**
   * Applies the coordinate transformation specified by the coord spec.
   *
   * @param coordSpec chart coordinate specification
   * @param data layer data from position adjustment
   * @return coordinate-transformed layer data
   */
  static List<LayerData> apply(CoordSpec coordSpec, List<LayerData> data) {
    CharmCoordType coordType = coordSpec?.type ?: CharmCoordType.CARTESIAN
    switch (coordType) {
      case CharmCoordType.CARTESIAN -> CartesianCoord.compute(coordSpec, data)
      case CharmCoordType.FLIP -> FlipCoord.compute(coordSpec, data)
      case CharmCoordType.FIXED -> FixedCoord.compute(coordSpec, data)
      case CharmCoordType.POLAR -> PolarCoord.compute(coordSpec, data)
      case CharmCoordType.RADIAL -> RadialCoord.compute(coordSpec, data)
      case CharmCoordType.TRANS -> TransCoord.compute(coordSpec, data)
      default -> {
        log.debug("Unimplemented coord type ${coordType}, falling back to cartesian")
        CartesianCoord.compute(coordSpec, data)
      }
    }
  }
}
