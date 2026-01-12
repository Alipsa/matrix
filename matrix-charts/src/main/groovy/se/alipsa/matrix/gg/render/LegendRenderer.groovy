package se.alipsa.matrix.gg.render

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.Guide
import se.alipsa.matrix.gg.Guides
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleColorGradient
import se.alipsa.matrix.gg.scale.ScaleColorViridisC
import se.alipsa.matrix.gg.scale.ScaleContinuous
import se.alipsa.matrix.gg.scale.ScaleDiscrete
import se.alipsa.matrix.gg.theme.Theme

/**
 * Utility class for rendering legends and guides in ggplot charts.
 * Handles discrete legends, continuous legends, color steps, size, alpha, and custom guides.
 */
@CompileStatic
class LegendRenderer {

  private final RenderContext context

  // Constants
  private static final int LEGEND_PLOT_GAP = 20
  private static final int LEGEND_TITLE_SPACING = 10
  private static final int LEGEND_CONTINUOUS_BAR_WIDTH_VERTICAL = 15
  private static final int LEGEND_CONTINUOUS_BAR_HEIGHT_VERTICAL = 100
  private static final int LEGEND_CONTINUOUS_BAR_WIDTH_HORIZONTAL = 100
  private static final int LEGEND_CONTINUOUS_BAR_HEIGHT_HORIZONTAL = 15
  private static final int MARGIN_LEFT = 60
  private static final int MARGIN_RIGHT = 60
  private static final int MARGIN_TOP = 40
  private static final int MARGIN_BOTTOM = 50

  LegendRenderer(RenderContext context) {
    this.context = context
  }

