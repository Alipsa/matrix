package se.alipsa.matrix.gg.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Defs
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.CutWidth
import se.alipsa.matrix.gg.aes.Expression
import se.alipsa.matrix.gg.aes.Factor
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.coord.CoordCartesian
import se.alipsa.matrix.gg.coord.CoordFixed
import se.alipsa.matrix.gg.coord.CoordFlip
import se.alipsa.matrix.gg.coord.CoordPolar
import se.alipsa.matrix.gg.facet.Facet
import se.alipsa.matrix.gg.facet.FacetGrid
import se.alipsa.matrix.gg.facet.FacetWrap
import se.alipsa.matrix.gg.geom.GeomBoxplot
import se.alipsa.matrix.gg.layer.Layer
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.position.GgPosition
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleContinuous
import se.alipsa.matrix.gg.scale.ScaleDiscrete
import se.alipsa.matrix.gg.scale.ScaleShape
import se.alipsa.matrix.gg.scale.ScaleXContinuous
import se.alipsa.matrix.gg.scale.ScaleYContinuous
import se.alipsa.matrix.gg.scale.ScaleXDiscrete
import se.alipsa.matrix.gg.scale.ScaleYDiscrete
import se.alipsa.matrix.gg.stat.GgStat
import se.alipsa.matrix.gg.theme.Theme
import se.alipsa.matrix.gg.theme.ElementLine

/**
 * Main rendering orchestrator for Grammar of Graphics plots.
 * Coordinates the rendering pipeline:
 * 1. Setup plot area
 * 2. Compute scales from data
 * 3. Draw each layer (stat → position → geom)
 * 4. Draw axes
 * 5. Draw legend
 * 6. Apply theme styling
 */
@CompileStatic
class GgRenderer {

  // Default plot margins
  static final int MARGIN_TOP = 60
  static final int MARGIN_RIGHT = 80
  static final int MARGIN_BOTTOM = 60
  static final int MARGIN_LEFT = 80
  static final int LEGEND_PLOT_GAP = 10
  static final int LEGEND_TITLE_SPACING = 7
  static final double AVERAGE_CHAR_WIDTH_RATIO = 0.6d  // Rough estimate for typical sans-serif fonts; adjust if legends clip.
  static final int LEGEND_WIDTH_PADDING = 15
  static final String DEFAULT_STRIP_FILL = '#D9D9D9'
  static final String DEFAULT_STRIP_STROKE = 'none'
  static final int LEGEND_CONTINUOUS_BAR_WIDTH_VERTICAL = 15
  static final int LEGEND_CONTINUOUS_BAR_HEIGHT_VERTICAL = 80
  static final int LEGEND_CONTINUOUS_BAR_WIDTH_HORIZONTAL = 100
  static final int LEGEND_CONTINUOUS_BAR_HEIGHT_HORIZONTAL = 15

  /**
   * Render a GgChart to an SVG.
   * @param chart The chart specification
   * @return The rendered SVG
   */
  Svg render(GgChart chart) {
    // Check if faceting is enabled
    if (chart.facet != null) {
      return renderFaceted(chart)
    }
    return renderSingle(chart)
  }

  /**
   * Render a single (non-faceted) chart.
   */
  private Svg renderSingle(GgChart chart) {
    Theme theme = chart.theme ?: defaultTheme()

    // Calculate initial plot area dimensions
    int plotX = MARGIN_LEFT
    int plotY = MARGIN_TOP
    int plotWidth = chart.width - MARGIN_LEFT - MARGIN_RIGHT
    int plotHeight = chart.height - MARGIN_TOP - MARGIN_BOTTOM

    // Setup coordinate system
    Coord coord = chart.coord ?: new CoordCartesian()

    // For CoordFixed, calculate effective dimensions and resize the entire SVG
    int effectiveWidth = plotWidth
    int effectiveHeight = plotHeight
    int svgWidth = chart.width
    int svgHeight = chart.height

    if (coord instanceof CoordFixed) {
      Map<String, List> aestheticData = collectAestheticData(chart)
      if (aestheticData.x && aestheticData.y) {
        double[] adjusted = computeFixedAspectDimensionsWithExpansion(
            aestheticData.x, aestheticData.y, plotWidth, plotHeight, coord as CoordFixed, chart)
        effectiveWidth = (int) adjusted[0]
        effectiveHeight = (int) adjusted[1]

        // Resize the entire SVG to fit the effective plot dimensions plus margins
        svgWidth = effectiveWidth + MARGIN_LEFT + MARGIN_RIGHT
        svgHeight = effectiveHeight + MARGIN_TOP + MARGIN_BOTTOM
      }
    }

    // Compute scales early so we can estimate legend width
    Map<String, Scale> computedScales = computeScales(chart, effectiveWidth, effectiveHeight, coord)

    // Calculate legend width and adjust SVG if legend is on right side
    String legendPos = theme.legendPosition ?: 'right'
    boolean legendOnRight = legendPos == 'right' ||
        (!(legendPos instanceof String) && !(legendPos instanceof List))
    if (legendOnRight) {
      int legendWidth = estimateLegendWidth(computedScales, theme)
      if (legendWidth > MARGIN_RIGHT) {
        // Legend needs more space than default margin provides
        int extraWidth = legendWidth - MARGIN_RIGHT + LEGEND_PLOT_GAP
        svgWidth += extraWidth
      }
    }

    // Create SVG with adjusted dimensions
    Svg svg = new Svg()
    svg.width(svgWidth)
    svg.height(svgHeight)
    svg.viewBox("0 0 ${svgWidth} ${svgHeight}")

    // 1. Draw background
    renderBackground(svg, svgWidth, svgHeight, theme)

    // Set coord dimensions
    if (coord instanceof CoordCartesian) {
      coord.plotWidth = effectiveWidth
      coord.plotHeight = effectiveHeight
    } else if (coord instanceof CoordFlip) {
      coord.plotWidth = effectiveWidth
      coord.plotHeight = effectiveHeight
    } else if (coord instanceof CoordPolar) {
      coord.plotWidth = effectiveWidth
      coord.plotHeight = effectiveHeight
    }

    // 2. Setup plot area group
    G plotArea = svg.addG()
    plotArea.id('plot-area')
    plotArea.transform("translate($plotX, $plotY)")

    // Draw panel background (use effective dimensions for CoordFixed)
    renderPanelBackground(plotArea, effectiveWidth, effectiveHeight, theme)

    // 6. Draw grid lines before data.
    renderGridLines(plotArea, computedScales, effectiveWidth, effectiveHeight, theme, coord)

    // 7. Create data layer group with clip path matching effective dimensions
    G dataLayer = plotArea.addG()
    dataLayer.id('data-layer')
    Defs defs = svg.addDefs()
    def clipPath = defs.addClipPath().id('plot-clip')
    clipPath.addRect(effectiveWidth, effectiveHeight).x(0).y(0)
    dataLayer.addAttribute('clip-path', 'url(#plot-clip)')

    // 8. Render each layer
    chart.layers.each { layer ->
      renderLayer(dataLayer, layer, chart, computedScales, coord, effectiveWidth, effectiveHeight)
    }

    // 9. Draw axes (on top of data, use effective dimensions)
    renderAxes(plotArea, computedScales, effectiveWidth, effectiveHeight, theme, coord)

    // 10. Draw title and labels
    renderLabels(svg, chart, effectiveWidth, effectiveHeight, svgHeight, theme)

    // 11. Draw legend (if needed)
    renderLegend(svg, computedScales, chart, theme)

    return svg
  }

