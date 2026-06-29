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
@SuppressWarnings(['CyclomaticComplexity', 'PropertyName'])
class AesDsl {

  private static final String COLOR_FIELD = 'color'
  private static final String LINEWIDTH_FIELD = 'linewidth'
  private static final String MAP_ID_FIELD = 'map_id'

  private static final Map<String, String> PROPERTY_FIELDS = [
      x        : 'x',
      y        : 'y',
      xend     : 'xend',
      yend     : 'yend',
      xmin     : 'xmin',
      xmax     : 'xmax',
      ymin     : 'ymin',
      ymax     : 'ymax',
      color    : COLOR_FIELD,
      colour   : COLOR_FIELD,
      col      : COLOR_FIELD,
      fill     : 'fill',
      size     : 'size',
      shape    : 'shape',
      alpha    : 'alpha',
      linetype : 'linetype',
      linewidth: LINEWIDTH_FIELD,
      lineWidth: LINEWIDTH_FIELD,
      group    : 'group',
      label    : 'label',
      tooltip  : 'tooltip',
      weight   : 'weight',
      geometry : 'geometry',
      map_id   : MAP_ID_FIELD,
      mapId    : MAP_ID_FIELD
  ]

  // Known aesthetics kept as explicit fields for IDE completion.
  def x
  def y
  def xend
  def yend
  def xmin
  def xmax
  def ymin
  def ymax
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
    String fieldName = PROPERTY_FIELDS[name]
    if (fieldName == null) {
      return propertyMissing(name)
    }
    def value = this.@"$fieldName"
    value != null ? value : name
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
    if (this.@x != null) {
      aes.x = this.@x
    }
    if (this.@y != null) {
      aes.y = this.@y
    }
    if (this.@xend != null) {
      aes.xend = this.@xend
    }
    if (this.@yend != null) {
      aes.yend = this.@yend
    }
    if (this.@xmin != null) {
      aes.xmin = this.@xmin
    }
    if (this.@xmax != null) {
      aes.xmax = this.@xmax
    }
    if (this.@ymin != null) {
      aes.ymin = this.@ymin
    }
    if (this.@ymax != null) {
      aes.ymax = this.@ymax
    }
    if (this.@color != null) {
      aes.color = this.@color
    }
    if (this.@fill != null) {
      aes.fill = this.@fill
    }
    if (this.@size != null) {
      aes.size = this.@size
    }
    if (this.@shape != null) {
      aes.shape = this.@shape
    }
    if (this.@alpha != null) {
      aes.alpha = this.@alpha
    }
    if (this.@linetype != null) {
      aes.linetype = this.@linetype
    }
    if (this.@linewidth != null) {
      aes.linewidth = this.@linewidth
    }
    if (this.@group != null) {
      aes.group = this.@group
    }
    if (this.@label != null) {
      aes.label = this.@label
    }
    if (this.@tooltip != null) {
      aes.tooltip = this.@tooltip
    }
    if (this.@weight != null) {
      aes.weight = this.@weight
    }
    if (this.@geometry != null) {
      aes.geometry = this.@geometry
    }
    if (this.@map_id != null) {
      aes.map_id = this.@map_id
    }
    aes
  }

  /**
   * @return true when at least one aesthetic has been explicitly set
   */
  boolean hasMappings() {
    this.@x != null ||
        this.@y != null ||
        this.@xend != null ||
        this.@yend != null ||
        this.@xmin != null ||
        this.@xmax != null ||
        this.@ymin != null ||
        this.@ymax != null ||
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
