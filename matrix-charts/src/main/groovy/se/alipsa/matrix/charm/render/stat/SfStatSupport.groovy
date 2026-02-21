package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.render.LayerDataRowAccess
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

    List<String> columns = firstRowColumns(data)
    if (columns == null || columns.isEmpty()) {
      return 'geometry'
    }
    if (columns.contains('geometry')) {
      return 'geometry'
    }
    if (columns.contains('geom')) {
      return 'geom'
    }
    if (columns.contains('wkt')) {
      return 'wkt'
    }
    columns.find { String key -> key?.toLowerCase()?.contains('geometry') } ?: 'geometry'
  }

  static Map<String, Object> rowMap(LayerData datum) {
    LayerDataRowAccess.rowMap(datum)
  }

  static Object rowValue(LayerData datum, String columnName) {
    LayerDataRowAccess.value(datum, columnName)
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

  private static List<String> firstRowColumns(List<LayerData> data) {
    data?.findResult { LayerData datum ->
      List<String> cols = LayerDataRowAccess.columns(datum)
      cols.isEmpty() ? null : cols
    }
  }
}
