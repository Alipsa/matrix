package se.alipsa.matrix.charm.render

import groovy.transform.CompileStatic

/**
 * Utilities for working with {@link LayerData} instances in the render pipeline.
 */
@CompileStatic
class LayerDataUtil {

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
}
