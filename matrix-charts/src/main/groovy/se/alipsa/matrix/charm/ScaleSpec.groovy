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
  private Scale size
  private Scale shape
  private Scale alpha
  private Scale linetype
  private Scale group

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
   * Returns size scale.
   *
   * @return size scale
   */
  Scale getSize() {
    size
  }

  /**
   * Sets size scale.
   *
   * @param size size scale
   */
  void setSize(Scale size) {
    this.size = size
  }

  /**
   * Builder-style size scale assignment.
   *
   * @param value size scale
   * @return this spec
   */
  ScaleSpec size(Scale value) {
    setSize(value)
    this
  }

  /**
   * Returns shape scale.
   *
   * @return shape scale
   */
  Scale getShape() {
    shape
  }

  /**
   * Sets shape scale.
   *
   * @param shape shape scale
   */
  void setShape(Scale shape) {
    this.shape = shape
  }

  /**
   * Builder-style shape scale assignment.
   *
   * @param value shape scale
   * @return this spec
   */
  ScaleSpec shape(Scale value) {
    setShape(value)
    this
  }

  /**
   * Returns alpha scale.
   *
   * @return alpha scale
   */
  Scale getAlpha() {
    alpha
  }

  /**
   * Sets alpha scale.
   *
   * @param alpha alpha scale
   */
  void setAlpha(Scale alpha) {
    this.alpha = alpha
  }

  /**
   * Builder-style alpha scale assignment.
   *
   * @param value alpha scale
   * @return this spec
   */
  ScaleSpec alpha(Scale value) {
    setAlpha(value)
    this
  }

  /**
   * Returns linetype scale.
   *
   * @return linetype scale
   */
  Scale getLinetype() {
    linetype
  }

  /**
   * Sets linetype scale.
   *
   * @param linetype linetype scale
   */
  void setLinetype(Scale linetype) {
    this.linetype = linetype
  }

  /**
   * Builder-style linetype scale assignment.
   *
   * @param value linetype scale
   * @return this spec
   */
  ScaleSpec linetype(Scale value) {
    setLinetype(value)
    this
  }

  /**
   * Returns group scale.
   *
   * @return group scale
   */
  Scale getGroup() {
    group
  }

  /**
   * Sets group scale.
   *
   * @param group group scale
   */
  void setGroup(Scale group) {
    this.group = group
  }

  /**
   * Builder-style group scale assignment.
   *
   * @param value group scale
   * @return this spec
   */
  ScaleSpec group(Scale value) {
    setGroup(value)
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
        fill: fill?.copy(),
        size: size?.copy(),
        shape: shape?.copy(),
        alpha: alpha?.copy(),
        linetype: linetype?.copy(),
        group: group?.copy()
    )
  }
}
