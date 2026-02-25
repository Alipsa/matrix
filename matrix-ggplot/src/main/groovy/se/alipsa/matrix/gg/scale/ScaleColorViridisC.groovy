package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Continuous viridis color scale for numeric data.
 * Maps numeric values to colors along the viridis (or related) palette.
 *
 * The viridis scales are designed to be:
 * - Perceptually uniform
 * - Colorblind friendly (deuteranopia and protanopia safe)
 * - Print friendly (works in grayscale)
 *
 * Usage:
 * scale_color_viridis_c()  // Default viridis palette
 * scale_color_viridis_c(option: 'magma')  // Use magma palette
 * scale_color_viridis_c(begin: 0.2, end: 0.8)  // Use middle portion
 * scale_color_viridis_c(direction: -1)  // Reverse direction
 */
@CompileStatic
class ScaleColorViridisC extends ScaleContinuous {

  /** Palette option: 'viridis' (D), 'magma' (A), 'inferno' (B), 'plasma' (C),
   *  'cividis' (E), 'rocket' (F), 'mako' (G), 'turbo' (H) */
  String option = 'viridis'

  /** Start of the color range (0-1) */
  BigDecimal begin = 0.0

  /** End of the color range (0-1) */
  BigDecimal end = 1.0

  /** Direction: 1 = normal, -1 = reversed */
  int direction = 1

  /** Color for NA/missing values */
  String naValue = 'grey50'

  /** Alpha transparency (0-1) */
  BigDecimal alpha = 1.0

  /** Guide type: 'colorbar' or 'legend' */
  String guideType = 'colorbar'

  // Viridis palette key colors (used for smooth interpolation)
  private static final Map<String, List<String>> PALETTES = [
    'viridis': ['#440154', '#481567', '#482677', '#453781', '#3F4788',
                '#39558C', '#32648E', '#2D718E', '#287D8E', '#238A8D',
                '#1F968B', '#20A386', '#29AF7F', '#3CBC75', '#56C667',
                '#74D055', '#94D840', '#B8DE29', '#DCE318', '#FDE725'],
    'magma':   ['#000004', '#0C0926', '#231151', '#410F75', '#5C187E',
                '#762181', '#902B85', '#A93C8A', '#C14D8F', '#D66094',
                '#E8779A', '#F58DA4', '#FCA4B0', '#FDBBBD', '#FCD3CB',
                '#FCEADB', '#FCFDBF'],
    'inferno': ['#000004', '#0D082A', '#210C4A', '#390962', '#540B6E',
                '#6D0D74', '#870C75', '#A01A6C', '#B6305C', '#CB4748',
                '#DC5E32', '#E97B1A', '#F19709', '#F4B41B', '#F2CE4C',
                '#EEE681', '#FCFFA4'],
    'plasma':  ['#0D0887', '#2C0594', '#4903A0', '#6900A8', '#8707A6',
                '#A21EA0', '#BA3894', '#CF5286', '#E06C75', '#ED8665',
                '#F7A056', '#FCBA4A', '#FCD444', '#F7EA48', '#F0F921'],
    'cividis': ['#002051', '#093569', '#1E4B7A', '#336188', '#4A7692',
                '#618C99', '#79A19F', '#93B6A4', '#AECBA8', '#CBDFAE',
                '#EAF3B5', '#FDEA45'],
    'rocket':  ['#03051A', '#1C1044', '#3B0F70', '#5E177F', '#7E2482',
                '#9C2E7F', '#BC3F7A', '#D75171', '#EB6C5A', '#F88D51',
                '#FBAB46', '#F6CA3D', '#FCFFA4'],
    'mako':    ['#0B0405', '#1C0F27', '#2A1D46', '#332D5F', '#354175',
                '#325886', '#2E6E8E', '#2C8592', '#309B8E', '#45B180',
                '#71C66C', '#A8DA63', '#DEF5E5'],
    'turbo':   ['#30123B', '#4662D7', '#36AAF9', '#1BD0D5', '#2EF088',
                '#8EF835', '#D1E834', '#FABA39', '#F66B19', '#CF3A27',
                '#A51B38', '#7A0403']
  ]

  ScaleColorViridisC() {
    aesthetic = 'color'
    expand = NO_EXPAND  // No expansion for color scales
  }

  ScaleColorViridisC(Map params) {
    aesthetic = 'color'
    expand = NO_EXPAND
    applyParams(params)
  }

