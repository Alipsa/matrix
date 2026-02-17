package se.alipsa.matrix.charm.render.position

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Fill position adjustment - stacks and normalizes to [0, 1] range.
 * Like stack but scales each x group to fill the entire height.
 *
 * Supports params:
 * - reverse: boolean (default false) - if true, reverses stacking order within each x group
 */
@CompileStatic
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

    // Find max ymax per x group
    Map<Object, BigDecimal> maxByX = new LinkedHashMap<>()
    stacked.each { LayerData datum ->
      Object xVal = datum.x
      BigDecimal ymax = datum.ymax as BigDecimal
      if (ymax != null) {
        BigDecimal current = maxByX.get(xVal)
        if (current == null || ymax > current) {
          maxByX.put(xVal, ymax)
        }
      }
    }

    // Normalize to [0, 1]
    List<LayerData> result = []
    stacked.each { LayerData datum ->
      Object xVal = datum.x
      BigDecimal total = maxByX.get(xVal) ?: BigDecimal.ONE

      if (total > BigDecimal.ZERO) {
        BigDecimal yMin = (datum.ymin as BigDecimal) / total
        BigDecimal yMax = (datum.ymax as BigDecimal) / total
        datum.ymin = yMin
        datum.ymax = yMax
        datum.y = (yMin + yMax) / 2
      }
      result.add(datum)
    }

    result
  }
}
