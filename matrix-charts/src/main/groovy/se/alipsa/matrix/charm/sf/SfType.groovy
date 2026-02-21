package se.alipsa.matrix.charm.sf

import groovy.transform.CompileStatic

/**
 * Supported simple feature geometry types for WKT parsing.
 */
@CompileStatic
enum SfType {
  POINT,
  LINESTRING,
  POLYGON,
  MULTIPOINT,
  MULTILINESTRING,
  MULTIPOLYGON,
  GEOMETRYCOLLECTION
}
