package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext

/**
 * Renders contour line geometry from ordered x/y points grouped by contour level/group.
 */
@CompileStatic
class ContourRenderer {

  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    if (layerData.size() < 2) {
      return
    }

    int elementIndex = 0
    Map<Object, List<LayerData>> groups = new LinkedHashMap<>()
    layerData.each { LayerData datum ->
      Object key = datum.group ?: datum.meta?.level ?: '__all__'
      List<LayerData> bucket = groups[key]
      if (bucket == null) {
        bucket = []
        groups[key] = bucket
      }
      bucket << datum
    }

    groups.each { Object _, List<LayerData> groupData ->
      if (groupData.size() < 2) {
        return
      }
      LayerData first = groupData.first()
      String stroke = GeomUtils.resolveStroke(context, layer, first)
      BigDecimal lineWidth = GeomUtils.resolveLineWidth(context, layer, first, 0.75)
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, first)
      String dashArray = GeomUtils.dashArray(GeomUtils.resolveLinetype(context, layer, first))

      for (int i = 0; i < groupData.size() - 1; i++) {
        BigDecimal x1 = context.xScale.transform(groupData[i].x)
        BigDecimal y1 = context.yScale.transform(groupData[i].y)
        BigDecimal x2 = context.xScale.transform(groupData[i + 1].x)
        BigDecimal y2 = context.yScale.transform(groupData[i + 1].y)
        if (x1 == null || y1 == null || x2 == null || y2 == null) {
          continue
        }

        def line = dataLayer.addLine(x1, y1, x2, y2)
            .stroke(stroke)
            .strokeWidth(lineWidth)
            .styleClass('charm-contour')
        if (dashArray != null) {
          line.addAttribute('stroke-dasharray', dashArray)
        }
        if (alpha < 1.0) {
          line.addAttribute('stroke-opacity', alpha)
        }
        GeomUtils.applyCssAttributes(line, context, layer.geomType.name(), elementIndex, groupData[i])
        elementIndex++
      }
    }
  }
}
