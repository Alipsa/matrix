package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext

/**
 * Renders step geometry.
 */
@CompileStatic
class StepRenderer {

  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    if (layerData.size() < 2) {
      return
    }

    String direction = layer.params.direction?.toString()?.toLowerCase() ?: 'hv'
    int elementIndex = 0

    Map<Object, List<LayerData>> groups = GeomUtils.groupSeries(layerData)
    groups.each { Object _, List<LayerData> groupData ->
      List<LayerData> sorted = GeomUtils.sortByX(groupData)
      if (sorted.size() < 2) {
        return
      }

      LayerData first = sorted.first()
      String stroke = GeomUtils.resolveStroke(context, layer, first)
      BigDecimal lineWidth = GeomUtils.resolveLineWidth(context, layer, first)
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, first)
      String dashArray = GeomUtils.dashArray(GeomUtils.resolveLinetype(context, layer, first))

      List<BigDecimal[]> points = []
      for (int i = 1; i < sorted.size(); i++) {
        BigDecimal xPrev = context.xScaleForLayer(context.layerIndex).transform(sorted[i - 1].x)
        BigDecimal yPrev = context.yScaleForLayer(context.layerIndex).transform(sorted[i - 1].y)
        BigDecimal xCurr = context.xScaleForLayer(context.layerIndex).transform(sorted[i].x)
        BigDecimal yCurr = context.yScaleForLayer(context.layerIndex).transform(sorted[i].y)
        if (xPrev == null || yPrev == null || xCurr == null || yCurr == null) {
          continue
        }

        if (points.isEmpty()) {
          points << ([xPrev, yPrev] as BigDecimal[])
        }

        switch (direction) {
          case 'vh' -> {
            points << ([xPrev, yCurr] as BigDecimal[])
            points << ([xCurr, yCurr] as BigDecimal[])
          }
          case 'mid' -> {
            BigDecimal midX = (xPrev + xCurr) / 2
            points << ([midX, yPrev] as BigDecimal[])
            points << ([midX, yCurr] as BigDecimal[])
            points << ([xCurr, yCurr] as BigDecimal[])
          }
          default -> {
            points << ([xCurr, yPrev] as BigDecimal[])
            points << ([xCurr, yCurr] as BigDecimal[])
          }
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
          .styleClass('charm-step')
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
