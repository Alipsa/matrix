package se.alipsa.matrix.gg.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Defs
import se.alipsa.groovy.svg.G
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.coord.CoordCartesian
import se.alipsa.matrix.gg.coord.CoordFlip
import se.alipsa.matrix.gg.coord.CoordPolar
import se.alipsa.matrix.gg.facet.Facet
import se.alipsa.matrix.gg.facet.FacetGrid
import se.alipsa.matrix.gg.facet.FacetWrap
import se.alipsa.matrix.gg.layer.Layer
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.position.GgPosition
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleContinuous
import se.alipsa.matrix.gg.scale.ScaleDiscrete
import se.alipsa.matrix.gg.scale.ScaleXContinuous
import se.alipsa.matrix.gg.scale.ScaleYContinuous
import se.alipsa.matrix.gg.scale.ScaleXDiscrete
import se.alipsa.matrix.gg.scale.ScaleYDiscrete
import se.alipsa.matrix.gg.stat.GgStat
import se.alipsa.matrix.gg.theme.Theme

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
    Svg svg = new Svg()
    svg.width(chart.width)
    svg.height(chart.height)
    svg.viewBox("0 0 ${chart.width} ${chart.height}")

    Theme theme = chart.theme ?: defaultTheme()

    // Calculate plot area dimensions
    int plotX = MARGIN_LEFT
    int plotY = MARGIN_TOP
    int plotWidth = chart.width - MARGIN_LEFT - MARGIN_RIGHT
    int plotHeight = chart.height - MARGIN_TOP - MARGIN_BOTTOM

    // 1. Draw background
    renderBackground(svg, chart, theme)

    // 2. Setup plot area group
    G plotArea = svg.addG()
    plotArea.id('plot-area')
    plotArea.transform("translate($plotX, $plotY)")

    // Draw panel background
    renderPanelBackground(plotArea, plotWidth, plotHeight, theme)

    // 3. Setup coordinate system (needed before computing scales for CoordFlip/CoordPolar)
    Coord coord = chart.coord ?: new CoordCartesian()
    if (coord instanceof CoordCartesian) {
      coord.plotWidth = plotWidth
      coord.plotHeight = plotHeight
    } else if (coord instanceof CoordFlip) {
      coord.plotWidth = plotWidth
      coord.plotHeight = plotHeight
    } else if (coord instanceof CoordPolar) {
      coord.plotWidth = plotWidth
      coord.plotHeight = plotHeight
    }

    // 4. Compute scales from data (pass coord for flip handling)
    Map<String, Scale> computedScales = computeScales(chart, plotWidth, plotHeight, coord)

    // 5. Draw grid lines (before data)
    renderGridLines(plotArea, computedScales, plotWidth, plotHeight, theme, coord)

    // 6. Create data layer group
    G dataLayer = plotArea.addG()
    dataLayer.id('data-layer')
    Defs defs = svg.addDefs()
    def clipPath = defs.addClipPath().id('plot-clip')
    clipPath.addRect(plotWidth, plotHeight).x(0).y(0)
    dataLayer.addAttribute('clip-path', 'url(#plot-clip)')

    // 7. Render each layer
    chart.layers.each { layer ->
      renderLayer(dataLayer, layer, chart, computedScales, coord, plotWidth, plotHeight)
    }

    // 8. Draw axes (on top of data)
    renderAxes(plotArea, computedScales, plotWidth, plotHeight, theme, coord)

    // 9. Draw title and labels
    renderLabels(svg, chart, plotWidth, plotHeight)

    // 10. Draw legend (if needed)
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
    renderBackground(svg, chart, theme)

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
    int stripHeight = facet.strip ? 20 : 0
    int totalPlotWidth = chart.width - MARGIN_LEFT - MARGIN_RIGHT
    int totalPlotHeight = chart.height - MARGIN_TOP - MARGIN_BOTTOM

    // Account for spacing between panels
    int panelSpacing = facet.panelSpacing
    int availableWidth = totalPlotWidth - (ncol - 1) * panelSpacing
    int availableHeight = totalPlotHeight - (nrow - 1) * panelSpacing - nrow * stripHeight

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
      } else if (facet instanceof FacetGrid) {
        FacetGrid fg = facet as FacetGrid
        row = fg.getRowIndex(panelValues, chart.data)
        col = fg.getColIndex(panelValues, chart.data)
      } else {
        row = panelIdx / ncol as int
        col = panelIdx % ncol
      }

      // Calculate panel position in pixels
      int panelX = MARGIN_LEFT + col * (panelWidth + panelSpacing)
      int panelY = MARGIN_TOP + row * (panelHeight + stripHeight + panelSpacing)

      // Filter data for this panel
      Matrix panelData = facet.filterDataForPanel(chart.data, panelValues)

      // Create panel group
      G panelGroup = svg.addG()
      panelGroup.id("panel-${row}-${col}")
      panelGroup.transform("translate($panelX, $panelY)")

      // Render strip label (if enabled)
      if (facet.strip) {
        renderFacetStrip(panelGroup, facet.getPanelLabel(panelValues), panelWidth, stripHeight, theme)
      }

      // Create panel content group (below strip)
      G contentGroup = panelGroup.addG()
      contentGroup.transform("translate(0, $stripHeight)")

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

      // Draw grid lines
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

    // 8. Draw title and labels
    renderLabels(svg, chart, totalPlotWidth, totalPlotHeight)

    // 9. Draw legend (if needed)
    renderLegend(svg, globalScales, chart, theme)

    return svg
  }

  /**
   * Render a facet strip label.
   */
  private void renderFacetStrip(G group, String label, int width, int height, Theme theme) {
    // Draw strip background
    group.addRect(width, height)
        .fill(theme.stripBackground?.fill ?: '#E0E0E0')
        .stroke(theme.stripBackground?.color ?: 'none')

    // Draw strip text
    group.addText(label)
        .x(width / 2)
        .y(height / 2 + 4)
        .textAnchor('middle')
        .fontSize(theme.stripText?.size ?: 10)
        .fill(theme.stripText?.color ?: 'black')
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
    Aes aes = layer.aes ?: globalAes

    if (data == null || data.rowCount() == 0 || aes == null) return

    // Apply statistical transformation
    Matrix statData = applyStats(data, aes, layer)

    // Apply position adjustment
    Matrix posData = applyPosition(statData, aes, layer)

    // Create layer group
    G layerGroup = dataLayer.addG()
    layerGroup.styleClass(layer.geom?.class?.simpleName?.toLowerCase() ?: 'layer')

    // Render the geom
    if (layer.geom) {
      layer.geom.render(layerGroup, posData, aes, scales, coord)
    }
  }

  /**
   * Render background elements.
   */
  private void renderBackground(Svg svg, GgChart chart, Theme theme) {
    if (theme.plotBackground) {
      svg.addRect(chart.width, chart.height)
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

    // For CoordFlip: x data maps to vertical axis, y data maps to horizontal axis
    // Normal: x -> [0, plotWidth], y -> [plotHeight, 0] (inverted for SVG)
    // Flipped: x -> [plotHeight, 0], y -> [0, plotWidth]

    List<Number> xRange = isFlipped ? [plotHeight, 0] as List<Number> : [0, plotWidth] as List<Number>
    List<Number> yRange = isFlipped ? [0, plotWidth] as List<Number> : [plotHeight, 0] as List<Number>

    // Create x scale (auto-detect discrete vs continuous)
    if (aestheticData.x) {
      Scale xScale = createAutoScale('x', aestheticData.x)
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
      scales['color'] = colorScale
    }

    // Create fill scale if fill data exists
    if (aestheticData.fill) {
      Scale fillScale = createAutoScale('fill', aestheticData.fill)
      fillScale.train(aestheticData.fill)
      scales['fill'] = fillScale
    }

    // Add user-specified scales (these override auto-detected scales)
    chart.scales.each { scale ->
      if (scale.aesthetic) {
        // Train user scale if not already trained
        if (!scale.isTrained() && aestheticData[scale.aesthetic]) {
          scale.train(aestheticData[scale.aesthetic])
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
   * Auto-detect and create the appropriate scale type based on data.
   */
  private Scale createAutoScale(String aesthetic, List data) {
    boolean isDiscrete = isDiscreteData(data)

    switch (aesthetic) {
      case 'x':
        return isDiscrete ? new ScaleXDiscrete() : new ScaleXContinuous()
      case 'y':
        return isDiscrete ? new ScaleYDiscrete() : new ScaleYContinuous()
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
      Aes layerAes = layer.aes ?: globalAes

      // Check if this is a bar/column geom that needs y-axis to include 0
      if (layer.geom?.class?.simpleName in ['GeomBar', 'GeomCol', 'GeomHistogram']) {
        hasBarGeom = true
      }

      if (layerAes && layerData) {
        // Apply stat transformation to get computed values
        Matrix statData = applyStats(layerData, layerAes, layer)

        // For x aesthetic: use xmin/xmax for histograms, otherwise use x column
        if (statData.columnNames().contains('xmin') && statData.columnNames().contains('xmax')) {
          // stat_bin produces xmin/xmax columns - include both for full range
          data['x'].addAll(statData['xmin'] ?: [])
          data['x'].addAll(statData['xmax'] ?: [])
        } else if (layerAes.xColName && statData.columnNames().contains(layerAes.xColName)) {
          data['x'].addAll(statData[layerAes.xColName] ?: [])
        }

        // For y aesthetic, check both the original y column and computed columns like 'count'
        if (statData.columnNames().contains('ymin') && statData.columnNames().contains('ymax')) {
          // stat_boxplot produces ymin/ymax columns - include both for full y range
          data['y'].addAll(statData['ymin'] ?: [])
          data['y'].addAll(statData['ymax'] ?: [])
        } else if (layerAes.isAfterStat('y')) {
          // Explicit after_stat() reference - use the specified computed column
          String statCol = layerAes.getAfterStatName('y')
          if (statData.columnNames().contains(statCol)) {
            data['y'].addAll(statData[statCol] ?: [])
          }
        } else if (layerAes.yColName && statData.columnNames().contains(layerAes.yColName)) {
          data['y'].addAll(statData[layerAes.yColName] ?: [])
        } else if (statData.columnNames().contains('count')) {
          // stat_count and stat_bin produce 'count' column for y values (default behavior)
          data['y'].addAll(statData['count'] ?: [])
        }

        // For boxplot x-axis, use the computed 'x' column (group keys)
        if (statData.columnNames().contains('x') && !statData.columnNames().contains('xmin')) {
          // stat_boxplot produces 'x' column with group names (but not xmin/xmax like histograms)
          data['x'].addAll(statData['x'] ?: [])
        }

        if (layerAes.colorColName && statData.columnNames().contains(layerAes.colorColName)) {
          data['color'].addAll(statData[layerAes.colorColName] ?: [])
        }
        if (layerAes.fillColName && statData.columnNames().contains(layerAes.fillColName)) {
          data['fill'].addAll(statData[layerAes.fillColName] ?: [])
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
   * Render a single layer.
   */
  private void renderLayer(G dataLayer, Layer layer, GgChart chart,
                           Map<String, Scale> scales, Coord coord,
                           int plotWidth, int plotHeight) {
    // Get the data for this layer
    Matrix data = layer.data ?: chart.data
    Aes aes = layer.aes ?: chart.globalAes

    if (data == null || aes == null) return

    // Apply statistical transformation
    Matrix statData = applyStats(data, aes, layer)

    // Apply position adjustment
    Matrix posData = applyPosition(statData, aes, layer)

    // Create layer group
    G layerGroup = dataLayer.addG()
    layerGroup.styleClass(layer.geom?.class?.simpleName?.toLowerCase() ?: 'layer')

    // Render the geom
    if (layer.geom) {
      layer.geom.render(layerGroup, posData, aes, scales, coord)
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
        return GgStat.boxplot(data, aes)
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
   */
  private void renderLabels(Svg svg, GgChart chart, int plotWidth, int plotHeight) {
    if (chart.labels == null) return

    // Title
    if (chart.labels.title) {
      svg.addText(chart.labels.title)
         .x(MARGIN_LEFT + plotWidth / 2)
         .y(30)
         .textAnchor('middle')
         .fontSize(16)
         .styleClass('chart-title')
    }

    // Subtitle
    if (chart.labels.subTitle) {
      svg.addText(chart.labels.subTitle)
         .x(MARGIN_LEFT + plotWidth / 2)
         .y(50)
         .textAnchor('middle')
         .fontSize(12)
    }

    // X axis label
    if (chart.labels.x) {
      svg.addText(chart.labels.x)
         .x(MARGIN_LEFT + plotWidth / 2)
         .y(chart.height - 15)
         .textAnchor('middle')
         .fontSize(12)
    }

    // Y axis label
    if (chart.labels.y) {
      svg.addText(chart.labels.y)
         .x(20)
         .y(MARGIN_TOP + plotHeight / 2)
         .textAnchor('middle')
         .fontSize(12)
         .transform("rotate(-90, 20, ${MARGIN_TOP + plotHeight / 2})")
    }
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

    // Calculate legend position
    int plotWidth = chart.width - MARGIN_LEFT - MARGIN_RIGHT
    int plotHeight = chart.height - MARGIN_TOP - MARGIN_BOTTOM

    int legendX, legendY
    boolean isVertical = theme.legendDirection != 'horizontal'

    // Determine legend position
    switch (theme.legendPosition) {
      case 'right':
        legendX = chart.width - MARGIN_RIGHT + 10
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
          legendX = chart.width - MARGIN_RIGHT + 10
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
      def titleText = legendGroup.addText(legendTitle)
          .x(0)
          .y(currentY)
          .addAttribute('font-weight', 'bold')
          .fontSize(theme.legendTitle?.size ?: 11)
      if (theme.legendTitle?.color) {
        titleText.fill(theme.legendTitle.color)
      }
      currentY += 18
    }

    // Render each legend scale
    legendScales.each { aesthetic, scale ->
      if (scale instanceof ScaleDiscrete) {
        currentY = renderDiscreteLegend(legendGroup, scale as ScaleDiscrete, aesthetic,
            currentX, currentY, isVertical, theme)
      } else if (scale instanceof ScaleContinuous) {
        currentY = renderContinuousLegend(legendGroup, scale as ScaleContinuous, aesthetic,
            currentX, currentY, isVertical, theme)
      }
    }
  }

  /**
   * Render legend for a discrete scale (color categories).
   */
  private int renderDiscreteLegend(G group, ScaleDiscrete scale, String aesthetic,
                                   int startX, int startY, boolean vertical, Theme theme) {
    List<Number> keySize = theme.legendKeySize ?: [15, 15] as List<Number>
    int keyWidth = keySize[0].intValue()
    int keyHeight = keySize[1].intValue()
    int spacing = 5
    int textOffset = keyWidth + 8

    List<Object> levels = scale.levels
    List<String> labels = scale.computedLabels

    int x = startX
    int y = startY

    levels.eachWithIndex { level, int idx ->
      // Get color for this level
      String color = scale.transform(level)?.toString() ?: '#999999'
      String label = idx < labels.size() ? labels[idx] : level?.toString() ?: ''

      // Draw key (colored rectangle or circle)
      if (aesthetic == 'color' || aesthetic == 'colour' || aesthetic == 'fill') {
        def rect = group.addRect(keyWidth, keyHeight)
            .x(x)
            .y(y)
            .fill(color)
        if (theme.legendKey?.color) {
          rect.stroke(theme.legendKey.color)
        }
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
    // For continuous scales, render a gradient color bar
    int barWidth = vertical ? 15 : 100
    int barHeight = vertical ? 80 : 15
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
}
