package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Utility class for color space conversions.
 * Provides HCL (CIELUV-based) to RGB conversion for perceptually uniform color scales.
 */
@CompileStatic
class ColorSpaceUtil {

  // D65 reference white point for CIE XYZ (used in CIELUV conversions)
  // See: CIE standard illuminant D65 and CIE 1931 2° observer
  private static final BigDecimal REF_X = 95.047
  private static final BigDecimal REF_Y = 100.0
  private static final BigDecimal REF_Z = 108.883

  /**
   * Convert CIELUV-derived HCL (Hue, Chroma, Luminance) to sRGB.
   *
   * HCL is a cylindrical representation of the CIELUV color space,
   * which is designed to be perceptually uniform.
   *
   * @param h hue in degrees (0-360)
   * @param c chroma (saturation)
   * @param l luminance (lightness, 0-100)
   * @return hex RGB string (e.g., '#FF0000')
   */
  static String hclToHex(Number h, Number c, Number l) {
    // Convert HCL cylindrical coordinates to CIELUV Cartesian
    BigDecimal hr = (h as BigDecimal).toRadians()
    BigDecimal u = (c as BigDecimal) * hr.cos()
    BigDecimal v = (c as BigDecimal) * hr.sin()
    BigDecimal lVal = l as BigDecimal

    if (lVal <= 0) {
      return '#000000'
    }

    // Convert CIELUV to CIE XYZ using D65 white point
    BigDecimal denom = REF_X + 15 * REF_Y + 3 * REF_Z
    BigDecimal u0 = (4 * REF_X) / denom
    BigDecimal v0 = (9 * REF_Y) / denom

    BigDecimal up = u / (13 * lVal) + u0
    BigDecimal vp = v / (13 * lVal) + v0

    // Y component (luminance)
    BigDecimal y = lVal > 8 ? REF_Y * ((lVal + 16) / 116) ** 3 : REF_Y * lVal / 903.3

    // X and Z components
    BigDecimal denom1 = (up - 4) * vp - up * vp
    if (denom1.abs() < 1.0e-9 || vp.abs() < 1.0e-9) {
      return '#000000'
    }

    BigDecimal x = -(9 * y * up) / denom1
    if (x.abs() > 1.0e6) {
      return '#000000'
    }

    BigDecimal z = (9 * y - 15 * vp * y - vp * x) / (3 * vp)

    return xyzToHex(x, y, z)
  }

  /**
   * Convert CIE XYZ to sRGB hex using D65 reference.
   *
   * @param x X component
   * @param y Y component
   * @param z Z component
   * @return hex RGB string
   */
  private static String xyzToHex(BigDecimal x, BigDecimal y, BigDecimal z) {
    // Normalize to 0-1 range
    BigDecimal xn = x / 100
    BigDecimal yn = y / 100
    BigDecimal zn = z / 100

    // XYZ to linear RGB (sRGB D65)
    BigDecimal r = 3.2406 * xn - 1.5372 * yn - 0.4986 * zn
    BigDecimal g = -0.9689 * xn + 1.8758 * yn + 0.0415 * zn
    BigDecimal b = 0.0557 * xn - 0.2040 * yn + 1.0570 * zn

    // Apply gamma correction
    r = gammaCorrect(r)
    g = gammaCorrect(g)
    b = gammaCorrect(b)

    // Clamp to [0, 1] and convert to 0-255
    int ri = (0.max(r.min(1)) * 255).round() as int
    int gi = (0.max(g.min(1)) * 255).round() as int
    int bi = (0.max(b.min(1)) * 255).round() as int

    return String.format('#%02X%02X%02X', ri, gi, bi)
  }

  /**
   * Apply sRGB gamma correction to a linear RGB component.
   *
   * @param c linear RGB component
   * @return gamma-corrected component in [0, 1]
   */
  private static BigDecimal gammaCorrect(BigDecimal c) {
    if (c <= 0.0031308) {
      return 12.92 * c
    }
    return 1.055 * (c ** (1 / 2.4)) - 0.055
  }
}