  /**
   * Render a faceted chart with multiple panels.
   */
  private Svg renderFaceted(GgChart chart) {
    Svg svg = new Svg()
    svg.width(chart.width)
    svg.height(chart.height)
    svg.viewBox("0 0 ${chart.width} ${chart.height}")

    Theme theme = chart.theme ?: defaultTheme()
    Facet facet = chart.facet

    // 1. Draw background
    renderBackground(svg, chart.width, chart.height, theme)

    // 2. Calculate facet layout
    Map<String, Integer> layout = facet.computeLayout(chart.data)
    int nrow = layout.nrow
    int ncol = layout.ncol
    List<Map<String, Object>> panels = facet.getPanelValues(chart.data)

    if (panels.isEmpty()) {
      // No faceting data, render as single chart
      return renderSingle(chart)
    }

    // 3. Calculate panel dimensions
    // For FacetGrid: column strips at top, row strips at right
    // For FacetWrap: strip at top of each panel
    boolean isFacetGrid = facet instanceof FacetGrid
    FacetGrid fg = isFacetGrid ? (FacetGrid) facet : null

    int colStripHeight = (facet.strip && isFacetGrid && fg.cols) ? 20 : 0
    int rowStripWidth = (facet.strip && isFacetGrid && fg.rows) ? 20 : 0
    int perPanelStripHeight = (facet.strip && !isFacetGrid) ? 20 : 0

    int totalPlotWidth = chart.width - MARGIN_LEFT - MARGIN_RIGHT - rowStripWidth
    int totalPlotHeight = chart.height - MARGIN_TOP - MARGIN_BOTTOM - colStripHeight

    // Account for spacing between panels
    int panelSpacing = facet.panelSpacing
    int availableWidth = totalPlotWidth - (ncol - 1) * panelSpacing
    int availableHeight = totalPlotHeight - (nrow - 1) * panelSpacing - nrow * perPanelStripHeight

    int panelWidth = (availableWidth / ncol) as int
    int panelHeight = (availableHeight / nrow) as int

    // 4. Setup coordinate system
    Coord coord = chart.coord ?: new CoordCartesian()

    // 5. Compute global scales (for fixed scales mode)
    Map<String, Scale> globalScales = computeScales(chart, panelWidth, panelHeight, coord)

    // 6. Create clip path for panels
    Defs defs = svg.addDefs()

    // 7. Render each panel
    for (int panelIdx = 0; panelIdx < panels.size(); panelIdx++) {
      Map<String, Object> panelValues = panels[panelIdx]

      // Get panel position
      int row, col
      if (facet instanceof FacetWrap) {
        FacetWrap fw = facet as FacetWrap
        Map<String, Integer> pos = fw.getPanelPosition(panelIdx, layout)
        row = pos.row
        col = pos.col
      } else if (isFacetGrid) {
        row = fg.getRowIndex(panelValues, chart.data)
        col = fg.getColIndex(panelValues, chart.data)
      } else {
        row = panelIdx / ncol as int
        col = panelIdx % ncol
      }

      // Calculate panel position in pixels
      // For FacetGrid: panels start below column strips
      int panelX = MARGIN_LEFT + col * (panelWidth + panelSpacing)
      int panelY = MARGIN_TOP + colStripHeight + row * (panelHeight + perPanelStripHeight + panelSpacing)

      // Filter data for this panel
      Matrix panelData = facet.filterDataForPanel(chart.data, panelValues)

      // Create panel group
      G panelGroup = svg.addG()
      panelGroup.id("panel-${row}-${col}")
      panelGroup.transform("translate($panelX, $panelY)")

      // Render strip label (if enabled and FacetWrap only - FacetGrid has separate strips)
      if (facet.strip && !isFacetGrid) {
        renderFacetStrip(panelGroup, facet.getPanelLabel(panelValues), panelWidth, perPanelStripHeight, theme)
      }

      // Create panel content group (below strip for FacetWrap)
      G contentGroup = panelGroup.addG()
      contentGroup.transform("translate(0, $perPanelStripHeight)")

      // Draw panel background
      renderPanelBackground(contentGroup, panelWidth, panelHeight, theme)

      // Compute scales for this panel
      Map<String, Scale> panelScales
      if (facet.scales == 'fixed') {
        // Use global scales
        panelScales = cloneScales(globalScales, panelWidth, panelHeight, coord)
      } else {
        // Compute panel-specific scales
        panelScales = computeScalesForData(panelData, chart, panelWidth, panelHeight, coord, facet)
      }

      // Setup coord dimensions for this panel
      if (coord instanceof CoordCartesian) {
        coord.plotWidth = panelWidth
        coord.plotHeight = panelHeight
      } else if (coord instanceof CoordFlip) {
        coord.plotWidth = panelWidth
        coord.plotHeight = panelHeight
      } else if (coord instanceof CoordPolar) {
        coord.plotWidth = panelWidth
        coord.plotHeight = panelHeight
      }

      // Draw grid lines before data.
      renderGridLines(contentGroup, panelScales, panelWidth, panelHeight, theme, coord)

      // Create data layer with clip path
      G dataLayer = contentGroup.addG()
      dataLayer.id("data-layer-${row}-${col}")
      String clipId = "panel-clip-${row}-${col}"
      def clipPath = defs.addClipPath().id(clipId)
      clipPath.addRect(panelWidth, panelHeight).x(0).y(0)
      dataLayer.addAttribute('clip-path', "url(#${clipId})")

      // Render each layer with filtered data
      chart.layers.each { layer ->
        renderLayerWithData(dataLayer, layer, panelData, chart.globalAes, panelScales, coord, panelWidth, panelHeight)
      }

      // Draw axes
      // Only draw y-axis labels on leftmost column
      boolean showYLabels = (col == 0)
      // Only draw x-axis labels on bottom row
      boolean showXLabels = (row == nrow - 1)

      renderFacetAxes(contentGroup, panelScales, panelWidth, panelHeight, theme, coord, showXLabels, showYLabels)
    }

    // 8. Render FacetGrid strips (column strips at top, row strips at right)
    if (isFacetGrid && facet.strip) {
      // Column strips at top
      if (fg.cols && colStripHeight > 0) {
        for (int c = 0; c < ncol; c++) {
          int stripX = MARGIN_LEFT + c * (panelWidth + panelSpacing)
          int stripY = MARGIN_TOP
          G colStripGroup = svg.addG()
          colStripGroup.id("col-strip-${c}")
          colStripGroup.transform("translate($stripX, $stripY)")
          renderFacetStrip(colStripGroup, fg.getColLabel(c, chart.data), panelWidth, colStripHeight, theme)
        }
      }

      // Row strips at right (rotated 90 degrees)
      if (fg.rows && rowStripWidth > 0) {
        for (int r = 0; r < nrow; r++) {
          int stripX = MARGIN_LEFT + totalPlotWidth
          int stripY = MARGIN_TOP + colStripHeight + r * (panelHeight + panelSpacing)
          G rowStripGroup = svg.addG()
          rowStripGroup.id("row-strip-${r}")
          rowStripGroup.transform("translate($stripX, $stripY)")
          renderFacetStripRotated(rowStripGroup, fg.getRowLabel(r, chart.data), rowStripWidth, panelHeight, theme)
        }
      }
    }

    // 9. Draw title and labels
    renderLabels(svg, chart, totalPlotWidth, totalPlotHeight, chart.height, theme)

    // 9. Draw legend (if needed)
    renderLegend(svg, globalScales, chart, theme)

    return svg
  }

  /**
   * Render a facet strip label (horizontal, for column strips at top).
   */
  private void renderFacetStrip(G group, String label, int width, int height, Theme theme) {
    // Draw strip background (use fill from theme if set, otherwise default to gray)
    String stripFill = resolveStripFill(theme)
    String stripStroke = resolveStripStroke(theme)
    group.addRect(width, height)
        .fill(stripFill)
        .stroke(stripStroke)

    // Draw strip text
    group.addText(label)
        .x(width / 2)
        .y(height / 2 + 4)
        .textAnchor('middle')
        .fontSize(theme.stripText?.size ?: 10)
        .fill(theme.stripText?.color ?: 'black')
  }

  /**
   * Render a facet strip label rotated 90 degrees (for row strips on the right).
   */
  private void renderFacetStripRotated(G group, String label, int width, int height, Theme theme) {
    // Draw strip background (use fill from theme if set, otherwise default to gray)
    String stripFill = resolveStripFill(theme)
    String stripStroke = resolveStripStroke(theme)
    group.addRect(width, height)
        .fill(stripFill)
        .stroke(stripStroke)

    // Draw strip text rotated 90 degrees
    int centerX = width / 2 as int
    int centerY = height / 2 as int
    group.addText(label)
        .x(centerX)
        .y(centerY + 4)
        .textAnchor('middle')
        .fontSize(theme.stripText?.size ?: 10)
        .fill(theme.stripText?.color ?: 'black')
        .transform("rotate(90, $centerX, $centerY)")
  }

  /**
   * Adjust scales for a panel with new dimensions.
   * Updates the range of position scales for the panel size.
   */
  private Map<String, Scale> cloneScales(Map<String, Scale> original, int width, int height, Coord coord) {
    Map<String, Scale> adjusted = [:]
    boolean isFlipped = coord instanceof CoordFlip

    original.each { key, scale ->
      // For position scales, update the range for this panel's dimensions
      if (key == 'x') {
        if (scale instanceof ScaleContinuous) {
          ScaleContinuous sc = scale as ScaleContinuous
          sc.range = isFlipped ? [height, 0] as List<Number> : [0, width] as List<Number>
        } else if (scale instanceof ScaleDiscrete) {
          ScaleDiscrete sd = scale as ScaleDiscrete
          sd.range = isFlipped ? [height, 0] as List<Number> : [0, width] as List<Number>
        }
      } else if (key == 'y') {
        if (scale instanceof ScaleContinuous) {
          ScaleContinuous sc = scale as ScaleContinuous
          sc.range = isFlipped ? [0, width] as List<Number> : [height, 0] as List<Number>
        } else if (scale instanceof ScaleDiscrete) {
          ScaleDiscrete sd = scale as ScaleDiscrete
          sd.range = isFlipped ? [0, width] as List<Number> : [height, 0] as List<Number>
        }
      }
      adjusted[key] = scale
    }
    return adjusted
  }

  /**
   * Compute scales for specific panel data (for free scales mode).
   */
  private Map<String, Scale> computeScalesForData(Matrix data, GgChart chart, int width, int height, Coord coord, Facet facet) {
    // Create a temporary chart with filtered data
    GgChart tempChart = new GgChart(data, chart.globalAes)
    tempChart.layers = chart.layers
    tempChart.scales = chart.scales

    Map<String, Scale> scales = computeScales(tempChart, width, height, coord)

    // If scales are partially free, override with global scale
    if (facet.scales == 'free_x' && scales['y']) {
      // Keep y fixed from original chart
      Map<String, Scale> globalScales = computeScales(chart, width, height, coord)
      scales['y'] = globalScales['y']
    } else if (facet.scales == 'free_y' && scales['x']) {
      // Keep x fixed from original chart
      Map<String, Scale> globalScales = computeScales(chart, width, height, coord)
      scales['x'] = globalScales['x']
    }

    return scales
  }

