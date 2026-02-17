package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.util.NumberCoercionUtil
import se.alipsa.matrix.stats.kde.KernelDensity

/**
 * Y-density stat transformation - kernel density estimation on y values.
 * Used by geom_violin. Extracts y values per x-group, computes KDE, and returns
 * LayerData with x as group center and y as evaluation points.
 * Density is stored in {@code meta.density}.
 */
@CompileStatic
class YDensityStat {

  /**
   * Computes kernel density estimate from y values.
   *
   * @param layer layer specification
   * @param data layer data
   * @return density curves as LayerData with x=center, y=evaluation and meta.density
   */
  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    Map<String, Object> kdeParams = DensityStat.buildKdeParams(StatEngine.effectiveParams(layer))
    Map<Object, List<LayerData>> groups = groupByX(data)
    List<LayerData> result = []
    groups.each { Object centerX, List<LayerData> bucket ->
      List<Number> values = []
      bucket.each { LayerData datum ->
        BigDecimal value = NumberCoercionUtil.coerceToBigDecimal(datum.y)
        if (value != null) {
          values << value
        }
      }
      if (values.size() < 2) {
        return
      }

      KernelDensity kde = new KernelDensity(values, kdeParams)
      double[] yVals = kde.getX()
      double[] densityVals = kde.getDensity()
      LayerData template = bucket.first()

      for (int i = 0; i < yVals.length; i++) {
        LayerData datum = new LayerData(
            x: centerX,
            y: yVals[i] as BigDecimal,
            color: template?.color,
            fill: template?.fill,
            group: template?.group,
            rowIndex: -1
        )
        datum.meta.centerX = centerX
        datum.meta.density = densityVals[i] as BigDecimal
        result << datum
      }
    }
    result
  }

  private static Map<Object, List<LayerData>> groupByX(List<LayerData> data) {
    Map<Object, List<LayerData>> groups = new LinkedHashMap<>()
    data.each { LayerData datum ->
      Object key = datum.x ?: '__all__'
      List<LayerData> bucket = groups[key]
      if (bucket == null) {
        bucket = []
        groups[key] = bucket
      }
      bucket << datum
    }
    groups
  }
}
