package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.CharmCoordType
import se.alipsa.matrix.charm.GuideSpec
import se.alipsa.matrix.charm.GuideType
import se.alipsa.matrix.charm.Log10ScaleTransform
import se.alipsa.matrix.charm.GuidesSpec
import se.alipsa.matrix.charm.render.scale.CharmScale
import se.alipsa.matrix.charm.render.scale.ContinuousCharmScale
import se.alipsa.matrix.charm.theme.ElementLine
import se.alipsa.matrix.charm.theme.ElementText
import se.alipsa.matrix.core.util.Logger

import java.math.RoundingMode
import static se.alipsa.matrix.ext.NumberExtension.PI

/**
 * Renders cartesian axes, tick labels, and axis guide variants.
 */
@CompileStatic
class AxisRenderer {

  private static final Logger log = Logger.getLogger(AxisRenderer)

  /**
   * Renders x and y axes with guide-type dispatching.
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

    String defaultLineColor = '#333333'
    BigDecimal defaultLineWidth = 1
    String defaultTextColor = '#333333'
    BigDecimal defaultTextSize = (context.chart.theme.baseSize ?: 10) as BigDecimal

    G axes = group.addG().id('axes')

    // Read axis guide specs
    GuidesSpec guides = context.chart.guides
    GuideSpec xGuide = guides?.getSpec('x')
    GuideSpec yGuide = guides?.getSpec('y')
    CharmCoordType coordType = context.chart.coord?.type ?: CharmCoordType.CARTESIAN
    if (coordType == CharmCoordType.POLAR || coordType == CharmCoordType.RADIAL) {
      renderPolarAxes(group, context, panelWidth, panelHeight, coordType, guides)
      return
    }

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
      GuideSpec effectiveXGuide = resolveEffectiveGuide(xGuide)
      if (effectiveXGuide?.type == GuideType.AXIS_LOGTICKS && context.xScale instanceof ContinuousCharmScale) {
        renderAxisLogticks(axes, context, panelWidth, panelHeight, true, effectiveXGuide, xLine, xText,
            defaultLineColor, defaultLineWidth, defaultTextColor, defaultTextSize)
      } else {
        renderStandardXAxis(axes, context, panelWidth, panelHeight, tickLen, tickCount,
            xLine, xText, effectiveXGuide, nulls, defaultLineColor, defaultLineWidth, defaultTextColor, defaultTextSize)
      }

      // Render additional stacked axis guides
      if (xGuide?.type == GuideType.AXIS_STACK) {
        renderStackedAxes(axes, context, panelWidth, panelHeight, true, xGuide, xLine, xText,
            tickLen, tickCount, nulls, defaultLineColor, defaultLineWidth, defaultTextColor, defaultTextSize)
      }
    }

    // Y-axis ticks and labels
    if (!nulls.contains('axisTextY')) {
      GuideSpec effectiveYGuide = resolveEffectiveGuide(yGuide)
      if (effectiveYGuide?.type == GuideType.AXIS_LOGTICKS && context.yScale instanceof ContinuousCharmScale) {
        renderAxisLogticks(axes, context, panelWidth, panelHeight, false, effectiveYGuide, yLine, yText,
            defaultLineColor, defaultLineWidth, defaultTextColor, defaultTextSize)
      } else {
        renderStandardYAxis(axes, context, panelHeight, tickLen, tickCount,
            yLine, yText, effectiveYGuide, nulls, defaultLineColor, defaultLineWidth, defaultTextColor, defaultTextSize)
      }

      // Render additional stacked axis guides
      if (yGuide?.type == GuideType.AXIS_STACK) {
        renderStackedAxes(axes, context, panelWidth, panelHeight, false, yGuide, yLine, yText,
            tickLen, tickCount, nulls, defaultLineColor, defaultLineWidth, defaultTextColor, defaultTextSize)
      }
    }
  }

  private void renderStandardXAxis(G axes, RenderContext context, int panelWidth, int panelHeight,
                                    int tickLen, int tickCount,
                                    ElementLine xLine, ElementText xText, GuideSpec xGuide,
                                    Set<String> nulls,
                                    String defaultLineColor, BigDecimal defaultLineWidth,
                                    String defaultTextColor, BigDecimal defaultTextSize,
                                    String axisGroupId = 'x-axis') {
    ElementLine xTicks = context.chart.theme.axisTicksX
    String xTickColor = xTicks?.color ?: xLine?.color ?: defaultLineColor
    BigDecimal xTickWidth = (xTicks?.size ?: xLine?.size ?: defaultLineWidth) as BigDecimal
    String xTextColor = xText?.color ?: defaultTextColor
    BigDecimal xTextSize = (xText?.size ?: defaultTextSize) as BigDecimal

    // Label rotation angle from guide params
    Number labelAngle = xGuide?.params?.get('angle') as Number
    Map<?, ?> xParams = xGuide?.params as Map<?, ?>
    boolean checkOverlap = false
    if (xParams != null) {
      if (xParams.containsKey('check.overlap')) {
        checkOverlap = xParams.get('check.overlap') as boolean
      } else if (xParams.containsKey('checkOverlap')) {
        checkOverlap = xParams.get('checkOverlap') as boolean
      }
    }

    G xAxisGroup = axes.addG().id(axisGroupId)
    List<Object> xTickValues = context.xScale.ticks(tickCount)
    List<String> xLabels = context.xScale.tickLabels(tickCount)

    BigDecimal lastLabelEnd = null

    xTickValues.eachWithIndex { Object tick, int idx ->
      BigDecimal x = context.xScale.transform(tick)
      if (x != null) {
        if (!nulls.contains('axisTicksX')) {
          xAxisGroup.addLine(x, panelHeight, x, panelHeight + tickLen)
              .stroke(xTickColor).strokeWidth(xTickWidth).styleClass('charm-axis-tick')
        }
        String label = idx < xLabels.size() ? xLabels[idx] : formatTick(tick)

        // Estimate label width (heuristic: assumes ~0.6em average character width; may need
        // tuning for non-monospace fonts since SVG lacks text measurement without rendering)
        BigDecimal estimatedWidth = label.length() * xTextSize * 0.6

        // Check overlap
        if (checkOverlap && lastLabelEnd != null) {
          if (x - estimatedWidth / 2 < lastLabelEnd) {
            return // skip this label
          }
        }

        if (labelAngle != null && labelAngle != 0) {
          BigDecimal textY = panelHeight + tickLen + 14
          def textEl = xAxisGroup.addText(label)
              .x(x).y(textY)
              .textAnchor('end')
              .fill(xTextColor)
              .fontSize(xTextSize)
              .transform("rotate(${labelAngle}, ${x}, ${textY})")
              .styleClass('charm-axis-label')
        } else {
          xAxisGroup.addText(label)
              .x(x)
              .y(panelHeight + tickLen + 14)
              .textAnchor('middle')
              .fill(xTextColor)
              .fontSize(xTextSize)
              .styleClass('charm-axis-label')
        }

        lastLabelEnd = x + estimatedWidth / 2
      }
    }
  }

  private void renderStandardYAxis(G axes, RenderContext context, int panelHeight,
                                    int tickLen, int tickCount,
                                    ElementLine yLine, ElementText yText, GuideSpec yGuide,
                                    Set<String> nulls,
                                    String defaultLineColor, BigDecimal defaultLineWidth,
                                    String defaultTextColor, BigDecimal defaultTextSize,
                                    String axisGroupId = 'y-axis') {
    ElementLine yTicks = context.chart.theme.axisTicksY
    String yTickColor = yTicks?.color ?: yLine?.color ?: defaultLineColor
    BigDecimal yTickWidth = (yTicks?.size ?: yLine?.size ?: defaultLineWidth) as BigDecimal
    String yTextColor = yText?.color ?: defaultTextColor
    BigDecimal yTextSize = (yText?.size ?: defaultTextSize) as BigDecimal

    G yAxisGroup = axes.addG().id(axisGroupId)
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

  /**
   * Renders logarithmic tick marks with three tiers:
   * long ticks at powers of 10, mid at 2x/5x, short at intermediates.
   */
  private void renderAxisLogticks(G axes, RenderContext context, int panelWidth, int panelHeight,
                                   boolean isXAxis, GuideSpec guide, ElementLine axisLine,
                                   ElementText axisText,
                                   String defaultLineColor, BigDecimal defaultLineWidth,
                                   String defaultTextColor, BigDecimal defaultTextSize) {
    ContinuousCharmScale scale = isXAxis
        ? context.xScale as ContinuousCharmScale
        : context.yScale as ContinuousCharmScale

    Map<String, Object> params = guide?.params ?: [:]
    BigDecimal longMult = (params['long'] ?: 1.0) as BigDecimal
    BigDecimal midMult = (params['mid'] ?: 0.6) as BigDecimal
    BigDecimal shortMult = (params['short'] ?: 0.3) as BigDecimal

    String tickColor = axisLine?.color ?: defaultLineColor
    BigDecimal tickWidth = (axisLine?.size ?: defaultLineWidth) as BigDecimal
    String textColor = axisText?.color ?: defaultTextColor
    BigDecimal textSize = (axisText?.size ?: defaultTextSize) as BigDecimal

    int baseTickLen = context.config.axisTickLength
    G tickGroup = axes.addG().id(isXAxis ? 'x-axis' : 'y-axis')

    // Determine range of powers of 10
    BigDecimal dMin = scale.domainMin
    BigDecimal dMax = scale.domainMax
    if (dMin == null || dMax == null) {
      log.warn("renderAxisLogticks: domain bounds are null. Skipping log tick rendering.")
      return
    }

    // When the scale uses a log10 transform, or the guide declares prescaleBase,
    // domain bounds are already in log space (exponents). Otherwise they are in
    // data space and we need to take log10 to derive exponents.
    boolean domainIsLogSpace = scale.transformStrategy instanceof Log10ScaleTransform ||
        params['prescaleBase'] != null

    int minExp, maxExp
    if (domainIsLogSpace) {
      // Domain bounds ARE the exponents (e.g. -0.1 .. 2.1 for data range ~0.8 .. 126)
      minExp = dMin.floor() as int
      maxExp = dMax.ceil() as int
    } else {
      // Domain is in data space; need positive values for log
      if (dMin <= 0 || dMax <= 0) {
        log.warn("renderAxisLogticks: log scale requires positive domain bounds, " +
            "but got domainMin=${dMin}, domainMax=${dMax}. Skipping log tick rendering.")
        return
      }
      minExp = dMin.log10().floor() as int
      maxExp = dMax.log10().ceil() as int
    }

    // Cap exponent range to avoid excessive tick generation on very wide domains.
    // Configurable via guide param 'maxExponentRange'; defaults to 50.
    int maxIterations = (params['maxExponentRange'] ?: 50) as int
    if (maxExp - minExp > maxIterations) {
      log.warn("renderAxisLogticks: exponent range ${minExp}..${maxExp} exceeds limit of ${maxIterations}; " +
          "some ticks may be omitted. Set guide param 'maxExponentRange' to increase the limit.")
      maxExp = minExp + maxIterations
    }

    for (int exp = minExp; exp <= maxExp; exp++) {
      BigDecimal powerOf10 = (10 ** exp) as BigDecimal

      // Long tick at power of 10
      BigDecimal pos = scale.transform(powerOf10)
      if (pos != null) {
        int len = (baseTickLen * longMult) as int
        drawLogtick(tickGroup, pos, len, isXAxis, panelWidth, panelHeight, tickColor, tickWidth)
        // Label at powers of 10
        String label = formatTick(powerOf10)
        if (isXAxis) {
          tickGroup.addText(label)
              .x(pos).y(panelHeight + len + 14)
              .textAnchor('middle').fill(textColor).fontSize(textSize)
              .styleClass('charm-axis-label')
        } else {
          tickGroup.addText(label)
              .x(-len - 4).y(pos + 4)
              .textAnchor('end').fill(textColor).fontSize(textSize)
              .styleClass('charm-axis-label')
        }
      }

      // Mid ticks at 2x and 5x
      [2, 5].each { int mult ->
        BigDecimal midVal = powerOf10 * mult
        BigDecimal midPos = scale.transform(midVal)
        if (midPos != null) {
          int len = (baseTickLen * midMult) as int
          drawLogtick(tickGroup, midPos, len, isXAxis, panelWidth, panelHeight, tickColor, tickWidth)
        }
      }

      // Short ticks at other intermediates
      [3, 4, 6, 7, 8, 9].each { int mult ->
        BigDecimal shortVal = powerOf10 * mult
        BigDecimal shortPos = scale.transform(shortVal)
        if (shortPos != null) {
          int len = (baseTickLen * shortMult) as int
          drawLogtick(tickGroup, shortPos, len, isXAxis, panelWidth, panelHeight, tickColor, tickWidth)
        }
      }
    }
  }

