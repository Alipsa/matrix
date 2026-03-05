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

  // Typed setter overloads improve IDE completion while preserving wrapper support.
  void setX(String columnName) { this.@x = columnName }
  void setX(Identity constant) { this.@x = constant }
  void setX(Factor factor) { this.@x = factor }
  void setX(Expression expr) { this.@x = expr }
  void setX(AfterStat stat) { this.@x = stat }
  void setX(AfterScale scale) { this.@x = scale }
  void setX(CutWidth cut) { this.@x = cut }
  void setX(Closure expression) { this.@x = expression }

  void setY(String columnName) { this.@y = columnName }
  void setY(Identity constant) { this.@y = constant }
  void setY(Factor factor) { this.@y = factor }
  void setY(Expression expr) { this.@y = expr }
  void setY(AfterStat stat) { this.@y = stat }
  void setY(AfterScale scale) { this.@y = scale }
  void setY(CutWidth cut) { this.@y = cut }
  void setY(Closure expression) { this.@y = expression }

  void setColor(String columnName) { this.@color = columnName }
  void setColor(Identity constant) { this.@color = constant }
  void setColor(Factor factor) { this.@color = factor }
  void setColor(Expression expr) { this.@color = expr }
  void setColor(AfterStat stat) { this.@color = stat }
  void setColor(AfterScale scale) { this.@color = scale }
  void setColor(CutWidth cut) { this.@color = cut }
  void setColor(Closure expression) { this.@color = expression }

  void setColour(String columnName) { setColor(columnName) }
  void setColour(Identity constant) { setColor(constant) }
  void setColour(Factor factor) { setColor(factor) }
  void setColour(Expression expr) { setColor(expr) }
  void setColour(AfterStat stat) { setColor(stat) }
  void setColour(AfterScale scale) { setColor(scale) }
  void setColour(CutWidth cut) { setColor(cut) }
  void setColour(Closure expression) { setColor(expression) }

  void setCol(String columnName) { setColor(columnName) }
  void setCol(Identity constant) { setColor(constant) }
  void setCol(Factor factor) { setColor(factor) }
  void setCol(Expression expr) { setColor(expr) }
  void setCol(AfterStat stat) { setColor(stat) }
  void setCol(AfterScale scale) { setColor(scale) }
  void setCol(CutWidth cut) { setColor(cut) }
  void setCol(Closure expression) { setColor(expression) }

  void setFill(String columnName) { this.@fill = columnName }
  void setFill(Identity constant) { this.@fill = constant }
  void setFill(Factor factor) { this.@fill = factor }
  void setFill(Expression expr) { this.@fill = expr }
  void setFill(AfterStat stat) { this.@fill = stat }
  void setFill(AfterScale scale) { this.@fill = scale }
  void setFill(CutWidth cut) { this.@fill = cut }
  void setFill(Closure expression) { this.@fill = expression }

  void setSize(String columnName) { this.@size = columnName }
  void setSize(Identity constant) { this.@size = constant }
  void setSize(Factor factor) { this.@size = factor }
  void setSize(Expression expr) { this.@size = expr }
  void setSize(AfterStat stat) { this.@size = stat }
  void setSize(AfterScale scale) { this.@size = scale }
  void setSize(CutWidth cut) { this.@size = cut }
  void setSize(Closure expression) { this.@size = expression }

  void setShape(String columnName) { this.@shape = columnName }
  void setShape(Identity constant) { this.@shape = constant }
  void setShape(Factor factor) { this.@shape = factor }
  void setShape(Expression expr) { this.@shape = expr }
  void setShape(AfterStat stat) { this.@shape = stat }
  void setShape(AfterScale scale) { this.@shape = scale }
  void setShape(CutWidth cut) { this.@shape = cut }
  void setShape(Closure expression) { this.@shape = expression }

  void setAlpha(String columnName) { this.@alpha = columnName }
  void setAlpha(Identity constant) { this.@alpha = constant }
  void setAlpha(Factor factor) { this.@alpha = factor }
  void setAlpha(Expression expr) { this.@alpha = expr }
  void setAlpha(AfterStat stat) { this.@alpha = stat }
  void setAlpha(AfterScale scale) { this.@alpha = scale }
  void setAlpha(CutWidth cut) { this.@alpha = cut }
  void setAlpha(Closure expression) { this.@alpha = expression }

  void setLinetype(String columnName) { this.@linetype = columnName }
  void setLinetype(Identity constant) { this.@linetype = constant }
  void setLinetype(Factor factor) { this.@linetype = factor }
  void setLinetype(Expression expr) { this.@linetype = expr }
  void setLinetype(AfterStat stat) { this.@linetype = stat }
  void setLinetype(AfterScale scale) { this.@linetype = scale }
  void setLinetype(CutWidth cut) { this.@linetype = cut }
  void setLinetype(Closure expression) { this.@linetype = expression }

  void setLinewidth(String columnName) { this.@linewidth = columnName }
  void setLinewidth(Identity constant) { this.@linewidth = constant }
  void setLinewidth(Factor factor) { this.@linewidth = factor }
  void setLinewidth(Expression expr) { this.@linewidth = expr }
  void setLinewidth(AfterStat stat) { this.@linewidth = stat }
  void setLinewidth(AfterScale scale) { this.@linewidth = scale }
  void setLinewidth(CutWidth cut) { this.@linewidth = cut }
  void setLinewidth(Closure expression) { this.@linewidth = expression }

  void setLineWidth(String columnName) { setLinewidth(columnName) }
  void setLineWidth(Identity constant) { setLinewidth(constant) }
  void setLineWidth(Factor factor) { setLinewidth(factor) }
  void setLineWidth(Expression expr) { setLinewidth(expr) }
  void setLineWidth(AfterStat stat) { setLinewidth(stat) }
  void setLineWidth(AfterScale scale) { setLinewidth(scale) }
  void setLineWidth(CutWidth cut) { setLinewidth(cut) }
  void setLineWidth(Closure expression) { setLinewidth(expression) }

  void setGroup(String columnName) { this.@group = columnName }
  void setGroup(Identity constant) { this.@group = constant }
  void setGroup(Factor factor) { this.@group = factor }
  void setGroup(Expression expr) { this.@group = expr }
  void setGroup(AfterStat stat) { this.@group = stat }
  void setGroup(AfterScale scale) { this.@group = scale }
  void setGroup(CutWidth cut) { this.@group = cut }
  void setGroup(Closure expression) { this.@group = expression }

  void setLabel(String columnName) { this.@label = columnName }
  void setLabel(Identity constant) { this.@label = constant }
  void setLabel(Factor factor) { this.@label = factor }
  void setLabel(Expression expr) { this.@label = expr }
  void setLabel(AfterStat stat) { this.@label = stat }
  void setLabel(AfterScale scale) { this.@label = scale }
  void setLabel(CutWidth cut) { this.@label = cut }
  void setLabel(Closure expression) { this.@label = expression }

  void setTooltip(String columnName) { this.@tooltip = columnName }
  void setTooltip(Identity constant) { this.@tooltip = constant }
  void setTooltip(Factor factor) { this.@tooltip = factor }
  void setTooltip(Expression expr) { this.@tooltip = expr }
  void setTooltip(AfterStat stat) { this.@tooltip = stat }
  void setTooltip(AfterScale scale) { this.@tooltip = scale }
  void setTooltip(CutWidth cut) { this.@tooltip = cut }
  void setTooltip(Closure expression) { this.@tooltip = expression }

  void setWeight(String columnName) { this.@weight = columnName }
  void setWeight(Identity constant) { this.@weight = constant }
  void setWeight(Factor factor) { this.@weight = factor }
  void setWeight(Expression expr) { this.@weight = expr }
  void setWeight(AfterStat stat) { this.@weight = stat }
  void setWeight(AfterScale scale) { this.@weight = scale }
  void setWeight(CutWidth cut) { this.@weight = cut }
  void setWeight(Closure expression) { this.@weight = expression }

  void setGeometry(String columnName) { this.@geometry = columnName }
  void setGeometry(Identity constant) { this.@geometry = constant }
  void setGeometry(Factor factor) { this.@geometry = factor }
  void setGeometry(Expression expr) { this.@geometry = expr }
  void setGeometry(AfterStat stat) { this.@geometry = stat }
  void setGeometry(AfterScale scale) { this.@geometry = scale }
  void setGeometry(CutWidth cut) { this.@geometry = cut }
  void setGeometry(Closure expression) { this.@geometry = expression }

  void setMap_id(String columnName) { this.@map_id = columnName }
  void setMap_id(Identity constant) { this.@map_id = constant }
  void setMap_id(Factor factor) { this.@map_id = factor }
  void setMap_id(Expression expr) { this.@map_id = expr }
  void setMap_id(AfterStat stat) { this.@map_id = stat }
  void setMap_id(AfterScale scale) { this.@map_id = scale }
  void setMap_id(CutWidth cut) { this.@map_id = cut }
  void setMap_id(Closure expression) { this.@map_id = expression }

  void setMapId(String columnName) { setMap_id(columnName) }
  void setMapId(Identity constant) { setMap_id(constant) }
  void setMapId(Factor factor) { setMap_id(factor) }
  void setMapId(Expression expr) { setMap_id(expr) }
  void setMapId(AfterStat stat) { setMap_id(stat) }
  void setMapId(AfterScale scale) { setMap_id(scale) }
  void setMapId(CutWidth cut) { setMap_id(cut) }
  void setMapId(Closure expression) { setMap_id(expression) }

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
      setX(colNames[0])
    }
    if (colNames.size() > 1) {
      setY(colNames[1])
    }
  }

  Aes(List<String> colNames, String colorColumn) {
    this(colNames)
    setColor(colorColumn)
  }

  Aes(Map params) {
    // Handle both old-style (xCol, yCol) and new-style (x, y) parameter names
    def xValue = params.x != null ? params.x : params.xCol
    if (xValue != null) assignAesthetic('x', xValue)

    def yValue = params.y != null ? params.y : params.yCol
    if (yValue != null) assignAesthetic('y', yValue)

    def colorValue = params.color != null ? params.color : (params.col != null ? params.col : (params.colour != null ? params.colour : params.colorCol))
    if (colorValue != null) assignAesthetic('color', colorValue)
    if (params.fill != null) assignAesthetic('fill', params.fill)
    if (params.size != null) assignAesthetic('size', params.size)
    if (params.shape != null) assignAesthetic('shape', params.shape)
    if (params.alpha != null) assignAesthetic('alpha', params.alpha)
    if (params.linetype != null) assignAesthetic('linetype', params.linetype)

    def linewidthValue = params.linewidth != null ? params.linewidth : params.lineWidth
    if (linewidthValue != null) assignAesthetic('linewidth', linewidthValue)

    if (params.group != null) assignAesthetic('group', params.group)
    if (params.label != null) assignAesthetic('label', params.label)
    if (params.tooltip != null) assignAesthetic('tooltip', params.tooltip)
    if (params.weight != null) assignAesthetic('weight', params.weight)
    if (params.geometry != null) assignAesthetic('geometry', params.geometry)

    def mapIdValue = params.map_id != null ? params.map_id : params.mapId
    if (mapIdValue != null) assignAesthetic('map_id', mapIdValue)
  }

  /**
   * Constructor with positional x, y and additional named parameters.
   * Allows syntax like: aes('cty', 'hwy', colour: 'class')
   * Note: Map must come first for Groovy to collect named parameters.
   */
  Aes(Map params, String xCol, String yCol) {
    this(params)
    // Override x and y with the positional arguments
    setX(xCol)
    setY(yCol)
  }

  /**
   * Assign through typed setters when possible, with direct-field fallback
   * for legacy value types that do not have explicit typed overloads.
   */
  private void assignAesthetic(String name, def value) {
    try {
      setProperty(name, value)
    } catch (MissingMethodException ignored) {
      switch (name) {
        case 'x' -> this.@x = value
        case 'y' -> this.@y = value
        case 'color' -> this.@color = value
        case 'fill' -> this.@fill = value
        case 'size' -> this.@size = value
        case 'shape' -> this.@shape = value
        case 'alpha' -> this.@alpha = value
        case 'linetype' -> this.@linetype = value
        case 'linewidth' -> this.@linewidth = value
        case 'group' -> this.@group = value
        case 'label' -> this.@label = value
        case 'tooltip' -> this.@tooltip = value
        case 'weight' -> this.@weight = value
        case 'geometry' -> this.@geometry = value
        case 'map_id' -> this.@map_id = value
        default -> throw ignored
      }
    }
  }

  /**
   * Assign an aesthetic by name, using typed setter overloads when available.
   */
  void setAesthetic(String name, def value) {
    assignAesthetic(name, value)
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
      case 'x' -> x
      case 'y' -> y
      case 'color', 'colour' -> color
      case 'fill' -> fill
      case 'size' -> size
      case 'shape' -> shape
      case 'alpha' -> alpha
      case 'linetype' -> linetype
      case 'linewidth' -> linewidth
      case 'group' -> group
      case 'label' -> label
      case 'tooltip' -> tooltip
      case 'weight' -> weight
      case 'geometry' -> geometry
      case 'map_id' -> map_id
      default -> null
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
      return new Expression((Closure) value)
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
    result.@x = base.x
    result.@y = base.y
    result.@color = base.color
    result.@fill = base.fill
    result.@size = base.size
    result.@shape = base.shape
    result.@alpha = base.alpha
    result.@linetype = base.linetype
    result.@linewidth = base.linewidth
    result.@group = base.group
    result.@label = base.label
    result.@tooltip = base.tooltip
    result.@weight = base.weight
    result.@geometry = base.geometry
    result.@map_id = base.map_id
    // Override with this Aes's non-null values and corresponding column names
    if (this.x != null) {
      result.@x = this.x
    }
    if (this.y != null) {
      result.@y = this.y
    }
    if (this.color != null) {
      result.@color = this.color
    }
    if (this.fill != null) {
      result.@fill = this.fill
    }
    if (this.size != null) {
      result.@size = this.size
    }
    if (this.shape != null) {
      result.@shape = this.shape
    }
    if (this.alpha != null) {
      result.@alpha = this.alpha
    }
    if (this.linetype != null) {
      result.@linetype = this.linetype
    }
    if (this.linewidth != null) {
      result.@linewidth = this.linewidth
    }
    if (this.group != null) {
      result.@group = this.group
    }
    if (this.label != null) {
      result.@label = this.label
    }
    if (this.tooltip != null) {
      result.@tooltip = this.tooltip
    }
    if (this.weight != null) {
      result.@weight = this.weight
    }
    if (this.geometry != null) {
      result.@geometry = this.geometry
    }
    if (this.map_id != null) {
      result.@map_id = this.map_id
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
