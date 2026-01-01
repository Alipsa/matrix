package se.alipsa.matrix.gg

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.gg.geom.GeomBin2d
import se.alipsa.matrix.gg.geom.GeomBlank
import se.alipsa.matrix.gg.geom.GeomBoxplot
import se.alipsa.matrix.gg.geom.GeomContour
import se.alipsa.matrix.gg.geom.GeomCount
import se.alipsa.matrix.gg.geom.GeomDensity
import se.alipsa.matrix.gg.geom.GeomErrorbar
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
import se.alipsa.matrix.gg.aes.AfterStat
import se.alipsa.matrix.gg.aes.Expression
import se.alipsa.matrix.gg.aes.Factor
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.CoordCartesian
import se.alipsa.matrix.gg.coord.CoordFixed
import se.alipsa.matrix.gg.coord.CoordFlip
import se.alipsa.matrix.gg.coord.CoordPolar
import se.alipsa.matrix.gg.facet.FacetGrid
import se.alipsa.matrix.gg.facet.FacetWrap
import se.alipsa.matrix.gg.facet.FormulaParser
import se.alipsa.matrix.gg.geom.GeomAbline
import se.alipsa.matrix.gg.geom.GeomArea
import se.alipsa.matrix.gg.geom.GeomBar
import se.alipsa.matrix.gg.geom.GeomCol
import se.alipsa.matrix.gg.geom.GeomContourFilled
import se.alipsa.matrix.gg.geom.GeomPoint
import se.alipsa.matrix.gg.geom.GeomViolin
import se.alipsa.matrix.gg.scale.ScaleColorGradient
import se.alipsa.matrix.gg.scale.ScaleColorManual
import se.alipsa.matrix.gg.scale.ScaleColorViridis
import se.alipsa.matrix.gg.scale.ScaleColorViridisC
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
import se.alipsa.matrix.gg.theme.ElementBlank
import se.alipsa.matrix.gg.theme.ElementLine
import se.alipsa.matrix.gg.theme.ElementRect
import se.alipsa.matrix.gg.theme.ElementText
import se.alipsa.matrix.gg.theme.Themes

/**
 * An api very similar to ggplot2 making ports from R code using ggplot2 simple.
 */
@CompileStatic
class GgPlot {

  /** Constants for convenience when converting R code */
  static final def NULL = null
  static final boolean TRUE = true
  static final boolean FALSE = false

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

  static Aes aes(Object x) {
    Aes aes = new Aes()
    aes.x = x
    return aes
  }

  static Aes aes(Object x, Object y) {
    Aes aes = new Aes()
    aes.x = x
    aes.y = y
    return aes
  }

  static Aes aes(List<String> colNames) {
    return new Aes(colNames)
  }

  static Aes aes(List<String> colNames, String colour) {
    return new Aes(colNames, colour)
  }

  /**
   * Create aesthetic mappings with positional x, y and additional named parameters.
   * Example: aes('cty', 'hwy', colour: 'class')
   */
  static Aes aes(Map params, Object x) {
    Aes aes = new Aes(params)
    aes.x = x
    return aes
  }

  static Aes aes(Map params, Object x, Object y) {
    Aes aes = new Aes(params)
    aes.x = x
    aes.y = y
    return aes
  }

  /**
   * Identity wrapper for constant values in aes().
   * Use: aes(color: I('red')) to specify a constant color instead of mapping to a column.
   */
  static Identity I(Object value) {
    return new Identity(value)
  }

  /**
   * Reference to a computed statistic for use in aesthetic mappings.
   * Use: aes(y: after_stat('count')) to map the computed count from stat_bin.
   *
   * Common computed variables:
   * - 'count': number of observations in each bin/group
   * - 'density': density of observations
   * - 'ncount': count normalized to maximum of 1
   * - 'ndensity': density normalized to maximum of 1
   *
   * @param stat The name of the computed statistic
   */
  static AfterStat after_stat(String stat) {
    return new AfterStat(stat)
  }

  /**
   * Expression wrapper for closure-based transformations in aesthetic mappings.
   * Use: aes(y: expr { 1.0 / it.hwy }) to compute values from row data.
   *
   * The closure receives a Row object and should return a numeric value.
   *
   * @param closure The closure that computes the value from row data
   */
  static Expression expr(Closure<Number> closure) {
    return new Expression(closure)
  }

  /**
   * Expression wrapper with custom column name.
   *
   * @param name The name for the computed column
   * @param closure The closure that computes the value from row data
   */
  static Expression expr(String name, Closure<Number> closure) {
    return new Expression(closure, name)
  }