  /**
   * Resolves the effective guide for rendering.
   * For AXIS_STACK, returns the 'first' guide. Otherwise returns the guide as-is.
   */
  private static GuideSpec resolveEffectiveGuide(GuideSpec guide) {
    if (guide == null) return null
    if (guide.type == GuideType.AXIS_STACK) {
      // Extract 'first' guide from stack params
      Object first = guide.params['first']
      if (first instanceof GuideSpec) {
        return first as GuideSpec
      }
      if (first instanceof Map) {
        Map firstMap = first as Map
        String typeStr = firstMap['type']?.toString()
        GuideType type = typeStr ? GuideType.fromString(typeStr) : GuideType.AXIS
        Map<String, Object> params = firstMap.findAll { k, v -> k != 'type' } as Map<String, Object>
        return new GuideSpec(type, params)
      }
      // Default: standard axis
      return new GuideSpec(GuideType.AXIS)
    }
    guide
  }

  /**
   * Renders stacked (additional) axis guides.
   */
  private void renderStackedAxes(G axes, RenderContext context, int panelWidth, int panelHeight,
                                  boolean isXAxis, GuideSpec stackGuide,
                                  ElementLine axisLine, ElementText axisText,
                                  int tickLen, int tickCount, Set<String> nulls,
                                  String defaultLineColor, BigDecimal defaultLineWidth,
                                  String defaultTextColor, BigDecimal defaultTextSize) {
    Object additional = stackGuide.params['additional']
    if (!(additional instanceof List)) return

    List additionalGuides = additional as List
    String axisPrefix = isXAxis ? 'x-axis' : 'y-axis'
    BigDecimal spacingParam = (stackGuide.params['spacing'] ?: 0) as BigDecimal
    // Each stacked axis is offset by tick length + estimated label height/width + spacing
    BigDecimal axisTextSize = (axisText?.size ?: defaultTextSize) as BigDecimal
    BigDecimal baseOffset = tickLen + axisTextSize + 14 + spacingParam

    additionalGuides.eachWithIndex { Object additionalEntry, int idx ->
      GuideSpec additionalGuide
      if (additionalEntry instanceof GuideSpec) {
        additionalGuide = additionalEntry as GuideSpec
      } else if (additionalEntry instanceof Map) {
        Map entryMap = additionalEntry as Map
        String typeStr = entryMap['type']?.toString()
        GuideType type = typeStr ? GuideType.fromString(typeStr) : GuideType.AXIS
        Map<String, Object> params = entryMap.findAll { k, v -> k != 'type' } as Map<String, Object>
        additionalGuide = new GuideSpec(type, params)
      } else {
        return
      }

      String stackedId = "${axisPrefix}-stack-${idx}"
      BigDecimal offset = baseOffset * (idx + 1)
      G stackGroup = axes.addG().id(stackedId)
      if (isXAxis) {
        stackGroup.addAttribute('transform', "translate(0, ${offset})")
        renderStandardXAxis(stackGroup, context, panelWidth, panelHeight, tickLen, tickCount,
            axisLine, axisText, additionalGuide, nulls, defaultLineColor, defaultLineWidth,
            defaultTextColor, defaultTextSize, "${stackedId}-inner")
      } else {
        stackGroup.addAttribute('transform', "translate(${-offset}, 0)")
        renderStandardYAxis(stackGroup, context, panelHeight, tickLen, tickCount,
            axisLine, axisText, additionalGuide, nulls, defaultLineColor, defaultLineWidth,
            defaultTextColor, defaultTextSize, "${stackedId}-inner")
      }
    }
  }

