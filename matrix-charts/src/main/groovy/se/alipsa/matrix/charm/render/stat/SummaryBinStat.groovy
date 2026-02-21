package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.util.NumberCoercionUtil
import se.alipsa.matrix.core.Stat
import java.util.Locale

/**
 * Summary stat over x bins.
 */
@CompileStatic
class SummaryBinStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    String fun = (params.fun ?: 'mean').toString().toLowerCase(Locale.ROOT)
    int bins = NumberCoercionUtil.coerceToBigDecimal(params.bins)?.intValue() ?: 30
    if (bins < 1) {
      bins = 30
    }
    BigDecimal binWidth = NumberCoercionUtil.coerceToBigDecimal(params.binwidth)

    List<LayerData> points = data.findAll { LayerData datum ->
      NumberCoercionUtil.coerceToBigDecimal(datum.x) != null &&
          NumberCoercionUtil.coerceToBigDecimal(datum.y) != null
    }
    if (points.isEmpty()) {
      return []
    }

    List<BigDecimal> xs = points.collect { NumberCoercionUtil.coerceToBigDecimal(it.x) }
    BigDecimal xMin = xs.min()
    BigDecimal xMax = xs.max()
    BigDecimal range = xMax - xMin
    if (range == 0) {
      range = 1
    }
    if (binWidth == null || binWidth <= 0) {
      binWidth = range / bins
    } else {
      bins = (range / binWidth).ceil().intValue()
      bins = bins < 1 ? 1 : bins
    }

    Map<Integer, List<BigDecimal>> yByBin = [:]
    points.each { LayerData datum ->
      BigDecimal x = NumberCoercionUtil.coerceToBigDecimal(datum.x)
      BigDecimal y = NumberCoercionUtil.coerceToBigDecimal(datum.y)
      int idx = ((x - xMin) / binWidth).intValue()
      if (idx < 0) {
        idx = 0
      } else if (idx >= bins) {
        idx = bins - 1
      }
      List<BigDecimal> bucket = yByBin[idx]
      if (bucket == null) {
        bucket = []
        yByBin[idx] = bucket
      }
      bucket << y
    }

    List<LayerData> result = []
    yByBin.keySet().sort().each { int idx ->
      List<BigDecimal> values = yByBin[idx]
      if (values == null || values.isEmpty()) {
        return
      }
      BigDecimal ySummary = switch (fun) {
        case 'median' -> Stat.median(values) as BigDecimal
        case 'sum' -> Stat.sum(values) as BigDecimal
        case 'min' -> Stat.min(values) as BigDecimal
        case 'max' -> Stat.max(values) as BigDecimal
        default -> Stat.mean(values) as BigDecimal
      }
      BigDecimal xmin = xMin + idx * binWidth
      BigDecimal xmax = xmin + binWidth
      LayerData datum = new LayerData(
          x: xmin + binWidth / 2,
          y: ySummary,
          rowIndex: -1
      )
      datum.meta.n = values.size()
      datum.meta.xmin = xmin
      datum.meta.xmax = xmax
      result << datum
    }

    result
  }
}
