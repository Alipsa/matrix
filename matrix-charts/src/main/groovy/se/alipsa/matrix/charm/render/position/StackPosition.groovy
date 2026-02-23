package se.alipsa.matrix.charm.render.position

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.core.ValueConverter

/**
 * Stack position adjustment - stacks overlapping objects on top of each other.
 * Used for stacked bar charts and area charts.
 *
 * Supports params:
 * - reverse: boolean (default false) - if true, reverses stacking order within each x group
 */
@CompileStatic
class StackPosition {

  /**
   * Applies stack position adjustment.
   * Groups data by x value and cumulatively sums y values, setting ymin/ymax on each datum.
   *
   * @param layer layer specification with position params
   * @param data layer data to adjust
   * @return position-adjusted layer data with ymin, ymax, and centered y values
   */
  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return data
    }

    Map<String, Object> params = PositionEngine.effectiveParams(layer)
    boolean reverse = params.reverse as boolean ?: false

    // Group data by x value, preserving insertion order
    Map<Object, List<LayerData>> byX = new LinkedHashMap<>()
    data.each { LayerData datum ->
      Object key = datum.x
      List<LayerData> bucket = byX.get(key)
      if (bucket == null) {
        bucket = []
        byX.put(key, bucket)
      }
      bucket.add(datum)
    }

    List<LayerData> result = []
    byX.each { Object xVal, List<LayerData> bucket ->
      List<LayerData> ordered = reverse ? new ArrayList<>(bucket).reverse() as List<LayerData> : bucket
      BigDecimal cumSum = BigDecimal.ZERO

      ordered.each { LayerData datum ->
        LayerData updated = LayerDataUtil.copyDatum(datum)
        BigDecimal yVal = ValueConverter.asBigDecimal(datum.y) ?: BigDecimal.ZERO
        updated.ymin = cumSum
        cumSum = cumSum + yVal
        updated.ymax = cumSum
        BigDecimal yMin = updated.ymin as BigDecimal
        BigDecimal yMax = updated.ymax as BigDecimal
        updated.y = (yMin + yMax) / 2
        result.add(updated)
      }
    }

    result
  }
}
