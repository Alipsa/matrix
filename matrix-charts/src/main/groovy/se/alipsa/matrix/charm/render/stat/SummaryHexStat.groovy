package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Hex summary stat currently reuses 2D rectangular summaries.
 */
@CompileStatic
class SummaryHexStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    List<LayerData> summarized = Summary2DStat.compute(layer, data)
    summarized.each { LayerData datum ->
      datum.meta.hex = true
    }
    summarized
  }
}
