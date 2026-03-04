package se.alipsa.matrix.gg.aes

import groovy.transform.CompileDynamic

/**
 * Closure delegate for building {@link Aes} mappings with unquoted column names.
 *
 * <p>Example:
 * <pre>{@code
 * aes { x = mpg; y = wt; color = cyl }
 * }</pre>
 *
 * <p>Bare identifiers on the right-hand side (for example {@code mpg}) are
 * resolved through {@link #propertyMissing(String)} and become column name
 * strings (for example {@code 'mpg'}).</p>
 */
@CompileDynamic
class AesDsl {

  // Known aesthetics kept as explicit fields for IDE completion.
  def x
  def y
  def color
  def fill
  def size
  def shape
  def alpha
  def linetype
  def linewidth
  def group
  def label
  def tooltip
  def weight
  def geometry
  def map_id

  /**
   * Alias for British spelling.
   *
   * @param value mapped value for the colour aesthetic
   */
  void setColour(def value) {
    color = value
  }

  /**
   * Alias for short {@code col} spelling, matching map-based {@code aes(Map)}.
   *
   * @param value mapped value for the color aesthetic
   */
  void setCol(def value) {
    color = value
  }

  /**
   * Resolves property reads for known aesthetics. If an aesthetic has not
   * been assigned yet, reading it returns its own name so expressions like
   * {@code x = x} map to column {@code 'x'}.
   */
  def getProperty(String name) {
    switch (name) {
      case 'x': this.@x != null ? this.@x : 'x'
      case 'y': this.@y != null ? this.@y : 'y'
      case 'color': this.@color != null ? this.@color : 'color'
      case 'colour': this.@color != null ? this.@color : 'colour'
      case 'col': this.@color != null ? this.@color : 'col'
      case 'fill': this.@fill != null ? this.@fill : 'fill'
      case 'size': this.@size != null ? this.@size : 'size'
      case 'shape': this.@shape != null ? this.@shape : 'shape'
      case 'alpha': this.@alpha != null ? this.@alpha : 'alpha'
      case 'linetype': this.@linetype != null ? this.@linetype : 'linetype'
      case 'linewidth': this.@linewidth != null ? this.@linewidth : 'linewidth'
      case 'group': this.@group != null ? this.@group : 'group'
      case 'label': this.@label != null ? this.@label : 'label'
      case 'tooltip': this.@tooltip != null ? this.@tooltip : 'tooltip'
      case 'weight': this.@weight != null ? this.@weight : 'weight'
      case 'geometry': this.@geometry != null ? this.@geometry : 'geometry'
      case 'map_id': this.@map_id != null ? this.@map_id : 'map_id'
      default: propertyMissing(name)
    }
  }

  /**
   * Resolves unknown properties to their own name so unquoted identifiers
   * become column mappings.
   *
   * @param name property name
   * @return the property name
   */
  String propertyMissing(String name) {
    name
  }

  /**
   * Convert configured non-null aesthetics to an {@link Aes} instance.
   *
   * @return a populated Aes object
   */
  Aes toAes() {
    Aes aes = new Aes()
    if (this.@x != null) aes.x = this.@x
    if (this.@y != null) aes.y = this.@y
    if (this.@color != null) aes.color = this.@color
    if (this.@fill != null) aes.fill = this.@fill
    if (this.@size != null) aes.size = this.@size
    if (this.@shape != null) aes.shape = this.@shape
    if (this.@alpha != null) aes.alpha = this.@alpha
    if (this.@linetype != null) aes.linetype = this.@linetype
    if (this.@linewidth != null) aes.linewidth = this.@linewidth
    if (this.@group != null) aes.group = this.@group
    if (this.@label != null) aes.label = this.@label
    if (this.@tooltip != null) aes.tooltip = this.@tooltip
    if (this.@weight != null) aes.weight = this.@weight
    if (this.@geometry != null) aes.geometry = this.@geometry
    if (this.@map_id != null) aes.map_id = this.@map_id
    aes
  }
}
