package se.alipsa.matrix.charm.render.coord

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.core.ValueConverter

/**
 * Cartesian coordinate transformation - the default coordinate system.
 *
 * Applies optional xlim/ylim clamping to numeric x/y values.
 * Values outside the configured limits are squished to the nearest bound.
 *
 * This coordinate step runs after stats/positions but before scale training,
 * so clamped values can influence trained scale domains.
 */
@CompileStatic
class CartesianCoord {

  /**
   * Applies cartesian coordinate transformation.
   * If xlim/ylim are specified, clamps numeric x/y values to those limits.
   * Otherwise, returns data unchanged.
   *
   * @param coordSpec coordinate specification with optional xlim/ylim params
   * @param data layer data to transform
   * @return transformed layer data
   */
  static List<LayerData> compute(CoordSpec coordSpec, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return data
    }

    List<Number> xlim = coordSpec?.xlim
    List<Number> ylim = coordSpec?.ylim

    // No limits: pass through
    if (xlim == null && ylim == null) {
      return data
    }

    BigDecimal xMin = xlim != null && xlim.size() >= 2 && xlim[0] != null ? xlim[0] as BigDecimal : null
    BigDecimal xMax = xlim != null && xlim.size() >= 2 && xlim[1] != null ? xlim[1] as BigDecimal : null
    BigDecimal yMin = ylim != null && ylim.size() >= 2 && ylim[0] != null ? ylim[0] as BigDecimal : null
    BigDecimal yMax = ylim != null && ylim.size() >= 2 && ylim[1] != null ? ylim[1] as BigDecimal : null

    List<LayerData> result = []
    data.each { LayerData datum ->
      LayerData updated = LayerDataUtil.copyDatum(datum)

      // Clamp x
      if (xMin != null || xMax != null) {
        BigDecimal xVal = ValueConverter.asBigDecimal(datum.x)
        if (xVal != null) {
          if (xMin != null && xVal < xMin) updated.x = xMin
          if (xMax != null && xVal > xMax) updated.x = xMax
        }
      }

      // Clamp y
      if (yMin != null || yMax != null) {
        BigDecimal yVal = ValueConverter.asBigDecimal(datum.y)
        if (yVal != null) {
          if (yMin != null && yVal < yMin) updated.y = yMin
          if (yMax != null && yVal > yMax) updated.y = yMax
        }
      }

      result.add(updated)
    }

    result
  }
}
