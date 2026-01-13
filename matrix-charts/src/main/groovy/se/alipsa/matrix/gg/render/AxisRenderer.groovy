package se.alipsa.matrix.gg.render

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.gg.Guide
import se.alipsa.matrix.gg.coord.CoordFlip
import se.alipsa.matrix.gg.coord.CoordPolar
import se.alipsa.matrix.gg.coord.CoordRadial
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleContinuous
import se.alipsa.matrix.gg.scale.ScaleXContinuous
import se.alipsa.matrix.gg.scale.ScaleYContinuous
import se.alipsa.matrix.gg.theme.Theme
import java.util.Locale

/**
 * Utility class for rendering axes in ggplot charts.
 * Handles regular axes, polar axes, stacked axes, and logarithmic tick axes.
 */
@CompileStatic
class AxisRenderer {

  private final RenderContext context

  AxisRenderer(RenderContext context) {
    this.context = context
  }

  /**
   * Render axes based on coordinate system.
   */
  void renderAxes(G plotArea, Map<String, Scale> scales, int width, int height, Theme theme, def coord) {
    if (coord instanceof CoordRadial) {
      renderRadialAxes(plotArea, scales, coord as CoordRadial, theme)
      return
    }
    if (coord instanceof CoordPolar) {
      renderPolarAxes(plotArea, scales, coord as CoordPolar, theme)
      return
    }
    G axesGroup = plotArea.addG()
    axesGroup.id('axes')

    boolean isFlipped = coord instanceof CoordFlip

    if (isFlipped) {
      // For flipped coords: x data appears on left axis, y data appears on bottom axis
      // Check for stacked axes
      String xGuideType = context.parseGuideType(scales['x']?.guide)
      String yGuideType = context.parseGuideType(scales['y']?.guide)

      if (xGuideType == 'axis_stack') {
        renderStackedYAxis(axesGroup, scales['x'], width, height, theme)  // x scale on left (vertical)
      } else {
        renderYAxis(axesGroup, scales['x'], width, height, theme)  // x scale on left (vertical)
      }

      if (yGuideType == 'axis_stack') {
        renderStackedXAxis(axesGroup, scales['y'], width, height, theme)  // y scale on bottom (horizontal)
      } else {
        renderXAxis(axesGroup, scales['y'], width, height, theme)  // y scale on bottom (horizontal)
      }

      renderSecondaryAxes(axesGroup, scales, width, height, theme, true)
    } else {
      // Normal: x on bottom, y on left
      // Check for stacked axes
      String xGuideType = context.parseGuideType(scales['x']?.guide)
      String yGuideType = context.parseGuideType(scales['y']?.guide)

      if (xGuideType == 'axis_stack') {
        renderStackedXAxis(axesGroup, scales['x'], width, height, theme)
      } else {
        renderXAxis(axesGroup, scales['x'], width, height, theme)
      }

      if (yGuideType == 'axis_stack') {
        renderStackedYAxis(axesGroup, scales['y'], width, height, theme)
      } else {
        renderYAxis(axesGroup, scales['y'], width, height, theme)
      }

      renderSecondaryAxes(axesGroup, scales, width, height, theme, false)
    }
  }

  /**
   * Render axes for radial coordinates.
   */
  @CompileStatic
  private void renderRadialAxes(G plotArea, Map<String, Scale> scales, CoordRadial coord, Theme theme) {
    Scale thetaScale = coord.theta == 'x' ? scales['x'] : scales['y']
    Scale radiusScale = coord.theta == 'x' ? scales['y'] : scales['x']

    String thetaGuideType = context.parseGuideType(thetaScale?.guide)
    if (thetaGuideType && thetaGuideType != 'none') {
      Map guideParams = context.extractGuideParams(thetaScale?.guide)
      renderRadialThetaAxis(plotArea, thetaScale, coord, theme, guideParams)
    }

    String radiusGuideType = context.parseGuideType(radiusScale?.guide)
    if (radiusScale && radiusGuideType != 'none') {
      renderRadialRadiusAxis(plotArea, radiusScale, coord, theme)
    }
  }

