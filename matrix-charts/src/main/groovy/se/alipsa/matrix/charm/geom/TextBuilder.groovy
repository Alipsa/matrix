package se.alipsa.matrix.charm.geom

import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for text layers.
 *
 * <p>Produces a {@code TEXT / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y'; label = 'name' }
 *   layers {
 *     geomText().size(4).color('#333333')
 *   }
 * }
 * }</pre>
 */
class TextBuilder extends TextLayerBuilder<TextBuilder> {

  /**
   * Sets the label text content.
   *
   * @param value label text
   * @return this builder
   */
  TextBuilder label(String value) {
    params['label'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.TEXT
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }

}