  /**
   * Render legend for color, fill, and other aesthetic scales.
   */
  void renderLegend(Svg svg, Map<String, Scale> scales, GgChart chart, Theme theme) {
    // Check if legend should be shown
    if (theme.legendPosition == 'none') return

    // Find scales that need legends (color, fill, size - not x, y)
    List<String> legendAesthetics = ['color', 'colour', 'fill', 'size', 'shape', 'alpha', 'linetype']
    Map<String, Scale> legendScales = scales.findAll { k, v ->
      if (!legendAesthetics.contains(k) || !v.isTrained()) return false
      // Check appropriate domain field based on scale type
      if (v instanceof ScaleContinuous) {
        return !(v as ScaleContinuous).computedDomain.isEmpty()
      } else if (v instanceof ScaleDiscrete) {
        return !(v as ScaleDiscrete).levels.isEmpty()
      }
      return !v.domain.isEmpty()
    }

    Map<String, String> guideTypes = [:]
    legendScales = legendScales.findAll { aesthetic, scale ->
      String guideType = resolveGuideType(aesthetic, scale, chart.guides)
      if (guideType == 'none') {
        return false
      }
      guideTypes[aesthetic] = guideType
      return true
    }

    // Check if there are any custom guides to render
    boolean hasCustomGuides = false
    if (chart.guides != null) {
      hasCustomGuides = chart.guides.specs.any { aesthetic, guide ->
        guide instanceof Guide && guide.type == 'custom'
      }
    }

    // Only return early if there are no legend scales AND no custom guides
    if (legendScales.isEmpty() && !hasCustomGuides) return

    ScaleDiscrete shapeScale = legendScales['shape'] instanceof ScaleDiscrete ?
        (legendScales['shape'] as ScaleDiscrete) : null
    ScaleDiscrete colorScale = legendScales['color'] instanceof ScaleDiscrete ?
        (legendScales['color'] as ScaleDiscrete) :
        (legendScales['colour'] instanceof ScaleDiscrete ? legendScales['colour'] as ScaleDiscrete : null)
    if (colorScale == null && legendScales['fill'] instanceof ScaleDiscrete) {
      colorScale = legendScales['fill'] as ScaleDiscrete
    }
    boolean mergeShapeIntoColor = shapeScale != null && colorScale != null && sameDiscreteLevels(shapeScale, colorScale)
    if (mergeShapeIntoColor) {
      legendScales = legendScales.findAll { key, value -> key != 'shape' }
    }

    // Calculate legend position
    int plotWidth = chart.width - MARGIN_LEFT - MARGIN_RIGHT
    int plotHeight = chart.height - MARGIN_TOP - MARGIN_BOTTOM

    int legendX, legendY
    boolean isVertical = theme.legendDirection != 'horizontal'

    // Determine legend position
    switch (theme.legendPosition) {
      case 'right' -> {
        legendX = chart.width - MARGIN_RIGHT + LEGEND_PLOT_GAP
        legendY = MARGIN_TOP + 20
      }
      case 'left' -> {
        legendX = 10
        legendY = MARGIN_TOP + 20
      }
      case 'top' -> {
        legendX = MARGIN_LEFT
        legendY = 10
        isVertical = false
      }
      case 'bottom' -> {
        legendX = MARGIN_LEFT
        legendY = chart.height - 40
        isVertical = false
      }
      default -> {
        // Custom position [x, y] or default to right
        if (theme.legendPosition instanceof List && (theme.legendPosition as List).size() >= 2) {
          List pos = theme.legendPosition as List
          legendX = (pos[0] as Number).intValue()
          legendY = (pos[1] as Number).intValue()
        } else {
          legendX = chart.width - MARGIN_RIGHT + LEGEND_PLOT_GAP
          legendY = MARGIN_TOP + 20
        }
      }
    }

    // Create legend group
    G legendGroup = svg.addG()
    legendGroup.id('legend')
    legendGroup.transform("translate($legendX, $legendY)")

    // Get legend title from chart labels or scale name
    String legendTitle = determineLegendTitle(chart, legendScales)

    int currentY = 0
    int currentX = 0

    // Render title if present
    if (legendTitle) {
      int titleFontSize = (theme.legendTitle?.size ?: 11) as int
      def titleText = legendGroup.addText(legendTitle)
          .x(0)
          .y(titleFontSize)  // Position baseline at font size so text is visible
          .addAttribute('font-weight', 'bold')
          .fontSize(titleFontSize)
      if (theme.legendTitle?.color) {
        titleText.fill(theme.legendTitle.color)
      }
      currentY = titleFontSize + LEGEND_TITLE_SPACING
    }

    // Determine if primary geom uses points (for legend key shape)
    boolean usesPoints = chart.layers.any { layer ->
      layer.geom instanceof se.alipsa.matrix.gg.geom.GeomPoint
    }

    // Render each legend scale
    legendScales.each { aesthetic, scale ->
      String guideType = guideTypes[aesthetic]
      if (aesthetic == 'size') {
        currentY = renderSizeLegend(legendGroup, scale, currentX, currentY, isVertical, theme)
      } else if (aesthetic == 'alpha') {
        currentY = renderAlphaLegend(legendGroup, scale, currentX, currentY, isVertical, theme)
      } else if (scale instanceof ScaleDiscrete) {
        ScaleDiscrete shapeForColorKeys = mergeShapeIntoColor &&
            (aesthetic == 'color' || aesthetic == 'colour' || aesthetic == 'fill') ? shapeScale : null
        ScaleDiscrete colorForShapes = (!mergeShapeIntoColor && aesthetic == 'shape' &&
            colorScale != null && sameDiscreteLevels(scale as ScaleDiscrete, colorScale)) ? colorScale : null
        currentY = renderDiscreteLegend(legendGroup, scale as ScaleDiscrete, aesthetic,
            currentX, currentY, isVertical, theme, usesPoints, shapeForColorKeys, colorForShapes)
      } else if (scale instanceof ScaleContinuous) {
        if (guideType == 'legend') {
          currentY = renderContinuousLegendAsDiscrete(legendGroup, scale as ScaleContinuous, aesthetic,
              currentX, currentY, isVertical, theme)
        } else if (guideType == 'coloursteps' || guideType == 'colorsteps') {
          Map guideParams = context.extractGuideParams(scale?.guide)
          currentY = renderColorStepsLegend(legendGroup, scale as ScaleContinuous, aesthetic,
              currentX, currentY, isVertical, theme, guideParams)
        } else {
          currentY = renderContinuousLegend(legendGroup, scale as ScaleContinuous, aesthetic,
              currentX, currentY, isVertical, theme)
        }
      }
    }

    // Render custom guides if specified
    if (chart.guides != null) {
      chart.guides.specs.each { aesthetic, guide ->
        if (guide instanceof Guide && guide.type == 'custom') {
          currentY = renderCustomGuide(legendGroup, guide, aesthetic, currentX, currentY, scales, theme)
        }
      }
    }
  }

  /**
   * Determine the legend title from chart labels or scale name.
   */
  @CompileStatic
  private String determineLegendTitle(GgChart chart, Map<String, Scale> legendScales) {
    if (chart.labels?.legendTitle) {
      return chart.labels.legendTitle
    }
    if (!legendScales.isEmpty()) {
      return legendScales.values().first()?.name
    }
    return null
  }

