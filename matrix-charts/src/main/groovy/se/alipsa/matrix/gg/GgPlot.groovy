package se.alipsa.matrix.gg

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.gg.geom.GeomBin2d
import se.alipsa.matrix.gg.geom.GeomBlank
import se.alipsa.matrix.gg.geom.GeomBoxplot
import se.alipsa.matrix.gg.geom.GeomContour
import se.alipsa.matrix.gg.geom.GeomCount
import se.alipsa.matrix.gg.geom.GeomHistogram
import se.alipsa.matrix.gg.geom.GeomHline
import se.alipsa.matrix.gg.geom.GeomLabel
import se.alipsa.matrix.gg.geom.GeomLine
import se.alipsa.matrix.gg.geom.GeomRug
import se.alipsa.matrix.gg.geom.GeomSegment
import se.alipsa.matrix.gg.geom.GeomSmooth
import se.alipsa.matrix.gg.geom.GeomText
import se.alipsa.matrix.gg.geom.GeomVline
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.CoordCartesian
import se.alipsa.matrix.gg.coord.CoordFlip
import se.alipsa.matrix.gg.coord.CoordPolar
import se.alipsa.matrix.gg.geom.GeomAbline
import se.alipsa.matrix.gg.geom.GeomArea
import se.alipsa.matrix.gg.geom.GeomBar
import se.alipsa.matrix.gg.geom.GeomCol
import se.alipsa.matrix.gg.geom.GeomContourFilled
import se.alipsa.matrix.gg.geom.GeomPoint
import se.alipsa.matrix.gg.geom.GeomViolin
import se.alipsa.matrix.gg.scale.ScaleColorGradient
import se.alipsa.matrix.gg.scale.ScaleColorManual
import se.alipsa.matrix.gg.scale.ScaleXContinuous
import se.alipsa.matrix.gg.scale.ScaleXDiscrete
import se.alipsa.matrix.gg.scale.ScaleYContinuous
import se.alipsa.matrix.gg.scale.ScaleYDiscrete
import se.alipsa.matrix.gg.stat.StatBin2d
import se.alipsa.matrix.gg.stat.StatBoxplot
import se.alipsa.matrix.gg.stat.StatContour
import se.alipsa.matrix.gg.stat.StatContourFilled
import se.alipsa.matrix.gg.stat.StatCount
import se.alipsa.matrix.gg.stat.StatSum
import se.alipsa.matrix.gg.theme.Theme
import se.alipsa.matrix.gg.theme.ElementLine
import se.alipsa.matrix.gg.theme.ElementRect
import se.alipsa.matrix.gg.theme.ElementText

/**
 * An api very similar to ggplot2 making ports from R code using ggplot2 simple.
 */
@CompileStatic
class GgPlot {

  // ============ Core functions ============

  static GgChart ggplot(Matrix data, Aes aes) {
    return new GgChart(data, aes)
  }

  /**
   * Create aesthetic mappings.
   * All parameters are treated as column name mappings.
   * Use I(value) for constant values.
   */
  static Aes aes(Map params) {
    return new Aes(params)
  }

  static Aes aes(String... colNames) {
    return new Aes(Arrays.asList(colNames))
  }

  static Aes aes(List<String> colNames) {
    return new Aes(colNames)
  }

  static Aes aes(List<String> colNames, String colour) {
    return new Aes(colNames, colour)
  }

  /**
   * Identity wrapper for constant values in aes().
   * Use: aes(color: I('red')) to specify a constant color instead of mapping to a column.
   */
  static Identity I(Object value) {
    return new Identity(value)
  }

  // ============ Labels ============

  /**
   * Create label specification for chart titles and axis labels.
   */
  static Label labs(Map params) {
    Label label = new Label()
    if (params.title) label.title = params.title
    if (params.subtitle) label.subTitle = params.subtitle
    if (params.caption) label.caption = params.caption
    if (params.x) label.x = params.x
    if (params.y) label.y = params.y
    if (params.colour || params.color) label.legendTitle = params.colour ?: params.color
    if (params.fill) label.legendTitle = params.fill
    return label
  }

