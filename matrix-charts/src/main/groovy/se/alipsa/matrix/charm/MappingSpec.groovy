package se.alipsa.matrix.charm

/**
 * Typed aesthetic mapping specification for Charm core.
 */
class MappingSpec extends Mapping {

  /**
   * Builder-style x mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec x(String value) { setX(value); this }

  /**
   * Builder-style x mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec x(ColumnExpr value) { setX(value); this }

  /**
   * Builder-style y mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec y(String value) { setY(value); this }

  /**
   * Builder-style y mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec y(ColumnExpr value) { setY(value); this }

  /**
   * Builder-style color mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec color(String value) { setColor(value); this }

  /**
   * Builder-style color mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec color(ColumnExpr value) { setColor(value); this }

  /**
   * Builder-style fill mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec fill(String value) { setFill(value); this }

  /**
   * Builder-style fill mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec fill(ColumnExpr value) { setFill(value); this }

  /**
   * Builder-style size mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec size(String value) { setSize(value); this }

  /**
   * Builder-style size mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec size(ColumnExpr value) { setSize(value); this }

  /**
   * Builder-style shape mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec shape(String value) { setShape(value); this }

  /**
   * Builder-style shape mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec shape(ColumnExpr value) { setShape(value); this }

  /**
   * Builder-style group mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec group(String value) { setGroup(value); this }

  /**
   * Builder-style group mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec group(ColumnExpr value) { setGroup(value); this }

  /**
   * Builder-style xend mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec xend(String value) { setXend(value); this }

  /**
   * Builder-style xend mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec xend(ColumnExpr value) { setXend(value); this }

  /**
   * Builder-style yend mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec yend(String value) { setYend(value); this }

  /**
   * Builder-style yend mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec yend(ColumnExpr value) { setYend(value); this }

  /**
   * Builder-style xmin mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec xmin(String value) { setXmin(value); this }

  /**
   * Builder-style xmin mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec xmin(ColumnExpr value) { setXmin(value); this }

  /**
   * Builder-style xmax mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec xmax(String value) { setXmax(value); this }

  /**
   * Builder-style xmax mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec xmax(ColumnExpr value) { setXmax(value); this }

  /**
   * Builder-style ymin mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec ymin(String value) { setYmin(value); this }

  /**
   * Builder-style ymin mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec ymin(ColumnExpr value) { setYmin(value); this }

  /**
   * Builder-style ymax mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec ymax(String value) { setYmax(value); this }

  /**
   * Builder-style ymax mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec ymax(ColumnExpr value) { setYmax(value); this }

  /**
   * Builder-style alpha mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec alpha(String value) { setAlpha(value); this }

  /**
   * Builder-style alpha mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec alpha(ColumnExpr value) { setAlpha(value); this }

  /**
   * Builder-style linetype mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec linetype(String value) { setLinetype(value); this }

  /**
   * Builder-style linetype mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec linetype(ColumnExpr value) { setLinetype(value); this }

  /**
   * Builder-style label mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec label(String value) { setLabel(value); this }

  /**
   * Builder-style label mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec label(ColumnExpr value) { setLabel(value); this }

  /**
   * Builder-style tooltip mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec tooltip(String value) { setTooltip(value); this }

  /**
   * Builder-style tooltip mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec tooltip(ColumnExpr value) { setTooltip(value); this }

  /**
   * Builder-style weight mapping assignment from a column name.
   *
   * @param value column name
   * @return this spec
   */
  MappingSpec weight(String value) { setWeight(value); this }

  /**
   * Builder-style weight mapping assignment from a column expression.
   *
   * @param value column expression
   * @return this spec
   */
  MappingSpec weight(ColumnExpr value) { setWeight(value); this }

  /**
   * Builder-style named mapping apply.
   *
   * @param mapping mapping map
   * @return this spec
   */
  MappingSpec mappings(Map<String, ?> mapping) {
    apply(mapping)
    this
  }

  /**
   * Copies this mapping as MappingSpec.
   *
   * @return copied mapping
   */
  @Override
  MappingSpec copy() {
    MappingSpec cloned = new MappingSpec()
    cloned.apply(mappings())
    cloned
  }

}
