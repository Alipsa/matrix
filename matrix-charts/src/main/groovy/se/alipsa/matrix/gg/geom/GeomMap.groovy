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
 * Polygon map geometry that joins data to map polygons via map_id.
 */
@CompileStatic
class GeomMap extends Geom {

  /** Map polygon data */
  Matrix map

  /** Map id column name */
  String mapIdCol

  /** Map x/y column names */
  String xCol
  String yCol

  /** Group column name */
  String groupCol

  /** Fill color */
  String fill = 'gray'

  /** Border color */
  String color = 'black'

  /** Border width */
  Number size = 1

  /** Line type */
  String linetype = 'solid'

  /** Alpha transparency */
  Number alpha = 1.0

  GeomMap() {
    defaultStat = StatType.IDENTITY
    requiredAes = []
    defaultAes = [fill: 'gray', color: 'black', size: 1, linetype: 'solid'] as Map<String, Object>
  }

  GeomMap(Map params) {
    this()
    if (params.get('map') instanceof Matrix) {
      this.map = params.get('map') as Matrix
    }
    Object mapId = params.get('map_id_col') ?: params.get('mapIdCol')
    if (mapId != null) this.mapIdCol = mapId.toString()
    Object xVal = params.get('x')
    if (xVal != null) this.xCol = xVal.toString()
    Object yVal = params.get('y')
    if (yVal != null) this.yCol = yVal.toString()
    Object groupVal = params.get('group')
    if (groupVal != null) this.groupCol = groupVal.toString()
    Object fillVal = params.get('fill')
    if (fillVal != null) this.fill = ColorUtil.normalizeColor(fillVal as String)
    Object colorVal = params.get('color')
    if (colorVal != null) this.color = ColorUtil.normalizeColor(colorVal as String)
    Object colourVal = params.get('colour')
    if (colourVal != null) this.color = ColorUtil.normalizeColor(colourVal as String)
    if (params.get('size') != null) this.size = params.get('size') as Number
    if (params.get('linewidth') != null) this.size = params.get('linewidth') as Number
    Object linetypeVal = params.get('linetype')
    if (linetypeVal != null) this.linetype = linetypeVal as String
    if (params.get('alpha') != null) this.alpha = params.get('alpha') as Number
    this.params = params
  }

  /**
   * Render map polygons by joining data to map geometry.
   *
   * @param group SVG group for rendering
   * @param data layer data to join (may be null for map-only rendering)
   * @param aes aesthetic mappings (expects map_id for joins)
   * @param scales scale map for rendering
   * @param coord coordinate system
   */
  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (map == null) {
      throw new IllegalArgumentException("geom_map requires a 'map' Matrix")
    }

    String resolvedMapIdCol = resolveMapIdColumn()
    String resolvedXCol = resolveMapXCol()
    String resolvedYCol = resolveMapYCol()
    String resolvedGroupCol = resolveMapGroupCol(resolvedMapIdCol)

    Matrix merged = data != null ? mergeMapData(data, aes, resolvedMapIdCol) : map
    if (merged == null || merged.rowCount() == 0) return

    Aes resolvedAes = resolveAes(aes, resolvedXCol, resolvedYCol, resolvedGroupCol)
    GeomPolygon polygon = new GeomPolygon([
        fill: fill,
        color: color,
        size: size,
        linetype: linetype,
        alpha: alpha
    ])
    polygon.render(group, merged, resolvedAes, scales, coord)
  }

  private String resolveMapIdColumn() {
    if (mapIdCol != null) return mapIdCol
    List<String> columns = map.columnNames()
    if (columns.contains('region')) return 'region'
    if (columns.contains('id')) return 'id'
    if (columns.contains('map_id')) return 'map_id'
    throw new IllegalArgumentException("geom_map map data must include an id column (region, id, or map_id)")
  }

  private String resolveMapXCol() {
    if (xCol != null) return xCol
    List<String> columns = map.columnNames()
    if (columns.contains('x')) return 'x'
    if (columns.contains('long')) return 'long'
    throw new IllegalArgumentException("geom_map map data must include an x column (x or long)")
  }

  private String resolveMapYCol() {
    if (yCol != null) return yCol
    List<String> columns = map.columnNames()
    if (columns.contains('y')) return 'y'
    if (columns.contains('lat')) return 'lat'
    throw new IllegalArgumentException("geom_map map data must include a y column (y or lat)")
  }

  private String resolveMapGroupCol(String resolvedMapIdCol) {
    if (groupCol != null) return groupCol
    List<String> columns = map.columnNames()
    if (columns.contains('group')) return 'group'
    return resolvedMapIdCol
  }

  private Matrix mergeMapData(Matrix data, Aes aes, String resolvedMapIdCol) {
    String dataMapIdCol = resolveDataMapIdColumn(data, aes, resolvedMapIdCol)
    Map<Object, Map<String, Object>> dataById = new LinkedHashMap<>()
    data.each { row ->
      Object idVal = row[dataMapIdCol]
      if (idVal != null && !dataById.containsKey(idVal)) {
        dataById[idVal] = new LinkedHashMap<>(row.toMap())
      }
    }

    // Check for data IDs that don't exist in the map
    Set<Object> mapIds = map[resolvedMapIdCol].toSet()
    Set<Object> unmatchedDataIds = dataById.keySet() - mapIds
    if (unmatchedDataIds) {
      String preview = unmatchedDataIds.take(5).collect { it.toString() }.join(', ')
      String suffix = unmatchedDataIds.size() > 5 ? '...' : ''
      System.err.println("geom_map warning: ${unmatchedDataIds.size()} data ID(s) not found in map: ${preview}${suffix}")
    }

    List<Map<String, Object>> rows = []
    map.each { mapRow ->
      Map<String, Object> rowMap = new LinkedHashMap<>(mapRow.toMap())
      Object idVal = mapRow[resolvedMapIdCol]
      Map<String, Object> dataRow = dataById[idVal]
      if (dataRow != null) {
        rowMap.putAll(dataRow)
      }
      rows << rowMap
    }
    return Matrix.builder().mapList(rows).build()
  }

  private String resolveDataMapIdColumn(Matrix data, Aes aes, String resolvedMapIdCol) {
    String dataMapIdCol = params?.get('map_id')?.toString()
    if (dataMapIdCol == null && params?.get('mapId') != null) {
      dataMapIdCol = params.get('mapId').toString()
    }
    if (dataMapIdCol == null && aes?.mapIdColName) {
      dataMapIdCol = aes.mapIdColName
    }
    if (dataMapIdCol == null && data.columnNames().contains(resolvedMapIdCol)) {
      dataMapIdCol = resolvedMapIdCol
    }
    if (dataMapIdCol == null) {
      throw new IllegalArgumentException("geom_map requires map_id aesthetic or map_id parameter")
    }
    return dataMapIdCol
  }

  private static Aes resolveAes(Aes aes, String xCol, String yCol, String groupCol) {
    Aes resolved = aes != null ? aes.merge(new Aes()) : new Aes()
    resolved.x = xCol
    resolved.y = yCol
    resolved.group = groupCol
    return resolved
  }
}
