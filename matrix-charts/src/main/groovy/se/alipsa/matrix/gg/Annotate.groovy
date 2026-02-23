package se.alipsa.matrix.gg

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.geom.*
import se.alipsa.matrix.gg.layer.Layer
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.layer.PositionType

/**
 * Annotation layer for adding fixed elements (text, shapes, lines) to plots.
 * Unlike regular geoms, annotations don't use data from the main dataset -
 * they use fixed coordinates and values specified as parameters.
 *
 * Usage:
 * - annotate('text', x: 5, y: 10, label: 'Important point')
 * - annotate('rect', xmin: 0, xmax: 5, ymin: 0, ymax: 10, alpha: 0.3)
 * - annotate('segment', x: 0, xend: 10, y: 5, yend: 5, color: 'red')
 * - annotate('point', x: 5, y: 5, size: 10, color: 'blue')
 * - annotate('label', x: 5, y: 10, label: 'Label text')
 */
@CompileStatic
class Annotate {

  /** The geom type for this annotation */
  String geomType

  /** The geom instance */
  Geom geom

  /** The annotation data (single or multiple rows) */
  Matrix data

  /** The aesthetics for this annotation */
  Aes aes

  /** Parameters for the geom */
  Map params = [:]

  /**
   * Create an annotation layer.
   *
   * @param geomType The type of geom: 'text', 'rect', 'segment', 'point', 'label', 'pointrange', 'linerange', 'crossbar'
   * @param params Fixed aesthetic parameters (x, y, xend, yend, xmin, xmax, ymin, ymax, label, color, fill, alpha, size, etc.)
   */
  Annotate(String geomType, Map params) {
    this.geomType = geomType
    this.params = params

    // Create the geom
    this.geom = createGeom(geomType, params)

    // Create data and aes from the params
    createDataAndAes(params)
  }

  private Geom createGeom(String type, Map params) {
    // Extract style parameters (not coordinate parameters)
    Map styleParams = extractStyleParams(params)

    switch (type.toLowerCase()) {
      case 'text':
        return new GeomText(styleParams)
      case 'label':
        return new GeomLabel(styleParams)
      case 'rect':
        return new GeomRect(styleParams)
      case 'segment':
        return new GeomSegment(styleParams)
      case 'point':
        return new GeomPoint(styleParams)
      case 'line':
        return new GeomLine(styleParams)
      case 'hline':
        return new GeomHline(styleParams)
      case 'vline':
        return new GeomVline(styleParams)
      case 'pointrange':
        return new GeomPointrange(styleParams)
      case 'linerange':
        return new GeomLinerange(styleParams)
      case 'crossbar':
        return new GeomCrossbar(styleParams)
      case 'ribbon':
        return new GeomRibbon(styleParams)
      case 'tile':
        return new GeomTile(styleParams)
      case 'path':
        return new GeomPath(styleParams)
      case 'step':
        return new GeomStep(styleParams)
      default:
        throw new IllegalArgumentException("Unknown annotation geom type: $type. " +
            "Supported types: text, label, rect, segment, point, line, hline, vline, " +
            "pointrange, linerange, crossbar, ribbon, tile, path, step")
    }
  }

  private Map extractStyleParams(Map params) {
    // Style parameters that should be passed to the geom constructor
    def styleKeys = ['color', 'colour', 'fill', 'alpha', 'size', 'linewidth',
                     'linetype', 'fontface', 'family', 'hjust', 'vjust', 'angle',
                     'nudge_x', 'nudge_y', 'width', 'height', 'direction'] as Set
    Map styleParams = [:]
    params.each { key, value ->
      if (styleKeys.contains(key)) {
        styleParams[key] = value
      }
    }
    return styleParams
  }

