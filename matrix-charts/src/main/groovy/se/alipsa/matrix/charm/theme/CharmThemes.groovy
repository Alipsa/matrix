package se.alipsa.matrix.charm.theme

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.Theme

/**
 * Predefined theme factories for Charm charts.
 *
 * Mirrors the gg Themes class, providing themed element configurations
 * for all standard ggplot2 theme presets.
 */
@CompileStatic
class CharmThemes {

  /**
   * Default gray theme (ggplot2 default).
   *
   * @return gray theme
   */
  static Theme gray() {
    Theme theme = new Theme()
    theme.themeName = 'gray'
    theme.panelBackground = new ElementRect(fill: '#EBEBEB', color: null)
    theme.panelGridMajor = new ElementLine(color: 'white', size: 1)
    theme.panelGridMinor = new ElementLine(color: 'white', size: 0.5)
    theme.plotBackground = new ElementRect(fill: 'white', color: null)
    theme
  }

  /**
   * Classic theme with axis lines and no grid.
   *
   * @return classic theme
   */
  static Theme classic() {
    Theme theme = new Theme()
    theme.themeName = 'classic'
    theme.panelBackground = new ElementRect(fill: 'white', color: null)
    theme.panelGridMajor = null
    theme.panelGridMinor = null
    theme.explicitNulls.add('panelGridMajor')
    theme.explicitNulls.add('panelGridMinor')
    theme.plotBackground = new ElementRect(fill: 'white', color: null)
    theme.axisLineX = new ElementLine(color: 'black', size: 1)
    theme.axisLineY = new ElementLine(color: 'black', size: 1)
    theme
  }

  /**
   * Black and white theme.
   *
   * @return bw theme
   */
  static Theme bw() {
    Theme theme = new Theme()
    theme.themeName = 'bw'
    theme.panelBackground = new ElementRect(fill: 'white', color: 'black')
    theme.panelGridMajor = new ElementLine(color: '#D3D3D3', size: 0.5)
    theme.panelGridMinor = new ElementLine(color: '#E5E5E5', size: 0.25)
    theme.plotBackground = new ElementRect(fill: 'white', color: null)
    theme
  }

  /**
   * Minimal theme with transparent backgrounds.
   *
   * @return minimal theme
   */
  static Theme minimal() {
    Theme theme = new Theme()
    theme.themeName = 'minimal'
    theme.panelBackground = new ElementRect(fill: 'none', color: null)
    theme.panelGridMajor = new ElementLine(color: '#D3D3D3', size: 0.5)
    theme.panelGridMinor = null
    theme.explicitNulls.add('panelGridMinor')
    theme.plotBackground = new ElementRect(fill: 'none', color: null)
    theme.axisLineX = new ElementLine(color: '#D3D3D3')
    theme.axisLineY = new ElementLine(color: '#D3D3D3')
    theme.stripBackground = new ElementRect(fill: 'none', color: null)
    theme
  }

  /**
   * Completely blank theme - only data is displayed.
   *
   * @return void theme
   */
  static Theme void_() {
    Theme theme = new Theme()
    theme.themeName = 'void'
    theme.panelBackground = null
    theme.plotBackground = null
    theme.stripBackground = null
    theme.panelGridMajor = null
    theme.panelGridMinor = null
    theme.panelBorder = null
    theme.axisLineX = null
    theme.axisLineY = null
    theme.axisTicksX = null
    theme.axisTicksY = null
    theme.axisTextX = null
    theme.axisTextY = null
    theme.axisTitleX = null
    theme.axisTitleY = null
    theme.explicitNulls.addAll([
        'panelBackground', 'plotBackground', 'stripBackground',
        'panelGridMajor', 'panelGridMinor', 'panelBorder',
        'axisLineX', 'axisLineY', 'axisTicksX', 'axisTicksY',
        'axisTextX', 'axisTextY', 'axisTitleX', 'axisTitleY'
    ])
    theme
  }

  /**
   * Light theme with light gray backgrounds.
   *
   * @return light theme
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
    theme
  }

  /**
   * Dark theme with dark backgrounds and light text.
   *
   * @return dark theme
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
    theme
  }

  /**
   * Line draw theme - crisp black lines on white background.
   *
   * @return linedraw theme
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
    theme
  }

  /**
   * Test theme for visual unit tests.
   *
   * @param baseSize base font size (default: 11)
   * @param baseFamily base font family (default: 'sans-serif')
   * @return test theme
   */
  static Theme test(Number baseSize = 11, String baseFamily = 'sans-serif') {
    Theme theme = new Theme()
    theme.themeName = 'test'
    theme.baseSize = baseSize
    theme.baseFamily = baseFamily ?: 'sans-serif'

    Number baseLineSize = baseSize / 22.0
    Number baseRectSize = baseSize / 22.0

    theme.panelBackground = new ElementRect(fill: 'white', color: '#CCCCCC', size: baseRectSize)
    theme.plotBackground = new ElementRect(fill: 'white', color: null)
    theme.panelGridMajor = null
    theme.panelGridMinor = null
    theme.explicitNulls.add('panelGridMajor')
    theme.explicitNulls.add('panelGridMinor')
    theme.axisLineX = null
    theme.axisLineY = null
    theme.explicitNulls.add('axisLineX')
    theme.explicitNulls.add('axisLineY')
    theme.axisTicksX = new ElementLine(color: '#333333', size: baseLineSize)
    theme.axisTicksY = new ElementLine(color: '#333333', size: baseLineSize)
    theme.axisTextX = new ElementText(color: 'black', size: baseSize * 0.8, family: baseFamily)
    theme.axisTextY = new ElementText(color: 'black', size: baseSize * 0.8, family: baseFamily)
    theme.axisTitleX = new ElementText(color: 'black', size: baseSize * 0.9, family: baseFamily)
    theme.axisTitleY = new ElementText(color: 'black', size: baseSize * 0.9, family: baseFamily)
    theme.plotTitle = new ElementText(color: 'black', size: baseSize * 1.2, family: baseFamily, face: 'bold')
    theme.plotSubtitle = new ElementText(color: '#333333', size: baseSize, family: baseFamily)
    theme.plotCaption = new ElementText(color: '#666666', size: baseSize * 0.8, family: baseFamily)
    theme.stripBackground = new ElementRect(fill: 'white', color: '#CCCCCC', size: baseRectSize)
    theme.stripText = new ElementText(color: 'black', size: baseSize * 0.9, family: baseFamily)
    theme.legendBackground = new ElementRect(fill: 'white', color: null)
    theme.legendKey = new ElementRect(fill: 'white', color: null)
    theme.legendText = new ElementText(color: 'black', size: baseSize * 0.8, family: baseFamily)
    theme.legendTitle = new ElementText(color: 'black', size: baseSize * 0.9, family: baseFamily)
    theme
  }
}
