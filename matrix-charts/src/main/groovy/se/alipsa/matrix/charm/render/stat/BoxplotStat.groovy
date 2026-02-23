package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.ValueConverter

/**
 * Boxplot stat transformation - computes quartiles, whiskers, and outliers.
 *
 * <p>Supports params:</p>
 * <ul>
 *   <li>{@code coef} - whisker coefficient (default 1.5, multiplied by IQR)</li>
 *   <li>{@code width} - box width</li>
 * </ul>
 *
 * <p>Uses quantile type 7 (linear interpolation) matching ggplot2 behavior.</p>
 */
@CompileStatic
class BoxplotStat {

  /**
   * Groups data by x value, computes quartiles per group.
   *
   * @param layer layer specification (params: coef, width)
   * @param data layer data
   * @return one LayerData per group with y=median, meta: q1, median, q3, whiskerLow, whiskerHigh, outliers, n
   */
  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data.isEmpty()) {
      return []
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    BigDecimal coef = ValueConverter.asBigDecimal(params.coef) ?: 1.5

    Map<String, List<BigDecimal>> groups = [:]
    Map<String, List<BigDecimal>> xGroups = [:]
    Map<String, LayerData> templates = [:]
    data.each { LayerData d ->
      String key = d.group?.toString() ?: d.x?.toString() ?: ''
      BigDecimal yVal = ValueConverter.asBigDecimal(d.y)
      if (yVal != null) {
        groups.computeIfAbsent(key) { [] } << yVal
        BigDecimal xVal = ValueConverter.asBigDecimal(d.x)
        if (xVal != null) {
          xGroups.computeIfAbsent(key) { [] } << xVal
        }
        if (!templates.containsKey(key)) {
          templates[key] = d
        }
      }
    }

    BigDecimal maxSqrtN = groups.values().collect { List<BigDecimal> values ->
      Math.sqrt(values.size() as double) as BigDecimal
    }.max() ?: 1.0

    List<LayerData> result = []
    groups.each { String key, List<BigDecimal> values ->
      values.sort()
      int n = values.size()
      BigDecimal q1 = quantileType7(values, 0.25)
      BigDecimal median = quantileType7(values, 0.5)
      BigDecimal q3 = quantileType7(values, 0.75)
      BigDecimal iqr = q3 - q1
      BigDecimal lowerFence = q1 - coef * iqr
      BigDecimal upperFence = q3 + coef * iqr

      BigDecimal whiskerLow = values.find { it >= lowerFence } ?: values.first()
      BigDecimal whiskerHigh = values.reverse().find { it <= upperFence } ?: values.last()
      List<BigDecimal> outliers = values.findAll { it < lowerFence || it > upperFence }
      List<BigDecimal> xValues = xGroups[key] ?: []
      BigDecimal xmin = xValues.isEmpty() ? null : xValues.min()
      BigDecimal xmax = xValues.isEmpty() ? null : xValues.max()
      BigDecimal centerX = (xmin != null && xmax != null) ? (xmin + xmax) / 2 : null

      LayerData template = templates[key]
      LayerData datum = new LayerData(
          x: centerX ?: template.x,
          y: median,
          xmin: xmin,
          xmax: xmax,
          color: template.color,
          fill: template.fill,
          group: template.group,
          rowIndex: -1
      )
      datum.meta.q1 = q1
      datum.meta.median = median
      datum.meta.q3 = q3
      datum.meta.whiskerLow = whiskerLow
      datum.meta.whiskerHigh = whiskerHigh
      datum.meta.outliers = outliers
      datum.meta.n = n
      datum.meta.relvarwidth = maxSqrtN > 0 ? (Math.sqrt(n as double) as BigDecimal) / maxSqrtN : 1.0
      result << datum
    }
    result
  }

  /**
   * Quantile type 7 (ggplot2 compatible) linear interpolation.
   *
   * @param sorted sorted values in ascending order
   * @param p probability (0 to 1)
   * @return quantile value
   */
  static BigDecimal quantileType7(List<BigDecimal> sorted, BigDecimal p) {
    if (sorted.isEmpty()) {
      return 0
    }
    if (sorted.size() == 1) {
      return sorted.first()
    }
    int n = sorted.size()
    double h = (n - 1) * (p as double) + 1
    int j = (h as BigDecimal).floor() as int
    double g = h - j

    if (j <= 1) {
      double x1 = sorted[0] as double
      double x2 = sorted[1] as double
      return (x1 + g * (x2 - x1)) as BigDecimal
    }
    if (j >= n) {
      return sorted[n - 1]
    }

    double xj = sorted[j - 1] as double
    double xj1 = sorted[j] as double
    (xj + g * (xj1 - xj)) as BigDecimal
  }
}
