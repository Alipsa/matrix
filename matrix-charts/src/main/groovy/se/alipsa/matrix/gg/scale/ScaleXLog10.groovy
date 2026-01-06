package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Log10-transformed continuous scale for the x-axis.
 * Maps values using log10 transformation, useful for data spanning multiple orders of magnitude.
 *
 * Usage:
 * - scale_x_log10() - basic log10 x-axis
 * - scale_x_log10(limits: [1, 1000]) - with explicit limits (in data space)
 *
 * Note: Values <= 0 are filtered out since log10 is undefined for non-positive numbers.
 */
@CompileStatic
class ScaleXLog10 extends ScaleContinuous {

  /** Position of the x-axis: 'bottom' (default) or 'top' */
  String position = 'bottom'

  ScaleXLog10() {
    aesthetic = 'x'
  }

  ScaleXLog10(Map params) {
    aesthetic = 'x'
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.expand) this.expand = params.expand as List<Number>
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.position) this.position = params.position as String
    if (params.nBreaks) this.nBreaks = params.nBreaks as int
  }

  @Override
  void train(List data) {
    if (data == null || data.isEmpty()) return

    // Filter to positive numeric values (log10 is undefined for <= 0)
    List<Double> numericData = data.findResults { coerceToPositiveNumber(it) } as List<Double>

    if (numericData.isEmpty()) return

    // Transform to log space and convert to BigDecimal
    List<BigDecimal> logData = numericData.collect { Math.log10(it) as BigDecimal }

    // Compute min/max in log space as BigDecimal
    BigDecimal min = logData.min()
    BigDecimal max = logData.max()

    // Apply explicit limits if set (limits are in data space, convert to log space)
    if (limits && limits.size() >= 2) {
      if (limits[0] != null && (limits[0] as Number) > 0) {
        min = Math.log10(limits[0] as double) as BigDecimal
      }
      if (limits[1] != null && (limits[1] as Number) > 0) {
        max = Math.log10(limits[1] as double) as BigDecimal
      }
    }

    // Apply expansion in log space using BigDecimal arithmetic
    if (expand != null && expand.size() >= 2) {
      BigDecimal mult = expand[0] != null ? expand[0] as BigDecimal : DEFAULT_EXPAND_MULT
      BigDecimal add = expand[1] != null ? expand[1] as BigDecimal : DEFAULT_EXPAND_ADD
      BigDecimal delta = max - min
      min = min - delta * mult - add
      max = max + delta * mult + add
    }

    computedDomain = [min, max]
    trained = true
  }

  @Override
  Object transform(Object value) {
    Double numeric = coerceToPositiveNumber(value)
    if (numeric == null) return null

    // Transform to log space
    double logValue = Math.log10(numeric)

    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]
    BigDecimal rMin = range[0]
    BigDecimal rMax = range[1]

    if (dMax == dMin) return (rMin + rMax).divide(ScaleUtils.TWO, ScaleUtils.MATH_CONTEXT)

    // Linear interpolation in log space
    BigDecimal logVal = logValue as BigDecimal
    BigDecimal normalized = (logVal - dMin).divide((dMax - dMin), ScaleUtils.MATH_CONTEXT)
    return rMin + normalized * (rMax - rMin)
  }

  @Override
  Object inverse(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    if (v == null) return null

    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]
    BigDecimal rMin = range[0]
    BigDecimal rMax = range[1]

    if (rMax == rMin) {
      double midLog = (dMin + dMax).divide(ScaleUtils.TWO, ScaleUtils.MATH_CONTEXT).doubleValue()
      return Math.pow(10, midLog) as BigDecimal
    }

    // Inverse linear interpolation to log space
    BigDecimal normalized = (v - rMin).divide((rMax - rMin), ScaleUtils.MATH_CONTEXT)
    BigDecimal logValue = dMin + normalized * (dMax - dMin)

    // Transform back from log space (returns BigDecimal)
    return Math.pow(10, logValue.doubleValue()) as BigDecimal
  }

  @Override
  List getComputedBreaks() {
    if (breaks) return breaks

    BigDecimal logMin = computedDomain[0]
    BigDecimal logMax = computedDomain[1]

    // Generate nice breaks in log space (powers of 10)
    return generateLogBreaks(logMin, logMax)
  }

  /**
   * Generate nice breaks for log scale (powers of 10 and intermediates).
   */
  private List<Number> generateLogBreaks(BigDecimal logMin, BigDecimal logMax) {
    List<Number> breaks = []

    int minPow = logMin.floor().intValue()
    int maxPow = logMax.ceil().intValue()

    // Generate breaks at powers of 10
    for (int pow = minPow; pow <= maxPow; pow++) {
      BigDecimal val = Math.pow(10, pow) as BigDecimal
      BigDecimal logVal = pow as BigDecimal
      if (logVal >= logMin && logVal <= logMax) {
        breaks << val
      }
    }

    // If too few breaks, add intermediate values (2, 5 multiples)
    if (breaks.size() < 3) {
      List<Number> intermediates = []
      for (int pow = minPow; pow <= maxPow; pow++) {
        for (BigDecimal mult : [2G, 5G]) {
          BigDecimal val = mult * (Math.pow(10, pow) as BigDecimal)
          BigDecimal logVal = Math.log10(val.doubleValue()) as BigDecimal
          if (logVal >= logMin && logVal <= logMax) {
            intermediates << val
          }
        }
      }
      breaks.addAll(intermediates)
      breaks = breaks.sort() as List<Number>
    }

    return breaks
  }

  @Override
  List<String> getComputedLabels() {
    if (labels) return labels
    return getComputedBreaks().collect { formatLogNumber(it as Number) }
  }

  private String formatLogNumber(Number n) {
    if (n == null) return ''
    double d = n as double
    if (d >= 1 && d == Math.floor(d) && d < 1e10) {
      return String.valueOf((long) d)
    }
    if (d < 1 && d > 0) {
      return String.format('%.2g', d)
    }
    return String.format('%.0f', d)
  }

  private static Double coerceToPositiveNumber(Object value) {
    BigDecimal num = ScaleUtils.coerceToNumber(value)
    if (num == null) return null
    double dv = num.doubleValue()
    if (dv <= 0) return null
    return dv
  }
}
