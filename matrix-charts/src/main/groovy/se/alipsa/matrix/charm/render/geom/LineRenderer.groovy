package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.core.ValueConverter

/**
 * Renders line geometry.
 */
@CompileStatic
class LineRenderer {

  /**
   * Render line segments grouped and ordered by x.
   */
  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    if (layerData.size() < 2) {
      return
    }

    int elementIndex = 0
    Map<Object, List<LayerData>> groups = GeomUtils.groupSeries(layerData)
    groups.each { Object _, List<LayerData> groupData ->
      List<LayerData> sorted = GeomUtils.sortByX(groupData)
      if (sorted.size() < 2) {
        return
      }

      LayerData first = sorted.first()
      String stroke = GeomUtils.resolveStroke(context, layer, first)
      BigDecimal lineWidth = ValueConverter.asBigDecimal(first.size) ?:
          ValueConverter.asBigDecimal(layer.params.lineWidth) ?:
          ValueConverter.asBigDecimal(layer.params.size) ?: 2
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, first)
      String dashArray = GeomUtils.dashArray(first.linetype ?: layer.params.linetype)

      for (int i = 0; i < sorted.size() - 1; i++) {
        BigDecimal x1 = context.xScale.transform(sorted[i].x)
        BigDecimal y1 = context.yScale.transform(sorted[i].y)
        BigDecimal x2 = context.xScale.transform(sorted[i + 1].x)
        BigDecimal y2 = context.yScale.transform(sorted[i + 1].y)
        if (x1 == null || y1 == null || x2 == null || y2 == null) {
          continue
        }
        def line = dataLayer.addLine(x1, y1, x2, y2)
            .stroke(stroke)
            .strokeWidth(lineWidth)
            .styleClass('charm-line')
        if (dashArray != null) {
          line.addAttribute('stroke-dasharray', dashArray)
        }
        if (alpha < 1.0) {
          line.addAttribute('stroke-opacity', alpha)
        }
        GeomUtils.applyCssAttributes(line, context, layer.geomType.name(), elementIndex, sorted[i])
        elementIndex++
      }
    }
  }
}