  /**
   * Set x-axis label.
   */
  static Label xlab(String label) {
    return labs(x: label)
  }

  /**
   * Set y-axis label.
   */
  static Label ylab(String label) {
    return labs(y: label)
  }

  /**
   * Set chart title.
   */
  static Label ggtitle(String title, String subtitle = null) {
    return labs(title: title, subtitle: subtitle)
  }

  // ============ Write/Export ============

  /**
   * Write an SVG to a file.
   */
  static void write(Svg svg, File file) {
    file.text = SvgWriter.toXmlPretty(svg)
  }

  /**
   * Write an SVG to an output stream.
   */
  static void write(Svg svg, OutputStream stream) {
    stream.withWriter { writer ->
      writer.write(SvgWriter.toXmlPretty(svg))
    }
  }

  /**
   * Render a chart and write to file.
   */
  static void write(GgChart chart, File file) {
    write(chart.render(), file)
  }

  /**
   * Render a chart and write to stream.
   */
  static void write(GgChart chart, OutputStream stream) {
    write(chart.render(), stream)
  }

  /**
   * Convenience alias for write().
   */
  static void ggsave(GgChart chart, String filename, Map params = [:]) {
    if (params.width) chart.width = params.width as int
    if (params.height) chart.height = params.height as int
    write(chart, new File(filename))
  }

  // ============ Axis limits ============

  /**
   * Set x-axis limits.
   */
  static CoordCartesian xlim(Number min, Number max) {
    return new CoordCartesian(xlim: [min, max])
  }

  /**
   * Set y-axis limits.
   */
  static CoordCartesian ylim(Number min, Number max) {
    return new CoordCartesian(ylim: [min, max])
  }

  // ============ Themes ============

  /**
   * Default gray theme (ggplot2 default).
   */
  static Theme theme_gray() {
    Theme theme = new Theme()
    theme.panelBackground = new ElementRect(fill: '#EBEBEB', color: null)
    theme.panelGridMajor = new ElementLine(color: 'white', size: 1)
    theme.panelGridMinor = new ElementLine(color: 'white', size: 0.5)
    theme.plotBackground = new ElementRect(fill: 'white', color: null)
    return theme
  }

  /** Alias for theme_gray */
  static Theme theme_grey() { return theme_gray() }

  /**
   * Minimal theme with no background annotations.
   */
  static Theme theme_minimal() {
    Theme theme = new Theme()
    theme.panelBackground = new ElementRect(fill: 'white', color: null)
    theme.panelGridMajor = new ElementLine(color: '#D3D3D3', size: 0.5)
    theme.panelGridMinor = null  // No minor grid
    theme.plotBackground = new ElementRect(fill: 'white', color: null)
    theme.axisLineX = new ElementLine(color: '#D3D3D3')
    theme.axisLineY = new ElementLine(color: '#D3D3D3')
    return theme
  }

  /**
   * Black and white theme.
   */
  static Theme theme_bw() {
    Theme theme = new Theme()
    theme.panelBackground = new ElementRect(fill: 'white', color: 'black')
    theme.panelGridMajor = new ElementLine(color: '#D3D3D3', size: 0.5)
    theme.panelGridMinor = new ElementLine(color: '#E5E5E5', size: 0.25)
    theme.plotBackground = new ElementRect(fill: 'white', color: null)
    return theme
  }

