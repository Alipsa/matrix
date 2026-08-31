package se.alipsa.matrix.charm.render.stat

import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.core.ValueConverter

/**
 * 2D binned summary stat for z-like values.
 */
class Summary2DStat {

  private static final int DEFAULT_BINS = 30
  private static final int BIN_CENTER_DIVISOR = 2
  private static final BigDecimal RANGE_FALLBACK = 1G

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    int bins = resolveBins(params)
    String fun = params.fun?.toString()?.toLowerCase() ?: 'mean'

    List<LayerData> numeric = filterNumeric(data)
    if (numeric.isEmpty()) {
      return []
    }

    Bounds bounds = computeBounds(numeric, bins)
    Map<String, List<BigDecimal>> buckets = binValues(numeric, bounds)
    buildResult(buckets, bounds, fun)
  }

  private static int resolveBins(Map<String, Object> params) {
    int bins = ValueConverter.asBigDecimal(params.bins)?.intValue() ?: DEFAULT_BINS
    bins < 1 ? DEFAULT_BINS : bins
  }

  private static List<LayerData> filterNumeric(List<LayerData> data) {
    data.findAll { LayerData datum ->
      ValueConverter.asBigDecimal(datum.x) != null &&
          ValueConverter.asBigDecimal(datum.y) != null
    }
  }

  private static Bounds computeBounds(List<LayerData> numeric, int bins) {
    List<BigDecimal> xs = numeric.collect { ValueConverter.asBigDecimal(it.x) }
    List<BigDecimal> ys = numeric.collect { ValueConverter.asBigDecimal(it.y) }
    BigDecimal xMin = xs.min()
    BigDecimal xMax = xs.max()
    BigDecimal yMin = ys.min()
    BigDecimal yMax = ys.max()
    if (xMin == xMax) {
      xMax = xMax + RANGE_FALLBACK
    }
    if (yMin == yMax) {
      yMax = yMax + RANGE_FALLBACK
    }
    new Bounds(
        xMin: xMin,
        xMax: xMax,
        yMin: yMin,
        yMax: yMax,
        xStep: (xMax - xMin) / bins,
        yStep: (yMax - yMin) / bins,
        bins: bins
    )
  }

  private static Map<String, List<BigDecimal>> binValues(List<LayerData> numeric, Bounds bounds) {
    Map<String, List<BigDecimal>> buckets = [:]
    numeric.each { LayerData datum ->
      BigDecimal x = ValueConverter.asBigDecimal(datum.x)
      BigDecimal y = ValueConverter.asBigDecimal(datum.y)
      int xBin = clampToBin(((x - bounds.xMin) / bounds.xStep).intValue(), bounds.bins - 1)
      int yBin = clampToBin(((y - bounds.yMin) / bounds.yStep).intValue(), bounds.bins - 1)

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
    buckets
  }

  private static int clampToBin(int bin, int maxBin) {
    if (bin < 0) {
      return 0
    }
    if (bin > maxBin) {
      return maxBin
    }
    bin
  }

  private static List<LayerData> buildResult(Map<String, List<BigDecimal>> buckets, Bounds bounds, String fun) {
    List<LayerData> result = []
    buckets.each { String key, List<BigDecimal> values ->
      if (values.isEmpty()) {
        return
      }
      String[] parts = key.split(':')
      int xBin = parts[0] as int
      int yBin = parts[1] as int

      BigDecimal xmin = bounds.xMin + bounds.xStep * xBin
      BigDecimal xmax = xmin + bounds.xStep
      BigDecimal ymin = bounds.yMin + bounds.yStep * yBin
      BigDecimal ymax = ymin + bounds.yStep

      BigDecimal summary = summarize(values, fun)
      LayerData datum = new LayerData(
          x: xmin + bounds.xStep / BIN_CENTER_DIVISOR,
          y: ymin + bounds.yStep / BIN_CENTER_DIVISOR,
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
    BigDecimal value = ValueConverter.asBigDecimal(datum.fill)
    if (value != null) {
      return value
    }
    value = ValueConverter.asBigDecimal(datum.label)
    if (value != null) {
      return value
    }
    ValueConverter.asBigDecimal(datum.weight)
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

  private static class Bounds {
    BigDecimal xMin
    BigDecimal xMax
    BigDecimal yMin
    BigDecimal yMax
    BigDecimal xStep
    BigDecimal yStep
    int bins
  }
}
