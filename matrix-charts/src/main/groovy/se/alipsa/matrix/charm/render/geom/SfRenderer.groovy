package se.alipsa.matrix.charm.render.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.RenderContext
import java.util.Locale

/**
 * Renders simple-feature outputs by routing to point/path/polygon renderers.
 */
@CompileStatic
class SfRenderer {

  static void render(G dataLayer, RenderContext context, LayerSpec layer, List<LayerData> layerData) {
    if (layerData == null || layerData.isEmpty()) {
      return
    }

    List<LayerData> pointData = []
    List<LayerData> lineData = []
    List<LayerData> polygonData = []

    layerData.each { LayerData datum ->
      String type = sfType(datum)
      switch (type) {
        case 'POINT', 'MULTIPOINT' -> pointData << datum
        case 'LINESTRING', 'MULTILINESTRING' -> lineData << datum
        case 'POLYGON', 'MULTIPOLYGON' -> polygonData << datum
        default -> lineData << datum
      }
    }

    if (!polygonData.isEmpty()) {
      PolygonRenderer.render(dataLayer, context, layer, polygonData)
    }
    if (!lineData.isEmpty()) {
      PathRenderer.render(dataLayer, context, layer, lineData)
    }
    if (!pointData.isEmpty()) {
      PointRenderer.render(dataLayer, context, layer, pointData)
    }
  }

  private static String sfType(LayerData datum) {
    String fromMeta = datum?.meta?.__sf_type?.toString()
    if (fromMeta != null && !fromMeta.isEmpty()) {
      return fromMeta.toUpperCase(Locale.ROOT)
    }
    if (datum?.group?.toString()?.contains(':')) {
      return 'POLYGON'
    }
    'POINT'
  }
}
