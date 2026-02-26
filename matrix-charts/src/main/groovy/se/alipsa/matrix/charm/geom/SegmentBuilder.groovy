package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for segment layers.
 *
 * <p>Produces a {@code SEGMENT / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y'; xend = 'xend'; yend = 'yend' }
 *   layers {
 *     geomSegment().color('#336699').size(1)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class SegmentBuilder extends LayerBuilder {

  /**
   * Sets segment colour.
   *
   * @param value colour value
   * @return this builder
   */
  SegmentBuilder color(String value) {
    params['color'] = value
    this
  }

  /**
   * Sets segment colour (British spelling alias).
   *
   * @param value colour value
   * @return this builder
   */
  SegmentBuilder colour(String value) {
    color(value)
  }

  /**
   * Sets segment line width.
   *
   * @param value size value
   * @return this builder
   */
  SegmentBuilder size(Number value) {
    params['size'] = value
    this
  }

  /**
   * Sets segment line type.
   *
   * @param value linetype name or integer code
   * @return this builder
   */
  SegmentBuilder linetype(String value) {
    params['linetype'] = value
    this
  }

  /**
   * Sets arrow specification for segment endpoints.
   *
   * @param value arrow specification
   * @return this builder
   */
  SegmentBuilder arrow(Object value) {
    params['arrow'] = value
    this
  }

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.SEGMENT
  }

  @Override
  protected CharmStatType statType() {
    CharmStatType.IDENTITY
  }
}
