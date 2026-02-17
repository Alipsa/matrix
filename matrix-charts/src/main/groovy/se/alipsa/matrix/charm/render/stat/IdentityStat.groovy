package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Identity stat transformation - returns data unchanged.
 * Default stat for most geom types.
 */
@CompileStatic
class IdentityStat {

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
