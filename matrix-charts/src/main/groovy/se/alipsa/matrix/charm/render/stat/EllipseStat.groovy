package se.alipsa.matrix.charm.render.stat

import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.stats.Ellipse

/**
 * Confidence-ellipse style stat for grouped x/y data.
 */
@SuppressWarnings(['DuplicateNumberLiteral', 'DuplicateStringLiteral'])
class EllipseStat {

  private static final Logger log = Logger.getLogger(EllipseStat)

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
    String type = params.type?.toString() ?: 't'
    if (!(type in ['t', 'norm', 'euclid'])) {
      log.warn("Unknown ellipse type $type; skipping ellipse")
      return []
    }

    Map<Object, List<LayerData>> groups = StatUtils.groupBySeries(data.findAll { LayerData datum ->
      ValueConverter.asBigDecimal(datum.x) != null &&
          ValueConverter.asBigDecimal(datum.y) != null
    })
    if (groups.isEmpty()) {
      return []
    }

    List<LayerData> result = []
    groups.each { Object key, List<LayerData> groupData ->
      if (groupData.size() < 4) {
        log.debug('Ellipse requires at least four observations')
        return
      }

      List<BigDecimal> xs = groupData.collect { ValueConverter.asBigDecimal(it.x) }
      List<BigDecimal> ys = groupData.collect { ValueConverter.asBigDecimal(it.y) }

      Ellipse.EllipseData ellipse
      try {
        ellipse = Ellipse.calculate(xs, ys, level.doubleValue(), type, segments + 1)
      } catch (IllegalArgumentException exception) {
        log.warn("Skipping ellipse: ${exception.message}")
        ellipse = new Ellipse.EllipseData([], [])
      }
      for (int i = 0; i < ellipse.x.size(); i++) {
        BigDecimal x = ellipse.x[i]
        BigDecimal y = ellipse.y[i]

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