  /**
   * Render axes for a facet panel.
   */
  private void renderFacetAxes(G plotArea, Map<String, Scale> scales, int width, int height,
                               Theme theme, Coord coord, boolean showXLabels, boolean showYLabels) {
    if (coord instanceof CoordPolar) {
      return
    }
    G axesGroup = plotArea.addG()
    axesGroup.id('axes')

    boolean isFlipped = coord instanceof CoordFlip

    if (isFlipped) {
      renderFacetYAxis(axesGroup, scales['x'], height, theme, showYLabels)
      renderFacetXAxis(axesGroup, scales['y'], width, height, theme, showXLabels)
    } else {
      renderFacetXAxis(axesGroup, scales['x'], width, height, theme, showXLabels)
      renderFacetYAxis(axesGroup, scales['y'], height, theme, showYLabels)
    }
  }

  /**
   * Render X axis for facet panel.
   */
  private void renderFacetXAxis(G axesGroup, Scale scale, int width, int height, Theme theme, boolean showLabels) {
    if (scale == null) return

    G xAxisGroup = axesGroup.addG()
    xAxisGroup.id('x-axis')
    xAxisGroup.transform("translate(0, $height)")

    // Axis line
    xAxisGroup.addLine(0, 0, width, 0)
        .stroke(theme.axisLineX?.color ?: 'black')
        .strokeWidth(theme.axisLineX?.size ?: 1)

    if (showLabels) {
      List breaks = scale.getComputedBreaks()
      List<String> labels = scale.getComputedLabels()

      breaks.eachWithIndex { breakVal, i ->
        Double xPos = scale.transform(breakVal) as Double
        if (xPos == null) return

        xAxisGroup.addLine(xPos, 0, xPos, theme.axisTickLength ?: 5)
            .stroke('black')

        String label = i < labels.size() ? labels[i] : breakVal.toString()
        xAxisGroup.addText(label)
            .x(xPos)
            .y((theme.axisTickLength ?: 5) + 12)
            .textAnchor('middle')
            .fontSize(theme.axisTextX?.size ?: 9)
      }
    }
  }

  /**
   * Render Y axis for facet panel.
   */
  private void renderFacetYAxis(G axesGroup, Scale scale, int height, Theme theme, boolean showLabels) {
    if (scale == null) return

    G yAxisGroup = axesGroup.addG()
    yAxisGroup.id('y-axis')

    // Axis line
    yAxisGroup.addLine(0, 0, 0, height)
        .stroke(theme.axisLineY?.color ?: 'black')
        .strokeWidth(theme.axisLineY?.size ?: 1)

    if (showLabels) {
      List breaks = scale.getComputedBreaks()
      List<String> labels = scale.getComputedLabels()

      breaks.eachWithIndex { breakVal, i ->
        Double yPos = scale.transform(breakVal) as Double
        if (yPos == null) return

        yAxisGroup.addLine(-1 * (theme.axisTickLength ?: 5), yPos, 0, yPos)
            .stroke('black')

        String label = i < labels.size() ? labels[i] : breakVal.toString()
        yAxisGroup.addText(label)
            .x(-1 * (theme.axisTickLength ?: 5) - 3)
            .y(yPos + 3)
            .textAnchor('end')
            .fontSize(theme.axisTextY?.size ?: 9)
      }
    }
  }

  /**
   * Render a layer with specific data (for faceted charts).
   */
  private void renderLayerWithData(G dataLayer, Layer layer, Matrix data, Aes globalAes,
                                   Map<String, Scale> scales, Coord coord,
                                   int plotWidth, int plotHeight) {
    // Merge layer aes with global aes when inheritAes is true (the default)
    Aes aes
    if (layer.inheritAes && layer.aes != null) {
      aes = layer.aes.merge(globalAes)
    } else {
      aes = layer.aes ?: globalAes
    }

    if (data == null || data.rowCount() == 0 || aes == null) return

    // Evaluate closure expressions in aesthetics
    EvaluatedAes evalResult = evaluateExpressions(data, aes)
    Matrix exprData = evalResult.data
    Aes resolvedAes = evalResult.aes

    // Apply statistical transformation
    Matrix statData = applyStats(exprData, resolvedAes, layer)

    // Apply position adjustment
    Matrix posData = applyPosition(statData, resolvedAes, layer)

    // Create layer group
    G layerGroup = dataLayer.addG()
    layerGroup.styleClass(layer.geom?.class?.simpleName?.toLowerCase() ?: 'layer')

    // Render the geom
    if (layer.geom) {
      layer.geom.render(layerGroup, posData, resolvedAes, scales, coord)
    }
  }

  /**
   * Render background elements.
   */
  private void renderBackground(Svg svg, int width, int height, Theme theme) {
    if (theme.plotBackground) {
      svg.addRect(width, height)
         .fill(theme.plotBackground.fill ?: 'white')
    }
  }

  /**
   * Render the panel (plot area) background.
   */
  private void renderPanelBackground(G plotArea, int width, int height, Theme theme) {
    if (theme.panelBackground) {
      plotArea.addRect(width, height)
              .fill(theme.panelBackground.fill ?: '#EBEBEB')
              .stroke(theme.panelBackground.color ?: 'none')
    }
  }

  /**
   * Compute scales for all aesthetics based on data.
   */
  private Map<String, Scale> computeScales(GgChart chart, int plotWidth, int plotHeight, Coord coord) {
    Map<String, Scale> scales = [:]

    // Collect all data values for each aesthetic
    Map<String, List> aestheticData = collectAestheticData(chart)

    // Determine if axes are flipped
    boolean isFlipped = coord instanceof CoordFlip

    // Note: For CoordFixed, dimensions are already adjusted by renderSingle before calling this method
    // So plotWidth and plotHeight are the effective dimensions

    // For CoordFlip: x data maps to vertical axis, y data maps to horizontal axis
    // Normal: x -> [0, plotWidth], y -> [plotHeight, 0] (inverted for SVG)
    // Flipped: x -> [plotHeight, 0], y -> [0, plotWidth]

    List<Number> xRange = isFlipped ? [plotHeight, 0] as List<Number> : [0, plotWidth] as List<Number>
    List<Number> yRange = isFlipped ? [0, plotWidth] as List<Number> : [plotHeight, 0] as List<Number>

    boolean isPolar = coord instanceof CoordPolar
    String thetaAes = isPolar ? ((coord as CoordPolar).theta == 'y' ? 'y' : 'x') : null

    // Create x scale (auto-detect discrete vs continuous)
    if (aestheticData.x) {
      Scale xScale = createAutoScale('x', aestheticData.x)
      if (isPolar && thetaAes == 'x' && xScale instanceof ScaleContinuous) {
        (xScale as ScaleContinuous).expand = [0, 0] as List<Number>
      }
      xScale.train(aestheticData.x)
      if (xScale instanceof ScaleContinuous) {
        (xScale as ScaleContinuous).range = xRange
      } else if (xScale instanceof ScaleDiscrete) {
        (xScale as ScaleDiscrete).range = xRange
      }
      scales['x'] = xScale
    }

    // Create y scale (auto-detect discrete vs continuous)
    if (aestheticData.y) {
      Scale yScale = createAutoScale('y', aestheticData.y)
      if (isPolar && thetaAes == 'y' && yScale instanceof ScaleContinuous) {
        (yScale as ScaleContinuous).expand = [0, 0] as List<Number>
      }
      yScale.train(aestheticData.y)
      if (yScale instanceof ScaleContinuous) {
        (yScale as ScaleContinuous).range = yRange
      } else if (yScale instanceof ScaleDiscrete) {
        (yScale as ScaleDiscrete).range = yRange
      }
      scales['y'] = yScale
    }

    // Create color scale if color data exists
    if (aestheticData.color) {
      Scale colorScale = createAutoScale('color', aestheticData.color)
      colorScale.train(aestheticData.color)
      // Set name from aesthetic mapping for legend title
      if (chart.globalAes?.color && !chart.globalAes.isConstant('color')) {
        colorScale.name = chart.globalAes.colorColName
      }
      scales['color'] = colorScale
    }

    // Create fill scale if fill data exists
    if (aestheticData.fill) {
      Scale fillScale = createAutoScale('fill', aestheticData.fill)
      fillScale.train(aestheticData.fill)
      // Set name from aesthetic mapping for legend title
      if (chart.globalAes?.fill && !chart.globalAes.isConstant('fill')) {
        fillScale.name = chart.globalAes.fillColName
      }
      scales['fill'] = fillScale
    }

    // Create shape scale if shape data exists
    if (aestheticData.shape) {
      Scale shapeScale = createAutoScale('shape', aestheticData.shape)
      shapeScale.train(aestheticData.shape)
      if (chart.globalAes?.shape instanceof String) {
        shapeScale.name = chart.globalAes.shape as String
      }
      scales['shape'] = shapeScale
    }

    // Add user-specified scales (these override auto-detected scales)
    chart.scales.each { scale ->
      if (scale.aesthetic) {
        // Train user scale if not already trained
        if (!scale.isTrained() && aestheticData[scale.aesthetic]) {
          scale.train(aestheticData[scale.aesthetic])
        }
        // Helper to set name from aesthetic mapping for legend title
        def setScaleNameFromAesthetic = { Scale s, String aestheticName, GgChart c ->
          if (s.name) {
            return
          }
          String aes = aestheticName == 'colour' ? 'color' : aestheticName
          // First check globalAes
          if (aes == 'color' && c.globalAes?.color && !c.globalAes.isConstant('color')) {
            s.name = c.globalAes.colorColName
          } else if (aes == 'fill' && c.globalAes?.fill && !c.globalAes.isConstant('fill')) {
            s.name = c.globalAes.fillColName
          } else {
            // Check layer aesthetics for color/fill mappings
            for (layer in c.layers) {
              if (layer.aes != null) {
                if (aes == 'color' && layer.aes.color && !layer.aes.isConstant('color')) {
                  s.name = layer.aes.colorColName
                  break
                } else if (aes == 'fill' && layer.aes.fill && !layer.aes.isConstant('fill')) {
                  s.name = layer.aes.fillColName
                  break
                }
              }
            }
          }
        }
        // Set name from aesthetic mapping for legend title (if not already set by user)
        if (!scale.name) {
          setScaleNameFromAesthetic(scale, scale.aesthetic as String, chart)
        }
        // Set range for position scales (respecting CoordFlip)
        if (scale.aesthetic == 'x' && scale instanceof ScaleContinuous) {
          (scale as ScaleContinuous).range = xRange
        } else if (scale.aesthetic == 'x' && scale instanceof ScaleDiscrete) {
          (scale as ScaleDiscrete).range = xRange
        } else if (scale.aesthetic == 'y' && scale instanceof ScaleContinuous) {
          (scale as ScaleContinuous).range = yRange
        } else if (scale.aesthetic == 'y' && scale instanceof ScaleDiscrete) {
          (scale as ScaleDiscrete).range = yRange
        }
        scales[scale.aesthetic] = scale
      }
    }

    return scales
  }

