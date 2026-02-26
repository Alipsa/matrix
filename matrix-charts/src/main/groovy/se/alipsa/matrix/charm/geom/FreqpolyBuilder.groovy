package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for frequency polygon layers.
 *
 * <p>Produces a {@code FREQPOLY / BIN} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'value' }
 *   layers {
 *     geomFreqpoly().bins(20).color('#336699')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class FreqpolyBuilder extends LayerBuilder {

  /**
   * Sets line colour.
   *
   * @param value colour value
   * @return this builder
   */
  FreqpolyBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets line colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  FreqpolyBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets line width.
   *
   * @param value size value
   * @return this builder
   */
  FreqpolyBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  FreqpolyBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets the number of bins.
   *
   * @param value bin count
   * @return this builder
   */
  FreqpolyBuilder bins(Integer value) {
    params['bins'] = value
    this
  }

  /**
   * Sets the bin width.
   *
   * @param value bin width
   * @return this builder
   */
  FreqpolyBuilder binwidth(Number value) {
    params['binwidth'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.FREQPOLY
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.BIN
  }
}
