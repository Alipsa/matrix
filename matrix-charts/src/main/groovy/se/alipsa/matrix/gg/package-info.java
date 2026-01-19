/**
 * The se.alipsa.matrix.gg package is an implementation of grammar of graphics interpreted in the
 * same way that the R package ggplot2 does. It is close to 100% compatible with ggplot2 syntax
 * with the only exception being that named method calls in groovy uses : instead of = and that
 * String such as column names must be quoted.
 * <p>
 * Ggplot creates a GgChart. Ggchart.render() is then used to create a Svg model. This Svg model
 * can be written to a file or converted to png, swing panel of JavaFX node using the appropriate
 * class in the se.alipsa.matrix.chartexport package.
 *
 * <h2>CSS Attributes for SVG Styling</h2>
 * <p>
 * The gg package supports adding CSS classes and IDs to SVG elements for custom styling,
 * JavaScript interactivity, and improved accessibility. This feature is disabled by default
 * and can be enabled via {@link CssAttributeConfig}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Enable CSS attributes
 * def chart = ggplot(data, aes(x: 'x', y: 'y')) +
 *     css_attributes(enabled: true) +
 *     geom_point()
 *
 * // With custom prefix for multi-chart pages
 * def chart = ggplot(data, aes(x: 'x', y: 'y')) +
 *     css_attributes(enabled: true, chartIdPrefix: 'iris') +
 *     geom_point()
 * }</pre>
 *
 * <h3>CSS Class Reference</h3>
 * <p>
 * When CSS attributes are enabled, the following classes are applied to SVG elements:
 *
 * <h4>Geom Elements</h4>
 * <table border="1">
 *   <tr><th>Element</th><th>CSS Class</th><th>Description</th></tr>
 *   <tr><td>Point</td><td>gg-point</td><td>Scatter plot points</td></tr>
 *   <tr><td>Bar</td><td>gg-bar</td><td>Bar chart rectangles</td></tr>
 *   <tr><td>Line</td><td>gg-line</td><td>Line chart segments</td></tr>
 *   <tr><td>Area</td><td>gg-area</td><td>Area chart fills</td></tr>
 *   <tr><td>Histogram</td><td>gg-histogram</td><td>Histogram bars</td></tr>
 *   <tr><td>Boxplot</td><td>gg-boxplot-*</td><td>Box, whisker, median, outlier components</td></tr>
 *   <tr><td>Violin</td><td>gg-violin</td><td>Violin plot shapes</td></tr>
 *   <tr><td>Density</td><td>gg-density</td><td>Density curves</td></tr>
 *   <tr><td>Smooth</td><td>gg-smooth</td><td>Smoothed trend lines</td></tr>
 *   <tr><td>Text</td><td>gg-text</td><td>Text labels</td></tr>
 *   <tr><td>Label</td><td>gg-label</td><td>Positioned labels</td></tr>
 *   <tr><td>Errorbar</td><td>gg-errorbar</td><td>Error bars</td></tr>
 *   <tr><td>Ribbon</td><td>gg-ribbon</td><td>Confidence bands</td></tr>
 *   <tr><td>Segment</td><td>gg-segment</td><td>Line segments</td></tr>
 *   <tr><td>Tile</td><td>gg-tile</td><td>Heatmap tiles</td></tr>
 *   <tr><td>Contour</td><td>gg-contour</td><td>Contour lines</td></tr>
 *   <tr><td>Reference Line</td><td>gg-hline, gg-vline, gg-abline</td><td>Reference lines</td></tr>
 * </table>
 *
 * <h4>Renderer Components (Always Present)</h4>
 * <table border="1">
 *   <tr><th>Component</th><th>CSS Class</th><th>Description</th></tr>
 *   <tr><td>Axis line</td><td>gg-axis-line</td><td>Main axis lines</td></tr>
 *   <tr><td>Axis tick</td><td>gg-axis-tick</td><td>Tick marks</td></tr>
 *   <tr><td>Axis label</td><td>gg-axis-label</td><td>Tick labels</td></tr>
 *   <tr><td>Grid major</td><td>gg-grid-major</td><td>Major grid lines</td></tr>
 *   <tr><td>Grid minor</td><td>gg-grid-minor</td><td>Minor grid lines</td></tr>
 *   <tr><td>Legend key</td><td>gg-legend-key</td><td>Legend symbols</td></tr>
 *   <tr><td>Legend label</td><td>gg-legend-label</td><td>Legend text</td></tr>
 *   <tr><td>Legend title</td><td>gg-legend-title</td><td>Legend title</td></tr>
 *   <tr><td>Colorbar</td><td>gg-legend-colorbar</td><td>Continuous scale bar</td></tr>
 * </table>
 *
 * <h3>Element ID Format</h3>
 * <p>
 * When IDs are enabled, elements receive unique identifiers following these patterns:
 * <ul>
 *   <li>Single panel: {@code gg-layer-{layer}-{geom}-{element}}</li>
 *   <li>Faceted: {@code gg-panel-{row}-{col}-layer-{layer}-{geom}-{element}}</li>
 *   <li>Custom prefix: Replace {@code gg} with normalized {@code chartIdPrefix}</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <p>
 * See {@code matrix-charts/examples/gg/CssAttributesExample.groovy} for comprehensive usage examples.
 *
 * @see CssAttributeConfig
 * @see <a href="https://github.com/Alipsa/matrix/blob/main/matrix-charts/ggCssAttributes.md">CSS Attributes Implementation Plan</a>
 */
package se.alipsa.matrix.gg;