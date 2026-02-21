package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.Stat
import java.util.Locale

/**
 * Summary stat for grouped summaries on y.
 */
@CompileStatic
class SummaryStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    String fun = (params.fun ?: params['fun.y'] ?: 'mean').toString().toLowerCase(Locale.ROOT)
    Map<List<Object>, List<LayerData>> groups = new LinkedHashMap<>()
    data.each { LayerData datum ->
      List<Object> key = [StatUtils.seriesKey(datum), datum.x]
      List<LayerData> bucket = groups[key]
      if (bucket == null) {
        bucket = []
        groups[key] = bucket
      }
      bucket << datum
    }
    List<LayerData> result = []

    groups.each { List<Object> key, List<LayerData> bucket ->
      List<BigDecimal> values = StatUtils.sortedNumericValues(bucket) { LayerData d -> d.y }
      if (values.isEmpty()) {
        return
      }

      BigDecimal summary = switch (fun) {
        case 'median' -> Stat.median(values) as BigDecimal
        case 'sum' -> Stat.sum(values) as BigDecimal
        case 'min' -> Stat.min(values) as BigDecimal
        case 'max' -> Stat.max(values) as BigDecimal
        default -> Stat.mean(values) as BigDecimal
      }

      LayerData template = bucket.first()
      LayerData datum = new LayerData(
          x: template.x,
          y: summary,
          color: template.color,
          fill: template.fill,
          group: template.group,
          rowIndex: -1
      )
      datum.meta.n = values.size()
      result << datum
    }

    result
  }
}