  private static void drawLogtick(G group, BigDecimal pos, int len, boolean isXAxis,
                                   int panelWidth, int panelHeight,
                                   String color, BigDecimal width) {
    if (isXAxis) {
      group.addLine(pos, panelHeight, pos, panelHeight + len)
          .stroke(color).strokeWidth(width).styleClass('charm-axis-tick')
    } else {
      group.addLine(0, pos, -len, pos)
          .stroke(color).strokeWidth(width).styleClass('charm-axis-tick')
    }
  }

  private static String formatTick(Object tick) {
    if (tick instanceof BigDecimal) {
      BigDecimal rounded = (tick as BigDecimal).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
      return rounded.toPlainString()
    }
    tick?.toString() ?: ''
  }

  private void renderPolarAxes(
      G plotArea,
      RenderContext context,
      int panelWidth,
      int panelHeight,
      CharmCoordType coordType,
      GuidesSpec guides
  ) {
    String thetaAes = resolveThetaAesthetic(context.chart.coord?.params)
    GuideSpec thetaGuide = guides?.getSpec(thetaAes)
    if (thetaGuide?.type != GuideType.AXIS_THETA) {
      return
    }
    CharmScale thetaScale = thetaAes == 'y' ? context.yScale : context.xScale
    if (thetaScale == null) {
      return
    }

    renderThetaAxis(plotArea, context, thetaScale, thetaGuide, panelWidth, panelHeight, coordType)
  }

