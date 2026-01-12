package se.alipsa.matrix.gg.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.Rect
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.theme.Theme

/**
 * Utility class for rendering facet strips and axes.
 * Handles horizontal and vertical facet strips with proper styling.
 */
@CompileStatic
class FacetRenderer {

  /**
   * Render a horizontal facet strip label.
   */
  void renderFacetStrip(G group, String label, int width, int height, Theme theme) {
    // Strip background
    group.addRect(width, height)
         .x(0)
         .y(0)
         .fill(theme.stripBackground?.fill ?: '#E0E0E0')
         .stroke(theme.stripBackground?.color ?: 'none')

    // Strip text
    group.addText(label)
         .x(width / 2)
         .y(height / 2 + 4)
         .fontSize(theme.stripText?.size ?: 10)
         .fill(theme.stripText?.color ?: 'black')
         .textAnchor('middle')
  }

  /**
   * Render a vertical (rotated) facet strip label.
   */
  void renderFacetStripRotated(G group, String label, int width, int height, Theme theme) {
    // Strip background
    group.addRect(width, height)
         .x(0)
         .y(0)
         .fill(theme.stripBackground?.fill ?: '#E0E0E0')
         .stroke(theme.stripBackground?.color ?: 'none')

    // Strip text (rotated 90 degrees clockwise to match ggplot2)
    BigDecimal centerX = (width / 2).round()
    BigDecimal centerY = (height / 2).round()
    group.addText(label)
         .x(centerX)
         .y(centerY + 4)
         .fontSize(theme.stripText?.size ?: 10)
         .fill(theme.stripText?.color ?: 'black')
         .textAnchor('middle')
         .transform("rotate(90, $centerX, $centerY)")
  }

  /**
   * Render axes for a facet panel.
   */
  void renderFacetAxes(G plotArea, Map<String, Scale> scales, int width, int height,
                       Theme theme, boolean showXLabels, boolean showYLabels) {
    G axesGroup = plotArea.addG()
    axesGroup.id('axes')

    // X-axis
    if (scales['x'] != null) {
      renderFacetXAxis(axesGroup, scales['x'], width, height, theme, showXLabels)
    }

    // Y-axis
    if (scales['y'] != null) {
      renderFacetYAxis(axesGroup, scales['y'], width, height, theme, showYLabels)
    }
  }

  /**
   * Render X-axis for a facet panel.
   */
  private void renderFacetXAxis(G axesGroup, Scale scale, int width, int height, Theme theme, boolean showLabels) {
    G xAxisGroup = axesGroup.addG()
    xAxisGroup.id('x-axis')
    xAxisGroup.transform("translate(0, $height)")

    // Axis line
    xAxisGroup.addLine(0, 0, width, 0)
              .stroke(theme.axisLineX?.color ?: 'black')
              .strokeWidth(theme.axisLineX?.size ?: 1)

    if (showLabels) {
      // Ticks and labels
      List breaks = scale.getComputedBreaks()
      List<String> labels = scale.getComputedLabels()

      breaks.eachWithIndex { breakVal, i ->
        Number xPos = scale.transform(breakVal) as Number
        if (xPos == null) return

        // Tick mark
        xAxisGroup.addLine(xPos, 0, xPos, (theme.axisTickLength ?: 5))
                  .stroke('black')

        // Label
        String label = i < labels.size() ? labels[i] : breakVal.toString()
        xAxisGroup.addText(label)
                  .x(xPos)
                  .y((theme.axisTickLength ?: 5) + 15)
                  .fontSize(theme.axisTextX?.size ?: 10)
                  .textAnchor('middle')
      }
    }
  }

  /**
   * Render Y-axis for a facet panel.
   */
  private void renderFacetYAxis(G axesGroup, Scale scale, int width, int height, Theme theme, boolean showLabels) {
    G yAxisGroup = axesGroup.addG()
    yAxisGroup.id('y-axis')

    // Axis line
    yAxisGroup.addLine(0, 0, 0, height)
              .stroke(theme.axisLineY?.color ?: 'black')
              .strokeWidth(theme.axisLineY?.size ?: 1)

    if (showLabels) {
      // Ticks and labels
      List breaks = scale.getComputedBreaks()
      List<String> labels = scale.getComputedLabels()

      breaks.eachWithIndex { breakVal, i ->
        Number yPos = scale.transform(breakVal) as Number
        if (yPos == null) return

        // Tick mark
        yAxisGroup.addLine(0, yPos, -1 * (theme.axisTickLength ?: 5), yPos)
                  .stroke('black')

        // Label
        String label = i < labels.size() ? labels[i] : breakVal.toString()
        yAxisGroup.addText(label)
                  .x(-1 * (theme.axisTickLength ?: 5) - 5)
                  .y(yPos + 4)
                  .fontSize(theme.axisTextY?.size ?: 10)
                  .textAnchor('end')
      }
    }
  }
}
