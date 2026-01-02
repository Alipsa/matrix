package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Manual discrete color scale.
 * Maps categorical values to specific colors provided by the user.
 *
 * Usage:
 * scale_color_manual(values: ['red', 'green', 'blue'])  // Map by order
 * scale_color_manual(values: [cat1: 'red', cat2: 'blue'])  // Map by name
 */
@CompileStatic
class ScaleColorManual extends ScaleDiscrete {

  /** List of colors or map of value -> color */
  List<String> values = []

  /** Named mapping of data values to colors */
  Map<Object, String> namedValues = [:]

  /** Color for NA/missing values */
  String naValue = 'grey50'

  private List<String> computedPalette = []

  // D65 reference white point for CIE XYZ (used in CIELUV/CIELAB conversions).
  // See: CIE standard illuminant D65 and CIE 1931 2° observer.
  private static final double REF_X = 95.047d
  private static final double REF_Y = 100.0d
  private static final double REF_Z = 108.883d

  ScaleColorManual() {
    aesthetic = 'color'
  }

  ScaleColorManual(Map params) {
    aesthetic = 'color'
    applyParams(params)
  }

  private void applyParams(Map params) {
    if (params.values) {
      def v = params.values
      if (v instanceof List) {
        this.values = v.collect { it?.toString() }
        this.computedPalette = []
      } else if (v instanceof Map) {
        this.namedValues = (v as Map).collectEntries { k, val ->
          [k, val?.toString()]
        } as Map<Object, String>
        this.computedPalette = []
      }
    }
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.naValue) this.naValue = params.naValue as String
    // Support 'colour' British spelling
    if (params.aesthetic == 'colour') this.aesthetic = 'color'
    else if (params.aesthetic) this.aesthetic = params.aesthetic as String
  }

  @Override
  void train(List data) {
    super.train(data)
    if (values.isEmpty() && namedValues.isEmpty()) {
      computedPalette = generateHuePalette(levels.size())
    } else {
      computedPalette = []
    }
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    if (levels.isEmpty()) return naValue

    // Check named mapping first
    if (namedValues.containsKey(value)) {
      return namedValues[value]
    }

    // Fall back to positional mapping
    int index = levels.indexOf(value)
    if (index < 0) return naValue

    // Get color from values list or computed palette
    if (!values.isEmpty()) {
      return values[index % values.size()]
    }
    if (computedPalette.isEmpty()) {
      computedPalette = generateHuePalette(levels.size())
    }
    // Guard against empty or stale palettes even if levels are populated.
    if (index >= computedPalette.size()) return naValue
    return computedPalette[index]
  }

  @Override
  Object inverse(Object value) {
    // Find the level that maps to this color
    if (value == null) return null

    // Check named values
    def namedEntry = namedValues.find { k, v -> v == value }
    if (namedEntry) return namedEntry.key

    // Check positional mapping
    if (!values.isEmpty()) {
      int colorIndex = values.indexOf(value as String)
      if (colorIndex >= 0 && colorIndex < levels.size()) {
        return levels[colorIndex]
      }
    } else {
      if (computedPalette.isEmpty()) {
        computedPalette = generateHuePalette(levels.size())
      }
      int colorIndex = computedPalette.indexOf(value as String)
      if (colorIndex >= 0 && colorIndex < levels.size()) {
        return levels[colorIndex]
      }
    }

    return null
  }

  /**
   * Get the color for a specific level index.
   */
  String getColorForIndex(int index) {
    if (index < 0) return naValue
    if (!values.isEmpty()) {
      return values[index % values.size()]
    }
    if (computedPalette.isEmpty()) {
      computedPalette = generateHuePalette(levels.size())
    }
    if (index >= computedPalette.size()) return naValue
    return computedPalette[index]
  }

  /**
   * Get all colors in order of levels.
   */
  List<String> getColors() {
    if (values.isEmpty() && namedValues.isEmpty()) {
      if (computedPalette.isEmpty()) {
        computedPalette = generateHuePalette(levels.size())
      }
      return new ArrayList<>(computedPalette)
    }
    return levels.collect { transform(it) as String }
  }

  /**
   * Convenience method to set values.
   */
  ScaleColorManual values(List<String> colors) {
    this.values = colors
    this.computedPalette = []
    return this
  }

  /**
   * Convenience method to set named values.
   */
  ScaleColorManual values(Map<Object, String> mapping) {
    this.namedValues = mapping
    this.computedPalette = []
    return this
  }

  /**
   * Generate a ggplot2-like hue palette in HCL space.
   *
   * @param n number of colors to generate
   * @return list of hex RGB colors
   */
  private static List<String> generateHuePalette(int n) {
    if (n <= 0) return []
    // ggplot2 hue palette defaults: h = [15, 375], c = 100, l = 65.
    double start = 15.0d
    double end = 375.0d
    double step = (end - start) / n
    List<String> colors = new ArrayList<>(n)
    for (int i = 0; i < n; i++) {
      double hue = start + step * i
      hue = hue % 360.0d
      colors << hclToHex(hue, 100.0d, 65.0d)
    }
    return colors
  }

  /**
   * Convert CIELUV-derived HCL (Hue, Chroma, Luminance) to sRGB.
   * Constants like 903.3, 116.0, 13.0, 4.0, 9.0, 15.0, 3.0, 8.0, 16.0
   * come from the CIE L*u*v* / L*a*b* conversion formulas.
   *
   * @param h hue in degrees
   * @param c chroma
   * @param l luminance
   * @return hex RGB string
   */
  private static String hclToHex(double h, double c, double l) {
    double hr = Math.toRadians(h)
    double u = c * Math.cos(hr)
    double v = c * Math.sin(hr)
    if (l <= 0.0d) {
      return '#000000'
    }
    double denom = REF_X + 15.0d * REF_Y + 3.0d * REF_Z
    double u0 = (4.0d * REF_X) / denom
    double v0 = (9.0d * REF_Y) / denom
    double up = u / (13.0d * l) + u0
    double vp = v / (13.0d * l) + v0
    double y = l > 8.0d ? REF_Y * Math.pow((l + 16.0d) / 116.0d, 3.0d) : REF_Y * l / 903.3d
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
    double xn = x / 100.0d
    double yn = y / 100.0d
    double zn = z / 100.0d
    double r = 3.2406d * xn + -1.5372d * yn + -0.4986d * zn
    double g = -0.9689d * xn + 1.8758d * yn + 0.0415d * zn
    double b = 0.0557d * xn + -0.2040d * yn + 1.0570d * zn
    r = gammaCorrect(r)
    g = gammaCorrect(g)
    b = gammaCorrect(b)
    int ri = (int) Math.round(Math.max(0.0d, Math.min(1.0d, r)) * 255.0d)
    int gi = (int) Math.round(Math.max(0.0d, Math.min(1.0d, g)) * 255.0d)
    int bi = (int) Math.round(Math.max(0.0d, Math.min(1.0d, b)) * 255.0d)
    return String.format('#%02X%02X%02X', ri, gi, bi)
  }

  /**
   * Apply sRGB gamma correction to a linear RGB component.
   *
   * @param c linear RGB component
   * @return gamma-corrected component
   */
  private static double gammaCorrect(double c) {
    if (c <= 0.0031308d) {
      return 12.92d * c
    }
    return 1.055d * Math.pow(c, 1.0d / 2.4d) - 0.055d
  }
}
