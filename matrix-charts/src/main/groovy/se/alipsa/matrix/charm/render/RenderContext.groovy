package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Defs
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.CssAttributesSpec
import se.alipsa.matrix.charm.render.scale.CharmScale
import se.alipsa.matrix.charm.render.scale.ColorCharmScale
import se.alipsa.matrix.charm.render.scale.TrainedScales

/**
 * Per-render write-once context.
 *
 * The chart/config/svg references are immutable, while trained scales/panels/defs
 * are initialized once during renderer setup and then treated as read-only.
 */
@CompileStatic
class RenderContext {

  final Chart chart
  final CssAttributesSpec cssAttributes
  final RenderConfig config
  final Svg svg
  Defs defs

  CharmScale xScale
  CharmScale yScale
  ColorCharmScale colorScale
  ColorCharmScale fillScale
  CharmScale sizeScale
  CharmScale shapeScale
  CharmScale alphaScale
  CharmScale linetypeScale
  CharmScale groupScale
  int layerIndex = -1
  Integer panelRow
  Integer panelCol
  List<PanelSpec> panels = []
  final Map<se.alipsa.matrix.charm.LayerSpec, Map<List<Integer>, List<LayerData>>> pipelineCache = [:]

  /** Per-layer trained scales, keyed by layer index. */
  Map<Integer, TrainedScales> layerScales = [:]

  /**
   * Creates render context.
   *
   * @param chart compiled chart
   * @param config render config
   * @param svg target svg
   */
  RenderContext(Chart chart, RenderConfig config, Svg svg) {
    this.chart = chart
    this.cssAttributes = chart.cssAttributes
    this.config = config
    this.svg = svg
  }

  /**
   * Returns the x scale for the given layer, falling back to the global x scale.
   *
   * @param layerIdx layer index
   * @return per-layer x scale or global x scale
   */
  CharmScale xScaleForLayer(int layerIdx) {
    layerScales[layerIdx]?.x ?: xScale
  }

  /**
   * Returns the y scale for the given layer, falling back to the global y scale.
   *
   * @param layerIdx layer index
   * @return per-layer y scale or global y scale
   */
  CharmScale yScaleForLayer(int layerIdx) {
    layerScales[layerIdx]?.y ?: yScale
  }

  /**
   * Returns the color scale for the given layer, falling back to the global color scale.
   *
   * @param layerIdx layer index
   * @return per-layer color scale or global color scale
   */
  ColorCharmScale colorScaleForLayer(int layerIdx) {
    layerScales[layerIdx]?.color ?: colorScale
  }

  /**
   * Returns the fill scale for the given layer, falling back to the global fill scale.
   *
   * @param layerIdx layer index
   * @return per-layer fill scale or global fill scale
   */
  ColorCharmScale fillScaleForLayer(int layerIdx) {
    layerScales[layerIdx]?.fill ?: fillScale
  }

  /**
   * Returns the size scale for the given layer, falling back to the global size scale.
   *
   * @param layerIdx layer index
   * @return per-layer size scale or global size scale
   */
  CharmScale sizeScaleForLayer(int layerIdx) {
    layerScales[layerIdx]?.size ?: sizeScale
  }

  /**
   * Returns the shape scale for the given layer, falling back to the global shape scale.
   *
   * @param layerIdx layer index
   * @return per-layer shape scale or global shape scale
   */
  CharmScale shapeScaleForLayer(int layerIdx) {
    layerScales[layerIdx]?.shape ?: shapeScale
  }

  /**
   * Returns the alpha scale for the given layer, falling back to the global alpha scale.
   *
   * @param layerIdx layer index
   * @return per-layer alpha scale or global alpha scale
   */
  CharmScale alphaScaleForLayer(int layerIdx) {
    layerScales[layerIdx]?.alpha ?: alphaScale
  }

  /**
   * Returns the linetype scale for the given layer, falling back to the global linetype scale.
   *
   * @param layerIdx layer index
   * @return per-layer linetype scale or global linetype scale
   */
  CharmScale linetypeScaleForLayer(int layerIdx) {
    layerScales[layerIdx]?.linetype ?: linetypeScale
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
  /** Row strip label (used by FacetGrid for right-side row strips) */
  String rowLabel
  /** Column strip label (used by FacetGrid for top column strips) */
  String colLabel
  /** Facet variable values for this panel */
  Map<String, Object> facetValues = [:]
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
  Object xend
  Object yend
  Object xmin
  Object xmax
  Object ymin
  Object ymax
  Object size
  Object shape
  Object alpha
  Object linetype
  Object group
  Object label
  String tooltip
  Object weight
  int rowIndex
  Map<String, Object> meta = [:]
}

/**
 * Layer render job.
 */
@CompileStatic
class LayerRenderJob {
  se.alipsa.matrix.charm.LayerSpec layer
  se.alipsa.matrix.charm.Mapping mapping
  List<LayerData> data = []
}
