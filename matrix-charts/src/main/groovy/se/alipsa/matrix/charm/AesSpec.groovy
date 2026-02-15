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
