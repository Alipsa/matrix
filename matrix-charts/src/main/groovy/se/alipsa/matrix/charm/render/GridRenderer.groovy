package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.CharmCoordType
import se.alipsa.matrix.charm.GuideType
import se.alipsa.matrix.charm.render.scale.CharmScale
import se.alipsa.matrix.charm.theme.ElementLine
import static se.alipsa.matrix.ext.NumberExtension.PI

/**
 * Renders major grid lines.
 */
@CompileStatic
class GridRenderer {

  /**
   * Renders major x/y grid lines.
   *
   * @param group panel group
   * @param context render context
   * @param panelWidth panel width
   * @param panelHeight panel height
   */
  void render(G group, RenderContext context, int panelWidth, int panelHeight) {
    Set<String> nulls = context.chart.theme.explicitNulls

    // Skip grid entirely if major grid is explicitly blanked
    if (nulls.contains('panelGridMajor')) {
      return
    }

    ElementLine gridMajor = context.chart.theme.panelGridMajor
    String color = gridMajor?.color ?: '#eeeeee'
    BigDecimal width = (gridMajor?.size ?: 1) as BigDecimal

    CharmCoordType coordType = context.chart.coord?.type ?: CharmCoordType.CARTESIAN
    if (coordType == CharmCoordType.POLAR || coordType == CharmCoordType.RADIAL) {
      renderPolarGrid(group, context, panelWidth, panelHeight, color, width)
      return
    }

    G grid = group.addG().id('grid')
    context.xScale.ticks(context.config.axisTickCount).each { Object tick ->
      BigDecimal x = context.xScale.transform(tick)
      if (x != null) {
        grid.addLine(x, 0, x, panelHeight).stroke(color).strokeWidth(width).styleClass('charm-grid')
      }
    }
    context.yScale.ticks(context.config.axisTickCount).each { Object tick ->
      BigDecimal y = context.yScale.transform(tick)
      if (y != null) {
        grid.addLine(0, y, panelWidth, y).stroke(color).strokeWidth(width).styleClass('charm-grid')
      }
    }
  }

  private static void renderPolarGrid(
      G group,
      RenderContext context,
      int panelWidth,
      int panelHeight,
      String color,
      BigDecimal width
  ) {
    G grid = group.addG().id('grid')
    String thetaAes = resolveThetaAesthetic(context.chart.coord?.params)
    String radiusAes = thetaAes == 'x' ? 'y' : 'x'
    CharmScale thetaScale = thetaAes == 'x' ? context.xScale : context.yScale
    CharmScale radiusScale = radiusAes == 'x' ? context.xScale : context.yScale
    if (thetaScale == null || radiusScale == null) {
      return
    }

    BigDecimal start = (context.chart.coord?.params?.start as BigDecimal) ?: 0
    Integer direction = context.chart.coord?.params?.direction as Integer
    boolean clockwise = context.chart.coord?.params?.containsKey('clockwise')
        ? context.chart.coord?.params?.clockwise as boolean
        : (direction == null || direction >= 0)
    BigDecimal cx = panelWidth / 2 as BigDecimal
    BigDecimal cy = panelHeight / 2 as BigDecimal
    BigDecimal maxRadius = panelWidth.min(panelHeight) / 2 * 0.9

    List<Object> thetaBreaks = thetaScale.ticks(context.config.axisTickCount)
    List<String> thetaLabels = thetaScale.tickLabels(context.config.axisTickCount)
    boolean thetaGuideActive = context.chart.guides?.getSpec(thetaAes)?.type == GuideType.AXIS_THETA

    thetaBreaks.eachWithIndex { Object breakVal, int idx ->
      BigDecimal norm = normalizeFromScale(thetaScale, breakVal)
      if (norm == null) {
        return
      }
      BigDecimal angle = clockwise ? (start + norm * 2 * PI) : (start - norm * 2 * PI)
      BigDecimal x = cx + maxRadius * angle.sin()
      BigDecimal y = cy - maxRadius * angle.cos()
      grid.addLine(cx, cy, x, y)
          .stroke(color)
          .strokeWidth(width)
          .styleClass('charm-grid')

      if (!thetaGuideActive) {
        String label = idx < thetaLabels.size() ? thetaLabels[idx] : breakVal?.toString()
        if (label != null && !label.isEmpty()) {
          BigDecimal labelRadius = maxRadius + (context.chart.theme.axisTickLength ?: 5)
          group.addText(label)
              .x(cx + labelRadius * angle.sin())
              .y(cy - labelRadius * angle.cos())
              .textAnchor('middle')
              .dominantBaseline('middle')
              .fontSize(context.chart.theme.axisTextX?.size ?: 9)
              .fill(context.chart.theme.axisTextX?.color ?: '#4D4D4D')
        }
      }
    }

    radiusScale.ticks(context.config.axisTickCount).each { Object breakVal ->
      BigDecimal norm = normalizeFromScale(radiusScale, breakVal)
      if (norm == null) {
        return
      }
      BigDecimal r = norm * maxRadius
      if (r > 0) {
        grid.addCircle()
            .cx(cx)
            .cy(cy)
            .r(r)
            .fill('none')
            .stroke(color)
            .strokeWidth(width)
            .styleClass('charm-grid')
      }
    }

    grid.addCircle()
        .cx(cx)
        .cy(cy)
        .r(maxRadius)
        .fill('none')
        .stroke(color)
        .strokeWidth(width)
        .styleClass('charm-grid')
  }

  private static String resolveThetaAesthetic(Map<String, Object> coordParams) {
    String theta = coordParams?.theta?.toString()?.toLowerCase()
    theta == 'y' ? 'y' : 'x'
  }

  private static BigDecimal normalizeFromScale(CharmScale scale, Object value) {
    if (scale == null) {
      return null
    }
    BigDecimal transformed = scale.transform(value)
    if (transformed == null) {
      return null
    }
    BigDecimal rangeStart = scale.rangeStart
    BigDecimal rangeEnd = scale.rangeEnd
    if (rangeStart == null || rangeEnd == null) {
      return null
    }
    BigDecimal span = (rangeEnd - rangeStart).abs()
    if (span == 0) {
      return 0
    }
    BigDecimal low = [rangeStart, rangeEnd].min()
    BigDecimal norm = ((transformed - low) / span).min(1).max(0)
    if (rangeEnd < rangeStart) {
      norm = 1 - norm
    }
    norm
  }
}
