package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Shared helpers for Charm stat computations.
 */
@CompileStatic
class StatUtils {

  private StatUtils() {
    // Utility class
  }

  static Map<Object, List<LayerData>> groupBySeries(List<LayerData> data) {
    Map<Object, List<LayerData>> groups = new LinkedHashMap<>()
    data.each { LayerData datum ->
      Object key = seriesKey(datum)
      List<LayerData> bucket = groups[key]
      if (bucket == null) {
        bucket = []
        groups[key] = bucket
      }
      bucket << datum
    }
    groups
  }

  static Object seriesKey(LayerData datum) {
    if (datum.group != null) {
      return datum.group
    }
    if (datum.color != null) {
      return datum.color
    }
    if (datum.fill != null) {
      return datum.fill
    }
    '__all__'
  }

  static List<BigDecimal> sortedNumericValues(List<LayerData> data, Closure<Object> selector) {
    List<BigDecimal> values = data.collect { LayerData datum ->
      NumberCoercionUtil.coerceToBigDecimal(selector.call(datum))
    }.findAll { it != null } as List<BigDecimal>
    values.sort()
    values
  }

  static BigDecimal quantileType7(List<BigDecimal> sorted, BigDecimal p) {
    BoxplotStat.quantileType7(sorted, p)
  }

  /**
   * Approximate inverse CDF of standard normal distribution.
   */
  static BigDecimal normalQuantile(BigDecimal p) {
    if (p == null) {
      return null
    }
    BigDecimal probValue = p
    if (probValue <= 0) {
      probValue = 1.0E-16
    } else if (probValue >= 1) {
      probValue = 1 - 1.0E-16
    }

    // Coefficients from Peter J. Acklam's inverse normal approximation.
    double[] a = [-3.969683028665376e+01, 2.209460984245205e+02,
                  -2.759285104469687e+02, 1.383577518672690e+02,
                  -3.066479806614716e+01, 2.506628277459239e+00] as double[]
    double[] b = [-5.447609879822406e+01, 1.615858368580409e+02,
                  -1.556989798598866e+02, 6.680131188771972e+01,
                  -1.328068155288572e+01] as double[]
    double[] c = [-7.784894002430293e-03, -3.223964580411365e-01,
                  -2.400758277161838e+00, -2.549732539343734e+00,
                  4.374664141464968e+00, 2.938163982698783e+00] as double[]
    double[] d = [7.784695709041462e-03, 3.224671290700398e-01,
                  2.445134137142996e+00, 3.754408661907416e+00] as double[]

    double prob = probValue as double
    double plow = 0.02425d
    double phigh = 1 - plow
    double q
    double r
    double value

    if (prob < plow) {
      q = Math.sqrt(-2 * Math.log(prob))
      value = (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
          ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1)
    } else if (prob > phigh) {
      q = Math.sqrt(-2 * Math.log(1 - prob))
      value = -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
          ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1)
    } else {
      q = prob - 0.5d
      r = q * q
      value = (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q /
          (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1)
    }

    value as BigDecimal
  }
}