  /**
   * Render theta (angular) axis for radial coordinates.
   */
  @CompileStatic
  private void renderRadialThetaAxis(G plotArea, Scale scale, CoordRadial coord, Theme theme, Map guideParams) {
    BigDecimal labelAngle = (guideParams.angle ?: 0) as BigDecimal
    boolean showMinorTicks = (guideParams['minor.ticks'] ?: guideParams.minorTicks ?: false) as boolean
    String cap = (guideParams.cap ?: 'none') as String

    BigDecimal maxRadius = coord.getMaxRadius()
    List<BigDecimal> center = coord.getCenter()
    BigDecimal cx = center[0]
    BigDecimal cy = center[1]
    BigDecimal tickLength = (theme.axisTickLength ?: 5) as BigDecimal
    BigDecimal labelOffset = tickLength + 5
    BigDecimal labelRadius = maxRadius + labelOffset

    BigDecimal span = coord.getAngularSpan()
    BigDecimal base = coord.getAngularRange()[0]

    List breaks = scale.getComputedBreaks()
    List<String> labels = scale.getComputedLabels()

    G axisGroup = plotArea.addG()
    axisGroup.id('theta-axis')

    breaks.eachWithIndex { breakVal, int i ->
      BigDecimal norm = context.normalizeFromScale(scale, breakVal)
      if (norm == null) return
      if (context.isRangeReversed(scale)) {
        norm = 1.0 - norm
      }

      BigDecimal theta = coord.clockwise ? (base + norm * span) : (base - norm * span)

      BigDecimal tickOuter = maxRadius + tickLength
      BigDecimal x1 = cx + maxRadius * theta.sin()
      BigDecimal y1 = cy - maxRadius * theta.cos()
      BigDecimal x2 = cx + tickOuter * theta.sin()
      BigDecimal y2 = cy - tickOuter * theta.cos()

      axisGroup.addLine(x1, y1, x2, y2)
               .stroke(theme.axisLineX?.color ?: 'black')
               .strokeWidth(theme.axisLineX?.size ?: 0.5)

      if (i < labels.size()) {
        BigDecimal labelX = cx + labelRadius * theta.sin()
        BigDecimal labelY = cy - labelRadius * theta.cos()

        String textAnchor = calculatePolarTextAnchor(theta)

        def textElement = axisGroup.addText(labels[i])
            .x(labelX)
            .y(labelY)
            .fontSize(theme.axisTextX?.size ?: 10)
            .fill(theme.axisTextX?.color ?: 'black')
            .textAnchor(textAnchor)
            .dominantBaseline('middle')

        BigDecimal rotation = coord.rotateAngle ? coord.getTextRotation(norm) : 0
        rotation += labelAngle
        if (rotation != 0) {
          textElement.transform("rotate($rotation, $labelX, $labelY)")
        }
      }
    }

    if (showMinorTicks && breaks.size() >= 2) {
      for (int i = 0; i < breaks.size() - 1; i++) {
        BigDecimal startNorm = context.normalizeFromScale(scale, breaks[i])
        BigDecimal endNorm = context.normalizeFromScale(scale, breaks[i + 1])
        if (startNorm == null || endNorm == null) continue
        for (int j = 1; j <= 4; j++) {
          BigDecimal minorNorm = startNorm + (endNorm - startNorm) * j / 5
          if (context.isRangeReversed(scale)) {
            minorNorm = 1.0 - minorNorm
          }
          BigDecimal minorTheta = coord.clockwise ? (base + minorNorm * span) : (base - minorNorm * span)
          BigDecimal minorTickLength = tickLength * 0.5
          BigDecimal minorTickOuter = maxRadius + minorTickLength
          BigDecimal mx1 = cx + maxRadius * minorTheta.sin()
          BigDecimal my1 = cy - maxRadius * minorTheta.cos()
          BigDecimal mx2 = cx + minorTickOuter * minorTheta.sin()
          BigDecimal my2 = cy - minorTickOuter * minorTheta.cos()

          axisGroup.addLine(mx1, my1, mx2, my2)
              .stroke(theme.axisLineX?.color ?: 'black')
              .strokeWidth((theme.axisLineX?.size ?: 0.5) * 0.75)
        }
      }
    }

    if (cap != 'none') {
      addRadialArc(axisGroup, coord, maxRadius, theme.axisLineX?.color ?: 'black',
          theme.axisLineX?.size ?: 0.5)
    }
  }

