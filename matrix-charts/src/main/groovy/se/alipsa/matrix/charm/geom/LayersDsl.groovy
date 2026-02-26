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

  // Additional factory methods will be added as builders are introduced
  // in subsequent phases (6–11).
}
