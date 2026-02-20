package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Renders smoothed lines, optionally with confidence bands.
 */
@CompileStatic
class SmoothRenderer {

  /**
   * Render smooth layer.
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
      String fill = GeomUtils.resolveFill(context, layer, first)
      BigDecimal lineWidth = NumberCoercionUtil.coerceToBigDecimal(first.size) ?:
          NumberCoercionUtil.coerceToBigDecimal(layer.params.lineWidth) ?:
          NumberCoercionUtil.coerceToBigDecimal(layer.params.size) ?: 1.5
      BigDecimal alpha = GeomUtils.resolveAlpha(layer, first)
      BigDecimal bandAlpha = NumberCoercionUtil.coerceToBigDecimal(layer.params.fillAlpha) ?: 0.2
      String dashArray = GeomUtils.dashArray(first.linetype ?: layer.params.linetype)

      List<BigDecimal[]> linePoints = []
      List<BigDecimal[]> upper = []
      List<BigDecimal[]> lower = []

      sorted.each { LayerData datum ->
        BigDecimal x = context.xScale.transform(datum.x)
        BigDecimal y = context.yScale.transform(datum.y)
        if (x != null && y != null) {
          linePoints << ([x, y] as BigDecimal[])
          BigDecimal yMax = context.yScale.transform(datum.meta?.ymax)
          BigDecimal yMin = context.yScale.transform(datum.meta?.ymin)
          if (yMax != null && yMin != null) {
            upper << ([x, yMax] as BigDecimal[])
            lower << ([x, yMin] as BigDecimal[])
          }
        }
      }

      if (linePoints.size() < 2) {
        return
      }

      if (upper.size() > 1 && lower.size() == upper.size()) {
        StringBuilder bandPath = new StringBuilder()
        bandPath.append("M ${upper[0][0]} ${upper[0][1]}")
        for (int i = 1; i < upper.size(); i++) {
          bandPath.append(" L ${upper[i][0]} ${upper[i][1]}")
        }
        for (int i = lower.size() - 1; i >= 0; i--) {
          bandPath.append(" L ${lower[i][0]} ${lower[i][1]}")
        }
        bandPath.append(' Z')
        def band = dataLayer.addPath().d(bandPath.toString())
            .fill(fill)
            .stroke('none')
            .addAttribute('fill-opacity', bandAlpha)
            .styleClass('charm-smooth-band')
        GeomUtils.applyCssAttributes(band, context, layer.geomType.name(), elementIndex, first)
        elementIndex++
      }

      for (int i = 0; i < linePoints.size() - 1; i++) {
        def line = dataLayer.addLine(linePoints[i][0], linePoints[i][1], linePoints[i + 1][0], linePoints[i + 1][1])
            .stroke(stroke)
            .strokeWidth(lineWidth)
            .styleClass('charm-smooth')
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