  /**
   * Render r-axis labels for radial coordinates.
   */
  @CompileStatic
  private void renderRadialRadiusAxis(G plotArea, Scale scale, CoordRadial coord, Theme theme) {
    BigDecimal maxRadius = coord.getMaxRadius()
    BigDecimal innerRadius = coord.getInnerRadiusPx()
    List<BigDecimal> center = coord.getCenter()
    BigDecimal cx = center[0]
    BigDecimal cy = center[1]

    BigDecimal axisAngle = coord.getAngularRange()[0]
    boolean inside = coord.rAxisInside != null ? coord.rAxisInside : false

    BigDecimal tickLength = (theme.axisTickLength ?: 5) as BigDecimal
    BigDecimal labelOffset = tickLength + 4

    G axisGroup = plotArea.addG()
    axisGroup.id('radius-axis')

    BigDecimal axisStart = innerRadius
    BigDecimal axisEnd = maxRadius
    BigDecimal ax1 = cx + axisStart * axisAngle.sin()
    BigDecimal ay1 = cy - axisStart * axisAngle.cos()
    BigDecimal ax2 = cx + axisEnd * axisAngle.sin()
    BigDecimal ay2 = cy - axisEnd * axisAngle.cos()

    axisGroup.addLine(ax1, ay1, ax2, ay2)
        .stroke(theme.axisLineY?.color ?: 'black')
        .strokeWidth(theme.axisLineY?.size ?: 0.5)

    List breaks = scale.getComputedBreaks()
    List<String> labels = scale.getComputedLabels()

    breaks.eachWithIndex { breakVal, int i ->
      BigDecimal norm = context.normalizeFromScale(scale, breakVal)
      if (norm == null) return
      if (context.isRangeReversed(scale)) {
        norm = 1.0 - norm
      }

      BigDecimal radius = innerRadius + norm * (maxRadius - innerRadius)
      BigDecimal tickOuter = radius + (inside ? -tickLength : tickLength)
      BigDecimal tx1 = cx + radius * axisAngle.sin()
      BigDecimal ty1 = cy - radius * axisAngle.cos()
      BigDecimal tx2 = cx + tickOuter * axisAngle.sin()
      BigDecimal ty2 = cy - tickOuter * axisAngle.cos()

      axisGroup.addLine(tx1, ty1, tx2, ty2)
          .stroke(theme.axisLineY?.color ?: 'black')
          .strokeWidth(theme.axisLineY?.size ?: 0.5)

      if (i < labels.size()) {
        BigDecimal labelRadius = radius + (inside ? -labelOffset : labelOffset)
        if (inside && labelRadius < innerRadius) {
          labelRadius = innerRadius
        }
        BigDecimal labelX = cx + labelRadius * axisAngle.sin()
        BigDecimal labelY = cy - labelRadius * axisAngle.cos()
        String textAnchor = calculatePolarTextAnchor(axisAngle)

        axisGroup.addText(labels[i])
            .x(labelX)
            .y(labelY)
            .fontSize(theme.axisTextY?.size ?: 10)
            .fill(theme.axisTextY?.color ?: 'black')
            .textAnchor(textAnchor)
            .dominantBaseline('middle')
      }
    }
  }

  /**
   * Render axes for polar coordinates.
   */
  @CompileStatic
  private void renderPolarAxes(G plotArea, Map<String, Scale> scales, CoordPolar coord, Theme theme) {
    Scale thetaScale = coord.theta == 'x' ? scales['x'] : scales['y']

    String guideType = context.parseGuideType(thetaScale?.guide)

    // Render theta axis if guide is specified
    if (guideType && guideType != 'none') {
      Map guideParams = context.extractGuideParams(thetaScale?.guide)
      renderThetaAxis(plotArea, thetaScale, coord, theme, guideParams)
    }

    // Radial grid/axis rendering is handled by GridRenderer
  }

