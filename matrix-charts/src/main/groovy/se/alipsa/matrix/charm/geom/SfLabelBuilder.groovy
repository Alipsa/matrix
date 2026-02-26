package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for simple feature label layers.
 *
 * <p>Produces a {@code SF_LABEL / SF_COORDINATES} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   layers {
 *     geomSfLabel().size(3).color('#000000')
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class SfLabelBuilder extends LayerBuilder {

  /**
   * Sets label text size.
   *
   * @param value size value
   * @return this builder
   */
  SfLabelBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets label text colour.
   *
   * @param value colour value
   * @return this builder
   */
  SfLabelBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets label text colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  SfLabelBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets label fill colour (background).
   *
   * @param value fill colour
   * @return this builder
   */
  SfLabelBuilder fill(String value) {
    params['fill'] = value
    this
  }

  /**
   * Sets label font family.
   *
   * @param value font family name
   * @return this builder
   */
  SfLabelBuilder family(String value) {
    params['family'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.SF_LABEL
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.SF_COORDINATES
  }
}
