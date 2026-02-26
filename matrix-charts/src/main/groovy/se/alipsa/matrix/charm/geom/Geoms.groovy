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

  // TODO: geomLine()    — Phase 5
  // TODO: geomSmooth()  — Phase 5
  // TODO: geomArea()    — Phase 5
  // TODO: geomRibbon()  — Phase 5
  // TODO: geomBar()     — Phase 6
  // TODO: geomCol()     — Phase 6
  // TODO: geomHistogram() — Phase 6
  // TODO: geomBoxplot() — Phase 6
  // TODO: geomViolin()  — Phase 6
  // TODO: geomDotplot() — Phase 6
}