  /**
   * Render theta (angular) axis for polar coordinates.
   */
  @CompileStatic
  private void renderThetaAxis(G plotArea, Scale scale, CoordPolar coord, Theme theme, Map guideParams) {
    // Get parameters
    BigDecimal labelAngle = (guideParams.angle ?: 0) as BigDecimal
    boolean showMinorTicks = (guideParams['minor.ticks'] ?: guideParams.minorTicks ?: false) as boolean
    String cap = (guideParams.cap ?: 'none') as String

    // Calculate positioning
    BigDecimal maxRadius = coord.getMaxRadius()
    List<BigDecimal> center = coord.getCenter()
    BigDecimal cx = center[0]
    BigDecimal cy = center[1]
    BigDecimal tickLength = (theme.axisTickLength ?: 5) as BigDecimal
    BigDecimal labelOffset = tickLength + 5
    BigDecimal labelRadius = maxRadius + labelOffset

    // Get breaks and labels from scale
    List breaks = scale.getComputedBreaks()
    List<String> labels = scale.getComputedLabels()

    // Create axis group
    G axisGroup = plotArea.addG()
    axisGroup.id('theta-axis')

    // Render major ticks and labels
    breaks.eachWithIndex { breakVal, int i ->
      // Normalize break value to [0, 1]
      BigDecimal norm = context.normalizeFromScale(scale, breakVal)
      if (norm == null) return

      // Adjust for reversed scales
      if (context.isRangeReversed(scale)) {
        norm = 1.0 - norm
      }

      // Calculate angle, accounting for start offset and direction
      BigDecimal theta = coord.start + norm * 2 * Math.PI
      if (!coord.clockwise) {
        theta = coord.start - norm * 2 * Math.PI
      }

      // Draw major tick mark (radial line from circle edge outward)
      BigDecimal tickOuter = maxRadius + tickLength
      BigDecimal x1 = cx + maxRadius * theta.sin()
      BigDecimal y1 = cy - maxRadius * theta.cos()
      BigDecimal x2 = cx + tickOuter * theta.sin()
      BigDecimal y2 = cy - tickOuter * theta.cos()

      axisGroup.addLine(x1, y1, x2, y2)
               .stroke(theme.axisLineX?.color ?: 'black')
               .strokeWidth(theme.axisLineX?.size ?: 0.5)

      // Add label
      if (i < labels.size()) {
        BigDecimal labelX = cx + labelRadius * theta.sin()
        BigDecimal labelY = cy - labelRadius * theta.cos()

        // Calculate text anchor based on angular position
        String textAnchor = calculatePolarTextAnchor(theta)

        def textElement = axisGroup.addText(labels[i])
                 .x(labelX)
                 .y(labelY)
                 .fontSize(theme.axisTextX?.size ?: 10)
                 .fill(theme.axisTextX?.color ?: 'black')
                 .textAnchor(textAnchor)
                 .dominantBaseline('middle')

        // Apply label rotation if specified
        if (labelAngle != 0) {
          textElement.transform("rotate($labelAngle, $labelX, $labelY)")
        }
      }
    }

    // Render minor ticks if requested
    if (showMinorTicks && breaks.size() >= 2) {
      for (int i = 0; i < breaks.size() - 1; i++) {
        // Calculate 4 minor ticks between each pair of major ticks
        for (int j = 1; j <= 4; j++) {
          BigDecimal minorNorm = context.normalizeFromScale(scale, breaks[i]) +
                                  (context.normalizeFromScale(scale, breaks[i + 1]) -
                                   context.normalizeFromScale(scale, breaks[i])) * j / 5

          if (minorNorm == null) continue

          // Adjust for reversed scales
          if (context.isRangeReversed(scale)) {
            minorNorm = 1.0 - minorNorm
          }

          // Calculate angle for minor tick
          BigDecimal minorTheta = coord.start + minorNorm * 2 * Math.PI
          if (!coord.clockwise) {
            minorTheta = coord.start - minorNorm * 2 * Math.PI
          }

          // Draw shorter minor tick mark
          BigDecimal minorTickLength = tickLength * 0.5
          BigDecimal minorTickOuter = maxRadius + minorTickLength
          BigDecimal mx1 = cx + maxRadius * minorTheta.sin()
          BigDecimal my1 = cy - maxRadius * minorTheta.cos()
          BigDecimal mx2 = cx + minorTickOuter * minorTheta.sin()
          BigDecimal my2 = cy - minorTickOuter * minorTheta.cos()

          axisGroup.addLine(mx1, my1, mx2, my2)
                   .stroke(theme.axisLineX?.color ?: 'black')
                   .strokeWidth((theme.axisLineX?.size ?: 0.5) * 0.75)
        }
      }
    }

    // Optionally draw outer circle (axis line)
    if (cap != 'none') {
      axisGroup.addCircle()
               .cx(cx)
               .cy(cy)
               .r(maxRadius)
               .fill('none')
               .stroke(theme.axisLineX?.color ?: 'black')
               .strokeWidth(theme.axisLineX?.size ?: 0.5)
    }
  }

  /**
   * Calculate text anchor for polar axis labels based on angular position.
   */
  @CompileStatic
  private String calculatePolarTextAnchor(BigDecimal theta) {
    // Normalize angle to [0, 2π)
    BigDecimal normalizedTheta = theta % (2 * Math.PI)
    if (normalizedTheta < 0) {
      normalizedTheta += 2 * Math.PI
    }

    // Divide into sectors for text anchoring
    // Top (around 12 o'clock): middle
    // Right (3 o'clock): start
    // Bottom (6 o'clock): middle
    // Left (9 o'clock): end
    BigDecimal piOver4 = Math.PI / 4

    if (normalizedTheta < piOver4 || normalizedTheta >= 7 * piOver4) {
      return 'middle'  // Top
    } else if (normalizedTheta < 3 * piOver4) {
      return 'start'   // Right
    } else if (normalizedTheta < 5 * piOver4) {
      return 'middle'  // Bottom
    } else {
      return 'end'     // Left
    }
  }

  @CompileStatic
  private void addRadialArc(G axisGroup, CoordRadial coord, BigDecimal radius, String color, Number size) {
    BigDecimal span = coord.getAngularSpan()
    double spanVal = span as double
    double fullCircle = 2 * Math.PI
    if (Math.abs(spanVal - fullCircle) < 0.0001d) {
      List<BigDecimal> center = coord.getCenter()
      axisGroup.addCircle()
          .cx(center[0])
          .cy(center[1])
          .r(radius)
          .fill('none')
          .stroke(color)
          .strokeWidth(size)
      return
    }

    List<BigDecimal> center = coord.getCenter()
    double cx = center[0] as double
    double cy = center[1] as double
    double startRad = (coord.getAngularRange()[0]) as double
    double endRad = (coord.getAngularRange()[1]) as double

    double x1 = cx + (radius as double) * Math.sin(startRad)
    double y1 = cy - (radius as double) * Math.cos(startRad)
    double x2 = cx + (radius as double) * Math.sin(endRad)
    double y2 = cy - (radius as double) * Math.cos(endRad)

    int largeArc = spanVal > Math.PI ? 1 : 0
    int sweepFlag = coord.clockwise ? 1 : 0
    String path = "M ${formatNumber(x1)} ${formatNumber(y1)} A ${formatNumber(radius as double)} ${formatNumber(radius as double)} 0 ${largeArc} ${sweepFlag} ${formatNumber(x2)} ${formatNumber(y2)}"

    axisGroup.addPath()
        .d(path)
        .fill('none')
        .stroke(color)
        .strokeWidth(size)
  }

