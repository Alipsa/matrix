package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Typed aesthetic mapping specification for Charm core.
 */
@CompileStatic
class MappingSpec extends Mapping {

  /**
   * Builder-style x mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec x(Object value) {
    setX(value)
    this
  }

  /**
   * Builder-style y mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec y(Object value) {
    setY(value)
    this
  }

  /**
   * Builder-style color mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec color(Object value) {
    setColor(value)
    this
  }

  /**
   * Builder-style fill mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec fill(Object value) {
    setFill(value)
    this
  }

  /**
   * Builder-style size mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec size(Object value) {
    setSize(value)
    this
  }

  /**
   * Builder-style shape mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec shape(Object value) {
    setShape(value)
    this
  }

  /**
   * Builder-style group mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec group(Object value) {
    setGroup(value)
    this
  }

  /**
   * Builder-style xend mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec xend(Object value) {
    setXend(value)
    this
  }

  /**
   * Builder-style yend mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec yend(Object value) {
    setYend(value)
    this
  }

  /**
   * Builder-style xmin mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec xmin(Object value) {
    setXmin(value)
    this
  }

  /**
   * Builder-style xmax mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec xmax(Object value) {
    setXmax(value)
    this
  }

  /**
   * Builder-style ymin mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec ymin(Object value) {
    setYmin(value)
    this
  }

  /**
   * Builder-style ymax mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec ymax(Object value) {
    setYmax(value)
    this
  }

  /**
   * Builder-style alpha mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec alpha(Object value) {
    setAlpha(value)
    this
  }

  /**
   * Builder-style linetype mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec linetype(Object value) {
    setLinetype(value)
    this
  }

  /**
   * Builder-style label mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec label(Object value) {
    setLabel(value)
    this
  }

  /**
   * Builder-style weight mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  MappingSpec weight(Object value) {
    setWeight(value)
    this
  }

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
