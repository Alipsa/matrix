package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.render.scale.DiscreteCharmScale
import se.alipsa.matrix.core.ValueConverter

/**
 * Renders bar/col geometry.
 */
@CompileStatic
class BarRenderer {

  /**
   * Render bars.
   */
  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData, int panelHeight) {
    if (layerData.isEmpty()) {
      return
    }

    G barGroup = dataLayer.addG().styleClass('geombar')
    int elementIndex = 0
    BigDecimal baseline = context.yScale.transform(0)
    if (baseline == null) {
      baseline = panelHeight
    }

    boolean discreteX = context.xScale.isDiscrete()
    BigDecimal widthFactor = ValueConverter.asBigDecimal(layer.params.width) ?: 0.75
    BigDecimal barWidth
    if (discreteX && context.xScale instanceof DiscreteCharmScale) {
      DiscreteCharmScale scale = context.xScale as DiscreteCharmScale
      BigDecimal step = scale.levels.isEmpty() ? 20 : (scale.rangeEnd - scale.rangeStart) / scale.levels.size()
      barWidth = step * widthFactor
    } else {
      barWidth = ValueConverter.asBigDecimal(layer.params.barWidth) ?: 12
    }

    layerData.each { LayerData datum ->
      BigDecimal xLeft
      BigDecimal width
      if (datum.xmin != null && datum.xmax != null) {
        BigDecimal x1 = context.xScale.transform(datum.xmin)
        BigDecimal x2 = context.xScale.transform(datum.xmax)
        if (x1 == null || x2 == null) {
          return
        }
        xLeft = [x1, x2].min()
        width = (x2 - x1).abs()
      } else {
        BigDecimal xCenter = context.xScale.transform(datum.x)
        if (xCenter == null) {
          return
        }
        xLeft = xCenter - barWidth / 2
        width = barWidth
      }

      BigDecimal yTop
      BigDecimal yBottom
      if (datum.ymin != null && datum.ymax != null) {
        yTop = context.yScale.transform(datum.ymax)
        yBottom = context.yScale.transform(datum.ymin)
      } else {
        yTop = context.yScale.transform(datum.y)
        yBottom = baseline
      }
      if (yTop == null || yBottom == null) {
        return
      }

      BigDecimal rectY = [yTop, yBottom].min()
      BigDecimal rectHeight = (yBottom - yTop).abs()
      if (rectHeight <= 0 || width <= 0) {
        return
      }

      String fill = GeomUtils.resolveFill(context, layer, datum)
      String stroke = GeomUtils.resolveStroke(context, layer, datum)
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)
      BigDecimal strokeWidth = ValueConverter.asBigDecimal(layer.params.lineWidth) ?:
          ValueConverter.asBigDecimal(layer.params.linewidth) ?: 0.5

      def rect = barGroup.addRect(width, rectHeight)
          .x(xLeft)
          .y(rectY)
          .fill(fill)
          .stroke(stroke)
          .addAttribute('stroke-width', strokeWidth)
          .styleClass('charm-bar')
      if (alpha < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(rect, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++
    }
  }
}
