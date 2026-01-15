package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic

/**
 * Simple point representation with x and y coordinates.
 * Used for storing transformed coordinate positions in rendering pipeline.
 */
@CompileStatic
class Point {
  /** X coordinate */
  Number x

  /** Y coordinate */
  Number y

  /**
   * Create a point with the given coordinates.
   * @param x The x coordinate
   * @param y The y coordinate
   */
  Point(Number x, Number y) {
    this.x = x
    this.y = y
  }

  @Override
  String toString() {
    "Point(${x}, ${y})"
  }
}
