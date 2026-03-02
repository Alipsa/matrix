package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.GuideSpec
import se.alipsa.matrix.charm.GuideType
import se.alipsa.matrix.charm.GuidesSpec
import se.alipsa.matrix.charm.render.scale.CharmScale
import se.alipsa.matrix.charm.render.scale.ColorCharmScale
import se.alipsa.matrix.charm.render.scale.ContinuousCharmScale
import se.alipsa.matrix.charm.render.scale.DiscreteCharmScale
import se.alipsa.matrix.charm.render.scale.TrainedScales
import se.alipsa.matrix.charm.theme.ElementText
import se.alipsa.matrix.core.util.Logger

/**
 * Renders guides and legends for all aesthetic channels.
 *
 * Supports discrete legends, colorbars, colorsteps, size legends,
 * alpha legends, custom guides, and legend merging/positioning.
 */
@CompileStatic
class LegendRenderer {

  private static final Logger log = Logger.getLogger(LegendRenderer)

  private static final int LEGEND_PLOT_GAP = 20
  private static final int LEGEND_TITLE_SPACING = 10
  private static final int COLORBAR_WIDTH_VERTICAL = 15
  private static final int COLORBAR_HEIGHT_VERTICAL = 100
  private static final int COLORBAR_WIDTH_HORIZONTAL = 100
  private static final int COLORBAR_HEIGHT_HORIZONTAL = 15
  private static final int COLORBAR_STEPS = 20

  /**
   * Renders guides/legends for all non-positional aesthetics with trained scales.
   *
   * @param context render context
   */
  void render(RenderContext context) {
    Object legendPos = context.chart.theme.legendPosition ?: context.config.legendPosition
    if (legendPos == 'none') {
      return
    }

    GuidesSpec guides = context.chart.guides

    // Collect aesthetics with trained scales
    Map<String, Object> legendScales = collectLegendScales(context, guides)

    // Check for custom guides
    boolean hasCustomGuides = guides?.specs?.any { String aes, GuideSpec spec ->
      spec?.type == GuideType.CUSTOM
    }

    if (legendScales.isEmpty() && !hasCustomGuides) {
      return
    }

    // Detect legend merging: shape + color with same discrete levels
    DiscreteCharmScale shapeDiscrete = legendScales.containsKey('shape') && context.shapeScale instanceof DiscreteCharmScale
        ? context.shapeScale as DiscreteCharmScale : null
    DiscreteCharmScale colorDiscrete = resolveColorDiscrete(context)
    boolean mergeShapeIntoColor = shapeDiscrete != null && colorDiscrete != null &&
        sameDiscreteLevels(shapeDiscrete, colorDiscrete)
    if (mergeShapeIntoColor) {
      legendScales.remove('shape')
    }

    // Calculate position
    int legendX, legendY
    boolean isVertical = context.chart.theme.legendDirection != 'horizontal'

    switch (legendPos) {
      case 'right' -> {
        legendX = context.config.width - context.config.marginRight + LEGEND_PLOT_GAP
        legendY = context.config.marginTop + 20
      }
      case 'left' -> {
        legendX = 10
        legendY = context.config.marginTop + 20
      }
      case 'top' -> {
        legendX = context.config.marginLeft
        legendY = 10
        isVertical = false
      }
      case 'bottom' -> {
        legendX = context.config.marginLeft
        legendY = context.config.height - 40
        isVertical = false
      }
      default -> {
        if (legendPos instanceof List && (legendPos as List).size() >= 2) {
          List pos = legendPos as List
          legendX = pos[0] as int
          legendY = pos[1] as int
        } else {
          legendX = context.config.width - context.config.marginRight + LEGEND_PLOT_GAP
          legendY = context.config.marginTop + 20
        }
      }
    }

    G legend = context.svg.addG().id('legend').transform("translate($legendX, $legendY)")

    ElementText titleText = context.chart.theme.legendTitle
    ElementText labelText = context.chart.theme.legendText
    BigDecimal defaultSize = (context.chart.theme.baseSize ?: 10) as BigDecimal

    // Render legend title
    String title = resolveLegendTitle(context, legendScales)
    int currentY = 0
    if (title) {
      String titleColor = titleText?.color ?: '#333333'
      BigDecimal titleSize = (titleText?.size ?: defaultSize + 1) as BigDecimal
      def titleEl = legend.addText(title)
          .x(0).y(titleSize)
          .fontSize(titleSize)
          .fill(titleColor)
          .addAttribute('font-weight', 'bold')
          .styleClass('charm-legend-title gg-legend-title')
      if (titleText?.family) {
        titleEl.addAttribute('font-family', titleText.family)
      }
      currentY = (titleSize as int) + LEGEND_TITLE_SPACING
    }

    // Determine if primary geom uses points
    boolean usesPoints = context.chart.layers.any { it.geomType?.name()?.toUpperCase() == 'POINT' }

    // Render each legend scale
    legendScales.each { String aesthetic, Object scaleObj ->
      // Per-layer scale entries have keys like "color_layer1"
      boolean isPerLayer = aesthetic.contains('_layer')
      String baseAesthetic = isPerLayer ? aesthetic.replaceAll(/_layer\d+$/, '') : aesthetic
      GuideType guideType = resolveGuideType(baseAesthetic, context, guides, scaleObj)

      // Render per-layer legend title
      if (isPerLayer) {
        String layerTitle = resolvePerLayerLegendTitle(aesthetic, scaleObj)
        if (layerTitle) {
          ElementText ltText = context.chart.theme.legendText
          BigDecimal ltSize = (ltText?.size ?: defaultSize) as BigDecimal
          legend.addText(layerTitle)
              .x(0).y(currentY + ltSize)
              .fontSize(ltSize)
              .fill(ltText?.color ?: '#333333')
              .addAttribute('font-style', 'italic')
              .styleClass('charm-legend-title charm-legend-layer-title')
          currentY += (ltSize as int) + 5
        }
      }

      switch (baseAesthetic) {
        case 'size' -> {
          currentY = renderSizeLegendFromScale(legend, context, scaleObj as CharmScale, currentY, isVertical)
        }
        case 'alpha' -> {
          currentY = renderAlphaLegendFromScale(legend, context, scaleObj as CharmScale, currentY, isVertical)
        }
        default -> {
          switch (guideType) {
            case GuideType.COLORBAR -> {
              currentY = renderColorbarFromScale(legend, context, baseAesthetic, scaleObj, currentY, isVertical)
            }
            case GuideType.COLORSTEPS -> {
              Map<String, Object> params = resolveGuideParams(baseAesthetic, guides, scaleObj)
              currentY = renderColorStepsFromScale(legend, context, baseAesthetic, scaleObj, currentY, isVertical, params)
            }
            case GuideType.LEGEND -> {
              if (isColorAesthetic(baseAesthetic)) {
                ColorCharmScale cs = resolveColorScaleObj(context, baseAesthetic, scaleObj)
                if (cs != null && !cs.levels.isEmpty()) {
                  DiscreteCharmScale shapeForKeys = (!isPerLayer && mergeShapeIntoColor) ? shapeDiscrete : null
                  currentY = renderDiscreteLegendFromScale(legend, context, baseAesthetic, cs, currentY,
                      isVertical, usesPoints, shapeForKeys)
                } else if (cs != null && cs.domainMin != null) {
                  currentY = renderContinuousAsDiscreteFromScale(legend, context, baseAesthetic, cs, currentY, isVertical)
                }
              } else if (baseAesthetic == 'shape') {
                currentY = renderShapeLegendFromScale(legend, context, scaleObj, currentY, isVertical, usesPoints)
              }
            }
            default -> {
              if (isColorAesthetic(baseAesthetic)) {
                ColorCharmScale cs = resolveColorScaleObj(context, baseAesthetic, scaleObj)
                if (cs != null && !cs.levels.isEmpty()) {
                  currentY = renderDiscreteLegendFromScale(legend, context, baseAesthetic, cs, currentY,
                      isVertical, usesPoints, null)
                } else if (cs != null && cs.domainMin != null) {
                  currentY = renderColorbarFromScale(legend, context, baseAesthetic, scaleObj, currentY, isVertical)
                }
              }
            }
          }
        }
      }
    }

    // Render custom guides
    if (guides?.specs) {
      guides.specs.each { String aesthetic, GuideSpec spec ->
        if (spec?.type == GuideType.CUSTOM) {
          currentY = renderCustomGuide(legend, context, aesthetic, currentY, spec)
        }
      }
    }
  }

