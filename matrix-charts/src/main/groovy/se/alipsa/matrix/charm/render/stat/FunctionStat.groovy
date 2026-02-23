package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.ValueConverter

/**
 * Function stat evaluates y = f(x) over a range.
 */
@CompileStatic
class FunctionStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    Map<String, Object> params = StatEngine.effectiveParams(layer)
    Closure<Number> fun = params.fun as Closure<Number>
    if (fun == null) {
      return []
    }

    int n = ValueConverter.asBigDecimal(params.n)?.intValue() ?: 101
    if (n < 1) {
      n = 101
    }

    BigDecimal xmin
    BigDecimal xmax
    List xlim = params.xlim instanceof List ? params.xlim as List : null
    if (xlim != null && xlim.size() >= 2) {
      xmin = ValueConverter.asBigDecimal(xlim[0]) ?: 0
      xmax = ValueConverter.asBigDecimal(xlim[1]) ?: 1
    } else {
      List<BigDecimal> xs = StatUtils.sortedNumericValues(data ?: []) { LayerData d -> d.x }
      xmin = xs.isEmpty() ? 0 : xs.min()
      xmax = xs.isEmpty() ? 1 : xs.max()
    }
    if (xmin == xmax) {
      xmax = xmax + 1
    }

    if (n == 1) {
      BigDecimal y = ValueConverter.asBigDecimal(fun.call(xmin))
      return y == null ? [] : [new LayerData(x: xmin, y: y, rowIndex: -1)]
    }

    BigDecimal step = (xmax - xmin) / (n - 1)
    List<LayerData> result = []
    for (int i = 0; i < n; i++) {
      BigDecimal x = xmin + i * step
      BigDecimal y = ValueConverter.asBigDecimal(fun.call(x))
      if (y != null) {
        result << new LayerData(x: x, y: y, rowIndex: -1)
      }
    }
    result
  }
}
