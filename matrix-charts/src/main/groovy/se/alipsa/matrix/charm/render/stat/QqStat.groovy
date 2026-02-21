package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Q-Q stat against the standard normal distribution.
 */
@CompileStatic
class QqStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    List<LayerData> result = []
    StatUtils.groupBySeries(data).each { Object key, List<LayerData> bucket ->
      List<BigDecimal> values = StatUtils.sortedNumericValues(bucket) { LayerData d -> d.x != null ? d.x : d.y }
      if (values.isEmpty()) {
        return
      }
      int n = values.size()
      for (int i = 0; i < n; i++) {
        BigDecimal p = (i + 0.5) / n
        BigDecimal theoretical = StatUtils.normalQuantile(p)
        LayerData datum = new LayerData(
            x: theoretical,
            y: values[i],
            group: key == '__all__' ? null : key,
            rowIndex: -1
        )
        datum.meta.level = key
        result << datum
      }
    }
    result
  }
}