  /**
   * Compute adjusted dimensions to enforce a fixed aspect ratio.
   * For ratio r, ensures: (pixelsPerUnitY / pixelsPerUnitX) = r
   * This means one unit in y appears as r times the size of one unit in x.
   *
   * @param xData List of x values
   * @param yData List of y values
   * @param plotWidth Available plot width in pixels
   * @param plotHeight Available plot height in pixels
   * @param coord CoordFixed with ratio and optional xlim/ylim
   * @return double[] with [effectiveWidth, effectiveHeight]
   */
  private double[] computeFixedAspectDimensions(List xData, List yData, int plotWidth, int plotHeight, CoordFixed coord) {
    double ratio = coord.ratio

    // Get numeric values only
    List<Number> xNums = xData.findAll { it instanceof Number } as List<Number>
    List<Number> yNums = yData.findAll { it instanceof Number } as List<Number>

    if (xNums.isEmpty() || yNums.isEmpty()) {
      return [plotWidth, plotHeight] as double[]
    }

    // Compute data ranges - use xlim/ylim if specified, otherwise use data min/max
    double xMin, xMax, yMin, yMax

    if (coord.xlim != null && coord.xlim.size() >= 2) {
      xMin = coord.xlim[0] as double
      xMax = coord.xlim[1] as double
    } else {
      xMin = xNums.min() as double
      xMax = xNums.max() as double
    }

    if (coord.ylim != null && coord.ylim.size() >= 2) {
      yMin = coord.ylim[0] as double
      yMax = coord.ylim[1] as double
    } else {
      yMin = yNums.min() as double
      yMax = yNums.max() as double
    }

    double xDataRange = xMax - xMin
    double yDataRange = yMax - yMin

    // Avoid division by zero
    if (xDataRange <= 0) xDataRange = 1
    if (yDataRange <= 0) yDataRange = 1

    // Current pixels per data unit
    double pxPerUnitX = plotWidth / xDataRange
    double pxPerUnitY = plotHeight / yDataRange

    // Desired: pxPerUnitY / pxPerUnitX = ratio
    // So: pxPerUnitY = ratio * pxPerUnitX
    // We need to adjust either width or height to achieve this

    double currentRatio = pxPerUnitY / pxPerUnitX
    double effectiveWidth = plotWidth as double
    double effectiveHeight = plotHeight as double

    if (currentRatio > ratio) {
      // Height is too large relative to width, reduce effective height
      effectiveHeight = (ratio * pxPerUnitX) * yDataRange
    } else if (currentRatio < ratio) {
      // Width is too large relative to height, reduce effective width
      effectiveWidth = (pxPerUnitY / ratio) * xDataRange
    }

    return [effectiveWidth, effectiveHeight] as double[]
  }

  /**
   * Compute adjusted dimensions to enforce a fixed aspect ratio, accounting for scale expansion.
   * This version applies the same expansion that ScaleContinuous uses (defaulting to
   * its constants when no scale is provided), ensuring the plot area matches the actual
   * scale domain.
   *
   * @param xData List of x values
   * @param yData List of y values
   * @param plotWidth Available plot width in pixels
   * @param plotHeight Available plot height in pixels
   * @param coord CoordFixed with ratio and optional xlim/ylim
   * @param chart Chart (used to resolve expansion settings)
   * @return double[] with [effectiveWidth, effectiveHeight]
   */
  private double[] computeFixedAspectDimensionsWithExpansion(List xData, List yData, int plotWidth, int plotHeight,
                                                             CoordFixed coord, GgChart chart) {
    double ratio = coord.ratio

    // Get numeric values only
    List<Number> xNums = xData.findAll { it instanceof Number } as List<Number>
    List<Number> yNums = yData.findAll { it instanceof Number } as List<Number>

    if (xNums.isEmpty() || yNums.isEmpty()) {
      return [plotWidth, plotHeight] as double[]
    }

    // Compute data ranges - use xlim/ylim if specified, otherwise use data min/max
    double xMin, xMax, yMin, yMax

    if (coord.xlim != null && coord.xlim.size() >= 2) {
      xMin = coord.xlim[0] as double
      xMax = coord.xlim[1] as double
    } else {
      xMin = xNums.min() as double
      xMax = xNums.max() as double
    }

    if (coord.ylim != null && coord.ylim.size() >= 2) {
      yMin = coord.ylim[0] as double
      yMax = coord.ylim[1] as double
    } else {
      yMin = yNums.min() as double
      yMax = yNums.max() as double
    }

    // Apply expansion per side (mult * range plus add) using ScaleContinuous defaults.
    double xDelta = xMax - xMin
    double yDelta = yMax - yMin
    double[] xExpansion = resolveContinuousExpansion(chart, 'x')
    double[] yExpansion = resolveContinuousExpansion(chart, 'y')

    xMin = xMin - xDelta * xExpansion[0] - xExpansion[1]
    xMax = xMax + xDelta * xExpansion[0] + xExpansion[1]
    yMin = yMin - yDelta * yExpansion[0] - yExpansion[1]
    yMax = yMax + yDelta * yExpansion[0] + yExpansion[1]

    double xDataRange = xMax - xMin
    double yDataRange = yMax - yMin

    // If the range collapses, keep original dimensions to avoid misleading scaling.
    if (xDataRange <= 0 || yDataRange <= 0) {
      return [plotWidth, plotHeight] as double[]
    }

    // Current pixels per data unit
    double pxPerUnitX = plotWidth / xDataRange
    double pxPerUnitY = plotHeight / yDataRange

    // Desired: pxPerUnitY / pxPerUnitX = ratio
    // So: pxPerUnitY = ratio * pxPerUnitX
    // We need to adjust either width or height to achieve this

    double currentRatio = pxPerUnitY / pxPerUnitX
    double effectiveWidth = plotWidth as double
    double effectiveHeight = plotHeight as double

    if (currentRatio > ratio) {
      // Height is too large relative to width, reduce effective height
      effectiveHeight = (ratio * pxPerUnitX) * yDataRange
    } else if (currentRatio < ratio) {
      // Width is too large relative to height, reduce effective width
      effectiveWidth = (pxPerUnitY / ratio) * xDataRange
    }

    return [effectiveWidth, effectiveHeight] as double[]
  }

  private double[] resolveContinuousExpansion(GgChart chart, String aesthetic) {
    ScaleContinuous scale = chart.scales.find {
      it instanceof ScaleContinuous && it.aesthetic == aesthetic
    } as ScaleContinuous

    if (scale == null) {
      return [ScaleContinuous.DEFAULT_EXPAND_MULT, ScaleContinuous.DEFAULT_EXPAND_ADD] as double[]
    }
    if (scale.expand == null || scale.expand.size() < 2) {
      return [0.0d, 0.0d] as double[]
    }

    double mult = scale.expand[0] != null ? (scale.expand[0] as double) : ScaleContinuous.DEFAULT_EXPAND_MULT
    double add = scale.expand[1] != null ? (scale.expand[1] as double) : ScaleContinuous.DEFAULT_EXPAND_ADD
    return [mult, add] as double[]
  }