  /**
   * Render a custom guide with user-defined closure.
   */
  @CompileStatic(TypeCheckingMode.SKIP)
  private int renderCustomGuide(G parentGroup, Guide customGuide, String aesthetic, int startX, int startY,
                                Map<String, Scale> scales, Theme theme) {
    Map params = customGuide.params ?: [:]

    // Allocate dimensions
    int width = (params.width != null ? params.width : 50) as int
    int height = (params.height != null ? params.height : 50) as int
    int spacing = 10

    int currentY = startY

    // Render title if provided
    if (params.title) {
      int titleFontSize = (theme.legendTitle?.size ?: 11) as int
      def titleText = parentGroup.addText(params.title as String)
          .x(startX)
          .y(currentY + titleFontSize)
          .addAttribute('font-weight', 'bold')
          .fontSize(titleFontSize)
      if (theme.legendTitle?.color) {
        titleText.fill(theme.legendTitle.color)
      }
      currentY += titleFontSize + 5
    }

    // Create group for custom content
    G customGroup = parentGroup.addG()
    customGroup.id("custom-guide-${aesthetic}")

    try {
      if (params.renderClosure) {
        Closure closure = params.renderClosure as Closure

        // Build context map for closure
        Map closureContext = [
          svg: customGroup,
          x: startX,
          y: currentY,
          width: width,
          height: height,
          theme: theme,
          scales: scales
        ]

        // Call user's closure - return value ignored as closure adds elements to context.svg
        closure.call(closureContext)
      }
    } catch (Exception e) {
      // Log error and render placeholder
      println "Warning: Custom guide rendering failed: ${e.message}"
      e.printStackTrace()

      // Render error placeholder
      customGroup.addRect(width, 20)
                 .x(startX)
                 .y(currentY)
                 .fill('#ffcccc')
                 .stroke('#cc0000')

      customGroup.addText("Error rendering custom guide")
                 .x(startX + 5)
                 .y(currentY + 14)
                 .fontSize(9)
                 .fill('#cc0000')
    }

    return currentY + height + spacing
  }

  /**
   * Render legend for a discrete scale (color categories).
   * @param usesPoints If true, draw circles instead of rectangles for color/fill keys
   */
  private int renderDiscreteLegend(G group, ScaleDiscrete scale, String aesthetic,
                                   int startX, int startY, boolean vertical, Theme theme,
                                   boolean usesPoints = false, ScaleDiscrete shapeScale = null,
                                   ScaleDiscrete colorScale = null) {
    List<Number> keySize = theme.legendKeySize ?: [15, 15] as List<Number>
    int keyWidth = keySize[0].intValue()
    int keyHeight = keySize[1].intValue()
    int spacing = 5
    int textOffset = keyWidth + 8

    List<Object> levels = scale.levels
    List<String> labels = scale.computedLabels

    int x = startX
    int y = startY

    boolean isColorKey = (aesthetic == 'color' || aesthetic == 'colour' || aesthetic == 'fill')
    boolean isShapeKey = (aesthetic == 'shape')

    levels.eachWithIndex { level, int idx ->
      // Get color for this level (for color/fill keys)
      String color = scale.transform(level)?.toString() ?: '#999999'
      String label = idx < labels.size() ? labels[idx] : level?.toString() ?: ''

      // Draw key (circle for points, rectangle otherwise)
      if (isColorKey) {
        if (usesPoints) {
          String shape = shapeScale?.transform(level)?.toString()
          if (shape) {
            BigDecimal centerX = (x + keyWidth / 2).round()
            BigDecimal centerY = (y + keyHeight / 2).round()
            drawLegendShape(group, centerX, centerY,
                Math.min(keyWidth, keyHeight), shape, color, theme.legendKey?.color)
          } else {
            // Draw circle for point geoms
            BigDecimal radius = (Math.min(keyWidth, keyHeight) / 2).round()
            def circle = group.addCircle()
                .cx((x + keyWidth / 2).round())
                .cy((y + keyHeight / 2).round())
                .r(radius - 1)  // Slightly smaller to fit in key area
                .fill(color)
            if (theme.legendKey?.color) {
              circle.stroke(theme.legendKey.color)
            }
          }
        } else {
          // Draw rectangle for bar/area geoms
          def rect = group.addRect(keyWidth, keyHeight)
              .x(x)
              .y(y)
              .fill(color)
          if (theme.legendKey?.color) {
            rect.stroke(theme.legendKey.color)
          }
        }
      } else if (isShapeKey) {
        String shape = scale.transform(level)?.toString() ?: 'circle'
        String shapeColor = colorScale?.transform(level)?.toString() ?: (theme.legendKey?.color ?: 'black')
        BigDecimal centerX = (x + keyWidth / 2).round()
        BigDecimal centerY = (y + keyHeight / 2).round()
        drawLegendShape(group, centerX, centerY,
            Math.min(keyWidth, keyHeight), shape, shapeColor, theme.legendKey?.color)
      }

      // Draw label
      group.addText(label)
          .x(x + textOffset)
          .y(y + keyHeight - 3)
          .fontSize(theme.legendText?.size ?: 10)
          .fill(theme.legendText?.color ?: 'black')

      // Move to next position
      if (vertical) {
        y += keyHeight + spacing
      } else {
        x += keyWidth + textOffset + label.length() * 6 + spacing
      }
    }

    return y + (vertical ? 0 : keyHeight + spacing)
  }

