package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.core.ValueConverter

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

    int elementIndex = 0
    BigDecimal baseline = context.yScaleForLayer(context.layerIndex).transform(0)
    if (baseline == null) {
      baseline = panelHeight
    }

    layerData.each { LayerData datum ->
      Object startVal = datum.meta.binStart ?: datum.meta.xmin
      Object endVal = datum.meta.binEnd ?: datum.meta.xmax
      BigDecimal xLeft
      BigDecimal width
      if (startVal != null && endVal != null) {
        BigDecimal xStart = context.xScaleForLayer(context.layerIndex).transform(startVal)
        BigDecimal xEnd = context.xScaleForLayer(context.layerIndex).transform(endVal)
        if (xStart == null || xEnd == null) {
          return
        }
        xLeft = [xStart, xEnd].min()
        width = (xEnd - xStart).abs()
      } else {
        BigDecimal xCenter = context.xScaleForLayer(context.layerIndex).transform(datum.x)
        if (xCenter == null) {
          return
        }
        BigDecimal fallbackWidth = ValueConverter.asBigDecimal(layer.params.barWidth) ?: 12
        xLeft = xCenter - fallbackWidth / 2
        width = fallbackWidth
      }

      BigDecimal yValue = context.yScaleForLayer(context.layerIndex).transform(datum.y)
      if (yValue == null) {
        return
      }

      BigDecimal rectY = [yValue, baseline].min()
      BigDecimal rectHeight = (yValue - baseline).abs()
      if (rectHeight <= 0 || width <= 0) {
        return
      }

      String fill = GeomUtils.resolveFill(context, layer, datum)
      String stroke = GeomUtils.resolveStroke(context, layer, datum, 'white')
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)
      BigDecimal strokeWidth = GeomUtils.resolveLineWidth(context, layer, datum, 0.5)

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
      GeomUtils.applyCssAttributes(rect, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++
    }
  }
}
