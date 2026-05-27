package se.alipsa.matrix.charm.render.stat

import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.sf.SfGeometry
import se.alipsa.matrix.charm.sf.SfGeometryUtils
import se.alipsa.matrix.core.util.Logger

/**
 * Computes representative label points for simple-feature geometries.
 */
@SuppressWarnings('ReturnNullFromCatchBlock')
class SfCoordinatesStat {

  private static final Logger log = Logger.getLogger(SfCoordinatesStat)

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    String geometryCol = SfStatSupport.resolveGeometryColumn(params, data)

    List<LayerData> result = []
    data.eachWithIndex { LayerData source, int idx ->
      Object geometryValue = SfStatSupport.rowValue(source, geometryCol)
      if (geometryValue == null) {
        geometryValue = source.label
      }
      SfGeometry geometry
      try {
        geometry = SfStatSupport.toGeometry(geometryValue)
      } catch (Exception e) {
        log.debug("SfCoordinatesStat: skipping row - cannot parse geometry from '${geometryValue}': ${e.message}")
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
          label: source.label ?: SfStatSupport.rowValue(source, 'label'),
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
