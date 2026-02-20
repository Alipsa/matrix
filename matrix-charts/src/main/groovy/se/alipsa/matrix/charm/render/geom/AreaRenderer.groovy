package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext

/**
 * Renders area geometry.
 */
@CompileStatic
class AreaRenderer {

  /**
   * Render filled areas for grouped series.
   */
  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData, int panelHeight) {
    if (layerData.size() < 2) {
      return
    }

    int elementIndex = 0
    BigDecimal baseline = context.yScale.transform(0)
    if (baseline == null) {
      baseline = panelHeight
    }

    Map<Object, List<LayerData>> groups = GeomUtils.groupSeries(layerData)
    groups.each { Object _, List<LayerData> groupData ->
      List<LayerData> sorted = GeomUtils.sortByX(groupData)
      List<BigDecimal[]> points = []
      sorted.each { LayerData datum ->
        BigDecimal px = context.xScale.transform(datum.x)
        BigDecimal py = context.yScale.transform(datum.y)
        if (px != null && py != null) {
          points << ([px, py] as BigDecimal[])
        }
      }
      if (points.size() < 2) {
        return
      }

      LayerData firstDatum = sorted.first()
      String fill = GeomUtils.resolveFill(context, layer, firstDatum)
      String stroke = GeomUtils.resolveStroke(context, layer, firstDatum)
      BigDecimal alpha = GeomUtils.resolveAlpha(layer, firstDatum, 0.7)
      String dashArray = GeomUtils.dashArray(firstDatum.linetype ?: layer.params.linetype)

      StringBuilder pathD = new StringBuilder()
      pathD.append("M ${points[0][0]} ${baseline}")
      points.each { BigDecimal[] pt ->
        pathD.append(" L ${pt[0]} ${pt[1]}")
      }
      pathD.append(" L ${points.last()[0]} ${baseline} Z")

      def path = dataLayer.addPath().d(pathD.toString())
          .fill(fill)
          .stroke(stroke)
          .styleClass('charm-area')
      if (dashArray != null) {
        path.addAttribute('stroke-dasharray', dashArray)
      }
      if (alpha < 1.0) {
        path.addAttribute('fill-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(path, context, layer.geomType.name(), elementIndex, firstDatum)
      elementIndex++
    }
  }
}