  /**
   * Classic theme with axis lines and no grid.
   */
  static Theme theme_classic() {
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
   * Customize theme elements.
   */
  static Theme theme(Map params) {
    Theme theme = new Theme()
    params.each { key, value ->
      if (theme.hasProperty(key as String)) {
        theme.setProperty(key as String, value)
      }
    }
    return theme
  }

  // ============ Coordinates ============

  static CoordFlip coord_flip() {
    return new CoordFlip()
  }

  /**
   * @param theta: variable to map angle to (x or y)
   * @param start: Offset of starting point from 12 o'clock in radians.
   * Offset is applied clockwise or anticlockwise depending on value of direction.
   * @param direction: 1, clockwise; -1, anticlockwise
   * @param clip: Should drawing be clipped to the extent of the plot panel? A setting of "on"
   * (the default) means yes, and a setting of "off" means no.
   */
  static CoordPolar coord_polar(String theta = "x", BigDecimal start = 0, Integer direction = 1, String clip = "on") {
    return new CoordPolar(theta, start, 1 == direction, "on" == clip)
  }

  static CoordPolar coord_polar(Map params) {
    coord_polar(
            params.getOrDefault('theta', 'x') as String,
            params.getOrDefault('start', 0) as BigDecimal,
            params.getOrDefault('direction', 1) as Integer,
            params.getOrDefault('clip', 'on') as String
    )
  }

  static GeomAbline geom_abline() {
    return new GeomAbline()
  }

  static GeomAbline geom_abline(Map params) {
    return new GeomAbline(params)
  }

  static GeomArea geom_area() {
    return new GeomArea()
  }

  static GeomArea geom_area(Map params) {
    return new GeomArea(params)
  }

  static GeomBar geom_bar() {
    return new GeomBar()
  }

  static GeomBar geom_bar(Map params) {
    return new GeomBar(params)
  }

  static GeomBin2d geom_bin_2d() {
    return new GeomBin2d()
  }

  static GeomBlank geom_blank() {
    return new GeomBlank()
  }

  static GeomBoxplot geom_boxplot() {
    return new GeomBoxplot()
  }

  static GeomBoxplot geom_boxplot(Map params) {
    return new GeomBoxplot(params)
  }

  static GeomCol geom_col() {
    return new GeomCol()
  }

  static GeomCol geom_col(Map params) {
    return new GeomCol(params)
  }

  static GeomContour geom_contour() {
    return new GeomContour()
  }

  static GeomContourFilled geom_contour_filled() {
    return new GeomContourFilled()
  }

  static GeomCount geom_count() {
    return new GeomCount()
  }

  static GeomHistogram geom_histogram() {
    return new GeomHistogram()
  }

  static GeomHistogram geom_histogram(Map params) {
    return new GeomHistogram(params)
  }

  static GeomHline geom_hline() {
    return new GeomHline()
  }

  static GeomHline geom_hline(Map params) {
    return new GeomHline(params)
  }

  static GeomLabel geom_label() {
    return new GeomLabel()
  }

  static GeomLabel geom_label(Map params) {
    return new GeomLabel(params)
  }

  static GeomLine geom_line() {
    return new GeomLine()
  }

  static GeomLine geom_line(Map params) {
    return new GeomLine(params)
  }

  static GeomPoint geom_point() {
    return new GeomPoint()
  }

  static GeomPoint geom_point(Map params) {
    return new GeomPoint(params)
  }

  static GeomRug geom_rug(Map params) {
    return new GeomRug(params)
  }

  static GeomSegment geom_segment() {
    return new GeomSegment()
  }

  static GeomSegment geom_segment(Map params) {
    return new GeomSegment(params)
  }

  static GeomSmooth geom_smooth() {
    return new GeomSmooth()
  }

  static GeomSmooth geom_smooth(Map params) {
    return new GeomSmooth(params)
  }

  static GeomText geom_text() {
    return new GeomText()
  }

  static GeomText geom_text(Map params) {
    return new GeomText(params)
  }

  static GeomViolin geom_violin(Aes aes) {
    return new GeomViolin(aes)
  }

  static GeomVline geom_vline() {
    return new GeomVline()
  }

  static GeomVline geom_vline(Map params) {
    return new GeomVline(params)
  }

  // ============ Scales ============

  // --- Position scales ---

  /**
   * Continuous scale for x-axis.
   */
  static ScaleXContinuous scale_x_continuous(Map params = [:]) {
    return new ScaleXContinuous(params)
  }

  /**
   * Continuous scale for y-axis.
   */
  static ScaleYContinuous scale_y_continuous(Map params = [:]) {
    return new ScaleYContinuous(params)
  }

  /**
   * Discrete scale for x-axis (categorical data).
   */
  static ScaleXDiscrete scale_x_discrete(Map params = [:]) {
    return new ScaleXDiscrete(params)
  }

  /**
   * Discrete scale for y-axis (categorical data).
   */
  static ScaleYDiscrete scale_y_discrete(Map params = [:]) {
    return new ScaleYDiscrete(params)
  }

  // --- Color scales ---

  /**
   * Manual color scale for discrete data.
   * @param mappings Map containing 'values' key with list of colors or map of value->color
   */
  static ScaleColorManual scale_color_manual(Map mappings) {
    return new ScaleColorManual(mappings)
  }

  /** British spelling alias for scale_color_manual */
  static ScaleColorManual scale_colour_manual(Map mappings) {
    return scale_color_manual(mappings)
  }

  /**
   * Continuous color gradient scale.
   * @param params Map with 'low', 'high', optionally 'mid' and 'midpoint'
   */
  static ScaleColorGradient scale_color_gradient(Map params = [:]) {
    return new ScaleColorGradient(params)
  }

  /** British spelling alias for scale_color_gradient */
  static ScaleColorGradient scale_colour_gradient(Map params = [:]) {
    return scale_color_gradient(params)
  }

  /**
   * Two-color gradient with specified low and high colors.
   */
  static ScaleColorGradient scale_color_gradient(String low, String high) {
    return new ScaleColorGradient(low: low, high: high)
  }

  /** British spelling alias */
  static ScaleColorGradient scale_colour_gradient(String low, String high) {
    return scale_color_gradient(low, high)
  }

  /**
   * Diverging color gradient with mid color.
   */
  static ScaleColorGradient scale_color_gradient2(Map params = [:]) {
    // gradient2 is typically a diverging scale
    if (!params.mid) params.mid = 'white'
    return new ScaleColorGradient(params)
  }

  /** British spelling alias */
  static ScaleColorGradient scale_colour_gradient2(Map params = [:]) {
    return scale_color_gradient2(params)
  }

  // --- Fill scales ---

  /**
   * Manual fill scale for discrete data.
   */
  static ScaleColorManual scale_fill_manual(Map mappings) {
    def scale = new ScaleColorManual(mappings)
    scale.aesthetic = 'fill'
    return scale
  }

  /**
   * Continuous fill gradient scale.
   */
  static ScaleColorGradient scale_fill_gradient(Map params = [:]) {
    def scale = new ScaleColorGradient(params)
    scale.aesthetic = 'fill'
    return scale
  }

  /**
   * Two-color fill gradient.
   */
  static ScaleColorGradient scale_fill_gradient(String low, String high) {
    def scale = new ScaleColorGradient(low: low, high: high)
    scale.aesthetic = 'fill'
    return scale
  }

  /**
   * Diverging fill gradient with mid color.
   */
  static ScaleColorGradient scale_fill_gradient2(Map params = [:]) {
    if (!params.mid) params.mid = 'white'
    def scale = new ScaleColorGradient(params)
    scale.aesthetic = 'fill'
    return scale
  }

  static StatBin2d stat_bin_2d() {
    return new StatBin2d()
  }

  static StatBoxplot stat_boxplot() {
    return new StatBoxplot()
  }

  static StatContour stat_contour() {
    return new StatContour()
  }

  static StatContourFilled stat_contour_filled() {
    return new StatContourFilled()
  }

  static StatCount stat_count() {
    return new StatCount()
  }

  static StatSum stat_summary(Map params) {
    return new StatSum(params)
  }

  static class As {

    static List factor(List column) {
      return ListConverter.toStrings(column)
    }
  }
}
