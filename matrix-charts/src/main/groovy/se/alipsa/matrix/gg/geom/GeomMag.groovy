package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.G
import se.alipsa.matrix.charts.util.ColorUtil
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.aes.Identity
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleUtils

/**
 * Magnitude geometry for vector field visualization.
 * Renders vector magnitudes at specified (x,y) locations, typically using
 * size and/or color to encode the magnitude value.
 *
 * Common use cases include:
 * - Physics: Electric fields, magnetic fields, force fields
 * - Fluid dynamics: Velocity fields, pressure gradients
 * - Meteorology: Wind speed visualization
 * - Engineering: Stress fields, displacement fields
 */
@CompileStatic
class GeomMag extends Geom {

  /** Default point color */
  String color = 'steelblue'

  /** Default point fill */
  String fill = 'steelblue'

  /** Default base size (radius) */
  BigDecimal size = 3

  /** Default point shape */
  String shape = 'circle'

  /** Default alpha (transparency) */
  BigDecimal alpha = 1.0

  /** Whether to automatically scale size/color to magnitude (default: true) */
  boolean scaleToMagnitude = true

  /** Size multiplier for magnitude scaling */
  BigDecimal sizeMultiplier = 1.0

  GeomMag() {
    requiredAes = ['x', 'y']
    defaultAes = [color: 'steelblue', size: 3, alpha: 1.0] as Map<String, Object>
  }

  GeomMag(Map params) {
    this()
    this.color = ColorUtil.normalizeColor((params.color ?: params.colour) as String) ?: this.color
    this.fill = params.fill ? ColorUtil.normalizeColor(params.fill as String) : this.fill
    if (params.size != null) this.size = params.size as BigDecimal
    this.shape = params.shape as String ?: this.shape
    if (params.alpha != null) this.alpha = params.alpha as BigDecimal
    if (params.scaleToMagnitude != null) this.scaleToMagnitude = params.scaleToMagnitude as boolean
    if (params.sizeMultiplier != null) this.sizeMultiplier = params.sizeMultiplier as BigDecimal
    this.params = params
  }

  @Override
  void render(G group, Matrix data, Aes aes, Map<String, Scale> scales, Coord coord) {
    if (data == null || data.rowCount() == 0) return

    String xCol = aes.xColName
    String yCol = aes.yColName
    String colorCol = aes.colorColName
    String sizeCol = aes.size instanceof String ? aes.size as String : null
    String shapeCol = aes.shape instanceof String ? aes.shape as String : null
    String alphaCol = aes.alpha instanceof String ? aes.alpha as String : null

    // Find magnitude column - try 'magnitude' or 'mag' from aesthetics or params
    String magCol = findMagnitudeColumn(aes, data)

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("GeomMag requires x and y aesthetics")
    }

    Scale xScale = scales['x']
    Scale yScale = scales['y']
    Scale colorScale = scales['color']
    Scale sizeScale = scales['size']
    Scale shapeScale = scales['shape']
    Scale alphaScale = scales['alpha']

    // Get min/max magnitude for scaling if needed
    BigDecimal minMag = null
    BigDecimal maxMag = null
    if (scaleToMagnitude && magCol != null) {
      def magValues = data.rows().collect { row ->
        ScaleUtils.coerceToNumber(row[magCol])
      }.findAll { it != null }

      if (!magValues.isEmpty()) {
        minMag = magValues.min() as BigDecimal
        maxMag = magValues.max() as BigDecimal
      }
    }

