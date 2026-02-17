package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.util.NumberCoercionUtil
import se.alipsa.matrix.stats.kde.KernelDensity

/**
 * Y-density stat transformation - kernel density estimation on y values.
 * Used by geom_violin. Extracts y values, computes KDE, and returns
 * LayerData with y (original values) and x (density).
 */
@CompileStatic
class YDensityStat {

  /**
   * Computes kernel density estimate from y values.
   *
   * @param layer layer specification
   * @param data layer data
   * @return density curve as LayerData with y=evaluation points and x=density
   */
  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    List<Number> values = []
    data.each { LayerData d ->
      BigDecimal v = NumberCoercionUtil.coerceToBigDecimal(d.y)
      if (v != null) {
        values << v
      }
    }

    if (values.size() < 2) {
      return []
    }

    Map<String, Object> kdeParams = DensityStat.buildKdeParams(StatEngine.effectiveParams(layer))
    KernelDensity kde = new KernelDensity(values, kdeParams)
    double[] yVals = kde.getX()
    double[] densityVals = kde.getDensity()

    LayerData template = data.first()
    List<LayerData> result = []
    for (int i = 0; i < yVals.length; i++) {
      LayerData datum = new LayerData(
          x: densityVals[i] as BigDecimal,
          y: yVals[i] as BigDecimal,
          color: template?.color,
          fill: template?.fill,
          rowIndex: -1
      )
      result << datum
    }
    result
  }
}
