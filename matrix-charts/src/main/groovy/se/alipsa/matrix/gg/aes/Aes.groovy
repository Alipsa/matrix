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
   * Extract column name from a value (returns null for Identity wrappers).
   */
  private static String extractColName(def value) {
    if (value == null) return null
    if (value instanceof Identity) return null
    return value.toString()
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


