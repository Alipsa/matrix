package se.alipsa.matrix.charm

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Canonical aesthetic mappings for a plot or layer.
 */
@CompileStatic
class Aes {

  private final Cols col = new Cols()

  private ColumnExpr x
  private ColumnExpr y
  private ColumnExpr color
  private ColumnExpr fill
  private ColumnExpr size
  private ColumnExpr shape
  private ColumnExpr group

  /**
   * Returns the column namespace proxy for DSL usage.
   *
   * @return the `col` proxy
   */
  Cols getCol() {
    col
  }

  /**
   * Returns x mapping.
   *
   * @return x mapping
   */
  ColumnExpr getX() {
    x
  }

  /**
   * Sets x mapping from a ColumnExpr or CharSequence.
   *
   * @param value mapping input
   */
  void setX(Object value) {
    x = coerceToColumnExpr(value, 'x')
  }

  /**
   * Returns y mapping.
   *
   * @return y mapping
   */
  ColumnExpr getY() {
    y
  }

  /**
   * Sets y mapping from a ColumnExpr or CharSequence.
   *
   * @param value mapping input
   */
  void setY(Object value) {
    y = coerceToColumnExpr(value, 'y')
  }

  /**
   * Returns color mapping.
   *
   * @return color mapping
   */
  ColumnExpr getColor() {
    color
  }

  /**
   * Sets color mapping from a ColumnExpr or CharSequence.
   *
   * @param value mapping input
   */
  void setColor(Object value) {
    color = coerceToColumnExpr(value, 'color')
  }

  /**
   * Returns fill mapping.
   *
   * @return fill mapping
   */
  ColumnExpr getFill() {
    fill
  }

  /**
   * Sets fill mapping from a ColumnExpr or CharSequence.
   *
   * @param value mapping input
   */
  void setFill(Object value) {
    fill = coerceToColumnExpr(value, 'fill')
  }

  /**
   * Returns size mapping.
   *
   * @return size mapping
   */
  ColumnExpr getSize() {
    size
  }

  /**
   * Sets size mapping from a ColumnExpr or CharSequence.
   *
   * @param value mapping input
   */
  void setSize(Object value) {
    size = coerceToColumnExpr(value, 'size')
  }

  /**
   * Returns shape mapping.
   *
   * @return shape mapping
   */
  ColumnExpr getShape() {
    shape
  }

  /**
   * Sets shape mapping from a ColumnExpr or CharSequence.
   *
   * @param value mapping input
   */
  void setShape(Object value) {
    shape = coerceToColumnExpr(value, 'shape')
  }

  /**
   * Returns group mapping.
   *
   * @return group mapping
   */
  ColumnExpr getGroup() {
    group
  }

  /**
   * Sets group mapping from a ColumnExpr or CharSequence.
   *
   * @param value mapping input
   */
  void setGroup(Object value) {
    group = coerceToColumnExpr(value, 'group')
  }

  /**
   * Applies a named mapping map (`aes(x: 'cty', y: 'hwy')`).
   *
   * @param mapping the mapping map
   */
  void apply(Map<String, ?> mapping) {
    if (mapping == null) {
      return
    }
    mapping.each { String key, Object value ->
      switch (key) {
        case 'x' -> setX(value)
        case 'y' -> setY(value)
        case 'color', 'colour' -> setColor(value)
        case 'fill' -> setFill(value)
        case 'size' -> setSize(value)
        case 'shape' -> setShape(value)
        case 'group' -> setGroup(value)
        default -> throw new CharmMappingException("Unsupported aesthetic '${key}'")
      }
    }
  }

  /**
   * Creates a shallow copy of this mapping.
   *
   * @return a copied aes instance
   */
  Aes copy() {
    Aes cloned = new Aes()
    cloned.x = x
    cloned.y = y
    cloned.color = color
    cloned.fill = fill
    cloned.size = size
    cloned.shape = shape
    cloned.group = group
    cloned
  }

  /**
   * Returns all non-null mappings as a named map.
   *
   * @return aesthetic map
   */
  Map<String, ColumnExpr> mappings() {
    Map<String, ColumnExpr> values = [:]
    if (x != null) values.x = x
    if (y != null) values.y = y
    if (color != null) values.color = color
    if (fill != null) values.fill = fill
    if (size != null) values.size = size
    if (shape != null) values.shape = shape
    if (group != null) values.group = group
    values
  }

  /**
   * Coerces an input value to a column expression.
   *
   * @param value value to coerce
   * @param aestheticName aesthetic key used in diagnostics
   * @return a typed column expression
   */
  static ColumnExpr coerceToColumnExpr(Object value, String aestheticName = 'aes') {
    if (value == null) {
      return null
    }
    if (value instanceof ColumnExpr) {
      return value as ColumnExpr
    }
    if (value instanceof CharSequence) {
      return new ColumnRef(value.toString())
    }
    throw new CharmMappingException(
        "Unsupported mapping for '${aestheticName}': ${value.getClass().name}. " +
            'Supported types are ColumnExpr and CharSequence.'
    )
  }

  /**
   * Fallback dynamic property assignment hook for DSL closures.
   *
   * @param name property name
   * @param value property value
   */
  @CompileDynamic
  void propertyMissing(String name, Object value) {
    apply([(name): value])
  }

  /**
   * Fallback dynamic property read hook for DSL closures.
   *
   * @param name property name
   * @return property value
   */
  @CompileDynamic
  Object propertyMissing(String name) {
    switch (name) {
      case 'x' -> x
      case 'y' -> y
      case 'color', 'colour' -> color
      case 'fill' -> fill
      case 'size' -> size
      case 'shape' -> shape
      case 'group' -> group
      case 'col' -> col
      default -> null
    }
  }
}