    // Render each point
    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal == null || yVal == null) return

      // Transform to pixel coordinates using scales
      def xTransformed = xScale?.transform(xVal)
      def yTransformed = yScale?.transform(yVal)

      // Skip if scale couldn't transform the value
      if (xTransformed == null || yTransformed == null) return

      BigDecimal xPx = xTransformed as BigDecimal
      BigDecimal yPx = yTransformed as BigDecimal

      // Get magnitude value
      BigDecimal magValue = null
      if (magCol != null && row[magCol] != null) {
        magValue = ScaleUtils.coerceToNumber(row[magCol])
        // Clamp negative magnitudes to zero
        if (magValue != null && magValue < 0) {
          magValue = 0.0
        }
      }

      // Determine size
      BigDecimal pointSize = this.size
      if (scaleToMagnitude && magValue != null && minMag != null && maxMag != null && maxMag > minMag) {
        // Auto-scale size based on magnitude
        BigDecimal normalized = (magValue - minMag) / (maxMag - minMag)
        pointSize = this.size * (1 + normalized * sizeMultiplier * 3)
      } else if (sizeCol && row[sizeCol] != null) {
        // Explicit size mapping overrides auto-scaling
        pointSize = GeomUtils.extractPointSize(this.size, aes, sizeCol, row.toMap(), sizeScale)
      } else if (aes.size instanceof Identity) {
        pointSize = (aes.size as Identity).value as BigDecimal
      }

      // Determine color
      String pointColor = this.color
      if (colorCol && row[colorCol] != null) {
        if (colorScale) {
          pointColor = colorScale.transform(row[colorCol])?.toString() ?: this.color
        } else {
          pointColor = GeomUtils.getDefaultColor(row[colorCol])
        }
      } else if (aes.color instanceof Identity) {
        pointColor = (aes.color as Identity).value.toString()
      } else if (scaleToMagnitude && magValue != null && minMag != null && maxMag != null && maxMag > minMag && !colorCol) {
        // Auto-scale color intensity based on magnitude (blue to red gradient)
        BigDecimal normalized = (magValue - minMag) / (maxMag - minMag)
        pointColor = interpolateColor('steelblue', 'orangered', normalized)
      }
      pointColor = ColorUtil.normalizeColor(pointColor) ?: pointColor

      // Determine shape
      String pointShape = this.shape
      if (shapeCol && row[shapeCol] != null) {
        pointShape = shapeScale?.transform(row[shapeCol])?.toString() ?: row[shapeCol].toString()
      } else if (aes.shape instanceof Identity) {
        pointShape = (aes.shape as Identity).value.toString()
      }

      // Determine alpha
      BigDecimal pointAlpha = GeomUtils.extractPointAlpha(this.alpha, aes, alphaCol, row.toMap(), alphaScale)

      // Draw the point
      GeomUtils.drawPoint(group, xPx, yPx, pointSize, pointColor, pointShape, pointAlpha)
    }
  }

  /**
   * Find the magnitude column from aesthetics or data columns.
   * Checks for 'magnitude', 'mag', or params mapping.
   */
  private String findMagnitudeColumn(Aes aes, Matrix data) {
    // Check if explicitly mapped via params
    if (params?.magnitude instanceof String) {
      return params.magnitude as String
    }
    if (params?.mag instanceof String) {
      return params.mag as String
    }

    // Check data columns for 'magnitude' or 'mag'
    def colNames = data.columnNames()
    if ('magnitude' in colNames) {
      return 'magnitude'
    }
    if ('mag' in colNames) {
      return 'mag'
    }

    // No magnitude column found
    return null
  }

  /**
   * Simple linear interpolation between two colors.
   * For production use, consider using a proper color space (HSV, Lab).
   */
  private static String interpolateColor(String color1, String color2, BigDecimal t) {
    // Simple RGB interpolation (not perceptually uniform, but adequate for basic use)
    // Normalize t to [0, 1]
    BigDecimal normalized = t.max(0.0).min(1.0)

    // Map color names to approximate RGB
    Map<String, List<Integer>> colorMap = [
        'steelblue': [70, 130, 180],
        'orangered': [255, 69, 0],
        'blue': [0, 0, 255],
        'red': [255, 0, 0]
    ]

    List<Integer> rgb1 = colorMap[color1.toLowerCase()] ?: [0, 0, 255]
    List<Integer> rgb2 = colorMap[color2.toLowerCase()] ?: [255, 0, 0]

    int r = (rgb1[0] + (rgb2[0] - rgb1[0]) * normalized) as int
    int g = (rgb1[1] + (rgb2[1] - rgb1[1]) * normalized) as int
    int b = (rgb1[2] + (rgb2[2] - rgb1[2]) * normalized) as int

    return String.format('#%02X%02X%02X', r, g, b)
  }
}
