package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.core.ValueConverter

import static se.alipsa.matrix.ext.NumberExtension.PI

/**
 * Renders pie geometry.
 */
@CompileStatic
class PieRenderer {

  /**
   * Render pie slices from y values.
   */
  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData,
                     int panelWidth, int panelHeight) {
    if (layerData.isEmpty()) {
      return
    }

    G pieGroup = dataLayer.addG().styleClass('geombar')
    BigDecimal cx = panelWidth / 2.0
    BigDecimal cy = panelHeight / 2.0
    BigDecimal outerRadius = panelWidth.min(panelHeight) * 0.45
    BigDecimal innerRadiusRatio = ValueConverter.asBigDecimal(context.chart.coord?.params?.innerRadius)
    innerRadiusRatio = innerRadiusRatio == null ? 0.0 : innerRadiusRatio.max(0).min(1)
    BigDecimal innerRadius = outerRadius * innerRadiusRatio
    String theta = context.chart.coord?.params?.theta?.toString()?.toLowerCase() ?: 'x'
    BigDecimal start = ValueConverter.asBigDecimal(context.chart.coord?.params?.start) ?: (-PI / 2)
    Integer directionParam = context.chart.coord?.params?.direction as Integer
    boolean clockwise = context.chart.coord?.params?.containsKey('clockwise')
        ? context.chart.coord?.params?.clockwise as boolean
        : (directionParam == null || directionParam >= 0)

    List<BigDecimal> values = layerData.collect { LayerData datum ->
      BigDecimal ymin = ValueConverter.asBigDecimal(datum.ymin)
      BigDecimal ymax = ValueConverter.asBigDecimal(datum.ymax)
      BigDecimal v = (ymin != null && ymax != null)
          ? (ymax - ymin).abs()
          : ValueConverter.asBigDecimal(datum.y)
      v != null && v > 0 ? v : 0.0
    }
    BigDecimal total = values.sum() as BigDecimal
    if (total == 0) {
      return
    }

    List<Integer> order = (0..<layerData.size()).collect { int idx -> idx }
    if (theta == 'y') {
      order = order.reverse(false) as List<Integer>
    }

    BigDecimal currentAngle = start
    order.each { int idx ->
      LayerData datum = layerData[idx]
      BigDecimal slice = values[idx]
      if (slice == 0) {
        return
      }
      BigDecimal sweep = (slice / total) * 2 * PI
      BigDecimal nextAngle = clockwise ? currentAngle + sweep : currentAngle - sweep
      BigDecimal x1 = cx + outerRadius * currentAngle.cos()
      BigDecimal y1 = cy + outerRadius * currentAngle.sin()
      BigDecimal x2 = cx + outerRadius * nextAngle.cos()
      BigDecimal y2 = cy + outerRadius * nextAngle.sin()
      int largeArc = sweep > PI ? 1 : 0
      int sweepFlag = clockwise ? 1 : 0

      String pathD
      if (innerRadius > 0) {
        BigDecimal x3 = cx + innerRadius * nextAngle.cos()
        BigDecimal y3 = cy + innerRadius * nextAngle.sin()
        BigDecimal x4 = cx + innerRadius * currentAngle.cos()
        BigDecimal y4 = cy + innerRadius * currentAngle.sin()
        pathD = "M ${x1} ${y1} " +
            "A ${outerRadius} ${outerRadius} 0 ${largeArc} ${sweepFlag} ${x2} ${y2} " +
            "L ${x3} ${y3} " +
            "A ${innerRadius} ${innerRadius} 0 ${largeArc} ${1 - sweepFlag} ${x4} ${y4} Z"
      } else {
        pathD = "M ${cx} ${cy} L ${x1} ${y1} A ${outerRadius} ${outerRadius} 0 ${largeArc} ${sweepFlag} ${x2} ${y2} Z"
      }
      String fill = GeomUtils.resolveFill(context, layer, datum)
      def slicePath = pieGroup.addPath().d(pathD)
          .fill(fill)
          .stroke('#ffffff')
          .addAttribute('stroke-width', '1')
          .styleClass('charm-pie')
      GeomUtils.applyCssAttributes(slicePath, context, layer.geomType.name(), idx, datum)
      currentAngle = nextAngle
    }
  }
}
