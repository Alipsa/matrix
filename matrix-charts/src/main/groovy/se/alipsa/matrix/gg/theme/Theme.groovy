package se.alipsa.matrix.gg.theme

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Theme configuration for ggplot charts.
 * Controls all visual styling aspects of the chart.
 */
@CompileStatic
class Theme {

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

  /**
   * Create a copy of this theme with modifications.
   */
  Theme plus(Theme other) {
    Theme merged = this.clone() as Theme
    // Merge non-null properties from other
    other.properties.each { key, value ->
      if (value != null && key != 'class') {
        //merged."$key" = value
        merged[key as String] = value
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
      if (modified.hasProperty(key as String)) {
        //modified."$key" = value
        modified[key as String] = value
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
      if (this.hasProperty(key as String)) {
        //this."$key" = value
        this[key as String] = value
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
      if (this.hasProperty(key as String)) {
        this[key as String] = value
        //this."$key" = value
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