  /**
   * Auto-detect and create the appropriate scale type based on data.
   */
  private Scale createAutoScale(String aesthetic, List data) {
    boolean isDiscrete = isDiscreteData(data)

    switch (aesthetic) {
      case 'x':
        return isDiscrete ? new ScaleXDiscrete() : new ScaleXContinuous()
      case 'y':
        return isDiscrete ? new ScaleYDiscrete() : new ScaleYContinuous()
      case 'shape':
        return new ScaleShape()
      case 'color':
      case 'colour':
      case 'fill':
        // For color/fill, use ScaleColorManual for discrete, ScaleColorGradient for continuous
        if (isDiscrete) {
          def scale = new se.alipsa.matrix.gg.scale.ScaleColorManual()
          scale.aesthetic = aesthetic == 'colour' ? 'color' : aesthetic
          return scale
        } else {
          def scale = new se.alipsa.matrix.gg.scale.ScaleColorGradient()
          scale.aesthetic = aesthetic == 'colour' ? 'color' : aesthetic
          return scale
        }
      default:
        return isDiscrete ? new ScaleDiscrete() : new ScaleContinuous()
    }
  }

  /**
   * Determine if data should be treated as discrete (categorical).
   * Data is considered discrete if:
   * - It contains non-numeric values (strings, etc.)
   *
   * Numeric data (including integers) is always treated as continuous.
   * This is important for computed values like counts in bar charts.
   */
  private boolean isDiscreteData(List data) {
    if (data == null || data.isEmpty()) return false

    // Filter out nulls
    List nonNull = data.findAll { it != null }
    if (nonNull.isEmpty()) return false

    // Check if all values are numeric - if so, treat as continuous
    boolean allNumeric = nonNull.every { it instanceof Number }
    if (allNumeric) return false  // Numeric data is always continuous

    // Non-numeric data is discrete (strings, etc.)
    return true
  }

  /**
   * Collect data values for each aesthetic across all layers.
   * For layers with stat transformations that compute y values (like stat_count),
   * we need to apply the stat first to get the computed values.
   */
  private Map<String, List> collectAestheticData(GgChart chart) {
    Map<String, List> data = [:].withDefault { [] }

    Aes globalAes = chart.globalAes
    boolean hasBarGeom = false

    // Collect from each layer (including stat-transformed data)
    chart.layers.each { layer ->
      Matrix layerData = layer.data ?: chart.data
      // Merge layer aes with global aes when inheritAes is true (the default)
      Aes layerAes
      if (layer.inheritAes && layer.aes != null) {
        layerAes = layer.aes.merge(globalAes)
      } else {
        layerAes = layer.aes ?: globalAes
      }

      // Check if this is a bar/column geom that needs y-axis to include 0
      if (layer.geom?.class?.simpleName in ['GeomBar', 'GeomCol', 'GeomHistogram']) {
        hasBarGeom = true
      }

      if (layerAes && layerData) {
        // Evaluate expressions first
        EvaluatedAes evalResult = evaluateExpressions(layerData, layerAes)
        Matrix exprData = evalResult.data
        Aes resolvedLayerAes = evalResult.aes

        // Apply stat transformation to get computed values
        Matrix statData = applyStats(exprData, resolvedLayerAes, layer)
        // Apply position adjustment so scales reflect stacked/dodged coordinates
        Matrix posData = layer.position != PositionType.IDENTITY ?
            applyPosition(statData, resolvedLayerAes, layer) : statData

        // For x aesthetic: use stat-computed columns appropriately
        // For histograms: use xmin/xmax (bin boundaries) for scale
        // For boxplots: use only x (median position) for scale - don't include data ranges
        boolean isHistogramOrBar = layer.geom?.class?.simpleName in ['GeomBar', 'GeomCol', 'GeomHistogram']
        boolean isBoxplot = layer.geom instanceof GeomBoxplot

        if (posData.columnNames().contains('x')) {
          // Use the 'x' column from stat output
          data['x'].addAll(posData['x'] ?: [])
        }
        if (isHistogramOrBar && posData.columnNames().contains('xmin') && posData.columnNames().contains('xmax')) {
          // For histograms/bars: include xmin/xmax to show full bar widths
          data['x'].addAll(posData['xmin'] ?: [])
          data['x'].addAll(posData['xmax'] ?: [])
        } else if (!posData.columnNames().contains('x') &&
                   resolvedLayerAes.xColName && posData.columnNames().contains(resolvedLayerAes.xColName)) {
          // Fallback: use original x column if no stat-computed x exists
          data['x'].addAll(posData[resolvedLayerAes.xColName] ?: [])
        }

        // For y aesthetic, check both the original y column and computed columns like 'count'
        if (posData.columnNames().contains('ymin') && posData.columnNames().contains('ymax')) {
          // stat_boxplot produces ymin/ymax columns - include both for full y range
          data['y'].addAll(posData['ymin'] ?: [])
          data['y'].addAll(posData['ymax'] ?: [])
          // Also include outlier values so y-scale covers them
          if (posData.columnNames().contains('outliers')) {
            posData['outliers']?.each { outlierList ->
              if (outlierList instanceof List) {
                data['y'].addAll(outlierList.findAll { it instanceof Number })
              }
            }
          }
        } else if (resolvedLayerAes.isAfterStat('y')) {
          // Explicit after_stat() reference - use the specified computed column
          String statCol = resolvedLayerAes.getAfterStatName('y')
          if (posData.columnNames().contains(statCol)) {
            data['y'].addAll(posData[statCol] ?: [])
          }
        } else if (resolvedLayerAes.yColName && posData.columnNames().contains(resolvedLayerAes.yColName)) {
          data['y'].addAll(posData[resolvedLayerAes.yColName] ?: [])
        } else if (posData.columnNames().contains('count')) {
          // stat_count and stat_bin produce 'count' column for y values (default behavior)
          data['y'].addAll(posData['count'] ?: [])
        }

        if (isBoxplot && posData.columnNames().contains('x')) {
          boolean hasBounds = posData.columnNames().contains('xmin') && posData.columnNames().contains('xmax')
          GeomBoxplot geom = (GeomBoxplot) layer.geom
          double widthValue = geom?.width != null ? (geom.width as Number).doubleValue() : 0.75d
          boolean useVarwidth = geom?.varwidth == true
          double maxRelVarwidth = 1.0d
          if (useVarwidth && posData.columnNames().contains('relvarwidth')) {
            List<Number> relValues = (posData['relvarwidth'] as List)
                .findAll { it instanceof Number } as List<Number>
            if (!relValues.isEmpty()) {
              maxRelVarwidth = relValues.max() as double
            }
          }
          if (hasBounds) {
            if (useVarwidth) {
              posData.eachWithIndex { Row row, int idx ->
                if (!(row['xmin'] instanceof Number) || !(row['xmax'] instanceof Number)) {
                  return
                }
                double xmin = (row['xmin'] as Number).doubleValue()
                double xmax = (row['xmax'] as Number).doubleValue()
                double center = (xmin + xmax) / 2.0d
                double widthData = GeomBoxplot.resolveWidthData(row, widthValue, useVarwidth, maxRelVarwidth, xmax - xmin)
                if (widthData > 0d) {
                  double half = widthData / 2.0d
                  data['x'].add(center - half)
                  data['x'].add(center + half)
                }
              }
            } else {
              data['x'].addAll(posData['xmin'] ?: [])
              data['x'].addAll(posData['xmax'] ?: [])
            }
          }
          def firstX = (posData['x'] as List)?.find { it != null }
          if (!hasBounds && firstX instanceof Number) {
            // Compute x bounds for scale domain calculation using the same width resolution as GeomBoxplot.
            // Complexity: O(n) with O(1) lookups per row.
            posData.eachWithIndex { Row row, int idx ->
              def xVal = row['x']
              if (!(xVal instanceof Number)) return

              double widthData = GeomBoxplot.resolveWidthData(row, widthValue, useVarwidth, maxRelVarwidth, null)

              if (widthData > 0d) {
                double half = widthData / 2
                data['x'].add(((Number) xVal).doubleValue() - half)
                data['x'].add(((Number) xVal).doubleValue() + half)
              }
            }
          }
        }

        if (resolvedLayerAes.colorColName && posData.columnNames().contains(resolvedLayerAes.colorColName)) {
          data['color'].addAll(posData[resolvedLayerAes.colorColName] ?: [])
        }
        if (resolvedLayerAes.fillColName && posData.columnNames().contains(resolvedLayerAes.fillColName)) {
          data['fill'].addAll(posData[resolvedLayerAes.fillColName] ?: [])
        }
        if (resolvedLayerAes.shape instanceof String && posData.columnNames().contains(resolvedLayerAes.shape as String)) {
          String shapeCol = resolvedLayerAes.shape as String
          data['shape'].addAll(posData[shapeCol] ?: [])
        }
      }
    }

    // If no layers, collect from global aesthetics with original data
    if (chart.layers.isEmpty() && globalAes && chart.data) {
      if (globalAes.xColName) {
        data['x'].addAll(chart.data[globalAes.xColName] ?: [])
      }
      if (globalAes.yColName) {
        data['y'].addAll(chart.data[globalAes.yColName] ?: [])
      }
      if (globalAes.colorColName) {
        data['color'].addAll(chart.data[globalAes.colorColName] ?: [])
      }
      if (globalAes.fillColName) {
        data['fill'].addAll(chart.data[globalAes.fillColName] ?: [])
      }
      if (globalAes.shape instanceof String && chart.data.columnNames().contains(globalAes.shape as String)) {
        String shapeCol = globalAes.shape as String
        data['shape'].addAll(chart.data[shapeCol] ?: [])
      }
    }

    // For bar/column charts, ensure y-axis includes 0 (bars should start from baseline)
    if (hasBarGeom && !data['y'].isEmpty()) {
      data['y'].add(0)
    }

    return data
  }