  private static String formatNumber(double value) {
    return String.format(Locale.US, "%.3f", value)
  }

  /**
   * Render secondary axes if present.
   */
  private void renderSecondaryAxes(G axesGroup, Map<String, Scale> scales, int width, int height, Theme theme, boolean isFlipped) {
    // Render secondary x-axis if present
    if (scales['x'] instanceof ScaleXContinuous) {
      ScaleXContinuous xScale = scales['x'] as ScaleXContinuous
      xScale.updateSecondaryScale()
      if (xScale.secAxis != null) {
        if (isFlipped) {
          renderYAxis(axesGroup, xScale.secAxis, width, height, theme)  // secondary x on right (vertical)
        } else {
          renderXAxis(axesGroup, xScale.secAxis, width, height, theme)  // secondary x on top
        }
      }
    }

    // Render secondary y-axis if present
    if (scales['y'] instanceof ScaleYContinuous) {
      ScaleYContinuous yScale = scales['y'] as ScaleYContinuous
      yScale.updateSecondaryScale()
      if (yScale.secAxis != null) {
        if (isFlipped) {
          renderXAxis(axesGroup, yScale.secAxis, width, height, theme)  // secondary y on top (horizontal)
        } else {
          renderYAxis(axesGroup, yScale.secAxis, width, height, theme)  // secondary y on right
        }
      }
    }
  }

  /**
   * Render the X axis.
   */
  private void renderXAxis(G axesGroup, Scale scale, int width, int height, Theme theme, Number offsetY = 0, Guide overrideGuide = null) {
    if (scale == null) return

    // Use override guide if provided, otherwise use scale's guide
    def effectiveGuide = overrideGuide ?: scale?.guide

    // Check for log ticks guide
    String guideType = context.parseGuideType(effectiveGuide)
    if (guideType == 'axis_logticks') {
      Map guideParams = context.extractGuideParams(effectiveGuide)
      renderXAxisLogTicks(axesGroup, scale, width, height, theme, guideParams)
      return
    }

    // Determine position: bottom (default) or top (for secondary axis)
    String position = 'bottom'
    if (scale instanceof ScaleXContinuous) {
      position = ((ScaleXContinuous) scale).position ?: 'bottom'
    } else if (scale instanceof ScaleYContinuous) {
      // For flipped coords, y scale appears as horizontal axis
      position = ((ScaleYContinuous) scale).position == 'left' ? 'bottom' : 'top'
    }

    boolean isTop = (position == 'top')
    def yTranslate = isTop ? -(offsetY as BigDecimal) : height + offsetY

    G xAxisGroup = axesGroup.addG()
    xAxisGroup.id(isTop ? 'x-axis-top' : 'x-axis')
    xAxisGroup.transform("translate(0, $yTranslate)")

    // Axis line
    xAxisGroup.addLine(0, 0, width, 0)
              .stroke(theme.axisLineX?.color ?: 'black')
              .strokeWidth(theme.axisLineX?.size ?: 1)

    // Extract guide parameters
    Map guideParams = context.extractGuideParams(effectiveGuide)
    Number angle = guideParams.angle as Number ?: 0
    boolean checkOverlap = (guideParams['check.overlap'] ?: guideParams.checkOverlap) as Boolean ?: false
    // Calculate minimum spacing based on font size if not specified
    // Default: 4x font size to account for typical character width
    Number fontSize = (theme.axisTextX?.size ?: 10) as Number
    Number minSpacing = guideParams['min.spacing'] as Number ?: guideParams.minSpacing as Number ?: (fontSize.intValue() * 4)

    // Ticks and labels - works for both continuous and discrete scales
    List breaks = scale.getComputedBreaks()
    List<String> labels = scale.getComputedLabels()

    List<Double> renderedPositions = []
    breaks.eachWithIndex { breakVal, i ->
      Double xPos = scale.transform(breakVal) as Double
      if (xPos == null) return  // Skip if transform returns null

      // Check overlap if requested
      if (checkOverlap && context.shouldSkipForOverlap(xPos as Number, renderedPositions as List<Number>, minSpacing)) {
        return  // Skip this label
      }
      renderedPositions << xPos

      // Tick mark (direction depends on position)
      int tickDir = isTop ? -1 : 1
      xAxisGroup.addLine(xPos, 0, xPos, tickDir * (theme.axisTickLength ?: 5))
                .stroke('black')

      // Label (position depends on axis position)
      String label = i < labels.size() ? labels[i] : breakVal.toString()
      int labelY = isTop ? (-1 * (theme.axisTickLength ?: 5) - 3) : ((theme.axisTickLength ?: 5) + 15)
      def text = xAxisGroup.addText(label)
                .x(xPos)
                .y(labelY)
                .fontSize(theme.axisTextX?.size ?: 10)

      // Apply rotation if specified
      if (angle != 0) {
        text.transform("rotate($angle, $xPos, $labelY)")
        // For bottom axis: positive angles anchor at 'end', negative at 'start'.
        // For top axis: reverse the logic so labels align visually as expected.
        if (isTop) {
          text.textAnchor(angle > 0 ? 'start' : 'end')
        } else {
          text.textAnchor(angle > 0 ? 'end' : 'start')
        }
      } else {
        text.textAnchor('middle')
      }
    }
  }

