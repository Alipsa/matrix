package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.render.scale.ColorScaleUtil as CharmColorScaleUtil

/**
 * Utilities for color scale interpolation.
 *
 * @deprecated Use {@link se.alipsa.matrix.charm.render.scale.ColorScaleUtil} instead.
 *             This stub delegates all calls to the charm implementation.
 */
@Deprecated
@CompileStatic
class ColorScaleUtil {

  /** @see CharmColorScaleUtil#interpolateColor(String, String, BigDecimal) */
  static String interpolateColor(String color1, String color2, BigDecimal t) {
    CharmColorScaleUtil.interpolateColor(color1, color2, t)
  }

  /** @see CharmColorScaleUtil#parseColor(String) */
  static int[] parseColor(String color) {
    CharmColorScaleUtil.parseColor(color)
  }
}
