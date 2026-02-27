package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Mutable container for per-datum style overrides set by a style callback.
 *
 * <p>Properties left {@code null} are ignored; only non-null values override
 * the normal resolution chain in {@link se.alipsa.matrix.charm.render.geom.GeomUtils}.</p>
 */
@CompileStatic
class StyleOverride {

  String fill
  String color
  BigDecimal alpha
  BigDecimal size
  String shape
  String linetype
}