  /**
   * Estimates the width needed for legends.
   *
   * @param context render context
   * @return estimated legend width in pixels, or 0 if no legend needed
   */
  int estimateLegendWidth(RenderContext context) {
    Object legendPos = context.chart.theme.legendPosition ?: context.config.legendPosition
    if (legendPos == 'none') {
      return 0
    }

    Map<String, Object> legendScales = collectLegendScales(context, context.chart.guides)
    if (legendScales.isEmpty()) {
      return 0
    }

    if (legendPos == 'right' || legendPos == 'left') {
      150
    } else {
      0
    }
  }

  // ---- Discrete Legend ----

  private int renderDiscreteLegend(G group, RenderContext context, String aesthetic,
                                    int startY, boolean vertical, boolean usesPoints,
                                    DiscreteCharmScale shapeForKeys) {
    ElementText labelText = context.chart.theme.legendText
    BigDecimal defaultSize = (context.chart.theme.baseSize ?: 10) as BigDecimal
    String labelColor = labelText?.color ?: '#333333'
    BigDecimal labelSize = (labelText?.size ?: defaultSize) as BigDecimal

    int keySize = context.config.legendKeySize
    int spacing = context.config.legendSpacing
    int textOffset = keySize + 8
    boolean isColorKey = isColorAesthetic(aesthetic)
    boolean isShapeKey = aesthetic == 'shape'

    List<String> levels
    if (isColorKey) {
      ColorCharmScale cs = aesthetic == 'fill' ? context.fillScale : context.colorScale
      levels = cs?.levels ?: []
    } else if (isShapeKey) {
      levels = (context.shapeScale instanceof DiscreteCharmScale)
          ? (context.shapeScale as DiscreteCharmScale).levels : []
    } else {
      return startY
    }

    int y = startY
    int x = 0

    levels.each { String level ->
      if (isColorKey) {
        ColorCharmScale cs = aesthetic == 'fill' ? context.fillScale : context.colorScale
        String color = cs.colorFor(level)

        if (shapeForKeys != null) {
          // Merged shape+color: draw shape with color fill
          int idx = shapeForKeys.levels.indexOf(level)
          String shapeName = resolveShapeName(shapeForKeys, idx)
          BigDecimal centerX = (x + keySize / 2).round()
          BigDecimal centerY = (y + keySize / 2).round()
          drawLegendShape(group, centerX, centerY, keySize, shapeName, color, '#666666')
        } else if (usesPoints) {
          BigDecimal radius = ((keySize - 2) / 2).round()
          group.addCircle()
              .styleClass('charm-legend-key gg-legend-key')
              .cx((x + keySize / 2).round())
              .cy((y + keySize / 2).round())
              .r(radius)
              .fill(color)
              .stroke('#666666')
        } else {
          group.addRect(keySize, keySize)
              .x(x).y(y)
              .fill(color)
              .stroke('#666666')
              .styleClass('charm-legend-key gg-legend-key')
        }
      } else if (isShapeKey) {
        DiscreteCharmScale colorDisc = resolveColorDiscrete(context)
        String shapeColor = colorDisc != null
            ? (context.colorScale?.colorFor(level) ?: '#333333')
            : '#333333'
        BigDecimal centerX = (x + keySize / 2).round()
        BigDecimal centerY = (y + keySize / 2).round()
        drawLegendShape(group, centerX, centerY, keySize, level, shapeColor, '#666666')
      }

      def labelEl = group.addText(level)
          .x(x + textOffset)
          .y(y + keySize - 2)
          .fontSize(labelSize)
          .fill(labelColor)
          .styleClass('charm-legend-label gg-legend-label')
      if (labelText?.family) {
        labelEl.addAttribute('font-family', labelText.family)
      }

      if (vertical) {
        y += keySize + spacing
      } else {
        x += keySize + textOffset + level.length() * 6 + spacing
      }
    }

    vertical ? y : y + keySize + spacing
  }

  // ---- Colorbar ----

