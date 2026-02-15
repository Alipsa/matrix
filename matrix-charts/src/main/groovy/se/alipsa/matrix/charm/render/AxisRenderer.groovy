package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G

/**
 * Renders cartesian axes and labels.
 */
@CompileStatic
class AxisRenderer {

  /**
   * Renders x and y axes.
   *
   * @param group panel group
   * @param context render context
   * @param panelWidth panel width
   * @param panelHeight panel height
   */
  void render(G group, RenderContext context, int panelWidth, int panelHeight) {
    String lineColor = (context.chart.theme.axis?.color ?: '#333333') as String
    BigDecimal lineWidth = (context.chart.theme.axis?.lineWidth ?: 1) as BigDecimal
    String textColor = (context.chart.theme.text?.color ?: '#333333') as String
    BigDecimal textSize = (context.chart.theme.text?.size ?: 10) as BigDecimal

    G axes = group.addG().id('axes')
    axes.addLine(0, panelHeight, panelWidth, panelHeight)
        .stroke(lineColor).strokeWidth(lineWidth).styleClass('charm-axis-line')
    axes.addLine(0, 0, 0, panelHeight)
        .stroke(lineColor).strokeWidth(lineWidth).styleClass('charm-axis-line')

    int tickLen = context.config.axisTickLength
    context.xScale.ticks(context.config.axisTickCount).each { Object tick ->
      BigDecimal x = context.xScale.transform(tick)
      if (x != null) {
        axes.addLine(x, panelHeight, x, panelHeight + tickLen)
            .stroke(lineColor).strokeWidth(lineWidth).styleClass('charm-axis-tick')
        axes.addText(formatTick(tick))
            .x(x)
            .y(panelHeight + tickLen + 14)
            .textAnchor('middle')
            .fill(textColor)
            .fontSize(textSize)
            .styleClass('charm-axis-label')
      }
    }

    context.yScale.ticks(context.config.axisTickCount).each { Object tick ->
      BigDecimal y = context.yScale.transform(tick)
      if (y != null) {
        axes.addLine(0, y, -tickLen, y)
            .stroke(lineColor).strokeWidth(lineWidth).styleClass('charm-axis-tick')
        axes.addText(formatTick(tick))
            .x(-tickLen - 4)
            .y(y + 4)
            .textAnchor('end')
            .fill(textColor)
            .fontSize(textSize)
            .styleClass('charm-axis-label')
      }
    }
  }

  private static String formatTick(Object tick) {
    if (tick instanceof BigDecimal) {
      BigDecimal rounded = (tick as BigDecimal).setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros()
      return rounded.toPlainString()
    }
    tick?.toString() ?: ''
  }
}
