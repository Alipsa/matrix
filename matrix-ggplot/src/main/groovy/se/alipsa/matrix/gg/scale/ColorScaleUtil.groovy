package se.alipsa.matrix.gg.scale


import se.alipsa.matrix.charm.Scale as CharmScale
import se.alipsa.matrix.charm.render.scale.ColorScaleUtil as CharmColorScaleUtil
import se.alipsa.matrix.charm.util.ColorUtil

/**
 * Utilities for color scale interpolation.
 *
 * @deprecated Use {@link se.alipsa.matrix.charm.render.scale.ColorScaleUtil} instead.
 *             This stub delegates all calls to the charm implementation.
 */
@Deprecated
class ColorScaleUtil {

  /** @see CharmColorScaleUtil#interpolateColor(String, String, BigDecimal) */
  static String interpolateColor(String color1, String color2, BigDecimal t) {
    CharmColorScaleUtil.interpolateColor(color1, color2, t)
  }

  /** @see CharmColorScaleUtil#parseColor(String) */
  static int[] parseColor(String color) {
    CharmColorScaleUtil.parseColor(color)
  }

  /** @see CharmColorScaleUtil#binIndex(BigDecimal, int) */
  static int binIndex(BigDecimal normalized, int bins) {
    CharmColorScaleUtil.binIndex(normalized, bins)
  }

  /** @see CharmColorScaleUtil#binCentre(int, int) */
  static BigDecimal binCentre(int index, int bins) {
    CharmColorScaleUtil.binCentre(index, bins)
  }

  /** @see CharmColorScaleUtil#gradientNColorAt(List, List, BigDecimal) */
  static String gradientNColorAt(List<String> colors, List<BigDecimal> stops, BigDecimal point) {
    CharmColorScaleUtil.gradientNColorAt(colors, stops, point)
  }

  /** Copies a normalized NA colour into a Charm scale specification. */
  static CharmScale withNaValue(CharmScale target, String naValue) {
    if (naValue != null) {
      target.params['naValue'] = ColorUtil.normalizeColor(naValue) ?: naValue
    }
    target
  }
}
