package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext

/**
 * Renders path geometry preserving input data order.
 */
@CompileStatic
class PathRenderer {

  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    if (layerData.size() < 2) {
      return
    }

    int elementIndex = 0
    Map<Object, List<LayerData>> groups = GeomUtils.groupSeries(layerData)
    groups.each { Object _, List<LayerData> groupData ->
      if (groupData.size() < 2) {
        return
      }

      LayerData first = groupData.first()
      String stroke = GeomUtils.resolveStroke(context, layer, first)
      BigDecimal lineWidth = GeomUtils.resolveLineWidth(context, layer, first)
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
      if (points.size() < 2) {
        return
      }

      StringBuilder pathD = new StringBuilder()
      pathD << "M ${fmt(points[0][0])} ${fmt(points[0][1])}"
      for (int i = 1; i < points.size(); i++) {
        pathD << " L ${fmt(points[i][0])} ${fmt(points[i][1])}"
      }

      def path = dataLayer.addPath().d(pathD.toString())
          .fill('none')
          .stroke(stroke)
          .strokeWidth(lineWidth)
          .styleClass('charm-path')
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
