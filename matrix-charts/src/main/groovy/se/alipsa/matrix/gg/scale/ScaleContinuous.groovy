package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Continuous scale for numeric data.
 * Maps numeric values from a data domain to a range (e.g., pixel positions).
 */
@CompileStatic
class ScaleContinuous extends Scale {

  static final double DEFAULT_EXPAND_MULT = 0.05d
  static final double DEFAULT_EXPAND_ADD = 0.0d
  static final double BREAK_TOLERANCE_RATIO = 0.001d  // Small epsilon for float comparisons.

  /** Output range [min, max] */
  List<Number> range = [0, 1] as List<Number>

  /** Domain computed from data [min, max] */
  protected List<Number> computedDomain = [0, 1] as List<Number>

  /** Number of breaks to generate (default 7; can be adjusted per scale). */
  int nBreaks = 7

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

    // Apply expansion only when configured (ggplot2 default is mult=0.05, add=0).
    if (expand != null && expand.size() >= 2) {
      Number mult = expand[0] != null ? expand[0] : DEFAULT_EXPAND_MULT
      Number add = expand[1] != null ? expand[1] : DEFAULT_EXPAND_ADD
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
   * Based on Wilkinson's algorithm for nice axis labels.
   */
  private List<Number> generateNiceBreaks(double min, double max, int n) {
    if (max == min) return [min] as List<Number>

    // Calculate nice spacing directly from the data range
    double rawRange = max - min
    double spacing = niceNum(rawRange / (n - 1) as double, true)

    // Calculate nice min/max that are multiples of spacing
    double niceMin = Math.floor(min / spacing as double) * spacing
    double niceMax = Math.ceil(max / spacing as double) * spacing

    List<Number> breaks = []
    // Generate breaks from niceMin to niceMax
    double tolerance = spacing * BREAK_TOLERANCE_RATIO
    for (double val = niceMin; val <= niceMax + tolerance; val += spacing) {
      // Include all breaks within the expanded domain (with small tolerance)
      if (val >= min - tolerance && val <= max + tolerance) {
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

  /**
   * Get the computed domain [min, max].
   */
  List<Number> getComputedDomain() {
    return computedDomain
  }
}
