package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Continuous ColorBrewer scale for numeric data.
 */
@CompileStatic
class ScaleColorDistiller extends ScaleColorGradientN {

  /** Palette name (e.g. Blues, Spectral). */
  String palette

  /** Palette type: 'seq', 'div', or 'qual'. */
  String type = 'seq'

  /** Direction: 1 for normal, -1 for reversed. */
  int direction = 1

  /**
   * Create a ColorBrewer continuous scale with defaults.
   */
  ScaleColorDistiller() {
    super()
    applyPalette()
  }

  /**
   * Create a ColorBrewer continuous scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleColorDistiller(Map params) {
    super(params)
    applyParams(params)
    applyPalette()
  }

  private void applyParams(Map params) {
    if (params.palette != null) this.palette = params.palette.toString()
    if (params.type) this.type = params.type as String
    if (params.direction != null) this.direction = (params.direction as Number).intValue()
  }

  private void applyPalette() {
    String resolvedPalette = resolvePaletteName()
    List<String> paletteColors = BrewerPalettes.getPalette(resolvedPalette) ?: []
    if (direction < 0) {
      paletteColors = paletteColors.reverse()
    }
    if (!paletteColors.isEmpty()) {
      colors = paletteColors
    }
  }

  private String resolvePaletteName() {
    if (palette != null && BrewerPalettes.getPalette(palette) != null) {
      return palette
    }
    switch (type?.toLowerCase()) {
      case 'div':
        return 'Spectral'
      case 'qual':
        return 'Set1'
      case 'seq':
      default:
        return 'Blues'
    }
  }
}
