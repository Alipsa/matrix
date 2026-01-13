package se.alipsa.matrix.gg.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.gg.coord.CoordFlip
import se.alipsa.matrix.gg.coord.CoordPolar
import se.alipsa.matrix.gg.coord.CoordRadial
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleContinuous
import se.alipsa.matrix.gg.scale.ScaleDiscrete
import se.alipsa.matrix.gg.theme.ElementLine
import se.alipsa.matrix.gg.theme.Theme
import java.util.Locale

/**
 * Utility class for rendering grid lines in ggplot charts.
 * Handles both regular Cartesian grids and polar coordinate grids.
 */
@CompileStatic
class GridRenderer {

  private final RenderContext context

  GridRenderer(RenderContext context) {
    this.context = context
  }

  /**
   * Render grid lines based on coordinate system.
   */
  void renderGridLines(G plotArea, Map<String, Scale> scales, Number width, Number height, Theme theme, def coord) {
    if (coord instanceof CoordRadial) {
      renderRadialGridLines(plotArea, scales, theme, coord as CoordRadial)
      return
    }
    if (coord instanceof CoordPolar) {
      renderPolarGridLines(plotArea, scales, theme, coord as CoordPolar)
      return
    }

    G gridGroup = plotArea.addG()
    gridGroup.id('grid')

    // Render major grid lines
    ElementLine majorStyle = theme.panelGridMajor
    if (majorStyle != null) {
      renderMajorGridLines(gridGroup, scales, width, height, majorStyle, coord)
    }

    // Render minor grid lines
    ElementLine minorStyle = theme.panelGridMinor
    if (minorStyle != null) {
      renderMinorGridLines(gridGroup, scales, width, height, minorStyle, coord)
    }
  }

  /**
   * Render major grid lines.
   */
  private void renderMajorGridLines(G gridGroup, Map<String, Scale> scales, Number width, Number height,
                                     ElementLine style, def coord) {
    String color = style.color ?: '#E0E0E0'
    Number size = style.size ?: 0.5

    boolean isFlipped = coord instanceof CoordFlip

    // Vertical grid lines (for x-axis breaks)
    Scale xScale = isFlipped ? scales['y'] : scales['x']
    if (xScale != null) {
      List breaks = xScale.getComputedBreaks()
      breaks.each { breakVal ->
        Number xPos = xScale.transform(breakVal) as Number
        if (xPos != null) {
          gridGroup.addLine(xPos, 0, xPos, height)
                   .stroke(color)
                   .strokeWidth(size)
        }
      }
    }

    // Horizontal grid lines (for y-axis breaks)
    Scale yScale = isFlipped ? scales['x'] : scales['y']
    if (yScale != null) {
      List breaks = yScale.getComputedBreaks()
      breaks.each { breakVal ->
        Number yPos = yScale.transform(breakVal) as Number
        if (yPos != null) {
          gridGroup.addLine(0, yPos, width, yPos)
                   .stroke(color)
                   .strokeWidth(size)
        }
      }
    }
  }

  /**
   * Render minor grid lines.
   */
  private void renderMinorGridLines(G gridGroup, Map<String, Scale> scales, Number width, Number height,
                                     ElementLine style, def coord) {
    String color = style.color ?: '#F0F0F0'
    Number size = style.size ?: 0.25

    boolean isFlipped = coord instanceof CoordFlip

    // Generate minor breaks for continuous numeric scales only (not date/time scales)
    Scale xScale = isFlipped ? scales['y'] : scales['x']
    if (xScale instanceof ScaleContinuous && shouldGenerateMinorBreaks(xScale)) {
      List<BigDecimal> minorBreaks = generateMinorBreaks(xScale as ScaleContinuous)
      minorBreaks.each { breakVal ->
        Number xPos = xScale.transform(breakVal) as Number
        if (xPos != null) {
          gridGroup.addLine(xPos, 0, xPos, height)
                   .stroke(color)
                   .strokeWidth(size)
        }
      }
    }

    Scale yScale = isFlipped ? scales['x'] : scales['y']
    if (yScale instanceof ScaleContinuous && shouldGenerateMinorBreaks(yScale)) {
      List<BigDecimal> minorBreaks = generateMinorBreaks(yScale as ScaleContinuous)
      minorBreaks.each { breakVal ->
        Number yPos = yScale.transform(breakVal) as Number
        if (yPos != null) {
          gridGroup.addLine(0, yPos, width, yPos)
                   .stroke(color)
                   .strokeWidth(size)
        }
      }
    }
  }

  /**
   * Check if a scale should have minor breaks generated.
   * Date/time scales should not have minor breaks.
   */
  private boolean shouldGenerateMinorBreaks(Scale scale) {
    String className = scale.getClass().simpleName
    return !className.contains('Date') && !className.contains('Time')
  }