  private int renderColorbar(G group, RenderContext context, String aesthetic,
                              int startY, boolean vertical) {
    ColorCharmScale cs = aesthetic == 'fill' ? context.fillScale : context.colorScale
    if (cs == null || cs.domainMin == null || cs.domainMax == null) {
      return startY
    }

    ElementText labelText = context.chart.theme.legendText
    BigDecimal labelSize = (labelText?.size ?: 9) as BigDecimal
    String labelColor = labelText?.color ?: '#333333'
    String borderColor = context.chart.theme.legendKey?.color ?: '#333333'

    int barWidth = vertical ? COLORBAR_WIDTH_VERTICAL : COLORBAR_WIDTH_HORIZONTAL
    int barHeight = vertical ? COLORBAR_HEIGHT_VERTICAL : COLORBAR_HEIGHT_HORIZONTAL
    int x = 0
    int y = startY

    // Draw gradient as small rects, using rounding to tile without gaps or overlaps
    int tDenominator = (COLORBAR_STEPS - 1).max(1) as int
    for (int i = 0; i < COLORBAR_STEPS; i++) {
      BigDecimal t = (i as BigDecimal) / tDenominator
      BigDecimal value = cs.domainMin + t * (cs.domainMax - cs.domainMin)
      String color = cs.colorFor(value) ?: '#999999'

      if (vertical) {
        BigDecimal fromRel = (i as BigDecimal) / COLORBAR_STEPS
        BigDecimal toRel = ((i + 1) as BigDecimal) / COLORBAR_STEPS
        int fromBottom = (fromRel * barHeight).round() as int
        int toBottom = (toRel * barHeight).round() as int
        int stepHeight = toBottom - fromBottom
        if (stepHeight <= 0) continue
        int stepY = y + barHeight - toBottom
        group.addRect(barWidth, stepHeight)
            .x(x).y(stepY).fill(color).stroke('none')
            .styleClass('charm-legend-colorbar gg-legend-colorbar')
      } else {
        BigDecimal fromRel = (i as BigDecimal) / COLORBAR_STEPS
        BigDecimal toRel = ((i + 1) as BigDecimal) / COLORBAR_STEPS
        int from = (fromRel * barWidth).round() as int
        int to = (toRel * barWidth).round() as int
        int stepWidth = to - from
        if (stepWidth <= 0) continue
        int stepX = x + from
        group.addRect(stepWidth, barHeight)
            .x(stepX).y(y).fill(color).stroke('none')
            .styleClass('charm-legend-colorbar gg-legend-colorbar')
      }
    }

    // Border
    group.addRect(barWidth, barHeight)
        .x(x).y(y).fill('none').stroke(borderColor)
        .styleClass('charm-legend-colorbar-border gg-legend-colorbar')

    // Min/max labels
    String minLabel = formatNumber(cs.domainMin)
    String maxLabel = formatNumber(cs.domainMax)

    if (vertical) {
      group.addText(minLabel)
          .x(x + barWidth + 5).y(y + barHeight)
          .fontSize(labelSize).fill(labelColor)
          .styleClass('charm-legend-colorbar-label gg-legend-label')
      group.addText(maxLabel)
          .x(x + barWidth + 5).y(y + 10)
          .fontSize(labelSize).fill(labelColor)
          .styleClass('charm-legend-colorbar-label gg-legend-label')
      y + barHeight + 5
    } else {
      group.addText(minLabel)
          .x(x).y(y + barHeight + 12)
          .fontSize(labelSize).fill(labelColor)
          .styleClass('charm-legend-colorbar-label gg-legend-label')
      group.addText(maxLabel)
          .x(x + barWidth - 20).y(y + barHeight + 12)
          .fontSize(labelSize).fill(labelColor)
          .styleClass('charm-legend-colorbar-label gg-legend-label')
      y + barHeight + 20
    }
  }

  // ---- Color Steps ----

  private int renderColorSteps(G group, RenderContext context, String aesthetic,
                                int startY, boolean vertical, Map<String, Object> guideParams) {
    ColorCharmScale cs = aesthetic == 'fill' ? context.fillScale : context.colorScale
    if (cs == null || cs.domainMin == null || cs.domainMax == null) {
      return startY
    }

    boolean evenSteps
    if (guideParams.containsKey('even.steps')) {
      evenSteps = guideParams['even.steps'] as boolean
    } else if (guideParams.containsKey('evenSteps')) {
      evenSteps = guideParams['evenSteps'] as boolean
    } else {
      evenSteps = true
    }

    boolean reverse
    if (guideParams.containsKey('reverse')) {
      reverse = guideParams['reverse'] as boolean
    } else {
      reverse = false
    }

    Boolean showLimits
    if (guideParams.containsKey('show.limits')) {
      showLimits = guideParams['show.limits'] as Boolean
    } else if (guideParams.containsKey('showLimits')) {
      showLimits = guideParams['showLimits'] as Boolean
    } else {
      showLimits = null
    }

    int barWidth = vertical ? COLORBAR_WIDTH_VERTICAL : COLORBAR_WIDTH_HORIZONTAL
    int barHeight = vertical ? COLORBAR_HEIGHT_VERTICAL : COLORBAR_HEIGHT_HORIZONTAL
    if (guideParams['barwidth'] != null) barWidth = guideParams['barwidth'] as int
    if (guideParams['barheight'] != null) barHeight = guideParams['barheight'] as int

    ElementText labelText = context.chart.theme.legendText
    BigDecimal labelSize = (labelText?.size ?: 9) as BigDecimal
    String labelColor = labelText?.color ?: '#333333'
    String borderColor = context.chart.theme.legendKey?.color ?: '#333333'

    int x = 0
    int y = startY

    // Generate breaks for steps
    int numBins = guideParams['nbin'] != null ? (guideParams['nbin'] as int) : 5
    BigDecimal range = cs.domainMax - cs.domainMin
    List<BigDecimal> breaks = []
    for (int i = 0; i <= numBins; i++) {
      breaks << cs.domainMin + range * i / numBins
    }
    if (reverse) breaks = breaks.reverse(false) as List<BigDecimal>

    // Render bins
    if (evenSteps || range == 0) {
      renderEvenColorSteps(group, cs, breaks, numBins, x, y, barWidth, barHeight, vertical, reverse)
    } else {
      renderProportionalColorSteps(group, cs, breaks, numBins, x, y, barWidth, barHeight, vertical, reverse)
    }

    // Border
    group.addRect(barWidth, barHeight)
        .x(x).y(y).fill('none').stroke(borderColor)
        .styleClass('charm-legend-colorbar-border gg-legend-colorbar')

    // Labels
    if (showLimits || showLimits == null) {
      String minLabel = formatNumber(breaks.first())
      String maxLabel = formatNumber(breaks.last())
      if (vertical) {
        group.addText(minLabel)
            .x(x + barWidth + 5).y(y + barHeight)
            .fontSize(labelSize).fill(labelColor)
            .styleClass('charm-legend-colorbar-label gg-legend-label')
        group.addText(maxLabel)
            .x(x + barWidth + 5).y(y + 10)
            .fontSize(labelSize).fill(labelColor)
            .styleClass('charm-legend-colorbar-label gg-legend-label')
      } else {
        group.addText(minLabel)
            .x(x).y(y + barHeight + 12)
            .fontSize(labelSize).fill(labelColor)
            .styleClass('charm-legend-colorbar-label gg-legend-label')
        group.addText(maxLabel)
            .x(x + barWidth - 20).y(y + barHeight + 12)
            .fontSize(labelSize).fill(labelColor)
            .styleClass('charm-legend-colorbar-label gg-legend-label')
      }
    }

    vertical ? y + barHeight + 5 : y + barHeight + 20
  }