  /**
   * Render grid lines.
   */
  private void renderGridLines(G plotArea, Map<String, Scale> scales, int width, int height, Theme theme, Coord coord) {
    if (coord instanceof CoordPolar) {
      renderPolarGridLines(plotArea, scales, theme, coord as CoordPolar)
      return
    }
    G gridGroup = plotArea.addG()
    gridGroup.id('grid')

    boolean isFlipped = coord instanceof CoordFlip

    // Major grid lines
    if (theme.panelGridMajor) {
      String color = theme.panelGridMajor.color ?: 'white'
      Number size = theme.panelGridMajor.size ?: 1

      Scale xScale = scales['x']
      Scale yScale = scales['y']

      if (isFlipped) {
        // For flipped coords: x scale controls horizontal grid lines, y scale controls vertical
        if (xScale) {
          xScale.getComputedBreaks().each { breakVal ->
            Double pos = xScale.transform(breakVal) as Double
            if (pos != null) {
              // x scale now maps to vertical position, so draw horizontal lines
              gridGroup.addLine(0, pos, width, pos)
                       .stroke(color)
                       .strokeWidth(size)
            }
          }
        }
        if (yScale) {
          yScale.getComputedBreaks().each { breakVal ->
            Double pos = yScale.transform(breakVal) as Double
            if (pos != null) {
              // y scale now maps to horizontal position, so draw vertical lines
              gridGroup.addLine(pos, 0, pos, height)
                       .stroke(color)
                       .strokeWidth(size)
            }
          }
        }
      } else {
        // Normal: x scale controls vertical lines, y scale controls horizontal lines
        if (xScale) {
          xScale.getComputedBreaks().each { breakVal ->
            Double xPos = xScale.transform(breakVal) as Double
            if (xPos != null) {
              gridGroup.addLine(xPos, 0, xPos, height)
                       .stroke(color)
                       .strokeWidth(size)
            }
          }
        }
        if (yScale) {
          yScale.getComputedBreaks().each { breakVal ->
            Double yPos = yScale.transform(breakVal) as Double
            if (yPos != null) {
              gridGroup.addLine(0, yPos, width, yPos)
                       .stroke(color)
                       .strokeWidth(size)
            }
          }
        }
      }
    }
  }

  /**
   * Render polar grid lines (spokes and rings) for theta/radius scales.
   *
   * @param plotArea plot group for the current panel
   * @param scales computed scales
   * @param theme active theme (grid line styling)
   * @param coord polar coordinate system
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

    List<Number> center = coord.getCenter()
    double cx = center[0] as double
    double cy = center[1] as double
    double maxRadius = coord.getMaxRadius()

    if (thetaScale) {
      List breaks = thetaScale.getComputedBreaks()
      List<String> labels = thetaScale.getComputedLabels()
      breaks.eachWithIndex { breakVal, int i ->
        Double norm = normalizeFromScale(thetaScale, breakVal)
        if (norm == null) return
        if (isRangeReversed(thetaScale)) {
          norm = 1.0d - norm
        }
        double angle = (coord.start as double) + norm * 2.0d * Math.PI
        if (!coord.clockwise) {
          angle = (coord.start as double) - norm * 2.0d * Math.PI
        }
        double x = cx + maxRadius * Math.sin(angle)
        double y = cy - maxRadius * Math.cos(angle)
        gridGroup.addLine(cx, cy, x, y)
                 .stroke(color)
                 .strokeWidth(size)

        String label = i < labels.size() ? labels[i] : breakVal?.toString()
        if (label != null && !label.isEmpty()) {
          double labelRadius = maxRadius + (theme.axisTickLength ?: 5)
          double lx = cx + labelRadius * Math.sin(angle)
          double ly = cy - labelRadius * Math.cos(angle)
          plotArea.addText(label)
                  .x(lx)
                  .y(ly)
                  .textAnchor('middle')
                  .fontSize(theme.axisTextY?.size ?: 9)
                  .fill(theme.axisTextY?.color ?: '#4D4D4D')
        }
      }
    }

    if (radiusScale) {
      radiusScale.getComputedBreaks().each { breakVal ->
        Double norm = normalizeFromScale(radiusScale, breakVal)
        if (norm == null) return
        double r = norm * maxRadius
        if (r <= 0.0d) return
        gridGroup.addCircle()
                 .cx(cx)
                 .cy(cy)
                 .r(r)
                 .fill('none')
                 .stroke(color)
                 .strokeWidth(size)
      }
    }

    // Always draw an outer ring to match ggplot2 polar grid appearance.
    gridGroup.addCircle()
             .cx(cx)
             .cy(cy)
             .r(maxRadius)
             .fill('none')
             .stroke(color)
             .strokeWidth(size)
  }

  /**
   * Normalize a scale-transformed value into 0..1 based on its scale range.
   *
   * @param scale scale instance
   * @param value data value to normalize
   * @return normalized value or null if the scale/value cannot be normalized
   */
  private Double normalizeFromScale(Scale scale, Object value) {
    if (scale == null) return null
    def transformed = scale.transform(value)
    if (!(transformed instanceof Number)) return null
    List<Number> range = null
    if (scale instanceof ScaleContinuous) {
      range = (scale as ScaleContinuous).range
    } else if (scale instanceof ScaleDiscrete) {
      range = (scale as ScaleDiscrete).range
    }
    if (range == null || range.size() < 2) return null
    double min = Math.min(range[0] as double, range[1] as double)
    double max = Math.max(range[0] as double, range[1] as double)
    double span = max - min
    if (span == 0.0d) return 0.0d
    return ((transformed as double) - min) / span
  }

  /**
   * Check if a scale range is reversed (high to low).
   *
   * @param scale scale instance
   * @return true when the range is reversed
   */
  private boolean isRangeReversed(Scale scale) {
    if (scale == null) return false
    List<Number> range = null
    if (scale instanceof ScaleContinuous) {
      range = (scale as ScaleContinuous).range
    } else if (scale instanceof ScaleDiscrete) {
      range = (scale as ScaleDiscrete).range
    }
    if (range == null || range.size() < 2) return false
    return (range[0] as double) > (range[1] as double)
  }

  /**
   * Render a single layer.
   */
  private void renderLayer(G dataLayer, Layer layer, GgChart chart,
                           Map<String, Scale> scales, Coord coord,
                           int plotWidth, int plotHeight) {
    // Get the data for this layer
    Matrix data = layer.data ?: chart.data
    // Merge layer aes with global aes when inheritAes is true (the default)
    Aes aes
    if (layer.inheritAes && layer.aes != null) {
      aes = layer.aes.merge(chart.globalAes)
    } else {
      aes = layer.aes ?: chart.globalAes
    }

    if (data == null || aes == null) return

    // Evaluate closure expressions in aesthetics
    EvaluatedAes evalResult = evaluateExpressions(data, aes)
    Matrix exprData = evalResult.data
    Aes resolvedAes = evalResult.aes

    // Apply statistical transformation
    Matrix statData = applyStats(exprData, resolvedAes, layer)

    // Apply position adjustment
    Matrix posData = applyPosition(statData, resolvedAes, layer)

    // Create layer group
    G layerGroup = dataLayer.addG()
    layerGroup.styleClass(layer.geom?.class?.simpleName?.toLowerCase() ?: 'layer')

    // Render the geom
    if (layer.geom) {
      layer.geom.render(layerGroup, posData, resolvedAes, scales, coord)
    }
  }

  /**
   * Apply statistical transformation to data.
   */
  private Matrix applyStats(Matrix data, Aes aes, Layer layer) {
    switch (layer.stat) {
      case StatType.IDENTITY:
        return GgStat.identity(data, aes)
      case StatType.COUNT:
        return GgStat.count(data, aes)
      case StatType.BIN:
        return GgStat.bin(data, aes, layer.statParams)
      case StatType.BOXPLOT:
        return GgStat.boxplot(data, aes, layer.statParams)
      case StatType.SMOOTH:
        return GgStat.smooth(data, aes, layer.statParams)
      case StatType.SUMMARY:
        return GgStat.summary(data, aes, layer.statParams)
      default:
        return data
    }
  }

  /**
   * Apply position adjustment to data.
   */
  private Matrix applyPosition(Matrix data, Aes aes, Layer layer) {
    switch (layer.position) {
      case PositionType.IDENTITY:
        return GgPosition.identity(data, aes)
      case PositionType.DODGE:
        return GgPosition.dodge(data, aes, layer.positionParams)
      case PositionType.DODGE2:
        return GgPosition.dodge2(data, aes, layer.positionParams)
      case PositionType.STACK:
        return GgPosition.stack(data, aes, layer.positionParams)
      case PositionType.FILL:
        return GgPosition.fill(data, aes, layer.positionParams)
      case PositionType.JITTER:
        return GgPosition.jitter(data, aes, layer.positionParams)
      default:
        return data
    }
  }

