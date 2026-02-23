package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.render.scale.ColorSpaceUtil as CharmColorSpaceUtil

/**
 * Utility class for color space conversions.
 *
 * @deprecated Use {@link se.alipsa.matrix.charm.render.scale.ColorSpaceUtil} instead.
 *             This stub delegates all calls to the charm implementation.
 */
@Deprecated
@CompileStatic
class ColorSpaceUtil {

  /** @see CharmColorSpaceUtil#hclToHex(Number, Number, Number) */
  static String hclToHex(Number h, Number c, Number l) {
    CharmColorSpaceUtil.hclToHex(h, c, l)
  }
}