  private void renderEvenColorSteps(G group, ColorCharmScale cs, List<BigDecimal> breaks,
                                     int numBins, int x, int y, int barWidth, int barHeight,
                                     boolean vertical, boolean reverse) {
    for (int i = 0; i < numBins; i++) {
      BigDecimal lower = breaks[i]
      BigDecimal upper = breaks[i + 1]
      BigDecimal midpoint = (lower + upper) / 2
      String color = cs.colorFor(midpoint) ?: '#999999'

      if (vertical) {
        int stepHeight = (barHeight / numBins) as int
        int stepY = reverse ? (y + i * stepHeight) : (y + barHeight - (i + 1) * stepHeight)
        group.addRect(barWidth, stepHeight + 1)
            .x(x).y(stepY).fill(color).stroke('none')
            .styleClass('charm-legend-colorbar gg-legend-colorbar')
      } else {
        int stepWidth = (barWidth / numBins) as int
        int stepX = x + i * stepWidth
        group.addRect(stepWidth + 1, barHeight)
            .x(stepX).y(y).fill(color).stroke('none')
            .styleClass('charm-legend-colorbar gg-legend-colorbar')
      }
    }
  }

  private void renderProportionalColorSteps(G group, ColorCharmScale cs, List<BigDecimal> breaks,
                                             int numBins, int x, int y, int barWidth, int barHeight,
                                             boolean vertical, boolean reverse) {
    BigDecimal totalRange = (breaks.last() - breaks.first()).abs()
    if (totalRange == 0) {
      // All breaks collapsed to the same value; fall back to even rendering
      renderEvenColorSteps(group, cs, breaks, numBins, x, y, barWidth, barHeight, vertical, reverse)
      return
    }
    int accumulated = 0

    for (int i = 0; i < numBins; i++) {
      BigDecimal lower = breaks[i]
      BigDecimal upper = breaks[i + 1]
      BigDecimal midpoint = (lower + upper) / 2
      BigDecimal binRange = (upper - lower).abs()
      String color = cs.colorFor(midpoint) ?: '#999999'

      if (vertical) {
        int stepHeight = (barHeight * (binRange / totalRange)) as int
        int stepY = reverse ? (y + accumulated) : (y + barHeight - accumulated - stepHeight)
        group.addRect(barWidth, stepHeight + 1)
            .x(x).y(stepY).fill(color).stroke('none')
            .styleClass('charm-legend-colorbar gg-legend-colorbar')
        accumulated += stepHeight
      } else {
        int stepWidth = (barWidth * (binRange / totalRange)) as int
        int stepX = x + accumulated
        group.addRect(stepWidth + 1, barHeight)
            .x(stepX).y(y).fill(color).stroke('none')
            .styleClass('charm-legend-colorbar gg-legend-colorbar')
        accumulated += stepWidth
      }
    }
  }

  // ---- Size Legend ----

  private int renderSizeLegend(G group, RenderContext context, int startY, boolean vertical) {
    CharmScale sizeScale = context.sizeScale
    if (sizeScale == null) {
      return startY
    }

    ElementText labelText = context.chart.theme.legendText
    BigDecimal labelSize = (labelText?.size ?: 10) as BigDecimal
    String labelColor = labelText?.color ?: '#333333'
    String keyColor = context.chart.theme.legendKey?.color ?: '#999999'

    int keySize = context.config.legendKeySize
    int spacing = context.config.legendSpacing
    int textOffset = keySize + 8
    BigDecimal maxRadius = keySize / 2.0

    int x = 0
    int y = startY

    if (sizeScale instanceof DiscreteCharmScale) {
      DiscreteCharmScale disc = sizeScale as DiscreteCharmScale
      disc.levels.each { String level ->
        BigDecimal centerX = x + keySize / 2.0
        BigDecimal centerY = y + keySize / 2.0
        BigDecimal radius = maxRadius / 2.0  // Default radius for discrete
        group.addCircle()
            .styleClass('charm-legend-key gg-legend-key')
            .cx(centerX).cy(centerY).r(radius.max(1.0))
            .fill(keyColor).stroke(keyColor)

        def labelEl = group.addText(level)
            .x(x + textOffset).y(y + keySize - 2)
            .fontSize(labelSize).fill(labelColor)
            .styleClass('charm-legend-label gg-legend-label')
        if (labelText?.family) labelEl.addAttribute('font-family', labelText.family)
        y += keySize + spacing
      }
    } else if (sizeScale instanceof ContinuousCharmScale) {
      ContinuousCharmScale cont = sizeScale as ContinuousCharmScale
      List<Object> ticks = cont.ticks(5)
      List<String> labels = cont.tickLabels(5)
      BigDecimal transformedMax = cont.rangeEnd ?: 10.0

      ticks.eachWithIndex { Object tickVal, int idx ->
        BigDecimal scaled = cont.transform(tickVal)
        if (scaled == null) return
        // transform() returns values in the scale range (e.g. 2..10); normalize using range max
        BigDecimal radius = transformedMax > 0 ? (scaled / transformedMax * maxRadius).max(1.0) : maxRadius / 2.0
        BigDecimal centerX = x + keySize / 2.0
        BigDecimal centerY = y + keySize / 2.0
        group.addCircle()
            .styleClass('charm-legend-key gg-legend-key')
            .cx(centerX).cy(centerY).r(radius)
            .fill(keyColor).stroke(keyColor)

        String label = idx < labels.size() ? labels[idx] : tickVal?.toString() ?: ''
        def labelEl = group.addText(label)
            .x(x + textOffset).y(y + keySize - 2)
            .fontSize(labelSize).fill(labelColor)
            .styleClass('charm-legend-label gg-legend-label')
        if (labelText?.family) labelEl.addAttribute('font-family', labelText.family)
        y += keySize + spacing
      }
    }

    y
  }

  // ---- Alpha Legend ----