  /**
   * Generate minor breaks between major breaks for a continuous scale.
   */
  private List<BigDecimal> generateMinorBreaks(ScaleContinuous scale) {
    List<BigDecimal> minorBreaks = []
    List breaks = scale.getComputedBreaks()

    if (breaks == null || breaks.size() < 2) return minorBreaks

    // Generate 4 minor breaks between each pair of major breaks
    for (int i = 0; i < breaks.size() - 1; i++) {
      BigDecimal lower = breaks[i] as BigDecimal
      BigDecimal upper = breaks[i + 1] as BigDecimal
      BigDecimal step = (upper - lower) / 5

      for (int j = 1; j <= 4; j++) {
        minorBreaks << (lower + step * j)
      }
    }

    return minorBreaks
  }

  /**
   * Render polar grid lines (spokes and rings) for theta/radius scales.
   */
  private void renderPolarGridLines(G plotArea, Map<String, Scale> scales, Theme theme, CoordPolar coord) {
    G gridGroup = plotArea.addG()
    gridGroup.id('grid')

    ElementLine majorStyle = theme.panelGridMajor
    if (majorStyle == null) return

    String color = majorStyle.color ?: 'white'
    Number size = majorStyle.size ?: 1

    Scale thetaScale = coord.theta == 'y' ? scales['y'] : scales['x']
    Scale radiusScale = coord.theta == 'y' ? scales['x'] : scales['y']

    List<BigDecimal> center = coord.getCenter()
    BigDecimal cx = center[0]
    BigDecimal cy = center[1]
    BigDecimal maxRadius = coord.getMaxRadius()

    // Render theta grid lines (spokes)
    if (thetaScale) {
      List breaks = thetaScale.getComputedBreaks()
      List<String> labels = thetaScale.getComputedLabels()
      breaks.eachWithIndex { breakVal, int i ->
        BigDecimal norm = context.normalizeFromScale(thetaScale, breakVal)
        if (norm == null) return
        if (context.isRangeReversed(thetaScale)) {
          norm = 1.0 - norm
        }
        BigDecimal angle = coord.start + norm * 2.0d * Math.PI
        if (!coord.clockwise) {
          angle = coord.start - norm * 2.0d * Math.PI
        }
        BigDecimal x = cx + maxRadius * angle.sin()
        BigDecimal y = cy - maxRadius * angle.cos()
        gridGroup.addLine(cx, cy, x, y)
                 .stroke(color)
                 .strokeWidth(size)

        // Labels are rendered by AxisRenderer if theta axis guide is present
        // Otherwise, render them here
        String label = i < labels.size() ? labels[i] : breakVal?.toString()
        if (label != null && !label.isEmpty()) {
          BigDecimal labelRadius = maxRadius + (theme.axisTickLength ?: 5)
          BigDecimal lx = cx + labelRadius * angle.sin()
          BigDecimal ly = cy - labelRadius * angle.cos()
          plotArea.addText(label)
                  .x(lx)
                  .y(ly)
                  .textAnchor('middle')
                  .fontSize(theme.axisTextY?.size ?: 9)
                  .fill(theme.axisTextY?.color ?: '#4D4D4D')
        }
      }
    }

    // Render radius grid lines (concentric circles)
    if (radiusScale) {
      radiusScale.getComputedBreaks().each { breakVal ->
        BigDecimal norm = context.normalizeFromScale(radiusScale, breakVal)
        if (norm == null) return
        BigDecimal r = norm * maxRadius
        if (r <= 0.0) return
        gridGroup.addCircle()
                 .cx(cx)
                 .cy(cy)
                 .r(r)
                 .fill('none')
                 .stroke(color)
                 .strokeWidth(size)
      }
    }

    // Always draw an outer ring to match ggplot2 polar grid appearance
    gridGroup.addCircle()
             .cx(cx)
             .cy(cy)
             .r(maxRadius)
             .fill('none')
             .stroke(color)
             .strokeWidth(size)
  }

