package se.alipsa.matrix.charm.render.scale

import groovy.transform.CompileStatic

/**
 * Utilities for color scale interpolation.
 */
@CompileStatic
class ColorScaleUtil {

  /**
   * Interpolate between two colors.
   *
   * @param color1 start color (hex string or named)
   * @param color2 end color (hex string or named)
   * @param t interpolation factor (0-1)
   * @return interpolated color as hex string
   */
  static String interpolateColor(String color1, String color2, BigDecimal t) {
    int[] rgb1 = parseColor(color1)
    int[] rgb2 = parseColor(color2)

    int r = (rgb1[0] + t * (rgb2[0] - rgb1[0])).round() as int
    int g = (rgb1[1] + t * (rgb2[1] - rgb1[1])).round() as int
    int b = (rgb1[2] + t * (rgb2[2] - rgb1[2])).round() as int

    r = 0.max(r.min(255)) as int
    g = 0.max(g.min(255)) as int
    b = 0.max(b.min(255)) as int

    String.format('#%02X%02X%02X', r, g, b)
  }

  /**
   * Parse a color string to RGB values.
   * Supports hex format (#RGB, #RRGGBB) and some named colors.
   *
   * @param color color string
   * @return RGB array
   */
  static int[] parseColor(String color) {
    if (color == null) return [128, 128, 128] as int[]

    if (color.startsWith('#')) {
      String hex = color.substring(1)
      if (hex.length() == 3) {
        hex = "${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
      }
      if (hex.length() == 6) {
        int r = Integer.parseInt(hex.substring(0, 2), 16)
        int g = Integer.parseInt(hex.substring(2, 4), 16)
        int b = Integer.parseInt(hex.substring(4, 6), 16)
        return [r, g, b] as int[]
      }
    }

    NAMED_COLORS[color.toLowerCase()] ?: ([128, 128, 128] as int[])
  }

  private static final Map<String, int[]> NAMED_COLORS = [
      'white': [255, 255, 255] as int[],
      'black': [0, 0, 0] as int[],
      'red': [255, 0, 0] as int[],
      'green': [0, 128, 0] as int[],
      'blue': [0, 0, 255] as int[],
      'yellow': [255, 255, 0] as int[],
      'cyan': [0, 255, 255] as int[],
      'magenta': [255, 0, 255] as int[],
      'orange': [255, 165, 0] as int[],
      'purple': [128, 0, 128] as int[],
      'pink': [255, 192, 203] as int[],
      'grey': [128, 128, 128] as int[],
      'gray': [128, 128, 128] as int[],
      'grey50': [128, 128, 128] as int[],
      'gray50': [128, 128, 128] as int[],
      'darkblue': [0, 0, 139] as int[],
      'lightblue': [173, 216, 230] as int[],
      'darkgreen': [0, 100, 0] as int[],
      'lightgreen': [144, 238, 144] as int[],
      'darkred': [139, 0, 0] as int[],
      'steelblue': [70, 130, 180] as int[],
      'navy': [0, 0, 128] as int[],
      'maroon': [128, 0, 0] as int[],
      'olive': [128, 128, 0] as int[],
      'teal': [0, 128, 128] as int[]
  ]
}
