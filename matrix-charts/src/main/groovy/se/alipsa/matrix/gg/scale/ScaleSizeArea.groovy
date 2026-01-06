package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Size scale where area is proportional to the data.
 * Missing or invalid values map to naValue (BigDecimal, nullable) inherited from ScaleSizeContinuous.
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
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    if (v == null) return naValue

    BigDecimal dMin = computedDomain[0] as BigDecimal
    BigDecimal dMax = computedDomain[1] as BigDecimal
    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal rMax = range[1] as BigDecimal

    if (dMax.compareTo(dMin) == 0) {
      BigDecimal midArea = (rMin * rMin + rMax * rMax).divide(BigDecimal.valueOf(2), ScaleUtils.MATH_CONTEXT)
      return midArea.sqrt(ScaleUtils.MATH_CONTEXT)
    }

    BigDecimal normalized = (v - dMin).divide((dMax - dMin), ScaleUtils.MATH_CONTEXT)
    BigDecimal areaMin = rMin * rMin
    BigDecimal areaMax = rMax * rMax
    BigDecimal area = areaMin + normalized * (areaMax - areaMin)
    return area.sqrt(ScaleUtils.MATH_CONTEXT)
  }
}