  /**
   * Render legend for a continuous scale (color gradient bar).
   */
  private int renderContinuousLegend(G group, ScaleContinuous scale, String aesthetic,
                                     int startX, int startY, boolean vertical, Theme theme) {
    // For continuous scales, render a gradient color bar with ggplot2-like defaults.
    int barWidth = vertical ? LEGEND_CONTINUOUS_BAR_WIDTH_VERTICAL : LEGEND_CONTINUOUS_BAR_WIDTH_HORIZONTAL
    int barHeight = vertical ? LEGEND_CONTINUOUS_BAR_HEIGHT_VERTICAL : LEGEND_CONTINUOUS_BAR_HEIGHT_HORIZONTAL
    int spacing = 5

    int x = startX
    int y = startY

    // Get domain for labels
    List domain = scale.computedDomain
    if (domain.size() < 2) return y

    Number minVal = domain[0] as Number
    Number maxVal = domain[1] as Number

    // Create gradient definition
    String gradientId = "legend-gradient-${aesthetic}"

    // We can't easily add to existing defs, so create a simple representation
    // Draw multiple small rectangles to simulate gradient
    int numSteps = 20
    for (int i = 0; i < numSteps; i++) {
      double t = i / (numSteps - 1)
      Number value = minVal + t * (maxVal - minVal)
      String color = scale.transform(value)?.toString() ?: '#999999'

      if (vertical) {
        int stepHeight = (int) (barHeight / numSteps)
        int stepY = y + barHeight - (i + 1) * stepHeight
        group.addRect(barWidth, stepHeight + 1)
            .x(x)
            .y(stepY)
            .fill(color)
            .stroke('none')
      } else {
        int stepWidth = (int) (barWidth / numSteps)
        int stepX = x + i * stepWidth
        group.addRect(stepWidth + 1, barHeight)
            .x(stepX)
            .y(y)
            .fill(color)
            .stroke('none')
      }
    }

    // Draw border around the bar
    group.addRect(barWidth, barHeight)
        .x(x)
        .y(y)
        .fill('none')
        .stroke(theme.legendKey?.color ?: '#333333')

    // Draw min/max labels
    List<Number> breaks = scale.computedBreaks as List<Number>
    List<String> labels = scale.computedLabels

    if (vertical) {
      // Min at bottom
      group.addText(labels.first() ?: context.formatNumber(minVal))
          .x(x + barWidth + 5)
          .y(y + barHeight)
          .fontSize(theme.legendText?.size ?: 9)
          .fill(theme.legendText?.color ?: 'black')

      // Max at top
      group.addText(labels.last() ?: context.formatNumber(maxVal))
          .x(x + barWidth + 5)
          .y(y + 10)
          .fontSize(theme.legendText?.size ?: 9)
          .fill(theme.legendText?.color ?: 'black')

      return y + barHeight + spacing
    } else {
      // Min at left
      group.addText(labels.first() ?: context.formatNumber(minVal))
          .x(x)
          .y(y + barHeight + 12)
          .fontSize(theme.legendText?.size ?: 9)
          .fill(theme.legendText?.color ?: 'black')

      // Max at right
      group.addText(labels.last() ?: context.formatNumber(maxVal))
          .x(x + barWidth - 20)
          .y(y + barHeight + 12)
          .fontSize(theme.legendText?.size ?: 9)
          .fill(theme.legendText?.color ?: 'black')

      return y + barHeight + 20
    }
  }

