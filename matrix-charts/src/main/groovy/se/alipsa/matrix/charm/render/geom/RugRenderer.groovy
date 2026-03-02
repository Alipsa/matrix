package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.core.ValueConverter

/**
 * Renders rug geometry as small axis ticks.
 */
@CompileStatic
class RugRenderer {

  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData, int panelWidth, int panelHeight) {
    String sides = layer.params.sides?.toString()?.toLowerCase() ?: 'bl'
    BigDecimal lengthRatio = ValueConverter.asBigDecimal(layer.params.length) ?: 0.03
    BigDecimal tickX = panelWidth * lengthRatio
    BigDecimal tickY = panelHeight * lengthRatio

    int elementIndex = 0
    layerData.each { LayerData datum ->
      BigDecimal x = context.xScaleForLayer(context.layerIndex).transform(datum.x)
      BigDecimal y = context.yScaleForLayer(context.layerIndex).transform(datum.y)
      if (x == null || y == null) {
        return
      }

      if (sides.contains('b')) {
        BigDecimal panelBottom = panelHeight as BigDecimal
        drawTick(dataLayer, context, layer, datum, x, panelBottom, x, panelBottom - tickY, elementIndex++)
      }
      if (sides.contains('t')) {
        drawTick(dataLayer, context, layer, datum, x, 0.0, x, tickY, elementIndex++)
      }
      if (sides.contains('l')) {
        drawTick(dataLayer, context, layer, datum, 0.0, y, tickX, y, elementIndex++)
      }
      if (sides.contains('r')) {
        BigDecimal panelRight = panelWidth as BigDecimal
        drawTick(dataLayer, context, layer, datum, panelRight, y, panelRight - tickX, y, elementIndex++)
      }
    }
  }

  private static void drawTick(
      G dataLayer,
      RenderContext context,
      LayerSpec layer,
      LayerData datum,
      BigDecimal x1,
      BigDecimal y1,
      BigDecimal x2,
      BigDecimal y2,
      int elementIndex
  ) {
    String stroke = GeomUtils.resolveStroke(context, layer, datum)
    BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)
    BigDecimal lineWidth = GeomUtils.resolveLineWidth(context, layer, datum, 1.0)
    def line = dataLayer.addLine(x1, y1, x2, y2)
        .stroke(stroke)
        .strokeWidth(lineWidth)
        .styleClass('charm-rug')
    if (alpha < 1.0) {
      line.addAttribute('stroke-opacity', alpha)
    }
    GeomUtils.applyCssAttributes(line, context, layer.geomType.name(), elementIndex, datum)
  }
}
