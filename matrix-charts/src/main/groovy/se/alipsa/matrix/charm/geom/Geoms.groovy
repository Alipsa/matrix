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

  // TODO: geomBar()     — Phase 6
  // TODO: geomCol()     — Phase 6
  // TODO: geomHistogram() — Phase 6
  // TODO: geomBoxplot() — Phase 6
  // TODO: geomViolin()  — Phase 6
  // TODO: geomDotplot() — Phase 6
}
