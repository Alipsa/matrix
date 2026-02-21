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

      for (int i = 1; i < sorted.size(); i++) {
        BigDecimal xPrev = context.xScale.transform(sorted[i - 1].x)
        BigDecimal yPrev = context.yScale.transform(sorted[i - 1].y)
        BigDecimal xCurr = context.xScale.transform(sorted[i].x)
        BigDecimal yCurr = context.yScale.transform(sorted[i].y)
        if (xPrev == null || yPrev == null || xCurr == null || yCurr == null) {
          continue
        }

        List<BigDecimal[]> segments = []
        switch (direction) {
          case 'vh' -> {
            segments << ([xPrev, yPrev, xPrev, yCurr] as BigDecimal[])
            segments << ([xPrev, yCurr, xCurr, yCurr] as BigDecimal[])
          }
          case 'mid' -> {
            BigDecimal midX = (xPrev + xCurr) / 2
            segments << ([xPrev, yPrev, midX, yPrev] as BigDecimal[])
            segments << ([midX, yPrev, midX, yCurr] as BigDecimal[])
            segments << ([midX, yCurr, xCurr, yCurr] as BigDecimal[])
          }
          default -> {
            segments << ([xPrev, yPrev, xCurr, yPrev] as BigDecimal[])
            segments << ([xCurr, yPrev, xCurr, yCurr] as BigDecimal[])
          }
        }

        segments.each { BigDecimal[] segment ->
          def line = dataLayer.addLine(segment[0], segment[1], segment[2], segment[3])
              .stroke(stroke)
              .strokeWidth(lineWidth)
              .styleClass('charm-step')
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
}
