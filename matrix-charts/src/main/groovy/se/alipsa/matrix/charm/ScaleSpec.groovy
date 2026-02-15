package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Grouped scale specification for x/y/color/fill.
 */
@CompileStatic
class ScaleSpec {

  private Scale x
  private Scale y
  private Scale color
  private Scale fill

  /**
   * Returns x scale.
   *
   * @return x scale
   */
  Scale getX() {
    x
  }

  /**
   * Sets x scale.
   *
   * @param x x scale
   */
  void setX(Scale x) {
    this.x = x
  }

  /**
   * Builder-style x scale assignment.
   *
   * @param value x scale
   * @return this spec
   */
  ScaleSpec x(Scale value) {
    setX(value)
    this
  }

  /**
   * Returns y scale.
   *
   * @return y scale
   */
  Scale getY() {
    y
  }

  /**
   * Sets y scale.
   *
   * @param y y scale
   */
  void setY(Scale y) {
    this.y = y
  }

  /**
   * Builder-style y scale assignment.
   *
   * @param value y scale
   * @return this spec
   */
  ScaleSpec y(Scale value) {
    setY(value)
    this
  }

  /**
   * Returns color scale.
   *
   * @return color scale
   */
  Scale getColor() {
    color
  }

  /**
   * Sets color scale.
   *
   * @param color color scale
   */
  void setColor(Scale color) {
    this.color = color
  }

  /**
   * Builder-style color scale assignment.
   *
   * @param value color scale
   * @return this spec
   */
  ScaleSpec color(Scale value) {
    setColor(value)
    this
  }

  /**
   * Returns fill scale.
   *
   * @return fill scale
   */
  Scale getFill() {
    fill
  }

  /**
   * Sets fill scale.
   *
   * @param fill fill scale
   */
  void setFill(Scale fill) {
    this.fill = fill
  }

  /**
   * Builder-style fill scale assignment.
   *
   * @param value fill scale
   * @return this spec
   */
  ScaleSpec fill(Scale value) {
    setFill(value)
    this
  }

  /**
   * Copies this scale spec.
   *
   * @return copied scale spec
   */
  ScaleSpec copy() {
    new ScaleSpec(
        x: x?.copy(),
        y: y?.copy(),
        color: color?.copy(),
        fill: fill?.copy()
    )
  }
}
