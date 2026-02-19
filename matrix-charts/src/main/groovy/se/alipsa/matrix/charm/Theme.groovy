package se.alipsa.matrix.charm

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.theme.ElementLine
import se.alipsa.matrix.charm.theme.ElementRect
import se.alipsa.matrix.charm.theme.ElementText

/**
 * Theme configuration with typed element fields mirroring the gg theme model.
 *
 * Replaces the previous map-based approach with strongly typed element classes
 * for plot, panel, axis, legend, strip, and text styling.
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
  List<Number> plotMargin = [10, 10, 10, 10]

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
  List<Number> panelSpacing = [5, 5]

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
  List<Number> legendKeySize = [20, 20]

  /** Legend title styling */
  ElementText legendTitle

  /** Legend text styling */
  ElementText legendText

  /** Legend margin */
  List<Number> legendMargin = [5, 5, 5, 5]

  // ============ Strip elements (for facets) ============

  /** Facet strip background */
  ElementRect stripBackground

  /** Facet strip text */
  ElementText stripText

  // ============ Default colors ============

  /** Default color palette for discrete scales */
  List<String> discreteColors = [
      '#F8766D', '#C49A00', '#53B400',
      '#00C094', '#00B6EB', '#A58AFF',
      '#FB61D7'
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

  /** Theme name for identification (e.g., 'gray', 'minimal', 'dark') */
  String themeName

  /** Track properties explicitly set to null (e.g., element_blank()). */
  Set<String> explicitNulls = new HashSet<>()

  /**
   * Copies this theme.
   *
   * @return copied theme
   */
  Theme copy() {
    Theme copy = new Theme()
    copy.plotBackground = plotBackground?.copy()
    copy.plotTitle = plotTitle?.copy()
    copy.plotSubtitle = plotSubtitle?.copy()
    copy.plotCaption = plotCaption?.copy()
    copy.plotMargin = plotMargin != null ? new ArrayList<>(plotMargin) : null

    copy.panelBackground = panelBackground?.copy()
    copy.panelBorder = panelBorder?.copy()
    copy.panelGridMajor = panelGridMajor?.copy()
    copy.panelGridMinor = panelGridMinor?.copy()
    copy.panelSpacing = panelSpacing != null ? new ArrayList<>(panelSpacing) : null

    copy.axisLineX = axisLineX?.copy()
    copy.axisLineY = axisLineY?.copy()
    copy.axisTicksX = axisTicksX?.copy()
    copy.axisTicksY = axisTicksY?.copy()
    copy.axisTextX = axisTextX?.copy()
    copy.axisTextY = axisTextY?.copy()
    copy.axisTitleX = axisTitleX?.copy()
    copy.axisTitleY = axisTitleY?.copy()
    copy.axisTickLength = axisTickLength

    copy.legendPosition = legendPosition instanceof List
        ? new ArrayList<>(legendPosition as List) : legendPosition
    copy.legendDirection = legendDirection
    copy.legendBackground = legendBackground?.copy()
    copy.legendKey = legendKey?.copy()
    copy.legendKeySize = legendKeySize != null ? new ArrayList<>(legendKeySize) : null
    copy.legendTitle = legendTitle?.copy()
    copy.legendText = legendText?.copy()
    copy.legendMargin = legendMargin != null ? new ArrayList<>(legendMargin) : null

    copy.stripBackground = stripBackground?.copy()
    copy.stripText = stripText?.copy()

    copy.discreteColors = discreteColors != null ? new ArrayList<>(discreteColors) : null
    copy.gradientColors = gradientColors != null ? new ArrayList<>(gradientColors) : null

    copy.baseFamily = baseFamily
    copy.baseSize = baseSize
    copy.baseLineHeight = baseLineHeight
    copy.themeName = themeName
    copy.explicitNulls = new HashSet<>(explicitNulls)
    copy
  }

  /**
   * Merges another theme into this one. Non-null fields from the other theme
   * override fields in this theme. Explicit nulls from the other theme null
   * out corresponding fields.
   *
   * @param other theme to merge
   * @return new merged theme
   */
  Theme plus(Theme other) {
    if (other == null) {
      return this.copy()
    }
    Theme merged = this.copy()
    mergeField(merged, other, 'plotBackground')
    mergeField(merged, other, 'plotTitle')
    mergeField(merged, other, 'plotSubtitle')
    mergeField(merged, other, 'plotCaption')
    mergeField(merged, other, 'plotMargin')
    mergeField(merged, other, 'panelBackground')
    mergeField(merged, other, 'panelBorder')
    mergeField(merged, other, 'panelGridMajor')
    mergeField(merged, other, 'panelGridMinor')
    mergeField(merged, other, 'panelSpacing')
    mergeField(merged, other, 'axisLineX')
    mergeField(merged, other, 'axisLineY')
    mergeField(merged, other, 'axisTicksX')
    mergeField(merged, other, 'axisTicksY')
    mergeField(merged, other, 'axisTextX')
    mergeField(merged, other, 'axisTextY')
    mergeField(merged, other, 'axisTitleX')
    mergeField(merged, other, 'axisTitleY')
    mergeField(merged, other, 'axisTickLength')
    mergeField(merged, other, 'legendPosition')
    mergeField(merged, other, 'legendDirection')
    mergeField(merged, other, 'legendBackground')
    mergeField(merged, other, 'legendKey')
    mergeField(merged, other, 'legendKeySize')
    mergeField(merged, other, 'legendTitle')
    mergeField(merged, other, 'legendText')
    mergeField(merged, other, 'legendMargin')
    mergeField(merged, other, 'stripBackground')
    mergeField(merged, other, 'stripText')
    mergeField(merged, other, 'discreteColors')
    mergeField(merged, other, 'gradientColors')
    mergeField(merged, other, 'baseFamily')
    mergeField(merged, other, 'baseSize')
    mergeField(merged, other, 'baseLineHeight')
    mergeField(merged, other, 'themeName')
    merged
  }

  private static void mergeField(Theme merged, Theme other, String fieldName) {
    if (other.explicitNulls.contains(fieldName)) {
      merged.setProperty(fieldName, null)
      merged.explicitNulls.add(fieldName)
    } else {
      Object value = other.getProperty(fieldName)
      if (value != null) {
        merged.setProperty(fieldName, value)
        merged.explicitNulls.remove(fieldName)
      }
    }
  }
}
