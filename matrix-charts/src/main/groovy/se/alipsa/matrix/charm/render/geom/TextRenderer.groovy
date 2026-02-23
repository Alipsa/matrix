package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.core.ValueConverter

/**
 * Renders text geometry.
 */
@CompileStatic
class TextRenderer {

  /**
   * Render text labels at x/y positions.
   */
  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    BigDecimal size = ValueConverter.asBigDecimal(layer.params.size) ?: 10
    String family = layer.params.family?.toString() ?: 'sans-serif'
    String fontface = layer.params.fontface?.toString() ?: 'normal'
    BigDecimal angle = ValueConverter.asBigDecimal(layer.params.angle) ?: 0
    BigDecimal hjust = ValueConverter.asBigDecimal(layer.params.hjust) ?: 0.5
    BigDecimal vjust = ValueConverter.asBigDecimal(layer.params.vjust) ?: 0.5
    BigDecimal nudgeX = ValueConverter.asBigDecimal(layer.params.nudge_x) ?: 0
    BigDecimal nudgeY = ValueConverter.asBigDecimal(layer.params.nudge_y) ?: 0
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

      BigDecimal drawX = x + nudgeX * 10
      BigDecimal drawY = y - nudgeY * 10
      String color = GeomUtils.resolveStroke(context, layer, datum)
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)

      def text = dataLayer.addText(label)
          .x(drawX)
          .y(drawY)
          .fill(color)
          .styleClass('charm-text')

      text.addAttribute('font-size', size)
      text.addAttribute('font-family', family)
      text.addAttribute('text-anchor', textAnchor(hjust))
      text.addAttribute('dominant-baseline', dominantBaseline(vjust))
      if (fontface == 'bold' || fontface == 'bold.italic') {
        text.addAttribute('font-weight', 'bold')
      }
      if (fontface == 'italic' || fontface == 'bold.italic') {
        text.addAttribute('font-style', 'italic')
      }
      if (angle != 0) {
        text.addAttribute('transform', "rotate(${-angle}, ${drawX}, ${drawY})")
      }
      if (alpha < 1.0) {
        text.addAttribute('fill-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(text, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++
    }
  }

  private static String textAnchor(BigDecimal hjust) {
    if (hjust <= 0.25) {
      return 'start'
    }
    if (hjust >= 0.75) {
      return 'end'
    }
    'middle'
  }

  private static String dominantBaseline(BigDecimal vjust) {
    if (vjust <= 0.25) {
      return 'text-after-edge'
    }
    if (vjust >= 0.75) {
      return 'text-before-edge'
    }
    'middle'
  }
}
