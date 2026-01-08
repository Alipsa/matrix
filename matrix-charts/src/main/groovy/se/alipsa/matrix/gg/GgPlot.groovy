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
import se.alipsa.matrix.gg.geom.GeomErrorbarh
import se.alipsa.matrix.gg.geom.GeomFreqpoly
import se.alipsa.matrix.gg.geom.GeomHistogram
import se.alipsa.matrix.gg.geom.GeomHline
import se.alipsa.matrix.gg.geom.GeomJitter
import se.alipsa.matrix.gg.geom.GeomLabel
import se.alipsa.matrix.gg.geom.GeomLine
import se.alipsa.matrix.gg.geom.GeomQq
import se.alipsa.matrix.gg.geom.GeomQqLine
import se.alipsa.matrix.gg.geom.GeomRug
import se.alipsa.matrix.gg.geom.GeomSegment
import se.alipsa.matrix.gg.geom.GeomSmooth
import se.alipsa.matrix.gg.geom.GeomSpoke
import se.alipsa.matrix.gg.geom.GeomCurve
import se.alipsa.matrix.gg.geom.GeomText
import se.alipsa.matrix.gg.geom.GeomVline
import se.alipsa.matrix.gg.geom.GeomRibbon
import se.alipsa.matrix.gg.geom.GeomTile
import se.alipsa.matrix.gg.geom.GeomRaster
import se.alipsa.matrix.gg.geom.GeomRect
import se.alipsa.matrix.gg.geom.GeomPath
import se.alipsa.matrix.gg.geom.GeomPolygon
import se.alipsa.matrix.gg.geom.GeomStep
import se.alipsa.matrix.gg.geom.GeomPointrange
import se.alipsa.matrix.gg.geom.GeomLinerange
import se.alipsa.matrix.gg.geom.GeomCrossbar
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.AfterScale
import se.alipsa.matrix.gg.aes.AfterStat
import se.alipsa.matrix.gg.aes.CutWidth
import se.alipsa.matrix.gg.aes.Expression
import se.alipsa.matrix.gg.aes.Factor
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.CoordCartesian
import se.alipsa.matrix.gg.coord.CoordFixed
import se.alipsa.matrix.gg.coord.CoordFlip
import se.alipsa.matrix.gg.coord.CoordPolar
import se.alipsa.matrix.gg.facet.FacetGrid
import se.alipsa.matrix.gg.facet.FacetWrap
import se.alipsa.matrix.gg.geom.GeomAbline
import se.alipsa.matrix.gg.geom.GeomArea
import se.alipsa.matrix.gg.geom.GeomBar
import se.alipsa.matrix.gg.geom.GeomCol
import se.alipsa.matrix.gg.geom.GeomContourFilled
import se.alipsa.matrix.gg.geom.GeomPoint
import se.alipsa.matrix.gg.geom.GeomViolin
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.position.Position
import se.alipsa.matrix.gg.scale.ScaleColorGradient
import se.alipsa.matrix.gg.scale.ScaleColorGradientN
import se.alipsa.matrix.gg.scale.ScaleColorManual
import se.alipsa.matrix.gg.scale.ScaleColorBrewer
import se.alipsa.matrix.gg.scale.ScaleColorDistiller
import se.alipsa.matrix.gg.scale.ScaleColorGrey
import se.alipsa.matrix.gg.scale.ScaleColorViridis
import se.alipsa.matrix.gg.scale.ScaleColorViridisC
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleAlphaContinuous
import se.alipsa.matrix.gg.scale.ScaleAlphaDiscrete
import se.alipsa.matrix.gg.scale.ScaleAlphaBinned
import se.alipsa.matrix.gg.scale.ScaleSizeContinuous
import se.alipsa.matrix.gg.scale.ScaleSizeDiscrete
import se.alipsa.matrix.gg.scale.ScaleSizeBinned
import se.alipsa.matrix.gg.scale.ScaleSizeArea
import se.alipsa.matrix.gg.scale.ScaleRadius
import se.alipsa.matrix.gg.scale.ScaleXContinuous
import se.alipsa.matrix.gg.scale.ScaleXDiscrete
import se.alipsa.matrix.gg.scale.ScaleYContinuous
import se.alipsa.matrix.gg.scale.ScaleYDiscrete
import se.alipsa.matrix.gg.scale.ScaleXLog10
import se.alipsa.matrix.gg.scale.ScaleYLog10
import se.alipsa.matrix.gg.scale.ScaleXSqrt
import se.alipsa.matrix.gg.scale.ScaleYSqrt
import se.alipsa.matrix.gg.scale.ScaleXReverse
import se.alipsa.matrix.gg.scale.ScaleYReverse
import se.alipsa.matrix.gg.scale.ScaleXDate
import se.alipsa.matrix.gg.scale.ScaleYDate
import se.alipsa.matrix.gg.scale.ScaleXDatetime
import se.alipsa.matrix.gg.scale.ScaleYDatetime
import se.alipsa.matrix.gg.stat.StatsBin2D
import se.alipsa.matrix.gg.stat.StatsBin
import se.alipsa.matrix.gg.stat.StatsBoxplot
import se.alipsa.matrix.gg.stat.StatsContour
import se.alipsa.matrix.gg.stat.StatsContourFilled
import se.alipsa.matrix.gg.stat.StatsCount
import se.alipsa.matrix.gg.stat.StatsDensity
import se.alipsa.matrix.gg.stat.StatsSmooth
import se.alipsa.matrix.gg.stat.StatsSum
import se.alipsa.matrix.gg.stat.StatsYDensity
import se.alipsa.matrix.gg.stat.StatsEcdf
import se.alipsa.matrix.gg.stat.StatsQq
import se.alipsa.matrix.gg.stat.StatsQqLine
import se.alipsa.matrix.gg.layer.Layer
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
  static final boolean T = true
  static final boolean F = false

  // ============ Core functions ============

  static GgChart ggplot(Matrix data, Aes aes) {
    return new GgChart(data, aes)
  }

  /**
   * Create a ggplot chart from a map containing data and mapping.
   *
   * Example: ggplot(data: df, mapping: aes(x: 'x', y: 'y'))
   *
   * @param params map with keys: data (Matrix) and mapping (Aes)
   * @return a new GgChart instance
   */
  static GgChart ggplot(Map params) {
    if (params == null) {
      throw new IllegalArgumentException('ggplot requires data and mapping parameters')
    }
    Matrix data = params.data as Matrix
    Aes mapping = params.mapping as Aes
    if (data == null || mapping == null) {
      throw new IllegalArgumentException('ggplot requires data (Matrix) and mapping (Aes)')
    }
    return new GgChart(data, mapping)
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

  /**
   * Create aesthetic mappings with positional x.
   * Accepts column names (String), constants via I(...), Factor, AfterStat, or closures.
   *
   * Example: aes('cty')
   *
   * @param x x mapping (column name, Factor, Identity, AfterStat, or closure)
   * @return a new Aes instance
   */
  static Aes aes(Object x) {
    Aes aes = new Aes()
    aes.x = x
    return aes
  }

  /**
   * Create aesthetic mappings with positional x and y.
   * Accepts column names (String), constants via I(...), Factor, AfterStat, or closures.
   *
   * Example: aes('cty', 'hwy')
   *
   * @param x x mapping (column name, Factor, Identity, AfterStat, or closure)
   * @param y y mapping (column name, Factor, Identity, AfterStat, or closure)
   * @return a new Aes instance
   */
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
   * Create aesthetic mappings with positional x and additional named parameters.
   * Accepts column names (String), constants via I(...), Factor, AfterStat, or closures.
   *
   * Example: aes([colour: 'class'], 'cty')
   *
   * @param params named aesthetic mappings (e.g., colour, fill, size)
   * @param x x mapping (column name, Factor, Identity, AfterStat, or closure)
   * @return a new Aes instance
   */
  static Aes aes(Map params, Object x) {
    Aes aes = new Aes(params)
    aes.x = x
    return aes
  }

  /**
   * Create aesthetic mappings with positional x, y and additional named parameters.
   * Accepts column names (String), constants via I(...), Factor, AfterStat, or closures.
   *
   * Example: aes([colour: 'class'], 'cty', 'hwy')
   *
   * @param params named aesthetic mappings (e.g., colour, fill, size)
   * @param x x mapping (column name, Factor, Identity, AfterStat, or closure)
   * @param y y mapping (column name, Factor, Identity, AfterStat, or closure)
   * @return a new Aes instance
   */
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
   * Create an after_scale reference for scaled aesthetics.
   *
   * @param aesthetic the scaled aesthetic name
   * @return an AfterScale wrapper
   */
  static AfterScale after_scale(String aesthetic) {
    return new AfterScale(aesthetic)
  }

  /**
   * Create a position_nudge specification.
   *
   * @param params optional params with x and/or y offsets
   * @return a Position instance for nudge adjustments
   */
  static Position position_nudge(Map params = [:]) {
    return new Position(PositionType.NUDGE, params)
  }

  /**
   * Create an expansion specification for continuous scales.
   *
   * @param params map with mult and/or add values
   * @return list with [mult, add]
   */
  static List<Number> expansion(Map params = [:]) {
    Number mult = params.mult as Number
    Number add = params.add as Number
    return [mult, add] as List<Number>
  }

  /**
   * Create an expansion specification for continuous scales.
   *
   * @param mult multiplicative expansion
   * @param add additive expansion
   * @return list with [mult, add]
   */
  static List<Number> expansion(Number mult, Number add = null) {
    return [mult, add] as List<Number>
  }

  /**
   * Quote facet variables for facet helpers.
   *
   * @param vars facet variable names
   * @return list of variable names
   */
  static List<String> vars(Object... vars) {
    if (vars == null) return []
    List<Object> flattened = []
    vars.each { value ->
      if (value instanceof Collection) {
        flattened.addAll(value as Collection)
      } else {
        flattened << value
      }
    }
    return flattened.findAll { it != null }.collect { it.toString() }
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
   * Accepts constants, column names, or lists of values aligned with the data rows.
   *
   * Examples:
   * - factor(1)
   * - factor('class')
   * - factor(mtcars['cyl'])
   *
   * @param value constant, column name, or list of values
   * @return a Factor wrapper
   */
  static Factor factor(Object value) {
    return new Factor(value)
  }

  /**
   * Cut a continuous variable into bins of fixed width.
   * Similar to ggplot2's cut_width function.
   *
   * Creates a categorical variable from a continuous column by dividing it
   * into bins of the specified width. Useful for grouping continuous data
   * in boxplots, histograms, and other visualizations.
   *
   * Examples:
   * - cut_width('displ', 1)       - bins like (1,2], (2,3], etc.
   * - cut_width('displ', 0.5)     - bins like (1,1.5], (1.5,2], etc.
   *
   * @param column the column name containing continuous values
   * @param width the width of each bin
   * @return a CutWidth wrapper
   */
  static CutWidth cut_width(String column, Number width) {
    return new CutWidth(column, width)
  }

  /**
   * Cut a continuous variable into bins of fixed width with additional options.
   *
   * @param column the column name containing continuous values
   * @param width the width of each bin
   * @param params optional map with 'center', 'boundary', or 'closed' ('right' or 'left')
   * @return a CutWidth wrapper
   */
  static CutWidth cut_width(Map params, String column, Number width) {
    Number center = params.center as Number
    Number boundary = params.boundary as Number
    boolean closedRight = params.closed != 'left'
    return new CutWidth(column, width, center, boundary, closedRight)
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
      label.x = params.x?.toString()
      label.xSet = true
    }
    if (params.containsKey('y')) {
      label.y = params.y?.toString()
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

  // ============ Annotations ============

  /**
   * Add an annotation layer with fixed coordinates and values.
   *
   * @param geomType annotation geom type (e.g., 'text', 'rect', 'segment')
   * @param params fixed coordinates and styling options
   * @return a Layer with annotation data
   */
  static Layer annotate(String geomType, Map params) {
    return new Annotate(geomType, params).toLayer()
  }

  /**
   * Add an annotation layer using named parameters.
   * Note: Map must come first for Groovy to collect named arguments.
   *
   * @param params fixed coordinates and styling options
   * @param geomType annotation geom type (e.g., 'text', 'rect', 'segment')
   * @return a Layer with annotation data
   */
  static Layer annotate(Map params, String geomType) {
    return annotate(geomType, params)
  }

  // ============ Guide System ============

  /**
   * Control which guides appear for each aesthetic.
   *
   * Example: guides(color: 'none', fill: guide_colourbar())
   *
   * @param params map of aesthetic -> guide spec
   * @return a Guides specification
   */
  static Guides guides(Map params) {
    return new Guides(params)
  }

  /**
   * Create a legend guide specification.
   *
   * @param params optional legend options
   */
  static Guide guide_legend(Map params = [:]) {
    return new Guide('legend', params)
  }

  /**
   * Create a colorbar guide specification (continuous color guide).
   *
   * @param params optional colorbar options
   */
  static Guide guide_colorbar(Map params = [:]) {
    return new Guide('colorbar', params)
  }

  /** British spelling alias for guide_colorbar */
  static Guide guide_colourbar(Map params = [:]) {
    return guide_colorbar(params)
  }

  // ============ Limit helpers ============

  /**
   * Set multiple axis limits at once.
   *
   * Example: lims(x: [0, 10], y: [0, 5])
   *
   * @param params map containing x and/or y limits
   * @return list of scale specifications
   */
  static List lims(Map params) {
    if (params == null || params.isEmpty()) {
      throw new IllegalArgumentException('lims requires at least one of x or y')
    }
    List scales = []
    if (params.containsKey('x')) {
      Scale xScale = buildLimitScale('x', params.x)
      if (xScale != null) {
        scales << xScale
      }
    }
    if (params.containsKey('y')) {
      Scale yScale = buildLimitScale('y', params.y)
      if (yScale != null) {
        scales << yScale
      }
    }
    return scales
  }

  /**
   * Expand axis limits to include specific values.
   *
   * Example: expand_limits(x: [0, 10], y: 100)
   *
   * @param params map containing x and/or y values
   * @return a Layer with blank geometry to extend scales
   */
  static Layer expand_limits(Map params) {
    if (params == null || params.isEmpty()) {
      throw new IllegalArgumentException('expand_limits requires at least one of x or y')
    }

    List<String> columns = []
    List<List<Object>> values = []
    Map<String, Object> aesMap = [:]

    if (params.containsKey('x')) {
      columns << 'x'
      values << coerceToList(params.x)
      aesMap['x'] = 'x'
    }
    if (params.containsKey('y')) {
      columns << 'y'
      values << coerceToList(params.y)
      aesMap['y'] = 'y'
    }

    if (columns.isEmpty()) {
      throw new IllegalArgumentException('expand_limits requires at least one of x or y')
    }

    int nRows = 1
    values.each { List<Object> val ->
      nRows = Math.max(nRows, val.size())
    }

    List<List<Object>> expandedValues = values.collect { List<Object> val ->
      if (val.size() == nRows) {
        return val
      }
      if (val.size() == 1) {
        return (0..<nRows).collect { val[0] }
      }
      throw new IllegalArgumentException('Inconsistent expand_limits vector lengths')
    }

    List<List<Object>> rows = (0..<nRows).collect { rowIdx ->
      expandedValues.collect { it[rowIdx] }
    }

    Matrix data = Matrix.builder()
        .columnNames(columns)
        .rows(rows)
        .build()

    return new Layer(
        geom: new GeomBlank(),
        data: data,
        aes: new Aes(aesMap),
        inheritAes: false
    )
  }

  private static Scale buildLimitScale(String aesthetic, Object limitSpec) {
    if (limitSpec == null) {
      return null
    }
    List values = coerceToList(limitSpec)
    if (values == null || values.isEmpty()) {
      return null
    }

    List nonNull = values.findAll { it != null }
    boolean hasDateTime = nonNull.any { isDateTimeValue(it) }
    boolean hasDateOnly = nonNull.any { it instanceof java.time.LocalDate }
    boolean isNumeric = nonNull.isEmpty() || nonNull.every { it instanceof Number }
    boolean usePair = values.size() <= 2

    if ((hasDateTime || hasDateOnly) && usePair) {
      List limits = toLimitPair(values)
      if (aesthetic == 'x') {
        return hasDateTime ? new ScaleXDatetime(limits: limits) : new ScaleXDate(limits: limits)
      }
      return hasDateTime ? new ScaleYDatetime(limits: limits) : new ScaleYDate(limits: limits)
    }

    if (isNumeric && usePair) {
      List limits = toLimitPair(values)
      if (aesthetic == 'x') {
        return new ScaleXContinuous(limits: limits)
      }
      return new ScaleYContinuous(limits: limits)
    }

    if (aesthetic == 'x') {
      return new ScaleXDiscrete(limits: values)
    }
    return new ScaleYDiscrete(limits: values)
  }

  private static List<Object> coerceToList(Object value) {
    if (value == null) {
      return [null]
    }
    if (value instanceof List) {
      return value as List<Object>
    }
    if (value instanceof Object[]) {
      return (value as Object[]).toList()
    }
    if (value instanceof Iterable) {
      List<Object> result = []
      (value as Iterable).each { result << it }
      return result
    }
    return [value]
  }

  private static boolean isDateTimeValue(Object value) {
    return value instanceof Date ||
        value instanceof java.time.LocalDateTime ||
        value instanceof java.time.Instant ||
        value instanceof java.time.OffsetDateTime ||
        value instanceof java.time.ZonedDateTime
  }

  private static List<Object> toLimitPair(List values) {
    if (values.size() == 1) {
      return [values[0], values[0]]
    }
    if (values.size() >= 2) {
      return [values[0], values[1]]
    }
    return [null, null]
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
   * Completely blank theme - only data is displayed.
   */
  static Theme theme_void() {
    return Themes.void_()
  }

  /**
   * Light theme with light gray backgrounds and subtle grid lines.
   */
  static Theme theme_light() {
    return Themes.light()
  }

  /**
   * Dark theme with dark backgrounds and light text/grid lines.
   */
  static Theme theme_dark() {
    return Themes.dark()
  }

  /**
   * Line draw theme - crisp black lines on white background.
   */
  static Theme theme_linedraw() {
    return Themes.linedraw()
  }

  /**
   * Customize theme elements.
   * Supports both camelCase (e.g., 'legendPosition') and dot-notation (e.g., 'legend.position').
   */
  static Theme theme(Map params) {
    Themes.theme(params)
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
  static CoordFixed coord_fixed(Number ratio) {
    return new CoordFixed(ratio as BigDecimal)
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

  /**
   * Create an abline geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomAbline instance
   */
  static GeomAbline geom_abline(Aes mapping) {
    return geom_abline([mapping: mapping])
  }

  static GeomAbline geom_abline(Map params) {
    return new GeomAbline(params)
  }

  static GeomArea geom_area() {
    return new GeomArea()
  }

  /**
   * Create an area geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomArea instance
   */
  static GeomArea geom_area(Aes mapping) {
    return geom_area([mapping: mapping])
  }

  static GeomArea geom_area(Map params) {
    return new GeomArea(params)
  }

  static GeomBar geom_bar() {
    return new GeomBar()
  }

  /**
   * Create a bar geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomBar instance
   */
  static GeomBar geom_bar(Aes mapping) {
    return geom_bar([mapping: mapping])
  }

  static GeomBar geom_bar(Map params) {
    return new GeomBar(params)
  }

  static GeomBin2d geom_bin_2d() {
    return new GeomBin2d()
  }

  /**
   * Create a 2D bin geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomBin2d instance
   */
  static GeomBin2d geom_bin_2d(Aes mapping) {
    return geom_bin_2d([mapping: mapping])
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

  /**
   * Create a boxplot geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomBoxplot instance
   */
  static GeomBoxplot geom_boxplot(Aes mapping) {
    return geom_boxplot([mapping: mapping])
  }

  static GeomBoxplot geom_boxplot(Map params) {
    return new GeomBoxplot(params)
  }

  static GeomCol geom_col() {
    return new GeomCol()
  }

  /**
   * Create a column geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomCol instance
   */
  static GeomCol geom_col(Aes mapping) {
    return geom_col([mapping: mapping])
  }

  static GeomCol geom_col(Map params) {
    return new GeomCol(params)
  }

  static GeomContour geom_contour() {
    return new GeomContour()
  }

  /**
   * Create a contour geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomContour instance
   */
  static GeomContour geom_contour(Aes mapping) {
    return geom_contour([mapping: mapping])
  }

  static GeomContour geom_contour(Map params) {
    return new GeomContour(params)
  }

  static GeomContourFilled geom_contour_filled() {
    return new GeomContourFilled()
  }

  /**
   * Create a filled contour geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomContourFilled instance
   */
  static GeomContourFilled geom_contour_filled(Aes mapping) {
    return geom_contour_filled([mapping: mapping])
  }

  static GeomContourFilled geom_contour_filled(Map params) {
    return new GeomContourFilled(params)
  }

  static GeomCount geom_count() {
    return new GeomCount()
  }

  /**
   * Create a count geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomCount instance
   */
  static GeomCount geom_count(Aes mapping) {
    return geom_count([mapping: mapping])
  }

  static GeomCount geom_count(Map params) {
    return new GeomCount(params)
  }

  static GeomDensity geom_density() {
    return new GeomDensity()
  }

  /**
   * Create a density geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomDensity instance
   */
  static GeomDensity geom_density(Aes mapping) {
    return geom_density([mapping: mapping])
  }

  static GeomDensity geom_density(Map params) {
    return new GeomDensity(params)
  }

  /**
   * Create a frequency polygon geom.
   * Uses stat_bin and draws a line through bin centers.
   *
   * @return a new GeomFreqpoly instance
   */
  static GeomFreqpoly geom_freqpoly() {
    return new GeomFreqpoly()
  }

  /**
   * Create a frequency polygon geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomFreqpoly instance
   */
  static GeomFreqpoly geom_freqpoly(Aes mapping) {
    return geom_freqpoly([mapping: mapping])
  }

  /**
   * Create a frequency polygon geom with parameters.
   *
   * @param params geom parameters
   * @return a new GeomFreqpoly instance
   */
  static GeomFreqpoly geom_freqpoly(Map params) {
    return new GeomFreqpoly(params)
  }

  static GeomErrorbar geom_errorbar() {
    return new GeomErrorbar()
  }

  /**
   * Create an errorbar geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomErrorbar instance
   */
  static GeomErrorbar geom_errorbar(Aes mapping) {
    return geom_errorbar([mapping: mapping])
  }

  static GeomErrorbar geom_errorbar(Map params) {
    return new GeomErrorbar(params)
  }

  static GeomErrorbarh geom_errorbarh() {
    return new GeomErrorbarh()
  }

  /**
   * Create a horizontal error bar geom with a layer-specific aesthetic mapping.
   * Horizontal error bars display x intervals at each y position.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomErrorbarh instance
   */
  static GeomErrorbarh geom_errorbarh(Aes mapping) {
    return geom_errorbarh([mapping: mapping])
  }

  static GeomErrorbarh geom_errorbarh(Map params) {
    return new GeomErrorbarh(params)
  }

  static GeomHistogram geom_histogram() {
    return new GeomHistogram()
  }

  /**
   * Create a histogram geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomHistogram instance
   */
  static GeomHistogram geom_histogram(Aes mapping) {
    return geom_histogram([mapping: mapping])
  }

  static GeomHistogram geom_histogram(Map params) {
    return new GeomHistogram(params)
  }

  static GeomHline geom_hline() {
    return new GeomHline()
  }

  /**
   * Create a horizontal line geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomHline instance
   */
  static GeomHline geom_hline(Aes mapping) {
    return geom_hline([mapping: mapping])
  }

  static GeomHline geom_hline(Map params) {
    return new GeomHline(params)
  }

  static GeomLabel geom_label() {
    return new GeomLabel()
  }

  /**
   * Create a label geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomLabel instance
   */
  static GeomLabel geom_label(Aes mapping) {
    return geom_label([mapping: mapping])
  }

  static GeomLabel geom_label(Map params) {
    return new GeomLabel(params)
  }

  static GeomLine geom_line() {
    return new GeomLine()
  }

  /**
   * Create a line geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomLine instance
   */
  static GeomLine geom_line(Aes mapping) {
    return geom_line([mapping: mapping])
  }

  static GeomLine geom_line(Map params) {
    return new GeomLine(params)
  }

  static GeomPoint geom_point() {
    return new GeomPoint()
  }

  /**
   * Create a point geom with a layer-specific aesthetic mapping.
   *
   * Example: geom_point(aes(color: 'species'))
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomPoint instance
   */
  static GeomPoint geom_point(Aes mapping) {
    return geom_point([mapping: mapping])
  }

  static GeomPoint geom_point(Map params) {
    return new GeomPoint(params)
  }

  /**
   * Create a jittered point geom.
   *
   * @return a new GeomJitter instance
   */
  static GeomJitter geom_jitter() {
    return new GeomJitter()
  }

  /**
   * Create a jittered point geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomJitter instance
   */
  static GeomJitter geom_jitter(Aes mapping) {
    return geom_jitter([mapping: mapping])
  }

  /**
   * Create a jittered point geom with parameters.
   *
   * @param params geom parameters
   * @return a new GeomJitter instance
   */
  static GeomJitter geom_jitter(Map params) {
    return new GeomJitter(params)
  }

  /**
   * Create a Q-Q plot point geom.
   *
   * @return a new GeomQq instance
   */
  static GeomQq geom_qq() {
    return new GeomQq()
  }

  /**
   * Create a Q-Q plot point geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomQq instance
   */
  static GeomQq geom_qq(Aes mapping) {
    return geom_qq([mapping: mapping])
  }

  /**
   * Create a Q-Q plot point geom with parameters.
   *
   * @param params geom parameters
   * @return a new GeomQq instance
   */
  static GeomQq geom_qq(Map params) {
    return new GeomQq(params)
  }

  /**
   * Create a Q-Q plot reference line geom.
   *
   * @return a new GeomQqLine instance
   */
  static GeomQqLine geom_qq_line() {
    return new GeomQqLine()
  }

  /**
   * Create a Q-Q plot reference line geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomQqLine instance
   */
  static GeomQqLine geom_qq_line(Aes mapping) {
    return geom_qq_line([mapping: mapping])
  }

  /**
   * Create a Q-Q plot reference line geom with parameters.
   *
   * @param params geom parameters
   * @return a new GeomQqLine instance
   */
  static GeomQqLine geom_qq_line(Map params) {
    return new GeomQqLine(params)
  }

  static GeomRug geom_rug() {
    return new GeomRug()
  }

  /**
   * Create a rug geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomRug instance
   */
  static GeomRug geom_rug(Aes mapping) {
    return geom_rug([mapping: mapping])
  }

  static GeomRug geom_rug(Map params) {
    return new GeomRug(params)
  }

  static GeomSegment geom_segment() {
    return new GeomSegment()
  }

  /**
   * Create a segment geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomSegment instance
   */
  static GeomSegment geom_segment(Aes mapping) {
    return geom_segment([mapping: mapping])
  }

  static GeomSegment geom_segment(Map params) {
    return new GeomSegment(params)
  }

  static GeomSpoke geom_spoke() {
    return new GeomSpoke()
  }

  /**
   * Create a spoke geom with a layer-specific aesthetic mapping.
   * Spokes are radial line segments defined by angle and radius.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomSpoke instance
   */
  static GeomSpoke geom_spoke(Aes mapping) {
    return geom_spoke([mapping: mapping])
  }

  static GeomSpoke geom_spoke(Map params) {
    return new GeomSpoke(params)
  }

  static GeomCurve geom_curve() {
    return new GeomCurve()
  }

  /**
   * Create a curve geom with a layer-specific aesthetic mapping.
   * Curves are smooth curved line segments between two points.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomCurve instance
   */
  static GeomCurve geom_curve(Aes mapping) {
    return geom_curve([mapping: mapping])
  }

  static GeomCurve geom_curve(Map params) {
    return new GeomCurve(params)
  }

  static GeomSmooth geom_smooth() {
    return new GeomSmooth()
  }

  /**
   * Create a smooth geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomSmooth instance
   */
  static GeomSmooth geom_smooth(Aes mapping) {
    return geom_smooth([mapping: mapping])
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
   * Convenience wrapper for linear model regression line with layer-specific aesthetics.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomSmooth instance
   */
  static GeomSmooth geom_lm(Aes mapping) {
    return geom_lm([mapping: mapping])
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

  /**
   * Create a text geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomText instance
   */
  static GeomText geom_text(Aes mapping) {
    return geom_text([mapping: mapping])
  }

  static GeomText geom_text(Map params) {
    return new GeomText(params)
  }

  static GeomViolin geom_violin() {
    return new GeomViolin()
  }

  /**
   * Create a violin geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomViolin instance
   */
  static GeomViolin geom_violin(Aes mapping) {
    return geom_violin([mapping: mapping])
  }

  static GeomViolin geom_violin(Map params) {
    return new GeomViolin(params)
  }

  static GeomVline geom_vline() {
    return new GeomVline()
  }

  /**
   * Create a vertical line geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomVline instance
   */
  static GeomVline geom_vline(Aes mapping) {
    return geom_vline([mapping: mapping])
  }

  static GeomVline geom_vline(Map params) {
    return new GeomVline(params)
  }

  static GeomRibbon geom_ribbon() {
    return new GeomRibbon()
  }

  /**
   * Create a ribbon geom with a layer-specific aesthetic mapping.
   * Ribbons display a filled area between ymin and ymax values.
   *
   * @param mapping aesthetic mapping for this layer (should include ymin and ymax)
   * @return a new GeomRibbon instance
   */
  static GeomRibbon geom_ribbon(Aes mapping) {
    return geom_ribbon([mapping: mapping])
  }

  static GeomRibbon geom_ribbon(Map params) {
    return new GeomRibbon(params)
  }

  static GeomTile geom_tile() {
    return new GeomTile()
  }

  /**
   * Create a tile geom with a layer-specific aesthetic mapping.
   * Tiles are rectangular shapes centered at x,y positions, commonly used for heatmaps.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomTile instance
   */
  static GeomTile geom_tile(Aes mapping) {
    return geom_tile([mapping: mapping])
  }

  static GeomTile geom_tile(Map params) {
    return new GeomTile(params)
  }

  static GeomRaster geom_raster() {
    return new GeomRaster()
  }

  /**
   * Create a raster geom with a layer-specific aesthetic mapping.
   * Raster is optimized for regular grids (image data, heatmaps on regular grids).
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomRaster instance
   */
  static GeomRaster geom_raster(Aes mapping) {
    return geom_raster([mapping: mapping])
  }

  static GeomRaster geom_raster(Map params) {
    return new GeomRaster(params)
  }

  static GeomRect geom_rect() {
    return new GeomRect()
  }

  /**
   * Create a rect geom with a layer-specific aesthetic mapping.
   * Rectangles defined by xmin, xmax, ymin, ymax coordinates.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomRect instance
   */
  static GeomRect geom_rect(Aes mapping) {
    return geom_rect([mapping: mapping])
  }

  static GeomRect geom_rect(Map params) {
    return new GeomRect(params)
  }

  static GeomPath geom_path() {
    return new GeomPath()
  }

  /**
   * Create a path geom with a layer-specific aesthetic mapping.
   * Unlike geom_line, geom_path connects points in data order (not sorted by x).
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomPath instance
   */
  static GeomPath geom_path(Aes mapping) {
    return geom_path([mapping: mapping])
  }

  static GeomPath geom_path(Map params) {
    return new GeomPath(params)
  }

  static GeomPolygon geom_polygon() {
    return new GeomPolygon()
  }

  /**
   * Create a polygon geom with a layer-specific aesthetic mapping.
   * Polygons automatically close the path and fill the enclosed area.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomPolygon instance
   */
  static GeomPolygon geom_polygon(Aes mapping) {
    return geom_polygon([mapping: mapping])
  }

  static GeomPolygon geom_polygon(Map params) {
    return new GeomPolygon(params)
  }

  static GeomStep geom_step() {
    return new GeomStep()
  }

  /**
   * Create a step geom with a layer-specific aesthetic mapping.
   * Step plots create staircase patterns, useful for visualizing stepwise changes.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomStep instance
   */
  static GeomStep geom_step(Aes mapping) {
    return geom_step([mapping: mapping])
  }

  static GeomStep geom_step(Map params) {
    return new GeomStep(params)
  }

  static GeomPointrange geom_pointrange() {
    return new GeomPointrange()
  }

  /**
   * Create a pointrange geom with a layer-specific aesthetic mapping.
   * Displays a point with a vertical line range (ymin to ymax).
   *
   * @param mapping aesthetic mapping for this layer (should include ymin and ymax)
   * @return a new GeomPointrange instance
   */
  static GeomPointrange geom_pointrange(Aes mapping) {
    return geom_pointrange([mapping: mapping])
  }

  static GeomPointrange geom_pointrange(Map params) {
    return new GeomPointrange(params)
  }

  static GeomLinerange geom_linerange() {
    return new GeomLinerange()
  }

  /**
   * Create a linerange geom with a layer-specific aesthetic mapping.
   * Displays vertical line ranges (ymin to ymax) without end caps.
   *
   * @param mapping aesthetic mapping for this layer (should include ymin and ymax)
   * @return a new GeomLinerange instance
   */
  static GeomLinerange geom_linerange(Aes mapping) {
    return geom_linerange([mapping: mapping])
  }

  static GeomLinerange geom_linerange(Map params) {
    return new GeomLinerange(params)
  }

  static GeomCrossbar geom_crossbar() {
    return new GeomCrossbar()
  }

  /**
   * Create a crossbar geom with a layer-specific aesthetic mapping.
   * Displays a box from ymin to ymax with a horizontal line at y.
   *
   * @param mapping aesthetic mapping for this layer (should include ymin and ymax)
   * @return a new GeomCrossbar instance
   */
  static GeomCrossbar geom_crossbar(Aes mapping) {
    return geom_crossbar([mapping: mapping])
  }

  static GeomCrossbar geom_crossbar(Map params) {
    return new GeomCrossbar(params)
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

  // --- Transformed position scales ---

  /**
   * Log10-transformed scale for x-axis.
   * Useful for data spanning multiple orders of magnitude.
   */
  static ScaleXLog10 scale_x_log10(Map params = [:]) {
    return new ScaleXLog10(params)
  }

  /**
   * Log10-transformed scale for y-axis.
   * Useful for data spanning multiple orders of magnitude.
   */
  static ScaleYLog10 scale_y_log10(Map params = [:]) {
    return new ScaleYLog10(params)
  }

  /**
   * Square root-transformed scale for x-axis.
   * Useful for count data or to reduce right skew.
   */
  static ScaleXSqrt scale_x_sqrt(Map params = [:]) {
    return new ScaleXSqrt(params)
  }

  /**
   * Square root-transformed scale for y-axis.
   * Useful for count data or to reduce right skew.
   */
  static ScaleYSqrt scale_y_sqrt(Map params = [:]) {
    return new ScaleYSqrt(params)
  }

  /**
   * Reversed scale for x-axis.
   * High values appear on the left, low values on the right.
   */
  static ScaleXReverse scale_x_reverse(Map params = [:]) {
    return new ScaleXReverse(params)
  }

  /**
   * Reversed scale for y-axis.
   * High values appear at the bottom, low values at the top.
   */
  static ScaleYReverse scale_y_reverse(Map params = [:]) {
    return new ScaleYReverse(params)
  }

  // --- Date/time scales ---

  /**
   * Date scale for x-axis.
   * Handles Date, LocalDate, LocalDateTime, and other temporal types.
   *
   * @param params Optional map with:
   *   - name: axis label
   *   - limits: list of [min, max] date values
   *   - date_format/dateFormat/date_labels: date format pattern (default: 'yyyy-MM-dd')
   *   - date_breaks/dateBreaks: break interval (e.g., '1 month', '2 weeks')
   *   - position: 'bottom' (default) or 'top'
   */
  static ScaleXDate scale_x_date(Map params = [:]) {
    return new ScaleXDate(params)
  }

  /**
   * Date scale for y-axis.
   * Handles Date, LocalDate, LocalDateTime, and other temporal types.
   *
   * @param params Optional map with:
   *   - name: axis label
   *   - limits: list of [min, max] date values
   *   - date_format/dateFormat/date_labels: date format pattern (default: 'yyyy-MM-dd')
   *   - date_breaks/dateBreaks: break interval (e.g., '1 month', '2 weeks')
   */
  static ScaleYDate scale_y_date(Map params = [:]) {
    return new ScaleYDate(params)
  }

  /**
   * DateTime scale for x-axis.
   * Similar to scale_x_date but with hour/minute granularity for time display.
   *
   * @param params Optional map with:
   *   - name: axis label
   *   - limits: list of [min, max] datetime values
   *   - date_format/dateFormat/date_labels: datetime format pattern (default: 'yyyy-MM-dd HH:mm')
   *   - date_breaks/dateBreaks: break interval (e.g., '1 hour', '30 minutes')
   *   - position: 'bottom' (default) or 'top'
   */
  static ScaleXDatetime scale_x_datetime(Map params = [:]) {
    return new ScaleXDatetime(params)
  }

  /**
   * DateTime scale for y-axis.
   * Similar to scale_y_date but with hour/minute granularity for time display.
   *
   * @param params Optional map with:
   *   - name: axis label
   *   - limits: list of [min, max] datetime values
   *   - date_format/dateFormat/date_labels: datetime format pattern (default: 'yyyy-MM-dd HH:mm')
   *   - date_breaks/dateBreaks: break interval (e.g., '1 hour', '30 minutes')
   */
  static ScaleYDatetime scale_y_datetime(Map params = [:]) {
    return new ScaleYDatetime(params)
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

  /**
   * ColorBrewer discrete color scale.
   *
   * @param params Optional map with palette, type, n, direction
   */
  static ScaleColorBrewer scale_color_brewer(Map params = [:]) {
    return new ScaleColorBrewer(params)
  }

  /** British spelling alias for scale_color_brewer */
  static ScaleColorBrewer scale_colour_brewer(Map params = [:]) {
    return scale_color_brewer(params)
  }

  /**
   * ColorBrewer continuous color scale.
   *
   * @param params Optional map with palette, type, direction
   */
  static ScaleColorDistiller scale_color_distiller(Map params = [:]) {
    return new ScaleColorDistiller(params)
  }

  /** British spelling alias for scale_color_distiller */
  static ScaleColorDistiller scale_colour_distiller(Map params = [:]) {
    return scale_color_distiller(params)
  }

  /**
   * Greyscale discrete color scale.
   *
   * @param params Optional map with start, end, direction
   */
  static ScaleColorGrey scale_color_grey(Map params = [:]) {
    return new ScaleColorGrey(params)
  }

  /** British spelling alias for scale_color_grey */
  static ScaleColorGrey scale_colour_grey(Map params = [:]) {
    return scale_color_grey(params)
  }

  /**
   * Continuous multi-color gradient scale.
   *
   * @param params Map with colors/values and optional scale settings
   */
  static ScaleColorGradientN scale_color_gradientn(Map params = [:]) {
    return new ScaleColorGradientN(params)
  }

  /** British spelling alias for scale_color_gradientn */
  static ScaleColorGradientN scale_colour_gradientn(Map params = [:]) {
    return scale_color_gradientn(params)
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

  /**
   * ColorBrewer discrete fill scale.
   *
   * @param params Optional map with palette, type, n, direction
   */
  static ScaleColorBrewer scale_fill_brewer(Map params = [:]) {
    return new ScaleColorBrewer(params + [aesthetic: 'fill'])
  }

  /**
   * ColorBrewer continuous fill scale.
   *
   * @param params Optional map with palette, type, direction
   */
  static ScaleColorDistiller scale_fill_distiller(Map params = [:]) {
    return new ScaleColorDistiller(params + [aesthetic: 'fill'])
  }

  /**
   * Greyscale discrete fill scale.
   *
   * @param params Optional map with start, end, direction
   */
  static ScaleColorGrey scale_fill_grey(Map params = [:]) {
    return new ScaleColorGrey(params + [aesthetic: 'fill'])
  }

  /**
   * Continuous multi-color fill gradient scale.
   *
   * @param params Map with colors/values and optional scale settings
   */
  static ScaleColorGradientN scale_fill_gradientn(Map params = [:]) {
    return new ScaleColorGradientN(params + [aesthetic: 'fill'])
  }

  // --- Alpha scales ---

  /**
   * Alpha scale for mapped transparency.
   *
   * @param params optional scale parameters
   */
  static ScaleAlphaContinuous scale_alpha(Map params = [:]) {
    return new ScaleAlphaContinuous(params)
  }

  /**
   * Continuous alpha scale.
   */
  static ScaleAlphaContinuous scale_alpha_continuous(Map params = [:]) {
    return new ScaleAlphaContinuous(params)
  }

  /**
   * Discrete alpha scale.
   */
  static ScaleAlphaDiscrete scale_alpha_discrete(Map params = [:]) {
    return new ScaleAlphaDiscrete(params)
  }

  /**
   * Binned alpha scale for continuous data.
   */
  static ScaleAlphaBinned scale_alpha_binned(Map params = [:]) {
    return new ScaleAlphaBinned(params)
  }

  // --- Size scales ---

  /**
   * Size scale for mapped sizes.
   *
   * @param params optional scale parameters
   */
  static ScaleSizeContinuous scale_size(Map params = [:]) {
    return new ScaleSizeContinuous(params)
  }

  /**
   * Continuous size scale.
   */
  static ScaleSizeContinuous scale_size_continuous(Map params = [:]) {
    return new ScaleSizeContinuous(params)
  }

  /**
   * Discrete size scale.
   */
  static ScaleSizeDiscrete scale_size_discrete(Map params = [:]) {
    return new ScaleSizeDiscrete(params)
  }

  /**
   * Binned size scale for continuous data.
   */
  static ScaleSizeBinned scale_size_binned(Map params = [:]) {
    return new ScaleSizeBinned(params)
  }

  /**
   * Size scale where area is proportional to the data.
   */
  static ScaleSizeArea scale_size_area(Map params = [:]) {
    return new ScaleSizeArea(params)
  }

  /**
   * Radius-based size scale.
   */
  static ScaleRadius scale_radius(Map params = [:]) {
    return new ScaleRadius(params)
  }

  static StatsBin2D stat_bin_2d() {
    return new StatsBin2D()
  }

  static StatsBoxplot stat_boxplot() {
    return new StatsBoxplot()
  }

  static StatsContour stat_contour() {
    return new StatsContour()
  }

  static StatsContourFilled stat_contour_filled() {
    return new StatsContourFilled()
  }

  static StatsCount stat_count() {
    return new StatsCount()
  }

  /**
   * Create a binning stat for histograms and frequency polygons.
   *
   * @param params optional stat parameters (e.g. bins, binwidth)
   */
  static StatsBin stat_bin(Map params = [:]) {
    return new StatsBin(params)
  }

  /**
   * Create a 1D density stat.
   *
   * @param params optional stat parameters (e.g. adjust, kernel)
   */
  static StatsDensity stat_density(Map params = [:]) {
    return new StatsDensity(params)
  }

  /**
   * Create a smoothing stat for fitted trend lines.
   *
   * @param params optional stat parameters (e.g. method, n, se)
   */
  static StatsSmooth stat_smooth(Map params = [:]) {
    return new StatsSmooth(params)
  }

  /**
   * Create a density stat using y values (for violin plots).
   *
   * @param params optional stat parameters (e.g. adjust, kernel)
   */
  static StatsYDensity stat_ydensity(Map params = [:]) {
    return new StatsYDensity(params)
  }

  /**
   * Create an empirical CDF stat.
   *
   * @param params optional stat parameters (e.g. pad)
   */
  static StatsEcdf stat_ecdf(Map params = [:]) {
    return new StatsEcdf(params)
  }

  /**
   * Create a Q-Q stat for normal distribution quantiles.
   *
   * @param params optional stat parameters
   */
  static StatsQq stat_qq(Map params = [:]) {
    return new StatsQq(params)
  }

  /**
   * Create a Q-Q line stat for normal distribution reference lines.
   *
   * @param params optional stat parameters
   */
  static StatsQqLine stat_qq_line(Map params = [:]) {
    return new StatsQqLine(params)
  }

  static StatsSum stat_summary(Map params) {
    return new StatsSum(params)
  }

  static class As {

    /**
     * Convert a column list to factor values (legacy helper).
     * For ggplot mappings, prefer the top-level factor(...) helper.
     *
     * @param column list of values
     * @return list of string values
     */
    static List<String> factor(List column) {
      return ListConverter.toStrings(column)
    }

    /**
     * Convert a column list to a Factor wrapper for ggplot aesthetics.
     *
     * @param column list of values
     * @return Factor wrapper
     */
    static Factor factorWrap(List column) {
      return new Factor(ListConverter.toStrings(column))
    }
  }
}
