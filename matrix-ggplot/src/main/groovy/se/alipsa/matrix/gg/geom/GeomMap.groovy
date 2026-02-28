package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.gg.layer.StatType

/**
 * Polygon map geometry that joins data to map polygons via map_id.
 */
@CompileStatic
class GeomMap extends Geom {

  private static final Logger log = Logger.getLogger(GeomMap)

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

}
