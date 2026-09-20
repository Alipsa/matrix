package se.alipsa.matrix.charm.render.position

import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Fill position adjustment - stacks and normalizes to [-1, 1] when negatives are present.
 * Like stack but scales each x group to fill the entire height.
 *
 * Supports params:
 * - reverse: boolean (default false) - if true, reverses stacking order within each x group
 */
class FillPosition {

  /**
   * Applies fill position adjustment.
   * Delegates to StackPosition first, then normalizes ymin/ymax/y to [0, 1] per x group.
   *
   * @param layer layer specification with position params
   * @param data layer data to adjust
   * @return position-adjusted layer data normalized to [0, 1] range per x group
   */
  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return data
    }

    // First apply stacking
    List<LayerData> stacked = StackPosition.compute(layer, data)

    Map<Object, BigDecimal> positiveTotal = [:]
    Map<Object, BigDecimal> negativeTotal = [:]
    stacked.each { LayerData datum ->
      BigDecimal ymax = datum.ymax as BigDecimal
      BigDecimal ymin = datum.ymin as BigDecimal
      if (ymax != null && ymax > 0) {
        BigDecimal current = positiveTotal[datum.x]
        if (current == null || ymax > current) {
          positiveTotal[datum.x] = ymax
        }
      }
      if (ymin != null && ymin < 0) {
        BigDecimal current = negativeTotal[datum.x]
        if (current == null || ymin.abs() > current) {
          negativeTotal[datum.x] = ymin.abs()
        }
      }
    }

    List<LayerData> result = []
    stacked.each { LayerData datum ->
      BigDecimal ymin = datum.ymin as BigDecimal
      BigDecimal ymax = datum.ymax as BigDecimal
      boolean negative = ymin != null && ymin < 0
      BigDecimal total = negative ? negativeTotal[datum.x] : positiveTotal[datum.x]
      if (total != null && total > 0) {
        BigDecimal yMin = ymin / total
        BigDecimal yMax = ymax / total
        datum.ymin = yMin
        datum.ymax = yMax
        datum.y = (yMin + yMax) / 2
      }
      result.add(datum)
    }

    result
  }

}
