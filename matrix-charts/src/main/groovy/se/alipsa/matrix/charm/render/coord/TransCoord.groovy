package se.alipsa.matrix.charm.render.coord

import se.alipsa.matrix.charm.CharmValidationException
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.ScaleTransform
import se.alipsa.matrix.charm.ScaleTransforms
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.core.ValueConverter

/**
 * Coordinate transform applying named transforms to x/y.
 */
@SuppressWarnings('Instanceof')
class TransCoord {

  static List<LayerData> compute(CoordSpec coordSpec, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return data
    }

    ScaleTransform xTrans = resolveTransform(coordSpec?.params?.x)
    ScaleTransform yTrans = resolveTransform(coordSpec?.params?.y)
    if (xTrans == null && yTrans == null) {
      return CartesianCoord.compute(coordSpec, data)
    }

    List<LayerData> transformed = []
    data.each { LayerData datum ->
      LayerData updated = LayerDataUtil.copyDatum(datum)
      if (xTrans != null) {
        BigDecimal x = ValueConverter.asBigDecimal(updated.x)
        if (x != null) {
          updated.x = xTrans.apply(x)
        }
      }
      if (yTrans != null) {
        BigDecimal y = ValueConverter.asBigDecimal(updated.y)
        if (y != null) {
          updated.y = yTrans.apply(y)
        }
      }
      transformed << updated
    }
    CartesianCoord.compute(coordSpec, transformed)
  }

  private static ScaleTransform resolveTransform(Object value) {
    if (value == null) {
      return null
    }
    try {
      if (value instanceof ScaleTransform) {
        return ScaleTransforms.resolve(value as ScaleTransform)
      }
      if (value instanceof CharSequence) {
        return ScaleTransforms.resolve(value.toString())
      }
      null
    } catch (CharmValidationException ignored) {
      null
    }
  }

}