  /**
   * Helper method to render even-sized color steps.
   * Extracted to avoid duplication between evenSteps and totalRange==0 fallback.
   */
  @CompileStatic
  private void renderEvenColorSteps(G group, ScaleContinuous scale, List<Number> displayBreaks,
                                    int numBins, int x, int y, int barWidth, int barHeight,
                                    boolean vertical, boolean reverse) {
    for (int i = 0; i < numBins; i++) {
      BigDecimal lower = displayBreaks[i] as BigDecimal
      BigDecimal upper = displayBreaks[i + 1] as BigDecimal
      BigDecimal midpoint = (lower + upper) / 2
      String color = scale.transform(midpoint)?.toString() ?: '#999999'

      if (vertical) {
        BigDecimal binHeight = barHeight / numBins
        int stepHeight = binHeight.intValue()
        int stepY = reverse ? (y + i * stepHeight) : (y + barHeight - (i + 1) * stepHeight)

        group.addRect(barWidth, stepHeight + 1)
            .x(x)
            .y(stepY)
            .fill(color)
            .stroke('none')
      } else {
        BigDecimal binWidth = barWidth / numBins
        int stepWidth = binWidth.intValue()
        int stepX = x + i * stepWidth

        group.addRect(stepWidth + 1, barHeight)
            .x(stepX)
            .y(y)
            .fill(color)
            .stroke('none')
      }
    }
  }

  /**
   * Render stepped color legend for binned continuous scales.
   * Displays discrete color blocks instead of a smooth gradient.
   */
  @CompileStatic
  private int renderColorStepsLegend(G group, ScaleContinuous scale, String aesthetic,
                                     int startX, int startY, boolean vertical, Theme theme,
                                     Map guideParams) {
    // Extract parameters
    boolean evenSteps = (guideParams['even.steps'] != null ? guideParams['even.steps'] :
                         guideParams.evenSteps != null ? guideParams.evenSteps : true) as boolean
    Boolean showLimits = guideParams['show.limits'] != null ? guideParams['show.limits'] as Boolean :
                         guideParams.showLimits as Boolean
    boolean reverse = (guideParams.reverse ?: false) as boolean

    // Bar dimensions
    int barWidth = vertical ? LEGEND_CONTINUOUS_BAR_WIDTH_VERTICAL : LEGEND_CONTINUOUS_BAR_WIDTH_HORIZONTAL
    int barHeight = vertical ? LEGEND_CONTINUOUS_BAR_HEIGHT_VERTICAL : LEGEND_CONTINUOUS_BAR_HEIGHT_HORIZONTAL

    // Override with custom dimensions if provided
    if (guideParams.barwidth != null) {
      barWidth = guideParams.barwidth as int
    }
    if (guideParams.barheight != null) {
      barHeight = guideParams.barheight as int
    }

    int spacing = 5
    int x = startX
    int y = startY

    // Get breaks from scale
    List breaks = scale.computedBreaks
    if (breaks == null || breaks.size() < 2) return y

    List<String> labels = scale.computedLabels ?: []
    int numBins = breaks.size() - 1

    // Reverse breaks and labels if requested to maintain correspondence
    List<Number> displayBreaks = reverse ? breaks.reverse(false) as List<Number> : breaks as List<Number>
    List<String> displayLabels = reverse ? labels.reverse(false) as List<String> : labels

    // Calculate bin positions and colors
    if (evenSteps) {
      // Equal visual size for all bins
      renderEvenColorSteps(group, scale, displayBreaks, numBins, x, y, barWidth, barHeight, vertical, reverse)
    } else {
      // Proportional to data range
      BigDecimal minVal = displayBreaks[0] as BigDecimal
      BigDecimal maxVal = displayBreaks.last() as BigDecimal
      BigDecimal totalRange = maxVal - minVal

      // Defensive check for zero range (all breaks have same value)
      if (totalRange == 0) {
        // Fall back to even steps rendering
        renderEvenColorSteps(group, scale, displayBreaks, numBins, x, y, barWidth, barHeight, vertical, reverse)
      } else {

        int accumulatedHeight = 0
        int accumulatedWidth = 0

        for (int i = 0; i < numBins; i++) {
          BigDecimal lower = displayBreaks[i] as BigDecimal
          BigDecimal upper = displayBreaks[i + 1] as BigDecimal
          BigDecimal midpoint = (lower + upper) / 2
          BigDecimal binDataRange = upper - lower
          String color = scale.transform(midpoint)?.toString() ?: '#999999'

          if (vertical) {
            BigDecimal binHeight = barHeight * (binDataRange / totalRange)
            int stepHeight = binHeight.intValue()
            int stepY = reverse ? (y + accumulatedHeight) : (y + barHeight - accumulatedHeight - stepHeight)

            group.addRect(barWidth, stepHeight + 1)
                .x(x)
                .y(stepY)
                .fill(color)
                .stroke('none')

            accumulatedHeight += stepHeight
          } else {
            BigDecimal binWidth = barWidth * (binDataRange / totalRange)
            int stepWidth = binWidth.intValue()
            int stepX = x + accumulatedWidth

            group.addRect(stepWidth + 1, barHeight)
                .x(stepX)
                .y(y)
                .fill(color)
                .stroke('none')

            accumulatedWidth += stepWidth
          }
        }
      } // end else (totalRange != 0)
    }

    // Draw border around the bar
    group.addRect(barWidth, barHeight)
        .x(x)
        .y(y)
        .fill('none')
        .stroke(theme.legendKey?.color ?: '#333333')

    // Draw labels
    if (showLimits || showLimits == null) {
      // Show min/max labels by default
      if (vertical) {
        // Min at bottom
        group.addText(!displayLabels.isEmpty() ? displayLabels.first() : context.formatNumber(displayBreaks.first()))
            .x(x + barWidth + 5)
            .y(y + barHeight)
            .fontSize(theme.legendText?.size ?: 9)
            .fill(theme.legendText?.color ?: 'black')

        // Max at top
        group.addText(!displayLabels.isEmpty() ? displayLabels.last() : context.formatNumber(displayBreaks.last()))
            .x(x + barWidth + 5)
            .y(y + 10)
            .fontSize(theme.legendText?.size ?: 9)
            .fill(theme.legendText?.color ?: 'black')

        return y + barHeight + spacing
      } else {
        // Min at left
        group.addText(!displayLabels.isEmpty() ? displayLabels.first() : context.formatNumber(displayBreaks.first()))
            .x(x)
            .y(y + barHeight + 12)
            .fontSize(theme.legendText?.size ?: 9)
            .fill(theme.legendText?.color ?: 'black')

        // Max at right
        group.addText(!displayLabels.isEmpty() ? displayLabels.last() : context.formatNumber(displayBreaks.last()))
            .x(x + barWidth - 20)
            .y(y + barHeight + 12)
            .fontSize(theme.legendText?.size ?: 9)
            .fill(theme.legendText?.color ?: 'black')

        return y + barHeight + 20
      }
    } else {
      // No labels
      return y + barHeight + spacing
    }
  }

