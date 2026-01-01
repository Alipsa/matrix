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
  Number size = 10

  /** Font family */
  String family = 'sans-serif'

  /** Font weight: 'normal', 'bold' */
  String fontface = 'normal'

  /** Alpha transparency for fill (0-1) */
  Number alpha = 1.0

  /** Horizontal adjustment: 0=left, 0.5=center, 1=right */
  Number hjust = 0.5

  /** Vertical adjustment: 0=bottom, 0.5=middle, 1=top */
  Number vjust = 0.5

  /** Rotation angle in degrees */
  Number angle = 0

  /** Nudge x offset in data units */
  Number nudge_x = 0

  /** Nudge y offset in data units */
  Number nudge_y = 0

  /** Padding around text in pixels */
  Number label_padding = 4

  /** Corner radius for rounded rectangles */
  Number label_r = 2

  /** Border line width */
  Number label_size = 0.5

  /** Fixed label text (if not mapping from data) */
  String label

  GeomLabel() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', fill: 'white', size: 10] as Map<String, Object>
  }

  GeomLabel(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.size != null) this.size = params.size as Number
    if (params.family) this.family = params.family as String
    if (params.fontface) this.fontface = params.fontface as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.hjust != null) this.hjust = params.hjust as Number
    if (params.vjust != null) this.vjust = params.vjust as Number
    if (params.angle != null) this.angle = params.angle as Number
    if (params.nudge_x != null) this.nudge_x = params.nudge_x as Number
    if (params.nudge_y != null) this.nudge_y = params.nudge_y as Number
    if (params.label_padding != null) this.label_padding = params.label_padding as Number
    if (params.label_r != null) this.label_r = params.label_r as Number
    if (params.label_size != null) this.label_size = params.label_size as Number
    if (params.label) this.label = params.label as String
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
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
    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) return

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

      if (labelText == null || labelText.isEmpty()) return

      // Transform coordinates
      def xTransformed = xScale?.transform(xVal)
      def yTransformed = yScale?.transform(yVal)

      if (xTransformed == null || yTransformed == null) return

      double xPx = xTransformed as double
      double yPx = yTransformed as double

      // Apply nudge
      xPx += (nudge_x as double) * 10
      yPx -= (nudge_y as double) * 10

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
      double fontSize = size as double
      double textWidth = labelText.length() * fontSize * 0.6
      double textHeight = fontSize * 1.2

      // Calculate background rect position based on hjust/vjust
      double padding = label_padding as double
      double rectWidth = textWidth + padding * 2
      double rectHeight = textHeight + padding * 2

      double rectX = xPx - rectWidth * (hjust as double)
      double rectY = yPx - rectHeight * (1 - vjust as double)

      // Create a group for label if rotation is needed
      G labelGroup = group
      double angleVal = angle as double
      if (angleVal != 0) {
        labelGroup = group.addG()
        labelGroup.addAttribute('transform', "rotate(${-angleVal}, ${xPx as int}, ${yPx as int})")
      }

      // Draw background rectangle
      def rect = labelGroup.addRect()
          .x(rectX as int)
          .y(rectY as int)
          .width(rectWidth as int)
          .height(rectHeight as int)
          .fill(bgFill)
          .stroke(textColor)

      rect.addAttribute('stroke-width', label_size)

      if ((label_r as double) > 0) {
        rect.rx(label_r as int)
        rect.ry(label_r as int)
      }

      if ((alpha as double) < 1.0) {
        rect.addAttribute('fill-opacity', alpha)
      }

      // Calculate text position (center of rect)
      double textX = rectX + rectWidth / 2
      double textY = rectY + rectHeight / 2

      // Create text element
      def text = labelGroup.addText(labelText)
          .x(textX as int)
          .y(textY as int)
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
    }
  }
}
