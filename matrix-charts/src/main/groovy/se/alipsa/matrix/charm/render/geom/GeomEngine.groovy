package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmRenderException
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext

/**
 * Central dispatch for geometry rendering in Charm.
 */
@CompileStatic
class GeomEngine {

  /**
   * Render one layer by geom type.
   */
  static void render(G dataLayer,
                     RenderContext context,
                     LayerSpec layer,
                     List<LayerData> layerData,
                     int panelWidth,
                     int panelHeight) {
    CharmGeomType geomType = layer.geomType
    switch (geomType) {
      case CharmGeomType.POINT -> PointRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.LINE -> LineRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.BAR, CharmGeomType.COL -> BarRenderer.render(dataLayer, context, layer, layerData, panelHeight)
      case CharmGeomType.HISTOGRAM -> HistogramRenderer.render(dataLayer, context, layer, layerData, panelHeight)
      case CharmGeomType.BOXPLOT -> BoxplotRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.AREA -> AreaRenderer.render(dataLayer, context, layer, layerData, panelHeight)
      case CharmGeomType.SMOOTH -> SmoothRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.DENSITY -> DensityRenderer.render(dataLayer, context, layer, layerData, panelHeight)
      case CharmGeomType.VIOLIN -> ViolinRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.TILE -> TileRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.TEXT -> TextRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.LABEL -> LabelRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.JITTER -> PointRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.PATH -> PathRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.STEP -> StepRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.FREQPOLY -> LineRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.SEGMENT,
           CharmGeomType.HLINE,
           CharmGeomType.VLINE,
           CharmGeomType.ABLINE -> SegmentRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.ERRORBAR,
           CharmGeomType.ERRORBARH,
           CharmGeomType.CROSSBAR,
           CharmGeomType.LINERANGE,
           CharmGeomType.POINTRANGE -> IntervalRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.RIBBON -> RibbonRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.RECT -> RectRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.POLYGON -> PolygonRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.RUG -> RugRenderer.render(dataLayer, context, layer, layerData, panelWidth, panelHeight)
      case CharmGeomType.HEX -> HexRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.CONTOUR -> ContourRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.BIN2D -> TileRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.BLANK -> BlankRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.CONTOUR_FILLED -> ContourRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.COUNT -> PointRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.CURVE -> CurveRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.CUSTOM -> BlankRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.DENSITY_2D -> ContourRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.DENSITY_2D_FILLED -> ContourRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.DOTPLOT -> PointRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.FUNCTION -> LineRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.LOGTICKS -> RugRenderer.render(dataLayer, context, layer, layerData, panelWidth, panelHeight)
      case CharmGeomType.MAG -> PointRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.MAP -> PolygonRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.PARALLEL -> PathRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.QQ -> PointRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.QQ_LINE -> LineRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.QUANTILE -> LineRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.RASTER -> TileRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.RASTER_ANN -> TileRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.SPOKE -> SpokeRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.SF -> SfRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.SF_LABEL -> LabelRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.SF_TEXT -> TextRenderer.render(dataLayer, context, layer, layerData)
      case CharmGeomType.PIE -> PieRenderer.render(dataLayer, context, layer, layerData, panelWidth, panelHeight)
      default -> throw new CharmRenderException("Unsupported geom type: ${geomType}")
    }
  }
}
