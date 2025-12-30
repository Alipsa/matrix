package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Continuous scale for numeric data.
 * Maps numeric values from a data domain to a range (e.g., pixel positions).
 */
@CompileStatic
class ScaleContinuous extends Scale {

  /** Output range [min, max] */
  List<Number> range = [0, 1] as List<Number>

  /** Domain computed from data [min, max] */
  private List<Number> computedDomain = [0, 1] as List<Number>

  /** Number of breaks to generate */
  int nBreaks = 5

  @Override
  void train(List data) {
    if (data == null || data.isEmpty()) return

    // Filter to numeric values
    List<Number> numericData = data.findAll { it instanceof Number } as List<Number>

    if (numericData.isEmpty()) return

    // Compute min/max
    Number min = numericData.min()
    Number max = numericData.max()

    // Apply explicit limits if set
    if (limits && limits.size() >= 2) {
      min = limits[0] != null ? limits[0] as Number : min
      max = limits[1] != null ? limits[1] as Number : max
    }

    // Apply expansion
    if (expand && expand.size() >= 2) {
      Number mult = expand[0] ?: 0.05
      Number add = expand[1] ?: 0
      Number delta = max - min
      min = min - delta * mult - add
      max = max + delta * mult + add
    }

    computedDomain = [min, max]
    trained = true
  }

  @Override
  Object transform(Object value) {
    if (value == null) return null
    if (!(value instanceof Number)) return null

    double v = value as double
    double dMin = computedDomain[0] as double
    double dMax = computedDomain[1] as double
    double rMin = range[0] as double
    double rMax = range[1] as double

    // Handle edge case
    if (dMax == dMin) return (rMin + rMax) / 2

    // Linear interpolation
    double normalized = (v - dMin) / (dMax - dMin)
    return rMin + normalized * (rMax - rMin)
  }

  @Override
  Object inverse(Object value) {
    if (value == null) return null
    if (!(value instanceof Number)) return null

    double v = value as double
    double dMin = computedDomain[0] as double
    double dMax = computedDomain[1] as double
    double rMin = range[0] as double
    double rMax = range[1] as double

    // Handle edge case
    if (rMax == rMin) return (dMin + dMax) / 2

    // Inverse linear interpolation
    double normalized = (v - rMin) / (rMax - rMin)
    return dMin + normalized * (dMax - dMin)
  }

  @Override
  List getComputedBreaks() {
    if (breaks) return breaks

    double min = computedDomain[0] as double
    double max = computedDomain[1] as double

    // Generate nice breaks
    return generateNiceBreaks(min, max, nBreaks)
  }

  @Override
  List<String> getComputedLabels() {
    if (labels) return labels
    return getComputedBreaks().collect { formatNumber(it as Number) }
  }

  /**
   * Generate nice round breaks for axis ticks.
   */
  private List<Number> generateNiceBreaks(double min, double max, int n) {
    double range = niceNum(max - min, false)
    double spacing = niceNum(range / (n - 1) as double, true)
    double niceMin = Math.floor(min / spacing as double) * spacing
    double niceMax = Math.ceil(max / spacing as double) * spacing

    List<Number> breaks = []
    for (double val = niceMin; val <= niceMax + spacing * 0.5; val += spacing) {
      if (val >= min - spacing * 0.001 && val <= max + spacing * 0.001) {
        breaks << val
      }
    }
    return breaks
  }

  /**
   * Find a "nice" number approximately equal to x.
   * Round the number if round is true, otherwise take ceiling.
   */
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

  /**
   * Format a number for display.
   */
  private String formatNumber(Number n) {
    if (n == null) return ''
    double d = n as double
    // Remove trailing zeros
    if (d == Math.floor(d) && d < 1e10) {
      return String.valueOf((long) d)
    }
    return String.format('%.2g', d)
  }
}
