package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataRowAccess
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

    G boxplotGroup = dataLayer.addG().styleClass('geom-boxplot')
    int elementIndex = 0
    boolean discreteX = context.xScale.isDiscrete()
    BigDecimal baseBoxWidth
    if (discreteX && context.xScale instanceof DiscreteCharmScale) {
      DiscreteCharmScale scale = context.xScale as DiscreteCharmScale
      BigDecimal step = scale.levels.isEmpty() ? 20 : (scale.rangeEnd - scale.rangeStart) / scale.levels.size()
      baseBoxWidth = step * 0.5
    } else {
      baseBoxWidth = NumberCoercionUtil.coerceToBigDecimal(layer.params.boxWidth) ?: 20
    }

    layerData.each { LayerData datum ->
      Object xValue = datum.x ?: LayerDataRowAccess.value(datum, 'x')
      BigDecimal xCenter = context.xScale.transform(xValue)
      if (xCenter == null) {
        return
      }
      BigDecimal boxWidth = baseBoxWidth
      BigDecimal xmin = NumberCoercionUtil.coerceToBigDecimal(datum.xmin ?: LayerDataRowAccess.value(datum, 'xmin'))
      BigDecimal xmax = NumberCoercionUtil.coerceToBigDecimal(datum.xmax ?: LayerDataRowAccess.value(datum, 'xmax'))
      if (xmin != null && xmax != null) {
        BigDecimal x1 = context.xScale.transform(xmin)
        BigDecimal x2 = context.xScale.transform(xmax)
        if (x1 != null && x2 != null) {
          boxWidth = (x2 - x1).abs()
          xCenter = (x1 + x2) / 2
        }
      } else {
        BigDecimal relVarWidth = NumberCoercionUtil.coerceToBigDecimal(datum.meta.relvarwidth)
        if (relVarWidth != null && relVarWidth > 0) {
          boxWidth = baseBoxWidth * relVarWidth
        }
      }

      Object q1Value = datum.meta.q1 ?: LayerDataRowAccess.value(datum, 'q1') ?: LayerDataRowAccess.value(datum, 'lower')
      Object medianValue = datum.meta.median ?: LayerDataRowAccess.value(datum, 'middle') ?: LayerDataRowAccess.value(datum, 'median')
      Object q3Value = datum.meta.q3 ?: LayerDataRowAccess.value(datum, 'q3') ?: LayerDataRowAccess.value(datum, 'upper')
      Object whiskerLowValue = datum.meta.whiskerLow ?: LayerDataRowAccess.value(datum, 'whiskerLow') ?: LayerDataRowAccess.value(datum, 'ymin')
      Object whiskerHighValue = datum.meta.whiskerHigh ?: LayerDataRowAccess.value(datum, 'whiskerHigh') ?: LayerDataRowAccess.value(datum, 'ymax')
      BigDecimal q1 = context.yScale.transform(q1Value)
      BigDecimal median = context.yScale.transform(medianValue)
      BigDecimal q3 = context.yScale.transform(q3Value)
      BigDecimal whiskerLow = context.yScale.transform(whiskerLowValue)
      BigDecimal whiskerHigh = context.yScale.transform(whiskerHighValue)
      if ([q1, median, q3, whiskerLow, whiskerHigh].any { it == null }) {
        return
      }

      String fill = GeomUtils.resolveFill(context, layer, datum)
      String stroke = GeomUtils.resolveStroke(context, layer, datum)
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)

      BigDecimal xLeft = xCenter - boxWidth / 2
      BigDecimal boxTop = [q1, q3].min()
      BigDecimal boxHeight = (q3 - q1).abs()

      def box = boxplotGroup.addRect(boxWidth, boxHeight)
          .x(xLeft).y(boxTop)
          .fill(fill).stroke(stroke)
          .styleClass('charm-boxplot-box')
      if (alpha < 1.0) {
        box.addAttribute('fill-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(box, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++

      def medianLine = boxplotGroup.addLine(xLeft, median, xLeft + boxWidth, median)
          .stroke(stroke).strokeWidth(2)
          .styleClass('charm-boxplot-median')
      GeomUtils.applyCssAttributes(medianLine, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++
      def highWhisker = boxplotGroup.addLine(xCenter, boxTop, xCenter, whiskerHigh)
          .stroke(stroke).styleClass('charm-boxplot-whisker')
      GeomUtils.applyCssAttributes(highWhisker, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++
      BigDecimal boxBottom = [q1, q3].max()
      def lowWhisker = boxplotGroup.addLine(xCenter, boxBottom, xCenter, whiskerLow)
          .stroke(stroke).styleClass('charm-boxplot-whisker')
      GeomUtils.applyCssAttributes(lowWhisker, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++

      BigDecimal capHalf = boxWidth / 4
      def highCap = boxplotGroup.addLine(xCenter - capHalf, whiskerHigh, xCenter + capHalf, whiskerHigh)
          .stroke(stroke).styleClass('charm-boxplot-cap')
      GeomUtils.applyCssAttributes(highCap, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++
      def lowCap = boxplotGroup.addLine(xCenter - capHalf, whiskerLow, xCenter + capHalf, whiskerLow)
          .stroke(stroke).styleClass('charm-boxplot-cap')
      GeomUtils.applyCssAttributes(lowCap, context, layer.geomType.name(), elementIndex, datum)
      elementIndex++

      Object outlierValue = datum.meta.outliers ?: LayerDataRowAccess.value(datum, 'outliers')
      List outliers = outlierValue instanceof List ? outlierValue as List : []
      outliers.each { Object outlierVal ->
        BigDecimal oy = context.yScale.transform(outlierVal)
        if (oy != null) {
          def outlier = boxplotGroup.addCircle().cx(xCenter).cy(oy).r(3)
              .fill('none').stroke(stroke)
              .styleClass('charm-boxplot-outlier')
          GeomUtils.applyCssAttributes(outlier, context, layer.geomType.name(), elementIndex, datum)
          elementIndex++
        }
      }
    }
  }
}
