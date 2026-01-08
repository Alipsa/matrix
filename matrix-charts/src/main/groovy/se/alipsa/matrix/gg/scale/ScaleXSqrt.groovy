package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Square root-transformed continuous scale for the x-axis.
 * Maps values using sqrt transformation, useful for count data or to reduce right skew.
 *
 * Usage:
 * - scale_x_sqrt() - basic sqrt x-axis
 * - scale_x_sqrt(limits: [0, 100]) - with explicit limits (in data space)
 *
 * Note: Values < 0 are filtered out since sqrt is undefined for negative numbers.
 *
 * Precision behavior:
 * - Transform and inverse methods return BigDecimal for consistency with other scales
 * - Sqrt computation uses Math.sqrt (double precision) for the transformation
 * - For typical chart data, double precision sqrt is sufficient for visual display
 * - Break generation uses exact BigDecimal arithmetic in data space
 */
@CompileStatic
class ScaleXSqrt extends ScaleContinuous {

  /** Position of the x-axis: 'bottom' (default) or 'top' */
  String position = 'bottom'

  ScaleXSqrt() {
    aesthetic = 'x'
  }

  ScaleXSqrt(Map params) {
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

    // Filter to non-negative numeric values
    List<Double> numericData = data.findResults { coerceToNonNegativeNumber(it) } as List<Double>

    if (numericData.isEmpty()) return

    // Transform to sqrt space and convert to BigDecimal
    List<BigDecimal> sqrtData = numericData.collect { Math.sqrt(it) as BigDecimal }

    // Compute min/max in sqrt space as BigDecimal
    BigDecimal min = sqrtData.min()
    BigDecimal max = sqrtData.max()

    // Apply explicit limits if set (limits are in data space, convert to sqrt space)
    if (limits && limits.size() >= 2) {
      if (limits[0] != null && (limits[0] as Number) >= 0) {
        min = Math.sqrt(limits[0] as double) as BigDecimal
      }
      if (limits[1] != null && (limits[1] as Number) >= 0) {
        max = Math.sqrt(limits[1] as double) as BigDecimal
      }
    }

    // Apply expansion in sqrt space using BigDecimal arithmetic
    if (expand != null && expand.size() >= 2) {
      BigDecimal mult = (expand[0] != null) ? expand[0] as BigDecimal : DEFAULT_EXPAND_MULT
      BigDecimal add = (expand[1] != null) ? expand[1] as BigDecimal : DEFAULT_EXPAND_ADD
      BigDecimal delta = max - min
      min = (min - delta * mult - add).max(BigDecimal.ZERO)  // Can't go negative
      max = max + delta * mult + add
    }

    computedDomain = [min, max]
    trained = true
  }

  @Override
  Object transform(Object value) {
    BigDecimal numeric = coerceToNonNegativeNumber(value)
    if (numeric == null) return null

    // Transform to sqrt space, then perform linear interpolation
    BigDecimal sqrtValue = Math.sqrt(numeric as double) as BigDecimal
    return ScaleUtils.linearTransform(sqrtValue, computedDomain[0], computedDomain[1], range[0], range[1])
  }

  @Override
  Object inverse(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    if (v == null) return null

    // Inverse linear interpolation in sqrt space
    BigDecimal sqrtValue = ScaleUtils.linearInverse(v, computedDomain[0], computedDomain[1], range[0], range[1])
    if (sqrtValue == null) return null

    // Transform back from sqrt space (square the value)
    return sqrtValue * sqrtValue
  }

  @Override
  List getComputedBreaks() {
    if (breaks) return breaks

    BigDecimal sqrtMin = computedDomain[0]
    BigDecimal sqrtMax = computedDomain[1]

    // Generate nice breaks in data space
    BigDecimal dataMin = sqrtMin * sqrtMin
    BigDecimal dataMax = sqrtMax * sqrtMax

    return generateNiceDataBreaks(dataMin, dataMax, nBreaks)
  }

  /**
   * Generate nice breaks in data space.
   */
  private static List<Number> generateNiceDataBreaks(BigDecimal min, BigDecimal max, int n) {
    if (max == min) return [min] as List<Number>

    BigDecimal rawRange = max - min
    BigDecimal spacing = ScaleUtils.niceNum(rawRange / (n - 1), true)

    BigDecimal niceMin = (min / spacing).floor() * spacing
    BigDecimal niceMax = (max / spacing).ceil() * spacing

    // Ensure we start at 0 or positive value for sqrt
    niceMin = niceMin.max(BigDecimal.ZERO)

    List<Number> breaks = []
    BigDecimal tolerance = spacing * BREAK_TOLERANCE_RATIO
    BigDecimal minBd = min
    BigDecimal maxBd = max
    for (BigDecimal val = niceMin; val <= niceMax + tolerance; val += spacing) {
      if (val >= minBd - tolerance && val <= maxBd + tolerance && val >= BigDecimal.ZERO) {
        breaks << val
      }
    }
    return breaks
  }

  private static BigDecimal coerceToNonNegativeNumber(Object value) {
    BigDecimal num = ScaleUtils.coerceToNumber(value)
    if (num == null) return null
    if (num < 0) return null
    return num
  }
}
