package se.alipsa.matrix.gg.theme

class Themes {

  /**
   * Default gray theme (ggplot2 default).
   */
  static Theme gray() {
    Theme theme = new Theme()
    theme.themeName = 'gray'
    theme.panelBackground = new ElementRect(fill: '#EBEBEB', color: null)
    theme.panelGridMajor = new ElementLine(color: 'white', size: 1)
    theme.panelGridMinor = new ElementLine(color: 'white', size: 0.5)
    theme.plotBackground = new ElementRect(fill: 'white', color: null)
    return theme
  }

  /**
   * Classic theme with axis lines and no grid.
   */
  static Theme classic() {
    Theme theme = new Theme()
    theme.themeName = 'classic'
    theme.panelBackground = new ElementRect(fill: 'white', color: null)
    theme.panelGridMajor = null  // No grid
    theme.panelGridMinor = null
    theme.explicitNulls.add('panelGridMajor')
    theme.explicitNulls.add('panelGridMinor')
    theme.plotBackground = new ElementRect(fill: 'white', color: null)
    theme.axisLineX = new ElementLine(color: 'black', size: 1)
    theme.axisLineY = new ElementLine(color: 'black', size: 1)
    return theme
  }

  /**
   * Black and white theme.
   */
  static Theme bw() {
    Theme theme = new Theme()
    theme.themeName = 'bw'
    theme.panelBackground = new ElementRect(fill: 'white', color: 'black')
    theme.panelGridMajor = new ElementLine(color: '#D3D3D3', size: 0.5)
    theme.panelGridMinor = new ElementLine(color: '#E5E5E5', size: 0.25)
    theme.plotBackground = new ElementRect(fill: 'white', color: null)
    return theme
  }

  /**
   * Minimal theme with no background annotations.
   * In ggplot2, theme_minimal has:
   * - panel.background = element_blank() (transparent)
   * - strip.background = element_blank() (transparent)
   * - plot.background = element_blank() (transparent)
   * - axis lines are light gray
   */
  static Theme minimal() {
    Theme theme = new Theme()
    theme.themeName = 'minimal'
    // Transparent backgrounds like ggplot2's theme_minimal
    theme.panelBackground = new ElementRect(fill: 'none', color: null)
    theme.panelGridMajor = new ElementLine(color: '#D3D3D3', size: 0.5)
    theme.panelGridMinor = null  // No minor grid
    theme.explicitNulls.add('panelGridMinor')
    theme.plotBackground = new ElementRect(fill: 'none', color: null)
    theme.axisLineX = new ElementLine(color: '#D3D3D3')
    theme.axisLineY = new ElementLine(color: '#D3D3D3')
    // Strip backgrounds are transparent in theme_minimal
    theme.stripBackground = new ElementRect(fill: 'none', color: null)
    return theme
  }

  /**
   * Completely blank theme - only data is displayed.
   * All axes, grids, backgrounds, and annotations are removed.
   */
  static Theme void_() {
    Theme theme = new Theme()
    theme.themeName = 'void'
    // Remove all backgrounds
    theme.panelBackground = null
    theme.plotBackground = null
    theme.stripBackground = null
    // Remove all grid lines
    theme.panelGridMajor = null
    theme.panelGridMinor = null
    theme.panelBorder = null
    // Remove all axes
    theme.axisLineX = null
    theme.axisLineY = null
    theme.axisTicksX = null
    theme.axisTicksY = null
    theme.axisTextX = null
    theme.axisTextY = null
    theme.axisTitleX = null
    theme.axisTitleY = null
    // Mark elements as explicitly blank
    theme.explicitNulls.addAll([
        'panelBackground', 'plotBackground', 'stripBackground',
        'panelGridMajor', 'panelGridMinor', 'panelBorder',
        'axisLineX', 'axisLineY', 'axisTicksX', 'axisTicksY',
        'axisTextX', 'axisTextY', 'axisTitleX', 'axisTitleY'
    ])
    return theme
  }