  private int renderAlphaLegend(G group, RenderContext context, int startY, boolean vertical) {
    CharmScale alphaScale = context.alphaScale
    if (alphaScale == null) {
      return startY
    }

    ElementText labelText = context.chart.theme.legendText
    BigDecimal labelSize = (labelText?.size ?: 10) as BigDecimal
    String labelColor = labelText?.color ?: '#333333'
    String keyColor = context.chart.theme.legendKey?.color ?: '#999999'

    int keySize = context.config.legendKeySize
    int spacing = context.config.legendSpacing
    int textOffset = keySize + 8

    int x = 0
    int y = startY

    if (alphaScale instanceof DiscreteCharmScale) {
      DiscreteCharmScale disc = alphaScale as DiscreteCharmScale
      disc.levels.each { String level ->
        BigDecimal alphaVal = 0.5  // Default alpha for discrete levels
        def rect = group.addRect(keySize, keySize)
            .x(x).y(y).fill(keyColor)
            .styleClass('charm-legend-key gg-legend-key')
        rect.addAttribute('fill-opacity', alphaVal)

        def labelEl = group.addText(level)
            .x(x + textOffset).y(y + keySize - 2)
            .fontSize(labelSize).fill(labelColor)
            .styleClass('charm-legend-label gg-legend-label')
        if (labelText?.family) labelEl.addAttribute('font-family', labelText.family)
        y += keySize + spacing
      }
    } else if (alphaScale instanceof ContinuousCharmScale) {
      ContinuousCharmScale cont = alphaScale as ContinuousCharmScale
      List<Object> ticks = cont.ticks(5)
      List<String> labels = cont.tickLabels(5)

      ticks.eachWithIndex { Object tickVal, int idx ->
        BigDecimal scaled = cont.transform(tickVal)
        if (scaled == null) return
        // transform() already returns values in the alpha range (0.1..1.0); use directly
        BigDecimal alphaVal = scaled.min(1).max(0)
        def rect = group.addRect(keySize, keySize)
            .x(x).y(y).fill(keyColor)
            .styleClass('charm-legend-key gg-legend-key')
        rect.addAttribute('fill-opacity', alphaVal)

        String label = idx < labels.size() ? labels[idx] : tickVal?.toString() ?: ''
        def labelEl = group.addText(label)
            .x(x + textOffset).y(y + keySize - 2)
            .fontSize(labelSize).fill(labelColor)
            .styleClass('charm-legend-label gg-legend-label')
        if (labelText?.family) labelEl.addAttribute('font-family', labelText.family)
        y += keySize + spacing
      }
    }

    y
  }

  // ---- Continuous as Discrete ----

  private int renderContinuousAsDiscrete(G group, RenderContext context, String aesthetic,
                                          int startY, boolean vertical) {
    ColorCharmScale cs = aesthetic == 'fill' ? context.fillScale : context.colorScale
    if (cs == null || cs.domainMin == null || cs.domainMax == null) {
      return startY
    }

    ElementText labelText = context.chart.theme.legendText
    BigDecimal labelSize = (labelText?.size ?: 10) as BigDecimal
    String labelColor = labelText?.color ?: '#333333'

    int keySize = context.config.legendKeySize
    int spacing = context.config.legendSpacing
    int textOffset = keySize + 8

    List<Object> breakValues = []
    List<String> breakLabels = []
    List configuredBreaks = cs.scaleSpec?.breaks as List
    List configuredLabels = cs.scaleSpec?.labels as List

    if (configuredBreaks != null && !configuredBreaks.isEmpty()) {
      breakValues.addAll(configuredBreaks)
      if (configuredLabels != null && configuredLabels.size() == configuredBreaks.size()) {
        breakLabels.addAll(configuredLabels.collect { Object it -> it?.toString() ?: '' })
      }
    } else {
      int numBreaks = 5
      BigDecimal range = cs.domainMax - cs.domainMin
      for (int i = 0; i < numBreaks; i++) {
        breakValues << (cs.domainMin + range * i / (numBreaks - 1))
      }
    }
    if (breakLabels.isEmpty()) {
      breakLabels = (breakValues.collect { Object value ->
        if (value instanceof Number) {
          return formatNumber(value as BigDecimal)
        }
        value?.toString() ?: ''
      } as List<String>)
    }

    int x = 0
    int y = startY

    for (int i = 0; i < breakValues.size(); i++) {
      Object value = breakValues[i]
      String color = cs.colorFor(value) ?: '#999999'
      String label = i < breakLabels.size() ? breakLabels[i] : (value?.toString() ?: '')

      group.addRect(keySize, keySize)
          .x(x).y(y).fill(color)
          .stroke(context.chart.theme.legendKey?.color ?: '#666666')
          .styleClass('charm-legend-key gg-legend-key')

      def labelEl = group.addText(label)
          .x(x + textOffset).y(y + keySize - 2)
          .fontSize(labelSize).fill(labelColor)
          .styleClass('charm-legend-label gg-legend-label')
      if (labelText?.family) labelEl.addAttribute('font-family', labelText.family)

      if (vertical) {
        y += keySize + spacing
      } else {
        x += keySize + textOffset + label.length() * 6 + spacing
      }
    }

    vertical ? y : y + keySize + spacing
  }

  // ---- Custom Guide ----

  @CompileStatic(TypeCheckingMode.SKIP)
  private int renderCustomGuide(G parentGroup, RenderContext context, String aesthetic,
                                 int startY, GuideSpec spec) {
    Map<String, Object> params = spec.params ?: [:]
    int width = (params['width'] ?: 50) as int
    int height = (params['height'] ?: 50) as int
    int spacing = 10
    int currentY = startY

    // Render title if provided
    if (params['title']) {
      ElementText titleStyle = context.chart.theme.legendTitle
      BigDecimal titleSize = (titleStyle?.size ?: 11) as BigDecimal
      def titleText = parentGroup.addText(params['title'] as String)
          .x(0).y(currentY + titleSize)
          .addAttribute('font-weight', 'bold')
          .fontSize(titleSize)
          .styleClass('charm-legend-title gg-legend-title')
      if (titleStyle?.color) titleText.fill(titleStyle.color)
      currentY += (titleSize as int) + 5
    }

    G customGroup = parentGroup.addG().id("custom-guide-${aesthetic}")

    try {
      if (params['renderClosure']) {
        Closure closure = params['renderClosure'] as Closure
        // Build scales map for backward compatibility with gg custom guides
        Map<String, Object> scalesMap = [:]
        if (context.xScale) scalesMap['x'] = context.xScale
        if (context.yScale) scalesMap['y'] = context.yScale
        if (context.colorScale) scalesMap['color'] = context.colorScale
        if (context.fillScale) scalesMap['fill'] = context.fillScale
        if (context.sizeScale) scalesMap['size'] = context.sizeScale
        if (context.shapeScale) scalesMap['shape'] = context.shapeScale
        if (context.alphaScale) scalesMap['alpha'] = context.alphaScale
        if (context.linetypeScale) scalesMap['linetype'] = context.linetypeScale

        Map closureContext = [
            svg   : customGroup,
            x     : 0,
            y     : currentY,
            width : width,
            height: height,
            theme : context.chart.theme,
            scales: scalesMap
        ]
        closure.call(closureContext)
      }
    } catch (Exception e) {
      log.error("Custom guide rendering failed for aesthetic '$aesthetic': ${e.message}", e)

      customGroup.addRect(width, 20)
          .x(0).y(currentY)
          .fill('#ffcccc').stroke('#cc0000')
          .styleClass('charm-legend-error')
      customGroup.addText('Error rendering custom guide')
          .x(5).y(currentY + 14)
          .fontSize(9).fill('#cc0000')
          .styleClass('charm-legend-error-text')
    }

    currentY + height + spacing
  }

  // ---- Shape Drawing ----

  private static final List<String> DEFAULT_SHAPES = [
      'circle', 'square', 'triangle', 'diamond', 'plus', 'x', 'cross'
  ].asImmutable()

