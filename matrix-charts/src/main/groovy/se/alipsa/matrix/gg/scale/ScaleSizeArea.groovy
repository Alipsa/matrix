package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Size scale where area is proportional to the data.
 */
@CompileStatic
class ScaleSizeArea extends ScaleSizeContinuous {

  /**
   * Create a size-by-area scale with defaults.
   */
  ScaleSizeArea() {
    super()
  }

  /**
   * Create a size-by-area scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleSizeArea(Map params) {
    super(params)
  }

  @Override
  Object transform(Object value) {
    Double numeric = coerceToNumber(value)
    if (numeric == null) return naValue

    double v = numeric
    double dMin = computedDomain[0] as double
    double dMax = computedDomain[1] as double
    double rMin = range[0] as double
    double rMax = range[1] as double

    if (dMax == dMin) {
      double midArea = ((rMin * rMin) + (rMax * rMax)) / 2.0d
      return Math.sqrt(midArea)
    }

    double normalized = (v - dMin) / (dMax - dMin)
    double areaMin = rMin * rMin
    double areaMax = rMax * rMax
    double area = areaMin + normalized * (areaMax - areaMin)
    return Math.sqrt(area)
  }
}
