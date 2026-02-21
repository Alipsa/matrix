package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.sf.SfGeometry
import se.alipsa.matrix.charm.sf.SfGeometryUtils

/**
 * Computes representative label points for simple-feature geometries.
 */
@CompileStatic
class SfCoordinatesStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    String geometryCol = SfStatSupport.resolveGeometryColumn(params, data)

    List<LayerData> result = []
    data.eachWithIndex { LayerData source, int idx ->
      Map<String, Object> row = SfStatSupport.rowMap(source)
      Object geometryValue = row[geometryCol]
      if (geometryValue == null) {
        geometryValue = source.label
      }
      SfGeometry geometry
      try {
        geometry = SfStatSupport.toGeometry(geometryValue)
      } catch (Exception ignored) {
        return
      }
      if (geometry == null || geometry.empty) {
        return
      }

      def point = SfGeometryUtils.representativePoint(geometry)
      if (point == null) {
        return
      }

      LayerData datum = new LayerData(
          x: point.x,
          y: point.y,
          color: source.color,
          fill: source.fill,
          size: source.size,
          alpha: source.alpha,
          linetype: source.linetype,
          shape: source.shape,
          label: source.label ?: row['label'],
          rowIndex: source.rowIndex
      )
      datum.meta = source.meta != null ? new LinkedHashMap<>(source.meta) : [:]
      datum.meta.__sf_id = idx
      datum.meta.__sf_type = geometry.type?.toString()
      result << datum
    }

    result
  }
}
