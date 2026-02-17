package se.alipsa.matrix.charm.render.scale

import groovy.transform.CompileStatic

/**
 * Provides Viridis family color palettes.
 *
 * Generates perceptually uniform, colorblind-friendly colors from the viridis
 * palette family (viridis, magma, inferno, plasma, cividis, rocket, mako, turbo).
 */
@CompileStatic
class ViridisProvider {

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

  /**
   * Generates n evenly spaced colors from the specified palette.
   *
   * @param n number of colors to generate
   * @param option palette name (viridis, magma, inferno, plasma, cividis, rocket, mako, turbo)
   * @param begin start of the color range (0-1)
   * @param end end of the color range (0-1)
   * @param direction 1 for normal, -1 for reversed
   * @param alpha transparency (0-1), applied as #RRGGBBAA when < 1
   * @return list of hex color strings
   */
  static List<String> getColors(int n, String option = 'viridis',
                                 BigDecimal begin = 0.0, BigDecimal end = 1.0,
                                 int direction = 1, BigDecimal alpha = 1.0) {
    if (n <= 0) return []

    String normalizedOption = normalizeOption(option)
    List<String> palette = PALETTES[normalizedOption] ?: PALETTES['viridis']

    BigDecimal actualBegin = direction > 0 ? begin : end
    BigDecimal actualEnd = direction > 0 ? end : begin

    List<String> result = []
    for (int i = 0; i < n; i++) {
      BigDecimal t = n == 1 ? 0.5 : i / (n - 1)
      BigDecimal pos = actualBegin + t * (actualEnd - actualBegin)
      String color = interpolatePalette(palette, pos)
      result << applyAlpha(color, alpha)
    }
    result
  }

  /**
   * Normalize option names - accepts letter codes or full names.
   */
  static String normalizeOption(String opt) {
    final Map<String, String> optionMap = [
        'A': 'magma', 'MAGMA': 'magma', 'magma': 'magma',
        'B': 'inferno', 'INFERNO': 'inferno', 'inferno': 'inferno',
        'C': 'plasma', 'PLASMA': 'plasma', 'plasma': 'plasma',
        'D': 'viridis', 'VIRIDIS': 'viridis', 'viridis': 'viridis',
        'E': 'cividis', 'CIVIDIS': 'cividis', 'cividis': 'cividis',
        'F': 'rocket', 'ROCKET': 'rocket', 'rocket': 'rocket',
        'G': 'mako', 'MAKO': 'mako', 'mako': 'mako',
        'H': 'turbo', 'TURBO': 'turbo', 'turbo': 'turbo'
    ]
    optionMap[opt?.toUpperCase()] ?: 'viridis'
  }

  private static String interpolatePalette(List<String> palette, BigDecimal t) {
    t = 0.max(1.min(t))
    BigDecimal scaledPos = t * (palette.size() - 1)
    int lowIndex = scaledPos.floor() as int
    int highIndex = scaledPos.ceil() as int
    lowIndex = [0, lowIndex, palette.size() - 1].sort()[1]
    highIndex = [0, highIndex, palette.size() - 1].sort()[1]

    if (lowIndex == highIndex) {
      return palette[lowIndex]
    }
    BigDecimal localT = scaledPos - lowIndex
    ColorScaleUtil.interpolateColor(palette[lowIndex], palette[highIndex], localT)
  }

  private static String applyAlpha(String color, BigDecimal alpha) {
    if (color == null || alpha >= 1.0) return color
    String hex = color.startsWith('#') ? color.substring(1) : color
    if (hex.length() != 6) return color
    BigDecimal clampedAlpha = 0.0.max(1.0.min(alpha))
    int alphaInt = (clampedAlpha * 255.0).round() as int
    String alphaHex = String.format('%02X', alphaInt)
    '#' + hex + alphaHex
  }
}
