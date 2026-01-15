package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Discrete scale for the x-axis position aesthetic.
 * Used for categorical x-axis data (bar charts, etc.).
 */
@CompileStatic
class ScaleXDiscrete extends ScaleDiscrete {

  /** Position of the x-axis: 'bottom' (default) or 'top' */
  String position = 'bottom'

  /** Expansion factor for adding padding at the ends */
  List<Number> discreteExpand = [0, 0.6] as List<Number>

  ScaleXDiscrete() {
    aesthetic = 'x'
  }

  ScaleXDiscrete(Map params) {
    aesthetic = 'x'
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
    if (params.guide) this.guide = params.guide
  }

  @Override
  Object transform(Object value) {
    if (value == null) return null
    if (levels.isEmpty()) return null

    int index = levels.indexOf(value)
    if (index < 0) return null

    // Map to position within range with discrete expansion
    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal rMax = range[1] as BigDecimal

    // Apply discrete expansion
    BigDecimal mult = discreteExpand[0] as BigDecimal
    BigDecimal add = discreteExpand[1] as BigDecimal
    BigDecimal totalWidth = rMax - rMin
    BigDecimal expandedPadding = totalWidth * mult + add * getBandwidthUnexpanded()
    BigDecimal adjustedMin = rMin + expandedPadding / 2
    BigDecimal adjustedMax = rMax - expandedPadding / 2

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
    return (rMax - rMin).abs() / (levels.size() + 1)  // +1 for padding
  }

  @Override
  BigDecimal getBandwidth() {
    if (levels.isEmpty()) return 0
    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal rMax = range[1] as BigDecimal

    // Apply discrete expansion
    BigDecimal mult = discreteExpand[0] as BigDecimal
    BigDecimal add = discreteExpand[1] as BigDecimal
    BigDecimal totalWidth = rMax - rMin
    BigDecimal expandedPadding = totalWidth * mult + add * getBandwidthUnexpanded()
    BigDecimal adjustedWidth = totalWidth - expandedPadding

    return adjustedWidth / levels.size()
  }

  /**
   * Convenience method to set limits (subset of levels).
   */
  ScaleXDiscrete limits(List limits) {
    this.limits = limits
    return this
  }

  /**
   * Convenience method to set labels.
   */
  ScaleXDiscrete labels(List<String> labels) {
    this.labels = labels
    return this
  }

  /**
   * Convenience method to set expansion.
   */
  ScaleXDiscrete expand(Number mult, Number add = 0.6) {
    this.discreteExpand = [mult, add]
    return this
  }
}
