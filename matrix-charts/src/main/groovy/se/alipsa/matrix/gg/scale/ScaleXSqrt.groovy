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
    if (params.expand) this.expand = params.expand as List<Number>
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

    // Transform to sqrt space
    List<Double> sqrtData = numericData.collect { Math.sqrt(it) }

    // Compute min/max in sqrt space
    double min = sqrtData.min()
    double max = sqrtData.max()

    // Apply explicit limits if set (limits are in data space, convert to sqrt space)
    if (limits && limits.size() >= 2) {
      if (limits[0] != null && (limits[0] as Number) >= 0) {
        min = Math.sqrt(limits[0] as double)
      }
      if (limits[1] != null && (limits[1] as Number) >= 0) {
        max = Math.sqrt(limits[1] as double)
      }
    }

    // Apply expansion in sqrt space
    if (expand != null && expand.size() >= 2) {
      BigDecimal mult = expand[0] != null ? expand[0] as BigDecimal : DEFAULT_EXPAND_MULT
      BigDecimal add = expand[1] != null ? expand[1] as BigDecimal : DEFAULT_EXPAND_ADD
      BigDecimal delta = (max - min) as BigDecimal
      BigDecimal minBd = (min as BigDecimal) - delta * mult - add
      min = minBd.max(BigDecimal.ZERO).doubleValue()  // Can't go negative
      max = (max as BigDecimal) + delta * mult + add
    }

    // Store as BigDecimal (sqrt-transformed values)
    computedDomain = [min as BigDecimal, max as BigDecimal]
    trained = true
  }

  @Override
  Object transform(Object value) {
    Double numeric = coerceToNonNegativeNumber(value)
    if (numeric == null) return null

    // Transform to sqrt space
    double sqrtValue = Math.sqrt(numeric)

    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]
    BigDecimal rMin = range[0]
    BigDecimal rMax = range[1]

    if (dMax == dMin) return (rMin + rMax).divide(ScaleUtils.TWO, ScaleUtils.MATH_CONTEXT)

    // Linear interpolation in sqrt space
    BigDecimal sqrtVal = sqrtValue as BigDecimal
    BigDecimal normalized = (sqrtVal - dMin).divide((dMax - dMin), ScaleUtils.MATH_CONTEXT)
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
      BigDecimal midSqrt = (dMin + dMax).divide(ScaleUtils.TWO, ScaleUtils.MATH_CONTEXT)
      return midSqrt * midSqrt
    }

    // Inverse linear interpolation to sqrt space
    BigDecimal normalized = (v - rMin).divide((rMax - rMin), ScaleUtils.MATH_CONTEXT)
    BigDecimal sqrtValue = dMin + normalized * (dMax - dMin)

    // Transform back from sqrt space
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

    return generateNiceDataBreaks(dataMin.doubleValue(), dataMax.doubleValue(), nBreaks)
  }

  /**
   * Generate nice breaks in data space.
   * Uses double internally for the "nice number" algorithm.
   */
  private List<Number> generateNiceDataBreaks(double min, double max, int n) {
    if (max == min) return [min] as List<Number>

    double rawRange = max - min
    double spacing = niceNum(rawRange / (n - 1), true)

    double niceMin = Math.floor(min / spacing) * spacing
    double niceMax = Math.ceil(max / spacing) * spacing

    // Ensure we start at 0 or positive value for sqrt (use Groovy max)
    niceMin = (niceMin as BigDecimal).max(BigDecimal.ZERO).doubleValue()

    List<Number> breaks = []
    double tolerance = spacing * BREAK_TOLERANCE_RATIO
    for (double val = niceMin; val <= niceMax + tolerance; val += spacing) {
      if (val >= min - tolerance && val <= max + tolerance && val >= 0) {
        breaks << val
      }
    }
    return breaks
  }

  private double niceNum(double x, boolean round) {
    if (x == 0) return 0

    double exp = Math.floor(Math.log10(Math.abs(x)))
    double f = x / Math.pow(10, exp)

    double nf
    if (round) {
      if (f < 1.5) nf = 1
      else if (f < 3) nf = 2
      else if (f < 7) nf = 5
      else nf = 10
    } else {
      if (f <= 1) nf = 1
      else if (f <= 2) nf = 2
      else if (f <= 5) nf = 5
      else nf = 10
    }

    return nf * Math.pow(10, exp)
  }

  private static Double coerceToNonNegativeNumber(Object value) {
    BigDecimal num = ScaleUtils.coerceToNumber(value)
    if (num == null) return null
    double dv = num.doubleValue()
    if (dv < 0) return null
    return dv
  }
}