  /**
   * Render the Y axis.
   */
  private void renderYAxis(G axesGroup, Scale scale, int width, int height, Theme theme, Number offsetX = 0, Guide overrideGuide = null) {
    if (scale == null) return

    // Use override guide if provided, otherwise use scale's guide
    def effectiveGuide = overrideGuide ?: scale?.guide

    // Check for log ticks guide
    String guideType = context.parseGuideType(effectiveGuide)
    if (guideType == 'axis_logticks') {
      Map guideParams = context.extractGuideParams(effectiveGuide)
      renderYAxisLogTicks(axesGroup, scale, width, height, theme, guideParams)
      return
    }

    // Determine position: left (default) or right (for secondary axis)
    String position = 'left'
    if (scale instanceof ScaleYContinuous) {
      position = ((ScaleYContinuous) scale).position ?: 'left'
    } else if (scale instanceof ScaleXContinuous) {
      // For flipped coords, x scale appears as vertical axis
      position = ((ScaleXContinuous) scale).position == 'bottom' ? 'left' : 'right'
    }

    boolean isRight = (position == 'right')
    def xTranslate = isRight ? width + offsetX : -(offsetX as BigDecimal)

    G yAxisGroup = axesGroup.addG()
    yAxisGroup.id(isRight ? 'y-axis-right' : 'y-axis')
    yAxisGroup.transform("translate($xTranslate, 0)")

    // Axis line
    yAxisGroup.addLine(0, 0, 0, height)
              .stroke(theme.axisLineY?.color ?: 'black')
              .strokeWidth(theme.axisLineY?.size ?: 1)

    // Extract guide parameters
    Map guideParams = context.extractGuideParams(effectiveGuide)
    Number angle = guideParams.angle as Number ?: 0
    boolean checkOverlap = (guideParams['check.overlap'] ?: guideParams.checkOverlap) as Boolean ?: false
    // Calculate minimum spacing based on font size if not specified
    // Default: 2x font size for vertical spacing (labels are closer vertically)
    Number fontSize = (theme.axisTextY?.size ?: 10) as Number
    Number minSpacing = guideParams['min.spacing'] as Number ?: guideParams.minSpacing as Number ?: (fontSize.intValue() * 2)

    // Ticks and labels - works for both continuous and discrete scales
    List breaks = scale.getComputedBreaks()
    List<String> labels = scale.getComputedLabels()

    List<Double> renderedPositions = []
    breaks.eachWithIndex { breakVal, i ->
      Double yPos = scale.transform(breakVal) as Double
      if (yPos == null) return  // Skip if transform returns null

      // Check overlap if requested
      if (checkOverlap && context.shouldSkipForOverlap(yPos as Number, renderedPositions as List<Number>, minSpacing)) {
        return  // Skip this label
      }
      renderedPositions << yPos

      // Tick mark (direction depends on position)
      int tickDir = isRight ? 1 : -1
      yAxisGroup.addLine(0, yPos, tickDir * (theme.axisTickLength ?: 5), yPos)
                .stroke('black')

      // Label (position depends on axis position)
      String label = i < labels.size() ? labels[i] : breakVal.toString()
      if (isRight) {
        def text = yAxisGroup.addText(label)
                  .x((theme.axisTickLength ?: 5) + 5)
                  .y(yPos + 4)
                  .fontSize(theme.axisTextY?.size ?: 10)

        // Apply rotation if specified (Y axis rotation is less common but supported)
        if (angle != 0) {
          int labelX = (theme.axisTickLength ?: 5) + 5
          text.transform("rotate($angle, $labelX, $yPos)")
          text.textAnchor(angle > 0 ? 'end' : 'start')
        } else {
          text.textAnchor('start')
        }
      } else {
        def text = yAxisGroup.addText(label)
                  .x(-1 * (theme.axisTickLength ?: 5) - 5)
                  .y(yPos + 4)
                  .fontSize(theme.axisTextY?.size ?: 10)

        // Apply rotation if specified
        if (angle != 0) {
          int labelX = -1 * (theme.axisTickLength ?: 5) - 5
          text.transform("rotate($angle, $labelX, $yPos)")
          text.textAnchor(angle > 0 ? 'start' : 'end')
        } else {
          text.textAnchor('end')
        }
      }
    }
  }

