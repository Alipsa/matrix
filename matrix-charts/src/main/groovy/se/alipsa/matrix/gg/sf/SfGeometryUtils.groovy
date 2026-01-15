package se.alipsa.matrix.gg.sf

import groovy.transform.CompileStatic

/**
 * Utility helpers for simple feature geometry calculations.
 */
@CompileStatic
class SfGeometryUtils {

  /**
   * Compute a bounding box for the geometry.
   *
   * @param geometry the geometry to inspect
   * @return [minX, minY, maxX, maxY] or null if no points exist
   */
  static List<BigDecimal> bbox(SfGeometry geometry) {
    if (geometry == null || geometry.empty || geometry.shapes.isEmpty()) return null
    List<BigDecimal> xs = []
    List<BigDecimal> ys = []
    geometry.shapes.each { shape ->
      shape.rings.each { ring ->
        ring.points.each { point ->
          xs << point.x
          ys << point.y
        }
      }
    }
    if (xs.isEmpty()) return null
    return [xs.min(), ys.min(), xs.max(), ys.max()]
  }

  /**
   * Return a representative point for labeling (centroid/average).
   *
   * @param geometry the geometry to inspect
   * @return a representative point, or null if none
   */
  static SfPoint representativePoint(SfGeometry geometry) {
    if (geometry == null || geometry.empty || geometry.shapes.isEmpty()) return null
    switch (geometry.type) {
      case SfType.POINT:
      case SfType.MULTIPOINT:
        return averagePoint(geometry)
      case SfType.LINESTRING:
      case SfType.MULTILINESTRING:
        return averagePoint(geometry)
      case SfType.POLYGON:
      case SfType.MULTIPOLYGON:
        return polygonCentroid(geometry)
      case SfType.GEOMETRYCOLLECTION:
        return averagePoint(geometry)
      default:
        return averagePoint(geometry)
    }
  }

  private static SfPoint averagePoint(SfGeometry geometry) {
    double sumX = 0
    double sumY = 0
    int count = 0
    geometry.shapes.each { shape ->
      shape.rings.each { ring ->
        ring.points.each { point ->
          sumX += point.x as double
          sumY += point.y as double
          count++
        }
      }
    }
    if (count == 0) return null
    return new SfPoint(BigDecimal.valueOf(sumX / count), BigDecimal.valueOf(sumY / count))
  }

  private static SfPoint polygonCentroid(SfGeometry geometry) {
    double weightedX = 0
    double weightedY = 0
    double totalArea = 0

    geometry.shapes.each { shape ->
      if (shape.rings.isEmpty()) return
      SfRing outer = shape.rings[0]
      double[] centroidArea = ringCentroid(outer.points)
      double area = centroidArea[2]
      if (area == 0) return
      weightedX += centroidArea[0] * area
      weightedY += centroidArea[1] * area
      totalArea += area
    }

    if (totalArea == 0) {
      return averagePoint(geometry)
    }
    return new SfPoint(BigDecimal.valueOf(weightedX / totalArea), BigDecimal.valueOf(weightedY / totalArea))
  }

  private static double[] ringCentroid(List<SfPoint> points) {
    if (points == null || points.size() < 3) {
      return [0d, 0d, 0d] as double[]
    }
    double area = 0
    double cx = 0
    double cy = 0

    int n = points.size()
    for (int i = 0; i < n; i++) {
      SfPoint p1 = points[i]
      SfPoint p2 = points[(i + 1) % n]
      double x1 = p1.x as double
      double y1 = p1.y as double
      double x2 = p2.x as double
      double y2 = p2.y as double
      double cross = (x1 * y2) - (x2 * y1)
      area += cross
      cx += (x1 + x2) * cross
      cy += (y1 + y2) * cross
    }

    area /= 2d
    if (area == 0) {
      return [0d, 0d, 0d] as double[]
    }
    cx /= (6d * area)
    cy /= (6d * area)
    return [cx, cy, area.abs()] as double[]
  }
}
