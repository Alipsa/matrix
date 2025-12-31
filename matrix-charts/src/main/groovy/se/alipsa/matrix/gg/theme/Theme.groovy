package se.alipsa.matrix.gg.theme

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Theme configuration for ggplot charts.
 * Controls all visual styling aspects of the chart.
 */
@CompileStatic
class Theme implements Cloneable {

  // ============ Plot-level elements ============

  /** Overall plot background */
  ElementRect plotBackground

  /** Plot title styling */
  ElementText plotTitle

  /** Plot subtitle styling */
  ElementText plotSubtitle

  /** Plot caption styling */
  ElementText plotCaption

  /** Plot margins [top, right, bottom, left] in pixels */
  List<Number> plotMargin = [10, 10, 10, 10] as List<Number>

  // ============ Panel elements ============

  /** Panel (plot area) background */
  ElementRect panelBackground

  /** Panel border */
  ElementRect panelBorder

  /** Major grid lines */
  ElementLine panelGridMajor

  /** Minor grid lines */
  ElementLine panelGridMinor

  /** Panel spacing for faceted plots */
  List<Number> panelSpacing = [5, 5] as List<Number>

  // ============ Axis elements ============

  /** X-axis line */
  ElementLine axisLineX

  /** Y-axis line */
  ElementLine axisLineY

  /** X-axis tick marks */
  ElementLine axisTicksX

  /** Y-axis tick marks */
  ElementLine axisTicksY

  /** X-axis tick labels */
  ElementText axisTextX

  /** Y-axis tick labels */
  ElementText axisTextY

  /** X-axis title */
  ElementText axisTitleX

  /** Y-axis title */
  ElementText axisTitleY

  /** Tick length in pixels */
  Number axisTickLength = 5

  // ============ Legend elements ============

  /** Legend position: 'right', 'left', 'top', 'bottom', 'none', or [x, y] */
  def legendPosition = 'right'

  /** Legend direction: 'vertical' or 'horizontal' */
  String legendDirection = 'vertical'

  /** Legend background */
  ElementRect legendBackground

  /** Legend key background */
  ElementRect legendKey

  /** Legend key size [width, height] */
  List<Number> legendKeySize = [20, 20] as List<Number>

  /** Legend title styling */
  ElementText legendTitle

  /** Legend text styling */
  ElementText legendText

  /** Legend margin */
  List<Number> legendMargin = [5, 5, 5, 5] as List<Number>

  // ============ Strip elements (for facets) ============

  /** Facet strip background */
  ElementRect stripBackground

  /** Facet strip text */
  ElementText stripText

  // ============ Default colors ============

  /** Default color palette for discrete scales */
  List<String> discreteColors = [
      '#F8766D', '#00BA38', '#619CFF',  // ggplot2 defaults
      '#F564E3', '#00BFC4', '#B79F00',
      '#DE8C00', '#7CAE00', '#00B4F0',
      '#C77CFF'
  ]

  /** Default gradient colors [low, high] */
  List<String> gradientColors = ['#132B43', '#56B1F7']

  // ============ Font defaults ============

  /** Base font family */
  String baseFamily = 'sans-serif'

  /** Base font size in points */
  Number baseSize = 11

  /** Base line height */
  Number baseLineHeight = 1.2

  /** Track properties explicitly set to null (e.g., element_blank()). */
  Set<String> explicitNulls = new HashSet<>()

  @Override
  Theme clone() {
    Theme copy = super.clone() as Theme
    copy.plotMargin = copyList(plotMargin)
    copy.panelSpacing = copyList(panelSpacing)
    copy.legendKeySize = copyList(legendKeySize)
    copy.legendMargin = copyList(legendMargin)
    copy.discreteColors = copyList(discreteColors)
    copy.gradientColors = copyList(gradientColors)
    if (legendPosition instanceof List) {
      copy.legendPosition = copyList(legendPosition as List)
    }
    copy.explicitNulls = new HashSet<>(explicitNulls)

    copy.plotBackground = cloneRect(plotBackground)
    copy.plotTitle = cloneText(plotTitle)
    copy.plotSubtitle = cloneText(plotSubtitle)
    copy.plotCaption = cloneText(plotCaption)

    copy.panelBackground = cloneRect(panelBackground)
    copy.panelBorder = cloneRect(panelBorder)
    copy.panelGridMajor = cloneLine(panelGridMajor)
    copy.panelGridMinor = cloneLine(panelGridMinor)

    copy.axisLineX = cloneLine(axisLineX)
    copy.axisLineY = cloneLine(axisLineY)
    copy.axisTicksX = cloneLine(axisTicksX)
    copy.axisTicksY = cloneLine(axisTicksY)
    copy.axisTextX = cloneText(axisTextX)
    copy.axisTextY = cloneText(axisTextY)
    copy.axisTitleX = cloneText(axisTitleX)
    copy.axisTitleY = cloneText(axisTitleY)

    copy.legendBackground = cloneRect(legendBackground)
    copy.legendKey = cloneRect(legendKey)
    copy.legendTitle = cloneText(legendTitle)
    copy.legendText = cloneText(legendText)

    copy.stripBackground = cloneRect(stripBackground)
    copy.stripText = cloneText(stripText)

    return copy
  }

