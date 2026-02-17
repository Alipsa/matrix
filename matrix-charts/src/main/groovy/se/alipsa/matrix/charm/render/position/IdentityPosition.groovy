package se.alipsa.matrix.charm.render.position

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Identity position adjustment - returns data unchanged.
 * Default position for most geom types.
 */
@CompileStatic
class IdentityPosition {

  /**
   * Returns data unchanged (identity/pass-through).
   *
   * @param layer layer specification
   * @param data layer data
   * @return input data unchanged
   */
  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    data
  }
}
