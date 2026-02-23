package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic
import se.alipsa.matrix.gg.coord.CoordTrans

/**
 * Base class for scales.
 * Scales map data values to aesthetic values (positions, colors, sizes, etc.).
 * Subclasses should override these methods to provide actual implementations.
 */
@CompileStatic
class Scale {

  /** The aesthetic this scale applies to (x, y, color, fill, size, etc.) */
  protected String aesthetic

  /** Scale name (used in legend title) */
  protected String name

  /** Explicit limits [min, max] - null means auto from data */
  protected List<BigDecimal> limits

  /** Expansion factors [mult, add] applied to limits (set to null to disable). */
  protected List<BigDecimal> expand = [0.05G, 0.0G]

  /** Explicit breaks (tick positions) - null means auto */
  protected List breaks

  /** Labels for breaks - null means format from breaks */
  protected List<String> labels

  /** Guide specification (legend/colorbar configuration) */
  protected def guide

  /** Whether this scale has been trained on data */
  protected boolean trained = false

  /** Computed domain from training */
  protected List domain = []

  /**
   * Reference to CoordTrans if coordinate transformations are being used.
   * Used for inverse-transforming break values to display original data values as labels.
   */
  protected CoordTrans coordTrans

  /**
   * Train the scale on data to determine domain/range.
   * @param data List of values from the data
   */
  void train(List data) {
    if (data) {
      domain = data.unique()
      trained = true
    }
  }

  /**
   * Map data values to aesthetic values.
   * @param values List of data values
   * @return List of mapped aesthetic values
   */
  List map(List values) {
    return values.collect { transform(it) }
  }

  /**
   * Transform a single value from data space to aesthetic space.
   * @param value Data value
   * @return Aesthetic value
   */
  Object transform(Object value) {
    return value  // Identity by default
  }

  /**
   * Inverse transform from aesthetic space to data space.
   * @param value Aesthetic value
   * @return Data value
   */
  Object inverse(Object value) {
    return value  // Identity by default
  }

  /**
   * Get the computed breaks (tick positions).
   * @return List of break values in data space
   */
  List getComputedBreaks() {
    return breaks ?: domain
  }

  /**
   * Get the computed labels for breaks.
   * @return List of label strings
   */
  List<String> getComputedLabels() {
    if (labels) return labels
    return getComputedBreaks().collect { it?.toString() ?: '' }
  }

  /**
   * Check if this scale is trained (has seen data).
   */
  boolean isTrained() {
    return trained
  }

  /**
   * Reset the scale to untrained state.
   */
  void reset() {
    trained = false
    domain = []
  }

  /**
   * Get the domain of this scale.
   * @return List of domain values
   */
  List getDomain() {
    return domain
  }

  void setAesthetic(String aesthetic) {
    this.aesthetic = aesthetic
  }

  String getAesthetic() {
    return aesthetic
  }

  void setExpand(List<? extends Number> vals) {
    if (vals == null) {
      this.expand = null
    }
    else {
      this.expand = vals.collect { it as BigDecimal }
    }
  }

  List<BigDecimal> getExpand() {
    return expand
  }

  void setGuide(guide) {
    this.guide = guide
  }

  def getGuide() {
    return guide
  }

  String getName() {
    return name
  }

  Scale limits(Number... vals) {
    if (vals == null) {
      this.limits = null
    }
    else {
      this.limits = vals.collect { it as BigDecimal }
    }
    this
  }

  Scale setLimits(List<? extends Number> vals) {
    if (vals == null) {
      this.limits = null
    }
    else {
      this.limits = vals.collect { it as BigDecimal }
    }
    this
  }

  List<BigDecimal> getLimits() {
    return limits
  }

  List<String> getLabels() {
    return labels
  }

  Scale setLabels(List<String> labels) {
    this.labels = labels
    this
  }

  List getBreaks() {
    return breaks
  }

  Scale setBreaks(List breaks) {
    this.breaks = breaks
    this
  }

  void setCoordTrans(CoordTrans coordTrans) {
    this.coordTrans = coordTrans
  }

  CoordTrans getCoordTrans() {
    return coordTrans
  }

  Scale setName(String name) {
    this.name = name
    this
  }
}
