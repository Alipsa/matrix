package se.alipsa.matrix.charm.geom

import groovy.transform.CompileStatic

/**
 * DSL delegate for the {@code layers {}} block in {@link se.alipsa.matrix.charm.PlotSpec}.
 *
 * <p>Factory methods register builders into the {@link #collected} list and return
 * the builder so the user can chain fluent setters.</p>
 *
 * <pre>{@code
 * plot(data) {
 *   mapping { x = 'cty'; y = 'hwy' }
 *   layers {
 *     geomPoint().size(3).alpha(0.7)
 *   }
 * }
 * }</pre>
 */
@CompileStatic
class LayersDsl {

  /** Builders collected during DSL evaluation. */
  final List<LayerBuilder> collected = []

  /**
   * Creates and registers a point builder.
   *
   * @return new point builder
   */
  PointBuilder geomPoint() {
    PointBuilder b = new PointBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a sampled point builder.
   *
   * <p>Equivalent to {@code geomPoint().stat('sample')} with optional stat params
   * such as {@code n}, {@code seed}, and {@code method}.</p>
   *
   * @param params optional sampling parameters
   * @return sampled point builder
   */
  PointBuilder geomPointSampled(Map<String, Object> params = [:]) {
    PointBuilder b = geomPoint()
    b.stat('sample')
    b.params(params)
    b
  }

  /**
   * Creates and registers a line builder.
   *
   * @return new line builder
   */
  LineBuilder geomLine() {
    LineBuilder b = new LineBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a smooth builder.
   *
   * @return new smooth builder
   */
  SmoothBuilder geomSmooth() {
    SmoothBuilder b = new SmoothBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers an area builder.
   *
   * @return new area builder
   */
  AreaBuilder geomArea() {
    AreaBuilder b = new AreaBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a ribbon builder.
   *
   * @return new ribbon builder
   */
  RibbonBuilder geomRibbon() {
    RibbonBuilder b = new RibbonBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a bar builder.
   *
   * @return new bar builder
   */
  BarBuilder geomBar() {
    BarBuilder b = new BarBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a column builder.
   *
   * @return new column builder
   */
  ColBuilder geomCol() {
    ColBuilder b = new ColBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a histogram builder.
   *
   * @return new histogram builder
   */
  HistogramBuilder geomHistogram() {
    HistogramBuilder b = new HistogramBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a boxplot builder.
   *
   * @return new boxplot builder
   */
  BoxplotBuilder geomBoxplot() {
    BoxplotBuilder b = new BoxplotBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a violin builder.
   *
   * @return new violin builder
   */
  ViolinBuilder geomViolin() {
    ViolinBuilder b = new ViolinBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a dotplot builder.
   *
   * @return new dotplot builder
   */
  DotplotBuilder geomDotplot() {
    DotplotBuilder b = new DotplotBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a tile builder.
   *
   * @return new tile builder
   */
  TileBuilder geomTile() {
    TileBuilder b = new TileBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a pie builder.
   *
   * @return new pie builder
   */
  PieBuilder geomPie() {
    PieBuilder b = new PieBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a rect builder.
   *
   * @return new rect builder
   */
  RectBuilder geomRect() {
    RectBuilder b = new RectBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a hex bin builder.
   *
   * @return new hex builder
   */
  HexBuilder geomHex() {
    HexBuilder b = new HexBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a 2D bin builder.
   *
   * @return new bin2d builder
   */
  Bin2dBuilder geomBin2d() {
    Bin2dBuilder b = new Bin2dBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a raster builder.
   *
   * @return new raster builder
   */
  RasterBuilder geomRaster() {
    RasterBuilder b = new RasterBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a text builder.
   *
   * @return new text builder
   */
  TextBuilder geomText() {
    TextBuilder b = new TextBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a label builder.
   *
   * @return new label builder
   */
  LabelBuilder geomLabel() {
    LabelBuilder b = new LabelBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a segment builder.
   *
   * @return new segment builder
   */
  SegmentBuilder geomSegment() {
    SegmentBuilder b = new SegmentBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a curve builder.
   *
   * @return new curve builder
   */
  CurveBuilder geomCurve() {
    CurveBuilder b = new CurveBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers an abline builder.
   *
   * @return new abline builder
   */
  AblineBuilder geomAbline() {
    AblineBuilder b = new AblineBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a horizontal line builder.
   *
   * @return new hline builder
   */
  HlineBuilder geomHline() {
    HlineBuilder b = new HlineBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a vertical line builder.
   *
   * @return new vline builder
   */
  VlineBuilder geomVline() {
    VlineBuilder b = new VlineBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a density builder.
   *
   * @return new density builder
   */
  DensityBuilder geomDensity() {
    DensityBuilder b = new DensityBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a frequency polygon builder.
   *
   * @return new freqpoly builder
   */
  FreqpolyBuilder geomFreqpoly() {
    FreqpolyBuilder b = new FreqpolyBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a QQ plot builder.
   *
   * @return new qq builder
   */
  QqBuilder geomQq() {
    QqBuilder b = new QqBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a QQ line builder.
   *
   * @return new qq line builder
   */
  QqLineBuilder geomQqLine() {
    QqLineBuilder b = new QqLineBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a quantile builder.
   *
   * @return new quantile builder
   */
  QuantileBuilder geomQuantile() {
    QuantileBuilder b = new QuantileBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers an error bar builder.
   *
   * @return new errorbar builder
   */
  ErrorbarBuilder geomErrorbar() {
    ErrorbarBuilder b = new ErrorbarBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a crossbar builder.
   *
   * @return new crossbar builder
   */
  CrossbarBuilder geomCrossbar() {
    CrossbarBuilder b = new CrossbarBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a line range builder.
   *
   * @return new linerange builder
   */
  LinerangeBuilder geomLinerange() {
    LinerangeBuilder b = new LinerangeBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a point range builder.
   *
   * @return new pointrange builder
   */
  PointrangeBuilder geomPointrange() {
    PointrangeBuilder b = new PointrangeBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a path builder.
   *
   * @return new path builder
   */
  PathBuilder geomPath() {
    PathBuilder b = new PathBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a step builder.
   *
   * @return new step builder
   */
  StepBuilder geomStep() {
    StepBuilder b = new StepBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a jitter builder.
   *
   * @return new jitter builder
   */
  JitterBuilder geomJitter() {
    JitterBuilder b = new JitterBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a rug builder.
   *
   * @return new rug builder
   */
  RugBuilder geomRug() {
    RugBuilder b = new RugBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a count builder.
   *
   * @return new count builder
   */
  CountBuilder geomCount() {
    CountBuilder b = new CountBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a contour builder.
   *
   * @return new contour builder
   */
  ContourBuilder geomContour() {
    ContourBuilder b = new ContourBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a function builder.
   *
   * @return new function builder
   */
  FunctionBuilder geomFunction() {
    FunctionBuilder b = new FunctionBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a simple feature builder.
   *
   * @return new sf builder
   */
  SfBuilder geomSf() {
    SfBuilder b = new SfBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a simple feature label builder.
   *
   * @return new sf label builder
   */
  SfLabelBuilder geomSfLabel() {
    SfLabelBuilder b = new SfLabelBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a simple feature text builder.
   *
   * @return new sf text builder
   */
  SfTextBuilder geomSfText() {
    SfTextBuilder b = new SfTextBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a polygon builder.
   *
   * @return new polygon builder
   */
  PolygonBuilder geomPolygon() {
    PolygonBuilder b = new PolygonBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a map builder.
   *
   * @return new map builder
   */
  MapBuilder geomMap() {
    MapBuilder b = new MapBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a 2D density contour builder.
   *
   * @return new density2d builder
   */
  Density2dBuilder geomDensity2d() {
    Density2dBuilder b = new Density2dBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a filled 2D density builder.
   *
   * @return new density2d filled builder
   */
  Density2dFilledBuilder geomDensity2dFilled() {
    Density2dFilledBuilder b = new Density2dFilledBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a filled contour builder.
   *
   * @return new contour filled builder
   */
  ContourFilledBuilder geomContourFilled() {
    ContourFilledBuilder b = new ContourFilledBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a spoke builder.
   *
   * @return new spoke builder
   */
  SpokeBuilder geomSpoke() {
    SpokeBuilder b = new SpokeBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a magnification builder.
   *
   * @return new mag builder
   */
  MagBuilder geomMag() {
    MagBuilder b = new MagBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a parallel coordinate builder.
   *
   * @return new parallel builder
   */
  ParallelBuilder geomParallel() {
    ParallelBuilder b = new ParallelBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a log ticks builder.
   *
   * @return new logticks builder
   */
  LogticksBuilder geomLogticks() {
    LogticksBuilder b = new LogticksBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a blank builder.
   *
   * @return new blank builder
   */
  BlankBuilder geomBlank() {
    BlankBuilder b = new BlankBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a raster annotation builder.
   *
   * @return new raster annotation builder
   */
  RasterAnnBuilder geomRasterAnn() {
    RasterAnnBuilder b = new RasterAnnBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a horizontal error bar builder.
   *
   * @return new errorbarh builder
   */
  ErrorbarhBuilder geomErrorbarh() {
    ErrorbarhBuilder b = new ErrorbarhBuilder()
    collected << b
    b
  }

  /**
   * Creates and registers a custom builder.
   *
   * @return new custom builder
   */
  CustomBuilder geomCustom() {
    CustomBuilder b = new CustomBuilder()
    collected << b
    b
  }
}
