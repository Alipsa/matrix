package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.render.RenderContext

/**
 * Label geometry for adding text labels with backgrounds to plots.
 * Like geom_text but with a rectangular background behind the text.
 *
 * Usage:
 * - geom_label(aes(label: 'name')) - labels from data column
 * - geom_label(fill: 'white', color: 'black') - white background with black border
 */
@CompileStatic
class GeomLabel extends Geom {

  /** Text color */
  String color = 'black'

  /** Background fill color */
  String fill = 'white'

  /** Font size in pixels */
  BigDecimal size = 10

  /** Font family */
  String family = 'sans-serif'

  /** Font weight: 'normal', 'bold' */
  String fontface = 'normal'

  /** Alpha transparency for fill (0-1) */
  BigDecimal alpha = 1.0

  /** Horizontal adjustment: 0=left, 0.5=center, 1=right */
  BigDecimal hjust = 0.5

  /** Vertical adjustment: 0=bottom, 0.5=middle, 1=top */
  BigDecimal vjust = 0.5

  /** Rotation angle in degrees */
  BigDecimal angle = 0

  /** Nudge x offset in data units */
  BigDecimal nudge_x = 0

  /** Nudge y offset in data units */
  BigDecimal nudge_y = 0

  /** Padding around text in pixels */
  BigDecimal label_padding = 4

  /** Corner radius for rounded rectangles */
  BigDecimal label_r = 2

  /** Border line width */
  BigDecimal label_size = 0.5

  /** Fixed label text (if not mapping from data) */
  String label

  GeomLabel() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', fill: 'white', size: 10] as Map<String, Object>
  }

  GeomLabel(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    if (params.size != null) this.size = params.size as BigDecimal
    this.family = params.family as String ?: this.family
    this.fontface = params.fontface as String ?: this.fontface
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    if (params.hjust != null) this.hjust = params.hjust as BigDecimal
    if (params.vjust != null) this.vjust = params.vjust as BigDecimal
    if (params.angle != null) this.angle = params.angle as BigDecimal
    if (params.nudge_x != null) this.nudge_x = params.nudge_x as BigDecimal
    if (params.nudge_y != null) this.nudge_y = params.nudge_y as BigDecimal
    if (params.label_padding != null) this.label_padding = params.label_padding as BigDecimal
    if (params.label_r != null) this.label_r = params.label_r as BigDecimal
    if (params.label_size != null) this.label_size = params.label_size as BigDecimal
    this.label = params.label as String ?: this.label
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    render(group, data, aes, scales, coord, null)
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord, RenderContext ctx) {
    if (data == null || data.rowCount() == 0) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String labelCol = aes.label instanceof String ? aes.label as String : null
    String colorCol = aes.colorColName
    String fillCol = aes.fillColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomLabel requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']
    Scale fillScale = scales['fill']

    // Render each label
    int elementIndex = 0
    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) {
        elementIndex++
        return
      }

      // Get label text
      String labelText = this.label
      if (labelCol) {
        def labelVal = row[labelCol]
        if (labelVal != null) {
          labelText = labelVal.toString()
        }
      } else if (aes.label instanceof Identity) {
        labelText = (aes.label as Identity).value?.toString()
      }

      if (labelText == null || labelText.isEmpty()) {
        elementIndex++
        return
      }

      // Transform coordinates
      BigDecimal xPx = xScale?.transform(xVal) as BigDecimal
      BigDecimal yPx = yScale?.transform(yVal) as BigDecimal

      if (xPx == null || yPx == null) {
        elementIndex++
        return
      }

      // Apply nudge
      xPx += nudge_x * 10
      yPx -= nudge_y * 10

      // Determine colors
      String textColor = this.color
      String bgFill = this.fill

      if (colorCol) {
        def colorVal = row[colorCol]
        if (colorScale) {
          textColor = colorScale.transform(colorVal)?.toString() ?: this.color
        }
      } else if (aes.color instanceof Identity) {
        textColor = (aes.color as Identity).value.toString()
      }
      textColor = ColorUtil.normalizeColor(textColor) ?: textColor

      if (fillCol) {
        def fillVal = row[fillCol]
        if (fillScale) {
          bgFill = fillScale.transform(fillVal)?.toString() ?: this.fill
        }
      } else if (aes.fill instanceof Identity) {
        bgFill = (aes.fill as Identity).value.toString()
      }
      bgFill = ColorUtil.normalizeColor(bgFill) ?: bgFill

      // Estimate text dimensions (approximate)
      BigDecimal textWidth = labelText.length() * size * 0.6
      BigDecimal textHeight = size * 1.2

      // Calculate background rect position based on hjust/vjust
      BigDecimal rectWidth = textWidth + label_padding * 2
      BigDecimal rectHeight = textHeight + label_padding * 2

      BigDecimal rectX = xPx - rectWidth * hjust
      BigDecimal rectY = yPx - rectHeight * (1 - vjust)

      // Create a group for label if rotation is needed
      G labelGroup = group
      if (angle != 0) {
        labelGroup = group.addG()
        labelGroup.addAttribute('transform', "rotate(${-angle}, ${xPx}, ${yPx})")
      }

      // Draw background rectangle
      def rect = labelGroup.addRect()
          .x(rectX)
          .y(rectY)
          .width(rectWidth)
          .height(rectHeight)
          .fill(bgFill)
          .stroke(textColor)

      rect.addAttribute('stroke-width', label_size)

      if (label_r > 0) {
        rect.rx(label_r)
        rect.ry(label_r)
      }

      if (alpha < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }

      // Calculate text position (center of rect)
      BigDecimal textX = rectX + rectWidth / 2
      BigDecimal textY = rectY + rectHeight / 2

      // Create text element
      def text = labelGroup.addText(labelText)
          .x(textX)
          .y(textY)
          .fill(textColor)

      text.addAttribute('font-size', size)
      text.addAttribute('font-family', family)
      text.addAttribute('text-anchor', 'middle')
      text.addAttribute('dominant-baseline', 'middle')

      if (fontface == 'bold') {
        text.addAttribute('font-weight', 'bold')
      } else if (fontface == 'italic') {
        text.addAttribute('font-style', 'italic')
      } else if (fontface == 'bold.italic') {
        text.addAttribute('font-weight', 'bold')
        text.addAttribute('font-style', 'italic')
      }

      // Apply CSS attributes
      GeomUtils.applyAttributes(rect, ctx, 'label', 'gg-label', elementIndex)
      GeomUtils.applyAttributes(text, ctx, 'label', 'gg-label', elementIndex)
      elementIndex++
    }
  }
}
