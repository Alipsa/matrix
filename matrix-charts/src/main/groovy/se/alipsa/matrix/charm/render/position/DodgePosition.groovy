package se.alipsa.matrix.charm.render.position

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.core.ValueConverter

/**
 * Dodge position adjustment - places overlapping objects side by side.
 * Used for grouped bar charts and similar visualizations.
 *
 * Supports params:
 * - width: dodge width (default 0.9)
 */
@CompileStatic
class DodgePosition {

  /**
   * Applies dodge position adjustment.
   * Groups data by x value, then offsets each group/fill/color sub-group horizontally.
   *
   * @param layer layer specification with position params
   * @param data layer data to adjust
   * @return position-adjusted layer data with modified x values
   */
  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return data
    }

    Map<String, Object> params = PositionEngine.effectiveParams(layer)
    BigDecimal width = params.width != null ? ValueConverter.asBigDecimal(params.width) ?: 0.9 : 0.9

    // Group data by x value
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
      // Determine unique groups within this x bucket
      List<Object> groups = new ArrayList<>(new LinkedHashSet<>(
          bucket.collect { LayerData d -> resolveGroup(d) }
      ))
      int nGroups = groups.size()

      if (nGroups <= 1) {
        // Single group: copy data unchanged
        bucket.each { LayerData datum ->
          result.add(LayerDataUtil.copyDatum(datum))
        }
        return
      }

      BigDecimal groupWidth = width / nGroups
      Map<Object, BigDecimal> groupOffsets = new LinkedHashMap<>()
      groups.eachWithIndex { Object group, int i ->
        BigDecimal offset = (-width / 2) + (groupWidth / 2) + (i * groupWidth)
        groupOffsets.put(group, offset)
      }

      bucket.each { LayerData datum ->
        LayerData updated = LayerDataUtil.copyDatum(datum)
        Object group = resolveGroup(datum)
        BigDecimal xNum = ValueConverter.asBigDecimal(datum.x)
        if (xNum != null && groupOffsets.containsKey(group)) {
          updated.x = xNum + groupOffsets.get(group)
        }
        result.add(updated)
      }
    }

    result
  }

  /**
   * Resolves the grouping key for a datum, using group, fill, or color (in that order).
   */
  private static Object resolveGroup(LayerData datum) {
    datum.group ?: datum.fill ?: datum.color
  }
}
