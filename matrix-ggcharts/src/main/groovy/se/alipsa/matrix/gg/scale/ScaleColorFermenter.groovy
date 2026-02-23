package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.util.Logger

/**
 * Binned ColorBrewer scale for continuous data.
 *
 * Creates discrete color bins using ColorBrewer palettes WITHOUT interpolation.
 * Each bin uses an exact color from the chosen ColorBrewer palette, preserving
 * the carefully designed color relationships.
 *
 * Unlike scale_color_distiller() which interpolates between ColorBrewer colors,
 * fermenter uses the palette colors as-is for bins. If the number of bins exceeds
 * the palette size, a warning is issued.
 *
 * <b>Direction Default:</b> The default direction is -1 (reversed), which matches
 * ggplot2's behavior. This differs from scale_color_brewer() (which uses direction=1)
 * because continuous and binned scales conventionally show darker colors for higher
 * values. Use direction=1 to reverse this behavior.
 *
 * <b>Palette Type:</b> The type parameter accepts both short ('seq', 'div', 'qual')
 * and long forms ('sequential', 'diverging', 'qualitative').
 *
 * Usage:
 * <pre>
 * // Sequential Blues palette with default bins
 * scale_color_fermenter(palette: 'Blues')
 *
 * // Palette by numeric index (1-based)
 * scale_color_fermenter(type: 'div', palette: 3)  // 3rd diverging palette
 *
 * // Diverging Spectral palette, normal direction
 * scale_fill_fermenter(type: 'diverging', palette: 'Spectral', direction: 1)
 *
 * // Custom number of breaks
 * scale_color_fermenter(palette: 'Reds', 'n.breaks': 5)
 * </pre>
 *
 * @see ScaleColorBrewer
 * @see ScaleColorDistiller
 * @see BrewerPalettes
 */
@CompileStatic
class ScaleColorFermenter extends ScaleContinuous {

  private static final Logger log = Logger.getLogger(ScaleColorFermenter)

  /** Which aesthetic this scale applies to ('color' or 'fill') */
  String aesthetic = 'color'

  /** Palette name (e.g., 'Blues', 'Spectral', 'Set1') */
  String palette

  /** Palette type: 'seq' (sequential), 'div' (diverging), or 'qual' (qualitative) */
  String type = 'seq'

  /** Direction: 1 for normal, -1 for reversed */
  int direction = -1

  /** Color for NA/missing values */
  String naValue = 'grey50'

  /** Guide type (default: 'coloursteps' for binned legend) */
  String guideType = 'coloursteps'

  /** Track whether nBreaks was explicitly set by user (vs using default) */
  private boolean nBreaksExplicitlySet = false

  /** Computed colors from ColorBrewer palette */
  private List<String> paletteColors = []

  /** Bin boundaries for value-to-color mapping */
  private List<BigDecimal> binBoundaries = []

  /**
   * Create a ColorBrewer fermenter scale with defaults.
   */
  ScaleColorFermenter() {
    super()
    expand = ScaleContinuous.NO_EXPAND  // No expansion for color scales
    loadPalette()  // Load default palette to ensure paletteColors is initialized
  }

  /**
   * Create a ColorBrewer fermenter scale with parameters.
   *
   * @param params scale parameters
   */
  ScaleColorFermenter(Map params) {
    super()
    expand = ScaleContinuous.NO_EXPAND
    applyParams(params)
    loadPalette()
  }

  private void applyParams(Map params) {
    if (params.aesthetic == 'colour') this.aesthetic = 'color'
    else if (params.aesthetic) this.aesthetic = params.aesthetic as String

    // Handle type first (needed for numeric palette resolution)
    if (params.type) this.type = params.type as String

    // Handle palette parameter (string name or numeric index)
    if (params.palette != null) {
      if (params.palette instanceof Number) {
        // Numeric palette index (1-based)
        int index = (params.palette as Number).intValue()
        String resolvedName = BrewerPalettes.getPaletteNameByIndex(this.type ?: 'seq', index)
        if (resolvedName != null) {
          this.palette = resolvedName
        } else {
          log.warn("Warning: Palette index $index is out of range for type '${this.type ?: 'seq'}'. Using default palette.")
          this.palette = null  // Will use type-based default
        }
      } else {
        // String palette name
        this.palette = params.palette.toString()
      }
    }

    if (params.direction != null) this.direction = (params.direction as Number).intValue()

    // Handle na.value parameter (support both dot notation and camelCase)
    if (params.containsKey('na.value')) {
      this.naValue = params['na.value'] as String
    } else if (params.naValue) {
      this.naValue = params.naValue as String
    }

    if (params.guide) this.guideType = params.guide as String
    if (params.name) this.name = params.name as String
    if (params.limits) this.limits = params.limits as List
    if (params.breaks) this.breaks = params.breaks as List
    if (params.labels) this.labels = params.labels as List<String>

    // Handle n.breaks parameter (overrides default from parent class)
    if (params.containsKey('n.breaks')) {
      this.nBreaks = (params['n.breaks'] as Number).intValue()
      this.nBreaksExplicitlySet = true
    } else if (params.containsKey('nBreaks')) {
      this.nBreaks = (params.nBreaks as Number).intValue()
      this.nBreaksExplicitlySet = true
    }
  }

