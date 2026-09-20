package se.alipsa.matrix.charm.render

import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.LayerSpec

/**
 * Utilities for working with {@link LayerData} instances in the render pipeline.
 */
class LayerDataUtil {

  private static final Set<CharmGeomType> ZERO_BASELINE_GEOMS =
      EnumSet.of(CharmGeomType.BAR, CharmGeomType.COL, CharmGeomType.HISTOGRAM)
  private static final List<String> X_META_BOUNDS = ['xmin', 'xmax', 'binStart', 'binEnd'].asImmutable()
  private static final List<String> Y_META_BOUNDS =
      ['ymin', 'ymax', 'whiskerLow', 'whiskerHigh', 'outliers'].asImmutable()

  private LayerDataUtil() {
    // Utility class
  }

  /**
   * Creates a shallow copy of a {@link LayerData}.
   * The {@code meta} map is copied to avoid mutating the source datum.
   *
   * @param datum the source datum
   * @return copied datum
   */
  static LayerData copyDatum(LayerData datum) {
    new LayerData(
        x: datum.x,
        y: datum.y,
        color: datum.color,
        fill: datum.fill,
        xend: datum.xend,
        yend: datum.yend,
        xmin: datum.xmin,
        xmax: datum.xmax,
        ymin: datum.ymin,
        ymax: datum.ymax,
        size: datum.size,
        shape: datum.shape,
        alpha: datum.alpha,
        linetype: datum.linetype,
        group: datum.group,
        label: datum.label,
        tooltip: datum.tooltip,
        weight: datum.weight,
        rowIndex: datum.rowIndex,
        meta: datum.meta != null ? new LinkedHashMap<>(datum.meta) : [:]
    )
  }

  /**
   * Collect every x-axis value a layer contributes to scale training.
   *
   * @param layer the layer (used for geom-specific rules)
   * @param data position-adjusted layer data
   * @param includeZeroBaseline true when this is the bar-value axis
   * @param coordinateFlipped true when the data was transformed by {@code coordFlip}
   * @return non-null training values
   */
  static List<Object> xTrainingValues(
      LayerSpec layer,
      List<LayerData> data,
      boolean includeZeroBaseline = false,
      boolean coordinateFlipped = false
  ) {
    List<Object> values = []
    data.each { LayerData datum ->
      [datum.x, datum.xmin, datum.xmax, datum.xend].each { Object value ->
        if (value != null) {
          values << value
        }
      }
      addMetaBounds(values, datum.meta, coordinateFlipped ? Y_META_BOUNDS : X_META_BOUNDS)
    }
    addZeroBaseline(values, layer, data, includeZeroBaseline)
  }

  /**
   * Collect every y-axis value a layer contributes to scale training.
   *
   * @param layer the layer (used for geom-specific rules)
   * @param data position-adjusted layer data
   * @param includeZeroBaseline true when this is the bar-value axis
   * @param coordinateFlipped true when the data was transformed by {@code coordFlip}
   * @return non-null training values
   */
  static List<Object> yTrainingValues(
      LayerSpec layer,
      List<LayerData> data,
      boolean includeZeroBaseline = true,
      boolean coordinateFlipped = false
  ) {
    List<Object> values = []
    data.each { LayerData datum ->
      [datum.y, datum.ymin, datum.ymax, datum.yend].each { Object value ->
        if (value != null) {
          values << value
        }
      }
      addMetaBounds(values, datum.meta, coordinateFlipped ? X_META_BOUNDS : Y_META_BOUNDS)
    }
    addZeroBaseline(values, layer, data, includeZeroBaseline)
  }

  private static List<Object> addZeroBaseline(
      List<Object> values,
      LayerSpec layer,
      List<LayerData> data,
      boolean includeZeroBaseline
  ) {
    if (includeZeroBaseline && !data.isEmpty() && layer?.geomType in ZERO_BASELINE_GEOMS) {
      values << BigDecimal.ZERO
    }
    values
  }

  private static void addMetaBounds(List<Object> values, Map<String, Object> meta, List<String> keys) {
    if (meta == null) {
      return
    }
    keys.each { String key ->
      Object value = meta[key]
      if (value instanceof Collection) {
        value.each { Object item ->
          if (item != null) {
            values << item
          }
        }
      } else if (value != null) {
        values << value
      }
    }
  }

}
