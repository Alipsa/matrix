package se.alipsa.matrix.gg.theme

class Themes {

  /**
   * Default gray theme (ggplot2 default).
   */
  static Theme gray() {
    Theme theme = new Theme()
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
    theme.panelBackground = new ElementRect(fill: 'white', color: null)
    theme.panelGridMajor = null  // No grid
    theme.panelGridMinor = null
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
    theme.panelBackground = new ElementRect(fill: 'white', color: 'black')
    theme.panelGridMajor = new ElementLine(color: '#D3D3D3', size: 0.5)
    theme.panelGridMinor = new ElementLine(color: '#E5E5E5', size: 0.25)
    theme.plotBackground = new ElementRect(fill: 'white', color: null)
    return theme
  }

  /**
   * Minimal theme with no background annotations.
   */
  static Theme minimal() {
    Theme theme = new Theme()
    theme.panelBackground = new ElementRect(fill: 'white', color: null)
    theme.panelGridMajor = new ElementLine(color: '#D3D3D3', size: 0.5)
    theme.panelGridMinor = null  // No minor grid
    theme.plotBackground = new ElementRect(fill: 'white', color: null)
    theme.axisLineX = new ElementLine(color: '#D3D3D3')
    theme.axisLineY = new ElementLine(color: '#D3D3D3')
    return theme
  }
}
