package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.render.SpokeSupport
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Renders spokes from x/y using angle and radius columns or params.
 */
@CompileStatic
class SpokeRenderer {

  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    String angleCol = SpokeSupport.resolveAngleColumn(layer.params)
    BigDecimal angleDefault = SpokeSupport.resolveAngleDefault(layer.params)
    String radiusCol = SpokeSupport.resolveRadiusColumn(layer.params)
    BigDecimal defaultRadius = SpokeSupport.resolveRadiusDefault(layer.params)
    int elementIndex = 0

    layerData.each { LayerData datum ->
      BigDecimal x = NumberCoercionUtil.coerceToBigDecimal(datum.x)
      BigDecimal y = NumberCoercionUtil.coerceToBigDecimal(datum.y)
      if (x == null || y == null) {
        return
      }

      Map<String, Object> row = datum.meta?.__row instanceof Map ? (datum.meta.__row as Map<String, Object>) : [:]
      BigDecimal angle = angleCol == null ? null : NumberCoercionUtil.coerceToBigDecimal(row[angleCol])
      if (angle == null) {
        angle = angleDefault
      }
      BigDecimal radius = radiusCol == null ? null : NumberCoercionUtil.coerceToBigDecimal(row[radiusCol])
      if (radius == null) {
        radius = defaultRadius
      }

      BigDecimal xend = x + radius * angle.cos()
      BigDecimal yend = y + radius * angle.sin()

      BigDecimal x1 = context.xScale.transform(x)
      BigDecimal y1 = context.yScale.transform(y)
      BigDecimal x2 = context.xScale.transform(xend)
      BigDecimal y2 = context.yScale.transform(yend)
      if (x1 == null || y1 == null || x2 == null || y2 == null) {
        return
      }

      String stroke = GeomUtils.resolveStroke(context, layer, datum)
      BigDecimal lineWidth = GeomUtils.resolveLineWidth(context, layer, datum)
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)
      String dashArray = GeomUtils.dashArray(GeomUtils.resolveLinetype(context, layer, datum))

      def line = dataLayer.addLine(x1, y1, x2, y2)
          .stroke(stroke)
          .strokeWidth(lineWidth)
          .styleClass('charm-spoke')
      if (dashArray != null) {
        line.addAttribute('stroke-dasharray', dashArray)
      }
      if (alpha < 1.0) {
        line.addAttribute('stroke-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(line, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++
    }
  }
}