  /**
   * Render legend for a continuous scale using discrete keys.
   */
  private int renderContinuousLegendAsDiscrete(G group, ScaleContinuous scale, String aesthetic,
                                               int startX, int startY, boolean vertical, Theme theme) {
    List<Number> breaks = scale.computedBreaks as List<Number>
    if (breaks == null || breaks.isEmpty()) return startY

    List<String> labels = scale.computedLabels ?: []
    List<Number> keySize = theme.legendKeySize ?: [15, 15] as List<Number>
    int keyWidth = keySize[0].intValue()
    int keyHeight = keySize[1].intValue()
    int spacing = 5
    int textOffset = keyWidth + 8

    int x = startX
    int y = startY

    breaks.eachWithIndex { Number breakVal, int idx ->
      String color = scale.transform(breakVal)?.toString() ?: '#999999'
      String label = idx < labels.size() ? labels[idx] : context.formatNumber(breakVal)

      def rect = group.addRect(keyWidth, keyHeight)
          .x(x)
          .y(y)
          .fill(color)
      if (theme.legendKey?.color) {
        rect.stroke(theme.legendKey.color)
      }

      group.addText(label ?: '')
          .x(x + textOffset)
          .y(y + keyHeight - 3)
          .fontSize(theme.legendText?.size ?: 10)
          .fill(theme.legendText?.color ?: 'black')

      if (vertical) {
        y += keyHeight + spacing
      } else {
        x += keyWidth + textOffset + (label?.length() ?: 0) * 6 + spacing
      }
    }

    return y + (vertical ? 0 : keyHeight + spacing)
  }