  /**
   * Light theme with light gray backgrounds and subtle grid lines.
   */
  static Theme light() {
    Theme theme = new Theme()
    theme.themeName = 'light'
    theme.panelBackground = new ElementRect(fill: 'white', color: '#CCCCCC', size: 0.5)
    theme.panelGridMajor = new ElementLine(color: '#E5E5E5', size: 0.5)
    theme.panelGridMinor = new ElementLine(color: '#F0F0F0', size: 0.25)
    theme.plotBackground = new ElementRect(fill: 'white', color: null)
    theme.stripBackground = new ElementRect(fill: '#E5E5E5', color: '#CCCCCC', size: 0.5)
    theme.axisLineX = new ElementLine(color: '#CCCCCC', size: 0.5)
    theme.axisLineY = new ElementLine(color: '#CCCCCC', size: 0.5)
    theme.axisTicksX = new ElementLine(color: '#CCCCCC', size: 0.5)
    theme.axisTicksY = new ElementLine(color: '#CCCCCC', size: 0.5)
    return theme
  }

  /**
   * Dark theme with dark backgrounds and light text/grid lines.
   */
  static Theme dark() {
    Theme theme = new Theme()
    theme.themeName = 'dark'
    theme.panelBackground = new ElementRect(fill: '#3B3B3B', color: '#666666', size: 0.5)
    theme.panelGridMajor = new ElementLine(color: '#666666', size: 0.5)
    theme.panelGridMinor = new ElementLine(color: '#555555', size: 0.25)
    theme.plotBackground = new ElementRect(fill: '#222222', color: null)
    theme.stripBackground = new ElementRect(fill: '#555555', color: '#666666', size: 0.5)
    theme.axisLineX = new ElementLine(color: '#666666', size: 0.5)
    theme.axisLineY = new ElementLine(color: '#666666', size: 0.5)
    theme.axisTicksX = new ElementLine(color: '#666666', size: 0.5)
    theme.axisTicksY = new ElementLine(color: '#666666', size: 0.5)
    // Light colored text for readability on dark background
    theme.axisTextX = new ElementText(color: '#CCCCCC')
    theme.axisTextY = new ElementText(color: '#CCCCCC')
    theme.axisTitleX = new ElementText(color: '#CCCCCC')
    theme.axisTitleY = new ElementText(color: '#CCCCCC')
    theme.plotTitle = new ElementText(color: '#CCCCCC')
    theme.plotSubtitle = new ElementText(color: '#CCCCCC')
    theme.plotCaption = new ElementText(color: '#CCCCCC')
    theme.stripText = new ElementText(color: '#CCCCCC')
    theme.legendText = new ElementText(color: '#CCCCCC')
    theme.legendTitle = new ElementText(color: '#CCCCCC')
    theme.legendBackground = new ElementRect(fill: '#3B3B3B', color: '#666666')
    return theme
  }

  /**
   * Line draw theme - crisp black lines on white background.
   * Similar to theme_bw() but with thinner, crisper lines.
   */
  static Theme linedraw() {
    Theme theme = new Theme()
    theme.themeName = 'linedraw'
    theme.panelBackground = new ElementRect(fill: 'white', color: 'black', size: 0.5)
    theme.panelGridMajor = new ElementLine(color: 'black', size: 0.25)
    theme.panelGridMinor = new ElementLine(color: '#333333', size: 0.15)
    theme.plotBackground = new ElementRect(fill: 'white', color: null)
    theme.stripBackground = new ElementRect(fill: 'white', color: 'black', size: 0.5)
    theme.axisLineX = new ElementLine(color: 'black', size: 0.5)
    theme.axisLineY = new ElementLine(color: 'black', size: 0.5)
    theme.axisTicksX = new ElementLine(color: 'black', size: 0.5)
    theme.axisTicksY = new ElementLine(color: 'black', size: 0.5)
    return theme
  }

