package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil

/**
 * Unique stat drops duplicate observations by x/y/group.
 */
@CompileStatic
class UniqueStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    Set<List<Object>> seen = new LinkedHashSet<>()
    List<LayerData> result = []
    data.each { LayerData datum ->
      List<Object> key = [datum.x, datum.y, datum.group, datum.color, datum.fill]
      if (!seen.contains(key)) {
        seen << key
        result << LayerDataUtil.copyDatum(datum)
      }
    }
    result
  }
}
