package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.core.Stat
import static se.alipsa.matrix.ext.NumberExtension.PI

/**
 * Confidence-ellipse style stat for grouped x/y data.
 */
@CompileStatic
class EllipseStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    int segments = ValueConverter.asBigDecimal(params.segments)?.intValue() ?: 51
    if (segments < 8) {
      segments = 51
    }
    BigDecimal level = ValueConverter.asBigDecimal(params.level) ?: 0.95
    BigDecimal radiusScale = level >= 0.99 ? 2.58 : (level >= 0.95 ? 2.0 : 1.64)

    Map<Object, List<LayerData>> groups = StatUtils.groupBySeries(data.findAll { LayerData datum ->
      ValueConverter.asBigDecimal(datum.x) != null &&
          ValueConverter.asBigDecimal(datum.y) != null
    })
    if (groups.isEmpty()) {
      return []
    }

    List<LayerData> result = []
    groups.each { Object key, List<LayerData> groupData ->
      if (groupData.size() < 2) {
        return
      }

      List<BigDecimal> xs = groupData.collect { ValueConverter.asBigDecimal(it.x) }
      List<BigDecimal> ys = groupData.collect { ValueConverter.asBigDecimal(it.y) }

      BigDecimal meanX = Stat.mean(xs)
      BigDecimal meanY = Stat.mean(ys)
      BigDecimal stdX = Stat.sd(xs, true)
      BigDecimal stdY = Stat.sd(ys, true)
      if (stdX == null || stdX <= 0) {
        stdX = 1
      }
      if (stdY == null || stdY <= 0) {
        stdY = 1
      }

      for (int i = 0; i <= segments; i++) {
        BigDecimal t = i / (segments as BigDecimal)
        BigDecimal theta = 2 * PI * t
        BigDecimal x = meanX + stdX * radiusScale * theta.cos()
        BigDecimal y = meanY + stdY * radiusScale * theta.sin()

        LayerData datum = new LayerData(
            x: x,
            y: y,
            color: groupData.first().color,
            fill: groupData.first().fill,
            group: key,
            rowIndex: -1
        )
        datum.meta.level = level
        datum.meta.ellipse = true
        result << datum
      }
    }

    result
  }
}
