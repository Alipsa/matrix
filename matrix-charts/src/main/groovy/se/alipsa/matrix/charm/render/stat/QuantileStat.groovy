package se.alipsa.matrix.charm.render.stat

import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.stats.regression.QuantileRegression

/**
 * Quantile regression stat producing fitted lines for one or more quantiles.
 */
@SuppressWarnings('DuplicateNumberLiteral')
@SuppressWarnings('UnnecessaryCast')
class QuantileStat {

  private static final String ALL_SERIES = '__all__'

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }
    List<LayerData> result = []
    StatUtils.groupBySeries(data).each { Object seriesKey, List<LayerData> bucket ->
      result.addAll(computeGroup(layer, bucket, seriesKey))
    }
    result
  }

  private static List<LayerData> computeGroup(LayerSpec layer, List<LayerData> data, Object seriesKey) {

    List<LayerData> numeric = data.findAll { LayerData datum ->
      ValueConverter.asBigDecimal(datum.x) != null &&
          ValueConverter.asBigDecimal(datum.y) != null
    }
    if (numeric.size() < 2) {
      return []
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    List<Number> quantiles = []
    if (params.quantiles instanceof List) {
      (params.quantiles as List).each { Object value ->
        BigDecimal quantile = ValueConverter.asBigDecimal(value)
        if (quantile != null) {
          quantiles << quantile
        }
      }
    }
    if (quantiles.isEmpty()) {
      quantiles = [0.25, 0.5, 0.75] as List<Number>
    }
    int n = ValueConverter.asBigDecimal(params.n)?.intValue() ?: 80
    if (n < 1) {
      n = 80
    }

    List<BigDecimal> x = numeric.collect { ValueConverter.asBigDecimal(it.x) }
    List<BigDecimal> y = numeric.collect { ValueConverter.asBigDecimal(it.y) }
    BigDecimal xMin = x.min()
    BigDecimal xMax = x.max()
    if (xMin == xMax) {
      xMax = xMax + 1
    }

    LayerData template = numeric.first()
    List<LayerData> result = []
    quantiles.eachWithIndex { Number tauValue, int groupIndex ->
      BigDecimal tau = ValueConverter.asBigDecimal(tauValue)
      if (tau == null || tau <= 0 || tau >= 1) {
        return
      }
      QuantileRegression regression = new QuantileRegression(x, y, tau)
      for (int i = 0; i < n; i++) {
        BigDecimal xv = n == 1 ? (xMin + xMax) / 2 : xMin + (xMax - xMin) * i / (n - 1)
        BigDecimal yv = regression.predict(xv)
        Object groupKey = seriesKey == ALL_SERIES ? groupIndex : "${seriesKey}::${groupIndex}".toString()
        LayerData datum = new LayerData(
            x: xv,
            y: yv,
            color: template.color,
            fill: template.fill,
            group: groupKey,
            rowIndex: -1
        )
        datum.meta.quantile = tau
        datum.meta.series = seriesKey == ALL_SERIES ? null : seriesKey
        result << datum
      }
    }

    result
  }

}
