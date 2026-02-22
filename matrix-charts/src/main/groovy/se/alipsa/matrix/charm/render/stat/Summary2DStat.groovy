package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.util.NumberCoercionUtil
import se.alipsa.matrix.core.Stat

/**
 * 2D binned summary stat for z-like values.
 */
@CompileStatic
class Summary2DStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    int bins = NumberCoercionUtil.coerceToBigDecimal(params.bins)?.intValue() ?: 30
    if (bins < 1) {
      bins = 30
    }
    String fun = params.fun?.toString()?.toLowerCase() ?: 'mean'

    List<LayerData> numeric = data.findAll { LayerData datum ->
      NumberCoercionUtil.coerceToBigDecimal(datum.x) != null &&
          NumberCoercionUtil.coerceToBigDecimal(datum.y) != null
    }
    if (numeric.isEmpty()) {
      return []
    }

    List<BigDecimal> xs = numeric.collect { NumberCoercionUtil.coerceToBigDecimal(it.x) }
    List<BigDecimal> ys = numeric.collect { NumberCoercionUtil.coerceToBigDecimal(it.y) }
    BigDecimal xMin = xs.min()
    BigDecimal xMax = xs.max()
    BigDecimal yMin = ys.min()
    BigDecimal yMax = ys.max()
    if (xMin == xMax) {
      xMax = xMax + 1
    }
    if (yMin == yMax) {
      yMax = yMax + 1
    }

    BigDecimal xStep = (xMax - xMin) / bins
    BigDecimal yStep = (yMax - yMin) / bins

    Map<String, List<BigDecimal>> buckets = [:]
    numeric.each { LayerData datum ->
      BigDecimal x = NumberCoercionUtil.coerceToBigDecimal(datum.x)
      BigDecimal y = NumberCoercionUtil.coerceToBigDecimal(datum.y)
      int xBin = ((x - xMin) / xStep).intValue()
      int yBin = ((y - yMin) / yStep).intValue()
      if (xBin < 0) xBin = 0
      if (yBin < 0) yBin = 0
      if (xBin > bins - 1) xBin = bins - 1
      if (yBin > bins - 1) yBin = bins - 1

      BigDecimal z = resolveSummaryValue(datum)
      if (z == null) {
        return
      }
      String key = "${xBin}:${yBin}"
      List<BigDecimal> values = buckets[key]
      if (values == null) {
        values = []
        buckets[key] = values
      }
      values << z
    }

    List<LayerData> result = []
    buckets.each { String key, List<BigDecimal> values ->
      if (values.isEmpty()) {
        return
      }
      String[] parts = key.split(':')
      int xBin = parts[0] as int
      int yBin = parts[1] as int

      BigDecimal xmin = xMin + xStep * xBin
      BigDecimal xmax = xmin + xStep
      BigDecimal ymin = yMin + yStep * yBin
      BigDecimal ymax = ymin + yStep

      BigDecimal summary = summarize(values, fun)
      LayerData datum = new LayerData(
          x: xmin + xStep / 2,
          y: ymin + yStep / 2,
          xmin: xmin,
          xmax: xmax,
          ymin: ymin,
          ymax: ymax,
          fill: summary,
          label: summary,
          rowIndex: -1
      )
      datum.meta.summary = summary
      datum.meta.count = values.size()
      datum.meta.fun = fun
      result << datum
    }

    result
  }

  private static BigDecimal resolveSummaryValue(LayerData datum) {
    BigDecimal value = NumberCoercionUtil.coerceToBigDecimal(datum.fill)
    if (value != null) {
      return value
    }
    value = NumberCoercionUtil.coerceToBigDecimal(datum.label)
    if (value != null) {
      return value
    }
    NumberCoercionUtil.coerceToBigDecimal(datum.weight)
  }

  private static BigDecimal summarize(List<BigDecimal> values, String fun) {
    switch (fun) {
      case 'sum' -> Stat.sum(values) as BigDecimal
      case 'min' -> Stat.min(values) as BigDecimal
      case 'max' -> Stat.max(values) as BigDecimal
      case 'median' -> Stat.median(values) as BigDecimal
      case 'count' -> values.size() as BigDecimal
      default -> Stat.mean(values) as BigDecimal
    }
  }
}
