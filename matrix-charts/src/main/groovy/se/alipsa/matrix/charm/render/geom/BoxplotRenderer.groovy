package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.render.scale.DiscreteCharmScale
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Renders boxplot geometry.
 */
@CompileStatic
class BoxplotRenderer {

  /**
   * Render boxplots from stat_boxplot output fields.
   */
  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    if (layerData.isEmpty()) {
      return
    }

    boolean discreteX = context.xScale.isDiscrete()
    BigDecimal boxWidth
    if (discreteX && context.xScale instanceof DiscreteCharmScale) {
      DiscreteCharmScale scale = context.xScale as DiscreteCharmScale
      BigDecimal step = scale.levels.isEmpty() ? 20 : (scale.rangeEnd - scale.rangeStart) / scale.levels.size()
      boxWidth = step * 0.5
    } else {
      boxWidth = NumberCoercionUtil.coerceToBigDecimal(layer.params.boxWidth) ?: 20
    }

    layerData.each { LayerData datum ->
      BigDecimal xCenter = context.xScale.transform(datum.x)
      if (xCenter == null) {
        return
      }
      BigDecimal q1 = context.yScale.transform(datum.meta.q1)
      BigDecimal median = context.yScale.transform(datum.meta.median)
      BigDecimal q3 = context.yScale.transform(datum.meta.q3)
      BigDecimal whiskerLow = context.yScale.transform(datum.meta.whiskerLow)
      BigDecimal whiskerHigh = context.yScale.transform(datum.meta.whiskerHigh)
      if ([q1, median, q3, whiskerLow, whiskerHigh].any { it == null }) {
        return
      }

      String fill = GeomUtils.resolveFill(context, layer, datum)
      String stroke = GeomUtils.resolveStroke(context, layer, datum)
      BigDecimal alpha = GeomUtils.resolveAlpha(layer, datum)

      BigDecimal xLeft = xCenter - boxWidth / 2
      BigDecimal boxTop = [q1, q3].min()
      BigDecimal boxHeight = (q3 - q1).abs()

      def box = dataLayer.addRect(boxWidth, boxHeight)
          .x(xLeft).y(boxTop)
          .fill(fill).stroke(stroke)
          .styleClass('charm-boxplot-box')
      if (alpha < 1.0) {
        box.addAttribute('fill-opacity', alpha)
      }

      dataLayer.addLine(xLeft, median, xLeft + boxWidth, median)
          .stroke(stroke).strokeWidth(2)
          .styleClass('charm-boxplot-median')
      dataLayer.addLine(xCenter, boxTop, xCenter, whiskerHigh)
          .stroke(stroke).styleClass('charm-boxplot-whisker')
      BigDecimal boxBottom = [q1, q3].max()
      dataLayer.addLine(xCenter, boxBottom, xCenter, whiskerLow)
          .stroke(stroke).styleClass('charm-boxplot-whisker')

      BigDecimal capHalf = boxWidth / 4
      dataLayer.addLine(xCenter - capHalf, whiskerHigh, xCenter + capHalf, whiskerHigh)
          .stroke(stroke).styleClass('charm-boxplot-cap')
      dataLayer.addLine(xCenter - capHalf, whiskerLow, xCenter + capHalf, whiskerLow)
          .stroke(stroke).styleClass('charm-boxplot-cap')

      List outliers = datum.meta.outliers as List ?: []
      outliers.each { Object outlierVal ->
        BigDecimal oy = context.yScale.transform(outlierVal)
        if (oy != null) {
          dataLayer.addCircle().cx(xCenter).cy(oy).r(3)
              .fill('none').stroke(stroke)
              .styleClass('charm-boxplot-outlier')
        }
      }
    }
  }
}
