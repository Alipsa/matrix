package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.pictura.util.ColorUtil
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

}
