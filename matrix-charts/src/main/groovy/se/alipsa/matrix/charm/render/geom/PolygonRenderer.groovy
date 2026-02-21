package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext

/**
 * Renders closed polygon geometry.
 */
@CompileStatic
class PolygonRenderer {

  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    if (layerData.size() < 3) {
      return
    }

    int elementIndex = 0
    Map<Object, List<LayerData>> groups = GeomUtils.groupSeries(layerData)
    groups.each { Object _, List<LayerData> groupData ->
      if (groupData.size() < 3) {
        return
      }

      List<BigDecimal[]> points = []
      groupData.each { LayerData datum ->
        BigDecimal x = context.xScale.transform(datum.x)
        BigDecimal y = context.yScale.transform(datum.y)
        if (x != null && y != null) {
          points << ([x, y] as BigDecimal[])
        }
      }
      if (points.size() < 3) {
        return
      }

      LayerData first = groupData.first()
      String fill = GeomUtils.resolveFill(context, layer, first)
      String stroke = GeomUtils.resolveStroke(context, layer, first)
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, first)
      BigDecimal lineWidth = GeomUtils.resolveLineWidth(context, layer, first, 0.5)
      String dashArray = GeomUtils.dashArray(GeomUtils.resolveLinetype(context, layer, first))

      StringBuilder pathD = new StringBuilder()
      pathD << "M ${points[0][0]} ${points[0][1]}"
      for (int i = 1; i < points.size(); i++) {
        pathD << " L ${points[i][0]} ${points[i][1]}"
      }
      pathD << ' Z'

      def path = dataLayer.addPath().d(pathD.toString())
          .fill(fill)
          .stroke(stroke)
          .addAttribute('stroke-width', lineWidth)
          .styleClass('charm-polygon')
      if (dashArray != null) {
        path.addAttribute('stroke-dasharray', dashArray)
      }
      if (alpha < 1.0) {
        path.addAttribute('opacity', alpha)
      }
      GeomUtils.applyCssAttributes(path, context, layer.geomType.name(), elementIndex, first)
      elementIndex++
    }
  }
}
