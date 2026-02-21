package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Renders curved segments using cubic Bezier paths.
 */
@CompileStatic
class CurveRenderer {

  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    BigDecimal curvature = NumberCoercionUtil.coerceToBigDecimal(layer.params.curvature) ?: 0.5
    int elementIndex = 0

    layerData.each { LayerData datum ->
      BigDecimal x1 = context.xScale.transform(datum.x)
      BigDecimal y1 = context.yScale.transform(datum.y)
      BigDecimal x2 = context.xScale.transform(datum.xend)
      BigDecimal y2 = context.yScale.transform(datum.yend)
      if (x1 == null || y1 == null || x2 == null || y2 == null) {
        return
      }

      List<BigDecimal> controls = controlPoints(x1, y1, x2, y2, curvature)
      String stroke = GeomUtils.resolveStroke(context, layer, datum)
      BigDecimal lineWidth = GeomUtils.resolveLineWidth(context, layer, datum)
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)
      String dashArray = GeomUtils.dashArray(GeomUtils.resolveLinetype(context, layer, datum))

      String pathData = "M ${x1} ${y1} C ${controls[0]} ${controls[1]}, ${controls[2]} ${controls[3]}, ${x2} ${y2}"
      def path = dataLayer.addPath()
          .d(pathData)
          .fill('none')
          .stroke(stroke)
          .strokeWidth(lineWidth)
          .styleClass('charm-curve')
      if (dashArray != null) {
        path.addAttribute('stroke-dasharray', dashArray)
      }
      if (alpha < 1.0) {
        path.addAttribute('stroke-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(path, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++
    }
  }

  private static List<BigDecimal> controlPoints(
      BigDecimal x1,
      BigDecimal y1,
      BigDecimal x2,
      BigDecimal y2,
      BigDecimal curvature
  ) {
    BigDecimal mx = (x1 + x2) / 2
    BigDecimal my = (y1 + y2) / 2

    BigDecimal dx = x2 - x1
    BigDecimal dy = y2 - y1
    BigDecimal dist = (dx * dx + dy * dy).sqrt()

    BigDecimal px = -dy
    BigDecimal py = dx
    if (dist > 0) {
      px = px / dist
      py = py / dist
    }

    BigDecimal offset = dist * curvature * 0.5
    BigDecimal cx = mx + px * offset
    BigDecimal cy = my + py * offset

    BigDecimal cx1 = x1 + (cx - x1) * 0.67
    BigDecimal cy1 = y1 + (cy - y1) * 0.67
    BigDecimal cx2 = x2 + (cx - x2) * 0.67
    BigDecimal cy2 = y2 + (cy - y2) * 0.67

    [cx1, cy1, cx2, cy2]
  }
}
