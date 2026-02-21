package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.sf.SfGeometry
import se.alipsa.matrix.charm.sf.WktReader
import se.alipsa.matrix.charm.render.LayerData

/**
 * Shared helpers for SF-related stats.
 */
@CompileStatic
class SfStatSupport {

  private SfStatSupport() {
    // Utility class
  }

  static String resolveGeometryColumn(Map<String, Object> params, List<LayerData> data) {
    String explicit = params?.geometry?.toString()
    if (explicit != null && !explicit.isEmpty()) {
      return explicit
    }

    Map<String, Object> row = firstRowMap(data)
    if (row == null) {
      return 'geometry'
    }
    if (row.containsKey('geometry')) {
      return 'geometry'
    }
    if (row.containsKey('geom')) {
      return 'geom'
    }
    if (row.containsKey('wkt')) {
      return 'wkt'
    }
    row.keySet().find { String key -> key?.toLowerCase()?.contains('geometry') } ?: 'geometry'
  }

  static Map<String, Object> rowMap(LayerData datum) {
    if (datum?.meta?.__row instanceof Map) {
      return datum.meta.__row as Map<String, Object>
    }
    [:]
  }

  static SfGeometry toGeometry(Object value) {
    if (value == null) {
      return null
    }
    if (value instanceof SfGeometry) {
      return value as SfGeometry
    }
    WktReader.parse(value.toString())
  }

  private static Map<String, Object> firstRowMap(List<LayerData> data) {
    data?.findResult { LayerData datum ->
      Map<String, Object> row = rowMap(datum)
      row.isEmpty() ? null : row
    }
  }
}
