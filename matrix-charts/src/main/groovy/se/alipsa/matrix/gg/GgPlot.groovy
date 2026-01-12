package se.alipsa.matrix.gg

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.groovy.svg.utils.SvgMerger
import se.alipsa.matrix.gg.geom.GeomBin2d
import se.alipsa.matrix.gg.geom.GeomHex
import se.alipsa.matrix.gg.geom.GeomBlank
import se.alipsa.matrix.gg.geom.GeomBoxplot
import se.alipsa.matrix.gg.geom.GeomContour
import se.alipsa.matrix.gg.geom.GeomCount
import se.alipsa.matrix.gg.geom.GeomDensity
import se.alipsa.matrix.gg.geom.GeomDensity2d
import se.alipsa.matrix.gg.geom.GeomDensity2dFilled
import se.alipsa.matrix.gg.geom.GeomDotplot
import se.alipsa.matrix.gg.geom.GeomErrorbar
import se.alipsa.matrix.gg.geom.GeomErrorbarh
import se.alipsa.matrix.gg.geom.GeomFreqpoly
import se.alipsa.matrix.gg.geom.GeomHistogram
import se.alipsa.matrix.gg.geom.GeomHline
import se.alipsa.matrix.gg.geom.GeomJitter
import se.alipsa.matrix.gg.geom.GeomLabel
import se.alipsa.matrix.gg.geom.GeomLine
import se.alipsa.matrix.gg.geom.GeomMap
import se.alipsa.matrix.gg.geom.GeomQq
import se.alipsa.matrix.gg.geom.GeomQqLine
import se.alipsa.matrix.gg.geom.GeomQuantile
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
import se.alipsa.matrix.gg.geom.GeomSf
import se.alipsa.matrix.gg.geom.GeomSfLabel
import se.alipsa.matrix.gg.geom.GeomSfText
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
import se.alipsa.matrix.gg.coord.CoordMap
import se.alipsa.matrix.gg.coord.CoordPolar
import se.alipsa.matrix.gg.coord.CoordQuickmap
import se.alipsa.matrix.gg.coord.CoordSf
import se.alipsa.matrix.gg.coord.CoordTrans
import se.alipsa.matrix.gg.facet.FacetGrid
import se.alipsa.matrix.gg.facet.FacetWrap
import se.alipsa.matrix.gg.facet.Labeller
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
import se.alipsa.matrix.gg.scale.ScaleColorSteps
import se.alipsa.matrix.gg.scale.ScaleColorSteps2
import se.alipsa.matrix.gg.scale.ScaleColorStepsN
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
import se.alipsa.matrix.gg.scale.ScaleShape
import se.alipsa.matrix.gg.scale.ScaleShapeManual
import se.alipsa.matrix.gg.scale.ScaleShapeBinned
import se.alipsa.matrix.gg.scale.ScaleLinetype
import se.alipsa.matrix.gg.scale.ScaleLinetypeManual
import se.alipsa.matrix.gg.scale.ScaleColorIdentity
import se.alipsa.matrix.gg.scale.ScaleFillIdentity
import se.alipsa.matrix.gg.scale.ScaleSizeIdentity
import se.alipsa.matrix.gg.scale.ScaleAlphaIdentity
import se.alipsa.matrix.gg.scale.ScaleShapeIdentity
import se.alipsa.matrix.gg.scale.ScaleLinetypeIdentity
import se.alipsa.matrix.gg.scale.ScaleXContinuous
import se.alipsa.matrix.gg.scale.ScaleXDiscrete
import se.alipsa.matrix.gg.scale.SecondaryAxis
import se.alipsa.matrix.gg.scale.ScaleYContinuous
import se.alipsa.matrix.gg.scale.ScaleYDiscrete
import se.alipsa.matrix.gg.scale.ScaleXBinned
import se.alipsa.matrix.gg.scale.ScaleYBinned
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
import se.alipsa.matrix.gg.scale.ScaleXTime
import se.alipsa.matrix.gg.scale.ScaleYTime
import se.alipsa.matrix.gg.stat.StatsBin2D
import se.alipsa.matrix.gg.stat.StatsBin
import se.alipsa.matrix.gg.stat.StatsBinHex
import se.alipsa.matrix.gg.stat.StatsBoxplot
import se.alipsa.matrix.gg.stat.StatsContour
import se.alipsa.matrix.gg.stat.StatsContourFilled
import se.alipsa.matrix.gg.stat.StatsCount
import se.alipsa.matrix.gg.stat.StatsDensity
import se.alipsa.matrix.gg.stat.StatsEllipse
import se.alipsa.matrix.gg.stat.StatsFunction
import se.alipsa.matrix.gg.stat.StatsSf
import se.alipsa.matrix.gg.stat.StatsSfCoordinates
import se.alipsa.matrix.gg.stat.StatsSmooth
import se.alipsa.matrix.gg.stat.StatsSum
import se.alipsa.matrix.gg.stat.StatsSummaryBin
import se.alipsa.matrix.gg.stat.StatsSummaryHex
import se.alipsa.matrix.gg.stat.StatsUnique
import se.alipsa.matrix.gg.stat.StatsYDensity
import se.alipsa.matrix.gg.stat.StatsEcdf
import se.alipsa.matrix.gg.stat.StatsQq
import se.alipsa.matrix.gg.stat.StatsQqLine
import se.alipsa.matrix.gg.stat.StatsQuantile
import se.alipsa.matrix.gg.layer.Layer
import se.alipsa.matrix.gg.theme.Theme
import se.alipsa.matrix.chartexport.ChartToPng
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

  /**
   * Save one or more SVG objects to a file.
   * If multiple SVGs are provided, they are stacked vertically in a container SVG.
   * Supports .svg and .png file extensions.
   *
   * @param filePath Path to save the file (must end with .svg or .png)
   * @param svgs One or more Svg objects to save
   * @throws IllegalArgumentException if file extension is not .svg or .png
   */
  static void ggsave(String filePath, Svg... svgs) {
    if (svgs == null || svgs.length == 0) {
      throw new IllegalArgumentException("At least one SVG object must be provided")
    }

    File file = new File(filePath)

    // Extract and validate file extension
    int dotIndex = filePath.lastIndexOf('.')
    if (dotIndex == -1 || dotIndex == filePath.length() - 1) {
      throw new IllegalArgumentException("File path must have a valid extension (.svg or .png)")
    }
    String extension = filePath.substring(dotIndex + 1).toLowerCase()

    Svg finalSvg
    if (svgs.length == 1) {
      // Single SVG - use as-is
      finalSvg = svgs[0]
    } else {
      // Multiple SVGs - merge vertically using SvgMerger utility
      finalSvg = SvgMerger.mergeVertically(svgs)
    }

    // Save based on file extension
    if (extension == 'svg') {
      write(finalSvg, file)
    } else if (extension == 'png') {
      ChartToPng.export(finalSvg, file)
    } else {
      throw new IllegalArgumentException("File extension must be .svg or .png, got: .${extension}")
    }
  }

  /**
   * Save one or more GgChart objects to a file.
   * Charts are rendered to SVG and then saved.
   * If multiple charts are provided, they are stacked vertically in a container SVG.
   * Supports .svg and .png file extensions.
   *
   * @param filePath Path to save the file (must end with .svg or .png)
   * @param charts One or more GgChart objects to save
   * @throws IllegalArgumentException if file extension is not .svg or .png
   */
  static void ggsave(String filePath, GgChart... charts) {
    if (charts == null || charts.length == 0) {
      throw new IllegalArgumentException("At least one GgChart object must be provided")
    }

    // Render each chart to SVG
    Svg[] svgs = charts.collect { it.render() } as Svg[]

    // Delegate to SVG version
    ggsave(filePath, svgs)
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

  /**
   * Add logarithmic tick marks to plot.
   * Automatically detects log-scaled axes and generates tick marks at appropriate positions.
   *
   * Tick marks are generated for (base 10 example):
   * - Major positions: powers of the base (1, 10, 100, ...)
   * - Intermediate ticks: 2 and 5 multiples (2, 5, 20, 50, ...) for base 10
   *   For other bases >= 4, only mult==2 is intermediate; mult==5 is minor (if it exists)
   * - Minor ticks: other integers (3, 4, 6, 7, 8, 9, ...)
   *
   * For bases 2 and 3, only major ticks are generated.
   *
   * Example: annotation_logticks(sides: 'bl', base: 10)
   *
   * @param params optional parameters:
   *   - base: logarithmic base (default: 10)
   *   - sides: which sides to draw ticks: 't' (top), 'r' (right), 'b' (bottom), 'l' (left) (default: 'bl')
   *   - outside: whether ticks extend outside plot area (default: false)
   *   - scaled: whether data is already log-transformed (default: true)
   *   - short: length of minor tick marks in pixels (default: 1.5)
   *   - mid: length of intermediate tick marks in pixels (default: 2.25)
   *   - long: length of major tick marks in pixels (default: 4.5)
   *   - colour/color: tick color (default: 'black')
   *   - linewidth: tick line width (default: 0.5)
   *   - linetype: tick line type (default: 'solid')
   *   - alpha: transparency 0-1 (default: 1.0)
   * @return a Layer with log tick marks
   */
  static Layer annotation_logticks(Map params = [:]) {
    return new AnnotationLogticks(params).toLayer()
  }

  /**
   * Add custom graphical object (grob) to plot.
   * The grob can be a Closure, gsvg Element, or SVG String.
   *
   * The custom annotation does not affect scale limits and remains static across facets.
   * Position defaults (-Inf/Inf) fill the entire plot panel.
   *
   * Position parameters are specified in DATA coordinates (e.g., xmin: 1, xmax: 3).
   * The bounds map passed to closures contains PIXEL coordinates (already transformed).
   *
   * Example - bounds already in pixels, ready to use:
   * annotation_custom(
   *   grob: { G g, Map bounds ->
   *     // bounds contains PIXEL coordinates, use directly
   *     g.addRect()
   *       .x(bounds.xmin as int)
   *       .y(bounds.ymin as int)
   *       .width((bounds.xmax - bounds.xmin) as int)
   *       .height((bounds.ymax - bounds.ymin) as int)
   *       .fill('red').addAttribute('opacity', 0.2)
   *   },
   *   xmin: 1, xmax: 3, ymin: 5, ymax: 10  // DATA coordinates
   * )
   *
   * @param params required: grob (Closure, gsvg Element, or String)
   *               optional: xmin, xmax, ymin, ymax in DATA coordinates (default: -Inf to Inf)
   * @return a Layer with custom graphical object
   */
  static Layer annotation_custom(Map params) {
    return new AnnotationCustom(params).toLayer()
  }

  /**
   * Add a map annotation layer using map polygon data.
   *
   * @param params required: map (Matrix). Optional: mapping, data, styling
   * @return a Layer with map annotation
   */
  static Layer annotation_map(Map params) {
    GeomMap geom = new GeomMap(params)
    Aes mapping = params?.mapping instanceof Aes ? params.mapping as Aes : null
    Matrix data = params?.data instanceof Matrix ? params.data as Matrix : null
    return new Layer(
        geom: geom,
        data: data,
        aes: mapping,
        stat: geom.defaultStat,
        position: geom.defaultPosition,
        params: geom.params ?: [:],
        statParams: [:],
        positionParams: [:],
        inheritAes: false
    )
  }

  /**
   * Convenience wrapper for map borders using matrix-datasets map data.
   *
   * @param datasetName map dataset name (e.g. 'world', 'state')
   * @param region optional region filter
   * @param exact whether to match dataset name exactly
   * @param params additional styling parameters for geom_map/annotation_map
   * @return a Layer with map borders
   */
  @CompileDynamic
  static Layer borders(String datasetName, String region = null, boolean exact = false, Map params = [:]) {
    Class datasetClass
    try {
      datasetClass = Class.forName('se.alipsa.matrix.datasets.Dataset')
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException('borders() requires matrix-datasets on the classpath')
    }
    def mapData = datasetClass.getMethod('mapData', String, String, boolean).invoke(null, datasetName, region, exact)
    if (!(mapData instanceof Matrix)) {
      throw new IllegalArgumentException('borders() could not load map data')
    }
    Map allParams = new LinkedHashMap<>(params ?: [:])
    allParams.map = mapData
    return annotation_map(allParams)
  }

  /**
   * Convenience wrapper for map borders when map data is already available.
   *
   * @param mapData map polygon data
   * @param params additional styling parameters for geom_map/annotation_map
   * @return a Layer with map borders
   */
  static Layer borders(Matrix mapData, Map params = [:]) {
    Map allParams = new LinkedHashMap<>(params ?: [:])
    allParams.map = mapData
    return annotation_map(allParams)
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

  /**
   * Suppress guide display for an aesthetic.
   * Convenience function equivalent to using the string 'none'.
   *
   * Example: guides(color: guide_none())
   *
   * @return a Guide specification with type 'none'
   */
  static Guide guide_none() {
    return new Guide('none')
  }

  /**
   * Customize axis guide appearance and behavior.
   *
   * Example: scale_x_continuous(guide: guide_axis(angle: 45, 'check.overlap': true))
   *
   * @param params optional axis customization parameters:
   *   - angle: Rotation angle for axis labels (default: 0)
   *   - check.overlap (or checkOverlap): Hide overlapping labels (default: false)
   *   - min.spacing (or minSpacing): Minimum spacing between labels in pixels for overlap checking.
   *       Default: 4x font size for X-axis, 2x font size for Y-axis
   *   - n.dodge (or nDodge): Number of rows for dodging labels (default: 1).
   *       Note: This parameter is currently recognized but not yet implemented and will be ignored.
   * @return an axis guide specification
   */
  static Guide guide_axis(Map params = [:]) {
    return new Guide('axis', params)
  }

  /**
   * Create a binned legend guide for discrete breaks.
   * Used with binned scales to show breaks as bins rather than points.
   *
   * Example: guides(color: guide_bins())
   *
   * @param params optional binned legend options (currently experimental):
   *   - show.limits: Intended to show labels for limits (currently not implemented/ignored)
   * @return a binned legend guide specification
   */
  static Guide guide_bins(Map params = [:]) {
    return new Guide('bins', params)
  }

  /**
   * Create a stepped color guide for binned continuous scales.
   * Displays discrete color blocks instead of a smooth gradient.
   *
   * Example: guides(color: guide_coloursteps())
   * Example: scale_color_steps(bins: 5) + guides(color: guide_coloursteps(evenSteps: false))
   *
   * @param params optional parameters:
   *   - evenSteps or even.steps (Boolean, default: true): Equal-sized bins vs proportional to data range
   *   - showLimits or show.limits (Boolean, default: null): Show scale limit labels
   *   - reverse (Boolean, default: false): Reverse colorbar direction
   *   - barwidth: Custom bar width (overrides theme default)
   *   - barheight: Custom bar height (overrides theme default)
   * @return a stepped color guide specification
   */
  static Guide guide_coloursteps(Map params = [:]) {
    return new Guide('coloursteps', params)
  }

  /** American spelling alias for guide_coloursteps */
  static Guide guide_colorsteps(Map params = [:]) {
    return guide_coloursteps(params)
  }

  /**
   * Create a logarithmic tick mark axis guide with varying tick densities.
   * Displays major ticks at powers of 10, mid ticks at 2× and 5× multiples,
   * and short ticks at intermediate values.
   *
   * Example: scale_x_log10(guide: guide_axis_logticks())
   * Example: scale_y_log10(guide: guide_axis_logticks(long: 3.0, mid: 2.0, short: 1.0))
   * Example: scale_x_log10(guide: guide_axis_logticks(minAbsValue: 0.01)) // Show ticks down to 0.01
   *
   * @param params optional parameters:
   *   - long (Number, default: 2.25): Multiplier for long ticks (powers of 10)
   *   - mid (Number, default: 1.5): Multiplier for mid ticks (2×, 5× multiples)
   *   - short (Number, default: 0.75): Multiplier for short ticks (other values)
   *   - prescaleBase or prescale.base (Number, default: null): If data is already log-transformed,
   *     specify the base used (e.g., 10). When set, domain values are treated as exponents.
   *   - minAbsValue or min.abs.value (Number, default: 0.1): Minimum absolute value threshold.
   *     Ticks with |value| < minAbsValue are omitted (prevents visual clutter near zero).
   *     Note: The old parameter names negativeSmall/negative.small are still supported for backward compatibility.
   *   - expanded (Boolean, default: true): If true, use the expanded axis range (with padding).
   *     If false, use the exact scale limits.
   * @return a logarithmic tick axis guide specification
   */
  static Guide guide_axis_logticks(Map params = [:]) {
    return new Guide('axis_logticks', params)
  }

  /**
   * Create a circular axis guide for polar coordinates.
   * Displays axis labels around the perimeter of polar plots.
   *
   * Example: coord_polar(theta: 'x') + scale_x_continuous(guide: guide_axis_theta())
   *
   * @param params optional parameters:
   *   - angle (Number, default: 0): Label rotation angle
   *   - minorTicks or minor.ticks (Boolean, default: false): Show minor tick marks
   *   - cap (String, default: 'none'): Cap style for axis line
   * @return a theta axis guide specification for polar coordinates
   */
  static Guide guide_axis_theta(Map params = [:]) {
    return new Guide('axis_theta', params)
  }

  /**
   * Create a custom guide with user-defined rendering.
   * Accepts a Groovy closure that receives rendering context and creates SVG elements.
   *
   * Example with closure:
   *   guide_custom({ context ->
   *     context.svg.addRect(30, 20).x(context.x).y(context.y).fill('red')
   *     context.svg.addText('Custom').x(context.x + 5).y(context.y + 15)
   *   }, width: 40, height: 30)
   *
   * The closure receives a context map with:
   *   - svg: Parent SVG/G element to append to
   *   - x, y: Position to render at (pixels)
   *   - width, height: Allocated dimensions
   *   - theme: Theme object
   *   - scales: Map of scales by aesthetic
   *
   * @param renderClosure Groovy closure for custom rendering
   * @param params optional parameters:
   *   - width (Number, default: 50): Allocated width
   *   - height (Number, default: 50): Allocated height
   *   - title (String, default: null): Optional title
   *   - position (String, default: 'right'): Legend position
   * @return a custom guide specification
   */
  static Guide guide_custom(Closure renderClosure, Map params = [:]) {
    if (renderClosure == null) {
      throw new IllegalArgumentException("guide_custom requires a renderClosure parameter")
    }
    return new Guide('custom', [renderClosure: renderClosure] + params)
  }

  /**
   * Create a stacked axis guide that displays multiple axis guides on the same side.
   *
   * Example: scale_x_continuous(guide: guide_axis_stack(guide_axis(), guide_axis(angle: 45)))
   *
   * @param first First guide (closest to panel), can be Guide object or string like 'axis'
   * @param params optional parameters:
   *   - additional (List): Additional guides to stack outward
   *   - spacing (Number, default: 5): Pixel spacing between guides
   * @return a stacked axis guide specification
   */
  static Guide guide_axis_stack(Object first, Map params = [:]) {
    return new Guide('axis_stack', [first: first] + params)
  }

  /**
   * Create a stacked axis guide with multiple guides (overloaded version).
   *
   * Example: guide_axis_stack(guide_axis(), guide_axis(angle: 45), guide_axis(angle: 90))
   *
   * @param first First guide
   * @param additional Additional guides as varargs
   * @return a stacked axis guide specification
   */
  static Guide guide_axis_stack(Object first, Object... additional) {
    return new Guide('axis_stack', [first: first, additional: additional as List])
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
   * @param theta : variable to map angle to (x or y)
   * @param start : Offset of starting point from 12 o'clock in radians.
   * Offset is applied clockwise or anticlockwise depending on value of direction.
   * @param direction : 1, clockwise; -1, anticlockwise
   * @param clip : Should drawing be clipped to the extent of the plot panel? A setting of "on"
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

  /**
   * Create a transformed coordinate system.
   *
   * Applies transformations to x and/or y coordinates.
   * Available transformations: "identity", "log", "log10", "sqrt", "reverse", "reciprocal", "power", "asn"
   *
   * @param params transformation parameters:
   *   - x: x-axis transformation (String, Map with forward/inverse closures, or Trans)
   *   - y: y-axis transformation (String, Map with forward/inverse closures, or Trans)
   *   - xlim: x-axis limits [min, max]
   *   - ylim: y-axis limits [min, max]
   * @return CoordTrans instance
   *
   * Examples:
   * - coord_trans(x: "log10") - log10 transform on x-axis
   * - coord_trans(y: "sqrt") - square root transform on y-axis
   * - coord_trans(x: "log10", y: "sqrt") - both axes transformed
   * - coord_trans(x: [forward: {x -> x * x}, inverse: {x -> x.sqrt()}]) - custom transformation
   */
  static CoordTrans coord_trans(Map params = [:]) {
    return new CoordTrans(params)
  }

  /**
   * Coordinate system for simple feature data.
   */
  static CoordSf coord_sf() {
    return new CoordSf()
  }

  /**
   * Coordinate system for simple feature data with optional parameters.
   */
  static CoordSf coord_sf(Map params) {
    return new CoordSf(params)
  }

  /**
   * Map projection coordinate system.
   *
   * @param params projection parameters (projection, xlim, ylim)
   */
  static CoordMap coord_map(Map params = [:]) {
    return new CoordMap(params)
  }

  /**
   * Quick map coordinate system with approximate aspect ratio.
   */
  static CoordQuickmap coord_quickmap() {
    return new CoordQuickmap()
  }

  /**
   * Quick map coordinate system with optional parameters.
   */
  static CoordQuickmap coord_quickmap(Map params) {
    return new CoordQuickmap(params)
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

  static GeomHex geom_hex() {
    return new GeomHex()
  }

  /**
   * Create a hexagonal binning geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomHex instance
   */
  static GeomHex geom_hex(Aes mapping) {
    return geom_hex([mapping: mapping])
  }

  static GeomHex geom_hex(Map params) {
    return new GeomHex(params)
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

  static GeomDotplot geom_dotplot() {
    return new GeomDotplot()
  }

  /**
   * Create a dot plot geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomDotplot instance
   */
  static GeomDotplot geom_dotplot(Aes mapping) {
    return geom_dotplot([mapping: mapping])
  }

  static GeomDotplot geom_dotplot(Map params) {
    return new GeomDotplot(params)
  }

  static GeomDensity2d geom_density_2d() {
    return new GeomDensity2d()
  }

  /**
   * Create a 2D density contour geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomDensity2d instance
   */
  static GeomDensity2d geom_density_2d(Aes mapping) {
    return geom_density_2d([mapping: mapping])
  }

  static GeomDensity2d geom_density_2d(Map params) {
    return new GeomDensity2d(params)
  }

  static GeomDensity2dFilled geom_density_2d_filled() {
    return new GeomDensity2dFilled()
  }

  /**
   * Create a filled 2D density contour geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomDensity2dFilled instance
   */
  static GeomDensity2dFilled geom_density_2d_filled(Aes mapping) {
    return geom_density_2d_filled([mapping: mapping])
  }

  static GeomDensity2dFilled geom_density_2d_filled(Map params) {
    return new GeomDensity2dFilled(params)
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
   * Add quantile regression lines to the plot.
   *
   * Quantile regression fits lines that estimate conditional quantiles
   * rather than the conditional mean (as in ordinary least squares).
   *
   * Usage:
   * <pre>
   * // Default: fit 25th, 50th, and 75th percentiles
   * ggplot(mpg, aes('displ', 'hwy')) + geom_point() + geom_quantile()
   *
   * // Fit only the median (50th percentile)
   * ggplot(mpg, aes('displ', 'hwy')) + geom_point() + geom_quantile(quantiles: [0.5])
   *
   * // Fit 10th, 50th, and 90th percentiles with custom styling
   * ggplot(mpg, aes('displ', 'hwy')) + geom_point() +
   *   geom_quantile(quantiles: [0.1, 0.5, 0.9], color: 'blue', linetype: 'dashed')
   * </pre>
   *
   * @return a new GeomQuantile instance with default parameters
   */
  static GeomQuantile geom_quantile() {
    return new GeomQuantile()
  }

  /**
   * Add quantile regression lines with custom parameters.
   *
   * @param params Map of parameters:
   *   - quantiles: List of quantiles to fit (default: [0.25, 0.5, 0.75])
   *   - color/colour: Line color
   *   - size: Line width
   *   - linetype: 'solid', 'dashed', 'dotted', etc.
   *   - alpha: Transparency (0-1)
   *   - n: Number of fitted points (default: 80)
   * @return a new GeomQuantile instance
   */
  static GeomQuantile geom_quantile(Map params) {
    return new GeomQuantile(params)
  }

  /**
   * Quantile regression statistical transformation.
   *
   * This is typically used with geom_line() or other geoms that need
   * quantile regression data.
   *
   * @return a new StatsQuantile instance
   */
  static StatsQuantile stat_quantile() {
    return new StatsQuantile()
  }

  /**
   * Quantile regression statistical transformation with custom parameters.
   *
   * @param params Map of parameters:
   *   - quantiles: List of quantiles to fit (default: [0.25, 0.5, 0.75])
   *   - n: Number of fitted points (default: 80)
   * @return a new StatsQuantile instance
   */
  static StatsQuantile stat_quantile(Map params) {
    return new StatsQuantile(params)
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
        se       : false,
        method   : 'lm',
        colour   : 'steelblue',
        alpha    : 0.5,
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

  static GeomSf geom_sf() {
    return new GeomSf()
  }

  /**
   * Create an sf geom with a layer-specific aesthetic mapping.
   * Uses the geometry aesthetic to expand WKT into x/y coordinates.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomSf instance
   */
  static GeomSf geom_sf(Aes mapping) {
    return geom_sf([mapping: mapping])
  }

  static GeomSf geom_sf(Map params) {
    return new GeomSf(params)
  }

  static GeomSfText geom_sf_text() {
    return new GeomSfText()
  }

  /**
   * Create an sf text geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomSfText instance
   */
  static GeomSfText geom_sf_text(Aes mapping) {
    return geom_sf_text([mapping: mapping])
  }

  static GeomSfText geom_sf_text(Map params) {
    return new GeomSfText(params)
  }

  static GeomSfLabel geom_sf_label() {
    return new GeomSfLabel()
  }

  /**
   * Create an sf label geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer
   * @return a new GeomSfLabel instance
   */
  static GeomSfLabel geom_sf_label(Aes mapping) {
    return geom_sf_label([mapping: mapping])
  }

  static GeomSfLabel geom_sf_label(Map params) {
    return new GeomSfLabel(params)
  }

  static GeomMap geom_map() {
    return new GeomMap()
  }

  /**
   * Create a map geom with a layer-specific aesthetic mapping.
   *
   * @param mapping aesthetic mapping for this layer (map_id, fill, color, etc.)
   * @return a new GeomMap instance
   */
  static GeomMap geom_map(Aes mapping) {
    return geom_map([mapping: mapping])
  }

  static GeomMap geom_map(Map params) {
    return new GeomMap(params)
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

  // ============ Facet Labellers ============

  /**
   * Create a labeller that displays variable values only (default).
   *
   * Example:
   *   facet_wrap('category', labeller: label_value())
   *   // Labels: "A", "B", "C"
   *
   * @param multiLine Whether to display multiple variables on separate lines
   * @return Labeller that shows only values
   */
  static Labeller label_value(boolean multiLine = true) {
    return new Labeller({ Map<String, Object> values ->
      List<String> parts = values.values().collect { it?.toString() ?: '' }
      return multiLine ? parts.join('\n') : parts.join(', ')
    }, multiLine)
  }

  /**
   * Create a labeller that displays both variable names and values.
   *
   * Example:
   *   facet_wrap('category', labeller: label_both())
   *   // Labels: "category: A", "category: B", "category: C"
   *
   * @param sep Separator between variable name and value (default: ": ")
   * @param multiLine Whether to display multiple variables on separate lines
   * @return Labeller that shows "var: value" format
   */
  static Labeller label_both(String sep = ': ', boolean multiLine = true) {
    return new Labeller({ Map<String, Object> values ->
      List<String> parts = values.collect { k, v -> "${k}${sep}${v?.toString() ?: ''}".toString() }
      return multiLine ? parts.join('\n') : parts.join(', ')
    }, multiLine)
  }

  /**
   * Create a context-aware labeller that adapts based on faceting complexity.
   * Uses label_value() for single-variable faceting and label_both() for multiple variables.
   *
   * Example:
   *   facet_wrap('category', labeller: label_context())    // Shows: "A", "B", "C"
   *   facet_wrap(['cat', 'type'], labeller: label_context()) // Shows: "cat: A\ntype: X"
   *
   * @param multiLine Whether to display multiple variables on separate lines
   * @return Context-aware labeller
   */
  static Labeller label_context(boolean multiLine = true) {
    return new Labeller({ Map<String, Object> values ->
      if (values.size() == 1) {
        // Single variable: use value only
        return values.values().first()?.toString() ?: ''
      } else {
        // Multiple variables: use both
        List<String> parts = values.collect { k, v -> "${k}: ${v?.toString() ?: ''}".toString() }
        return multiLine ? parts.join('\n') : parts.join(', ')
      }
    }, multiLine)
  }

  /**
   * Create a labeller that wraps long labels to a specified width.
   *
   * Example:
   *   facet_wrap('long_category_name', labeller: label_wrap_gen(15))
   *   // Wraps labels longer than 15 characters
   *
   * @param width Maximum width in characters before wrapping (default: 25)
   * @param multiLine Whether to display multiple variables on separate lines
   * @return Labeller that wraps long text
   */
  static Labeller label_wrap_gen(int width = 25, boolean multiLine = true) {
    return new Labeller({ Map<String, Object> values ->
      List<String> parts = values.values().collect { Object val ->
        String text = val?.toString() ?: ''
        if (text.length() <= width) {
          return text
        }
        // Word wrapping: split into lines of max width
        List<String> lines = []
        String[] words = text.split(/\s+/)
        String currentLine = ''
        for (String word : words) {
          // Handle words longer than width by breaking them
          if (word.length() > width) {
            // Flush current line if it has content
            if (!currentLine.isEmpty()) {
              lines.add(currentLine)
              currentLine = ''
            }
            // Break long word into chunks
            while (word.length() > width) {
              lines.add(word.substring(0, width))
              word = word.substring(width)
            }
            // Add remaining part as current line
            currentLine = word
          } else if (currentLine.isEmpty()) {
            currentLine = word
          } else if ((currentLine.length() + 1 + word.length()) <= width) {
            currentLine += ' ' + word
          } else {
            lines.add(currentLine)
            currentLine = word
          }
        }
        if (!currentLine.isEmpty()) {
          lines.add(currentLine)
        }
        return lines.join('\n')
      }
      return multiLine ? parts.join('\n\n') : parts.join(', ')
    }, multiLine)
  }

  /**
   * Create a labeller for parsing labels as mathematical expressions.
   * Note: In the current implementation, this returns labels as-is without parsing.
   * Future versions may support plotmath-style expression rendering.
   *
   * Example:
   *   facet_wrap('alpha', labeller: label_parsed())
   *   // Future: could render Greek letter alpha
   *
   * @param multiLine Whether to display multiple variables on separate lines
   * @return Labeller for parsed expressions
   */
  static Labeller label_parsed(boolean multiLine = true) {
    // For now, just return the label as-is
    // In the future, this could support expression parsing/rendering
    return new Labeller({ Map<String, Object> values ->
      List<String> parts = values.values().collect { it?.toString() ?: '' }
      return multiLine ? parts.join('\n') : parts.join(', ')
    }, multiLine)
  }

  /**
   * Construct a composite labeller specification with different labellers for different variables.
   *
   * Examples:
   *   // Use label_both for 'cyl', label_value for others
   *   facet_grid(rows: 'cyl', cols: 'gear', labeller: labeller(cyl: label_both()))
   *
   *   // Different labellers for different variables
   *   facet_grid(rows: 'cyl', cols: 'gear',
   *              labeller: labeller(cyl: label_both(), gear: label_value()))
   *
   * @param params Named arguments mapping variable names to Labeller objects; may contain
   *               '.default' for unspecified variables and '.multi_line' to control multi-line labels
   * @return Composite labeller
   */
  static Labeller labeller(Map params) {
    Map<String, Labeller> variableLabellers = [:]
    Labeller defaultLabeller = null
    boolean multiLine = true

    params.each { key, value ->
      if (key == '.default' || key == 'default') {
        if (value instanceof Labeller) {
          defaultLabeller = value as Labeller
        }
      } else if (key == '.multi_line' || key == 'multiLine' || key == 'multi_line') {
        multiLine = value as boolean
      } else if (value instanceof Labeller) {
        variableLabellers[key as String] = value as Labeller
      }
    }

    // Use label_value as default if not specified
    if (defaultLabeller == null) {
      defaultLabeller = label_value(multiLine)
    }

    return new Labeller(variableLabellers, defaultLabeller, multiLine)
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

  /**
   * Binned position scale for x-axis.
   * Divides continuous data into equal-width bins and maps values to bin centers.
   * <p>
   * This scale is useful for creating histogram-like visualizations with discrete geoms
   * or for discretizing continuous position data. Values within the same bin are mapped
   * to the same position (the bin center), creating distinct groupings.
   * <p>
   * Example:
   * <pre>
   * def chart = ggplot(data, aes(x: 'value', y: 'count')) +
   *   geom_point() +
   *   scale_x_binned(bins: 10)
   * </pre>
   *
   * @param params scale parameters:
   *   - bins: Number of bins (default: 10)
   *   - right: Close bins on right (true, default) or left (false)
   *   - showLimits: Show scale limits as ticks (default: false)
   *   - limits, breaks, labels, expand, position, guide, name (standard scale params)
   * @return ScaleXBinned instance
   */
  static ScaleXBinned scale_x_binned(Map params = [:]) {
    return new ScaleXBinned(params)
  }

  /**
   * Binned position scale for y-axis.
   * Divides continuous data into equal-width bins and maps values to bin centers.
   * <p>
   * See {@link #scale_x_binned(Map)} for details.
   *
   * @param params scale parameters (same as scale_x_binned)
   * @return ScaleYBinned instance
   */
  static ScaleYBinned scale_y_binned(Map params = [:]) {
    return new ScaleYBinned(params)
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

  /**
   * Time-of-day scale for x-axis.
   * Handles LocalTime values representing times within a 24-hour cycle (00:00:00 to 23:59:59).
   *
   * @param params Optional map with:
   *   - name: axis label
   *   - limits: list of [min, max] time values (LocalTime or ISO time strings)
   *   - time_format/timeFormat: time format pattern (default: 'HH:mm')
   *   - time_breaks/timeBreaks: break interval (e.g., '1 hour', '30 minutes', '15 seconds')
   *   - position: 'bottom' (default) or 'top'
   *
   * @example
   * // Hourly time series
   * ggplot(data, aes(x: 'time', y: 'value')) +
   *   geom_line() +
   *   scale_x_time(time_breaks: '2 hours')
   *
   * // 12-hour format with AM/PM
   * ggplot(data, aes(x: 'time', y: 'count')) +
   *   geom_point() +
   *   scale_x_time(time_format: 'h:mm a')
   */
  static ScaleXTime scale_x_time(Map params = [:]) {
    return new ScaleXTime(params)
  }

  /**
   * Time-of-day scale for y-axis.
   * Handles LocalTime values representing times within a 24-hour cycle (00:00:00 to 23:59:59).
   *
   * @param params Optional map with:
   *   - name: axis label
   *   - limits: list of [min, max] time values (LocalTime or ISO time strings)
   *   - time_format/timeFormat: time format pattern (default: 'HH:mm')
   *   - time_breaks/timeBreaks: break interval (e.g., '1 hour', '30 minutes', '15 seconds')
   *   - position: 'left' (default) or 'right'
   *
   * @example
   * // Time-based categorical chart
   * ggplot(data, aes(x: 'category', y: 'time')) +
   *   geom_point() +
   *   scale_y_time(time_breaks: '30 minutes', time_format: 'HH:mm:ss')
   */
  static ScaleYTime scale_y_time(Map params = [:]) {
    return new ScaleYTime(params)
  }

  // --- Secondary axes ---

  /**
   * Specify a secondary axis with a transformation of the primary axis (idiomatic Groovy style).
   * <p>
   * The secondary axis must have a one-to-one transformation of the primary axis.
   * This means every value on the primary axis corresponds to exactly one value on the secondary axis.
   * <p>
   * Usage with scales:
   * <pre>
   *   // Celsius to Fahrenheit conversion (idiomatic Groovy style)
   *   scale_y_continuous(sec.axis: sec_axis("Fahrenheit") { it * 1.8 + 32 })
   *
   *   // With custom breaks and labels
   *   scale_x_continuous(sec.axis: sec_axis("Kilometers", breaks: [0, 5, 10, 15]) {
   *     it / 1000
   *   })
   *
   *   // Alternative syntax (closure first)
   *   scale_y_continuous(sec.axis: sec_axis({ it * 1.8 + 32 }, name: "Fahrenheit"))
   * </pre>
   *
   * @param name Label for the secondary axis
   * @param params Additional parameters (breaks, labels, guide) - defaults to empty map
   * @param transform Closure that transforms primary axis values to secondary axis values
   * @return SecondaryAxis configuration object
   */
  static SecondaryAxis sec_axis(String name, Map params = [:], Closure<BigDecimal> transform) {
    Map allParams = [name: name] + params
    return new SecondaryAxis(transform, allParams)
  }

  /**
   * Handle Groovy named parameter syntax where Map comes first.
   * This allows: sec_axis("Kilometers", breaks: [0, 5, 10]) { it / 1000 }
   */
  static SecondaryAxis sec_axis(Map params, String name, Closure<BigDecimal> transform) {
    Map allParams = [name: name] + params
    return new SecondaryAxis(transform, allParams)
  }

  /**
   * Alternate signature for sec_axis (closure first, less idiomatic but supported).
   *
   * @param transform Closure that transforms primary axis values to secondary axis values
   * @param params Additional parameters including name
   * @return SecondaryAxis configuration object
   */
  static SecondaryAxis sec_axis(Closure<BigDecimal> transform, Map params = [:]) {
    return new SecondaryAxis(transform, params)
  }

  /**
   * Alternate signature for sec_axis that handles Groovy's named parameters with closure.
   * This signature allows calling: sec_axis({ closure }, name: "label", ...)
   * where Groovy treats the named params as a Map that comes before the closure.
   */
  static SecondaryAxis sec_axis(Map params, Closure<BigDecimal> transform) {
    return new SecondaryAxis(transform, params)
  }

  /**
   * Duplicate the primary axis on the opposite side.
   * <p>
   * This is a convenience function equivalent to sec_axis() with an identity transformation.
   * It displays the same scale and labels on both sides of the plot.
   * <p>
   * Usage:
   * <pre>
   *   // Show y-axis on both left and right (no custom name)
   *   scale_y_continuous(sec.axis: dup_axis())
   *
   *   // With custom name for the duplicated axis (idiomatic)
   *   scale_x_continuous(sec.axis: dup_axis("Time (repeated)"))
   *
   *   // Alternative syntax
   *   scale_x_continuous(sec.axis: dup_axis(name: "Time (repeated)"))
   * </pre>
   *
   * @param name Optional label for the duplicated axis
   * @return SecondaryAxis with identity transformation
   */
  static SecondaryAxis dup_axis(String name = null) {
    Map params = name ? [name: name] : [:]
    return new SecondaryAxis({ it as BigDecimal }, params)
  }

  /**
   * Duplicate the primary axis with additional parameters.
   *
   * @param params Optional parameters (name, breaks, labels, guide)
   * @return SecondaryAxis with identity transformation
   */
  static SecondaryAxis dup_axis(Map params) {
    return new SecondaryAxis({ it as BigDecimal }, params)
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

  // --- Binned Color Scales ---

  /**
   * Binned sequential color scale - maps continuous values to discrete color steps.
   * Each bin maps to one color (no interpolation). Bin count = color count.
   *
   * Example:
   * <pre>
   * scale_color_steps(bins: 7, low: 'white', high: 'darkblue')  // 7 bins, 7 colors
   * scale_color_steps(colors: ['#ffffcc', '#a1dab4', '#41b6c4', '#225ea8'])  // 4 bins, 4 colors
   * </pre>
   *
   * @param params scale parameters (bins, low, high, colors, naValue, name, limits, breaks, labels, guide)
   */
  static ScaleColorSteps scale_color_steps(Map params = [:]) {
    return new ScaleColorSteps(params)
  }

  /**
   * Binned sequential colour scale (British spelling alias).
   */
  static ScaleColorSteps scale_colour_steps(Map params = [:]) {
    return scale_color_steps(params)
  }

  /**
   * Binned sequential fill scale.
   */
  static ScaleColorSteps scale_fill_steps(Map params = [:]) {
    return new ScaleColorSteps(params + [aesthetic: 'fill'])
  }

  /**
   * Binned diverging color scale - splits at midpoint with two color ranges.
   * Each bin maps to one color (no interpolation). Bin count adjusted to odd for symmetry.
   *
   * Example:
   * <pre>
   * scale_color_steps2(bins: 9, low: 'blue', mid: 'white', high: 'red')  // 9 bins (odd)
   * scale_color_steps2(bins: 6, midpoint: 50)  // 7 bins (6→7 for symmetry)
   * </pre>
   *
   * @param params scale parameters (bins, low, mid, high, midpoint, colors, naValue, name, limits, breaks, labels, guide)
   */
  static ScaleColorSteps2 scale_color_steps2(Map params = [:]) {
    return new ScaleColorSteps2(params)
  }

  /**
   * Binned diverging colour scale (British spelling alias).
   */
  static ScaleColorSteps2 scale_colour_steps2(Map params = [:]) {
    return scale_color_steps2(params)
  }

  /**
   * Binned diverging fill scale.
   */
  static ScaleColorSteps2 scale_fill_steps2(Map params = [:]) {
    return new ScaleColorSteps2(params + [aesthetic: 'fill'])
  }

  /**
   * Binned n-color scale - maps continuous values to bins from a custom palette.
   * Supports color interpolation - bins can differ from color count.
   *
   * Example:
   * <pre>
   * scale_color_stepsn(bins: 5, colors: ['red', 'yellow', 'green', 'blue'])  // 5 bins from 4-color gradient
   * scale_color_stepsn(bins: 10, colors: ['red', 'blue'])  // 10 bins interpolated red→blue
   * scale_color_stepsn(bins: 10, colors: ['#440154', '#31688e', '#35b779', '#fde724'],
   *                    values: [0.0, 0.33, 0.67, 1.0])  // Custom color positions
   * </pre>
   *
   * @param params scale parameters (bins, colors, values, naValue, name, limits, breaks, labels, guide)
   */
  static ScaleColorStepsN scale_color_stepsn(Map params = [:]) {
    return new ScaleColorStepsN(params)
  }

  /**
   * Binned n-colour scale (British spelling alias).
   */
  static ScaleColorStepsN scale_colour_stepsn(Map params = [:]) {
    return scale_color_stepsn(params)
  }

  /**
   * Binned n-color fill scale.
   */
  static ScaleColorStepsN scale_fill_stepsn(Map params = [:]) {
    return new ScaleColorStepsN(params + [aesthetic: 'fill'])
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

  // --- Shape scales ---

  /**
   * Shape scale for mapped shapes.
   *
   * @param params optional scale parameters
   */
  static ScaleShape scale_shape(Map params = [:]) {
    return new ScaleShape(params)
  }

  /**
   * Discrete shape scale (alias for scale_shape).
   */
  static ScaleShape scale_shape_discrete(Map params = [:]) {
    return new ScaleShape(params)
  }

  /**
   * Binned shape scale for continuous data.
   */
  static ScaleShapeBinned scale_shape_binned(Map params = [:]) {
    return new ScaleShapeBinned(params)
  }

  /**
   * Manual shape scale for custom shape mappings.
   */
  static ScaleShapeManual scale_shape_manual(Map params = [:]) {
    return new ScaleShapeManual(params)
  }

  /**
   * Linetype scale for line geometries.
   *
   * Available linetypes: solid, dashed, dotted, dotdash, longdash, twodash
   *
   * @param params optional scale parameters (name, limits, breaks, labels, linetypes)
   */
  static ScaleLinetype scale_linetype(Map params = [:]) {
    return new ScaleLinetype(params)
  }

  /**
   * Discrete linetype scale (alias for scale_linetype).
   */
  static ScaleLinetype scale_linetype_discrete(Map params = [:]) {
    return new ScaleLinetype(params)
  }

  /**
   * Manual linetype scale for custom linetype mappings.
   *
   * Usage:
   * scale_linetype_manual(values: ['solid', 'dashed', 'dotted'])  // Map by order
   * scale_linetype_manual(values: [cat1: 'solid', cat2: 'dashed'])  // Map by name
   */
  static ScaleLinetypeManual scale_linetype_manual(Map params = [:]) {
    return new ScaleLinetypeManual(params)
  }

  // --- Identity scales ---

  /**
   * Identity scale for color - uses data values directly without mapping.
   *
   * When the data contains color values (e.g., 'red', 'blue', '#FF0000'), this scale
   * uses those exact values rather than mapping them through a palette.
   *
   * @param params optional scale parameters (naValue, name, guide)
   */
  static ScaleColorIdentity scale_color_identity(Map params = [:]) {
    return new ScaleColorIdentity(params)
  }

  /**
   * Identity scale for colour (British spelling alias).
   */
  static ScaleColorIdentity scale_colour_identity(Map params = [:]) {
    return scale_color_identity(params)
  }

  /**
   * Identity scale for fill - uses data values directly without mapping.
   *
   * When the data contains color values for fill, this scale uses those exact values
   * rather than mapping them through a palette.
   *
   * @param params optional scale parameters (naValue, name, guide)
   */
  static ScaleFillIdentity scale_fill_identity(Map params = [:]) {
    return new ScaleFillIdentity(params)
  }

  /**
   * Identity scale for size - uses data values directly without mapping.
   *
   * When the data contains numeric size values, this scale uses those exact values
   * rather than mapping them through a range.
   *
   * @param params optional scale parameters (naValue, name, guide)
   */
  static ScaleSizeIdentity scale_size_identity(Map params = [:]) {
    return new ScaleSizeIdentity(params)
  }

  /**
   * Identity scale for alpha - uses data values directly without mapping.
   *
   * When the data contains numeric alpha/transparency values, this scale uses those
   * exact values rather than mapping them through a range. Values are clamped to [0, 1].
   *
   * @param params optional scale parameters (naValue, name, guide)
   */
  static ScaleAlphaIdentity scale_alpha_identity(Map params = [:]) {
    return new ScaleAlphaIdentity(params)
  }

  /**
   * Identity scale for shape - uses data values directly without mapping.
   *
   * When the data contains shape names (e.g., 'circle', 'square', 'triangle'), this scale
   * uses those exact values rather than mapping them from a palette.
   *
   * @param params optional scale parameters (naValue, name, guide)
   */
  static ScaleShapeIdentity scale_shape_identity(Map params = [:]) {
    return new ScaleShapeIdentity(params)
  }

  /**
   * Identity scale for linetype - uses data values directly without mapping.
   *
   * When the data contains linetype names (e.g., 'solid', 'dashed', 'dotted'), this scale
   * uses those exact values rather than mapping them from a palette.
   *
   * @param params optional scale parameters (naValue, name, guide)
   */
  static ScaleLinetypeIdentity scale_linetype_identity(Map params = [:]) {
    return new ScaleLinetypeIdentity(params)
  }

  static StatsBin2D stat_bin_2d() {
    return new StatsBin2D()
  }

  /**
   * Create a hexagonal binning stat.
   * Divides the plotting area into hexagonal bins and counts observations in each bin.
   *
   * @param params stat parameters (bins, binwidth)
   * @return StatsBinHex instance
   */
  static StatsBinHex stat_bin_hex(Map params = [:]) {
    return new StatsBinHex(params)
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

  /**
   * Create a confidence ellipse stat for bivariate normal data.
   *
   * @param params stat parameters (level, type, segments)
   */
  static StatsEllipse stat_ellipse(Map params = [:]) {
    return new StatsEllipse(params)
  }

  /**
   * Create a binned summary stat.
   *
   * @param params stat parameters (bins, binwidth, fun)
   */
  static StatsSummaryBin stat_summary_bin(Map params = [:]) {
    return new StatsSummaryBin(params)
  }

  /**
   * Create a hexagonal summary stat.
   * Divides the plotting area into hexagonal bins and computes summary statistics for each bin.
   *
   * @param params stat parameters (bins, binwidth, fun, fun.data)
   * @return StatsSummaryHex instance
   */
  static StatsSummaryHex stat_summary_hex(Map params = [:]) {
    return new StatsSummaryHex(params)
  }

  /**
   * Create a unique observations stat (removes duplicates).
   *
   * @param params stat parameters (columns)
   */
  static StatsUnique stat_unique(Map params = [:]) {
    return new StatsUnique(params)
  }

  /**
   * Create a simple feature stat that expands WKT geometry into x/y coordinates.
   *
   * @param params stat parameters (geometry column override)
   */
  static StatsSf stat_sf(Map params = [:]) {
    return new StatsSf(params)
  }

  /**
   * Create a simple feature stat that computes representative coordinates for labels.
   *
   * @param params stat parameters (geometry column override)
   */
  static StatsSfCoordinates stat_sf_coordinates(Map params = [:]) {
    return new StatsSfCoordinates(params)
  }

  /**
   * Create a function stat (computes y from function of x).
   *
   * @param params stat parameters (fun, xlim, n)
   */
  static StatsFunction stat_function(Map params = [:]) {
    return new StatsFunction(params)
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
