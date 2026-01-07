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
 *
 * Precision behavior:
 * - Transform and inverse methods return BigDecimal for consistency with other scales
 * - Log10 computation uses Math.log10 (double precision) for the transformation
 * - For typical chart data (values between 1e-10 and 1e10), double precision is sufficient
 * - Break generation produces exact BigDecimal values (powers of 10: 1, 10, 100, etc.)
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
    if (params.expand) this.expand = params.expand as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.position) this.position = params.position as String
    if (params.nBreaks) this.nBreaks = params.nBreaks as int
  }

  @Override
  void train(List data) {
    if (data == null || data.isEmpty()) return

    // Filter to positive numeric values (log10 is undefined for <= 0)
    List<BigDecimal> numericData = data.findResults { coerceToPositiveNumber(it) } as List<BigDecimal>

    if (numericData.isEmpty()) return

    // Transform to log space and convert to BigDecimal
    List<BigDecimal> logData = numericData.collect { it.log10() }

    // Compute min/max in log space as BigDecimal
    BigDecimal min = logData.min()
    BigDecimal max = logData.max()

    // Apply explicit limits if set (limits are in data space, convert to log space)
    if (limits && limits.size() >= 2) {
      if (limits[0] != null && (limits[0] as Number) > 0) {
        min = limits[0].log10()
      }
      if (limits[1] != null && (limits[1] as Number) > 0) {
        max = limits[1].log10()
      }
    }

    // Apply expansion in log space using BigDecimal arithmetic
    if (expand != null && expand.size() >= 2) {
      BigDecimal mult = (expand[0] != null) ? expand[0] as BigDecimal : DEFAULT_EXPAND_MULT
      BigDecimal add = (expand[1] != null) ? expand[1] as BigDecimal : DEFAULT_EXPAND_ADD
      BigDecimal delta = max - min
      min = min - delta * mult - add
      max = max + delta * mult + add
    }

    computedDomain = [min, max]
    trained = true
  }

  @Override
  Object transform(Object value) {
    BigDecimal numeric = coerceToPositiveNumber(value)
    if (numeric == null) return null

    // Transform to log space, then perform linear interpolation
    BigDecimal logValue = numeric.log10()
    return ScaleUtils.linearTransform(logValue, computedDomain[0], computedDomain[1], range[0], range[1])
  }

  @Override
  Object inverse(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    if (v == null) return null

    // Inverse linear interpolation in log space
    BigDecimal logValue = ScaleUtils.linearInverse(v, computedDomain[0], computedDomain[1], range[0], range[1])
    if (logValue == null) return null

    // Transform back from log space using Groovy power operator
    return (10 ** logValue) as BigDecimal
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
  private static List<Number> generateLogBreaks(BigDecimal logMin, BigDecimal logMax) {
    List<Number> breaks = []

    int minPow = logMin.floor().intValue()
    int maxPow = logMax.ceil().intValue()

    // Generate breaks at powers of 10
    for (int pow = minPow; pow <= maxPow; pow++) {
      BigDecimal val = 10 ** pow
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
          BigDecimal val = mult * (10 ** pow)
          BigDecimal logVal = val.log10()
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

  /**
   * Format a number for display on log scale axis.
   * Handles powers of 10 and intermediate values, formatting appropriately.
   *
   * Note: Log scale breaks are returned as BigDecimal (powers of 10 in data space),
   * but formatting uses double precision for display. This is acceptable because:
   * - Log scale breaks are typically clean powers of 10 (1, 10, 100, etc.)
   * - Intermediate values (2, 5 multiples) are representable in double precision
   * - Any precision loss is insignificant for axis label display purposes
   *
   * @param n the number to format (typically BigDecimal from break generation)
   * @return formatted string representation
   */
  private String formatLogNumber(Number n) {
    if (n == null) return ''

    // Convert to BigDecimal for consistent processing
    BigDecimal bd = n instanceof BigDecimal ? n as BigDecimal : new BigDecimal(n.toString())

    // Check if it's an integer value (after removing trailing zeros)
    if (bd.stripTrailingZeros().scale() <= 0) {
      // Format as integer
      return bd.toBigInteger().toString()
    }

    // For fractional values (< 1), use general format
    if (bd < BigDecimal.ONE) {
      return String.format('%.2g', bd.doubleValue())
    }

    // For large non-integer values, format without decimals
    return String.format('%.0f', bd.doubleValue())
  }

  private static BigDecimal coerceToPositiveNumber(Object value) {
    BigDecimal num = ScaleUtils.coerceToNumber(value)
    if (num == null) return null
    if (num <= 0) return null
    return num
  }
}
