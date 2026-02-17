package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Renders pie geometry.
 */
@CompileStatic
class PieRenderer {

  /**
   * Render pie slices from y values.
   */
  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData,
                     int panelWidth, int panelHeight) {
    if (layerData.isEmpty()) {
      return
    }

    double cx = panelWidth / 2.0d
    double cy = panelHeight / 2.0d
    double radius = Math.min(panelWidth, panelHeight) * 0.4d

    List<BigDecimal> values = layerData.collect { LayerData datum ->
      BigDecimal v = NumberCoercionUtil.coerceToBigDecimal(datum.y)
      v != null && v > 0 ? v : 0.0
    }
    BigDecimal total = values.sum() as BigDecimal
    if (total == 0) {
      return
    }

    double startAngle = -Math.PI / 2.0d
    layerData.eachWithIndex { LayerData datum, int idx ->
      BigDecimal slice = values[idx]
      if (slice == 0) {
        return
      }
      double sweep = (slice / total) as double * 2.0d * Math.PI
      double endAngle = startAngle + sweep
      double x1 = cx + radius * Math.cos(startAngle)
      double y1 = cy + radius * Math.sin(startAngle)
      double x2 = cx + radius * Math.cos(endAngle)
      double y2 = cy + radius * Math.sin(endAngle)
      int largeArc = sweep > Math.PI ? 1 : 0

      String pathD = "M ${cx} ${cy} L ${x1} ${y1} A ${radius} ${radius} 0 ${largeArc} 1 ${x2} ${y2} Z"
      String fill = GeomUtils.resolveFill(context, layer, datum)
      dataLayer.addPath().d(pathD)
          .fill(fill)
          .stroke('#ffffff')
          .addAttribute('stroke-width', '1')
          .styleClass('charm-pie')
      startAngle = endAngle
    }
  }
}
