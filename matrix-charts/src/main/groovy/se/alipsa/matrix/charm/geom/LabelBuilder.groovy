package se.alipsa.matrix.charm.geom

import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for label layers.
 *
 * <p>Produces a {@code LABEL / IDENTITY} layer specification.
 * Labels are like text but with a filled background rectangle.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y'; label = 'name' }
 *   layers {
 *     geomLabel().size(4).fill('#ffffff').color('#333333')
 *   }
 * }
 * }</pre>
 */
class LabelBuilder extends TextLayerBuilder<LabelBuilder> {

  /**
   * Sets label background fill colour.
   *
   * @param value fill colour
   * @return this builder
   */
  LabelBuilder fill(String value) {
    params['fill'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.LABEL
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }

}
