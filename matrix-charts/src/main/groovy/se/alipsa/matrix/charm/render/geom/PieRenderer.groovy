package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.util.NumberCoercionUtil

import static se.alipsa.matrix.ext.NumberExtension.PI

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

    BigDecimal cx = panelWidth / 2.0
    BigDecimal cy = panelHeight / 2.0
    BigDecimal radius = panelWidth.min(panelHeight) * 0.4

    List<BigDecimal> values = layerData.collect { LayerData datum ->
      BigDecimal v = NumberCoercionUtil.coerceToBigDecimal(datum.y)
      v != null && v > 0 ? v : 0.0
    }
    BigDecimal total = values.sum() as BigDecimal
    if (total == 0) {
      return
    }

    BigDecimal startAngle = -PI / 2
    layerData.eachWithIndex { LayerData datum, int idx ->
      BigDecimal slice = values[idx]
      if (slice == 0) {
        return
      }
      BigDecimal sweep = (slice / total) * 2 * PI
      BigDecimal endAngle = startAngle + sweep
      BigDecimal x1 = cx + radius * startAngle.cos()
      BigDecimal y1 = cy + radius * startAngle.sin()
      BigDecimal x2 = cx + radius * endAngle.cos()
      BigDecimal y2 = cy + radius * endAngle.sin()
      int largeArc = sweep > PI ? 1 : 0

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
