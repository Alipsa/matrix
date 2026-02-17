package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Expanded geometry types covering the full gg surface.
 *
 * Each value corresponds to a ggplot2 geom family. The Charm renderer
 * dispatches to rendering logic based on this type.
 */
@CompileStatic
enum CharmGeomType {
  POINT,
  LINE,
  BAR,
  COL,
  TILE,
  HISTOGRAM,
  BOXPLOT,
  SMOOTH,
  AREA,
  PIE,
  ABLINE,
  BIN2D,
  BLANK,
  CONTOUR,
  CONTOUR_FILLED,
  COUNT,
  CROSSBAR,
  CURVE,
  CUSTOM,
  DENSITY,
  DENSITY_2D,
  DENSITY_2D_FILLED,
  DOTPLOT,
  ERRORBAR,
  ERRORBARH,
  FREQPOLY,
  FUNCTION,
  HEX,
  HLINE,
  JITTER,
  LABEL,
  LINERANGE,
  LOGTICKS,
  MAG,
  MAP,
  PARALLEL,
  PATH,
  POINTRANGE,
  POLYGON,
  QQ,
  QQ_LINE,
  QUANTILE,
  RASTER,
  RASTER_ANN,
  RECT,
  RIBBON,
  RUG,
  SEGMENT,
  SF,
  SF_LABEL,
  SF_TEXT,
  SPOKE,
  STEP,
  TEXT,
  VIOLIN,
  VLINE

  /**
   * Geometry types currently supported by the Charm renderer.
   */
  static final Set<CharmGeomType> SUPPORTED = EnumSet.of(
      POINT, LINE, BAR, COL, HISTOGRAM, BOXPLOT, AREA, SMOOTH, DENSITY, VIOLIN, TILE, TEXT, PIE
  )
}
