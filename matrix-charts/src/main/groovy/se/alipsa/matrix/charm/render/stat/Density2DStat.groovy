package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Approximate 2D density stat by binning points and assigning level groups.
 */
@CompileStatic
class Density2DStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    int bins = NumberCoercionUtil.coerceToBigDecimal(params.bins)?.intValue() ?: 10
    if (bins < 2) {
      bins = 10
    }

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

    Map<String, Integer> counts = [:]
    numeric.each { LayerData datum ->
      BigDecimal x = NumberCoercionUtil.coerceToBigDecimal(datum.x)
      BigDecimal y = NumberCoercionUtil.coerceToBigDecimal(datum.y)
      int xBin = ((x - xMin) / xStep).intValue()
      int yBin = ((y - yMin) / yStep).intValue()
      if (xBin < 0) xBin = 0
      if (yBin < 0) yBin = 0
      if (xBin > bins - 1) xBin = bins - 1
      if (yBin > bins - 1) yBin = bins - 1
      String key = "${xBin}:${yBin}"
      counts[key] = (counts[key] ?: 0) + 1
    }

    if (counts.isEmpty()) {
      return []
    }

    int maxCount = counts.values().max() ?: 1
    List<LayerData> result = []
    counts.each { String key, Integer count ->
      String[] parts = key.split(':')
      int xBin = parts[0] as int
      int yBin = parts[1] as int

      BigDecimal xmin = xMin + xStep * xBin
      BigDecimal xmax = xmin + xStep
      BigDecimal ymin = yMin + yStep * yBin
      BigDecimal ymax = ymin + yStep

      BigDecimal normalized = maxCount == 0 ? 0 : (count as BigDecimal) / maxCount
      int levelIdx = (normalized * (bins - 1)).intValue()
      if (levelIdx < 0) levelIdx = 0
      if (levelIdx > bins - 1) levelIdx = bins - 1

      LayerData datum = new LayerData(
          x: xmin + xStep / 2,
          y: ymin + yStep / 2,
          xmin: xmin,
          xmax: xmax,
          ymin: ymin,
          ymax: ymax,
          fill: normalized,
          group: "level-${levelIdx}",
          rowIndex: -1
      )
      datum.meta.level = levelIdx
      datum.meta.density = normalized
      datum.meta.count = count
      result << datum
    }

    result.sort { LayerData a, LayerData b ->
      BigDecimal level1 = NumberCoercionUtil.coerceToBigDecimal(a.meta?.level) ?: 0
      BigDecimal level2 = NumberCoercionUtil.coerceToBigDecimal(b.meta?.level) ?: 0
      int levelCompare = level1 <=> level2
      if (levelCompare != 0) {
        return levelCompare
      }
      BigDecimal x1 = NumberCoercionUtil.coerceToBigDecimal(a.x) ?: 0
      BigDecimal x2 = NumberCoercionUtil.coerceToBigDecimal(b.x) ?: 0
      x1 <=> x2
    }
  }
}