  /**
   * Factor wrapper for categorical values in aesthetic mappings.
   */
  static Factor factor(Object value) {
    return new Factor(value)
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
    if (params.containsKey('x')) {
      label.x = params.x as String
      label.xSet = true
    }
    if (params.containsKey('y')) {
      label.y = params.y as String
      label.ySet = true
    }
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
    return Themes.gray()
  }

  /** Alias for theme_gray */
  static Theme theme_grey() { return theme_gray() }

  /**
   * Minimal theme with no background annotations.
   */
  static Theme theme_minimal() {
    return Themes.minimal()
  }

  /**
   * Black and white theme.
   */
  static Theme theme_bw() {
    return Themes.bw()
  }

  /**
   * Classic theme with axis lines and no grid.
   */
  static Theme theme_classic() {
    return Themes.classic()
  }

  /**
   * Customize theme elements.
   * Supports both camelCase (e.g., 'legendPosition') and dot-notation (e.g., 'legend.position').
   */
  static Theme theme(Map params) {
    Theme theme = new Theme()
    params.each { key, value ->
      String k = key as String

      // Convert dot-notation to camelCase property names
      String propName = convertThemeKey(k)

      // element_blank() means remove the element (set to null)
      boolean isBlank = value instanceof ElementBlank
      def effectiveValue = isBlank ? null : value

      // Handle properties that affect multiple elements
      if (propName == 'axisLine') {
        // 'axis.line' applies to both X and Y
        theme.axisLineX = isBlank ? null : value as ElementLine
        theme.axisLineY = isBlank ? null : value as ElementLine
        if (isBlank) {
          theme.explicitNulls.add('axisLineX')
          theme.explicitNulls.add('axisLineY')
        } else {
          theme.explicitNulls.remove('axisLineX')
          theme.explicitNulls.remove('axisLineY')
        }
      } else if (propName == 'axisTicks') {
        theme.axisTicksX = isBlank ? null : value as ElementLine
        theme.axisTicksY = isBlank ? null : value as ElementLine
        if (isBlank) {
          theme.explicitNulls.add('axisTicksX')
          theme.explicitNulls.add('axisTicksY')
        } else {
          theme.explicitNulls.remove('axisTicksX')
          theme.explicitNulls.remove('axisTicksY')
        }
      } else if (propName == 'axisText') {
        theme.axisTextX = isBlank ? null : value as ElementText
        theme.axisTextY = isBlank ? null : value as ElementText
        if (isBlank) {
          theme.explicitNulls.add('axisTextX')
          theme.explicitNulls.add('axisTextY')
        } else {
          theme.explicitNulls.remove('axisTextX')
          theme.explicitNulls.remove('axisTextY')
        }
      } else if (propName == 'axisTitle') {
        theme.axisTitleX = isBlank ? null : value as ElementText
        theme.axisTitleY = isBlank ? null : value as ElementText
        if (isBlank) {
          theme.explicitNulls.add('axisTitleX')
          theme.explicitNulls.add('axisTitleY')
        } else {
          theme.explicitNulls.remove('axisTitleX')
          theme.explicitNulls.remove('axisTitleY')
        }
      } else if (propName == 'panelGrid') {
        theme.panelGridMajor = isBlank ? null : value as ElementLine
        theme.panelGridMinor = isBlank ? null : value as ElementLine
        if (isBlank) {
          theme.explicitNulls.add('panelGridMajor')
          theme.explicitNulls.add('panelGridMinor')
        } else {
          theme.explicitNulls.remove('panelGridMajor')
          theme.explicitNulls.remove('panelGridMinor')
        }
      } else if (theme.hasProperty(propName)) {
        theme.setProperty(propName, effectiveValue)
        if (isBlank) {
          theme.explicitNulls.add(propName)
        } else {
          theme.explicitNulls.remove(propName)
        }
      }
    }
    return theme
  }

