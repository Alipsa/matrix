package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Viridis color scale for categorical/discrete data.
 * Implements the viridis color palette family from the viridisLite R package.
 *
 * The viridis scales are designed to be:
 * - Perceptually uniform
 * - Colorblind friendly (deuteranopia and protanopia safe)
 * - Print friendly (works in grayscale)
 *
 * Usage:
 * scale_color_viridis_d()  // Default viridis palette
 * scale_color_viridis_d(option: 'magma')  // Use magma palette
 * scale_color_viridis_d(begin: 0.2, end: 0.8)  // Use middle portion
 * scale_color_viridis_d(direction: -1)  // Reverse direction
 */
@CompileStatic
class ScaleColorViridis extends ScaleDiscrete {

  /** Palette option: 'viridis' (D), 'magma' (A), 'inferno' (B), 'plasma' (C),
   *  'cividis' (E), 'rocket' (F), 'mako' (G), 'turbo' (H) */
  String option = 'viridis'

  /** Start of the color range (0-1) */
  double begin = 0.0

  /** End of the color range (0-1) */
  double end = 1.0

  /** Direction: 1 = normal, -1 = reversed */
  int direction = 1

  /** Color for NA/missing values */
  String naValue = 'grey50'

  /** Alpha transparency (0-1) */
  double alpha = 1.0

  /** Cached colors for current levels */
  private List<String> computedColors = []

  // Viridis palette key colors (used for smooth interpolation)
  // Each palette is defined with approximately 12–20 discrete colors
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

  ScaleColorViridis() {
    aesthetic = 'color'
  }

  ScaleColorViridis(Map params) {
    aesthetic = 'color'
    applyParams(params)
  }

  private double normalizeAlpha(double a) {
    if (a < 0.0d) {
      return 0.0d
    }
    if (a > 1.0d) {
      return 1.0d
    }
    return a
  }

  /**
   * Validate and normalize begin/end parameters to [0, 1] range.
   * Throws IllegalArgumentException if the value is outside the valid range.
   */
  private double validateRange(double value, String paramName) {
    if (value < 0.0d || value > 1.0d) {
      throw new IllegalArgumentException("${paramName} must be in range [0, 1], got: ${value}")
    }
    return value
  }

  private void applyParams(Map params) {
    if (params.option) this.option = normalizeOption(params.option as String)
    if (params.begin != null) this.begin = validateRange(params.begin as double, 'begin')
    if (params.end != null) this.end = validateRange(params.end as double, 'end')
    // Validate that begin <= end after both are set
    if (this.begin > this.end) {
      throw new IllegalArgumentException("begin (${this.begin}) must be less than or equal to end (${this.end})")
    }
    if (params.direction != null) this.direction = params.direction as int
    if (params.alpha != null) this.alpha = normalizeAlpha(params.alpha as double)
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>
    if (params.naValue) this.naValue = params.naValue as String
    // Support 'colour' British spelling
    if (params.aesthetic == 'colour') this.aesthetic = 'color'
    else if (params.aesthetic) this.aesthetic = params.aesthetic as String
  }

  /**
   * Normalize option names - accepts letter codes or full names
   */
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
  void train(List data) {
    super.train(data)
    // Compute colors after training so we know the number of levels
    computedColors = generateColors(levels.size())
  }

  /**
   * Generate n evenly spaced colors from the palette.
   */
  private List<String> generateColors(int n) {
    if (n <= 0) return []

    List<String> palette = PALETTES[option] ?: PALETTES['viridis']
    List<String> result = []

    // Calculate actual begin/end based on direction
    double actualBegin = direction >= 0 ? begin : end
    double actualEnd = direction >= 0 ? end : begin

    for (int i = 0; i < n; i++) {
      // Calculate position in 0-1 range
      double t = n == 1 ? 0.5d : (i as double) / (n - 1)
      // Map to begin-end range
      double pos = actualBegin + t * (actualEnd - actualBegin)
      // Get interpolated color
      result << interpolatePalette(palette, pos)
    }

    return result
  }

  /**
   * Interpolate a color at position t (0-1) from the palette.
   */
  private String interpolatePalette(List<String> palette, double t) {
    // Clamp t to valid range
    t = Math.max(0, Math.min(1, t))

    // Find the two palette colors to interpolate between
    double scaledPos = t * (palette.size() - 1)
    int lowIndex = (int) Math.floor(scaledPos)
    int highIndex = (int) Math.ceil(scaledPos)

    // Clamp indices
    lowIndex = Math.max(0, Math.min(lowIndex, palette.size() - 1))
    highIndex = Math.max(0, Math.min(highIndex, palette.size() - 1))

    if (lowIndex == highIndex) {
      return applyAlpha(palette[lowIndex])
    }

    // Interpolate
    double localT = scaledPos - lowIndex
    return applyAlpha(interpolateColor(palette[lowIndex], palette[highIndex], localT))
  }

  /**
   * Interpolate between two hex colors.
   */
  private String interpolateColor(String color1, String color2, double t) {
    int[] rgb1 = parseHex(color1)
    int[] rgb2 = parseHex(color2)

    int r = (int) Math.round(rgb1[0] + t * (rgb2[0] - rgb1[0]))
    int g = (int) Math.round(rgb1[1] + t * (rgb2[1] - rgb1[1]))
    int b = (int) Math.round(rgb1[2] + t * (rgb2[2] - rgb1[2]))

    return String.format('#%02X%02X%02X',
        Math.max(0, Math.min(255, r)),
        Math.max(0, Math.min(255, g)),
        Math.max(0, Math.min(255, b)))
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
   * Converts #RRGGBB to #RRGGBBAA when alpha is in (0,1).
   */
  private String applyAlpha(String color) {
    if (color == null) {
      return null
    }
    // Preserve existing behavior when alpha is fully opaque or higher
    if (alpha >= 1.0d) {
      return color
    }

    String hex = color.startsWith('#') ? color.substring(1) : color
    // Only handle standard 6-digit hex colors; otherwise, return as-is
    if (hex.length() != 6) {
      return color
    }

    double clampedAlpha = Math.max(0.0d, Math.min(1.0d, alpha))
    int alphaInt = (int) Math.round(clampedAlpha * 255.0d)
    String alphaHex = String.format('%02X', alphaInt)

    return '#' + hex + alphaHex
  }

  @Override
  Object transform(Object value) {
    if (value == null) return naValue
    if (levels.isEmpty()) return naValue

    int index = levels.indexOf(value)
    if (index < 0) return naValue

    return computedColors.isEmpty() ? naValue : computedColors[index % computedColors.size()]
  }

  @Override
  Object inverse(Object value) {
    if (value == null) return null
    int colorIndex = computedColors.indexOf(value as String)
    if (colorIndex >= 0 && colorIndex < levels.size()) {
      return levels[colorIndex]
    }
    return null
  }

  /**
   * Get the color for a specific level index.
   */
  String getColorForIndex(int index) {
    if (index < 0 || computedColors.isEmpty()) return naValue
    return computedColors[index % computedColors.size()]
  }

  /**
   * Get all computed colors in order of levels.
   */
  List<String> getColors() {
    return computedColors
  }

  @Override
  void reset() {
    super.reset()
    computedColors = []
  }
}
