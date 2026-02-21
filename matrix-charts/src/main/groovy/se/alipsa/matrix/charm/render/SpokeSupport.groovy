package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Shared parameter resolution for spoke stat/geom behavior.
 */
@CompileStatic
class SpokeSupport {

  /**
   * Resolve angle column mapping; returns null when a literal angle value is used.
   */
  static String resolveAngleColumn(Map<String, Object> params) {
    Object angleParam = params?.angle
    if (angleParam instanceof CharSequence) {
      return angleParam.toString()
    }
    if (angleParam == null) {
      return 'angle'
    }
    null
  }

  /**
   * Resolve fallback angle when row data has no angle value.
   */
  static BigDecimal resolveAngleDefault(Map<String, Object> params) {
    NumberCoercionUtil.coerceToBigDecimal(params?.angle) ?: 0
  }

  /**
   * Resolve radius column mapping; returns null when a literal radius value is used.
   */
  static String resolveRadiusColumn(Map<String, Object> params) {
    Object radiusParam = params?.radius
    if (radiusParam instanceof CharSequence) {
      return radiusParam.toString()
    }
    if (radiusParam == null) {
      return 'radius'
    }
    null
  }

  /**
   * Resolve fallback radius while avoiding coercion of column-name strings.
   */
  static BigDecimal resolveRadiusDefault(Map<String, Object> params) {
    BigDecimal radiusDefault = NumberCoercionUtil.coerceToBigDecimal(params?.radiusDefault)
    if (radiusDefault != null) {
      return radiusDefault
    }
    BigDecimal radiusParam = NumberCoercionUtil.coerceToBigDecimal(params?.radius)
    radiusParam ?: 1
  }
}
