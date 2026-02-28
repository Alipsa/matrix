package se.alipsa.matrix.pict

import se.alipsa.matrix.core.Matrix

/**
 * Bubble chart type — not yet implemented.
 *
 * @deprecated This chart type is a stub with no implementation. A Charm-backed
 * version will be added when the POINT geom supports size aesthetic mapping.
 * Use {@code ScatterChart} as an alternative in the meantime.
 */
@Deprecated
class BubbleChart {

  static BubbleChart create(String title, Matrix table, String xCol, String yCol, String sizeColumn, String groupCol) {
    throw new RuntimeException("Not yet implemented")
  }
}
