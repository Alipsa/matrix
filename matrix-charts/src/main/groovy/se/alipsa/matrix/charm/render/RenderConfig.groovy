package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic

/**
 * Render configuration for Charm SVG output.
 */
@CompileStatic
class RenderConfig {

  int width = 800
  int height = 600
  int marginTop = 60
  int marginRight = 100
  int marginBottom = 60
  int marginLeft = 70
  int panelSpacing = 20
  int stripHeight = 24
  int stripWidth = 24
  int axisTickCount = 5
  int axisTickLength = 6
  int pointRadius = 3
  int labelPadding = 30
  int legendKeySize = 12
  int legendSpacing = 8
  String legendPosition = 'right'

  /**
   * Returns computed plot width.
   *
   * @return plot width
   */
  int plotWidth() {
    width - marginLeft - marginRight
  }

  /**
   * Returns computed plot height.
   *
   * @return plot height
   */
  int plotHeight() {
    height - marginTop - marginBottom
  }

  /**
   * Returns a copied config instance.
   *
   * @return copied config
   */
  RenderConfig copy() {
    new RenderConfig(
        width: width,
        height: height,
        marginTop: marginTop,
        marginRight: marginRight,
        marginBottom: marginBottom,
        marginLeft: marginLeft,
        panelSpacing: panelSpacing,
        stripHeight: stripHeight,
        stripWidth: stripWidth,
        axisTickCount: axisTickCount,
        axisTickLength: axisTickLength,
        pointRadius: pointRadius,
        labelPadding: labelPadding,
        legendKeySize: legendKeySize,
        legendSpacing: legendSpacing,
        legendPosition: legendPosition
    )
  }
}
