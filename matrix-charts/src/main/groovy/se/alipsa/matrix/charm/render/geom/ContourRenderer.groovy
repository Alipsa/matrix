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
      if (groupData.isEmpty()) {
        return
      }
      LayerData first = groupData.first()
      String stroke = GeomUtils.resolveStroke(context, layer, first)
      BigDecimal lineWidth = GeomUtils.resolveLineWidth(context, layer, first, 0.75)
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, first)
      String dashArray = GeomUtils.dashArray(GeomUtils.resolveLinetype(context, layer, first))

      List<BigDecimal[]> points = []
      groupData.each { LayerData datum ->
        BigDecimal x = context.xScaleForLayer(context.layerIndex).transform(datum.x)
        BigDecimal y = context.yScaleForLayer(context.layerIndex).transform(datum.y)
        if (x != null && y != null) {
          points << ([x, y] as BigDecimal[])
        }
      }
      if (points.isEmpty()) {
        return
      }

      StringBuilder pathD = new StringBuilder()
      pathD << "M ${fmt(points[0][0])} ${fmt(points[0][1])}"
      for (int i = 1; i < points.size(); i++) {
        pathD << " L ${fmt(points[i][0])} ${fmt(points[i][1])}"
      }
      if (points.size() == 1) {
        pathD << " L ${fmt(points[0][0])} ${fmt(points[0][1])}"
      }

      def path = dataLayer.addPath().d(pathD.toString())
          .fill('none')
          .stroke(stroke)
          .strokeWidth(lineWidth)
          .styleClass('charm-contour')
      if (dashArray != null) {
        path.addAttribute('stroke-dasharray', dashArray)
      }
      if (alpha < 1.0) {
        path.addAttribute('stroke-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(path, context, layer.geomType.name(), elementIndex, first)
      elementIndex++
    }
  }

  private static String fmt(BigDecimal value) {
    value?.stripTrailingZeros()?.toPlainString() ?: '0'
  }
}
