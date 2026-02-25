package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Discrete scale for categorical data.
 * Maps categorical values to positions, colors, or other aesthetic values.
 */
@CompileStatic
class ScaleDiscrete extends Scale {

  /** Output range [min, max] for position scales */
  List<Number> range = [0, 1] as List<Number>

  /** The unique levels in the data (in order) */
  protected List<Object> levels = []

  /** Drop unused levels from the scale */
  boolean drop = true

  List<Object> limits

  /** Optional ordering of levels */
  List<Object> orderLevels

  @Override
  void train(List data) {
    if (data == null || data.isEmpty()) return

    // Get unique values while preserving first occurrence order
    List<Object> uniqueValues = []
    data.each { val ->
      if (val != null && !uniqueValues.contains(val)) {
        uniqueValues << val
      }
    }

    // Apply explicit ordering if specified
    if (orderLevels) {
      levels = orderLevels.findAll { uniqueValues.contains(it) }
      // Add any remaining levels not in orderLevels
      uniqueValues.each { val ->
        if (!levels.contains(val)) {
          levels << val
        }
      }
    } else {
      // Sort if values are comparable, otherwise keep insertion order
      if (uniqueValues.every { it instanceof Comparable }) {
        levels = uniqueValues.sort { a, b -> (a as Comparable) <=> (b as Comparable) }
      } else {
        levels = uniqueValues
      }
    }

    // Apply explicit limits if set (allows subsetting)
    if (limits) {
      levels = levels.findAll { limits.contains(it) }
    }

    domain = levels
    trained = true
  }

  @Override
  Object transform(Object value) {
    if (value == null) return null
    if (levels.isEmpty()) return null

    int index = levels.indexOf(value)
    if (index < 0) return null

    // Map to position within range
    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal rMax = range[1] as BigDecimal

    if (levels.size() == 1) {
      return (rMin + rMax) / 2
    }

    // Calculate position with padding on each side
    // This creates bands with the value centered in each band
    BigDecimal bandWidth = (rMax - rMin) / levels.size()
    return rMin + bandWidth * (index + 0.5)
  }

  @Override
  Object inverse(Object value) {
    if (value == null) return null
    if (!(value instanceof Number)) return null
    if (levels.isEmpty()) return null

    BigDecimal v = value as BigDecimal
    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal rMax = range[1] as BigDecimal

    if (rMax == rMin) return levels[0]

    // Find which band this value falls into
    BigDecimal bandWidth = (rMax - rMin) / levels.size()
    int index = ((v - rMin) / bandWidth).floor() as int

    // Clamp to valid index range
    index = [0, index, levels.size() - 1].sort()[1]
    return levels[index]
  }

  @Override
  List getComputedBreaks() {
    if (breaks) return breaks
    return levels
  }

  @Override
  List<String> getComputedLabels() {
    if (labels) return labels
    return getComputedBreaks().collect { it?.toString() ?: '' }
  }

  /**
   * Get the bandwidth (width of each category band).
   * Useful for bar chart width calculations.
   */
  BigDecimal getBandwidth() {
    if (levels.isEmpty()) return 0
    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal rMax = range[1] as BigDecimal
    return (rMax - rMin).abs() / levels.size()
  }

  /**
   * Get the position of a level by index.
   */
  BigDecimal getPositionByIndex(int index) {
    if (index < 0 || index >= levels.size()) return 0
    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal bandWidth = getBandwidth()
    return rMin + bandWidth * (index + 0.5)
  }

  /**
   * Get all levels.
   */
  List<Object> getLevels() {
    return levels
  }

  /**
   * Get the number of levels.
   */
  int getLevelCount() {
    return levels.size()
  }

  @Override
  void reset() {
    super.reset()
    levels = []
  }

  /**
   * Build a palette map from domain values to colors.
   * Uses toString() on keys to ensure consistent lookup (handles GString vs String).
   *
   * @param colors list of colors corresponding to domain values
   * @return map from string keys to colors
   */
  protected Map<String, String> buildPaletteMap(List<String> colors) {
    if (domain == null || domain.isEmpty() || colors == null || colors.isEmpty()) {
      return [:]
    }
    return domain.withIndex().collectEntries { value, idx ->
      [(value.toString()): colors[idx % colors.size()]]
    } as Map<String, String>
  }

  /**
   * Look up a color in a palette map using toString() for consistent key matching.
   *
   * @param palette the palette map to look up in
   * @param value the value to look up
   * @param naValue the default value if not found
   * @return the color or naValue if not found
   */
  protected String lookupColor(Map<String, String> palette, Object value, String naValue) {
    if (value == null) return naValue
    String key = value.toString()
    return palette.containsKey(key) ? palette[key] : naValue
  }

  /**
   * Get colors in order of levels from a palette map.
   *
   * @param palette the palette map to get colors from
   * @param naValue the default value for missing colors
   * @return list of colors in level order
   */
  protected List<String> getColorsFromPalette(Map<String, String> palette, String naValue) {
    if (levels.isEmpty()) return []
    return levels.collect { Object level ->
      palette.get(level.toString()) ?: naValue
    }
  }

  Scale setLimits(List limits) {
    this.limits = limits
    this
  }

  void setRange(List<? extends Number> vals) {
    this.range = vals.collect{ it as Number }
  }

  List<Number> getRange() {
    return range
  }
}
