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
    double rMin = range[0] as double
    double rMax = range[1] as double

    // Apply discrete expansion
    double mult = discreteExpand[0] as double
    double add = discreteExpand[1] as double
    double totalWidth = rMax - rMin
    double expandedPadding = totalWidth * mult + add * getBandwidthUnexpanded()
    double adjustedMin = rMin + expandedPadding / 2
    double adjustedMax = rMax - expandedPadding / 2

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
    return Math.abs(rMax - rMin) / (levels.size() + 1)  // +1 for padding
  }

  @Override
  double getBandwidth() {
    if (levels.isEmpty()) return 0
    double rMin = range[0] as double
    double rMax = range[1] as double

    // Apply discrete expansion
    double mult = discreteExpand[0] as double
    double add = discreteExpand[1] as double
    double totalWidth = rMax - rMin
    double expandedPadding = totalWidth * mult + add * getBandwidthUnexpanded()
    double adjustedWidth = totalWidth - expandedPadding

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
