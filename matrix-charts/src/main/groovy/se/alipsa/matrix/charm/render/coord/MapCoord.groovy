package se.alipsa.matrix.charm.render.coord

import static se.alipsa.matrix.ext.NumberExtension.PI

import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.core.util.Logger

/**
 * Map projection coordinate transform for lon/lat data.
 */
class MapCoord {

  private static final Logger log = Logger.getLogger(MapCoord)
  private static final BigDecimal MAX_LATITUDE = 85.05

  static List<LayerData> compute(CoordSpec coordSpec, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return data
    }

    List<LayerData> clampedInput = CartesianCoord.compute(coordSpec, data)
    String requestedProjection = coordSpec?.params?.projection?.toString()?.toLowerCase(Locale.ROOT) ?: 'mercator'
    String projection = switch (requestedProjection) {
      case 'mercator', 'equirectangular', 'identity' -> requestedProjection
      default -> {
        log.warn("Unsupported map projection '${requestedProjection}', using identity")
        'identity'
      }
    }
    List<LayerData> transformed = []
    clampedInput.each { LayerData datum ->
      LayerData updated = LayerDataUtil.copyDatum(datum)
      BigDecimal x = ValueConverter.asBigDecimal(updated.x)
      BigDecimal y = ValueConverter.asBigDecimal(updated.y)
      if (x != null && y != null) {
        applyProjection(updated, x, y, projection)
      }
      transformed << updated
    }

    CartesianCoord.compute(withoutLimits(coordSpec), transformed)
  }

  private static void applyProjection(LayerData updated, BigDecimal x, BigDecimal y, String projection) {
    switch (projection) {
      case 'mercator' -> {
        BigDecimal clamped = y
        if (clamped.abs() > MAX_LATITUDE) {
          clamped = clamped.signum() * MAX_LATITUDE
        }
        BigDecimal rad = clamped.toRadians()
        updated.x = x.toRadians()
        updated.y = (PI / 4 + rad / 2).tan().log()
      }
      case 'equirectangular', 'identity' -> {
        updated.x = x
        updated.y = y
      }
      default -> {
        updated.x = x
        updated.y = y
      }
    }
  }

  private static CoordSpec withoutLimits(CoordSpec coordSpec) {
    CoordSpec copy = coordSpec?.copy() ?: new CoordSpec()
    copy.xlim = null
    copy.ylim = null
    copy
  }
}
