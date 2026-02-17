package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Renders histogram geometry.
 */
@CompileStatic
class HistogramRenderer {

  /**
   * Render histogram bins.
   */
  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData, int panelHeight) {
    if (layerData.isEmpty()) {
      return
    }

    BigDecimal baseline = context.yScale.transform(0)
    if (baseline == null) {
      baseline = panelHeight
    }

    layerData.each { LayerData datum ->
      Object startVal = datum.meta.binStart ?: datum.meta.xmin
      Object endVal = datum.meta.binEnd ?: datum.meta.xmax
      BigDecimal xLeft
      BigDecimal width
      if (startVal != null && endVal != null) {
        BigDecimal xStart = context.xScale.transform(startVal)
        BigDecimal xEnd = context.xScale.transform(endVal)
        if (xStart == null || xEnd == null) {
          return
        }
        xLeft = [xStart, xEnd].min()
        width = (xEnd - xStart).abs()
      } else {
        BigDecimal xCenter = context.xScale.transform(datum.x)
        if (xCenter == null) {
          return
        }
        BigDecimal fallbackWidth = NumberCoercionUtil.coerceToBigDecimal(layer.params.barWidth) ?: 12
        xLeft = xCenter - fallbackWidth / 2
        width = fallbackWidth
      }

      BigDecimal yValue = context.yScale.transform(datum.y)
      if (yValue == null) {
        return
      }

      BigDecimal rectY = [yValue, baseline].min()
      BigDecimal rectHeight = (yValue - baseline).abs()
      if (rectHeight <= 0 || width <= 0) {
        return
      }

      String fill = GeomUtils.resolveFill(context, layer, datum)
      String stroke = layer.params.color?.toString() ?: 'white'
      BigDecimal alpha = GeomUtils.resolveAlpha(layer, datum)
      BigDecimal strokeWidth = NumberCoercionUtil.coerceToBigDecimal(layer.params.lineWidth) ?:
          NumberCoercionUtil.coerceToBigDecimal(layer.params.linewidth) ?: 0.5

      def rect = dataLayer.addRect(width, rectHeight)
          .x(xLeft)
          .y(rectY)
          .fill(fill)
          .stroke(stroke)
          .addAttribute('stroke-width', strokeWidth)
          .styleClass('charm-histogram')
      if (alpha < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }
    }
  }
}
