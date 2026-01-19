package se.alipsa.matrix.gg.render

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import se.alipsa.matrix.gg.CssAttributeConfig
import se.alipsa.matrix.gg.Guide
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.scale.ScaleColorGradient
import se.alipsa.matrix.gg.scale.ScaleColorViridisC
import se.alipsa.matrix.gg.scale.ScaleContinuous
import se.alipsa.matrix.gg.scale.ScaleDiscrete

import java.text.DecimalFormat

/**
 * Context class providing shared utility methods for rendering components.
 * This allows renderers to access common functionality without coupling.
 * <p>
 * The RenderContext also carries CSS attribute configuration and panel/layer
 * information through the rendering pipeline to support CSS class and ID generation.
 */
@CompileStatic
class RenderContext {

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat('#.##########')

  /** CSS attribute configuration (may be null) */
  CssAttributeConfig cssConfig

  /** Panel row for faceted charts (null for single-panel charts) */
  Integer panelRow

  /** Panel column for faceted charts (null for single-panel charts) */
  Integer panelCol

  /** Current layer index (0-based) */
  int layerIndex = 0

  /**
   * Parse guide type from various guide specifications.
   */
  String parseGuideType(Object spec) {
    if (spec == null) {
      return null
    }
    if (spec instanceof Guide) {
      return normalizeGuideType((spec as Guide).type)
    }
    if (spec instanceof CharSequence) {
      return normalizeGuideType(spec.toString())
    }
    if (spec instanceof Boolean) {
      return (spec as Boolean) ? null : 'none'
    }
    return normalizeGuideType(spec.toString())
  }

  /**
   * Normalize guide type string.
   */
  String normalizeGuideType(String type) {
    if (type == null) return null
    String normalized = type.toLowerCase().trim()
    // Handle common variants
    if (normalized == 'colourbar' || normalized == 'colorbar') return 'colorbar'
    if (normalized == 'legend') return 'legend'
    if (normalized == 'none' || normalized == 'false') return 'none'
    return normalized
  }

  /**
   * Extract parameters from a guide specification.
   */
  Map extractGuideParams(Object guideSpec) {
    if (guideSpec == null) return [:]
    if (guideSpec instanceof Guide) {
      return (guideSpec as Guide).params ?: [:]
    }
    if (guideSpec instanceof Map) {
      return guideSpec as Map
    }
    return [:]
  }

  /**
   * Normalize a scale-transformed value into 0..1 based on its scale range.
   */
  BigDecimal normalizeFromScale(Scale scale, Object value) {
    if (scale == null) return null
    def transformed = scale.transform(value)
    if (!(transformed instanceof Number)) return null
    List<? extends Number> range = null
    if (scale instanceof ScaleContinuous) {
      range = (scale as ScaleContinuous).range
    } else if (scale instanceof ScaleDiscrete) {
      range = (scale as ScaleDiscrete).range
    }
    if (range == null || range.size() < 2) return null
    BigDecimal rMin = range[0] as BigDecimal
    BigDecimal rMax = range[1] as BigDecimal
    if (rMin == rMax) return 0.0
    return ((transformed as BigDecimal) - rMin) / (rMax - rMin)
  }

  /**
   * Check if a scale's range is reversed.
   */
  boolean isRangeReversed(Scale scale) {
    if (scale instanceof ScaleContinuous) {
      List<? extends Number> range = (scale as ScaleContinuous).range
      if (range != null && range.size() >= 2) {
        return (range[0] as BigDecimal) > (range[1] as BigDecimal)
      }
    } else if (scale instanceof ScaleDiscrete) {
      List<? extends Number> range = (scale as ScaleDiscrete).range
      if (range != null && range.size() >= 2) {
        return (range[0] as BigDecimal) > (range[1] as BigDecimal)
      }
    }
    return false
  }

  /**
   * Check if a label should be skipped due to overlap.
   */
  boolean shouldSkipForOverlap(Number position, List<Number> renderedPositions, Number minSpacing) {
    if (renderedPositions.isEmpty()) return false
    BigDecimal pos = position as BigDecimal
    BigDecimal spacing = minSpacing as BigDecimal
    return renderedPositions.any { Number renderedPos ->
      (pos - (renderedPos as BigDecimal)).abs() < spacing
    }
  }

  /**
   * Calculate logarithmic tick positions and types.
   */
  @CompileStatic(TypeCheckingMode.SKIP)
  List<AxisRenderer.LogTickInfo> calculateLogTicks(Scale scale, Map guideParams, BigDecimal longMult,
                                                    BigDecimal midMult, BigDecimal shortMult) {
    List<AxisRenderer.LogTickInfo> ticks = []

    // Additional parameters
    BigDecimal prescaleBase = (guideParams['prescale.base'] ?: guideParams.prescaleBase) as BigDecimal
    BigDecimal negativeSmall = ((guideParams['negative.small'] ?: guideParams.negativeSmall) ?: 0.1) as BigDecimal
    boolean expanded = (guideParams.expanded != null) ? (guideParams.expanded as boolean) : true

    // Get domain from scale
    List domain = expanded ? scale.computedDomain : scale.limits
    if (domain == null || domain.size() < 2) return ticks

    BigDecimal minVal = domain[0] as BigDecimal
    BigDecimal maxVal = domain[1] as BigDecimal

    // Calculate log range
    int minExp, maxExp
    if (prescaleBase != null) {
      minExp = minVal.floor().intValue()
      maxExp = maxVal.ceil().intValue()
    } else {
      // Validate that values are positive before computing logarithms
      if (minVal <= 0 || maxVal <= 0) {
        // Cannot compute logarithmic ticks for non-positive values
        return ticks
      }
      minExp = minVal.log10().floor().intValue()
      maxExp = maxVal.log10().ceil().intValue()
    }

    // Generate ticks for each decade
    for (int exp = minExp; exp <= maxExp; exp++) {
      // Major tick at 10^exp
      BigDecimal majorValue = prescaleBase != null ? exp : (10 ** exp) as BigDecimal
      BigDecimal transformValue = prescaleBase != null ? (prescaleBase ** exp) as BigDecimal : majorValue

      // Check if value is within negativeSmall threshold
      if (transformValue.abs() >= negativeSmall) {
        ticks << new AxisRenderer.LogTickInfo(transformValue, majorValue, 'long', longMult)
      }

      // Minor ticks at 2,3,4,5,6,7,8,9 × 10^exp
      for (int mult = 2; mult <= 9; mult++) {
        BigDecimal minorValue = prescaleBase != null ? exp + (mult / 10.0) : (mult * (10 ** exp)) as BigDecimal
        BigDecimal minorTransformValue = prescaleBase != null ? (prescaleBase ** minorValue) as BigDecimal : minorValue

        // Skip if outside domain
        if (prescaleBase != null) {
          if (minorValue < minVal || minorValue > maxVal) continue
        } else {
          if (minorTransformValue < minVal || minorTransformValue > maxVal) continue
        }

        // Check if value is within negativeSmall threshold
        if (minorTransformValue.abs() < negativeSmall) continue

        // Determine tick type
        String tickType
        BigDecimal tickMult
        if (mult == 5) {
          tickType = 'mid'
          tickMult = midMult
        } else {
          tickType = 'short'
          tickMult = shortMult
        }

        ticks << new AxisRenderer.LogTickInfo(minorTransformValue, minorValue, tickType, tickMult)
      }
    }

    return ticks
  }

  /**
   * Format a number for display.
   */
  String formatNumber(Object value) {
    if (value == null) return ''
    if (value instanceof Number) {
      return DECIMAL_FORMAT.format(value)
    }
    return value.toString()
  }
}
