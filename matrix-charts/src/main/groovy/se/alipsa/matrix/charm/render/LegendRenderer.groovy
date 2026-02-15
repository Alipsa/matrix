package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G

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
    if ((context.chart.theme.legend?.position ?: context.config.legendPosition) == 'none') {
      return
    }

    int originX = context.config.width - context.config.marginRight + 20
    int originY = context.config.marginTop + 20
    G legend = context.svg.addG().id('legend').transform("translate($originX, $originY)")
    String textColor = (context.chart.theme.text?.color ?: '#333333') as String
    BigDecimal textSize = (context.chart.theme.text?.size ?: 10) as BigDecimal

    String title = context.chart.labels?.guides?.get('color') ?: 'color'
    legend.addText(title)
        .x(0).y(12)
        .fontSize((textSize + 1) as BigDecimal)
        .fill(textColor)
        .addAttribute('font-weight', 'bold')
        .styleClass('charm-legend-title')

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
      legend.addText(level)
          .x(keySize + 8)
          .y(y + keySize - 2)
          .fontSize(textSize)
          .fill(textColor)
          .styleClass('charm-legend-label')
      y += keySize + spacing
    }
  }
}