  /**
   * Render radial grid lines (spokes and arcs) for coord_radial.
   */
  private void renderRadialGridLines(G plotArea, Map<String, Scale> scales, Theme theme, CoordRadial coord) {
    G gridGroup = plotArea.addG()
    gridGroup.id('grid')

    ElementLine majorStyle = theme.panelGridMajor
    if (majorStyle == null) return

    String color = majorStyle.color ?: 'white'
    Number size = majorStyle.size ?: 1

    Scale thetaScale = coord.theta == 'y' ? scales['y'] : scales['x']
    Scale radiusScale = coord.theta == 'y' ? scales['x'] : scales['y']

    List<BigDecimal> center = coord.getCenter()
    BigDecimal cx = center[0]
    BigDecimal cy = center[1]
    BigDecimal maxRadius = coord.getMaxRadius()
    BigDecimal innerRadius = coord.getInnerRadiusPx()
    BigDecimal span = coord.getAngularSpan()
    BigDecimal base = coord.getAngularRange()[0]

    double spanVal = span as double
    boolean fullCircle = Math.abs(spanVal - 2 * Math.PI) < 0.0001d

    String thetaGuideType = context.parseGuideType(thetaScale?.guide)
    boolean renderThetaLabels = (thetaGuideType == null || thetaGuideType == 'none')

    if (thetaScale) {
      List breaks = thetaScale.getComputedBreaks()
      List<String> labels = thetaScale.getComputedLabels()
      breaks.eachWithIndex { breakVal, int i ->
        BigDecimal norm = context.normalizeFromScale(thetaScale, breakVal)
        if (norm == null) return
        if (context.isRangeReversed(thetaScale)) {
          norm = 1.0 - norm
        }
        BigDecimal angle = coord.clockwise ? (base + norm * span) : (base - norm * span)
        BigDecimal x1 = cx + innerRadius * angle.sin()
        BigDecimal y1 = cy - innerRadius * angle.cos()
        BigDecimal x2 = cx + maxRadius * angle.sin()
        BigDecimal y2 = cy - maxRadius * angle.cos()

        gridGroup.addLine(x1, y1, x2, y2)
            .stroke(color)
            .strokeWidth(size)

        if (renderThetaLabels) {
          String label = i < labels.size() ? labels[i] : breakVal?.toString()
          if (label != null && !label.isEmpty()) {
            BigDecimal labelRadius = maxRadius + (theme.axisTickLength ?: 5)
            BigDecimal lx = cx + labelRadius * angle.sin()
            BigDecimal ly = cy - labelRadius * angle.cos()
            def textElement = plotArea.addText(label)
                .x(lx)
                .y(ly)
                .textAnchor('middle')
                .fontSize(theme.axisTextX?.size ?: 9)
                .fill(theme.axisTextX?.color ?: '#4D4D4D')

            if (coord.rotateAngle) {
              BigDecimal rotation = coord.getTextRotation(norm)
              if (rotation != 0) {
                textElement.transform("rotate($rotation, $lx, $ly)")
              }
            }
          }
        }
      }
    }

    if (radiusScale) {
      radiusScale.getComputedBreaks().each { breakVal ->
        BigDecimal norm = context.normalizeFromScale(radiusScale, breakVal)
        if (norm == null) return
        if (context.isRangeReversed(radiusScale)) {
          norm = 1.0 - norm
        }
        BigDecimal r = innerRadius + norm * (maxRadius - innerRadius)
        if (r <= innerRadius && innerRadius > 0) return
        if (fullCircle) {
          gridGroup.addCircle()
              .cx(cx)
              .cy(cy)
              .r(r)
              .fill('none')
              .stroke(color)
              .strokeWidth(size)
        } else {
          gridGroup.addPath()
              .d(createArcPath(coord, r))
              .fill('none')
              .stroke(color)
              .strokeWidth(size)
        }
      }
    }

    if (fullCircle) {
      gridGroup.addCircle()
          .cx(cx)
          .cy(cy)
          .r(maxRadius)
          .fill('none')
          .stroke(color)
          .strokeWidth(size)
      if (innerRadius > 0) {
        gridGroup.addCircle()
            .cx(cx)
            .cy(cy)
            .r(innerRadius)
            .fill('none')
            .stroke(color)
            .strokeWidth(size)
      }
    } else {
      gridGroup.addPath()
          .d(createArcPath(coord, maxRadius))
          .fill('none')
          .stroke(color)
          .strokeWidth(size)
      if (innerRadius > 0) {
        gridGroup.addPath()
            .d(createArcPath(coord, innerRadius))
            .fill('none')
            .stroke(color)
            .strokeWidth(size)
      }
    }
  }

  private String createArcPath(CoordRadial coord, BigDecimal radius) {
    List<BigDecimal> center = coord.getCenter()
    double cx = center[0] as double
    double cy = center[1] as double
    double startRad = coord.getAngularRange()[0] as double
    double endRad = coord.getAngularRange()[1] as double

    double x1 = cx + (radius as double) * Math.sin(startRad)
    double y1 = cy - (radius as double) * Math.cos(startRad)
    double x2 = cx + (radius as double) * Math.sin(endRad)
    double y2 = cy - (radius as double) * Math.cos(endRad)

    double spanVal = coord.getAngularSpan() as double
    int largeArc = spanVal > Math.PI ? 1 : 0
    int sweepFlag = coord.clockwise ? 1 : 0

    return "M ${formatNumber(x1)} ${formatNumber(y1)} A ${formatNumber(radius as double)} ${formatNumber(radius as double)} 0 ${largeArc} ${sweepFlag} ${formatNumber(x2)} ${formatNumber(y2)}"
  }

  private static String formatNumber(double value) {
    return String.format(Locale.US, "%.3f", value)
  }
}
