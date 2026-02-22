package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.charts.util.ColorUtil

/**
 * Parallel coordinates plot geometry for multivariate data visualization.
 * Creates a vertical axis for each variable and draws lines connecting values across axes.
 *
 * Each observation becomes a polyline, allowing visualization of patterns and clusters
 * in high-dimensional data.
 *
 * Required aesthetics: none (uses all numeric columns by default)
 * Optional aesthetics: color, group, alpha
 *
 * Parameters:
 * - vars: List of column names to include (default: all numeric columns)
 * - alpha: Line transparency (0-1, default: 0.5)
 * - color: Line color (default: 'steelblue')
 * - linewidth: Line width (default: 0.5)
 * - scale: Scaling method - 'uniminmax' (0-1 per variable), 'globalminmax' (0-1 global),
 *          'center' (mean-centered), 'std' (standardized), 'none' (default: 'uniminmax')
 *
 * Usage:
 * <pre>
 * // All numeric columns
 * ggplot(iris) + geom_parallel()
 *
 * // Specific columns
 * ggplot(iris) + geom_parallel(vars: ['Sepal.Length', 'Sepal.Width', 'Petal.Length'])
 *
 * // Colored by species
 * ggplot(iris, aes(color: 'Species')) + geom_parallel()
 *
 * // Custom styling
 * ggplot(mtcars) + geom_parallel(alpha: 0.3, linewidth: 1)
 * </pre>
 */
@CompileStatic
class GeomParallel extends Geom {

  /** Line color */
  String color = 'steelblue'

  /** Line width */
  Number linewidth = 0.5

  /** Alpha transparency (0-1) */
  Number alpha = 0.5

  /** Variables to include (null = all numeric columns) */
  List<String> vars = null

  /** Scaling method: 'uniminmax', 'globalminmax', 'center', 'std', 'none' */
  String scale = 'uniminmax'

  GeomParallel() {
    defaultStat = StatType.IDENTITY
    requiredAes = []
    defaultAes = [color: 'steelblue', alpha: 0.5] as Map<String, Object>
  }

  GeomParallel(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.linewidth != null) this.linewidth = params.linewidth as Number
    if (params.size != null) this.linewidth = params.size as Number
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.vars) this.vars = params.vars as List<String>
    if (params.scale) this.scale = params.scale as String
    this.params = params
  }

}
