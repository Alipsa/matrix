package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.render.scale.ContinuousCharmScale
import se.alipsa.matrix.charm.util.NumberCoercionUtil

/**
 * Renders segment-like geometries: segment, hline, vline, and abline.
 */
@CompileStatic
class SegmentRenderer {

  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    int elementIndex = 0
    LayerData styleDatum = layerData.isEmpty()
        ? new LayerData(rowIndex: -1, meta: [:])
        : layerData.first()
    switch (layer.geomType) {
      case CharmGeomType.HLINE -> {
        List<BigDecimal> yValues = collectReferenceValues(layerData, layer.params.yintercept, true)
        yValues.each { BigDecimal yRef ->
          BigDecimal yPx = context.yScale.transform(yRef)
          if (yPx == null) {
            return
          }
          drawLine(dataLayer, context, layer, styleDatum, context.xScale.rangeStart, yPx, context.xScale.rangeEnd, yPx, elementIndex)
          elementIndex++
        }
      }
      case CharmGeomType.VLINE -> {
        List<BigDecimal> xValues = collectReferenceValues(layerData, layer.params.xintercept, false)
        xValues.each { BigDecimal xRef ->
          BigDecimal xPx = context.xScale.transform(xRef)
          if (xPx == null) {
            return
          }
          drawLine(dataLayer, context, layer, styleDatum, xPx, context.yScale.rangeStart, xPx, context.yScale.rangeEnd, elementIndex)
          elementIndex++
        }
      }
      case CharmGeomType.ABLINE -> {
        BigDecimal intercept = NumberCoercionUtil.coerceToBigDecimal(layer.params.intercept) ?: 0
        BigDecimal slope = NumberCoercionUtil.coerceToBigDecimal(layer.params.slope) ?: 1
        if (context.xScale instanceof ContinuousCharmScale) {
          ContinuousCharmScale xScale = context.xScale as ContinuousCharmScale
          BigDecimal x1 = xScale.domainMin
          BigDecimal x2 = xScale.domainMax
          BigDecimal y1 = intercept + slope * x1
          BigDecimal y2 = intercept + slope * x2
          BigDecimal px1 = context.xScale.transform(x1)
          BigDecimal py1 = context.yScale.transform(y1)
          BigDecimal px2 = context.xScale.transform(x2)
          BigDecimal py2 = context.yScale.transform(y2)
          if (px1 != null && py1 != null && px2 != null && py2 != null) {
            drawLine(dataLayer, context, layer, styleDatum, px1, py1, px2, py2, elementIndex)
          }
        }
      }
      default -> {
        layerData.each { LayerData datum ->
          BigDecimal x1 = context.xScale.transform(datum.x)
          BigDecimal y1 = context.yScale.transform(datum.y)
          BigDecimal x2 = context.xScale.transform(datum.xend)
          BigDecimal y2 = context.yScale.transform(datum.yend)
          if (x1 == null || y1 == null || x2 == null || y2 == null) {
            return
          }
          drawLine(dataLayer, context, layer, datum, x1, y1, x2, y2, elementIndex)
          elementIndex++
        }
      }
    }
  }

  private static List<BigDecimal> collectReferenceValues(List<LayerData> layerData, Object layerParam, boolean horizontal) {
    List<BigDecimal> values = layerData.collect { LayerData datum ->
      Object value = horizontal
          ? (datum.y != null ? datum.y : datum.meta?.yintercept)
          : (datum.x != null ? datum.x : datum.meta?.xintercept)
      NumberCoercionUtil.coerceToBigDecimal(value)
    }.findAll { it != null } as List<BigDecimal>
    BigDecimal paramValue = NumberCoercionUtil.coerceToBigDecimal(layerParam)
    if (paramValue != null) {
      values << paramValue
    }
    values.unique()
  }

  private static void drawLine(
      G dataLayer,
      RenderContext context,
      LayerSpec layer,
      LayerData datum,
      BigDecimal x1,
      BigDecimal y1,
      BigDecimal x2,
      BigDecimal y2,
      int elementIndex
  ) {
    String stroke = GeomUtils.resolveStroke(context, layer, datum)
    BigDecimal lineWidth = GeomUtils.resolveLineWidth(context, layer, datum)
    BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)
    String dashArray = GeomUtils.dashArray(GeomUtils.resolveLinetype(context, layer, datum))

    def line = dataLayer.addLine(x1, y1, x2, y2)
        .stroke(stroke)
        .strokeWidth(lineWidth)
        .styleClass('charm-segment')
    if (dashArray != null) {
      line.addAttribute('stroke-dasharray', dashArray)
    }
    if (alpha < 1.0) {
      line.addAttribute('stroke-opacity', alpha)
    }
    GeomUtils.applyCssAttributes(line, context, layer.geomType.name(), elementIndex, datum)
  }
}
