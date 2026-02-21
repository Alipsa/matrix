package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext

/**
 * Renders rectangle geometry.
 */
@CompileStatic
class RectRenderer {

  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    int elementIndex = 0
    layerData.each { LayerData datum ->
      BigDecimal xMin = context.xScale.transform(datum.xmin)
      BigDecimal xMax = context.xScale.transform(datum.xmax)
      BigDecimal yMin = context.yScale.transform(datum.ymin)
      BigDecimal yMax = context.yScale.transform(datum.ymax)
      if (xMin == null || xMax == null || yMin == null || yMax == null) {
        return
      }

      BigDecimal rectX = [xMin, xMax].min()
      BigDecimal rectY = [yMin, yMax].min()
      BigDecimal width = (xMax - xMin).abs()
      BigDecimal height = (yMax - yMin).abs()
      if (width <= 0 || height <= 0) {
        return
      }

      String fill = GeomUtils.resolveFill(context, layer, datum)
      String stroke = GeomUtils.resolveStroke(context, layer, datum)
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)
      BigDecimal lineWidth = GeomUtils.resolveLineWidth(context, layer, datum, 0.5)
      String dashArray = GeomUtils.dashArray(GeomUtils.resolveLinetype(context, layer, datum))

      def rect = dataLayer.addRect(width, height)
          .x(rectX)
          .y(rectY)
          .fill(fill)
          .stroke(stroke)
          .addAttribute('stroke-width', lineWidth)
          .styleClass('charm-rect')
      if (dashArray != null) {
        rect.addAttribute('stroke-dasharray', dashArray)
      }
      if (alpha < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(rect, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++
    }
  }
}
