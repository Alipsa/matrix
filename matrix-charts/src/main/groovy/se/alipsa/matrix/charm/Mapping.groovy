package se.alipsa.matrix.charm

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Canonical aesthetic mappings for a plot or layer.
 */
@CompileStatic
class Mapping {

  private ColumnExpr x
  private ColumnExpr y
  private ColumnExpr color
  private ColumnExpr fill
  private ColumnExpr size
  private ColumnExpr shape
  private ColumnExpr group
  private ColumnExpr xend
  private ColumnExpr yend
  private ColumnExpr xmin
  private ColumnExpr xmax
  private ColumnExpr ymin
  private ColumnExpr ymax
  private ColumnExpr alpha
  private ColumnExpr linetype
  private ColumnExpr label
  private ColumnExpr tooltip
  private ColumnExpr weight

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
   * Returns xend mapping.
   *
   * @return xend mapping
   */
  ColumnExpr getXend() { xend }

  /**
   * Sets xend mapping.
   *
   * @param value mapping input
   */
  void setXend(Object value) { xend = coerceToColumnExpr(value, 'xend') }

  /**
   * Returns yend mapping.
   *
   * @return yend mapping
   */
  ColumnExpr getYend() { yend }

  /**
   * Sets yend mapping.
   *
   * @param value mapping input
   */
  void setYend(Object value) { yend = coerceToColumnExpr(value, 'yend') }

  /**
   * Returns xmin mapping.
   *
   * @return xmin mapping
   */
  ColumnExpr getXmin() { xmin }

  /**
   * Sets xmin mapping.
   *
   * @param value mapping input
   */
  void setXmin(Object value) { xmin = coerceToColumnExpr(value, 'xmin') }

  /**
   * Returns xmax mapping.
   *
   * @return xmax mapping
   */
  ColumnExpr getXmax() { xmax }

  /**
   * Sets xmax mapping.
   *
   * @param value mapping input
   */
  void setXmax(Object value) { xmax = coerceToColumnExpr(value, 'xmax') }

  /**
   * Returns ymin mapping.
   *
   * @return ymin mapping
   */
  ColumnExpr getYmin() { ymin }

  /**
   * Sets ymin mapping.
   *
   * @param value mapping input
   */
  void setYmin(Object value) { ymin = coerceToColumnExpr(value, 'ymin') }

  /**
   * Returns ymax mapping.
   *
   * @return ymax mapping
   */
  ColumnExpr getYmax() { ymax }

  /**
   * Sets ymax mapping.
   *
   * @param value mapping input
   */
  void setYmax(Object value) { ymax = coerceToColumnExpr(value, 'ymax') }

  /**
   * Returns alpha mapping.
   *
   * @return alpha mapping
   */
  ColumnExpr getAlpha() { alpha }

  /**
   * Sets alpha mapping.
   *
   * @param value mapping input
   */
  void setAlpha(Object value) { alpha = coerceToColumnExpr(value, 'alpha') }

  /**
   * Returns linetype mapping.
   *
   * @return linetype mapping
   */
  ColumnExpr getLinetype() { linetype }

  /**
   * Sets linetype mapping.
   *
   * @param value mapping input
   */
  void setLinetype(Object value) { linetype = coerceToColumnExpr(value, 'linetype') }

  /**
   * Returns label mapping.
   *
   * @return label mapping
   */
  ColumnExpr getLabel() { label }

  /**
   * Sets label mapping.
   *
   * @param value mapping input
   */
  void setLabel(Object value) { label = coerceToColumnExpr(value, 'label') }

  /**
   * Returns tooltip mapping.
   *
   * @return tooltip mapping
   */
  ColumnExpr getTooltip() { tooltip }

  /**
   * Sets tooltip mapping.
   *
   * @param value mapping input
   */
  void setTooltip(Object value) { tooltip = coerceToColumnExpr(value, 'tooltip') }

  /**
   * Returns weight mapping.
   *
   * @return weight mapping
   */
  ColumnExpr getWeight() { weight }

  /**
   * Sets weight mapping.
   *
   * @param value mapping input
   */
  void setWeight(Object value) { weight = coerceToColumnExpr(value, 'weight') }

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
        case 'xend' -> setXend(value)
        case 'yend' -> setYend(value)
        case 'xmin' -> setXmin(value)
        case 'xmax' -> setXmax(value)
        case 'ymin' -> setYmin(value)
        case 'ymax' -> setYmax(value)
        case 'alpha' -> setAlpha(value)
        case 'linetype' -> setLinetype(value)
        case 'label' -> setLabel(value)
        case 'tooltip' -> setTooltip(value)
        case 'weight' -> setWeight(value)
        default -> throw new CharmMappingException("Unsupported aesthetic '${key}'")
      }
    }
  }

  /**
   * Creates a shallow copy of this mapping.
   *
   * @return a copied mapping instance
   */
  Mapping copy() {
    Mapping cloned = new Mapping()
    cloned.x = x
    cloned.y = y
    cloned.color = color
    cloned.fill = fill
    cloned.size = size
    cloned.shape = shape
    cloned.group = group
    cloned.xend = xend
    cloned.yend = yend
    cloned.xmin = xmin
    cloned.xmax = xmax
    cloned.ymin = ymin
    cloned.ymax = ymax
    cloned.alpha = alpha
    cloned.linetype = linetype
    cloned.label = label
    cloned.tooltip = tooltip
    cloned.weight = weight
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
    if (xend != null) values.xend = xend
    if (yend != null) values.yend = yend
    if (xmin != null) values.xmin = xmin
    if (xmax != null) values.xmax = xmax
    if (ymin != null) values.ymin = ymin
    if (ymax != null) values.ymax = ymax
    if (alpha != null) values.alpha = alpha
    if (linetype != null) values.linetype = linetype
    if (label != null) values.label = label
    if (tooltip != null) values.tooltip = tooltip
    if (weight != null) values.weight = weight
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
      case 'xend' -> xend
      case 'yend' -> yend
      case 'xmin' -> xmin
      case 'xmax' -> xmax
      case 'ymin' -> ymin
      case 'ymax' -> ymax
      case 'alpha' -> alpha
      case 'linetype' -> linetype
      case 'label' -> label
      case 'tooltip' -> tooltip
      case 'weight' -> weight
      default -> null
    }
  }
}