  private static <T> List<T> copyList(List<T> source) {
    if (source == null) {
      return null
    }
    return new ArrayList<>(source) as List<T>
  }

  private static ElementText cloneText(ElementText text) {
    if (text == null) {
      return null
    }
    ElementText copy = new ElementText()
    copy.family = text.family
    copy.face = text.face
    copy.size = text.size
    copy.color = text.color
    copy.hjust = text.hjust
    copy.vjust = text.vjust
    copy.angle = text.angle
    copy.lineheight = text.lineheight
    copy.margin = copyList(text.margin)
    return copy
  }

  private static ElementLine cloneLine(ElementLine line) {
    if (line == null) {
      return null
    }
    ElementLine copy = new ElementLine()
    copy.color = line.color
    copy.size = line.size
    copy.linetype = line.linetype
    copy.lineend = line.lineend
    return copy
  }

  private static ElementRect cloneRect(ElementRect rect) {
    if (rect == null) {
      return null
    }
    ElementRect copy = new ElementRect()
    copy.fill = rect.fill
    copy.color = rect.color
    copy.size = rect.size
    copy.linetype = rect.linetype
    return copy
  }

  /**
   * Create a copy of this theme with modifications.
   */
  Theme plus(Theme other) {
    Theme merged = this.clone() as Theme
    // Merge non-null properties from other
    other.properties.each { key, value ->
      if (value != null && key != 'class' && key != 'explicitNulls') {
        merged.setProperty(key as String, value)
        merged.explicitNulls.remove(key as String)
      }
    }
    if (other.explicitNulls) {
      other.explicitNulls.each { key ->
        if (merged.hasProperty(key as String)) {
          merged.setProperty(key as String, null)
          merged.explicitNulls.add(key as String)
        }
      }
    }
    return merged
  }

  /**
   * Modify specific theme elements.
   */
  Theme plus(Map modifications) {
    Theme modified = this.clone() as Theme
    modifications.each { key, value ->
      String k = key as String
      if (modified.hasProperty(k)) {
        modified.setProperty(k, value)
        if (value == null) {
          modified.explicitNulls.add(k)
        } else {
          modified.explicitNulls.remove(k)
        }
      }
    }
    return modified
  }
}

/**
 * Text element styling.
 */
@CompileStatic
class ElementText {
  String family
  String face  // 'plain', 'italic', 'bold', 'bold.italic'
  Number size
  String color
  Number hjust = 0.5  // Horizontal justification (0=left, 0.5=center, 1=right)
  Number vjust = 0.5  // Vertical justification
  Number angle = 0    // Rotation angle in degrees
  Number lineheight
  List<Number> margin = [0, 0, 0, 0] as List<Number>

  ElementText() {}

  ElementText(Map params) {
    params.each { key, value ->
      String k = key as String
      if (k == 'colour') k = 'color'
      if (this.hasProperty(k)) {
        this.setProperty(k, value)
      }
    }
  }
}

/**
 * Line element styling.
 */
@CompileStatic
class ElementLine {
  String color = 'black'
  Number size = 1        // Line width in pixels
  String linetype = 'solid'  // 'solid', 'dashed', 'dotted', 'dotdash', 'longdash', 'twodash'
  String lineend = 'butt'    // 'butt', 'round', 'square'

  ElementLine() {}

  ElementLine(Map params) {
    params.each { key, value ->
      String k = key as String
      // Handle aliases
      if (k == 'colour') k = 'color'
      else if (k == 'linewidth') k = 'size'

      if (this.hasProperty(k)) {
        this.setProperty(k, value)
      }
    }
  }
}

/**
 * Rectangle element styling.
 */
@CompileStatic
class ElementRect {
  String fill = 'white'
  String color = 'black'  // Border color
  Number size = 1         // Border width
  String linetype = 'solid'

  ElementRect() {}

  @CompileDynamic
  ElementRect(Map params) {
    params.each { key, value ->
      if (this.hasProperty(key as String)) {
        this."$key" = value
      }
    }
  }
}

/**
 * Blank element - removes the element entirely.
 */
@CompileStatic
class ElementBlank {
  // Marker class - presence indicates element should not be drawn
}
