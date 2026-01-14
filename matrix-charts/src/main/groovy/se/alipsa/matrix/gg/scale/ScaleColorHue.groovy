package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Evenly spaced colors from the HCL color wheel.
 * This is ggplot2's default discrete color scale.
 *
 * Generates colors by selecting evenly-spaced hues around the color wheel
 * with fixed chroma and luminance for perceptual uniformity.
 *
 * Usage:
 * <pre>
 * scale_color_hue()  // Default: hues from 15° to 375°, chroma 100, luminance 65
 * scale_color_hue(h: [0, 360], c: 50, l: 70)  // Custom parameters
 * scale_color_hue(h.start: 90, direction: -1)  // Start at 90°, counter-clockwise
 * </pre>
 */
@CompileStatic
class ScaleColorHue extends ScaleDiscrete {

  /** Range of hues to use [min, max] in degrees (0-360) */
  List<Number> hueRange = [15, 375]  // Wraps around color wheel

  /** Chroma (saturation) - higher values are more saturated (default: 100) */
  Number chroma = 100

  /** Luminance (lightness) - 0 is black, 100 is white (default: 65) */
  Number luminance = 65

  /** Direction around color wheel: 1 (clockwise) or -1 (counter-clockwise) */
  int direction = 1

  /** Color for NA/missing values */
  String naValue = 'grey50'

  /** Generated color palette */
  private Map<Object, String> palette = [:]

  // D65 reference white point for CIE XYZ (used in CIELUV conversions)
  private static final double REF_X = 95.047d
  private static final double REF_Y = 100.0d
  private static final double REF_Z = 108.883d

  ScaleColorHue() {
    aesthetic = 'color'
  }

  ScaleColorHue(String aesthetic) {
    this.aesthetic = aesthetic
  }

  ScaleColorHue(String aesthetic, Map params) {
    this.aesthetic = aesthetic
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.h) {
      this.hueRange = params.h as List<Number>
    }
    if (params.c) {
      this.chroma = params.c as Number
    }
    if (params.l) {
      this.luminance = params.l as Number
    }
    if (params.direction) {
      this.direction = params.direction as int
    }
    if (params.containsKey('h.start')) {
      // h.start shifts the hue range
      Number start = params['h.start'] as Number
      Number span = (hueRange[1] as double) - (hueRange[0] as double)
      this.hueRange = [start, (start as double) + span]
    }
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.naValue) this.naValue = params.naValue as String
  }

  @Override
  void train(List data) {
    super.train(data)
    generatePalette()
  }

  /**
   * Generate color palette based on the current levels.
   */
  private void generatePalette() {
    if (domain == null || domain.isEmpty()) {
      palette = [:]
      return
    }

    int n = domain.size()
    List<String> colors = generateHueColors(n)

    palette = [:]
    domain.eachWithIndex { value, idx ->
      palette[value] = colors[idx]
    }
  }

  /**
   * Generate n evenly-spaced colors from the HCL color wheel.
   */
  private List<String> generateHueColors(int n) {
    if (n == 0) return []
    if (n == 1) {
      double hue = hueRange[0] as double
      return [hclToHex(hue, chroma as double, luminance as double)]
    }

    double hueMin = hueRange[0] as double
    double hueMax = hueRange[1] as double
    double hueSpan = (hueMax - hueMin) / n

    List<String> colors = []
    for (int i = 0; i < n; i++) {
      double hue
      if (direction < 0) {
        hue = hueMax - i * hueSpan
      } else {
        hue = hueMin + i * hueSpan
      }
      // Normalize to 0-360
      hue = hue % 360.0d
      if (hue < 0) hue += 360.0d

      String color = hclToHex(hue, chroma as double, luminance as double)
      colors.add(color)
    }

    return colors
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    // Convert to String to handle GString vs String key mismatch
    String key = value.toString()
    if (palette.containsKey(key)) {
      return palette[key]
    }
    return naValue
  }

  @Override
  Object inverse(Object value) {
    if (value == null) return null
    // Find the level that maps to this color
    def entry = palette.find { k, v -> v == value }
    return entry?.key
  }

  /**
   * Get all colors in order of levels.
   */
  List<String> getColors() {
    if (levels.isEmpty()) return []
    if (palette.isEmpty()) {
      generatePalette()
    }
    List<String> result = []
    for (Object level : levels) {
      // Convert to String to handle GString vs String key mismatch
      String color = palette.get(level.toString())
      result.add(color != null ? color : naValue)
    }
    return result
  }

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
  private static String hclToHex(double h, double c, double l) {
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
