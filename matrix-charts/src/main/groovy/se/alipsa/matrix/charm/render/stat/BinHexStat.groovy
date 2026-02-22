package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Hex-bin stat currently reuses rectangular binning output.
 */
@CompileStatic
class BinHexStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    List<LayerData> binned = Bin2DStat.compute(layer, data)
    binned.each { LayerData datum ->
      datum.meta.hex = true
    }
    binned
  }
}
