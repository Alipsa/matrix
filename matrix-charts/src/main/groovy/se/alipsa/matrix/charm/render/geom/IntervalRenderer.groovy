package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.render.scale.CharmScale
import se.alipsa.matrix.charm.render.scale.DiscreteCharmScale
import se.alipsa.matrix.core.ValueConverter

/**
 * Renders interval geometries: errorbar, errorbarh, crossbar, linerange, pointrange.
 */
@CompileStatic
class IntervalRenderer {

  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    if (layerData.isEmpty()) {
      return
    }

    BigDecimal widthRatio = ValueConverter.asBigDecimal(layer.params.width) ?: 0.5
    int elementIndex = 0

    layerData.each { LayerData datum ->
      int nextElementIndex = elementIndex
      BigDecimal x = context.xScaleForLayer(context.layerIndex).transform(datum.x)
      BigDecimal y = context.yScaleForLayer(context.layerIndex).transform(datum.y)
      BigDecimal xmin = context.xScaleForLayer(context.layerIndex).transform(datum.xmin)
      BigDecimal xmax = context.xScaleForLayer(context.layerIndex).transform(datum.xmax)
      BigDecimal ymin = context.yScaleForLayer(context.layerIndex).transform(datum.ymin)
      BigDecimal ymax = context.yScaleForLayer(context.layerIndex).transform(datum.ymax)

      switch (layer.geomType) {
        case CharmGeomType.ERRORBAR -> {
          if (x != null && ymin != null && ymax != null) {
            BigDecimal halfWidth = capHalfWidth(context.xScaleForLayer(context.layerIndex), x, widthRatio)
            drawSegment(dataLayer, context, layer, datum, x, ymin, x, ymax, 'charm-errorbar', nextElementIndex++)
            drawSegment(dataLayer, context, layer, datum, x - halfWidth, ymin, x + halfWidth, ymin, 'charm-errorbar', nextElementIndex++)
            drawSegment(dataLayer, context, layer, datum, x - halfWidth, ymax, x + halfWidth, ymax, 'charm-errorbar', nextElementIndex++)
          }
        }
        case CharmGeomType.ERRORBARH -> {
          if (y != null && xmin != null && xmax != null) {
            BigDecimal halfHeight = capHalfWidth(context.yScaleForLayer(context.layerIndex), y, widthRatio)
            drawSegment(dataLayer, context, layer, datum, xmin, y, xmax, y, 'charm-errorbarh', nextElementIndex++)
            drawSegment(dataLayer, context, layer, datum, xmin, y - halfHeight, xmin, y + halfHeight, 'charm-errorbarh', nextElementIndex++)
            drawSegment(dataLayer, context, layer, datum, xmax, y - halfHeight, xmax, y + halfHeight, 'charm-errorbarh', nextElementIndex++)
          }
        }
        case CharmGeomType.LINERANGE -> {
          if (x != null && ymin != null && ymax != null) {
            drawSegment(dataLayer, context, layer, datum, x, ymin, x, ymax, 'charm-linerange', nextElementIndex++)
          }
        }
        case CharmGeomType.CROSSBAR -> {
          if (x != null && y != null && ymin != null && ymax != null) {
            BigDecimal halfWidth = capHalfWidth(context.xScaleForLayer(context.layerIndex), x, widthRatio)
            drawSegment(dataLayer, context, layer, datum, x, ymin, x, ymax, 'charm-crossbar', nextElementIndex++)
            drawSegment(dataLayer, context, layer, datum, x - halfWidth, y, x + halfWidth, y, 'charm-crossbar', nextElementIndex++)
          }
        }
        case CharmGeomType.POINTRANGE -> {
          if (x != null && y != null && ymin != null && ymax != null) {
            drawSegment(dataLayer, context, layer, datum, x, ymin, x, ymax, 'charm-pointrange', nextElementIndex++)
            BigDecimal pointRadius = ValueConverter.asBigDecimal(layer.params.size) ?: 3
            String fill = GeomUtils.resolveFill(context, layer, datum)
            String stroke = GeomUtils.resolveStroke(context, layer, datum)
            BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)
            GeomUtils.drawPoint(dataLayer, x, y, pointRadius, fill, stroke, 'circle', alpha).each { point ->
              point.styleClass('charm-pointrange-point')
              GeomUtils.applyCssAttributes(point, context, layer.geomType.name(), nextElementIndex++, datum)
            }
          }
        }
        default -> {
          // no-op
        }
      }
      elementIndex = nextElementIndex
    }
  }

  private static BigDecimal capHalfWidth(CharmScale scale, BigDecimal center, BigDecimal widthRatio) {
    if (scale instanceof DiscreteCharmScale) {
      return (scale as DiscreteCharmScale).bandwidth * widthRatio / 2
    }
    BigDecimal span = (scale.rangeEnd - scale.rangeStart).abs()
    span * widthRatio / 100
  }

  private static void drawSegment(
      G dataLayer,
      RenderContext context,
      LayerSpec layer,
      LayerData datum,
      BigDecimal x1,
      BigDecimal y1,
      BigDecimal x2,
      BigDecimal y2,
      String cssClass,
      int elementIndex
  ) {
    String stroke = GeomUtils.resolveStroke(context, layer, datum)
    BigDecimal lineWidth = GeomUtils.resolveLineWidth(context, layer, datum)
    BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, datum)
    String dashArray = GeomUtils.dashArray(GeomUtils.resolveLinetype(context, layer, datum))

    def line = dataLayer.addLine(x1, y1, x2, y2)
        .stroke(stroke)
        .strokeWidth(lineWidth)
        .styleClass(cssClass)
    if (dashArray != null) {
      line.addAttribute('stroke-dasharray', dashArray)
    }
    if (alpha < 1.0) {
      line.addAttribute('stroke-opacity', alpha)
    }
    GeomUtils.applyCssAttributes(line, context, layer.geomType.name(), elementIndex, datum)
  }
}