  /**
   * Render axes.
   */
  private void renderAxes(G plotArea, Map<String, Scale> scales, int width, int height, Theme theme, Coord coord) {
    if (coord instanceof CoordPolar) {
      return
    }
    G axesGroup = plotArea.addG()
    axesGroup.id('axes')

    boolean isFlipped = coord instanceof CoordFlip

    if (isFlipped) {
      // For flipped coords: x data appears on left axis, y data appears on bottom axis
      renderYAxis(axesGroup, scales['x'], height, theme)  // x scale on left (vertical)
      renderXAxis(axesGroup, scales['y'], width, height, theme)  // y scale on bottom (horizontal)
    } else {
      // Normal: x on bottom, y on left
      renderXAxis(axesGroup, scales['x'], width, height, theme)
      renderYAxis(axesGroup, scales['y'], height, theme)
    }
  }

  /**
   * Render the X axis.
   */
  private void renderXAxis(G axesGroup, Scale scale, int width, int height, Theme theme) {
    if (scale == null) return

    G xAxisGroup = axesGroup.addG()
    xAxisGroup.id('x-axis')
    xAxisGroup.transform("translate(0, $height)")

    // Axis line
    xAxisGroup.addLine(0, 0, width, 0)
              .stroke(theme.axisLineX?.color ?: 'black')
              .strokeWidth(theme.axisLineX?.size ?: 1)

    // Ticks and labels - works for both continuous and discrete scales
    List breaks = scale.getComputedBreaks()
    List<String> labels = scale.getComputedLabels()

    breaks.eachWithIndex { breakVal, i ->
      Double xPos = scale.transform(breakVal) as Double
      if (xPos == null) return  // Skip if transform returns null

      // Tick mark
      xAxisGroup.addLine(xPos, 0, xPos, theme.axisTickLength ?: 5)
                .stroke('black')

      // Label
      String label = i < labels.size() ? labels[i] : breakVal.toString()
      xAxisGroup.addText(label)
                .x(xPos)
                .y((theme.axisTickLength ?: 5) + 15)
                .textAnchor('middle')
                .fontSize(theme.axisTextX?.size ?: 10)
    }
  }

  /**
   * Render the Y axis.
   */
  private void renderYAxis(G axesGroup, Scale scale, int height, Theme theme) {
    if (scale == null) return

    G yAxisGroup = axesGroup.addG()
    yAxisGroup.id('y-axis')

    // Axis line
    yAxisGroup.addLine(0, 0, 0, height)
              .stroke(theme.axisLineY?.color ?: 'black')
              .strokeWidth(theme.axisLineY?.size ?: 1)

    // Ticks and labels - works for both continuous and discrete scales
    List breaks = scale.getComputedBreaks()
    List<String> labels = scale.getComputedLabels()

    breaks.eachWithIndex { breakVal, i ->
      Double yPos = scale.transform(breakVal) as Double
      if (yPos == null) return  // Skip if transform returns null

      // Tick mark
      yAxisGroup.addLine(-1*(theme.axisTickLength ?: 5), yPos, 0, yPos)
                .stroke('black')

      // Label
      String label = i < labels.size() ? labels[i] : breakVal.toString()
      yAxisGroup.addText(label)
                .x(-1*(theme.axisTickLength ?: 5) - 5)
                .y(yPos + 4)
                .textAnchor('end')
                .fontSize(theme.axisTextY?.size ?: 10)
    }
  }

  /**
   * Render title and labels.
   * Labels default to the aesthetic column names if not explicitly set.
   */
  private void renderLabels(Svg svg, GgChart chart, int plotWidth, int plotHeight, int svgHeight, Theme theme) {
    double titleHjust = theme?.plotTitle?.hjust != null ? (theme.plotTitle.hjust as double) : 0.0d
    double subtitleHjust = theme?.plotSubtitle?.hjust != null ?
        (theme.plotSubtitle.hjust as double) : titleHjust
    String titleAnchor = resolveTextAnchor(titleHjust)
    String subtitleAnchor = resolveTextAnchor(subtitleHjust)
    double titleX = MARGIN_LEFT + plotWidth * titleHjust
    double subtitleX = MARGIN_LEFT + plotWidth * subtitleHjust

    // Title
    String title = chart.labels?.title
    if (title) {
      svg.addText(title)
         .x(titleX)
         .y(30)
         .textAnchor(titleAnchor)
         .fontSize(16)
         .styleClass('chart-title')
    }

    // Subtitle
    String subTitle = chart.labels?.subTitle
    if (subTitle) {
      svg.addText(subTitle)
         .x(subtitleX)
         .y(50)
         .textAnchor(subtitleAnchor)
         .fontSize(12)
    }

    // X axis label - default to aesthetic x column name unless explicitly set
    String xLabel = null
    if (chart.labels?.xSet) {
      xLabel = chart.labels.x
    } else {
      xLabel = chart.globalAes?.x
    }
    if (xLabel) {
      svg.addText(xLabel)
         .x(MARGIN_LEFT + plotWidth / 2)
         .y(svgHeight - 15)
         .textAnchor('middle')
         .fontSize(12)
    }

    // Y axis label - default to aesthetic y column name unless explicitly set
    String yLabel = null
    if (chart.labels?.ySet) {
      yLabel = chart.labels.y
    } else {
      yLabel = chart.globalAes?.y
    }
    if (yLabel) {
      svg.addText(yLabel)
         .x(20)
         .y(MARGIN_TOP + plotHeight / 2)
         .textAnchor('middle')
         .fontSize(12)
         .transform("rotate(-90, 20, ${MARGIN_TOP + plotHeight / 2})")
    }
  }

  private String resolveTextAnchor(double hjust) {
    if (hjust <= 0.25d) {
      return 'start'
    }
    if (hjust >= 0.75d) {
      return 'end'
    }
    return 'middle'
  }

