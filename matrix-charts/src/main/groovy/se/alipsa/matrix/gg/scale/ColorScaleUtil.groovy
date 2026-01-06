package se.alipsa.matrix.gg.scale

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
  static String interpolateColor(String color1, String color2, double t) {
    int[] rgb1 = parseColor(color1)
    int[] rgb2 = parseColor(color2)

    int r = (int) Math.round(rgb1[0] + t * (rgb2[0] - rgb1[0]))
    int g = (int) Math.round(rgb1[1] + t * (rgb2[1] - rgb1[1]))
    int b = (int) Math.round(rgb1[2] + t * (rgb2[2] - rgb1[2]))

    r = Math.max(0, Math.min(255, r))
    g = Math.max(0, Math.min(255, g))
    b = Math.max(0, Math.min(255, b))

    return String.format('#%02X%02X%02X', r, g, b)
  }

  /**
   * Parse a color string to RGB values.
   * Supports hex format (#RGB, #RRGGBB) and some named colors.
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

    switch (color.toLowerCase()) {
      case 'white': return [255, 255, 255] as int[]
      case 'black': return [0, 0, 0] as int[]
      case 'red': return [255, 0, 0] as int[]
      case 'green': return [0, 128, 0] as int[]
      case 'blue': return [0, 0, 255] as int[]
      case 'yellow': return [255, 255, 0] as int[]
      case 'cyan': return [0, 255, 255] as int[]
      case 'magenta': return [255, 0, 255] as int[]
      case 'orange': return [255, 165, 0] as int[]
      case 'purple': return [128, 0, 128] as int[]
      case 'pink': return [255, 192, 203] as int[]
      case 'grey': case 'gray': return [128, 128, 128] as int[]
      case 'grey50': case 'gray50': return [128, 128, 128] as int[]
      case 'darkblue': return [0, 0, 139] as int[]
      case 'lightblue': return [173, 216, 230] as int[]
      case 'darkgreen': return [0, 100, 0] as int[]
      case 'lightgreen': return [144, 238, 144] as int[]
      case 'darkred': return [139, 0, 0] as int[]
      case 'steelblue': return [70, 130, 180] as int[]
      case 'navy': return [0, 0, 128] as int[]
      case 'maroon': return [128, 0, 0] as int[]
      case 'olive': return [128, 128, 0] as int[]
      case 'teal': return [0, 128, 128] as int[]
      default: return [128, 128, 128] as int[]
    }
  }
}
