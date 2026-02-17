package se.alipsa.matrix.charm.render.coord

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.render.LayerData

/**
 * Flipped coordinate transformation - swaps x and y axes.
 * Used for creating horizontal bar charts and similar visualizations.
 *
 * Swaps all coordinate-paired fields: x↔y, xmin↔ymin, xmax↔ymax, xend↔yend.
 * Since this swap happens in the pipeline before scale training, the trained
 * xScale will contain (originally y) values mapped to horizontal pixels, and
 * yScale will contain (originally x) values mapped to vertical pixels.
 *
 * Axis labels are swapped by the renderer (not here) since labels are on the Chart,
 * not on LayerData.
 */
@CompileStatic
class FlipCoord {

  /**
   * Applies flip coordinate transformation.
   * Swaps x↔y and related min/max/end fields on each datum.
   *
   * @param coordSpec coordinate specification
   * @param data layer data to transform
   * @return transformed layer data with swapped x/y
   */
  static List<LayerData> compute(CoordSpec coordSpec, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return data
    }

    List<LayerData> result = []
    data.each { LayerData datum ->
      LayerData flipped = new LayerData(
          x: datum.y,
          y: datum.x,
          color: datum.color,
          fill: datum.fill,
          xend: datum.yend,
          yend: datum.xend,
          xmin: datum.ymin,
          xmax: datum.ymax,
          ymin: datum.xmin,
          ymax: datum.xmax,
          size: datum.size,
          shape: datum.shape,
          alpha: datum.alpha,
          linetype: datum.linetype,
          group: datum.group,
          label: datum.label,
          weight: datum.weight,
          rowIndex: datum.rowIndex,
          meta: new LinkedHashMap<>(datum.meta)
      )
      result.add(flipped)
    }

    result
  }
}
