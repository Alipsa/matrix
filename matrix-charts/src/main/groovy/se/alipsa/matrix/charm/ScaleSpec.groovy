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
