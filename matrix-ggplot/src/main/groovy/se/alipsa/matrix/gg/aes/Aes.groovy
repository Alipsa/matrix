package se.alipsa.matrix.gg.aes

import groovy.transform.CompileStatic

/**
 * Aesthetic mappings for ggplot charts.
 * Maps data columns to visual properties.
 *
 * All properties represent column names (String) or Identity wrappers for constants.
 * Use I(value) to specify a constant value instead of a column mapping.
 */
@CompileStatic
class Aes {

  // Position aesthetics
  /** X position - column name or I(value) */
  def x
  /** Y position - column name or I(value) */
  def y

  // Color aesthetics
  /** Outline color - column name or I(value) */
  def color
  /** Fill color - column name or I(value) */
  def fill

  // Point/shape aesthetics
  /** Size (for points, text) - column name or I(value) */
  def size
  /** Shape (for points) - column name or I(value) */
  def shape

  // Transparency
  /** Alpha/transparency (0-1) - column name or I(value) */
  def alpha

  // Line aesthetics
  /** Line type (solid, dashed, etc.) - column name or I(value) */
  def linetype
  /** Line width - column name or I(value) */
  def linewidth

  // Grouping
  /** Grouping variable for connecting observations */
  def group

  // Text
  /** Label text - column name or I(value) */
  def label
  /** Tooltip text - column name or I(value) */
  def tooltip

  // Statistical weights
  /** Weight for statistical computations - column name */
  def weight

  // Spatial
  /** Geometry column - column name or I(value) */
  def geometry
  /** Map id column for geom_map - column name or I(value) */
  def map_id

  // Legacy property names for backward compatibility
  // Using lowercase after 'get' so Groovy maps to xColName, not XColName
  String getxColName() { extractColName(x) }

  String getyColName() { extractColName(y) }

  String getColorColName() { extractColName(color) }

  String getFillColName() { extractColName(fill) }

  String getGroupColName() { extractColName(group) }

  String getLinetypeColName() { extractColName(linetype) }

  String getShapeColName() { extractColName(shape) }

  String getGeometryColName() { extractColName(geometry) }

  String getMapIdColName() { extractColName(map_id) }

  String getTooltipColName() { extractColName(tooltip) }

  Aes() {}

  Aes(List<String> colNames) {
    if (colNames.size() > 0) {
      x = colNames[0]
    }
    if (colNames.size() > 1) {
      y = colNames[1]
    }
  }

  Aes(List<String> colNames, String colorColumn) {
    this(colNames)
    this.color = colorColumn
  }

  Aes(Map params) {
    // Handle both old-style (xCol, yCol) and new-style (x, y) parameter names
    x = params.x ?: params.xCol
    y = params.y ?: params.yCol
    color = params.color ?: params.col ?: params.colour ?: params.colorCol
    fill = params.fill
    size = params.size
    shape = params.shape
    alpha = params.alpha
    linetype = params.linetype
    linewidth = params.linewidth ?: params.lineWidth
    group = params.group
    label = params.label
    tooltip = params.tooltip
    weight = params.weight
    geometry = params.geometry
    map_id = params.map_id ?: params.mapId
  }

  /**
   * Constructor with positional x, y and additional named parameters.
   * Allows syntax like: aes('cty', 'hwy', colour: 'class')
   * Note: Map must come first for Groovy to collect named parameters.
   */
  Aes(Map params, String xCol, String yCol) {
    this(params)
    // Override x and y with the positional arguments
    this.x = xCol
    this.y = yCol
  }

  /**
   * Get the value of an aesthetic by name.
   * This method provides type-safe access to aesthetic values without dynamic property access.
   * <p>
   * Supported aesthetic names: x, y, color, colour, fill, size, shape, alpha,
   * linetype, linewidth, group, label, tooltip, weight, geometry, map_id
   *
   * @param aesthetic the aesthetic name (e.g., 'x', 'y', 'color', 'fill', etc.)
   * @return the value of the aesthetic, or null if not found or unknown aesthetic name
   */
  Object getAestheticValue(String aesthetic) {
    switch (aesthetic) {
      case 'x': return x
      case 'y': return y
      case 'color': return color
      case 'colour': return color
      case 'fill': return fill
      case 'size': return size
      case 'shape': return shape
      case 'alpha': return alpha
      case 'linetype': return linetype
      case 'linewidth': return linewidth
      case 'group': return group
      case 'label': return label
      case 'tooltip': return tooltip
      case 'weight': return weight
      case 'geometry': return geometry
      case 'map_id': return map_id
      default: return null
    }
  }

  /**
   * Check if an aesthetic is a constant (wrapped in Identity) rather than a column mapping.
   */
  boolean isConstant(String aesthetic) {
    def value = getAestheticValue(aesthetic)
    return value instanceof Identity
  }

  /**
   * Get the constant value for an aesthetic (if it's an Identity wrapper).
   */
  def getConstantValue(String aesthetic) {
    def value = getAestheticValue(aesthetic)
    return value instanceof Identity ? ((Identity) value).value : null
  }

  /**
   * Check if an aesthetic references a computed statistic (wrapped in AfterStat).
   */
  boolean isAfterStat(String aesthetic) {
    def value = getAestheticValue(aesthetic)
    return value instanceof AfterStat
  }

  /**
   * Get the computed statistic name for an aesthetic (if it's an AfterStat wrapper).
   */
  String getAfterStatName(String aesthetic) {
    def value = getAestheticValue(aesthetic)
    return value instanceof AfterStat ? ((AfterStat) value).stat : null
  }

