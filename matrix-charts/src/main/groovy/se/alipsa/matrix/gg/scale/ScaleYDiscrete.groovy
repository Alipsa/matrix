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
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.position) this.position = params.position as String
    if (params.drop != null) this.drop = params.drop as boolean
    if (params.expand) this.discreteExpand = params.expand as List<Number>
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
    double rMin = range[0] as double
    double rMax = range[1] as double

    // Apply discrete expansion
    double mult = discreteExpand[0] as double
    double add = discreteExpand[1] as double
    double totalHeight = Math.abs(rMax - rMin)
    double expandedPadding = totalHeight * mult + add * getBandwidthUnexpanded()

    // Adjust bounds (accounting for SVG y-inversion)
    double adjustedMin, adjustedMax
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

    double bandWidth = (adjustedMax - adjustedMin) / levels.size()
    return adjustedMin + bandWidth * (index + 0.5)
  }

  /**
   * Get bandwidth without expansion applied.
   */
  private double getBandwidthUnexpanded() {
    if (levels.isEmpty()) return 0
    double rMin = range[0] as double
    double rMax = range[1] as double
    return Math.abs(rMax - rMin) / (levels.size() + 1)
  }

  @Override
  double getBandwidth() {
    if (levels.isEmpty()) return 0
    double rMin = range[0] as double
    double rMax = range[1] as double

    // Apply discrete expansion
    double mult = discreteExpand[0] as double
    double add = discreteExpand[1] as double
    double totalHeight = Math.abs(rMax - rMin)
    double expandedPadding = totalHeight * mult + add * getBandwidthUnexpanded()
    double adjustedHeight = totalHeight - expandedPadding

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
