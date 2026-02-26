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

  // Additional factory methods will be added as builders are introduced
  // in subsequent phases (9–11).
}