  /**
   * Render legend for size aesthetic.
   */
  private int renderSizeLegend(G group, Scale scale, int startX, int startY, boolean vertical, Theme theme) {
    List values
    List<String> labels
    if (scale instanceof ScaleDiscrete) {
      values = (scale as ScaleDiscrete).levels
      labels = (scale as ScaleDiscrete).computedLabels ?: []
    } else if (scale instanceof ScaleContinuous) {
      values = (scale as ScaleContinuous).computedBreaks
      labels = (scale as ScaleContinuous).computedLabels ?: []
    } else {
      return startY
    }
    if (values == null || values.isEmpty()) return startY

    List<Number> keySize = theme.legendKeySize ?: [15, 15] as List<Number>
    int keyWidth = keySize[0].intValue()
    int keyHeight = keySize[1].intValue()
    int spacing = 5
    int textOffset = keyWidth + 8

    List<Number> sizes = values.collect { val ->
      def scaled = scale.transform(val)
      scaled instanceof Number ? (scaled as Number) : null
    }.findAll { it != null } as List<Number>
    if (sizes.isEmpty()) return startY
    BigDecimal maxSize = sizes.max() as BigDecimal
    BigDecimal maxRadius = Math.min(keyWidth, keyHeight) / 2.0

    int x = startX
    int y = startY
    values.eachWithIndex { value, int idx ->
      BigDecimal scaled = scale.transform(value) as BigDecimal
      if (scaled == null) {
        return
      }
      BigDecimal radius = maxSize > 0 ? scaled / maxSize * maxRadius : maxRadius / 2.0
      BigDecimal centerX = x + keyWidth / 2.0
      BigDecimal centerY = y + keyHeight / 2.0
      def circle = group.addCircle()
          .cx(centerX)
          .cy(centerY)
          .r(radius.max(1.0))
          .fill(theme.legendKey?.color ?: '#999999')
      if (theme.legendKey?.color) {
        circle.stroke(theme.legendKey.color)
      }

      String label = idx < labels.size() ? labels[idx] : value?.toString() ?: ''
      group.addText(label)
          .x(x + textOffset)
          .y(y + keyHeight - 3)
          .fontSize(theme.legendText?.size ?: 10)
          .fill(theme.legendText?.color ?: 'black')

      if (vertical) {
        y += keyHeight + spacing
      } else {
        x += keyWidth + textOffset + label.length() * 6 + spacing
      }
    }

    return y + (vertical ? 0 : keyHeight + spacing)
  }

  /**
   * Render legend for alpha aesthetic.
   */
  private int renderAlphaLegend(G group, Scale scale, int startX, int startY, boolean vertical, Theme theme) {
    List values
    List<String> labels
    if (scale instanceof ScaleDiscrete) {
      values = (scale as ScaleDiscrete).levels
      labels = (scale as ScaleDiscrete).computedLabels ?: []
    } else if (scale instanceof ScaleContinuous) {
      values = (scale as ScaleContinuous).computedBreaks
      labels = (scale as ScaleContinuous).computedLabels ?: []
    } else {
      return startY
    }
    if (values == null || values.isEmpty()) return startY

    List<Number> keySize = theme.legendKeySize ?: [15, 15] as List<Number>
    int keyWidth = keySize[0].intValue()
    int keyHeight = keySize[1].intValue()
    int spacing = 5
    int textOffset = keyWidth + 8

    int x = startX
    int y = startY
    values.eachWithIndex { value, int idx ->
      BigDecimal scaled = scale.transform(value) as BigDecimal
      if (scaled == null) {
        return
      }
      BigDecimal alphaVal = scaled.min(1).max(0)
      def rect = group.addRect(keyWidth, keyHeight)
          .x(x)
          .y(y)
          .fill(theme.legendKey?.color ?: '#999999')
      rect.addAttribute('fill-opacity', alphaVal)
      if (theme.legendKey?.color) {
        rect.stroke(theme.legendKey.color)
      }

      String label = idx < labels.size() ? labels[idx] : value?.toString() ?: ''
      group.addText(label)
          .x(x + textOffset)
          .y(y + keyHeight - 3)
          .fontSize(theme.legendText?.size ?: 10)
          .fill(theme.legendText?.color ?: 'black')

      if (vertical) {
        y += keyHeight + spacing
      } else {
        x += keyWidth + textOffset + label.length() * 6 + spacing
      }
    }

    return y + (vertical ? 0 : keyHeight + spacing)
  }

