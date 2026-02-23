package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import se.alipsa.matrix.charm.render.scale.DiscreteCharmScale
import se.alipsa.matrix.core.ValueConverter

/**
 * Renders violin geometry from y-density stat output.
 */
@CompileStatic
class ViolinRenderer {

  /**
   * Render violins grouped by center x category.
   */
  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    if (layerData.size() < 2) {
      return
    }

    int elementIndex = 0
    BigDecimal maxDensity = layerData.collect { LayerData d ->
      ValueConverter.asBigDecimal(d.meta.density)
    }.findAll { it != null }.max() ?: 0
    if (maxDensity <= 0) {
      return
    }

    BigDecimal baseHalfWidth
    if (context.xScale instanceof DiscreteCharmScale) {
      DiscreteCharmScale scale = context.xScale as DiscreteCharmScale
      BigDecimal step = scale.levels.isEmpty() ? 20 : (scale.rangeEnd - scale.rangeStart) / scale.levels.size()
      baseHalfWidth = step * 0.45
    } else {
      baseHalfWidth = ValueConverter.asBigDecimal(layer.params.width) ?: 20
    }

    Map<Object, List<LayerData>> groups = new LinkedHashMap<>()
    layerData.each { LayerData datum ->
      Object key = datum.meta.centerX ?: datum.x
      List<LayerData> bucket = groups[key]
      if (bucket == null) {
        bucket = []
        groups[key] = bucket
      }
      bucket << datum
    }

    groups.each { Object centerKey, List<LayerData> groupData ->
      List<LayerData> sorted = new ArrayList<>(groupData)
      sorted.sort { LayerData a, LayerData b ->
        BigDecimal y1 = ValueConverter.asBigDecimal(a.y)
        BigDecimal y2 = ValueConverter.asBigDecimal(b.y)
        if (y1 != null && y2 != null) {
          return y1 <=> y2
        }
        0
      }

      BigDecimal centerX = context.xScale.transform(centerKey)
      if (centerX == null) {
        return
      }

      List<BigDecimal[]> right = []
      List<BigDecimal[]> left = []
      sorted.each { LayerData datum ->
        BigDecimal yPx = context.yScale.transform(datum.y)
        BigDecimal density = ValueConverter.asBigDecimal(datum.meta.density)
        if (yPx == null || density == null) {
          return
        }
        BigDecimal halfWidth = density / maxDensity * baseHalfWidth
        right << ([centerX + halfWidth, yPx] as BigDecimal[])
        left << ([centerX - halfWidth, yPx] as BigDecimal[])
      }

      if (right.size() < 2 || left.size() < 2) {
        return
      }

      LayerData first = sorted.first()
      String fill = GeomUtils.resolveFill(context, layer, first)
      String stroke = GeomUtils.resolveStroke(context, layer, first)
      BigDecimal alpha = GeomUtils.resolveAlpha(context, layer, first, 0.7)

      StringBuilder d = new StringBuilder()
      d.append("M ${right[0][0]} ${right[0][1]}")
      for (int i = 1; i < right.size(); i++) {
        d.append(" L ${right[i][0]} ${right[i][1]}")
      }
      for (int i = left.size() - 1; i >= 0; i--) {
        d.append(" L ${left[i][0]} ${left[i][1]}")
      }
      d.append(' Z')

      def path = dataLayer.addPath().d(d.toString())
          .fill(fill)
          .stroke(stroke)
          .styleClass('charm-violin')
      if (alpha < 1.0) {
        path.addAttribute('fill-opacity', alpha)
      }
      GeomUtils.applyCssAttributes(path, context, layer.geomType.name(), elementIndex, first)
      elementIndex++
    }
  }
}