  /**
   * Convert ggplot2 dot-notation theme keys to property names.
   * e.g., 'legend.position' -> 'legendPosition'
   *       'axis.line.x' -> 'axisLineX'
   *       'axis.line.x.bottom' -> 'axisLineX'
   * Note: axis-specific panel grid keys map to major/minor since per-axis grids
   * are not yet modeled separately.
   */
  private static String convertThemeKey(String key) {
    // Common mappings for dot-notation
    Map<String, String> mappings = [
      'legend.position': 'legendPosition',
      'legend.direction': 'legendDirection',
      'legend.background': 'legendBackground',
      'legend.key': 'legendKey',
      'legend.key.size': 'legendKeySize',
      'legend.title': 'legendTitle',
      'legend.text': 'legendText',
      'legend.margin': 'legendMargin',
      'axis.line': 'axisLine',
      'axis.line.x': 'axisLineX',
      'axis.line.y': 'axisLineY',
      'axis.line.x.bottom': 'axisLineX',
      'axis.line.x.top': 'axisLineX',
      'axis.line.y.left': 'axisLineY',
      'axis.line.y.right': 'axisLineY',
      'axis.ticks': 'axisTicks',
      'axis.ticks.x': 'axisTicksX',
      'axis.ticks.y': 'axisTicksY',
      'axis.text': 'axisText',
      'axis.text.x': 'axisTextX',
      'axis.text.y': 'axisTextY',
      'axis.title': 'axisTitle',
      'axis.title.x': 'axisTitleX',
      'axis.title.y': 'axisTitleY',
      'axis.ticks.length': 'axisTickLength',
      'panel.background': 'panelBackground',
      'panel.border': 'panelBorder',
      'panel.grid': 'panelGrid',
      'panel.grid.major': 'panelGridMajor',
      'panel.grid.major.x': 'panelGridMajor',
      'panel.grid.major.y': 'panelGridMajor',
      'panel.grid.minor': 'panelGridMinor',
      'panel.grid.minor.x': 'panelGridMinor',
      'panel.grid.minor.y': 'panelGridMinor',
      'panel.spacing': 'panelSpacing',
      'plot.background': 'plotBackground',
      'plot.title': 'plotTitle',
      'plot.subtitle': 'plotSubtitle',
      'plot.caption': 'plotCaption',
      'plot.margin': 'plotMargin',
      'strip.background': 'stripBackground',
      'strip.text': 'stripText'
    ]

    return mappings.get(key, key)
  }

  // ============ Theme Element Helpers ============

  /**
   * Create a line element for theme customization.
   * @param params Map with optional 'color'/'colour', 'size'/'linewidth', 'linetype', 'lineend'
   */
  static ElementLine element_line(Map params = [:]) {
    return new ElementLine(params)
  }

  /**
   * Create a text element for theme customization.
   * @param params Map with optional 'family', 'face', 'size', 'color'/'colour', 'hjust', 'vjust', 'angle'
   */
  static ElementText element_text(Map params = [:]) {
    return new ElementText(params)
  }

  /**
   * Create a rectangle element for theme customization.
   * @param params Map with optional 'fill', 'color'/'colour', 'size', 'linetype'
   */
  static ElementRect element_rect(Map params = [:]) {
    return new ElementRect(params)
  }

  /**
   * Create a blank element that removes the theme element.
   */
  static ElementBlank element_blank() {
    return new ElementBlank()
  }

  // ============ Coordinates ============

  static CoordFlip coord_flip() {
    return new CoordFlip()
  }

  static CoordFlip coord_flip(Map params) {
    return new CoordFlip(params)
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
    return new CoordPolar(params)
  }

  /**
   * Default Cartesian coordinate system with optional zooming.
   * Unlike scale limits which discard data outside the range,
   * coord_cartesian zooms to the specified range while keeping all data
   * for statistical calculations.
   *
   * @param params Map with optional 'xlim' and/or 'ylim' (List of [min, max])
   */
  static CoordCartesian coord_cartesian(Map params = [:]) {
    return new CoordCartesian(params)
  }

  /**
   * Fixed aspect ratio coordinate system.
   * Ensures equal scaling on both axes (1 unit on x = 1 unit on y in physical length).
   */
  static CoordFixed coord_fixed() {
    return new CoordFixed()
  }

  /**
   * Fixed aspect ratio coordinate system with specified ratio.
   *
   * @param ratio Aspect ratio: y units per x unit (1.0 = equal scaling)
   */
  static CoordFixed coord_fixed(double ratio) {
    return new CoordFixed(ratio)
  }

