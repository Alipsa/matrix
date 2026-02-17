package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Count stat transformation - counts occurrences of each unique x value.
 * Used by geom_bar to count observations in each category.
 */
@CompileStatic
class CountStat {

  /**
   * Groups data by x value and counts occurrences.
   *
   * @param layer layer specification
   * @param data layer data
   * @return one LayerData per unique x value with y=count and meta.percent
   */
  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data.isEmpty()) {
      return []
    }

    Map<String, List<LayerData>> groups = [:]
    data.each { LayerData d ->
      String key = d.x?.toString() ?: ''
      groups.computeIfAbsent(key) { [] } << d
    }

    int total = data.size()
    List<LayerData> result = []
    groups.each { String key, List<LayerData> groupData ->
      int count = groupData.size()
      BigDecimal percent = total == 0 ? 0 : (count * 100.0 / total)
      LayerData template = groupData.first()
      LayerData datum = new LayerData(
          x: template.x,
          y: count,
          color: template.color,
          fill: template.fill,
          rowIndex: -1
      )
      datum.meta.percent = percent
      result << datum
    }
    result
  }
}
