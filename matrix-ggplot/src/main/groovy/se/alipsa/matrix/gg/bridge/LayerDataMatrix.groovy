package se.alipsa.matrix.gg.bridge

import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.Matrix

/** Converts Charm pipeline rows to ggplot-style matrices. */
class LayerDataMatrix {

  private static final List<String> AESTHETICS = [
      'x', 'y', 'xmin', 'xmax', 'ymin', 'ymax', 'xend', 'yend', 'colour', 'fill',
      'size', 'shape', 'alpha', 'linetype', 'group', 'label', 'tooltip', 'weight'
  ].asImmutable()

  private LayerDataMatrix() {
  }

  /**
   * Converts panel-major layer data to one matrix with a one-based PANEL column.
   *
   * @param panels data grouped by panel
   * @param name matrix name
   * @return built-data matrix
   */
  static Matrix toMatrix(List<List<LayerData>> panels, String name) {
    List<LayerData> rows = []
    List<Object> panelIds = []
    (panels ?: []).eachWithIndex { List<LayerData> panelData, int index ->
      (panelData ?: []).each { LayerData datum ->
        rows << datum
        panelIds << index + 1
      }
    }
    Map<String, List> columns = [:]
    if (!rows.isEmpty()) {
      columns['PANEL'] = panelIds
    }
    AESTHETICS.each { String aesthetic ->
      List<Object> values = rows.collect { LayerData datum -> value(datum, aesthetic) }
      if (values.any { it != null }) {
        columns[aesthetic] = values
      }
    }
    Set<String> metaKeys = new LinkedHashSet<>()
    rows.each { LayerData datum -> metaKeys.addAll(datum.meta.keySet()) }
    metaKeys.each { String key ->
      if (!columns.containsKey(key)) {
        columns[key] = rows.collect { LayerData datum -> datum.meta[key] }
      }
    }
    columns.isEmpty() ? new Matrix(name, [], [], []) : Matrix.builder().matrixName(name).data(columns).build()
  }

  private static Object value(LayerData datum, String aesthetic) {
    switch (aesthetic) {
      case 'x' -> datum.x
      case 'y' -> datum.y
      case 'xmin' -> datum.xmin
      case 'xmax' -> datum.xmax
      case 'ymin' -> datum.ymin
      case 'ymax' -> datum.ymax
      case 'xend' -> datum.xend
      case 'yend' -> datum.yend
      case 'colour' -> datum.color
      case 'fill' -> datum.fill
      case 'size' -> datum.size
      case 'shape' -> datum.shape
      case 'alpha' -> datum.alpha
      case 'linetype' -> datum.linetype
      case 'group' -> datum.group
      case 'label' -> datum.label
      case 'tooltip' -> datum.tooltip
      case 'weight' -> datum.weight
      default -> null
    }
  }
}
