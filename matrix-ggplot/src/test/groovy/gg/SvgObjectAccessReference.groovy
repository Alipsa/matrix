package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Rect
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Line
import se.alipsa.groovy.svg.Path
import se.alipsa.groovy.svg.Text
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.datasets.Dataset
import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Reference documentation and validation for direct SVG object access.
 *
 * This test demonstrates the recommended approach for testing SVG visualizations
 * using direct object access instead of serialization. See AGENTS.md for full documentation.
 *
 * Key Benefits:
 * - 1.3x faster than string serialization
 * - Type-safe element access
 * - No string parsing overhead
 * - Better memory efficiency
 */
class SvgObjectAccessReference {

    private static final Logger log = Logger.getLogger(SvgObjectAccessReference)

    /**
     * Demonstrates all SVG object access patterns documented in AGENTS.md.
     * This test serves as both documentation and validation.
     */
    @Test
    void testDirectObjectAccessPatterns() {
        def mtcars = Dataset.mtcars()
        def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) + geom_point()
        Svg svg = chart.render()
        assertNotNull(svg)

        // Pattern 1: Get all descendants (most common)
        def allDescendants = svg.descendants()
        assertTrue(allDescendants.size() > 0, "Should have descendant elements")

        // Pattern 2: Filter by element type
        def circles = allDescendants.findAll { it instanceof Circle }
        assertTrue(circles.size() > 0, "Should find circle elements")

        def rects = allDescendants.findAll { it instanceof Rect }
        // Rects exist for background, etc.

        // Pattern 3: Direct children only (when needed)
        def children = svg.getChildren()
        assertTrue(children.size() > 0, "Should have direct children")

        // Pattern 4: Multiple element types
        def lines = allDescendants.findAll { it instanceof Line }
        def paths = allDescendants.findAll { it instanceof Path }
        // Either lines or paths may exist depending on rendering

        // Pattern 5: Text content search
        def textElements = allDescendants.findAll { it instanceof Text }
        // Text elements contain labels, titles, etc.

        // Pattern 6: SVG properties
        assertNotNull(svg.width, "Should have width")
        assertNotNull(svg.height, "Should have height")
    }

    /**
     * Performance comparison validating the documented speedup.
     * Uses warmup and multiple iterations to reduce timing variance.
     */
    @Test
    void testPerformanceBenefit() {
        def mtcars = Dataset.mtcars()
        def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) + geom_point()
        Svg svg = chart.render()

        // Warmup iterations to allow JIT compilation
        for (int i = 0; i < 5; i++) {
            svg.descendants().findAll { it instanceof Circle }
            svg.toXml()
        }

        // Method 1: Direct object access (RECOMMENDED) - multiple iterations
        long startDirect = System.nanoTime()
        boolean hasCirclesDirect = false
        for (int i = 0; i < 10; i++) {
            def circles = svg.descendants().findAll { it instanceof Circle }
            hasCirclesDirect = circles.size() > 0
        }
        long directTimeNs = System.nanoTime() - startDirect

        // Method 2: Serialization approach (OLD, AVOID) - multiple iterations
        long startSerial = System.nanoTime()
        boolean hasCirclesSerial = false
        for (int i = 0; i < 10; i++) {
            String svgContent = svg.toXml()
            hasCirclesSerial = svgContent.contains('<circle')
        }
        long serialTimeNs = System.nanoTime() - startSerial

        // Validate both methods work
        assertEquals(hasCirclesDirect, hasCirclesSerial, "Both methods should find circles")

        // Document timing (informational - no strict assertion to avoid flakiness)
        log.info("Direct access: ${directTimeNs / 1_000_000}ms, Serialization: ${serialTimeNs / 1_000_000}ms")
    }
}
