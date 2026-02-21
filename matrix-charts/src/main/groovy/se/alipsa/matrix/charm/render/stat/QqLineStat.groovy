package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Q-Q reference line stat.
 */
@CompileStatic
class QqLineStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    List<LayerData> result = []
    StatUtils.groupBySeries(data).each { Object key, List<LayerData> bucket ->
      List<BigDecimal> values = StatUtils.sortedNumericValues(bucket) { LayerData d -> d.x ?: d.y }
      if (values.size() < 2) {
        return
      }

      BigDecimal q1 = StatUtils.quantileType7(values, 0.25)
      BigDecimal q3 = StatUtils.quantileType7(values, 0.75)
      BigDecimal theoQ1 = StatUtils.normalQuantile(0.25)
      BigDecimal theoQ3 = StatUtils.normalQuantile(0.75)
      BigDecimal denom = theoQ3 - theoQ1
      if (denom == 0) {
        return
      }

      BigDecimal slope = (q3 - q1) / denom
      BigDecimal intercept = q1 - slope * theoQ1
      BigDecimal minTheo = StatUtils.normalQuantile(0.5 / values.size())
      BigDecimal maxTheo = StatUtils.normalQuantile((values.size() - 0.5) / values.size())

      LayerData p1 = new LayerData(
          x: minTheo,
          y: intercept + slope * minTheo,
          group: key == '__all__' ? null : key,
          rowIndex: -1
      )
      LayerData p2 = new LayerData(
          x: maxTheo,
          y: intercept + slope * maxTheo,
          group: key == '__all__' ? null : key,
          rowIndex: -1
      )
      p1.meta.slope = slope
      p1.meta.intercept = intercept
      p2.meta.slope = slope
      p2.meta.intercept = intercept
      result << p1
      result << p2
    }

    result
  }
}
