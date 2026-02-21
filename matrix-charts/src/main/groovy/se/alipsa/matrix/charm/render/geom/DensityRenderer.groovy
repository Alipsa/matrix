package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext

/**
 * Renders kernel density geometry.
 */
@CompileStatic
class DensityRenderer {

  /**
   * Render density lines and optional filled area.
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
        BigDecimal x = context.xScale.transform(datum.x)
        BigDecimal y = context.yScale.transform(datum.y)
        if (x != null && y != null) {
          points << ([x, y] as BigDecimal[])
        }
      }
      if (points.size() < 2) {
        return
      }

      LayerData first = sorted.first()
      String stroke = GeomUtils.resolveStroke(context, layer, first)
      String fill = GeomUtils.resolveFill(context, layer, first)
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, first)
      String dashArray = GeomUtils.dashArray(first.linetype ?: layer.params.linetype)
      boolean drawFill = layer.params.fill != null || first.fill != null

      if (drawFill) {
        StringBuilder areaPath = new StringBuilder()
        areaPath.append("M ${points[0][0]} ${baseline}")
        points.each { BigDecimal[] pt ->
          areaPath.append(" L ${pt[0]} ${pt[1]}")
        }
        areaPath.append(" L ${points.last()[0]} ${baseline} Z")

        def area = dataLayer.addPath().d(areaPath.toString())
            .fill(fill)
            .stroke('none')
            .styleClass('charm-density-area')
        if (alpha < 1.0) {
          area.addAttribute('fill-opacity', alpha)
        }
        GeomUtils.applyCssAttributes(area, context, layer.geomType.name(), elementIndex, first)
        elementIndex++
      }

      for (int i = 0; i < points.size() - 1; i++) {
        def line = dataLayer.addLine(points[i][0], points[i][1], points[i + 1][0], points[i + 1][1])
            .stroke(stroke)
            .strokeWidth(1)
            .styleClass('charm-density')
        if (dashArray != null) {
          line.addAttribute('stroke-dasharray', dashArray)
        }
        if (alpha < 1.0 && !drawFill) {
          line.addAttribute('stroke-opacity', alpha)
        }
        GeomUtils.applyCssAttributes(line, context, layer.geomType.name(), elementIndex, sorted[i])
        elementIndex++
      }
    }
  }
}