  /**
   * Render stacked X axes.
   */
  @CompileStatic
  private void renderStackedXAxis(G axesGroup, Scale scale, int width, int height, Theme theme) {
    Map params = context.extractGuideParams(scale?.guide)

    // Extract stacked guides
    Object first = params.first
    List additional = params.additional as List ?: []
    List<Object> allGuides = [first] + additional

    // Get spacing parameter
    Number spacing = (params.spacing ?: 5) as Number

    // Calculate offset increment
    Number tickLength = (theme.axisTickLength ?: 5) as Number
    Number labelHeight = (theme.axisTextX?.size ?: 10) as Number
    Number offsetIncrement = tickLength + labelHeight + spacing + 5  // +5 for padding

    // Render each stacked guide
    Number currentOffset = 0
    allGuides.each { guideSpec ->
      // Parse guide specification
      Guide childGuide
      if (guideSpec instanceof Guide) {
        childGuide = guideSpec
      } else if (guideSpec instanceof String) {
        childGuide = new Guide(guideSpec, [:])
      } else {
        childGuide = new Guide('axis', [:])
      }

      // Render axis with current offset and override guide
      renderXAxis(axesGroup, scale, width, height, theme, currentOffset, childGuide)

      // Increment offset for next guide
      currentOffset += offsetIncrement
    }
  }

  /**
   * Render stacked Y axes.
   */
  @CompileStatic
  private void renderStackedYAxis(G axesGroup, Scale scale, int width, int height, Theme theme) {
    Map params = context.extractGuideParams(scale?.guide)

    // Extract stacked guides
    Object first = params.first
    List additional = params.additional as List ?: []
    List<Object> allGuides = [first] + additional

    // Get spacing parameter
    Number spacing = (params.spacing ?: 5) as Number

    // Calculate offset increment
    Number tickLength = (theme.axisTickLength ?: 5) as Number
    Number labelWidth = 40  // Approximate max label width - could be calculated more precisely
    Number offsetIncrement = tickLength + labelWidth + spacing

    // Render each stacked guide
    Number currentOffset = 0
    allGuides.each { guideSpec ->
      // Parse guide specification
      Guide childGuide
      if (guideSpec instanceof Guide) {
        childGuide = guideSpec
      } else if (guideSpec instanceof String) {
        childGuide = new Guide(guideSpec, [:])
      } else {
        childGuide = new Guide('axis', [:])
      }

      // Render axis with current offset and override guide
      renderYAxis(axesGroup, scale, width, height, theme, currentOffset, childGuide)

      // Increment offset for next guide
      currentOffset += offsetIncrement
    }
  }

  /**
   * Render X axis with logarithmic ticks.
   */
  @CompileStatic(TypeCheckingMode.SKIP)
  private void renderXAxisLogTicks(G axesGroup, Scale scale, int width, int height, Theme theme, Map guideParams) {
    // Determine position: bottom (default) or top (for secondary axis)
    String position = 'bottom'
    if (scale instanceof ScaleXContinuous) {
      position = ((ScaleXContinuous) scale).position ?: 'bottom'
    } else if (scale instanceof ScaleYContinuous) {
      // For flipped coords, y scale appears as horizontal axis
      position = ((ScaleYContinuous) scale).position == 'left' ? 'bottom' : 'top'
    }

    boolean isTop = (position == 'top')
    int yTranslate = isTop ? 0 : height

    G xAxisGroup = axesGroup.addG()
    xAxisGroup.id(isTop ? 'x-axis-top' : 'x-axis')
    xAxisGroup.transform("translate(0, $yTranslate)")

    // Axis line
    xAxisGroup.addLine(0, 0, width, 0)
              .stroke(theme.axisLineX?.color ?: 'black')
              .strokeWidth(theme.axisLineX?.size ?: 1)

    // Extract log tick parameters
    BigDecimal longMult = (guideParams.long ?: 2.25) as BigDecimal
    BigDecimal midMult = (guideParams.mid ?: 1.5) as BigDecimal
    BigDecimal shortMult = (guideParams.short ?: 0.75) as BigDecimal

    // Calculate log ticks
    List<LogTickInfo> ticks = context.calculateLogTicks(scale, guideParams, longMult, midMult, shortMult)

    // Render ticks
    int tickDir = isTop ? -1 : 1
    ticks.each { LogTickInfo tick ->
      Double xPos = scale.transform(tick.transformValue) as Double
      if (xPos == null) return

      BigDecimal tickLength = (theme.axisTickLength ?: 5) * tick.tickMultiplier
      xAxisGroup.addLine(xPos, 0, xPos, tickDir * tickLength)
                .stroke('black')
    }

    // Render main labels (only for 'long' ticks)
    List<String> labels = scale.getComputedLabels()
    List breaks = scale.getComputedBreaks()
    int labelIdx = 0
    ticks.findAll { it.tickType == 'long' }.each { LogTickInfo tick ->
      Double xPos = scale.transform(tick.transformValue) as Double
      if (xPos == null) return

      String label
      if (labelIdx < labels.size()) {
        label = labels[labelIdx]
      } else if (labelIdx < breaks.size()) {
        label = context.formatNumber(breaks[labelIdx])
      } else {
        label = context.formatNumber(tick.displayValue)
      }
      labelIdx++

      int labelY = isTop ? (tickDir * (theme.axisTickLength ?: 5) * longMult - 3) as int
                        : (tickDir * (theme.axisTickLength ?: 5) * longMult + 15) as int

      xAxisGroup.addText(label)
                .x(xPos)
                .y(labelY)
                .fontSize(theme.axisTextX?.size ?: 10)
                .textAnchor('middle')
    }
  }

