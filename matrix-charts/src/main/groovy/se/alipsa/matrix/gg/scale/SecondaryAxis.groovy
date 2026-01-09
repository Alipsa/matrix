package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Specification for a secondary axis.
 * Used with continuous position scales to create derived secondary axes.
 * <p>
 * Secondary axes must have a one-to-one transformation of the primary axis.
 * In ggplot2-style usage:
 * <pre>
 *   scale_y_continuous(sec.axis: sec_axis({ it * 1.8 + 32 }, name: "Fahrenheit"))
 * </pre>
 */
@CompileStatic
class SecondaryAxis {

  /** Transformation function applied to primary axis values */
  Closure<BigDecimal> transform

  /** Name for the secondary axis (optional) */
  String name

  /** Explicit breaks for the secondary axis (optional) */
  List<BigDecimal> breaks

  /** Labels for the breaks (optional) */
  List<String> labels

  /** Guide specification for this axis (optional) */
  def guide

  /**
   * Create a secondary axis specification.
   *
   * @param params Map with optional keys: transform, name, breaks, labels, guide
   */
  SecondaryAxis(Map params = [:]) {
    if (params.transform) {
      this.transform = params.transform as Closure<BigDecimal>
    }
    if (params.name) {
      this.name = params.name as String
    }
    if (params.breaks) {
      this.breaks = (params.breaks as List).collect { it as BigDecimal }
    }
    if (params.labels) {
      this.labels = params.labels as List<String>
    }
    if (params.guide) {
      this.guide = params.guide
    }
  }

  /**
   * Create a secondary axis with a transformation closure.
   *
   * @param transform Closure that transforms primary axis values to secondary axis values
   * @param params Additional parameters (name, breaks, labels, guide)
   */
  SecondaryAxis(Closure<BigDecimal> transform, Map params = [:]) {
    this.transform = transform
    if (params.name) {
      this.name = params.name as String
    }
    if (params.breaks) {
      this.breaks = (params.breaks as List).collect { it as BigDecimal }
    }
    if (params.labels) {
      this.labels = params.labels as List<String>
    }
    if (params.guide) {
      this.guide = params.guide
    }
  }

  /**
   * Apply the transformation to a value.
   *
   * @param value Value from the primary axis
   * @return Transformed value for the secondary axis, or null if value is null
   */
  BigDecimal applyTransform(Number value) {
    if (value == null) {
      return null
    }
    if (transform == null) {
      return value as BigDecimal
    }
    BigDecimal bdValue = value as BigDecimal
    return transform.call(bdValue)
  }
}
