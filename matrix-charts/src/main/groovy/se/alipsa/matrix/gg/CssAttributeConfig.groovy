package se.alipsa.matrix.gg

import groovy.transform.CompileStatic

/**
 * Configuration for CSS class and ID attributes on SVG elements.
 * <p>
 * This configuration controls whether CSS classes, IDs, and data attributes
 * are added to individual SVG elements during chart rendering. When enabled,
 * these attributes facilitate custom styling via external stylesheets, JavaScript
 * interactivity, and improved accessibility.
 * <p>
 * <strong>Usage:</strong>
 * <pre>
 * // Enable CSS attributes with default settings
 * chart.cssAttributes = new CssAttributeConfig(enabled: true)
 *
 * // Enable with custom chart prefix for multi-chart pages
 * chart.cssAttributes = new CssAttributeConfig(
 *   enabled: true,
 *   chartIdPrefix: 'iris'
 * )
 *
 * // Using fluent API
 * def chart = ggplot(data, aes(x: 'x', y: 'y')) +
 *   css_attributes(enabled: true, chartIdPrefix: 'my-chart') +
 *   geom_point()
 * </pre>
 *
 * <strong>CSS Classes:</strong>
 * CSS classes are applied to elements based on their geom type (e.g., {@code gg-point},
 * {@code gg-bar}, {@code gg-line}). Group-level classes like {@code geompoint} remain
 * on container {@code <g>} elements for backward compatibility.
 *
 * <strong>Element IDs:</strong>
 * IDs follow a hierarchical naming scheme:
 * <ul>
 *   <li>Single panel: {@code gg-layer-{layer}-{geom}-{element}}</li>
 *   <li>Faceted: {@code gg-panel-{row}-{col}-layer-{layer}-{geom}-{element}}</li>
 *   <li>Custom prefix: Replace {@code gg} with normalized {@code chartIdPrefix}</li>
 * </ul>
 *
 * <strong>Data Attributes (Phase 2):</strong>
 * When {@code includeDataAttributes} is enabled, elements receive {@code data-x},
 * {@code data-y}, {@code data-row}, {@code data-panel}, and {@code data-layer} attributes
 * for interactivity.
 *
 * @see <a href="https://github.com/Alipsa/matrix/blob/main/matrix-charts/ggCssAttributes.md">Implementation Plan</a>
 */
@CompileStatic
class CssAttributeConfig {

  /**
   * Master toggle for CSS attributes.
   * <p>
   * When {@code false}, no CSS classes, IDs, or data attributes are added to SVG elements,
   * avoiding any performance impact. When {@code true}, CSS attributes are added according
   * to the other configuration options.
   * <p>
   * Default: {@code false} (to avoid performance impact on existing code)
   */
  boolean enabled = false

  /**
   * Whether to add CSS classes to SVG elements.
   * <p>
   * When {@code true}, elements receive CSS classes like {@code gg-point}, {@code gg-bar},
   * etc., enabling styling via external stylesheets. Group-level classes remain on container
   * {@code <g>} elements for backward compatibility.
   * <p>
   * Default: {@code true} (when {@code enabled} is {@code true})
   */
  boolean includeClasses = true

  /**
   * Whether to add unique IDs to SVG elements.
   * <p>
   * When {@code true}, elements receive unique IDs following the naming scheme:
   * {@code {prefix}-layer-{layer}-{geom}-{element}} or
   * {@code {prefix}-panel-{row}-{col}-layer-{layer}-{geom}-{element}} for faceted charts.
   * <p>
   * Default: {@code true} (when {@code enabled} is {@code true})
   */
  boolean includeIds = true

  /**
   * Whether to add data-* attributes to SVG elements.
   * <p>
   * When {@code true}, elements receive data attributes like {@code data-x}, {@code data-y},
   * {@code data-row}, {@code data-panel}, and {@code data-layer} for interactivity.
   * This is a Phase 2 enhancement and defaults to {@code false}.
   * <p>
   * Default: {@code false} (Phase 2 feature, not yet fully implemented)
   */
  boolean includeDataAttributes = false

  /**
   * Optional prefix for multi-chart pages.
   * <p>
   * This prefix is normalized (lowercase, whitespace replaced with {@code -}, invalid
   * characters stripped) and used in element IDs. If {@code null}, empty after normalization,
   * or starts with a digit, the {@code idPrefix} fallback is used.
   * <p>
   * Examples:
   * <ul>
   *   <li>{@code "My Chart"} → {@code "my-chart"}</li>
   *   <li>{@code "123data"} → {@code "gg"} (starts with digit, use fallback)</li>
   *   <li>{@code "!@#"} → {@code "gg"} (empty after normalization, use fallback)</li>
   * </ul>
   * <p>
   * Default: {@code null} (uses {@code idPrefix} as fallback)
   */
  String chartIdPrefix = null

  /**
   * Fallback prefix when {@code chartIdPrefix} is null or invalid.
   * <p>
   * This prefix is used when {@code chartIdPrefix} is {@code null}, empty after normalization,
   * or starts with a digit after normalization. The default is {@code 'gg'}.
   * <p>
   * Default: {@code 'gg'}
   */
  String idPrefix = 'gg'

  /**
   * Create a new CSS attribute configuration with default values.
   */
  CssAttributeConfig() {
    // Use default values
  }

  /**
   * Create a new CSS attribute configuration with specified values.
   *
   * @param params a map of property values
   */
  CssAttributeConfig(Map<String, ?> params) {
    if (params.containsKey('enabled')) {
      this.enabled = params.enabled as boolean
    }
    if (params.containsKey('includeClasses')) {
      this.includeClasses = params.includeClasses as boolean
    }
    if (params.containsKey('includeIds')) {
      this.includeIds = params.includeIds as boolean
    }
    if (params.containsKey('includeDataAttributes')) {
      this.includeDataAttributes = params.includeDataAttributes as boolean
    }
    if (params.containsKey('chartIdPrefix')) {
      this.chartIdPrefix = params.chartIdPrefix as String
    }
    if (params.containsKey('idPrefix')) {
      this.idPrefix = params.idPrefix as String
    }
  }
}
