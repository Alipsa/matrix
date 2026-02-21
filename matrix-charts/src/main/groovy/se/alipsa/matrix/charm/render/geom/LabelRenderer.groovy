package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Renders text labels with background rectangles.
 */
@CompileStatic
class LabelRenderer {

  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    BigDecimal fontSize = NumberCoercionUtil.coerceToBigDecimal(layer.params.size) ?: 10
    String textFamily = layer.params.family?.toString() ?: 'sans-serif'
    String textFace = layer.params.fontface?.toString() ?: 'normal'
    String textColor = layer.params.textColor?.toString() ?: '#111111'
    BigDecimal labelPadding = NumberCoercionUtil.coerceToBigDecimal(layer.params.labelPadding) ?: 2
    BigDecimal radius = NumberCoercionUtil.coerceToBigDecimal(layer.params.labelR) ?: 2

    int elementIndex = 0
    layerData.each { LayerData datum ->
      String label = datum.label?.toString() ?: layer.params.label?.toString()
      if (label == null || label.isBlank()) {
        return
      }

      BigDecimal x = context.xScale.transform(datum.x)
      BigDecimal y = context.yScale.transform(datum.y)
      if (x == null || y == null) {
        return
      }

      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)
      String fill = GeomUtils.resolveFill(context, layer, datum)
      String stroke = GeomUtils.resolveStroke(context, layer, datum)
      BigDecimal strokeWidth = GeomUtils.resolveLineWidth(context, layer, datum, 0.5)
      BigDecimal textWidth = label.length() * fontSize * 0.6
      BigDecimal textHeight = fontSize * 1.2

      BigDecimal rectX = x - textWidth / 2 - labelPadding
      BigDecimal rectY = y - textHeight / 2 - labelPadding
      BigDecimal rectW = textWidth + labelPadding * 2
      BigDecimal rectH = textHeight + labelPadding * 2

      def rect = dataLayer.addRect(rectW, rectH)
          .x(rectX)
          .y(rectY)
          .fill(fill)
          .stroke(stroke)
          .addAttribute('stroke-width', strokeWidth)
          .addAttribute('rx', radius)
          .addAttribute('ry', radius)
          .styleClass('charm-label-box')
      if (alpha < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(rect, context, layer.geomType.name(), elementIndex, datum)

      def text = dataLayer.addText(label)
          .x(x)
          .y(y + fontSize * 0.35)
          .fill(textColor)
          .textAnchor('middle')
          .styleClass('charm-label-text')
      text.addAttribute('font-size', fontSize)
      text.addAttribute('font-family', textFamily)
      if (textFace == 'bold' || textFace == 'bold.italic') {
        text.addAttribute('font-weight', 'bold')
      }
      if (textFace == 'italic' || textFace == 'bold.italic') {
        text.addAttribute('font-style', 'italic')
      }
      if (alpha < 1.0) {
        text.addAttribute('fill-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(text, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++
    }
  }
}