  /**
   * Resolves the actual shape name for a legend key from a discrete shape scale.
   *
   * <p>Uses the scale's explicit values mapping (Map or List) when available,
   * otherwise falls back to the default shape set by index.</p>
   *
   * @param shapeScale discrete shape scale
   * @param levelIndex index of the level in the scale's levels list
   * @return resolved shape name
   */
  private static String resolveShapeName(DiscreteCharmScale shapeScale, int levelIndex) {
    if (levelIndex < 0) {
      return 'circle'
    }
    Object values = shapeScale?.scaleSpec?.params?.values
    if (values instanceof Map) {
      String level = shapeScale.levels[levelIndex]
      Object mapped = (values as Map)[level]
      if (mapped != null) {
        return mapped.toString()
      }
    } else if (values instanceof List) {
      List listValues = values as List
      if (!listValues.isEmpty()) {
        return listValues[levelIndex % listValues.size()]?.toString() ?: 'circle'
      }
    }
    DEFAULT_SHAPES[levelIndex % DEFAULT_SHAPES.size()]
  }

  private void drawLegendShape(G group, Number centerX, Number centerY, int size,
                                String shape, String fillColor, String strokeColor) {
    String stroke = strokeColor ?: fillColor
    BigDecimal halfSize = 2.max(size - 2) / 2.0
    BigDecimal cx = centerX as BigDecimal
    BigDecimal cy = centerY as BigDecimal

    switch (shape?.toLowerCase()) {
      case 'square' -> {
        group.addRect((halfSize * 2).round(), (halfSize * 2).round())
            .styleClass('charm-legend-key gg-legend-key')
            .x((cx - halfSize).round())
            .y((cy - halfSize).round())
            .fill(fillColor).stroke(stroke)
      }
      case 'plus', 'cross' -> {
        group.addLine((cx - halfSize).round(), cy.round(), (cx + halfSize).round(), cy.round())
            .styleClass('charm-legend-key gg-legend-key').stroke(stroke)
        group.addLine(cx.round(), (cy - halfSize).round(), cx.round(), (cy + halfSize).round())
            .styleClass('charm-legend-key gg-legend-key').stroke(stroke)
      }
      case 'x' -> {
        group.addLine((cx - halfSize).round(), (cy - halfSize).round(),
            (cx + halfSize).round(), (cy + halfSize).round())
            .styleClass('charm-legend-key gg-legend-key').stroke(stroke)
        group.addLine((cx - halfSize).round(), (cy + halfSize).round(),
            (cx + halfSize).round(), (cy - halfSize).round())
            .styleClass('charm-legend-key gg-legend-key').stroke(stroke)
      }
      case 'triangle' -> {
        BigDecimal h = (halfSize * 2) * 3.sqrt() / 2
        BigDecimal topY = cy - h * 2 / 3
        BigDecimal bottomY = cy + h / 3
        BigDecimal leftX = cx - halfSize
        BigDecimal rightX = cx + halfSize
        String pathD = "M ${cx.round()} ${topY.round()} L ${leftX.round()} ${bottomY.round()} L ${rightX.round()} ${bottomY.round()} Z"
        group.addPath().d(pathD)
            .styleClass('charm-legend-key gg-legend-key')
            .fill(fillColor).stroke(stroke)
      }
      case 'diamond' -> {
        String diamond = "M ${cx.round()} ${(cy - halfSize).round()} " +
            "L ${(cx + halfSize).round()} ${cy.round()} " +
            "L ${cx.round()} ${(cy + halfSize).round()} " +
            "L ${(cx - halfSize).round()} ${cy.round()} Z"
        group.addPath().d(diamond)
            .styleClass('charm-legend-key gg-legend-key')
            .fill(fillColor).stroke(stroke)
      }
      default -> {
        group.addCircle()
            .styleClass('charm-legend-key gg-legend-key')
            .cx(centerX).cy(centerY)
            .r(halfSize.round())
            .fill(fillColor).stroke(stroke)
      }
    }
  }

  // ---- Per-layer scale adapters ----

  /** Resolves a ColorCharmScale from either a direct scale object or a context global scale. */
  private static ColorCharmScale resolveColorScaleObj(RenderContext context, String aesthetic, Object scaleObj) {
    if (scaleObj instanceof ColorCharmScale) {
      return scaleObj as ColorCharmScale
    }
    aesthetic == 'fill' ? context.fillScale : context.colorScale
  }

  /** Renders a discrete legend from an explicit color scale (for per-layer or global). */
  private int renderDiscreteLegendFromScale(G group, RenderContext context, String aesthetic,
                                              ColorCharmScale cs, int startY, boolean vertical,
                                              boolean usesPoints, DiscreteCharmScale shapeForKeys) {
    ElementText labelText = context.chart.theme.legendText
    BigDecimal defaultSize = (context.chart.theme.baseSize ?: 10) as BigDecimal
    String labelColor = labelText?.color ?: '#333333'
    BigDecimal labelSize = (labelText?.size ?: defaultSize) as BigDecimal
    int keySize = context.config.legendKeySize
    int spacing = context.config.legendSpacing
    int textOffset = keySize + 8
    List<String> levels = cs?.levels ?: []
    int y = startY
    int x = 0

    levels.each { String level ->
      String color = cs.colorFor(level)
      if (shapeForKeys != null) {
        int idx = shapeForKeys.levels.indexOf(level)
        String shapeName = resolveShapeName(shapeForKeys, idx)
        drawLegendShape(group, (x + keySize / 2).round(), (y + keySize / 2).round(), keySize, shapeName, color, '#666666')
      } else if (usesPoints) {
        BigDecimal radius = ((keySize - 2) / 2).round()
        group.addCircle().styleClass('charm-legend-key gg-legend-key')
            .cx((x + keySize / 2).round()).cy((y + keySize / 2).round())
            .r(radius).fill(color).stroke('#666666')
      } else {
        group.addRect(keySize, keySize).x(x).y(y).fill(color).stroke('#666666')
            .styleClass('charm-legend-key gg-legend-key')
      }
      def labelEl = group.addText(level).x(x + textOffset).y(y + keySize - 2)
          .fontSize(labelSize).fill(labelColor).styleClass('charm-legend-label gg-legend-label')
      if (labelText?.family) labelEl.addAttribute('font-family', labelText.family)
      if (vertical) { y += keySize + spacing } else { x += keySize + textOffset + level.length() * 6 + spacing }
    }
    vertical ? y : y + keySize + spacing
  }

