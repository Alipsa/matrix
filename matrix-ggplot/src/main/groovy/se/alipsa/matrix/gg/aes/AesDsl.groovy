package se.alipsa.matrix.gg.aes

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

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
@CompileStatic
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
   * Alias for camelCase {@code lineWidth}, matching map-based {@code aes(Map)}.
   *
   * @param value mapped value for linewidth
   */
  void setLineWidth(def value) {
    linewidth = value
  }

  /**
   * Alias for camelCase {@code mapId}, matching map-based {@code aes(Map)}.
   *
   * @param value mapped value for map id
   */
  void setMapId(def value) {
    map_id = value
  }

  /**
   * Resolves property reads for known aesthetics. If an aesthetic has not
   * been assigned yet, reading it returns its own name so expressions like
   * {@code x = x} map to column {@code 'x'}.
   */
  @CompileDynamic
  def getProperty(String name) {
    switch (name) {
      case 'x':
        return this.@x != null ? this.@x : 'x'
      case 'y':
        return this.@y != null ? this.@y : 'y'
      case 'color':
        return this.@color != null ? this.@color : 'color'
      case 'colour':
        return this.@color != null ? this.@color : 'colour'
      case 'col':
        return this.@color != null ? this.@color : 'col'
      case 'fill':
        return this.@fill != null ? this.@fill : 'fill'
      case 'size':
        return this.@size != null ? this.@size : 'size'
      case 'shape':
        return this.@shape != null ? this.@shape : 'shape'
      case 'alpha':
        return this.@alpha != null ? this.@alpha : 'alpha'
      case 'linetype':
        return this.@linetype != null ? this.@linetype : 'linetype'
      case 'linewidth':
        return this.@linewidth != null ? this.@linewidth : 'linewidth'
      case 'lineWidth':
        return this.@linewidth != null ? this.@linewidth : 'lineWidth'
      case 'group':
        return this.@group != null ? this.@group : 'group'
      case 'label':
        return this.@label != null ? this.@label : 'label'
      case 'tooltip':
        return this.@tooltip != null ? this.@tooltip : 'tooltip'
      case 'weight':
        return this.@weight != null ? this.@weight : 'weight'
      case 'geometry':
        return this.@geometry != null ? this.@geometry : 'geometry'
      case 'map_id':
        return this.@map_id != null ? this.@map_id : 'map_id'
      case 'mapId':
        return this.@map_id != null ? this.@map_id : 'mapId'
      default:
        return propertyMissing(name)
    }
  }

  /**
   * Resolves unknown properties to their own name so unquoted identifiers
   * become column mappings.
   *
   * @param name property name
   * @return the property name
   */
  @CompileDynamic
  String propertyMissing(String name) {
    name
  }

  /**
   * Convert configured non-null aesthetics to an {@link Aes} instance.
   *
   * @return a populated Aes object
   */
  @CompileDynamic
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

  /**
   * @return true when at least one aesthetic has been explicitly set
   */
  boolean hasMappings() {
    this.@x != null ||
        this.@y != null ||
        this.@color != null ||
        this.@fill != null ||
        this.@size != null ||
        this.@shape != null ||
        this.@alpha != null ||
        this.@linetype != null ||
        this.@linewidth != null ||
        this.@group != null ||
        this.@label != null ||
        this.@tooltip != null ||
        this.@weight != null ||
        this.@geometry != null ||
        this.@map_id != null
  }
}
