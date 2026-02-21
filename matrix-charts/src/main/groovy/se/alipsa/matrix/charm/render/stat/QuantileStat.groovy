package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.util.NumberCoercionUtil
import se.alipsa.matrix.stats.regression.QuantileRegression

/**
 * Quantile regression stat producing fitted lines for one or more quantiles.
 */
@CompileStatic
class QuantileStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    List<LayerData> numeric = data.findAll { LayerData datum ->
      NumberCoercionUtil.coerceToBigDecimal(datum.x) != null &&
          NumberCoercionUtil.coerceToBigDecimal(datum.y) != null
    }
    if (numeric.size() < 2) {
      return []
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    List<Number> quantiles = []
    if (params.quantiles instanceof List) {
      (params.quantiles as List).each { Object value ->
        BigDecimal quantile = NumberCoercionUtil.coerceToBigDecimal(value)
        if (quantile != null) {
          quantiles << quantile
        }
      }
    }
    if (quantiles.isEmpty()) {
      quantiles = [0.25, 0.5, 0.75] as List<Number>
    }
    int n = NumberCoercionUtil.coerceToBigDecimal(params.n)?.intValue() ?: 80
    if (n < 2) {
      n = 80
    }

    List<BigDecimal> x = numeric.collect { NumberCoercionUtil.coerceToBigDecimal(it.x) }
    List<BigDecimal> y = numeric.collect { NumberCoercionUtil.coerceToBigDecimal(it.y) }
    BigDecimal xMin = x.min()
    BigDecimal xMax = x.max()
    if (xMin == xMax) {
      xMax = xMax + 1
    }

    List<LayerData> result = []
    quantiles.eachWithIndex { Number tauValue, int groupIndex ->
      BigDecimal tau = NumberCoercionUtil.coerceToBigDecimal(tauValue)
      if (tau == null || tau <= 0 || tau >= 1) {
        return
      }
      QuantileRegression regression = new QuantileRegression(x, y, tau)
      for (int i = 0; i < n; i++) {
        BigDecimal xv = xMin + (xMax - xMin) * i / (n - 1)
        BigDecimal yv = regression.predict(xv)
        LayerData datum = new LayerData(
            x: xv,
            y: yv,
            group: groupIndex,
            rowIndex: -1
        )
        datum.meta.quantile = tau
        result << datum
      }
    }

    result
  }
}
