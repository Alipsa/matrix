/*
 * Example demonstrating CSS class and ID attributes for custom chart styling.
 *
 * This example shows how to enable CSS attributes on SVG elements, which allows:
 * - Custom styling via external CSS stylesheets
 * - JavaScript interactivity through element selection
 * - Unique element identification for testing and automation
 *
 * The equivalent R ggplot2 doesn't have built-in CSS attribute support,
 * but the ggiraph package provides similar interactivity features.
 */
@Grab('se.alipsa.matrix:matrix-core:3.7.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-charts:0.5.0-SNAPSHOT')
@Grab('se.alipsa.matrix:matrix-datasets:2.1.2-SNAPSHOT')

import groovy.transform.SourceURI
import static se.alipsa.matrix.gg.GgPlot.*
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix

@SourceURI
URI sourceUri

File subProjectDir = new File(sourceUri).parentFile.parentFile.parentFile
File targetDir = new File(subProjectDir, 'build/examples/gg')
targetDir.mkdirs()

// Example 1: Basic CSS attributes with default prefix
println("\n=== Example 1: Basic CSS attributes ===")

def iris = Dataset.iris()
def chart1 = ggplot(iris, aes(x: 'Sepal.Length', y: 'Sepal.Width', color: 'Species')) +
    css_attributes(enabled: true) +  // Enable CSS classes and IDs
    geom_point() +
    labs(title: 'Iris Dataset with CSS Attributes')

Svg svg1 = chart1.render()
File file1 = new File(targetDir, 'CssAttributesBasic.svg')
write(svg1, file1)
println("Generated: ${file1.absolutePath}")
println("  - Points have class 'gg-point' and IDs like 'gg-layer-0-point-0'")
println("  - Can style all points with CSS: .gg-point { fill: red; }")


// Example 2: Custom chart ID prefix for multi-chart pages
println("\n=== Example 2: Custom chart ID prefix ===")

def chart2 = ggplot(iris, aes(x: 'Petal.Length', y: 'Petal.Width', color: 'Species')) +
    css_attributes(enabled: true, chartIdPrefix: 'iris-petals') +
    geom_point() +
    labs(title: 'Iris Petals with Custom Prefix')

Svg svg2 = chart2.render()
File file2 = new File(targetDir, 'CssAttributesCustomPrefix.svg')
write(svg2, file2)
println("Generated: ${file2.absolutePath}")
println("  - Points have IDs like 'iris-petals-layer-0-point-0'")
println("  - Useful when multiple charts are on the same HTML page")


// Example 3: Faceted chart with panel coordinates in IDs
println("\n=== Example 3: Faceted chart with CSS attributes ===")

def chart3 = ggplot(iris, aes(x: 'Sepal.Length', y: 'Sepal.Width')) +
    css_attributes(enabled: true) +
    geom_point() +
    facet_wrap('Species') +
    labs(title: 'Faceted Iris with CSS Attributes')

Svg svg3 = chart3.render()
File file3 = new File(targetDir, 'CssAttributesFaceted.svg')
write(svg3, file3)
println("Generated: ${file3.absolutePath}")
println("  - Points have IDs like 'gg-panel-0-0-layer-0-point-0'")
println("  - Panel coordinates ensure unique IDs across facets")


// Example 4: Multiple layers with different geoms
println("\n=== Example 4: Multiple layers ===")

// Create sample data
def data = Matrix.builder()
    .columnNames('x', 'y', 'group')
    .rows([
        [1, 2, 'A'], [2, 3, 'A'], [3, 5, 'A'],
        [1, 3, 'B'], [2, 4, 'B'], [3, 4, 'B']
    ])
    .types(Integer, Integer, String)
    .build()

def chart4 = ggplot(data, aes(x: 'x', y: 'y', color: 'group')) +
    css_attributes(enabled: true) +
    geom_point() +      // Layer 0
    geom_line() +       // Layer 1
    labs(title: 'Multiple Layers with CSS Attributes')

Svg svg4 = chart4.render()
File file4 = new File(targetDir, 'CssAttributesMultipleLayers.svg')
write(svg4, file4)
println("Generated: ${file4.absolutePath}")
println("  - Points have IDs like 'gg-layer-0-point-0'")
println("  - Lines have IDs like 'gg-layer-1-line-0'")
println("  - Layer index distinguishes between geom types")


// Example 5: Classes only (no IDs) for smaller file size
println("\n=== Example 5: Classes only (no IDs) ===")

def chart5 = ggplot(iris, aes(x: 'Sepal.Length', y: 'Sepal.Width', color: 'Species')) +
    css_attributes(enabled: true, includeClasses: true, includeIds: false) +
    geom_point() +
    labs(title: 'CSS Classes Only')

Svg svg5 = chart5.render()
File file5 = new File(targetDir, 'CssAttributesClassesOnly.svg')
write(svg5, file5)
println("Generated: ${file5.absolutePath}")
println("  - Points have class 'gg-point' but no IDs")
println("  - Smaller file size, still allows CSS styling")


// Example 6: Demonstrating renderer CSS classes
println("\n=== Example 6: Renderer CSS classes (always present) ===")

def chart6 = ggplot(iris, aes(x: 'Sepal.Length', y: 'Sepal.Width')) +
    geom_point() +
    theme_minimal() +  // Shows grid lines
    labs(title: 'Renderer CSS Classes',
         x: 'Sepal Length',
         y: 'Sepal Width')

Svg svg6 = chart6.render()
File file6 = new File(targetDir, 'CssAttributesRenderers.svg')
write(svg6, file6)
println("Generated: ${file6.absolutePath}")
println("  - Axes have classes: gg-axis-line, gg-axis-tick, gg-axis-label")
println("  - Grid has classes: gg-grid-major, gg-grid-minor")
println("  - These are always present regardless of css_attributes() setting")


println("\n=== Summary ===")
println("CSS attributes enable:")
println("  ✓ Custom styling via external CSS")
println("  ✓ JavaScript element selection and manipulation")
println("  ✓ Unique element identification for testing")
println("  ✓ Improved accessibility")
println("\nGenerated SVG files can be opened in a browser and styled with CSS.")
println("For example, add this CSS to change all points to red:")
println("  <style>.gg-point { fill: red !important; }</style>")
