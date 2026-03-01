package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext

/**
 * Renders ribbon geometry from ymin/ymax bounds across x.
 */
@CompileStatic
class RibbonRenderer {

  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    if (layerData.size() < 2) {
      return
    }

    int elementIndex = 0
    Map<Object, List<LayerData>> groups = GeomUtils.groupSeries(layerData)
    groups.each { Object _, List<LayerData> groupData ->
      List<LayerData> sorted = GeomUtils.sortByX(groupData)
      List<BigDecimal[]> upper = []
      List<BigDecimal[]> lower = []
      sorted.each { LayerData datum ->
        BigDecimal x = context.xScaleForLayer(context.layerIndex).transform(datum.x)
        BigDecimal yMax = context.yScaleForLayer(context.layerIndex).transform(datum.ymax)
        BigDecimal yMin = context.yScaleForLayer(context.layerIndex).transform(datum.ymin)
        if (x != null && yMax != null && yMin != null) {
          upper << ([x, yMax] as BigDecimal[])
          lower << ([x, yMin] as BigDecimal[])
        }
      }
      if (upper.size() < 2 || lower.size() != upper.size()) {
        return
      }

      LayerData first = sorted.first()
      String fill = GeomUtils.resolveFill(context, layer, first)
      String stroke = GeomUtils.resolveStroke(context, layer, first)
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, first, 0.5)
      String dashArray = GeomUtils.dashArray(GeomUtils.resolveLinetype(context, layer, first))

      StringBuilder pathD = new StringBuilder()
      pathD << "M ${upper[0][0]} ${upper[0][1]}"
      for (int i = 1; i < upper.size(); i++) {
        pathD << " L ${upper[i][0]} ${upper[i][1]}"
      }
      for (int i = lower.size() - 1; i >= 0; i--) {
        pathD << " L ${lower[i][0]} ${lower[i][1]}"
      }
      pathD << ' Z'

      def path = dataLayer.addPath().d(pathD.toString())
          .fill(fill)
          .stroke(stroke)
          .styleClass('charm-ribbon')
      if (dashArray != null) {
        path.addAttribute('stroke-dasharray', dashArray)
      }
      if (alpha < 1.0) {
        path.addAttribute('fill-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(path, context, layer.geomType.name(), elementIndex, first)
      elementIndex++
    }
  }
}