  /** Renders a colorbar from an explicit scale object (for per-layer or global). */
  private int renderColorbarFromScale(G group, RenderContext context, String aesthetic,
                                        Object scaleObj, int startY, boolean vertical) {
    ColorCharmScale cs = resolveColorScaleObj(context, aesthetic, scaleObj)
    if (cs == null || cs.domainMin == null || cs.domainMax == null) {
      return startY
    }
    if ('fill' == aesthetic) {
      ColorCharmScale previous = context.fillScale
      try {
        context.fillScale = cs
        return renderColorbar(group, context, aesthetic, startY, vertical)
      } finally {
        context.fillScale = previous
      }
    }
    if ('color' == aesthetic || 'colour' == aesthetic) {
      ColorCharmScale previous = context.colorScale
      try {
        context.colorScale = cs
        return renderColorbar(group, context, aesthetic, startY, vertical)
      } finally {
        context.colorScale = previous
      }
    }
    renderColorbar(group, context, aesthetic, startY, vertical)
  }

  /** Renders color steps from an explicit scale object (for per-layer or global). */
  private int renderColorStepsFromScale(G group, RenderContext context, String aesthetic,
                                          Object scaleObj, int startY, boolean vertical, Map<String, Object> guideParams) {
    ColorCharmScale cs = resolveColorScaleObj(context, aesthetic, scaleObj)
    if (cs == null || cs.domainMin == null || cs.domainMax == null) {
      return startY
    }
    if ('fill' == aesthetic) {
      ColorCharmScale previous = context.fillScale
      try {
        context.fillScale = cs
        return renderColorSteps(group, context, aesthetic, startY, vertical, guideParams)
      } finally {
        context.fillScale = previous
      }
    }
    if ('color' == aesthetic || 'colour' == aesthetic) {
      ColorCharmScale previous = context.colorScale
      try {
        context.colorScale = cs
        return renderColorSteps(group, context, aesthetic, startY, vertical, guideParams)
      } finally {
        context.colorScale = previous
      }
    }
    renderColorSteps(group, context, aesthetic, startY, vertical, guideParams)
  }

  /** Renders a continuous-as-discrete legend from an explicit ColorCharmScale. */
  private int renderContinuousAsDiscreteFromScale(G group, RenderContext context, String aesthetic,
                                                     ColorCharmScale cs, int startY, boolean vertical) {
    if (cs == null) {
      return startY
    }
    if ('fill' == aesthetic) {
      ColorCharmScale previous = context.fillScale
      try {
        context.fillScale = cs
        return renderContinuousAsDiscrete(group, context, aesthetic, startY, vertical)
      } finally {
        context.fillScale = previous
      }
    }
    if ('color' == aesthetic || 'colour' == aesthetic) {
      ColorCharmScale previous = context.colorScale
      try {
        context.colorScale = cs
        return renderContinuousAsDiscrete(group, context, aesthetic, startY, vertical)
      } finally {
        context.colorScale = previous
      }
    }
    renderContinuousAsDiscrete(group, context, aesthetic, startY, vertical)
  }

  /** Renders a size legend from an explicit scale. */
  private int renderSizeLegendFromScale(G group, RenderContext context, CharmScale scale,
                                          int startY, boolean vertical) {
    if (scale == null) {
      return renderSizeLegend(group, context, startY, vertical)
    }
    CharmScale previous = context.sizeScale
    try {
      context.sizeScale = scale
      return renderSizeLegend(group, context, startY, vertical)
    } finally {
      context.sizeScale = previous
    }
  }

  /** Renders an alpha legend from an explicit scale. */
  private int renderAlphaLegendFromScale(G group, RenderContext context, CharmScale scale,
                                           int startY, boolean vertical) {
    if (scale == null) {
      return renderAlphaLegend(group, context, startY, vertical)
    }
    CharmScale previous = context.alphaScale
    try {
      context.alphaScale = scale
      return renderAlphaLegend(group, context, startY, vertical)
    } finally {
      context.alphaScale = previous
    }
  }

  /** Renders a shape legend from an explicit discrete scale when available. */
  private int renderShapeLegendFromScale(G group, RenderContext context, Object scaleObj,
                                          int startY, boolean vertical, boolean usesPoints) {
    if (!(scaleObj instanceof DiscreteCharmScale)) {
      return renderDiscreteLegend(group, context, 'shape', startY, vertical, usesPoints, null)
    }
    CharmScale previous = context.shapeScale
    try {
      context.shapeScale = scaleObj as CharmScale
      return renderDiscreteLegend(group, context, 'shape', startY, vertical, usesPoints, null)
    } finally {
      context.shapeScale = previous
    }
  }

  /** Resolves a per-layer legend title from the scale name param or a generated label. */
  private static String resolvePerLayerLegendTitle(String key, Object scaleObj) {
    if (scaleObj instanceof CharmScale) {
      CharmScale cs = scaleObj as CharmScale
      Object name = cs.scaleSpec?.params?.get('name')
      if (name instanceof CharSequence && !name.toString().isBlank()) {
        return name.toString()
      }
    }
    // Generate label: "color_layer1" -> "color (layer 2)"
    def match = key =~ /^(.+)_layer(\d+)$/
    if (match.matches()) {
      String aesthetic = match.group(1)
      int idx = Integer.parseInt(match.group(2))
      return "${aesthetic} (layer ${idx + 1})"
    }
    null
  }

  // ---- Helpers ----

  private Map<String, Object> collectLegendScales(RenderContext context, GuidesSpec guides) {
    Map<String, Object> result = [:]

    if (context.colorScale != null && !context.colorScale.levels.isEmpty()) {
      if (resolveGuideType('color', context, guides, context.colorScale) != GuideType.NONE) {
        result['color'] = context.colorScale
      }
    } else if (context.colorScale != null && context.colorScale.domainMin != null) {
      if (resolveGuideType('color', context, guides, context.colorScale) != GuideType.NONE) {
        result['color'] = context.colorScale
      }
    }

    if (context.fillScale != null && !context.fillScale.levels.isEmpty()) {
      if (resolveGuideType('fill', context, guides, context.fillScale) != GuideType.NONE) {
        result['fill'] = context.fillScale
      }
    } else if (context.fillScale != null && context.fillScale.domainMin != null) {
      if (resolveGuideType('fill', context, guides, context.fillScale) != GuideType.NONE) {
        result['fill'] = context.fillScale
      }
    }

    if (context.sizeScale != null) {
      if (resolveGuideType('size', context, guides, context.sizeScale) != GuideType.NONE) {
        result['size'] = context.sizeScale
      }
    }

    if (context.shapeScale != null) {
      if (resolveGuideType('shape', context, guides, context.shapeScale) != GuideType.NONE) {
        result['shape'] = context.shapeScale
      }
    }

    if (context.alphaScale != null) {
      if (resolveGuideType('alpha', context, guides, context.alphaScale) != GuideType.NONE) {
        result['alpha'] = context.alphaScale
      }
    }

    if (context.linetypeScale != null) {
      if (resolveGuideType('linetype', context, guides, context.linetypeScale) != GuideType.NONE) {
        result['linetype'] = context.linetypeScale
      }
    }

    // Per-layer scale overrides produce additional legend entries
    context.layerScales.each { Integer idx, TrainedScales layerTrained ->
      addPerLayerLegendEntry(result, context, guides, 'color', idx, layerTrained.color)
      addPerLayerLegendEntry(result, context, guides, 'fill', idx, layerTrained.fill as CharmScale)
      addPerLayerLegendEntry(result, context, guides, 'size', idx, layerTrained.size)
      addPerLayerLegendEntry(result, context, guides, 'shape', idx, layerTrained.shape)
      addPerLayerLegendEntry(result, context, guides, 'alpha', idx, layerTrained.alpha)
      addPerLayerLegendEntry(result, context, guides, 'linetype', idx, layerTrained.linetype)
    }

    result
  }

