package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Aligns grouped series to a shared x-grid using linear interpolation.
 */
@CompileStatic
class AlignStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    Map<Object, List<LayerData>> groups = StatUtils.groupBySeries(data.findAll { LayerData datum ->
      NumberCoercionUtil.coerceToBigDecimal(datum.x) != null &&
          NumberCoercionUtil.coerceToBigDecimal(datum.y) != null
    })
    if (groups.isEmpty()) {
      return []
    }

    List<BigDecimal> xGrid = groups.values()
        .collectMany { List<LayerData> series ->
          series.collect { LayerData datum -> NumberCoercionUtil.coerceToBigDecimal(datum.x) }
        }
        .findAll { it != null } as List<BigDecimal>
    xGrid = xGrid.unique().sort()
    if (xGrid.isEmpty()) {
      return []
    }

    List<LayerData> result = []
    groups.each { Object key, List<LayerData> series ->
      List<LayerData> sorted = series.sort { LayerData a, LayerData b ->
        BigDecimal x1 = NumberCoercionUtil.coerceToBigDecimal(a.x) ?: 0
        BigDecimal x2 = NumberCoercionUtil.coerceToBigDecimal(b.x) ?: 0
        x1 <=> x2
      }
      LayerData template = sorted.first()

      xGrid.each { BigDecimal x ->
        BigDecimal y = interpolateY(sorted, x)
        if (y == null) {
          return
        }
        LayerData datum = new LayerData(
            x: x,
            y: y,
            color: template.color,
            fill: template.fill,
            size: template.size,
            alpha: template.alpha,
            linetype: template.linetype,
            group: key,
            rowIndex: -1
        )
        datum.meta.aligned = true
        result << datum
      }
    }

    result
  }

  private static BigDecimal interpolateY(List<LayerData> sorted, BigDecimal targetX) {
    if (sorted == null || sorted.isEmpty()) {
      return null
    }

    LayerData first = sorted.first()
    LayerData last = sorted.last()
    BigDecimal firstX = NumberCoercionUtil.coerceToBigDecimal(first.x)
    BigDecimal firstY = NumberCoercionUtil.coerceToBigDecimal(first.y)
    BigDecimal lastX = NumberCoercionUtil.coerceToBigDecimal(last.x)
    BigDecimal lastY = NumberCoercionUtil.coerceToBigDecimal(last.y)

    if (targetX <= firstX) {
      return firstY
    }
    if (targetX >= lastX) {
      return lastY
    }

    for (int i = 0; i < sorted.size() - 1; i++) {
      LayerData left = sorted[i]
      LayerData right = sorted[i + 1]
      BigDecimal x1 = NumberCoercionUtil.coerceToBigDecimal(left.x)
      BigDecimal y1 = NumberCoercionUtil.coerceToBigDecimal(left.y)
      BigDecimal x2 = NumberCoercionUtil.coerceToBigDecimal(right.x)
      BigDecimal y2 = NumberCoercionUtil.coerceToBigDecimal(right.y)
      if (x1 == null || x2 == null || y1 == null || y2 == null) {
        continue
      }
      if (targetX < x1 || targetX > x2) {
        continue
      }
      if (x1 == x2) {
        return y1
      }
      BigDecimal t = (targetX - x1) / (x2 - x1)
      return y1 + (y2 - y1) * t
    }

    null
  }
}
