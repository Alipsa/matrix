package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.SvgElement
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.core.ValueConverter

/**
 * Renders point geometry.
 */
@CompileStatic
class PointRenderer {

  /**
   * Render points for one layer.
   */
  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    BigDecimal defaultRadius = ValueConverter.asBigDecimal(layer.params.size) ?: context.config.pointRadius
    String defaultShape = layer.params.shape?.toString() ?: 'circle'
    int elementIndex = 0

    layerData.each { LayerData datum ->
      BigDecimal x = context.xScale.transform(datum.x)
      BigDecimal y = context.yScale.transform(datum.y)
      if (x == null || y == null) {
        return
      }
      BigDecimal radius = context.sizeScale != null && datum.size != null
          ? context.sizeScale.transform(datum.size)
          : ValueConverter.asBigDecimal(datum.size)
      if (radius == null) {
        radius = defaultRadius
      }
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)
      String shape = GeomUtils.resolveShape(context, layer, datum, defaultShape)
      String fill = GeomUtils.resolveFill(context, layer, datum)
      String stroke = GeomUtils.resolveStroke(context, layer, datum)
      String tooltip = GeomUtils.resolveTooltip(layer, datum)

      List<SvgElement> elements = GeomUtils.drawPoint(dataLayer, x, y, radius, fill, stroke, shape, alpha)
      elements.each { SvgElement element ->
        GeomUtils.addTooltip(element, tooltip)
        GeomUtils.applyCssAttributes(element, context, layer.geomType.name(), elementIndex, datum)
        elementIndex++
      }
    }
  }
}
