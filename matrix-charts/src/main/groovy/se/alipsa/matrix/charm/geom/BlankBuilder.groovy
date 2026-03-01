package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType

/**
 * Fluent builder for blank layers.
 *
 * <p>A blank layer draws nothing but can be used to set up axes and
 * coordinate ranges. Produces a {@code BLANK / IDENTITY} layer specification.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'x'; y = 'y' }
 *   layers {
 *     geomBlank()
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class BlankBuilder extends LayerBuilder {

  @Override
  protected CharmGeomType geomType() {
    CharmGeomType.BLANK
  }

  @Override
  protected CharmStatType defaultStatType() {
    CharmStatType.IDENTITY
  }
}
