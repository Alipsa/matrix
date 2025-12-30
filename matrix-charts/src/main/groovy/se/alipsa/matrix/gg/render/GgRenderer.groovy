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
import se.alipsa.matrix.gg.layer.Layer
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.position.GgPosition
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleContinuous
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

    // 3. Compute scales from data
    Map<String, Scale> computedScales = computeScales(chart, plotWidth, plotHeight)

    // 4. Setup coordinate system
    Coord coord = chart.coord ?: new CoordCartesian()
    if (coord instanceof CoordCartesian) {
      coord.plotWidth = plotWidth
      coord.plotHeight = plotHeight
    }

    // 5. Draw grid lines (before data)
    renderGridLines(plotArea, computedScales, plotWidth, plotHeight, theme)

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
    renderAxes(plotArea, computedScales, plotWidth, plotHeight, theme)

    // 9. Draw title and labels
    renderLabels(svg, chart, plotWidth, plotHeight)

    // 10. Draw legend (if needed)
    renderLegend(svg, computedScales, chart, theme)

    return svg
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
  private Map<String, Scale> computeScales(GgChart chart, int plotWidth, int plotHeight) {
    Map<String, Scale> scales = [:]

    // Collect all data values for each aesthetic
    Map<String, List> aestheticData = collectAestheticData(chart)

    // Create x scale
    if (aestheticData.x) {
      Scale xScale = new ScaleContinuous()
      xScale.aesthetic = 'x'
      xScale.train(aestheticData.x)
      xScale.range = [0, plotWidth] as List<Number>
      scales['x'] = xScale
    }

    // Create y scale
    if (aestheticData.y) {
      Scale yScale = new ScaleContinuous()
      yScale.aesthetic = 'y'
      yScale.train(aestheticData.y)
      // Y axis is inverted in SVG (0 at top)
      yScale.range = [plotHeight, 0] as List<Number>
      scales['y'] = yScale
    }

    // Add user-specified scales
    chart.scales.each { scale ->
      if (scale.aesthetic) {
        scales[scale.aesthetic] = scale
      }
    }

    return scales
  }

  /**
   * Collect data values for each aesthetic across all layers.
   */
  private Map<String, List> collectAestheticData(GgChart chart) {
    Map<String, List> data = [:].withDefault { [] }

    Aes globalAes = chart.globalAes

    // Collect from global aesthetics
    if (globalAes) {
      if (globalAes.xColName && chart.data) {
        data['x'].addAll(chart.data[globalAes.xColName] ?: [])
      }
      if (globalAes.yColName && chart.data) {
        data['y'].addAll(chart.data[globalAes.yColName] ?: [])
      }
    }

    // Collect from each layer
    chart.layers.each { layer ->
      Matrix layerData = layer.data ?: chart.data
      Aes layerAes = layer.aes ?: globalAes

      if (layerAes && layerData) {
        if (layerAes.xColName) {
          data['x'].addAll(layerData[layerAes.xColName] ?: [])
        }
        if (layerAes.yColName) {
          data['y'].addAll(layerData[layerAes.yColName] ?: [])
        }
      }
    }

    return data
  }

  /**
   * Render grid lines.
   */
  private void renderGridLines(G plotArea, Map<String, Scale> scales, int width, int height, Theme theme) {
    G gridGroup = plotArea.addG()
    gridGroup.id('grid')

    // Major grid lines
    if (theme.panelGridMajor) {
      String color = theme.panelGridMajor.color ?: 'white'
      Number size = theme.panelGridMajor.size ?: 1

      // Vertical grid lines (x axis)
      Scale xScale = scales['x']
      if (xScale instanceof ScaleContinuous) {
        xScale.getComputedBreaks().each { breakVal ->
          double xPos = xScale.transform(breakVal) as double
          gridGroup.addLine(xPos, 0, xPos, height)
                   .stroke(color)
                   .strokeWidth(size)
        }
      }

      // Horizontal grid lines (y axis)
      Scale yScale = scales['y']
      if (yScale instanceof ScaleContinuous) {
        yScale.getComputedBreaks().each { breakVal ->
          double yPos = yScale.transform(breakVal) as double
          gridGroup.addLine(0, yPos, width, yPos)
                   .stroke(color)
                   .strokeWidth(size)
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
  private void renderAxes(G plotArea, Map<String, Scale> scales, int width, int height, Theme theme) {
    G axesGroup = plotArea.addG()
    axesGroup.id('axes')

    // X axis
    renderXAxis(axesGroup, scales['x'], width, height, theme)

    // Y axis
    renderYAxis(axesGroup, scales['y'], height, theme)
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

    // Ticks and labels
    if (scale instanceof ScaleContinuous) {
      List breaks = scale.getComputedBreaks()
      List<String> labels = scale.getComputedLabels()

      breaks.eachWithIndex { breakVal, i ->
        double xPos = scale.transform(breakVal) as double

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

    // Ticks and labels
    if (scale instanceof ScaleContinuous) {
      List breaks = scale.getComputedBreaks()
      List<String> labels = scale.getComputedLabels()

      breaks.eachWithIndex { breakVal, i ->
        double yPos = scale.transform(breakVal) as double

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
   * Render legend.
   */
  private void renderLegend(Svg svg, Map<String, Scale> scales, GgChart chart, Theme theme) {
    // TODO: Implement legend rendering based on color/fill/size scales
    // For now, just a placeholder
  }

  /**
   * Get the default theme (gray theme like ggplot2).
   */
  private Theme defaultTheme() {
    return se.alipsa.matrix.gg.GgPlot.theme_gray()
  }
}
