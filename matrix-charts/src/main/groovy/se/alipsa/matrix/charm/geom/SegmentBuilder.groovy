package se.alipsa.matrix.charm.geom

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
class SegmentBuilder extends LineEndpointBuilder<SegmentBuilder> {

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.SEGMENT
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }

}
