package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

import java.math.MathContext

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

    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]
    BigDecimal rMin = range[0]
    BigDecimal rMax = range[1]

    if (dMax == dMin) {
      BigDecimal midArea = (rMin * rMin + rMax * rMax) / 2
      return midArea.sqrt(MathContext.DECIMAL64)
    }

    BigDecimal normalized = (v - dMin) / (dMax - dMin)
    BigDecimal areaMin = rMin * rMin
    BigDecimal areaMax = rMax * rMax
    BigDecimal area = areaMin + normalized * (areaMax - areaMin)
    return area.sqrt(MathContext.DECIMAL64)
  }
}
