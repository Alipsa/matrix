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

  // TODO: geomTile()    — Phase 7
  // TODO: geomPie()     — Phase 7
  // TODO: geomRect()    — Phase 7
  // TODO: geomHex()     — Phase 7
  // TODO: geomBin2d()   — Phase 7
  // TODO: geomRaster()  — Phase 7
}
