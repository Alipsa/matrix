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
  private static final double REF_X = 95.047d
  private static final double REF_Y = 100.0d
  private static final double REF_Z = 108.883d

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
  static String hclToHex(double h, double c, double l) {
    // Convert HCL cylindrical coordinates to CIELUV Cartesian
    double hr = Math.toRadians(h)
    double u = c * Math.cos(hr)
    double v = c * Math.sin(hr)

    if (l <= 0.0d) {
      return '#000000'
    }

    // Convert CIELUV to CIE XYZ using D65 white point
    double denom = REF_X + 15.0d * REF_Y + 3.0d * REF_Z
    double u0 = (4.0d * REF_X) / denom
    double v0 = (9.0d * REF_Y) / denom

    double up = u / (13.0d * l) + u0
    double vp = v / (13.0d * l) + v0

    // Y component (luminance)
    double y = l > 8.0d ? REF_Y * Math.pow((l + 16.0d) / 116.0d, 3.0d) : REF_Y * l / 903.3d

    // X and Z components
    double denom1 = ((up - 4.0d) * vp - up * vp)
    if (Math.abs(denom1) < 1.0e-9d || Math.abs(vp) < 1.0e-9d) {
      return '#000000'
    }

    double x = 0.0d - (9.0d * y * up) / denom1
    if (!Double.isFinite(x) || Math.abs(x) > 1.0e6d) {
      return '#000000'
    }

    double z = (9.0d * y - 15.0d * vp * y - vp * x) / (3.0d * vp)

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
  private static String xyzToHex(double x, double y, double z) {
    // Normalize to 0-1 range
    double xn = x / 100.0d
    double yn = y / 100.0d
    double zn = z / 100.0d

    // XYZ to linear RGB (sRGB D65)
    double r = 3.2406d * xn + -1.5372d * yn + -0.4986d * zn
    double g = -0.9689d * xn + 1.8758d * yn + 0.0415d * zn
    double b = 0.0557d * xn + -0.2040d * yn + 1.0570d * zn

    // Apply gamma correction
    r = gammaCorrect(r)
    g = gammaCorrect(g)
    b = gammaCorrect(b)

    // Clamp to [0, 1] and convert to 0-255
    int ri = (int) Math.round(Math.max(0.0d, Math.min(1.0d, r)) * 255.0d)
    int gi = (int) Math.round(Math.max(0.0d, Math.min(1.0d, g)) * 255.0d)
    int bi = (int) Math.round(Math.max(0.0d, Math.min(1.0d, b)) * 255.0d)

    return String.format('#%02X%02X%02X', ri, gi, bi)
  }

  /**
   * Apply sRGB gamma correction to a linear RGB component.
   *
   * @param c linear RGB component
   * @return gamma-corrected component in [0, 1]
   */
  private static double gammaCorrect(double c) {
    if (c <= 0.0031308d) {
      return 12.92d * c
    }
    return 1.055d * Math.pow(c, 1.0d / 2.4d) - 0.055d
  }
}
