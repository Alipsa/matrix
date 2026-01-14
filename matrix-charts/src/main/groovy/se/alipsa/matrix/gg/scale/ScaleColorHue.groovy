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
      // Use toString() to ensure consistent String keys (handles GString vs String)
      palette[value.toString()] = colors[idx]
    }
  }

  /**
   * Generate n evenly-spaced colors from the HCL color wheel.
   */
  private List<String> generateHueColors(int n) {
    if (n == 0) return []
    if (n == 1) {
      double hue = hueRange[0] as double
      return [ColorSpaceUtil.hclToHex(hue, chroma as double, luminance as double)]
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

      String color = ColorSpaceUtil.hclToHex(hue, chroma as double, luminance as double)
      colors.add(color)
    }

    return colors
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    // Use toString() to match storage format
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
      // Use toString() to match storage format
      String color = palette.get(level.toString())
      result.add(color != null ? color : naValue)
    }
    return result
  }

  @Override
  void reset() {
    super.reset()
    palette = [:]
  }
}
