package se.alipsa.matrix.charm.render.position

import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Identity position adjustment - returns data unchanged.
 * Default position for most geom types.
 */
class IdentityPosition {

  /**
   * Returns data unchanged (identity/pass-through).
   *
   * @param layer layer specification
   * @param data layer data
   * @return input data unchanged
   */
  @SuppressWarnings('UnusedMethodParameter')
  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    data
  }

}
