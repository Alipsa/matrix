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
      case CharmGeomType.PIE -> PieRenderer.render(dataLayer, context, layer, layerData, panelWidth, panelHeight)
      default -> throw new CharmRenderException("Unsupported geom type: ${geomType}")
    }
  }
}