  /**
   * Helper method to draw different legend shapes.
   */
  private void drawLegendShape(G group, Number centerX, Number centerY, int size,
                               String shape, String fillColor, String strokeColor) {
    String stroke = strokeColor ?: fillColor
    BigDecimal halfSize = 2.max(size - 2) / 2.0
    BigDecimal cx = centerX as BigDecimal
    BigDecimal cy = centerY as BigDecimal

    switch (shape?.toLowerCase()) {
      case 'square' -> {
        group.addRect((halfSize * 2).round(), (halfSize * 2).round())
            .x((cx - halfSize).round())
            .y((cy - halfSize).round())
            .fill(fillColor)
            .stroke(stroke)
      }
      case 'plus', 'cross' -> {
        group.addLine((cx - halfSize).round(), cy.round(), (cx + halfSize).round(), cy.round())
            .stroke(stroke)
        group.addLine(cx.round(), (cy - halfSize).round(), cx.round(), (cy + halfSize).round())
            .stroke(stroke)
      }
      case 'x' -> {
        group.addLine((cx - halfSize).round(), (cy - halfSize).round(), (cx + halfSize).round(), (cy + halfSize).round())
            .stroke(stroke)
        group.addLine((cx - halfSize).round(), (cy + halfSize).round(), (cx + halfSize).round(), (cy - halfSize).round())
            .stroke(stroke)
      }
      case 'triangle' -> {
        BigDecimal h = (halfSize * 2) * (3 as BigDecimal).sqrt() / 2
        BigDecimal topY = cy - h * 2 / 3
        BigDecimal bottomY = cy + h / 3
        BigDecimal leftX = cx - halfSize
        BigDecimal rightX = cx + halfSize
        String pathD = "M ${cx.round()} ${topY.round()} L ${leftX.round()} ${bottomY.round()} L ${rightX.round()} ${bottomY.round()} Z"
        group.addPath().d(pathD)
            .fill(fillColor)
            .stroke(stroke)
      }
      case 'diamond' -> {
        String diamond = "M ${cx.round()} ${(cy - halfSize).round()} " +
            "L ${(cx + halfSize).round()} ${cy.round()} " +
            "L ${cx.round()} ${(cy + halfSize).round()} " +
            "L ${(cx - halfSize).round()} ${cy.round()} Z"
        group.addPath().d(diamond)
            .fill(fillColor)
            .stroke(stroke)
      }
      default -> {
        group.addCircle()
            .cx(centerX)
            .cy(centerY)
            .r(halfSize.round())
            .fill(fillColor)
            .stroke(stroke)
      }
    }
  }

  /**
   * Check if two discrete scales have the same levels.
   */
  private boolean sameDiscreteLevels(ScaleDiscrete left, ScaleDiscrete right) {
    if (left == null || right == null) return false
    List<Object> leftLevels = left.levels
    List<Object> rightLevels = right.levels
    if (leftLevels == null || rightLevels == null) return false
    return leftLevels == rightLevels
  }

  /**
   * Resolve guide type from scale and guides specifications.
   */
  private String resolveGuideType(String aesthetic, Scale scale, Guides guides) {
    Object spec = guides?.getSpec(aesthetic)
    if (spec == null && scale?.guide != null) {
      spec = scale.guide
    }
    String guideType = context.parseGuideType(spec)
    if (guideType != null) {
      return guideType
    }
    if (scale instanceof ScaleColorGradient) {
      return context.normalizeGuideType((scale as ScaleColorGradient).guideType)
    }
    if (scale instanceof se.alipsa.matrix.gg.scale.ScaleColorGradientN) {
      return context.normalizeGuideType((scale as se.alipsa.matrix.gg.scale.ScaleColorGradientN).guideType)
    }
    if (scale instanceof ScaleColorViridisC) {
      return context.normalizeGuideType((scale as ScaleColorViridisC).guideType)
    }
    return null
  }

  /**
   * Estimate the width needed for the legend based on scales and theme.
   * Returns 0 if no legend is needed.
   */
  int estimateLegendWidth(Map<String, Scale> scales, Theme theme, GgChart chart) {
    if (theme.legendPosition == 'none') return 0

    // Find scales that need legends
    List<String> legendAesthetics = ['color', 'colour', 'fill', 'size', 'shape', 'alpha', 'linetype']
    Map<String, Scale> legendScales = scales.findAll { k, v ->
      legendAesthetics.contains(k) && v.isTrained()
    }

    if (legendScales.isEmpty()) return 0

    Guides guides = chart?.guides
    legendScales = legendScales.findAll { aesthetic, scale ->
      resolveGuideType(aesthetic, scale, guides) != 'none'
    }

    if (legendScales.isEmpty()) return 0

    // Return estimated width based on position
    if (theme.legendPosition == 'right' || theme.legendPosition == 'left') {
      return 150  // Approximate width for vertical legends
    }

    return 0  // Top/bottom legends don't affect width
  }
}