  private void addPerLayerLegendEntry(Map<String, Object> result, RenderContext context, GuidesSpec guides,
                                        String aesthetic, int layerIdx, CharmScale scale) {
    if (scale == null) {
      return
    }
    if (resolveGuideType(aesthetic, context, guides, scale) == GuideType.NONE) {
      return
    }
    boolean hasContent = false
    if (scale instanceof ColorCharmScale) {
      ColorCharmScale cs = scale as ColorCharmScale
      hasContent = !cs.levels.isEmpty() || cs.domainMin != null
    } else if (scale instanceof DiscreteCharmScale) {
      hasContent = !(scale as DiscreteCharmScale).levels.isEmpty()
    } else if (scale instanceof ContinuousCharmScale) {
      hasContent = true
    }
    if (hasContent) {
      String key = "${aesthetic}_layer${layerIdx}"
      result[key] = scale
    }
  }

  private GuideType resolveGuideType(String aesthetic, RenderContext context, GuidesSpec guides, Object scaleObj = null) {
    // Check explicit guide spec
    GuideSpec spec = guides?.getSpec(aesthetic)
    if (spec != null) {
      return spec.type
    }

    GuideSpec scaleGuide = guideSpecFromScale(scaleObj)
    if (scaleGuide != null) {
      return scaleGuide.type
    }

    // Default: discrete scales -> LEGEND, continuous color -> COLORBAR
    if (isColorAesthetic(aesthetic)) {
      ColorCharmScale cs = resolveColorScaleObj(context, aesthetic, scaleObj)
      if (cs == null) {
        return GuideType.NONE
      }
      if (cs != null && !cs.levels.isEmpty()) {
        return GuideType.LEGEND
      }
      if (cs.domainMin == null || cs.domainMax == null) {
        return GuideType.NONE
      }
      return GuideType.COLORBAR
    }

    if (scaleObj instanceof DiscreteCharmScale || scaleObj instanceof ContinuousCharmScale) {
      return GuideType.LEGEND
    }

    GuideType.LEGEND
  }

  private static Map<String, Object> resolveGuideParams(String aesthetic, GuidesSpec guides, Object scaleObj) {
    GuideSpec explicit = guides?.getSpec(aesthetic)
    if (explicit != null) {
      return explicit.params ?: [:]
    }
    GuideSpec fromScale = guideSpecFromScale(scaleObj)
    fromScale?.params ?: [:]
  }

  private static GuideSpec guideSpecFromScale(Object scaleObj) {
    if (!(scaleObj instanceof CharmScale)) {
      return null
    }
    Object guide = (scaleObj as CharmScale).scaleSpec?.params?.get('guide')
    toGuideSpec(guide)
  }

  private static GuideSpec toGuideSpec(Object guide) {
    if (guide == null) {
      return null
    }
    if (guide instanceof GuideSpec) {
      return guide as GuideSpec
    }
    if (guide instanceof GuideType) {
      return new GuideSpec(guide as GuideType)
    }
    if (guide == false || guide == Boolean.FALSE) {
      return GuideSpec.none()
    }
    if (guide instanceof CharSequence) {
      GuideType type = GuideType.fromString(guide.toString())
      if (type != null) {
        return new GuideSpec(type)
      }
      return null
    }
    if (guide instanceof Map) {
      Map guideMap = guide as Map
      Object typeValue = guideMap['type']
      GuideType type = null
      if (typeValue instanceof GuideType) {
        type = typeValue as GuideType
      } else if (typeValue != null) {
        type = GuideType.fromString(typeValue.toString())
      }
      if (type == null) {
        return null
      }
      Map<String, Object> params
      if (guideMap['params'] instanceof Map) {
        params = new LinkedHashMap<>(guideMap['params'] as Map<String, Object>)
      } else {
        params = (guideMap.findAll { Object key, Object value -> key != 'type' } as Map<String, Object>)
      }
      return new GuideSpec(type, params ?: [:])
    }
    null
  }

  private String resolveLegendTitle(RenderContext context, Map<String, Object> legendScales) {
    // Priority: labels.guides map -> first aesthetic name
    Map<String, String> guideLabels = context.chart.labels?.guides
    if (guideLabels) {
      // Return first matching guide label
      for (String aesthetic : legendScales.keySet()) {
        String title = guideLabels[aesthetic]
        if (title) {
          return title
        }
      }
    }

    // Next priority: explicit scale name from scale params.
    for (Object scaleObj : legendScales.values()) {
      if (!(scaleObj instanceof se.alipsa.matrix.charm.render.scale.CharmScale)) {
        continue
      }
      def scale = scaleObj as se.alipsa.matrix.charm.render.scale.CharmScale
      Object name = scale.scaleSpec?.params?.get('name')
      if (name instanceof CharSequence && !name.toString().isBlank()) {
        return name.toString()
      }
    }

    // Fallback to aesthetic name
    if (!legendScales.isEmpty()) {
      return legendScales.keySet().first()
    }

    null
  }

  private static boolean sameDiscreteLevels(DiscreteCharmScale left, DiscreteCharmScale right) {
    if (left == null || right == null) return false
    left.levels == right.levels
  }

  private static DiscreteCharmScale resolveColorDiscrete(RenderContext context) {
    if (context.colorScale instanceof ColorCharmScale) {
      ColorCharmScale cs = context.colorScale as ColorCharmScale
      if (!cs.levels.isEmpty()) {
        // Wrap levels into a proxy DiscreteCharmScale for comparison
        return new DiscreteCharmScale(levels: cs.levels)
      }
    }
    null
  }

  private static boolean isColorAesthetic(String aesthetic) {
    aesthetic == 'color' || aesthetic == 'colour' || aesthetic == 'fill'
  }

  /**
   * Formats a number for display in legends.
   * Whole numbers display without decimals; fractional values display with up to 2 decimal places
   * (trailing zeros stripped for consistency, e.g. 1.50 becomes "1.5").
   */
  private static String formatNumber(BigDecimal value) {
    if (value == null) return ''
    if (value == value.setScale(0, java.math.RoundingMode.HALF_UP)) {
      value.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString()
    } else {
      value.setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }
  }
}
