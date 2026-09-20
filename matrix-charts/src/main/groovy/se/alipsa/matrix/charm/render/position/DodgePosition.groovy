package se.alipsa.matrix.charm.render.position

import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil
import se.alipsa.matrix.charm.render.scale.ScaleUtils
import se.alipsa.matrix.core.ValueConverter

/**
 * Dodge position adjustment - places overlapping objects side by side.
 * Used for grouped bar charts and similar visualizations.
 *
 * Supports params:
 * - width: dodge width (default 0.9). Multi-group buckets record {@code meta.dodgeIndex},
 *   {@code meta.dodgeCount}, and {@code meta.dodgeWidth} for renderers on discrete axes.
 */
@SuppressWarnings('DuplicateNumberLiteral')
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
    Map<Object, List<LayerData>> byX = [:]
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
      List<Object> groups = new ArrayList<>(new LinkedHashSet<>(
          bucket.collect { LayerData d -> resolveGroup(d) }
      ))
      int nGroups = groups.size()

      if (nGroups <= 1) {
        bucket.each { LayerData datum -> result.add(LayerDataUtil.copyDatum(datum)) }
        return
      }

      BigDecimal groupWidth = width / nGroups
      bucket.each { LayerData datum ->
        LayerData updated = LayerDataUtil.copyDatum(datum)
        int index = groups.indexOf(resolveGroup(datum))
        updated.meta.dodgeIndex = index
        updated.meta.dodgeCount = nGroups
        updated.meta.dodgeWidth = width
        BigDecimal xNum = ScaleUtils.coerceStrictNumber(datum.x)
        if (xNum != null) {
          BigDecimal offset = (-width / 2) + (groupWidth / 2) + (index * groupWidth)
          updated.x = xNum + offset
        }
        result.add(updated)
      }
    }

    result
  }

  /**
   * Resolves the grouping key for a datum, using group, fill, or color (in that order).
   * Explicit null checks retain {@code 0}, {@code false}, and {@code ''} as valid keys.
   */
  private static Object resolveGroup(LayerData datum) {
    if (datum.group != null) {
      return datum.group
    }
    if (datum.fill != null) {
      return datum.fill
    }
    datum.color
  }

}