  /**
   * Check if an aesthetic references a scaled aesthetic (wrapped in AfterScale).
   */
  boolean isAfterScale(String aesthetic) {
    def value = getAestheticValue(aesthetic)
    return value instanceof AfterScale
  }

  /**
   * Get the referenced aesthetic name for after_scale.
   */
  String getAfterScaleName(String aesthetic) {
    def value = getAestheticValue(aesthetic)
    return value instanceof AfterScale ? ((AfterScale) value).aesthetic : null
  }

  /**
   * Check if an aesthetic is a closure-based expression.
   */
  boolean isExpression(String aesthetic) {
    def value = getAestheticValue(aesthetic)
    return value instanceof Expression || value instanceof Closure
  }

  /**
   * Check if an aesthetic is a factor wrapper.
   *
   * @param aesthetic the aesthetic name (e.g., 'x', 'fill')
   * @return true if the aesthetic is backed by a Factor wrapper
   */
  boolean isFactor(String aesthetic) {
    def value = getAestheticValue(aesthetic)
    return value instanceof Factor
  }

  /**
   * Get the Factor wrapper for an aesthetic.
   *
   * @param aesthetic the aesthetic name (e.g., 'x', 'fill')
   * @return the Factor instance or null if not a factor mapping
   */
  Factor getFactor(String aesthetic) {
    def value = getAestheticValue(aesthetic)
    return value instanceof Factor ? (Factor) value : null
  }

  /**
   * Check if an aesthetic is a cut_width wrapper for binning continuous data.
   *
   * @param aesthetic the aesthetic name (e.g., 'group', 'x')
   * @return true if the aesthetic is backed by a CutWidth wrapper
   */
  boolean isCutWidth(String aesthetic) {
    def value = getAestheticValue(aesthetic)
    return value instanceof CutWidth
  }

  /**
   * Get the CutWidth wrapper for an aesthetic.
   *
   * @param aesthetic the aesthetic name (e.g., 'group', 'x')
   * @return the CutWidth instance or null if not a cut_width mapping
   */
  CutWidth getCutWidth(String aesthetic) {
    def value = getAestheticValue(aesthetic)
    return value instanceof CutWidth ? (CutWidth) value : null
  }

  /**
   * Get the Expression wrapper for an aesthetic.
   * Wraps raw Closures in Expression for convenience.
   */
  Expression getExpression(String aesthetic) {
    def value = getAestheticValue(aesthetic)
    if (value instanceof Expression) {
      return (Expression) value
    }
    if (value instanceof Closure) {
      return new Expression((Closure<Number>) value)
    }
    return null
  }

  /**
   * Extract column name from a value (returns null for Identity, AfterStat, AfterScale, Expression, Factor, and CutWidth wrappers).
   */
  private static String extractColName(def value) {
    if (value == null) return null
    if (value instanceof Identity) return null
    if (value instanceof AfterStat) return null
    if (value instanceof AfterScale) return null
    if (value instanceof Expression) return null
    if (value instanceof Factor) return null
    if (value instanceof CutWidth) return null
    if (value instanceof Closure) return null
    return value.toString()
  }

  /**
   * Create a new Aes by merging this Aes with a base Aes.
   * Values from this Aes override values from the base.
   * This is used when a layer has its own aesthetic mappings that should
   * override the global aesthetics.
   * Note: merged aesthetics can still contain nulls and should be checked.
   *
   * @param base The base aesthetics (typically globalAes)
   * @return A new Aes with merged values
   */
  Aes merge(Aes base) {
    if (base == null) return this
    Aes result = new Aes()
    // Start with base values
    result.x = base.x
    result.y = base.y
    result.color = base.color
    result.fill = base.fill
    result.size = base.size
    result.shape = base.shape
    result.alpha = base.alpha
    result.linetype = base.linetype
    result.linewidth = base.linewidth
    result.group = base.group
    result.label = base.label
    result.tooltip = base.tooltip
    result.weight = base.weight
    result.geometry = base.geometry
    result.map_id = base.map_id
    // Override with this Aes's non-null values and corresponding column names
    if (this.x != null) {
      result.x = this.x
    }
    if (this.y != null) {
      result.y = this.y
    }
    if (this.color != null) {
      result.color = this.color
    }
    if (this.fill != null) {
      result.fill = this.fill
    }
    if (this.size != null) {
      result.size = this.size
    }
    if (this.shape != null) {
      result.shape = this.shape
    }
    if (this.alpha != null) {
      result.alpha = this.alpha
    }
    if (this.linetype != null) {
      result.linetype = this.linetype
    }
    if (this.linewidth != null) {
      result.linewidth = this.linewidth
    }
    if (this.group != null) {
      result.group = this.group
    }
    if (this.label != null) {
      result.label = this.label
    }
    if (this.tooltip != null) {
      result.tooltip = this.tooltip
    }
    if (this.weight != null) {
      result.weight = this.weight
    }
    if (this.geometry != null) {
      result.geometry = this.geometry
    }
    if (this.map_id != null) {
      result.map_id = this.map_id
    }
    return result
  }

  @Override
  String toString() {
    def parts = []
    if (x != null) parts << "xCol=$x"
    if (y != null) parts << "yCol=$y"
    if (color != null) parts << "colorCol=$color"
    if (fill != null) parts << "fillCol=$fill"
    if (size != null) parts << "size=$size"
    if (group != null) parts << "group=$group"
    if (tooltip != null) parts << "tooltip=$tooltip"
    if (geometry != null) parts << "geometry=$geometry"
    if (map_id != null) parts << "map_id=$map_id"
    return "Aes(${parts.join(', ')})"
  }
}