  /**
   * Render legend for color, fill, and other aesthetic scales.
   */
  private void renderLegend(Svg svg, Map<String, Scale> scales, GgChart chart, Theme theme) {
    // Check if legend should be shown
    if (theme.legendPosition == 'none') return

    // Find scales that need legends (color, fill, size - not x, y)
    List<String> legendAesthetics = ['color', 'colour', 'fill', 'size', 'shape']
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

    if (legendScales.isEmpty()) return

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
      case 'right':
        legendX = chart.width - MARGIN_RIGHT + LEGEND_PLOT_GAP
        legendY = MARGIN_TOP + 20
        break
      case 'left':
        legendX = 10
        legendY = MARGIN_TOP + 20
        break
      case 'top':
        legendX = MARGIN_LEFT
        legendY = 10
        isVertical = false
        break
      case 'bottom':
        legendX = MARGIN_LEFT
        legendY = chart.height - 40
        isVertical = false
        break
      default:
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

    // Create legend group
    G legendGroup = svg.addG()
    legendGroup.id('legend')
    legendGroup.transform("translate($legendX, $legendY)")

    // Get legend title from chart labels or scale name
    String legendTitle = chart.labels?.legendTitle ?: legendScales.values().first()?.name

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
      if (scale instanceof ScaleDiscrete) {
        ScaleDiscrete shapeForColorKeys = mergeShapeIntoColor &&
            (aesthetic == 'color' || aesthetic == 'colour' || aesthetic == 'fill') ? shapeScale : null
        ScaleDiscrete colorForShapes = (!mergeShapeIntoColor && aesthetic == 'shape' &&
            colorScale != null && sameDiscreteLevels(scale as ScaleDiscrete, colorScale)) ? colorScale : null
        currentY = renderDiscreteLegend(legendGroup, scale as ScaleDiscrete, aesthetic,
            currentX, currentY, isVertical, theme, usesPoints, shapeForColorKeys, colorForShapes)
      } else if (scale instanceof ScaleContinuous) {
        currentY = renderContinuousLegend(legendGroup, scale as ScaleContinuous, aesthetic,
            currentX, currentY, isVertical, theme)
      }
    }
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
            int centerX = x + (keyWidth / 2) as int
            int centerY = y + (keyHeight / 2) as int
            drawLegendShape(group, centerX, centerY,
                Math.min(keyWidth, keyHeight), shape, color, theme.legendKey?.color)
          } else {
            // Draw circle for point geoms
            int radius = Math.min(keyWidth, keyHeight) / 2 as int
            def circle = group.addCircle()
                .cx(x + keyWidth / 2 as int)
                .cy(y + keyHeight / 2 as int)
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
        int centerX = x + (keyWidth / 2) as int
        int centerY = y + (keyHeight / 2) as int
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
      group.addText(labels.first() ?: formatNumber(minVal))
          .x(x + barWidth + 5)
          .y(y + barHeight)
          .fontSize(theme.legendText?.size ?: 9)
          .fill(theme.legendText?.color ?: 'black')

      // Max at top
      group.addText(labels.last() ?: formatNumber(maxVal))
          .x(x + barWidth + 5)
          .y(y + 10)
          .fontSize(theme.legendText?.size ?: 9)
          .fill(theme.legendText?.color ?: 'black')

      return y + barHeight + spacing
    } else {
      // Min at left
      group.addText(labels.first() ?: formatNumber(minVal))
          .x(x)
          .y(y + barHeight + 12)
          .fontSize(theme.legendText?.size ?: 9)
          .fill(theme.legendText?.color ?: 'black')

      // Max at right
      group.addText(labels.last() ?: formatNumber(maxVal))
          .x(x + barWidth - 20)
          .y(y + barHeight + 12)
          .fontSize(theme.legendText?.size ?: 9)
          .fill(theme.legendText?.color ?: 'black')

      return y + barHeight + 20
    }
  }

  private void drawLegendShape(G group, int centerX, int centerY, int size,
                               String shape, String fillColor, String strokeColor) {
    String stroke = strokeColor ?: fillColor
    double halfSize = Math.max(2, size - 2) / 2.0d

    switch (shape?.toLowerCase()) {
      case 'square':
        def rect = group.addRect((halfSize * 2) as int, (halfSize * 2) as int)
            .x((centerX - halfSize) as int)
            .y((centerY - halfSize) as int)
            .fill(fillColor)
            .stroke(stroke)
        break
      case 'plus':
      case 'cross':
        group.addLine((centerX - halfSize) as int, centerY as int, (centerX + halfSize) as int, centerY as int)
            .stroke(stroke)
        group.addLine(centerX as int, (centerY - halfSize) as int, centerX as int, (centerY + halfSize) as int)
            .stroke(stroke)
        break
      case 'x':
        group.addLine((centerX - halfSize) as int, (centerY - halfSize) as int, (centerX + halfSize) as int, (centerY + halfSize) as int)
            .stroke(stroke)
        group.addLine((centerX - halfSize) as int, (centerY + halfSize) as int, (centerX + halfSize) as int, (centerY - halfSize) as int)
            .stroke(stroke)
        break
      case 'triangle':
        double h = (halfSize * 2) * Math.sqrt(3) / 2
        double topY = centerY - h * 2 / 3
        double bottomY = centerY + h / 3
        double leftX = centerX - halfSize
        double rightX = centerX + halfSize
        String pathD = "M ${centerX} ${topY as int} L ${leftX as int} ${bottomY as int} L ${rightX as int} ${bottomY as int} Z"
        group.addPath().d(pathD)
            .fill(fillColor)
            .stroke(stroke)
        break
      case 'diamond':
        String diamond = "M ${centerX} ${(centerY - halfSize) as int} " +
            "L ${(centerX + halfSize) as int} ${centerY} " +
            "L ${centerX} ${(centerY + halfSize) as int} " +
            "L ${(centerX - halfSize) as int} ${centerY} Z"
        group.addPath().d(diamond)
            .fill(fillColor)
            .stroke(stroke)
        break
      case 'circle':
      default:
        group.addCircle()
            .cx(centerX)
            .cy(centerY)
            .r(halfSize as int)
            .fill(fillColor)
            .stroke(stroke)
        break
    }
  }

  private boolean sameDiscreteLevels(ScaleDiscrete left, ScaleDiscrete right) {
    if (left == null || right == null) return false
    List<Object> leftLevels = left.levels
    List<Object> rightLevels = right.levels
    if (leftLevels == null || rightLevels == null) return false
    return leftLevels == rightLevels
  }

  /**
   * Estimate the width needed for the legend based on scales and theme.
   * Returns 0 if no legend is needed.
   */
  private int estimateLegendWidth(Map<String, Scale> scales, Theme theme) {
    if (theme.legendPosition == 'none') return 0

    // Find scales that need legends
    List<String> legendAesthetics = ['color', 'colour', 'fill', 'size', 'shape']
    Map<String, Scale> legendScales = scales.findAll { k, v ->
      legendAesthetics.contains(k) && v.isTrained()
    }

    if (legendScales.isEmpty()) return 0

    // Calculate width based on longest label
    List<Number> keySize = theme.legendKeySize ?: [15, 15] as List<Number>
    int keyWidth = keySize[0].intValue()
    int textOffset = keyWidth + 8
    int fontSize = (theme.legendText?.size ?: 10) as int

    // Estimate text width: ~0.6 * fontSize per character for typical fonts
    double charWidth = fontSize * AVERAGE_CHAR_WIDTH_RATIO
    int maxLabelWidth = 0

    legendScales.each { aesthetic, scale ->
      List<String> labels = scale.computedLabels ?: []
      labels.each { label ->
        int labelWidth = (int) (label.length() * charWidth)
        if (labelWidth > maxLabelWidth) {
          maxLabelWidth = labelWidth
        }
      }
    }

    // Total legend width: textOffset + maxLabelWidth + some padding
    return textOffset + maxLabelWidth + LEGEND_WIDTH_PADDING
  }

  // Only fall back when unset; 'none' remains a valid explicit value.
  private String resolveStripFill(Theme theme) {
    String fill = theme.stripBackground?.fill
    if (fill == null) {
      return DEFAULT_STRIP_FILL
    }
    return fill
  }

  private String resolveStripStroke(Theme theme) {
    String stroke = theme.stripBackground?.color
    if (stroke == null) {
      return DEFAULT_STRIP_STROKE
    }
    return stroke
  }

  /**
   * Format a number for legend display.
   */
  private String formatNumber(Number value) {
    if (value == null) return ''
    double d = value.doubleValue()
    if (d == Math.floor(d) && d < 1e6) {
      return String.valueOf((long) d)
    }
    if (Math.abs(d) < 0.01 || Math.abs(d) >= 1e6) {
      return String.format('%.2e', d)
    }
    return String.format('%.2f', d)
  }

  /**
   * Get the default theme (gray theme like ggplot2).
   */
  private Theme defaultTheme() {
    return se.alipsa.matrix.gg.GgPlot.theme_gray()
  }

  /**
   * Result of evaluating expressions in aesthetics.
   * Contains the modified data matrix and resolved aesthetics.
   */
  private static class EvaluatedAes {
    Matrix data
    Aes aes

    EvaluatedAes(Matrix data, Aes aes) {
      this.data = data
      this.aes = aes
    }
  }

  /** List of all aesthetic property names that can contain expressions */
  private static final List<String> ALL_AESTHETICS = [
    'x', 'y', 'color', 'fill', 'size', 'shape', 'alpha',
    'linetype', 'linewidth', 'group', 'label', 'weight'
  ]

  /**
   * Evaluate closure expressions in aesthetics and add computed columns to data.
   * Returns a new Aes with expression references replaced by column names.
   * Supports expressions in all aesthetics (x, y, color, fill, size, etc.).
   *
   * @param data The original data matrix
   * @param aes The aesthetics (may contain closures or Expression wrappers)
   * @return EvaluatedAes containing modified data and resolved aesthetics
   */
  private EvaluatedAes evaluateExpressions(Matrix data, Aes aes) {
    if (aes == null) {
      return new EvaluatedAes(data, aes)
    }

    // Check if any aesthetic is an expression, factor, or cut_width
    boolean hasExpressions = ALL_AESTHETICS.any { aes.isExpression(it) || aes.isFactor(it) || aes.isCutWidth(it) }

    if (!hasExpressions) {
      return new EvaluatedAes(data, aes)
    }

    // Clone the data matrix to avoid modifying the original
    Matrix workData = data.clone()
    Aes resolvedAes = new Aes()

    // Evaluate expressions for all aesthetics
    resolvedAes.x = evaluateAesthetic(aes, 'x', workData)
    resolvedAes.y = evaluateAesthetic(aes, 'y', workData)
    resolvedAes.color = evaluateAesthetic(aes, 'color', workData)
    resolvedAes.fill = evaluateAesthetic(aes, 'fill', workData)
    resolvedAes.size = evaluateAesthetic(aes, 'size', workData)
    resolvedAes.shape = evaluateAesthetic(aes, 'shape', workData)
    resolvedAes.alpha = evaluateAesthetic(aes, 'alpha', workData)
    resolvedAes.linetype = evaluateAesthetic(aes, 'linetype', workData)
    resolvedAes.linewidth = evaluateAesthetic(aes, 'linewidth', workData)
    resolvedAes.group = evaluateAesthetic(aes, 'group', workData)
    resolvedAes.label = evaluateAesthetic(aes, 'label', workData)
    resolvedAes.weight = evaluateAesthetic(aes, 'weight', workData)

    return new EvaluatedAes(workData, resolvedAes)
  }

  /**
   * Evaluate a single aesthetic: if it's an expression, add the computed column
   * to the data and return the column name; otherwise return the original value.
   */
  @groovy.transform.CompileDynamic
  private Object evaluateAesthetic(Aes aes, String aesthetic, Matrix workData) {
    if (aes.isFactor(aesthetic)) {
      Factor factor = aes.getFactor(aesthetic)
      return factor.addToMatrix(workData)
    }
    if (aes.isCutWidth(aesthetic)) {
      CutWidth cutWidth = aes.getCutWidth(aesthetic)
      return cutWidth.addToMatrix(workData)
    }
    def rawValue = aes."$aesthetic"
    if (rawValue instanceof List) {
      Factor factor = new Factor(rawValue as List)
      return factor.addToMatrix(workData)
    }
    if (aes.isExpression(aesthetic)) {
      Expression expr = aes.getExpression(aesthetic)
      return expr.addToMatrix(workData)
    }
    // Return the original value (could be column name, Identity, AfterStat, or null)
    return aes."$aesthetic"
  }
}
