package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.theme.ElementLine
import se.alipsa.matrix.charm.theme.ElementText

import java.math.RoundingMode

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
    Set<String> nulls = context.chart.theme.explicitNulls

    ElementLine xLine = context.chart.theme.axisLineX
    ElementLine yLine = context.chart.theme.axisLineY
    ElementText xText = context.chart.theme.axisTextX
    ElementText yText = context.chart.theme.axisTextY

    // Default colors/sizes for backward compatibility
    String defaultLineColor = '#333333'
    BigDecimal defaultLineWidth = 1
    String defaultTextColor = '#333333'
    BigDecimal defaultTextSize = (context.chart.theme.baseSize ?: 10) as BigDecimal

    G axes = group.addG().id('axes')

    // X-axis line
    if (!nulls.contains('axisLineX')) {
      String xLineColor = xLine?.color ?: defaultLineColor
      BigDecimal xLineWidth = (xLine?.size ?: defaultLineWidth) as BigDecimal
      axes.addLine(0, panelHeight, panelWidth, panelHeight)
          .stroke(xLineColor).strokeWidth(xLineWidth).styleClass('charm-axis-line')
    }

    // Y-axis line
    if (!nulls.contains('axisLineY')) {
      String yLineColor = yLine?.color ?: defaultLineColor
      BigDecimal yLineWidth = (yLine?.size ?: defaultLineWidth) as BigDecimal
      axes.addLine(0, 0, 0, panelHeight)
          .stroke(yLineColor).strokeWidth(yLineWidth).styleClass('charm-axis-line')
    }

    int tickLen = context.config.axisTickLength
    int tickCount = context.config.axisTickCount

    // X-axis ticks and labels
    if (!nulls.contains('axisTextX')) {
      ElementLine xTicks = context.chart.theme.axisTicksX
      String xTickColor = xTicks?.color ?: xLine?.color ?: defaultLineColor
      BigDecimal xTickWidth = (xTicks?.size ?: xLine?.size ?: defaultLineWidth) as BigDecimal
      String xTextColor = xText?.color ?: defaultTextColor
      BigDecimal xTextSize = (xText?.size ?: defaultTextSize) as BigDecimal

      G xAxisGroup = axes.addG().id('x-axis')
      List<Object> xTickValues = context.xScale.ticks(tickCount)
      List<String> xLabels = context.xScale.tickLabels(tickCount)
      xTickValues.eachWithIndex { Object tick, int idx ->
        BigDecimal x = context.xScale.transform(tick)
        if (x != null) {
          if (!nulls.contains('axisTicksX')) {
            xAxisGroup.addLine(x, panelHeight, x, panelHeight + tickLen)
                .stroke(xTickColor).strokeWidth(xTickWidth).styleClass('charm-axis-tick')
          }
          String label = idx < xLabels.size() ? xLabels[idx] : formatTick(tick)
          xAxisGroup.addText(label)
              .x(x)
              .y(panelHeight + tickLen + 14)
              .textAnchor('middle')
              .fill(xTextColor)
              .fontSize(xTextSize)
              .styleClass('charm-axis-label')
        }
      }
    }

    // Y-axis ticks and labels
    if (!nulls.contains('axisTextY')) {
      ElementLine yTicks = context.chart.theme.axisTicksY
      String yTickColor = yTicks?.color ?: yLine?.color ?: defaultLineColor
      BigDecimal yTickWidth = (yTicks?.size ?: yLine?.size ?: defaultLineWidth) as BigDecimal
      String yTextColor = yText?.color ?: defaultTextColor
      BigDecimal yTextSize = (yText?.size ?: defaultTextSize) as BigDecimal

      G yAxisGroup = axes.addG().id('y-axis')
      List<Object> yTickValues = context.yScale.ticks(tickCount)
      List<String> yLabels = context.yScale.tickLabels(tickCount)
      yTickValues.eachWithIndex { Object tick, int idx ->
        BigDecimal y = context.yScale.transform(tick)
        if (y != null) {
          if (!nulls.contains('axisTicksY')) {
            yAxisGroup.addLine(0, y, -tickLen, y)
                .stroke(yTickColor).strokeWidth(yTickWidth).styleClass('charm-axis-tick')
          }
          String label = idx < yLabels.size() ? yLabels[idx] : formatTick(tick)
          yAxisGroup.addText(label)
              .x(-tickLen - 4)
              .y(y + 4)
              .textAnchor('end')
              .fill(yTextColor)
              .fontSize(yTextSize)
              .styleClass('charm-axis-label')
        }
      }
    }
  }

  private static String formatTick(Object tick) {
    if (tick instanceof BigDecimal) {
      BigDecimal rounded = (tick as BigDecimal).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
      return rounded.toPlainString()
    }
    tick?.toString() ?: ''
  }
}
