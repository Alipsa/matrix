package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Empirical cumulative distribution function stat.
 */
@CompileStatic
class EcdfStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    boolean pad = StatEngine.effectiveParams(layer).pad == true
    List<LayerData> result = []

    StatUtils.groupBySeries(data).each { Object key, List<LayerData> bucket ->
      List<BigDecimal> values = StatUtils.sortedNumericValues(bucket) { LayerData d -> d.x }
      if (values.isEmpty()) {
        return
      }

      if (pad) {
        result << new LayerData(
            x: values.first(),
            y: 0.0,
            group: key == '__all__' ? null : key,
            rowIndex: -1
        )
      }

      int n = values.size()
      for (int i = 0; i < n; i++) {
        result << new LayerData(
            x: values[i],
            y: (i + 1) / (n as BigDecimal),
            group: key == '__all__' ? null : key,
            rowIndex: -1
        )
      }
    }

    result
  }
}
