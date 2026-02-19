package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.theme.ElementText

/**
 * Renders simple discrete legends.
 */
@CompileStatic
class LegendRenderer {

  /**
   * Renders color legend when color mappings are present.
   *
   * @param context render context
   */
  void render(RenderContext context) {
    if (context.colorScale == null || context.colorScale.levels.isEmpty()) {
      return
    }

    Object legendPos = context.chart.theme.legendPosition ?: context.config.legendPosition
    if (legendPos == 'none') {
      return
    }

    int originX = context.config.width - context.config.marginRight + 20
    int originY = context.config.marginTop + 20
    G legend = context.svg.addG().id('legend').transform("translate($originX, $originY)")

    ElementText titleText = context.chart.theme.legendTitle
    ElementText labelText = context.chart.theme.legendText
    BigDecimal defaultSize = (context.chart.theme.baseSize ?: 10) as BigDecimal
    String defaultColor = '#333333'

    String titleColor = titleText?.color ?: defaultColor
    BigDecimal titleSize = (titleText?.size ?: defaultSize + 1) as BigDecimal
    String labelColor = labelText?.color ?: defaultColor
    BigDecimal labelSize = (labelText?.size ?: defaultSize) as BigDecimal

    String title = context.chart.labels?.guides?.get('color') ?: 'color'
    def titleEl = legend.addText(title)
        .x(0).y(12)
        .fontSize(titleSize)
        .fill(titleColor)
        .addAttribute('font-weight', 'bold')
        .styleClass('charm-legend-title')
    if (titleText?.family) {
      titleEl.addAttribute('font-family', titleText.family)
    }

    int keySize = context.config.legendKeySize
    int spacing = context.config.legendSpacing
    int y = 24
    context.colorScale.levels.each { String level ->
      legend.addRect(keySize, keySize)
          .x(0)
          .y(y)
          .fill(context.colorScale.colorFor(level))
          .stroke('#666666')
          .styleClass('charm-legend-key')
      def labelEl = legend.addText(level)
          .x(keySize + 8)
          .y(y + keySize - 2)
          .fontSize(labelSize)
          .fill(labelColor)
          .styleClass('charm-legend-label')
      if (labelText?.family) {
        labelEl.addAttribute('font-family', labelText.family)
      }
      y += keySize + spacing
    }
  }
}