  private void createDataAndAes(Map params) {
    // Determine which columns we need based on geom type
    List<String> columns = []
    List<Object> values = []
    Map<String, Object> aesMap = [:]

    // Handle x coordinate (scalar or list)
    if (params.containsKey('x')) {
      def xVal = params.x
      if (xVal instanceof List) {
        columns << 'x'
        values << xVal
      } else {
        columns << 'x'
        values << [xVal]
      }
      aesMap['x'] = 'x'
    }

    // Handle y coordinate (scalar or list)
    if (params.containsKey('y')) {
      def yVal = params.y
      if (yVal instanceof List) {
        columns << 'y'
        values << yVal
      } else {
        columns << 'y'
        values << [yVal]
      }
      aesMap['y'] = 'y'
    }

    // Handle xend for segments
    if (params.containsKey('xend')) {
      def val = params.xend
      columns << 'xend'
      values << (val instanceof List ? val : [val])
      aesMap['xend'] = 'xend'
    }

    // Handle yend for segments
    if (params.containsKey('yend')) {
      def val = params.yend
      columns << 'yend'
      values << (val instanceof List ? val : [val])
      aesMap['yend'] = 'yend'
    }

    // Handle xmin for rects
    if (params.containsKey('xmin')) {
      def val = params.xmin
      columns << 'xmin'
      values << (val instanceof List ? val : [val])
      aesMap['xmin'] = 'xmin'
    }

    // Handle xmax for rects
    if (params.containsKey('xmax')) {
      def val = params.xmax
      columns << 'xmax'
      values << (val instanceof List ? val : [val])
      aesMap['xmax'] = 'xmax'
    }

    // Handle ymin for rects/ranges
    if (params.containsKey('ymin')) {
      def val = params.ymin
      columns << 'ymin'
      values << (val instanceof List ? val : [val])
      aesMap['ymin'] = 'ymin'
    }

    // Handle ymax for rects/ranges
    if (params.containsKey('ymax')) {
      def val = params.ymax
      columns << 'ymax'
      values << (val instanceof List ? val : [val])
      aesMap['ymax'] = 'ymax'
    }

    // Handle label for text/label geoms
    if (params.containsKey('label')) {
      def val = params.label
      columns << 'label'
      values << (val instanceof List ? val : [val])
      aesMap['label'] = 'label'
    }

    // Handle yintercept for hline
    if (params.containsKey('yintercept')) {
      def val = params.yintercept
      columns << 'yintercept'
      values << (val instanceof List ? val : [val])
      aesMap['yintercept'] = 'yintercept'
      // For hline, map to y
      if (!aesMap.containsKey('y')) {
        aesMap['y'] = 'yintercept'
      }
    }

    // Handle xintercept for vline
    if (params.containsKey('xintercept')) {
      def val = params.xintercept
      columns << 'xintercept'
      values << (val instanceof List ? val : [val])
      aesMap['xintercept'] = 'xintercept'
      // For vline, map to x
      if (!aesMap.containsKey('x')) {
        aesMap['x'] = 'xintercept'
      }
    }

    // Handle slope and intercept for abline
    if (params.containsKey('slope')) {
      def val = params.slope
      columns << 'slope'
      values << (val instanceof List ? val : [val])
      aesMap['slope'] = 'slope'
    }
    if (params.containsKey('intercept')) {
      def val = params.intercept
      columns << 'intercept'
      values << (val instanceof List ? val : [val])
      aesMap['intercept'] = 'intercept'
    }

    if (columns.isEmpty()) {
      throw new IllegalArgumentException("Annotation requires at least one coordinate parameter (x, y, xmin, xmax, ymin, ymax, etc.)")
    }

    // Find the maximum length of any list value to handle vectorized annotations
    int nRows = 1
    values.each { val ->
      if (val instanceof List) {
        nRows = nRows.max((val as List).size()) as int
      }
    }

    // Expand scalar values to match the number of rows
    List<List<Object>> expandedValues = values.collect { val ->
      if (val instanceof List) {
        List list = val as List
        if (list.size() == nRows) {
          return list
        } else if (list.size() == 1) {
          // Repeat the single value
          return (0..<nRows).collect { list[0] }
        } else {
          throw new IllegalArgumentException("Inconsistent annotation vector lengths")
        }
      } else {
        return (0..<nRows).collect { val }
      }
    }

    // Build data matrix
    List<List<Object>> rows = (0..<nRows).collect { rowIdx ->
      expandedValues.collect { it[rowIdx] }
    }

    this.data = Matrix.builder()
        .columnNames(columns)
        .rows(rows)
        .build()

    this.aes = new Aes(aesMap)
  }

  /**
   * Convert this annotation to a Layer for rendering.
   */
  Layer toLayer() {
    Layer layer = new Layer(
        geom: this.geom,
        data: this.data,
        aes: this.aes,
        stat: StatType.IDENTITY,
        position: PositionType.IDENTITY,
        params: this.params,
        inheritAes: false  // Annotations use their own data/aes
    )
    return layer
  }
}
