package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic

/**
 * Static factory for geometry builders.
 *
 * <p>Intended for static import so users can write
 * {@code import static se.alipsa.matrix.charm.geom.Geoms.*}
 * and use builders outside of a {@code layers {}} block.</p>
 *
 * <pre>{@code
 * import static se.alipsa.matrix.charm.geom.Geoms.geomPoint
 *
 * plot(data) {
 *   mapping { x = 'cty'; y = 'hwy' }
 *   addLayer geomPoint().size(3)
 * }
 * }</pre>
 */
@CompileStatic
class Geoms {

  private Geoms() {}

  /**
   * Creates a new point builder.
   *
   * @return point builder
   */
  static PointBuilder geomPoint() {
    new PointBuilder()
  }

  /**
   * Creates a new line builder.
   *
   * @return line builder
   */
  static LineBuilder geomLine() {
    new LineBuilder()
  }

  /**
   * Creates a new smooth builder.
   *
   * @return smooth builder
   */
  static SmoothBuilder geomSmooth() {
    new SmoothBuilder()
  }

  /**
   * Creates a new area builder.
   *
   * @return area builder
   */
  static AreaBuilder geomArea() {
    new AreaBuilder()
  }

  /**
   * Creates a new ribbon builder.
   *
   * @return ribbon builder
   */
  static RibbonBuilder geomRibbon() {
    new RibbonBuilder()
  }

  /**
   * Creates a new bar builder.
   *
   * @return bar builder
   */
  static BarBuilder geomBar() {
    new BarBuilder()
  }

  /**
   * Creates a new column builder.
   *
   * @return column builder
   */
  static ColBuilder geomCol() {
    new ColBuilder()
  }

  /**
   * Creates a new histogram builder.
   *
   * @return histogram builder
   */
  static HistogramBuilder geomHistogram() {
    new HistogramBuilder()
  }

  /**
   * Creates a new boxplot builder.
   *
   * @return boxplot builder
   */
  static BoxplotBuilder geomBoxplot() {
    new BoxplotBuilder()
  }

  /**
   * Creates a new violin builder.
   *
   * @return violin builder
   */
  static ViolinBuilder geomViolin() {
    new ViolinBuilder()
  }

  /**
   * Creates a new dotplot builder.
   *
   * @return dotplot builder
   */
  static DotplotBuilder geomDotplot() {
    new DotplotBuilder()
  }

  /**
   * Creates a new tile builder.
   *
   * @return tile builder
   */
  static TileBuilder geomTile() {
    new TileBuilder()
  }

  /**
   * Creates a new pie builder.
   *
   * @return pie builder
   */
  static PieBuilder geomPie() {
    new PieBuilder()
  }

  /**
   * Creates a new rect builder.
   *
   * @return rect builder
   */
  static RectBuilder geomRect() {
    new RectBuilder()
  }

  /**
   * Creates a new hex bin builder.
   *
   * @return hex builder
   */
  static HexBuilder geomHex() {
    new HexBuilder()
  }

  /**
   * Creates a new 2D bin builder.
   *
   * @return bin2d builder
   */
  static Bin2dBuilder geomBin2d() {
    new Bin2dBuilder()
  }

  /**
   * Creates a new raster builder.
   *
   * @return raster builder
   */
  static RasterBuilder geomRaster() {
    new RasterBuilder()
  }

  /**
   * Creates a new text builder.
   *
   * @return text builder
   */
  static TextBuilder geomText() {
    new TextBuilder()
  }

  /**
   * Creates a new label builder.
   *
   * @return label builder
   */
  static LabelBuilder geomLabel() {
    new LabelBuilder()
  }

  /**
   * Creates a new segment builder.
   *
   * @return segment builder
   */
  static SegmentBuilder geomSegment() {
    new SegmentBuilder()
  }

  /**
   * Creates a new curve builder.
   *
   * @return curve builder
   */
  static CurveBuilder geomCurve() {
    new CurveBuilder()
  }

  /**
   * Creates a new abline builder.
   *
   * @return abline builder
   */
  static AblineBuilder geomAbline() {
    new AblineBuilder()
  }

  /**
   * Creates a new horizontal line builder.
   *
   * @return hline builder
   */
  static HlineBuilder geomHline() {
    new HlineBuilder()
  }

  /**
   * Creates a new vertical line builder.
   *
   * @return vline builder
   */
  static VlineBuilder geomVline() {
    new VlineBuilder()
  }

  /**
   * Creates a new density builder.
   *
   * @return density builder
   */
  static DensityBuilder geomDensity() {
    new DensityBuilder()
  }

  /**
   * Creates a new frequency polygon builder.
   *
   * @return freqpoly builder
   */
  static FreqpolyBuilder geomFreqpoly() {
    new FreqpolyBuilder()
  }

  /**
   * Creates a new QQ plot builder.
   *
   * @return qq builder
   */
  static QqBuilder geomQq() {
    new QqBuilder()
  }

  /**
   * Creates a new QQ line builder.
   *
   * @return qq line builder
   */
  static QqLineBuilder geomQqLine() {
    new QqLineBuilder()
  }

  /**
   * Creates a new quantile builder.
   *
   * @return quantile builder
   */
  static QuantileBuilder geomQuantile() {
    new QuantileBuilder()
  }

  /**
   * Creates a new error bar builder.
   *
   * @return errorbar builder
   */
  static ErrorbarBuilder geomErrorbar() {
    new ErrorbarBuilder()
  }

  /**
   * Creates a new crossbar builder.
   *
   * @return crossbar builder
   */
  static CrossbarBuilder geomCrossbar() {
    new CrossbarBuilder()
  }

  /**
   * Creates a new line range builder.
   *
   * @return linerange builder
   */
  static LinerangeBuilder geomLinerange() {
    new LinerangeBuilder()
  }

  /**
   * Creates a new point range builder.
   *
   * @return pointrange builder
   */
  static PointrangeBuilder geomPointrange() {
    new PointrangeBuilder()
  }

  /**
   * Creates a new path builder.
   *
   * @return path builder
   */
  static PathBuilder geomPath() {
    new PathBuilder()
  }

  /**
   * Creates a new step builder.
   *
   * @return step builder
   */
  static StepBuilder geomStep() {
    new StepBuilder()
  }

  /**
   * Creates a new jitter builder.
   *
   * @return jitter builder
   */
  static JitterBuilder geomJitter() {
    new JitterBuilder()
  }

  /**
   * Creates a new rug builder.
   *
   * @return rug builder
   */
  static RugBuilder geomRug() {
    new RugBuilder()
  }

  /**
   * Creates a new count builder.
   *
   * @return count builder
   */
  static CountBuilder geomCount() {
    new CountBuilder()
  }

  /**
   * Creates a new contour builder.
   *
   * @return contour builder
   */
  static ContourBuilder geomContour() {
    new ContourBuilder()
  }

  /**
   * Creates a new function builder.
   *
   * @return function builder
   */
  static FunctionBuilder geomFunction() {
    new FunctionBuilder()
  }
}
