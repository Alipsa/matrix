package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Renders hexagon glyphs centered at x/y coordinates.
 */
@CompileStatic
class HexRenderer {

  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    BigDecimal width = NumberCoercionUtil.coerceToBigDecimal(layer.params.binwidth) ?:
        NumberCoercionUtil.coerceToBigDecimal(layer.params.size) ?: 8
    BigDecimal height = width * ((3.0 as BigDecimal).sqrt() / 2)

    int elementIndex = 0
    layerData.each { LayerData datum ->
      BigDecimal cx = context.xScale.transform(datum.x)
      BigDecimal cy = context.yScale.transform(datum.y)
      if (cx == null || cy == null) {
        return
      }

      String fill = GeomUtils.resolveFill(context, layer, datum)
      String stroke = GeomUtils.resolveStroke(context, layer, datum)
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)
      BigDecimal lineWidth = GeomUtils.resolveLineWidth(context, layer, datum, 0.5)
      String pathData = createHexagonPath(cx, cy, width, height)

      def path = dataLayer.addPath()
          .d(pathData)
          .fill(fill)
          .stroke(stroke)
          .addAttribute('stroke-width', lineWidth)
          .styleClass('charm-hex')
      if (alpha < 1.0) {
        path.addAttribute('fill-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(path, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++
    }
  }

  private static String createHexagonPath(Number cx, Number cy, Number width, Number height) {
    BigDecimal w = width / 2
    BigDecimal h = height / 2
    List<BigDecimal[]> points = [
        [cx - w / 2, cy - h] as BigDecimal[],
        [cx + w / 2, cy - h] as BigDecimal[],
        [cx + w, cy] as BigDecimal[],
        [cx + w / 2, cy + h] as BigDecimal[],
        [cx - w / 2, cy + h] as BigDecimal[],
        [cx - w, cy] as BigDecimal[]
    ]
    StringBuilder path = new StringBuilder("M ${points[0][0]} ${points[0][1]}")
    for (int i = 1; i < points.size(); i++) {
      path << " L ${points[i][0]} ${points[i][1]}"
    }
    path << ' Z'
    path.toString()
  }
}