  private void applyParams(Map params) {
    this.option = params.option ? normalizeOption(params.option as String) : this.option
    if (params.begin != null) this.begin = validateRange(params.begin as BigDecimal, 'begin')
    if (params.end != null) this.end = validateRange(params.end as BigDecimal, 'end')
    if (this.begin > this.end) {
      throw new IllegalArgumentException("begin (${this.begin}) must be less than or equal to end (${this.end})")
    }
    if (params.direction != null) this.direction = normalizeDirection(params.direction as int)
    if (params.alpha != null) this.alpha = normalizeAlpha(params.alpha as BigDecimal)
    this.name = params.name as String ?: this.name
    this.limits = params.limits as List ?: this.limits
    this.breaks = params.breaks as List ?: this.breaks
    this.labels = params.labels as List<String> ?: this.labels
    this.naValue = params.naValue as String ?: this.naValue
    this.guideType = params.guide as String ?: this.guideType
    // Support 'colour' British spelling
    if (params.aesthetic == 'colour') this.aesthetic = 'color'
    else if (params.aesthetic) this.aesthetic = params.aesthetic as String
  }

  private BigDecimal normalizeAlpha(BigDecimal a) {
    return 0.0.max(1.0.min(a))
  }

  private BigDecimal validateRange(BigDecimal value, String paramName) {
    if (value < 0.0 || value > 1.0) {
      throw new IllegalArgumentException("${paramName} must be in range [0, 1], got: ${value}")
    }
    return value
  }

  private int normalizeDirection(int d) {
    return d < 0 ? -1 : 1
  }

  private String normalizeOption(String opt) {
    switch (opt?.toUpperCase()) {
      case 'A': case 'MAGMA': return 'magma'
      case 'B': case 'INFERNO': return 'inferno'
      case 'C': case 'PLASMA': return 'plasma'
      case 'D': case 'VIRIDIS': return 'viridis'
      case 'E': case 'CIVIDIS': return 'cividis'
      case 'F': case 'ROCKET': return 'rocket'
      case 'G': case 'MAKO': return 'mako'
      case 'H': case 'TURBO': return 'turbo'
      default: return 'viridis'
    }
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    if (!(value instanceof Number)) return naValue

    BigDecimal v = value as BigDecimal
    BigDecimal dMin = computedDomain[0] as BigDecimal
    BigDecimal dMax = computedDomain[1] as BigDecimal

    // Handle edge case
    if (dMax == dMin) return interpolatePalette(0.5)

    // Normalize to 0-1
    BigDecimal normalized = (v - dMin) / (dMax - dMin)
    normalized = 0.max(1.min(normalized))  // Clamp

    // Apply begin/end range and direction
    BigDecimal actualBegin = direction > 0 ? begin : end
    BigDecimal actualEnd = direction > 0 ? end : begin
    BigDecimal pos = actualBegin + normalized * (actualEnd - actualBegin)

    return interpolatePalette(pos)
  }

  /**
   * Interpolate a color at position t (0-1) from the palette.
   */
  private String interpolatePalette(BigDecimal t) {
    List<String> palette = PALETTES[option] ?: PALETTES['viridis']

    // Clamp t to valid range
    t = 0.max(1.min(t))

    // Find the two palette colors to interpolate between
    BigDecimal scaledPos = t * (palette.size() - 1)
    int lowIndex = scaledPos.floor() as int
    int highIndex = scaledPos.ceil() as int

    // Clamp indices
    lowIndex = [0, lowIndex, palette.size() - 1].sort()[1]
    highIndex = [0, highIndex, palette.size() - 1].sort()[1]

    if (lowIndex == highIndex) {
      return applyAlpha(palette[lowIndex])
    }

    // Interpolate
    BigDecimal localT = scaledPos - lowIndex
    return applyAlpha(interpolateColor(palette[lowIndex], palette[highIndex], localT))
  }

  /**
   * Interpolate between two hex colors.
   */
  private String interpolateColor(String color1, String color2, BigDecimal t) {
    int[] rgb1 = parseHex(color1)
    int[] rgb2 = parseHex(color2)

    int r = (rgb1[0] + t * (rgb2[0] - rgb1[0])).round() as int
    int g = (rgb1[1] + t * (rgb2[1] - rgb1[1])).round() as int
    int b = (rgb1[2] + t * (rgb2[2] - rgb1[2])).round() as int

    // Clamp to valid RGB range
    r = [0, r, 255].sort()[1]
    g = [0, g, 255].sort()[1]
    b = [0, b, 255].sort()[1]

    return String.format('#%02x%02x%02x', r, g, b)
  }

  /**
   * Parse hex color to RGB array.
   */
  private int[] parseHex(String hex) {
    String h = hex.startsWith('#') ? hex.substring(1) : hex
    return [
        Integer.parseInt(h.substring(0, 2), 16),
        Integer.parseInt(h.substring(2, 4), 16),
        Integer.parseInt(h.substring(4, 6), 16)
    ] as int[]
  }

  /**
   * Apply alpha transparency if needed.
   */
  private String applyAlpha(String color) {
    if (color == null) return null
    if (alpha >= 1.0) return color

    String hex = color.startsWith('#') ? color.substring(1) : color
    if (hex.length() != 6) return color

    BigDecimal clampedAlpha = 0.0.max(1.0.min(alpha))
    int alphaInt = (clampedAlpha * 255.0).round() as int
    String alphaHex = String.format('%02x', alphaInt)

    return '#' + hex + alphaHex
  }
}
