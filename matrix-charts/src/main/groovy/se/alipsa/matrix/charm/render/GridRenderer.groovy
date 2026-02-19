package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.theme.ElementLine

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
}