  /**
   * Load ColorBrewer palette colors based on type and palette name.
   */
  private void loadPalette() {
    String resolvedPalette = resolvePaletteName()
    List<String> colors = BrewerPalettes.getPalette(resolvedPalette)

    if (colors == null || colors.isEmpty()) {
      // Fallback to default
      colors = BrewerPalettes.getPalette('Blues') ?: []
    }

    // Apply direction (reverse if needed)
    if (direction < 0) {
      paletteColors = colors.reverse()
    } else {
      paletteColors = new ArrayList<>(colors)
    }

    // Check if requested breaks exceeds palette size
    if (nBreaksExplicitlySet && nBreaks > paletteColors.size()) {
      log.warn("Warning: Number of breaks ($nBreaks) exceeds palette size (${paletteColors.size()}). Using maximum of ${paletteColors.size()} colors from palette.")
    }
  }

  /**
   * Resolve palette name from type if not explicitly provided.
   */
  private String resolvePaletteName() {
    if (palette != null && BrewerPalettes.getPalette(palette) != null) {
      return palette
    }

    // Use type-based defaults matching ggplot2
    switch (type?.toLowerCase()) {
      case 'div':
      case 'diverging':
        return 'Spectral'
      case 'qual':
      case 'qualitative':
        return 'Set1'
      case 'seq':
      case 'sequential':
      default:
        return 'Blues'
    }
  }

  @Override
  void train(List data) {
    super.train(data)

    // Ensure palette is loaded before computing bins
    if (paletteColors.isEmpty()) {
      loadPalette()
    }

    computeBinBoundaries()
  }

  /**
   * Compute bin boundaries based on domain and number of bins.
   */
  private void computeBinBoundaries() {
    if (computedDomain.size() < 2) return

    BigDecimal dMin = computedDomain[0]
    BigDecimal dMax = computedDomain[1]

    // Determine number of bins (use palette size if nBreaks not explicitly set)
    int numBins = nBreaksExplicitlySet ? nBreaks : paletteColors.size()
    numBins = 1.max(numBins.min(paletteColors.size())) as int

    // Create bin boundaries (numBins + 1 boundaries)
    binBoundaries = []
    for (int i = 0; i <= numBins; i++) {
      BigDecimal boundary = dMin + (dMax - dMin) * i / numBins
      binBoundaries << boundary
    }
  }

  @Override
  Object transform(Object value) {
    BigDecimal v = ScaleUtils.coerceToNumber(value)
    if (v == null) return naValue

    if (paletteColors.isEmpty()) {
      loadPalette()
    }
    if (paletteColors.isEmpty()) return naValue

    // Ensure bins are computed
    if (binBoundaries.isEmpty()) {
      computeBinBoundaries()
    }
    if (binBoundaries.size() < 2) return paletteColors[0]

    // Find which bin this value falls into
    int binIndex = findBinIndex(v)

    // Map bin to color (use palette colors without interpolation)
    int colorIndex = binIndex.min(paletteColors.size() - 1) as int
    return paletteColors[colorIndex]
  }

  /**
   * Find which bin a value falls into based on bin boundaries.
   *
   * Uses half-open intervals [lower, upper) for all bins except the last.
   * The last bin uses a closed interval to ensure values exactly equal to
   * the maximum boundary are included (handled by the check on line 260-262).
   *
   * This matches ggplot2's binning behavior for scale_*_fermenter().
   */
  private int findBinIndex(BigDecimal value) {
    if (value <= binBoundaries[0]) return 0
    if (value >= binBoundaries[binBoundaries.size() - 1]) {
      return binBoundaries.size() - 2
    }

    for (int i = 0; i < binBoundaries.size() - 1; i++) {
      if (value >= binBoundaries[i] && value < binBoundaries[i + 1]) {
        return i
      }
    }
    return binBoundaries.size() - 2
  }

  /**
   * Get the colors used by this scale.
   * @return list of hex colors
   */
  List<String> getColors() {
    if (paletteColors.isEmpty()) {
      loadPalette()
    }
    // Return only the colors actually used for bins
    int numBins = (nBreaksExplicitlySet ?
                  nBreaks.min(paletteColors.size()) :
                  paletteColors.size()) as int
    return paletteColors.take(numBins)
  }
}