  /**
   * Render Y axis with logarithmic ticks.
   */
  @CompileStatic(TypeCheckingMode.SKIP)
  private void renderYAxisLogTicks(G axesGroup, Scale scale, int width, int height, Theme theme, Map guideParams) {
    // Determine position: left (default) or right (for secondary axis)
    String position = 'left'
    if (scale instanceof ScaleYContinuous) {
      position = ((ScaleYContinuous) scale).position ?: 'left'
    } else if (scale instanceof ScaleXContinuous) {
      // For flipped coords, x scale appears as vertical axis
      position = ((ScaleXContinuous) scale).position == 'bottom' ? 'left' : 'right'
    }

    boolean isRight = (position == 'right')

    G yAxisGroup = axesGroup.addG()
    yAxisGroup.id(isRight ? 'y-axis-right' : 'y-axis')
    if (isRight) {
      yAxisGroup.transform("translate($width, 0)")
    }

    // Axis line
    yAxisGroup.addLine(0, 0, 0, height)
              .stroke(theme.axisLineY?.color ?: 'black')
              .strokeWidth(theme.axisLineY?.size ?: 1)

    // Extract log tick parameters
    BigDecimal longMult = (guideParams.long ?: 2.25) as BigDecimal
    BigDecimal midMult = (guideParams.mid ?: 1.5) as BigDecimal
    BigDecimal shortMult = (guideParams.short ?: 0.75) as BigDecimal

    // Calculate log ticks
    List<LogTickInfo> ticks = context.calculateLogTicks(scale, guideParams, longMult, midMult, shortMult)

    // Render ticks
    int tickDir = isRight ? 1 : -1
    ticks.each { LogTickInfo tick ->
      Double yPos = scale.transform(tick.transformValue) as Double
      if (yPos == null) return

      BigDecimal tickLength = (theme.axisTickLength ?: 5) * tick.tickMultiplier
      yAxisGroup.addLine(0, yPos, tickDir * tickLength, yPos)
                .stroke('black')
    }

    // Render main labels (only for 'long' ticks)
    List<String> labels = scale.getComputedLabels()
    List breaks = scale.getComputedBreaks()
    int labelIdx = 0
    ticks.findAll { it.tickType == 'long' }.each { LogTickInfo tick ->
      Double yPos = scale.transform(tick.transformValue) as Double
      if (yPos == null) return

      String label
      if (labelIdx < labels.size()) {
        label = labels[labelIdx]
      } else if (labelIdx < breaks.size()) {
        label = context.formatNumber(breaks[labelIdx])
      } else {
        label = context.formatNumber(tick.displayValue)
      }
      labelIdx++

      if (isRight) {
        int labelX = ((theme.axisTickLength ?: 5) * longMult + 5) as int
        yAxisGroup.addText(label)
                  .x(labelX)
                  .y(yPos + 4)
                  .fontSize(theme.axisTextY?.size ?: 10)
                  .textAnchor('start')
      } else {
        int labelX = (-1 * (theme.axisTickLength ?: 5) * longMult - 5) as int
        yAxisGroup.addText(label)
                  .x(labelX)
                  .y(yPos + 4)
                  .fontSize(theme.axisTextY?.size ?: 10)
                  .textAnchor('end')
      }
    }
  }

  /**
   * Data class for logarithmic tick information.
   */
  @CompileStatic
  static class LogTickInfo {
    BigDecimal transformValue
    BigDecimal displayValue  // The actual value to show in labels
    String tickType  // 'long', 'mid', or 'short'
    BigDecimal tickMultiplier

    LogTickInfo(BigDecimal transformValue, BigDecimal displayValue, String tickType, BigDecimal tickMultiplier) {
      this.transformValue = transformValue
      this.displayValue = displayValue
      this.tickType = tickType
      this.tickMultiplier = tickMultiplier
    }
  }
}
