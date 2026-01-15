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
 * Text geometry for adding text labels to plots.
 *
 * Usage:
 * - geom_text(aes(label: 'name')) - labels from data column
 * - geom_text(label: 'fixed text') - fixed label for all points
 * - geom_text(size: 12, color: 'red') - styled text
 */
@CompileStatic
class GeomText extends Geom {

  /** Text color */
  String color = 'black'

  /** Font size in pixels */
  BigDecimal size = 10

  /** Font family */
  String family = 'sans-serif'

  /** Font weight: 'normal', 'bold' */
  String fontface = 'normal'

  /** Alpha transparency (0-1) */
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

  /** Fixed label text (if not mapping from data) */
  String label

  GeomText() {
    defaultStat = StatType.IDENTITY
    requiredAes = ['x', 'y']
    defaultAes = [color: 'black', size: 10] as Map<String, Object>
  }

  GeomText(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    if (params.size != null) this.size = params.size as BigDecimal
    this.family = params.family as String ?: this.family
    this.fontface = params.fontface as String ?: this.fontface
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    if (params.hjust != null) this.hjust = params.hjust as BigDecimal
    if (params.vjust != null) this.vjust = params.vjust as BigDecimal
    if (params.angle != null) this.angle = params.angle as BigDecimal
    if (params.nudge_x != null) this.nudge_x = params.nudge_x as BigDecimal
    if (params.nudge_y != null) this.nudge_y = params.nudge_y as BigDecimal
    this.label = params.label as String ?: this.label
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String labelCol = aes.label instanceof String ? aes.label as String : null
    String colorCol = aes.colorColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomText requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']

    // Render each text label
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
      BigDecimal xPx = xScale?.transform(xVal) as BigDecimal
      BigDecimal yPx = yScale?.transform(yVal) as BigDecimal

      if (xPx == null || yPx == null) return

      // Apply nudge (simple pixel offset - could be data units for more accuracy)
      xPx += nudge_x * 10  // approximate scaling
      yPx -= nudge_y * 10  // y increases downward in SVG

      // Determine text color
      String textColor = this.color
      if (colorCol) {
        def colorVal = row[colorCol]
        if (colorScale) {
          textColor = colorScale.transform(colorVal)?.toString() ?: this.color
        }
      } else if (aes.color instanceof Identity) {
        textColor = (aes.color as Identity).value.toString()
      }
      textColor = ColorUtil.normalizeColor(textColor) ?: textColor

      // Calculate text anchor based on hjust
      String textAnchor = getTextAnchor(hjust)

      // Calculate dominant-baseline based on vjust
      String dominantBaseline = getDominantBaseline(vjust)

      // Create text element
      def text = group.addText(labelText)
          .x(xPx)
          .y(yPx)
          .fill(textColor)

      text.addAttribute('font-size', size)
      text.addAttribute('font-family', family)
      text.addAttribute('text-anchor', textAnchor)
      text.addAttribute('dominant-baseline', dominantBaseline)

      if (fontface == 'bold') {
        text.addAttribute('font-weight', 'bold')
      } else if (fontface == 'italic') {
        text.addAttribute('font-style', 'italic')
      } else if (fontface == 'bold.italic') {
        text.addAttribute('font-weight', 'bold')
        text.addAttribute('font-style', 'italic')
      }

      // Apply rotation
      if (angle != 0) {
        text.addAttribute('transform', "rotate(${-angle}, ${xPx}, ${yPx})")
      }

      // Apply alpha
      if (alpha < 1.0) {
        text.addAttribute('fill-opacity', alpha)
      }
    }
  }

  /**
   * Convert hjust to SVG text-anchor.
   */
  private String getTextAnchor(BigDecimal hjust) {
    if (hjust <= 0.25) return 'start'
    if (hjust >= 0.75) return 'end'
    return 'middle'
  }

  /**
   * Convert vjust to SVG dominant-baseline.
   */
  private String getDominantBaseline(BigDecimal vjust) {
    if (vjust <= 0.25) return 'text-after-edge'  // bottom
    if (vjust >= 0.75) return 'text-before-edge'  // top
    return 'middle'
  }
}
