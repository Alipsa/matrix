package se.alipsa.matrix.charm.render.coord

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CoordSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.LayerDataUtil

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
      LayerData flipped = LayerDataUtil.copyDatum(datum)
      flipped.x = datum.y
      flipped.y = datum.x
      flipped.xend = datum.yend
      flipped.yend = datum.xend
      flipped.xmin = datum.ymin
      flipped.xmax = datum.ymax
      flipped.ymin = datum.xmin
      flipped.ymax = datum.xmax
      result.add(flipped)
    }

    // Apply optional limits after flip so xlim/ylim operate in flipped space.
    CartesianCoord.compute(coordSpec, result)
  }
}
