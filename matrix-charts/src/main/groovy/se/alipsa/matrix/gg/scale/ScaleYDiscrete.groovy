package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Discrete scale for the y-axis position aesthetic.
 * Used for categorical y-axis data (horizontal bar charts, etc.).
 * Note: Y-axis range is typically inverted in SVG coordinates (0 at top).
 */
@CompileStatic
class ScaleYDiscrete extends ScaleDiscrete {

  /** Position of the y-axis: 'left' (default) or 'right' */
  String position = 'left'

  /** Expansion factor for adding padding at the ends */
  List<Number> discreteExpand = [0, 0.6] as List<Number>

  ScaleYDiscrete() {
    aesthetic = 'y'
  }

  ScaleYDiscrete(Map params) {
    aesthetic = 'y'
    applyParams(params)
  }

  private void applyParams(Map params) {
    this.name = params.name as String ?: this.name
    this.limits = params.limits as List ?: this.limits
    this.breaks = params.breaks as List ?: this.breaks
    this.labels = params.labels as List<String> ?: this.labels
    this.position = params.position as String ?: this.position
    if (params.drop != null) this.drop = params.drop as boolean
    this.discreteExpand = params.expand as List<Number> ?: this.discreteExpand
  }

  @Override
  Object transform(Object value) {
    if (value == null) return null
    if (levels.isEmpty()) return null

    int index = levels.indexOf(value)
    if (index < 0) return null

    // Map to position within range with discrete expansion
    // Note: For y-axis, range[0] is typically the bottom (larger pixel value)
    // and range[1] is the top (0 or smaller pixel value) due to SVG inversion
    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal rMax = range[1] as BigDecimal

    // Apply discrete expansion
    BigDecimal mult = discreteExpand[0] as BigDecimal
    BigDecimal add = discreteExpand[1] as BigDecimal
    BigDecimal totalHeight = (rMax - rMin).abs()
    BigDecimal expandedPadding = totalHeight * mult + add * getBandwidthUnexpanded()

    // Adjust bounds (accounting for SVG y-inversion)
    BigDecimal adjustedMin, adjustedMax
    if (rMin > rMax) {
      // Typical SVG case: rMin = plotHeight, rMax = 0
      adjustedMin = rMin - expandedPadding / 2
      adjustedMax = rMax + expandedPadding / 2
    } else {
      adjustedMin = rMin + expandedPadding / 2
      adjustedMax = rMax - expandedPadding / 2
    }

    if (levels.size() == 1) {
      return (adjustedMin + adjustedMax) / 2
    }

    BigDecimal bandWidth = (adjustedMax - adjustedMin) / levels.size()
    return adjustedMin + bandWidth * (index + 0.5)
  }

  /**
   * Get bandwidth without expansion applied.
   */
  private BigDecimal getBandwidthUnexpanded() {
    if (levels.isEmpty()) return 0
    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal rMax = range[1] as BigDecimal
    return (rMax - rMin).abs() / (levels.size() + 1)
  }

  @Override
  BigDecimal getBandwidth() {
    if (levels.isEmpty()) return 0
    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal rMax = range[1] as BigDecimal

    // Apply discrete expansion
    BigDecimal mult = discreteExpand[0] as BigDecimal
    BigDecimal add = discreteExpand[1] as BigDecimal
    BigDecimal totalHeight = (rMax - rMin).abs()
    BigDecimal expandedPadding = totalHeight * mult + add * getBandwidthUnexpanded()
    BigDecimal adjustedHeight = totalHeight - expandedPadding

    return adjustedHeight / levels.size()
  }

  /**
   * Convenience method to set limits (subset of levels).
   */
  ScaleYDiscrete limits(List limits) {
    this.limits = limits
    return this
  }

  /**
   * Convenience method to set labels.
   */
  ScaleYDiscrete labels(List<String> labels) {
    this.labels = labels
    return this
  }

  /**
   * Convenience method to set expansion.
   */
  ScaleYDiscrete expand(Number mult, Number add = 0.6) {
    this.discreteExpand = [mult, add]
    return this
  }
}
