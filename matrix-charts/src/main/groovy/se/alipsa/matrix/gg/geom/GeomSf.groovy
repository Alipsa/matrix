package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale

/**
 * Simple Features geometry for rendering WKT-based spatial data.
 * Routes POINTs to geom_point, LINESTRINGs to geom_path, and POLYGONs to geom_polygon.
 */
@CompileStatic
class GeomSf extends Geom {

  /** Outline color */
  String color = 'black'

  /** Fill color */
  String fill = 'gray'

  /** Line width */
  Number size = 1

  /** Line type */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

  /** Point shape */
  String shape = 'circle'

  GeomSf() {
    defaultStat = StatType.SF
    requiredAes = []
    defaultAes = [color: 'black', fill: 'gray', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  GeomSf(Map params) {
    this()
    if (params.color) this.color = ColorUtil.normalizeColor(params.color as String)
    if (params.colour) this.color = ColorUtil.normalizeColor(params.colour as String)
    if (params.fill) this.fill = ColorUtil.normalizeColor(params.fill as String)
    if (params.size != null) this.size = params.size as Number
    if (params.linewidth != null) this.size = params.linewidth as Number
    if (params.linetype) this.linetype = params.linetype as String
    if (params.alpha != null) this.alpha = params.alpha as Number
    if (params.shape) this.shape = params.shape as String
    this.params = params
  }

  /**
   * Render simple feature geometries using existing point/line/polygon geoms.
   *
   * @param group SVG group for rendering
   * @param data stat-expanded geometry data
   * @param aes aesthetic mappings
   * @param scales scale map for rendering
   * @param coord coordinate system
   */
  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    // Check for stat_sf preprocessing
    if (!data.columnNames().contains('__sf_type')) {
      throw new IllegalStateException(
          "geom_sf requires stat_sf - data must contain '__sf_type' column. " +
          "Ensure stat='sf' is set or use stat_sf() explicitly.")
    }

    Aes resolvedAes = resolveAes(aes)
    Map<String, List<Map<String, Object>>> buckets = [
        'point': [],
        'line': [],
        'polygon': []
    ] as Map<String, List<Map<String, Object>>>

    data.each { row ->
      String type = row['__sf_type']?.toString()?.toUpperCase()
      if (type == null) return
      Map<String, Object> rowMap = new LinkedHashMap<>(row.toMap())
      switch (type) {
        case 'POINT':
        case 'MULTIPOINT':
          buckets['point'] << rowMap
          break
        case 'LINESTRING':
        case 'MULTILINESTRING':
          buckets['line'] << rowMap
          break
        case 'POLYGON':
        case 'MULTIPOLYGON':
          buckets['polygon'] << rowMap
          break
        default:
          buckets['line'] << rowMap
      }
    }

    if (!buckets['polygon'].isEmpty()) {
      GeomPolygon polygon = new GeomPolygon([
          fill: fill,
          color: color,
          size: size,
          linetype: linetype,
          alpha: alpha
      ])
      polygon.render(group, Matrix.builder().mapList(buckets['polygon']).build(), resolvedAes, scales, coord)
    }

    if (!buckets['line'].isEmpty()) {
      GeomPath path = new GeomPath([
          color: color,
          size: size,
          linetype: linetype,
          alpha: alpha
      ])
      path.render(group, Matrix.builder().mapList(buckets['line']).build(), resolvedAes, scales, coord)
    }

    if (!buckets['point'].isEmpty()) {
      GeomPoint point = new GeomPoint([
          color: color,
          fill: fill,
          size: size,
          alpha: alpha,
          shape: shape
      ])
      point.render(group, Matrix.builder().mapList(buckets['point']).build(), resolvedAes, scales, coord)
    }
  }

  private static Aes resolveAes(Aes aes) {
    Aes base = new Aes([x: 'x', y: 'y', group: '__sf_group'])
    return aes != null ? aes.merge(base) : base
  }
}
