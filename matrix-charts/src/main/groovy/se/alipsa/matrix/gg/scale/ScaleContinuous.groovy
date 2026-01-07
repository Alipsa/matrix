package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Continuous scale for numeric data.
 * Maps numeric values from a data domain to a range (e.g., pixel positions).
 */
@CompileStatic
class ScaleContinuous extends Scale {

  static final BigDecimal DEFAULT_EXPAND_MULT = 0.05G
  static final BigDecimal DEFAULT_EXPAND_ADD = 0G
  static final double BREAK_TOLERANCE_RATIO = 0.001d  // Small epsilon for float comparisons.

  /** Output range [min, max] as BigDecimal values. */
  List<BigDecimal> range = [BigDecimal.ZERO, BigDecimal.ONE]

  /** Domain computed from data [min, max] as BigDecimal values. */
  protected List<BigDecimal> computedDomain = [BigDecimal.ZERO, BigDecimal.ONE]

  /** Number of breaks to generate (default 7; can be adjusted per scale). */
  int nBreaks = 7

  @Override
  void train(List data) {
    if (data == null || data.isEmpty()) return

    // Filter to numeric values (including numeric strings)
    List<BigDecimal> numericData = data.findResults { ScaleUtils.coerceToNumber(it) } as List<BigDecimal>

    if (numericData.isEmpty()) return

    // Compute min/max
    BigDecimal min = numericData.min()
    BigDecimal max = numericData.max()

    // Apply explicit limits if set
    if (limits && limits.size() >= 2) {
      BigDecimal limitMin = ScaleUtils.coerceToNumber(limits[0])
      BigDecimal limitMax = ScaleUtils.coerceToNumber(limits[1])
      if (limitMin != null) min = limitMin
      if (limitMax != null) max = limitMax
    }

    // Apply expansion only when configured (ggplot2 default is mult=0.05, add=0).
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

  /**
   * Transform a data value to the output range.
   *
   * Note: Return type is Object (not BigDecimal) for API compatibility with subclasses.
   * ScaleContinuous serves as the base class for diverse scale types:
   * - Numeric scales (this class, ScaleXLog10, ScaleSizeContinuous, etc.) return BigDecimal
   * - Color scales (ScaleColorGradient, ScaleColorViridisC, etc.) return String
   * - Date scales (ScaleXDate, ScaleXDatetime) may return other types
   *
   * The implementation is type-safe internally (uses BigDecimal arithmetic), but the
   * return type must remain Object to support covariant return types in all subclasses.
   *
   * @param value the data value to transform
   * @return the transformed value in the output range (BigDecimal for numeric scales, null if value is invalid)
   */
  @Override
  Object transform(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    return ScaleUtils.linearTransform(v, computedDomain[0], computedDomain[1], range[0], range[1])
  }

  /**
   * Inverse transform from output range back to data space.
   *
   * Note: Return type is Object (not BigDecimal) for API compatibility with subclasses.
   * See {@link #transform(Object)} for explanation of why Object is used instead of BigDecimal.
   *
   * @param value the output value to inverse transform
   * @return the data value (BigDecimal for numeric scales, null if value is invalid)
   */
  @Override
  Object inverse(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    return ScaleUtils.linearInverse(v, computedDomain[0], computedDomain[1], range[0], range[1])
  }

  @Override
  List getComputedBreaks() {
    if (breaks) return breaks

    BigDecimal min = computedDomain[0]
    BigDecimal max = computedDomain[1]

    // Generate nice breaks (uses double internally for "nice number" algorithm)
    return generateNiceBreaks(min.doubleValue(), max.doubleValue(), nBreaks)
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
    BigDecimal rawRange = (max - min) as BigDecimal
    BigDecimal spacing = ScaleUtils.niceNum(rawRange / (n - 1), true)

    // Calculate nice min/max that are multiples of spacing
    BigDecimal niceMin = ((min as BigDecimal) / spacing).floor() * spacing
    BigDecimal niceMax = ((max as BigDecimal) / spacing).ceil() * spacing

    List<Number> breaks = []
    // Generate breaks from niceMin to niceMax
    BigDecimal tolerance = spacing * BREAK_TOLERANCE_RATIO
    BigDecimal minBd = min as BigDecimal
    BigDecimal maxBd = max as BigDecimal
    for (BigDecimal val = niceMin; val <= niceMax + tolerance; val += spacing) {
      // Include all breaks within the expanded domain (with small tolerance)
      if (val >= minBd - tolerance && val <= maxBd + tolerance) {
        breaks << val
      }
    }
    return breaks
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
   *
   * @return a list containing the domain minimum and maximum as BigDecimal values
   */
  List<BigDecimal> getComputedDomain() {
    return computedDomain
  }
}
