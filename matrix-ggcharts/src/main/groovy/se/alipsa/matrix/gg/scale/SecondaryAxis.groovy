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

  /**
   * Guide specification for this axis (optional).
   * Currently not implemented - reserved for future use to control axis appearance.
   * In ggplot2, this can be used to specify guide_axis() parameters.
   */
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

  /**
   * Numerically compute the inverse transformation for a secondary axis value.
   * Uses binary search to find the primary value that transforms to the given secondary value.
   * This works for monotonic transformations.
   *
   * @param secondaryValue Value in secondary axis units
   * @param primaryMin Minimum value to search in primary axis units
   * @param primaryMax Maximum value to search in primary axis units
   * @param tolerance Tolerance for convergence (default 1e-6)
   * @param maxIterations Maximum iterations (default 50)
   * @return Primary axis value that transforms to secondaryValue, or null if not found
   */
  BigDecimal inverseTransform(Number secondaryValue, Number primaryMin, Number primaryMax,
                              BigDecimal tolerance = 1e-6, int maxIterations = 50) {
    if (secondaryValue == null || primaryMin == null || primaryMax == null) {
      return null
    }
    if (transform == null) {
      return secondaryValue as BigDecimal
    }

    BigDecimal target = secondaryValue as BigDecimal
    BigDecimal min = primaryMin as BigDecimal
    BigDecimal max = primaryMax as BigDecimal

    // Check if transformation is monotonic increasing or decreasing
    BigDecimal fMin = applyTransform(min)
    BigDecimal fMax = applyTransform(max)

    if (fMin == null || fMax == null) {
      return null
    }

    boolean increasing = fMax > fMin

    // Check if target is within range
    if (increasing) {
      if (target < fMin || target > fMax) {
        return null
      }
    } else {
      if (target > fMin || target < fMax) {
        return null
      }
    }

    // Binary search
    for (int i = 0; i < maxIterations; i++) {
      BigDecimal mid = (min + max) / 2
      BigDecimal fMid = applyTransform(mid)

      if (fMid == null) {
        return null
      }

      BigDecimal diff = (fMid - target).abs()
      if (diff < tolerance) {
        return mid
      }

      if (increasing) {
        if (fMid < target) {
          min = mid
        } else {
          max = mid
        }
      } else {
        if (fMid > target) {
          min = mid
        } else {
          max = mid
        }
      }
    }

    // Return best approximation
    return (min + max) / 2
  }
}
