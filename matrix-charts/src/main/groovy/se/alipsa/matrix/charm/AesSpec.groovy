package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Typed aesthetic mapping specification for Charm core.
 */
@CompileStatic
class AesSpec extends Aes {

  /**
   * Builder-style x mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec x(Object value) {
    setX(value)
    this
  }

  /**
   * Builder-style y mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec y(Object value) {
    setY(value)
    this
  }

  /**
   * Builder-style color mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec color(Object value) {
    setColor(value)
    this
  }

  /**
   * Builder-style fill mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec fill(Object value) {
    setFill(value)
    this
  }

  /**
   * Builder-style size mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec size(Object value) {
    setSize(value)
    this
  }

  /**
   * Builder-style shape mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec shape(Object value) {
    setShape(value)
    this
  }

  /**
   * Builder-style group mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec group(Object value) {
    setGroup(value)
    this
  }

  /**
   * Builder-style xend mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec xend(Object value) {
    setXend(value)
    this
  }

  /**
   * Builder-style yend mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec yend(Object value) {
    setYend(value)
    this
  }

  /**
   * Builder-style xmin mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec xmin(Object value) {
    setXmin(value)
    this
  }

  /**
   * Builder-style xmax mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec xmax(Object value) {
    setXmax(value)
    this
  }

  /**
   * Builder-style ymin mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec ymin(Object value) {
    setYmin(value)
    this
  }

  /**
   * Builder-style ymax mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec ymax(Object value) {
    setYmax(value)
    this
  }

  /**
   * Builder-style alpha mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec alpha(Object value) {
    setAlpha(value)
    this
  }

  /**
   * Builder-style linetype mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec linetype(Object value) {
    setLinetype(value)
    this
  }

  /**
   * Builder-style label mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec label(Object value) {
    setLabel(value)
    this
  }

  /**
   * Builder-style weight mapping assignment.
   *
   * @param value mapping value
   * @return this spec
   */
  AesSpec weight(Object value) {
    setWeight(value)
    this
  }

  /**
   * Builder-style named mapping apply.
   *
   * @param mapping mapping map
   * @return this spec
   */
  AesSpec mappings(Map<String, ?> mapping) {
    apply(mapping)
    this
  }

  /**
   * Copies this mapping as AesSpec.
   *
   * @return copied mapping
   */
  @Override
  AesSpec copy() {
    AesSpec cloned = new AesSpec()
    cloned.apply(mappings())
    cloned
  }
}
