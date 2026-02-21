package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.sf.SfGeometry

/**
 * Expands simple-feature geometries into x/y rows for rendering.
 */
@CompileStatic
class SfStat {

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

      int partIndex = 0
      geometry.shapes.each { shape ->
        int ringIndex = 0
        shape.rings.each { ring ->
          ring.points.each { point ->
            LayerData datum = new LayerData(
                x: point.x,
                y: point.y,
                color: source.color,
                fill: source.fill,
                size: source.size,
                alpha: source.alpha,
                linetype: source.linetype,
                shape: source.shape,
                label: source.label,
                group: "${idx}:${partIndex}:${ringIndex}",
                rowIndex: source.rowIndex
            )
            datum.meta = source.meta != null ? new LinkedHashMap<>(source.meta) : [:]
            datum.meta.__sf_id = idx
            datum.meta.__sf_part = partIndex
            datum.meta.__sf_ring = ringIndex
            datum.meta.__sf_hole = ring.hole
            datum.meta.__sf_type = (shape.type ?: geometry.type)?.toString()
            datum.meta.__sf_group = datum.group
            result << datum
          }
          ringIndex++
        }
        partIndex++
      }
    }

    result
  }
}