  private static void renderThetaAxis(
      G plotArea,
      RenderContext context,
      CharmScale thetaScale,
      GuideSpec guide,
      int panelWidth,
      int panelHeight,
      CharmCoordType coordType
  ) {
    Map<String, Object> params = guide?.params ?: [:]
    BigDecimal labelAngle = (params.angle as BigDecimal) ?: 0
    boolean showMinorTicks = (params['minor.ticks'] ?: params.minorTicks ?: false) as boolean
    String cap = (params.cap ?: 'none').toString()
    BigDecimal start = (context.chart.coord?.params?.start as BigDecimal) ?: 0
    Integer direction = context.chart.coord?.params?.direction as Integer
    boolean clockwise = context.chart.coord?.params?.containsKey('clockwise')
        ? context.chart.coord?.params?.clockwise as boolean
        : (direction == null || direction >= 0)

    BigDecimal cx = panelWidth / 2 as BigDecimal
    BigDecimal cy = panelHeight / 2 as BigDecimal
    BigDecimal outerRadius = panelWidth.min(panelHeight) / 2 * 0.9
    BigDecimal tickLength = (context.chart.theme.axisTickLength ?: 5) as BigDecimal
    BigDecimal labelRadius = outerRadius + tickLength + 5
    String axisColor = context.chart.theme.axisLineX?.color ?: '#333333'
    BigDecimal axisWidth = (context.chart.theme.axisLineX?.size ?: 1) as BigDecimal
    String textColor = context.chart.theme.axisTextX?.color ?: '#333333'
    BigDecimal textSize = (context.chart.theme.axisTextX?.size ?: (context.chart.theme.baseSize ?: 10)) as BigDecimal

    List<Object> breaks = thetaScale.ticks(context.config.axisTickCount)
    List<String> labels = thetaScale.tickLabels(context.config.axisTickCount)
    G axisGroup = plotArea.addG().id('theta-axis')

    breaks.eachWithIndex { Object breakVal, int i ->
      BigDecimal norm = normalizeFromScale(thetaScale, breakVal)
      if (norm == null) {
        return
      }
      BigDecimal theta = clockwise ? (start + norm * 2 * PI) : (start - norm * 2 * PI)
      BigDecimal x1 = cx + outerRadius * theta.sin()
      BigDecimal y1 = cy - outerRadius * theta.cos()
      BigDecimal tickOuter = outerRadius + tickLength
      BigDecimal x2 = cx + tickOuter * theta.sin()
      BigDecimal y2 = cy - tickOuter * theta.cos()

      axisGroup.addLine(x1, y1, x2, y2)
          .stroke(axisColor)
          .strokeWidth(axisWidth)
          .styleClass('charm-axis-tick')

      String label = i < labels.size() ? labels[i] : breakVal?.toString()
      if (label != null && !label.isEmpty()) {
        BigDecimal labelX = cx + labelRadius * theta.sin()
        BigDecimal labelY = cy - labelRadius * theta.cos()
        def text = axisGroup.addText(label)
            .x(labelX)
            .y(labelY)
            .textAnchor('middle')
            .dominantBaseline('middle')
            .fill(textColor)
            .fontSize(textSize)
            .styleClass('charm-axis-label')
        if (labelAngle != 0) {
          text.transform("rotate(${labelAngle}, ${labelX}, ${labelY})")
        }
      }
    }

    if (showMinorTicks && breaks.size() >= 2) {
      for (int i = 0; i < breaks.size() - 1; i++) {
        BigDecimal startNorm = normalizeFromScale(thetaScale, breaks[i])
        BigDecimal endNorm = normalizeFromScale(thetaScale, breaks[i + 1])
        if (startNorm == null || endNorm == null) {
          continue
        }
        for (int j = 1; j <= 4; j++) {
          BigDecimal minorNorm = startNorm + (endNorm - startNorm) * j / 5
          BigDecimal minorTheta = clockwise ? (start + minorNorm * 2 * PI) : (start - minorNorm * 2 * PI)
          BigDecimal minorOuter = outerRadius + tickLength * 0.5
          axisGroup.addLine(
              cx + outerRadius * minorTheta.sin(),
              cy - outerRadius * minorTheta.cos(),
              cx + minorOuter * minorTheta.sin(),
              cy - minorOuter * minorTheta.cos()
          )
              .stroke(axisColor)
              .strokeWidth(axisWidth * 0.75)
              .styleClass('charm-axis-tick')
        }
      }
    }

    if (cap != 'none' || coordType == CharmCoordType.RADIAL) {
      axisGroup.addCircle()
          .cx(cx)
          .cy(cy)
          .r(outerRadius)
          .fill('none')
          .stroke(axisColor)
          .strokeWidth(axisWidth)
          .styleClass('charm-axis-line')
    }
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