  /**
   * Test theme for visual unit tests.
   *
   * A theme designed for visual unit tests. It should ideally never change
   * except for new features. This theme provides a stable, minimal baseline
   * for testing visual outputs.
   *
   * Characteristics:
   * - White background with subtle gray panel border
   * - NO grid lines (both major and minor are blank)
   * - NO axis lines
   * - Visible axis ticks and labels
   * - Minimal decoration for testing stability
   *
   * @param baseSize base font size (default: 11)
   * @param baseFamily base font family (default: 'sans-serif')
   * @return Theme configured for visual testing
   */
  static Theme test(Number baseSize = 11, String baseFamily = 'sans-serif') {
    Theme theme = new Theme()
    theme.themeName = 'test'

    // Set base font properties
    theme.baseSize = baseSize
    theme.baseFamily = baseFamily ?: 'sans-serif'

    // Calculate derived sizes
    Number baseLineSize = baseSize / 22.0
    Number baseRectSize = baseSize / 22.0

    // Background: white with subtle gray border
    theme.panelBackground = new ElementRect(
        fill: 'white',
        color: '#CCCCCC',
        size: baseRectSize
    )
    theme.plotBackground = new ElementRect(fill: 'white', color: null)

    // NO grid lines - this is a key characteristic for testing
    theme.panelGridMajor = null
    theme.panelGridMinor = null
    theme.explicitNulls.add('panelGridMajor')
    theme.explicitNulls.add('panelGridMinor')

    // NO axis lines - another key testing characteristic
    theme.axisLineX = null
    theme.axisLineY = null
    theme.explicitNulls.add('axisLineX')
    theme.explicitNulls.add('axisLineY')

    // Axis ticks: visible with subtle gray color
    theme.axisTicksX = new ElementLine(
        color: '#333333',
        size: baseLineSize
    )
    theme.axisTicksY = new ElementLine(
        color: '#333333',
        size: baseLineSize
    )

    // Axis text: black, standard size
    theme.axisTextX = new ElementText(
        color: 'black',
        size: baseSize * 0.8,
        family: baseFamily
    )
    theme.axisTextY = new ElementText(
        color: 'black',
        size: baseSize * 0.8,
        family: baseFamily
    )

    // Axis titles: black, standard size
    theme.axisTitleX = new ElementText(
        color: 'black',
        size: baseSize * 0.9,
        family: baseFamily
    )
    theme.axisTitleY = new ElementText(
        color: 'black',
        size: baseSize * 0.9,
        family: baseFamily
    )

    // Plot titles
    theme.plotTitle = new ElementText(
        color: 'black',
        size: baseSize * 1.2,
        family: baseFamily,
        face: 'bold'
    )
    theme.plotSubtitle = new ElementText(
        color: '#333333',
        size: baseSize,
        family: baseFamily
    )
    theme.plotCaption = new ElementText(
        color: '#666666',
        size: baseSize * 0.8,
        family: baseFamily
    )

    // Strip (facet labels): white background with border
    theme.stripBackground = new ElementRect(
        fill: 'white',
        color: '#CCCCCC',
        size: baseRectSize
    )
    theme.stripText = new ElementText(
        color: 'black',
        size: baseSize * 0.9,
        family: baseFamily
    )

    // Legend: standard settings
    theme.legendBackground = new ElementRect(fill: 'white', color: null)
    theme.legendKey = new ElementRect(fill: 'white', color: null)
    theme.legendText = new ElementText(
        color: 'black',
        size: baseSize * 0.8,
        family: baseFamily
    )
    theme.legendTitle = new ElementText(
        color: 'black',
        size: baseSize * 0.9,
        family: baseFamily
    )

    return theme
  }

