package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Expanded statistical transformation types covering the full gg surface.
 *
 * Each value corresponds to a ggplot2 stat family. Values mirror
 * {@code se.alipsa.matrix.gg.layer.StatType} to support 1:1 mapping
 * from the gg adapter.
 */
@CompileStatic
enum CharmStatType {
  IDENTITY,
  COUNT,
  BIN,
  BOXPLOT,
  SMOOTH,
  QUANTILE,
  SUMMARY,
  DENSITY,
  YDENSITY,
  DENSITY_2D,
  BIN2D,
  BIN_HEX,
  SUMMARY_HEX,
  SUMMARY_2D,
  CONTOUR,
  ECDF,
  QQ,
  QQ_LINE,
  ELLIPSE,
  SUMMARY_BIN,
  UNIQUE,
  SAMPLE,
  FUNCTION,
  SF,
  SF_COORDINATES,
  SPOKE,
  ALIGN
}
