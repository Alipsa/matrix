package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Defs
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart

/**
 * Per-render immutable context.
 */
@CompileStatic
class RenderContext {

  final Chart chart
  final RenderConfig config
  final Svg svg
  Defs defs

  ScaleModel xScale
  ScaleModel yScale
  ScaleModel colorScale
  ScaleModel fillScale
  List<PanelSpec> panels = []

  /**
   * Creates render context.
   *
   * @param chart compiled chart
   * @param config render config
   * @param svg target svg
   */
  RenderContext(Chart chart, RenderConfig config, Svg svg) {
    this.chart = chart
    this.config = config
    this.svg = svg
  }
}

/**
 * Panel definition used for facet layout.
 */
@CompileStatic
class PanelSpec {
  int row
  int col
  String label
  List<Integer> rowIndexes = []
}

/**
 * Layer data flowing through stat -> position -> scale -> coord.
 */
@CompileStatic
class LayerData {
  Object x
  Object y
  Object color
  Object fill
  int rowIndex
  Map<String, Object> meta = [:]
}

/**
 * Layer render job.
 */
@CompileStatic
class LayerRenderJob {
  se.alipsa.matrix.charm.LayerSpec layer
  se.alipsa.matrix.charm.Aes aes
  List<LayerData> data = []
}