  /**
   * Customize theme elements.
   * Supports both camelCase (e.g., 'legendPosition') and dot-notation (e.g., 'legend.position').
   */
  static Theme theme(Map params) {
    Theme theme = new Theme()
    params.each { key, value ->
      String k = key as String

      // Convert dot-notation to camelCase property names
      String propName = convertThemeKey(k)

      // element_blank() means remove the element (set to null)
      boolean isBlank = value instanceof ElementBlank
      def effectiveValue = isBlank ? null : value

      // Handle properties that affect multiple elements
      if (propName == 'axisLine') {
        // 'axis.line' applies to both X and Y
        theme.axisLineX = isBlank ? null : value as ElementLine
        theme.axisLineY = isBlank ? null : value as ElementLine
        if (isBlank) {
          theme.explicitNulls.add('axisLineX')
          theme.explicitNulls.add('axisLineY')
        } else {
          theme.explicitNulls.remove('axisLineX')
          theme.explicitNulls.remove('axisLineY')
        }
      } else if (propName == 'axisTicks') {
        theme.axisTicksX = isBlank ? null : value as ElementLine
        theme.axisTicksY = isBlank ? null : value as ElementLine
        if (isBlank) {
          theme.explicitNulls.add('axisTicksX')
          theme.explicitNulls.add('axisTicksY')
        } else {
          theme.explicitNulls.remove('axisTicksX')
          theme.explicitNulls.remove('axisTicksY')
        }
      } else if (propName == 'axisText') {
        theme.axisTextX = isBlank ? null : value as ElementText
        theme.axisTextY = isBlank ? null : value as ElementText
        if (isBlank) {
          theme.explicitNulls.add('axisTextX')
          theme.explicitNulls.add('axisTextY')
        } else {
          theme.explicitNulls.remove('axisTextX')
          theme.explicitNulls.remove('axisTextY')
        }
      } else if (propName == 'axisTitle') {
        theme.axisTitleX = isBlank ? null : value as ElementText
        theme.axisTitleY = isBlank ? null : value as ElementText
        if (isBlank) {
          theme.explicitNulls.add('axisTitleX')
          theme.explicitNulls.add('axisTitleY')
        } else {
          theme.explicitNulls.remove('axisTitleX')
          theme.explicitNulls.remove('axisTitleY')
        }
      } else if (propName == 'panelGrid') {
        theme.panelGridMajor = isBlank ? null : value as ElementLine
        theme.panelGridMinor = isBlank ? null : value as ElementLine
        if (isBlank) {
          theme.explicitNulls.add('panelGridMajor')
          theme.explicitNulls.add('panelGridMinor')
        } else {
          theme.explicitNulls.remove('panelGridMajor')
          theme.explicitNulls.remove('panelGridMinor')
        }
      } else if (theme.hasProperty(propName)) {
        theme.setProperty(propName, effectiveValue)
        if (isBlank) {
          theme.explicitNulls.add(propName)
        } else {
          theme.explicitNulls.remove(propName)
        }
      }
    }
    return theme
  }

  /**
   * Convert ggplot2 dot-notation theme keys to property names.
   * e.g., 'legend.position' -> 'legendPosition'
   *       'axis.line.x' -> 'axisLineX'
   *       'axis.line.x.bottom' -> 'axisLineX'
   * Note: axis-specific panel grid keys map to major/minor since per-axis grids
   * are not yet modeled separately.
   */
  private static String convertThemeKey(String key) {
    // Common mappings for dot-notation
    Map<String, String> mappings = [
        'legend.position': 'legendPosition',
        'legend.direction': 'legendDirection',
        'legend.background': 'legendBackground',
        'legend.key': 'legendKey',
        'legend.key.size': 'legendKeySize',
        'legend.title': 'legendTitle',
        'legend.text': 'legendText',
        'legend.margin': 'legendMargin',
        'axis.line': 'axisLine',
        'axis.line.x': 'axisLineX',
        'axis.line.y': 'axisLineY',
        'axis.line.x.bottom': 'axisLineX',
        'axis.line.x.top': 'axisLineX',
        'axis.line.y.left': 'axisLineY',
        'axis.line.y.right': 'axisLineY',
        'axis.ticks': 'axisTicks',
        'axis.ticks.x': 'axisTicksX',
        'axis.ticks.y': 'axisTicksY',
        'axis.text': 'axisText',
        'axis.text.x': 'axisTextX',
        'axis.text.y': 'axisTextY',
        'axis.title': 'axisTitle',
        'axis.title.x': 'axisTitleX',
        'axis.title.y': 'axisTitleY',
        'axis.ticks.length': 'axisTickLength',
        'panel.background': 'panelBackground',
        'panel.border': 'panelBorder',
        'panel.grid': 'panelGrid',
        'panel.grid.major': 'panelGridMajor',
        'panel.grid.major.x': 'panelGridMajor',
        'panel.grid.major.y': 'panelGridMajor',
        'panel.grid.minor': 'panelGridMinor',
        'panel.grid.minor.x': 'panelGridMinor',
        'panel.grid.minor.y': 'panelGridMinor',
        'panel.spacing': 'panelSpacing',
        'plot.background': 'plotBackground',
        'plot.title': 'plotTitle',
        'plot.subtitle': 'plotSubtitle',
        'plot.caption': 'plotCaption',
        'plot.margin': 'plotMargin',
        'strip.background': 'stripBackground',
        'strip.text': 'stripText'
    ]

    return mappings.get(key, key)
  }
}