  /**
   * Fixed aspect ratio coordinate system with parameters.
   *
   * @param params Map with optional 'ratio', 'xlim', 'ylim'
   */
  static CoordFixed coord_fixed(Map params) {
    return new CoordFixed(params)
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

  static GeomBin2d geom_bin_2d(Map params) {
    return new GeomBin2d(params)
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

  static GeomContour geom_contour(Map params) {
    return new GeomContour(params)
  }

  static GeomContourFilled geom_contour_filled() {
    return new GeomContourFilled()
  }

  static GeomContourFilled geom_contour_filled(Map params) {
    return new GeomContourFilled(params)
  }

  static GeomCount geom_count() {
    return new GeomCount()
  }

  static GeomCount geom_count(Map params) {
    return new GeomCount(params)
  }

  static GeomDensity geom_density() {
    return new GeomDensity()
  }

  static GeomDensity geom_density(Map params) {
    return new GeomDensity(params)
  }

  static GeomErrorbar geom_errorbar() {
    return new GeomErrorbar()
  }

  static GeomErrorbar geom_errorbar(Map params) {
    return new GeomErrorbar(params)
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

  static GeomRug geom_rug() {
    return new GeomRug()
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

  /**
   * Convenience wrapper for linear model regression line.
   * Equivalent to geom_smooth with method='lm', se=false, colour='steelblue' and alpha=0.5.
   *
   * Usage:
   * <pre>
   * ggplot(mpg, aes('displ', 'hwy')) + geom_point() + geom_lm()
   * ggplot(mpg, aes('displ', 'hwy')) + geom_point() + geom_lm(degree: 2, colour: 'red')
   * ggplot(mpg, aes('displ', 'hwy')) + geom_point() + geom_lm(formula: 'y ~ poly(x, 2)', colour: 'red')
   * </pre>
   */
  static GeomSmooth geom_lm() {
    return geom_lm([:])
  }

  /**
   * Convenience wrapper for linear model regression line with custom parameters.
   *
   * @param params Map with optional:
   *   - degree: polynomial degree (1=linear, 2=quadratic, 3=cubic, etc.) - Groovy shortcut
   *   - formula: regression formula ('y ~ x' or 'y ~ poly(x, 2)') - R-style
   *   - colour/color: line color (default: steelblue at 0.5 alpha)
   *   - linewidth: line width (default: 2)
   *   - se: show standard error band (default: false)
   *   - any other geom_smooth parameters
   */
  static GeomSmooth geom_lm(Map params) {
    Map defaults = [
      se: false,
      method: 'lm',
      colour: 'steelblue',
      alpha: 0.5,
      linewidth: 2
    ]
    // Only add default formula if degree is not explicitly provided
    if (!params.containsKey('degree')) {
      defaults['formula'] = 'y ~ x'
    }
    // User params override defaults
    Map merged = defaults + params
    return new GeomSmooth(merged)
  }

  static GeomText geom_text() {
    return new GeomText()
  }

  static GeomText geom_text(Map params) {
    return new GeomText(params)
  }

  static GeomViolin geom_violin() {
    return new GeomViolin()
  }

  static GeomViolin geom_violin(Aes aes) {
    return new GeomViolin(aes)
  }

  static GeomViolin geom_violin(Map params) {
    return new GeomViolin(params)
  }

  static GeomVline geom_vline() {
    return new GeomVline()
  }

  static GeomVline geom_vline(Map params) {
    return new GeomVline(params)
  }

  // ============ Faceting ============

  /**
   * Wrap a 1D ribbon of panels into a 2D grid.
   * @param facets Column name(s) to facet by
   */
  static FacetWrap facet_wrap(String facet) {
    return new FacetWrap(facet)
  }

  /**
   * Wrap a 1D ribbon of panels into a 2D grid.
   * @param facets List of column names to facet by (creates combinations)
   */
  static FacetWrap facet_wrap(List<String> facets) {
    return new FacetWrap(facets)
  }

  /**
   * Wrap a 1D ribbon of panels into a 2D grid.
   * @param params Map with 'facets' (String or List), 'ncol', 'nrow', 'scales', 'dir'
   */
  static FacetWrap facet_wrap(Map params) {
    return new FacetWrap(params)
  }

  /**
   * Create a matrix of panels defined by row and column faceting variables.
   * @param params Map with 'rows' and/or 'cols' (String or List), 'scales', 'space', 'labeller'
   */
  static FacetGrid facet_grid(Map params) {
    return new FacetGrid(params)
  }

  /**
   * Create a matrix of panels using ggplot2-style formula syntax.
   *
   * Formula examples:
   * - "year ~ drv"       - rows by year, columns by drv
   * - ". ~ drv" or "~ drv" - columns only (no rows)
   * - "year ~ ."         - rows only (no columns)
   * - "year + cyl ~ drv" - multiple row variables
   * - "year ~ drv + gear" - multiple column variables
   *
   * @param formula Formula string with ~ separator
   */
  static FacetGrid facet_grid(String formula) {
    return new FacetGrid(formula)
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
   * Discrete color scale for categorical data.
   * Uses a default color palette that maps categorical values to colors.
   * This is the default scale used when mapping a discrete variable to color.
   *
   * @param params Optional map with 'values' (list of colors), 'name', 'limits', 'breaks', 'labels'
   */
  static ScaleColorManual scale_color_discrete(Map params = [:]) {
    return new ScaleColorManual(params)
  }

  /** British spelling alias for scale_color_discrete */
  static ScaleColorManual scale_colour_discrete(Map params = [:]) {
    return scale_color_discrete(params)
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

  /**
   * Continuous color scale for numeric data.
   * This is the default scale used when mapping a continuous variable to color.
   * Alias for scale_color_gradient().
   *
   * @param params Optional map with 'low', 'high', 'mid', 'midpoint', 'name', 'limits'
   */
  static ScaleColorGradient scale_color_continuous(Map params = [:]) {
    return new ScaleColorGradient(params)
  }

  /** British spelling alias for scale_color_continuous */
  static ScaleColorGradient scale_colour_continuous(Map params = [:]) {
    return scale_color_continuous(params)
  }

  // --- Viridis color scales ---

  /**
   * Viridis discrete color scale.
   * Creates colorblind-friendly, perceptually uniform color palettes.
   *
   * @param params Optional map with:
   *   - option: palette name ('viridis', 'magma', 'inferno', 'plasma', 'cividis', 'rocket', 'mako', 'turbo')
   *   - begin: start of color range (0-1), default 0
   *   - end: end of color range (0-1), default 1
   *   - direction: 1 for normal, -1 for reversed
   *   - alpha: transparency (0-1)
   */
  static ScaleColorViridis scale_color_viridis_d(Map params = [:]) {
    return new ScaleColorViridis(params)
  }

  /** British spelling alias for scale_color_viridis_d */
  static ScaleColorViridis scale_colour_viridis_d(Map params = [:]) {
    return scale_color_viridis_d(params)
  }

  /**
   * Viridis discrete fill scale.
   */
  static ScaleColorViridis scale_fill_viridis_d(Map params = [:]) {
    return new ScaleColorViridis(params + [aesthetic: 'fill'])
  }

  /**
   * Viridis continuous color scale for numeric data.
   * Maps numeric values to colors along the viridis palette.
   */
  static ScaleColorViridisC scale_color_viridis_c(Map params = [:]) {
    return new ScaleColorViridisC(params)
  }

  /** British spelling alias for scale_color_viridis_c */
  static ScaleColorViridisC scale_colour_viridis_c(Map params = [:]) {
    return scale_color_viridis_c(params)
  }

  /**
   * Viridis continuous fill scale for numeric data.
   */
  static ScaleColorViridisC scale_fill_viridis_c(Map params = [:]) {
    return new ScaleColorViridisC(params + [aesthetic: 'fill'])
  }

  // --- Fill scales ---

  /**
   * Manual fill scale for discrete data.
   */
  static ScaleColorManual scale_fill_manual(Map mappings) {
    return new ScaleColorManual(mappings + [aesthetic: 'fill'])
  }

  /**
   * Discrete fill scale for categorical data.
   * Uses a default color palette that maps categorical values to fill colors.
   * This is the default scale used when mapping a discrete variable to fill.
   *
   * @param params Optional map with 'values' (list of colors), 'name', 'limits', 'breaks', 'labels'
   */
  static ScaleColorManual scale_fill_discrete(Map params = [:]) {
    return new ScaleColorManual(params + [aesthetic: 'fill'])
  }

  /**
   * Continuous fill gradient scale.
   */
  static ScaleColorGradient scale_fill_gradient(Map params = [:]) {
    return new ScaleColorGradient(params + [aesthetic: 'fill'])
  }

  /**
   * Two-color fill gradient.
   */
  static ScaleColorGradient scale_fill_gradient(String low, String high) {
    return new ScaleColorGradient([low: low, high: high, aesthetic: 'fill'])
  }

  /**
   * Diverging fill gradient with mid color.
   */
  static ScaleColorGradient scale_fill_gradient2(Map params = [:]) {
    return new ScaleColorGradient(params + [mid: params.containsKey('mid') ? params.mid : 'white', aesthetic: 'fill'])
  }

  /**
   * Continuous fill scale for numeric data.
   * This is the default scale used when mapping a continuous variable to fill.
   * Alias for scale_fill_gradient().
   *
   * @param params Optional map with 'low', 'high', 'mid', 'midpoint', 'name', 'limits'
   */
  static ScaleColorGradient scale_fill_continuous(Map params = [:]) {
    return new ScaleColorGradient(params + [aesthetic: 'fill'])
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

    static Factor factor(List column) {
      return new Factor(ListConverter.toStrings(column))
    }
  }
}
