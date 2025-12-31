package se.alipsa.matrix.gg.aes

import groovy.transform.CompileDynamic
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

  // Statistical weights
  /** Weight for statistical computations - column name */
  def weight

  // Legacy property names for backward compatibility
  // Using lowercase after 'get' so Groovy maps to xColName, not XColName
  String getxColName() { extractColName(x) }
  String getyColName() { extractColName(y) }
  String getColorColName() { extractColName(color) }
  String getFillColName() { extractColName(fill) }
  String getGroupColName() { extractColName(group) }

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
    weight = params.weight
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
   * Check if an aesthetic is a constant (wrapped in Identity) rather than a column mapping.
   */
  @CompileDynamic
  boolean isConstant(String aesthetic) {
    def value = this."$aesthetic"
    return value instanceof Identity
  }

  /**
   * Get the constant value for an aesthetic (if it's an Identity wrapper).
   */
  @CompileDynamic
  def getConstantValue(String aesthetic) {
    def value = this."$aesthetic"
    return value instanceof Identity ? value.value : null
  }

  /**
   * Check if an aesthetic references a computed statistic (wrapped in AfterStat).
   */
  @CompileDynamic
  boolean isAfterStat(String aesthetic) {
    def value = this."$aesthetic"
    return value instanceof AfterStat
  }

  /**
   * Get the computed statistic name for an aesthetic (if it's an AfterStat wrapper).
   */
  @CompileDynamic
  String getAfterStatName(String aesthetic) {
    def value = this."$aesthetic"
    return value instanceof AfterStat ? ((AfterStat) value).stat : null
  }

  /**
   * Extract column name from a value (returns null for Identity and AfterStat wrappers).
   */
  private static String extractColName(def value) {
    if (value == null) return null
    if (value instanceof Identity) return null
    if (value instanceof AfterStat) return null
    return value.toString()
  }

  /**
   * Create a new Aes by merging this Aes with a base Aes.
   * Values from this Aes override values from the base.
   * This is used when a layer has its own aesthetic mappings that should
   * override the global aesthetics.
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
    result.weight = base.weight
    // Also copy column name tracking fields from base
    result.xColName = base.xColName
    result.yColName = base.yColName
    result.colorColName = base.colorColName
    result.fillColName = base.fillColName
    result.sizeColName = base.sizeColName
    result.shapeColName = base.shapeColName
    result.alphaColName = base.alphaColName
    result.linetypeColName = base.linetypeColName
    result.linewidthColName = base.linewidthColName
    result.groupColName = base.groupColName
    result.labelColName = base.labelColName
    result.weightColName = base.weightColName
    // Override with this Aes's non-null values and corresponding column names
    if (this.x != null) {
      result.x = this.x
      result.xColName = this.xColName
    }
    if (this.y != null) {
      result.y = this.y
      result.yColName = this.yColName
    }
    if (this.color != null) {
      result.color = this.color
      result.colorColName = this.colorColName
    }
    if (this.fill != null) {
      result.fill = this.fill
      result.fillColName = this.fillColName
    }
    if (this.size != null) {
      result.size = this.size
      result.sizeColName = this.sizeColName
    }
    if (this.shape != null) {
      result.shape = this.shape
      result.shapeColName = this.shapeColName
    }
    if (this.alpha != null) {
      result.alpha = this.alpha
      result.alphaColName = this.alphaColName
    }
    if (this.linetype != null) {
      result.linetype = this.linetype
      result.linetypeColName = this.linetypeColName
    }
    if (this.linewidth != null) {
      result.linewidth = this.linewidth
      result.linewidthColName = this.linewidthColName
    }
    if (this.group != null) {
      result.group = this.group
      result.groupColName = this.groupColName
    }
    if (this.label != null) {
      result.label = this.label
      result.labelColName = this.labelColName
    }
    if (this.weight != null) {
      result.weight = this.weight
      result.weightColName = this.weightColName
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
    return "Aes(${parts.join(', ')})"
  }
}


