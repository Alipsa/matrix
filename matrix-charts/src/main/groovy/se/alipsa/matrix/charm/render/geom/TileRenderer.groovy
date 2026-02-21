package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Renders tile geometry.
 */
@CompileStatic
class TileRenderer {

  /**
   * Render tiles centered at x/y positions.
   */
  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    BigDecimal defaultWidth = NumberCoercionUtil.coerceToBigDecimal(layer.params.width) ?: 10
    BigDecimal defaultHeight = NumberCoercionUtil.coerceToBigDecimal(layer.params.height) ?: 10
    int elementIndex = 0

    layerData.each { LayerData datum ->
      BigDecimal xCenter = context.xScale.transform(datum.x)
      BigDecimal yCenter = context.yScale.transform(datum.y)
      if (xCenter == null || yCenter == null) {
        return
      }

      BigDecimal halfW
      BigDecimal halfH
      if (datum.xmin != null && datum.xmax != null) {
        BigDecimal xLeft = context.xScale.transform(datum.xmin)
        BigDecimal xRight = context.xScale.transform(datum.xmax)
        if (xLeft != null && xRight != null) {
          halfW = (xRight - xLeft).abs() / 2
        }
      }
      if (datum.ymin != null && datum.ymax != null) {
        BigDecimal yBottom = context.yScale.transform(datum.ymin)
        BigDecimal yTop = context.yScale.transform(datum.ymax)
        if (yBottom != null && yTop != null) {
          halfH = (yTop - yBottom).abs() / 2
        }
      }
      halfW = halfW ?: defaultWidth / 2
      halfH = halfH ?: defaultHeight / 2

      String fill = GeomUtils.resolveFill(context, layer, datum)
      String stroke = layer.params.color?.toString() ?: 'white'
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)
      BigDecimal strokeWidth = NumberCoercionUtil.coerceToBigDecimal(layer.params.lineWidth) ?:
          NumberCoercionUtil.coerceToBigDecimal(layer.params.linewidth) ?: 0.5

      def rect = dataLayer.addRect(halfW * 2, halfH * 2)
          .x(xCenter - halfW)
          .y(yCenter - halfH)
          .fill(fill)
          .stroke(stroke)
          .addAttribute('stroke-width', strokeWidth)
          .styleClass('charm-tile')
      if (alpha < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(rect, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++
    }
  }
}
