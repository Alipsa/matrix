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
    List<Double> sqrtData = numericData.collect { Math.sqrt(it) } as List<Double>

    // Compute min/max in sqrt space
    double min = sqrtData.min() as double
    double max = sqrtData.max() as double

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
      Number mult = expand[0] != null ? expand[0] : DEFAULT_EXPAND_MULT
      Number add = expand[1] != null ? expand[1] : DEFAULT_EXPAND_ADD
      Number delta = max - min
      min = Math.max(0, min - delta * mult - add)  // Can't go negative
      max = max + delta * mult + add
    }

    computedDomain = [min, max]
    trained = true
  }

  @Override
  Object transform(Object value) {
    Double numeric = coerceToNonNegativeNumber(value)
    if (numeric == null) return null

    // Transform to sqrt space
    double sqrtValue = Math.sqrt(numeric)

    double dMin = computedDomain[0] as double
    double dMax = computedDomain[1] as double
    double rMin = range[0] as double
    double rMax = range[1] as double

    if (dMax == dMin) return (rMin + rMax) / 2

    // Linear interpolation in sqrt space
    double normalized = (sqrtValue - dMin) / (dMax - dMin)
    return rMin + normalized * (rMax - rMin)
  }

  @Override
  Object inverse(Object value) {
    Double numeric = coerceToNumber(value)
    if (numeric == null) return null

    double v = numeric
    double dMin = computedDomain[0] as double
    double dMax = computedDomain[1] as double
    double rMin = range[0] as double
    double rMax = range[1] as double

    if (rMax == rMin) return Math.pow((dMin + dMax) / 2, 2)

    // Inverse linear interpolation to sqrt space
    double normalized = (v - rMin) / (rMax - rMin)
    double sqrtValue = dMin + normalized * (dMax - dMin)

    // Transform back from sqrt space
    return sqrtValue * sqrtValue
  }

  @Override
  List getComputedBreaks() {
    if (breaks) return breaks

    // Generate nice breaks in data space, then transform
    double sqrtMin = computedDomain[0] as double
    double sqrtMax = computedDomain[1] as double
    double dataMin = sqrtMin * sqrtMin
    double dataMax = sqrtMax * sqrtMax

    return generateNiceDataBreaks(dataMin, dataMax, nBreaks)
  }

  /**
   * Generate nice breaks in data space.
   */
  private List<Number> generateNiceDataBreaks(double min, double max, int n) {
    if (max == min) return [min] as List<Number>

    double rawRange = max - min
    double spacing = niceNum(rawRange / (n - 1) as double, true)

    double niceMin = Math.floor(min / spacing as double) * spacing
    double niceMax = Math.ceil(max / spacing as double) * spacing

    // Ensure we start at 0 or positive value for sqrt
    niceMin = Math.max(0, niceMin)

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
    Double num = coerceToNumber(value)
    if (num == null || num < 0) return null
    return num
  }

  private static Double coerceToNumber(Object value) {
    if (value == null) return null
    if (value instanceof Number) {
      double v = (value as Number).doubleValue()
      return Double.isNaN(v) ? null : v
    }
    if (value instanceof CharSequence) {
      String s = value.toString().trim()
      if (s.isEmpty() || s.equalsIgnoreCase('NA') || s.equalsIgnoreCase('NaN')) {
        return null
      }
      try {
        return Double.parseDouble(s)
      } catch (NumberFormatException ignored) {
        return null
      }
    }
    return null
  }
}
