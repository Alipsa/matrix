package se.alipsa.matrix.gg.scale

import groovy.transform.CompileStatic

/**
 * Base class for scales.
 * Scales map data values to aesthetic values (positions, colors, sizes, etc.).
 * Subclasses should override these methods to provide actual implementations.
 */
@CompileStatic
class Scale {

  /** The aesthetic this scale applies to (x, y, color, fill, size, etc.) */
  String aesthetic

  /** Scale name (used in legend title) */
  String name

  /** Explicit limits [min, max] - null means auto from data */
  List limits

  /** Expansion factors [mult, add] applied to limits */
  List<Number> expand = [0.05, 0] as List<Number>

  /** Explicit breaks (tick positions) - null means auto */
  List breaks

  /** Labels for breaks - null means format from breaks */
  List<String> labels

  /** Guide specification (legend/colorbar configuration) */
  def guide

  /** Whether this scale has been trained on data */
  protected boolean trained = false

  /** Computed domain from training */
  protected List domain = []

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
}
