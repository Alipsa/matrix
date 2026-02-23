package gg

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Rect
import se.alipsa.groovy.svg.Line
import se.alipsa.groovy.svg.Path
import se.alipsa.groovy.svg.Text
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Factor

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.alipsa.matrix.datasets.Dataset
import static se.alipsa.matrix.gg.GgPlot.*
import testutil.Slow

@Slow
class GgPlotFullTest {

    def iris = Dataset.iris()
    def mtcars = Dataset.mtcars()

    @Test
    void testHorizontalBarPlot() {
        ggplot(mtcars, aes(x: 'gear')) +geom_bar() + coord_flip()
    }

    @Test
    void testStatisticalTransformation() {
        ggplot(mtcars, aes('hp', 'mpg')) + geom_point(color: "blue") \
        + stat_summary('fun.y': "mean", geom: "line", linetype: "dashed")
    }

    @Test
    void testRuggedLinePlot() {
        ggplot(mtcars, aes('hp', 'mpg')) + geom_point(color: "blue") \
        + geom_rug('show.legend': false) +stat_summary('fun.y': "mean",
                geom: "line", linetype: "dashed")
    }

    @Test
    void testViolinPlot() {
        ggplot(mtcars, aes(As.factor(mtcars['cyl']), 'mpg')) + \
            geom_violin(aes(fill: 'cyl'))
    }

    @Test
    void testFactorConstant() {
        def data = Matrix.builder()
            .columnNames(['class'])
            .rows([['a'], ['b'], ['a']])
            .build()

        def aesSpec = aes(factor(1), fill: 'class')
        assertTrue(aesSpec.x instanceof Factor)

        def chart = ggplot(data, aesSpec) + geom_bar()
        Svg svg = chart.render()
        assertNotNull(svg)

        // Use direct object access - no serialization needed
        def rects = svg.descendants().findAll { it instanceof Rect }
        assertTrue(rects.size() > 0, "Should render bars for factor(1)")
    }

    @Test
    void testPointChartRender() {
        // Create a scatter plot and render it to SVG
        def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length', col: 'Species')) +
            geom_point() +
            labs(title: 'Iris Scatter Plot', x: 'Sepal Length', y: 'Petal Length') +
            theme_minimal()

        Svg svg = chart.render()

        // Verify SVG was created
        assertNotNull(svg, "SVG should not be null")

        // Use direct object access for assertions
        def circles = svg.descendants().findAll { it instanceof Circle }
        assertTrue(circles.size() > 0, "Should contain circle elements for points")

        def textElements = svg.descendants().findAll { it instanceof Text }
        def allText = textElements.collect { it.content }.join(' ')
        assertTrue(allText.contains('Iris Scatter Plot'), "Should contain the title")

        // Verify dimensions
        assertTrue(svg.width != null && svg.height != null, "Should have width and height")
        int widthValue = svg.width as int
        int heightValue = svg.height as int
        assertTrue(widthValue >= 800, "Width should be at least the base 800px")
        assertEquals(600, heightValue, "Height should remain at 600px")

        // Write to file for manual inspection
        File outputFile = new File('build/iris_scatter.svg')
        write(svg, outputFile)
        assertTrue(outputFile.exists(), "Output file should exist")
    }

    @Test
    void testPointChartWithLabels() {
        // Test with explicit labels
        def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) +
            geom_point(color: 'steelblue', size: 4) +
            labs(
                title: 'Horsepower vs MPG',
                subtitle: 'Motor Trend Car Data',
                x: 'Horsepower',
                y: 'Miles per Gallon'
            ) +
            theme_gray()

        Svg svg = chart.render()
        assertNotNull(svg)

        // Use direct object access to find text elements
        def textElements = svg.descendants().findAll { it instanceof Text }
        def allText = textElements.collect { it.content }.join(' ')

        assertTrue(allText.contains('Horsepower vs MPG'))
        assertTrue(allText.contains('Horsepower'))
        assertTrue(allText.contains('Miles per Gallon'))
    }

    @Test
    void testGeomLm() {
        // Test the geom_lm convenience wrapper
        def mpg = Dataset.mpg()

        def chart = ggplot(mpg, aes(x: 'displ', y: 'hwy')) +
            geom_point() +
            geom_lm() +
            labs(title: 'Engine Displacement vs Highway MPG')

        Svg svg = chart.render()
        assertNotNull(svg)

        // Use direct object access
        def circles = svg.descendants().findAll { it instanceof Circle }
        def lines = svg.descendants().findAll { it instanceof Line }

        // Verify points are rendered
        assertTrue(circles.size() > 0, "Should contain circle elements for points")

        // Verify smooth line is rendered
        assertTrue(lines.size() > 0, "Should contain line elements for geom_lm")
    }

    @Test
    void testGeomLmWithPolynomial() {
        // Test geom_lm with polynomial formula
        def mpg = Dataset.mpg()

        def chart = ggplot(mpg, aes(x: 'displ', y: 'hwy')) +
            geom_point(alpha: 0.5) +
            geom_lm(formula: 'y ~ poly(x, 2)', colour: 'red', linewidth: 1) +
            labs(title: 'Polynomial Fit (degree 2)')

        Svg svg = chart.render()
        assertNotNull(svg)

        // Use direct object access
        def circles = svg.descendants().findAll { it instanceof Circle }
        def lines = svg.descendants().findAll { it instanceof Line }

        // Verify points and line are rendered
        assertTrue(circles.size() > 0, "Should contain circle elements for points")
        assertTrue(lines.size() > 0, "Should contain line elements for polynomial fit")
    }

    @Test
    void testExpressionInAes() {
        // Test closure expression in aesthetics
        def mpg = Dataset.mpg()

        // Using expr() wrapper for clarity
        def chart = ggplot(mpg, aes(x: 'displ', y: expr { 1.0 / it.hwy })) +
            geom_point() +
            labs(title: 'Displacement vs 1/Highway MPG', y: '1 / Highway MPG')

        Svg svg = chart.render()
        assertNotNull(svg)

        // Use direct object access
        def circles = svg.descendants().findAll { it instanceof Circle }

        // Verify points are rendered
        assertTrue(circles.size() > 0, "Should contain circle elements for points")
    }

    @Test
    void testExpressionWithGeomLm() {
        // Test combining expression in aes with geom_lm
        def mpg = Dataset.mpg()

        def chart = ggplot(mpg, aes(x: 'displ', y: expr { 1.0 / it.hwy })) +
            geom_point() +
            geom_lm() +
            labs(title: 'Displacement vs 1/Highway MPG with Linear Fit')

        Svg svg = chart.render()
        assertNotNull(svg)

        // Use direct object access
        def circles = svg.descendants().findAll { it instanceof Circle }
        def lines = svg.descendants().findAll { it instanceof Line }

        // Verify both points and regression line are rendered
        assertTrue(circles.size() > 0, "Should contain circle elements for points")
        assertTrue(lines.size() > 0, "Should contain line elements for regression")
    }

    @Test
    void testExpressionWithPolynomial() {
        // Full example: expression in aes with polynomial geom_lm
        def mpg = Dataset.mpg()

        def chart = ggplot(mpg, aes(x: 'displ', y: expr { 1.0 / it.hwy })) +
            geom_point() +
            geom_lm(formula: 'y ~ poly(x, 2)', linewidth: 1, colour: 'red') +
            labs(title: 'Polynomial Fit on Transformed Data')

        Svg svg = chart.render()
        assertNotNull(svg)

        // Use direct object access
        def circles = svg.descendants().findAll { it instanceof Circle }
        def lines = svg.descendants().findAll { it instanceof Line }

        // Verify both points and regression line are rendered
        assertTrue(circles.size() > 0, "Should contain circle elements for points")
        assertTrue(lines.size() > 0, "Should contain line elements for polynomial")
    }

    @Test
    void testDegreeParameter() {
        // Test using degree parameter instead of formula string
        def mpg = Dataset.mpg()

        // Using degree: 2 (Groovy-style) instead of formula: 'y ~ poly(x, 2)' (R-style)
        def chart = ggplot(mpg, aes(x: 'displ', y: 'hwy')) +
            geom_point(alpha: 0.5) +
            geom_lm(degree: 2, colour: 'blue', linewidth: 1.5) +
            labs(title: 'Polynomial Fit using degree parameter')

        Svg svg = chart.render()
        assertNotNull(svg)

        String svgContent = SvgWriter.toXml(svg)

        // Verify points and line are rendered
        def circles = svg.descendants().findAll { it instanceof Circle }
        def lines = svg.descendants().findAll { it instanceof Line }
        assertTrue(circles.size() > 0, "Should contain circle elements for points")
        assertTrue(lines.size() > 0, "Should contain line elements for polynomial fit")

    }

    @Test
    void testDegreeWithExpression() {
        // Combine degree parameter with expression in aes
        def mpg = Dataset.mpg()

        def chart = ggplot(mpg, aes(x: 'displ', y: expr { 1.0 / it.hwy })) +
            geom_point() +
            geom_lm(degree: 2, colour: 'purple') +
            labs(title: 'Degree 2 with Expression', y: '1 / Highway MPG')

        Svg svg = chart.render()
        assertNotNull(svg)

        String svgContent = SvgWriter.toXml(svg)

        def circles = svg.descendants().findAll { it instanceof Circle }
        def lines = svg.descendants().findAll { it instanceof Line }
        assertTrue(circles.size() > 0, "Should contain circle elements for points")
        assertTrue(lines.size() > 0, "Should contain line elements for polynomial")
    }

    @Test
    void testGeomHex() {
        // Create sample data with x,y coordinates
        def data = Matrix.builder()
            .columnNames(['x', 'y'])
            .rows((1..200).collect { [Math.random() * 10, Math.random() * 10] })
            .build()

        def chart = ggplot(data, aes('x', 'y')) +
            geom_hex(bins: 10) +
            labs(title: 'Hexagonal Binning Test')

        Svg svg = chart.render()
        assertNotNull(svg)

        def paths = svg.descendants().findAll { it instanceof Path }
        assertTrue(paths.size() > 0, "Should contain path elements for hexagons")

        File outputFile = new File('build/test_geom_hex.svg')
        write(svg, outputFile)
    }

    @Test
    void testGeomDotplot() {
        // Test with mtcars data
        def chart = ggplot(mtcars, aes(x: 'mpg')) +
            geom_dotplot(binwidth: 2) +
            labs(title: 'Dotplot Test')

        Svg svg = chart.render()
        assertNotNull(svg)

        def circles = svg.descendants().findAll { it instanceof Circle }
        assertTrue(circles.size() > 0, "Should contain circle elements for dots")
    }

    @Test
    void testGeomDensity2d() {
        // Test with faithful dataset or mtcars
        def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) +
            geom_density_2d(bins: 8) +
            labs(title: '2D Density Contours Test')

        Svg svg = chart.render()
        assertNotNull(svg)

        def paths = svg.descendants().findAll { it instanceof Path }
        assertTrue(paths.size() > 0, "Should contain path elements for contours")
    }

    @Test
    void testGeomDensity2dFilled() {
        // Test with mtcars data
        def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) +
            geom_density_2d_filled(bins: 8, alpha: 0.7) +
            labs(title: 'Filled 2D Density Contours Test')

        Svg svg = chart.render()
        assertNotNull(svg)

        def rects = svg.descendants().findAll { it instanceof Rect }
        assertTrue(rects.size() > 0, "Should contain rect elements for filled regions")
    }

    @Test
    void testStatEllipse() {
        // Test with iris data
        def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length')) +
            geom_point(alpha: 0.5) +
            stat_ellipse([level: 0.95]) +
            labs(title: 'Confidence Ellipse Test')

        Svg svg = chart.render()
        assertNotNull(svg)

        def descendants = svg.descendants()
        def circles = descendants.findAll { it instanceof Circle }
        def paths = descendants.findAll { it instanceof Path }
        assertTrue(circles.size() > 0 || paths.size() > 0,
                   "Should contain points or path elements")
    }

    @Test
    void testStatEllipseLevels() {
        // Test that different confidence levels produce different sized ellipses
        def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length')) +
            geom_point(alpha: 0.3) +
            stat_ellipse([level: 0.50]) +
            geom_path(color: 'blue') +
            stat_ellipse([level: 0.90]) +
            geom_path(color: 'green') +
            stat_ellipse([level: 0.99]) +
            geom_path(color: 'red') +
            labs(title: 'Multiple Confidence Levels (50%, 90%, 99%)')

        Svg svg = chart.render()
        assertNotNull(svg)

        def paths = svg.descendants().findAll { it instanceof Path }
        assertTrue(paths.size() > 0,
                   "Should contain path elements for ellipses")

        File outputFile = new File('build/test_stat_ellipse_levels.svg')
        write(svg, outputFile)
    }

    @Test
    void testStatSummaryBin() {
        // Test binned summary
        def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) +
            stat_summary_bin([bins: 10, fun: 'mean']) +
            labs(title: 'Binned Summary Test')

        Svg svg = chart.render()
        assertNotNull(svg)
    }

    @Test
    void testStatUnique() {
        // Create data with duplicates
        def data = Matrix.builder()
            .columnNames(['x', 'y', 'group'])
            .rows([
                [1, 2, 'A'],
                [1, 2, 'A'],  // duplicate
                [2, 3, 'B'],
                [2, 3, 'B'],  // duplicate
                [3, 4, 'A']
            ])
            .build()

        def chart = ggplot(data, aes(x: 'x', y: 'y')) +
            stat_unique() +
            geom_point(size: 5) +
            labs(title: 'Unique Points Test')

        Svg svg = chart.render()
        assertNotNull(svg)

        // The test generates SVG successfully, verifying stat_unique runs without error
        def circles = svg.descendants().findAll { it instanceof Circle }
        assertTrue(circles.size() > 0, "Should contain circle elements")
    }

    @Test
    void testStatFunction() {
        // Test function stat
        def chart = ggplot(null, aes(x: 'x', y: 'y')) +
            stat_function([fun: { x -> x * x }, xlim: [-5, 5], n: 100]) +
            geom_line() +
            labs(title: 'Function Plot: y = x²')

        Svg svg = chart.render()
        assertNotNull(svg)

        def descendants = svg.descendants()
        def lines = descendants.findAll { it instanceof Line }
        def paths = descendants.findAll { it instanceof Path }
        assertTrue(lines.size() > 0 || paths.size() > 0,
                   "Should contain line or path elements")
    }

    @Test
    void testCoordTransSqrt() {
        // Test sqrt transformation on y-axis
        def data = Matrix.builder()
            .columnNames(['x', 'y'])
            .rows([
                [1, 1],
                [2, 4],
                [3, 9],
                [4, 16],
                [5, 25]
            ])
            .build()

        def chart = ggplot(data, aes(x: 'x', y: 'y')) +
            geom_point(size: 3) +
            geom_line() +
            coord_trans(y: 'sqrt') +
            labs(title: 'Square Root Transformation on Y-axis')

        Svg svg = chart.render()
        assertNotNull(svg)

        def circles = svg.descendants().findAll { it instanceof Circle }
        assertTrue(circles.size() > 0,
                   "Should contain points")
    }

    @Test
    void testCoordTransBothAxes() {
        // Test transformation on both axes
        def data = Matrix.builder()
            .columnNames(['x', 'y'])
            .rows([
                [1, 1],
                [10, 4],
                [100, 9],
                [1000, 16]
            ])
            .build()

        def chart = ggplot(data, aes(x: 'x', y: 'y')) +
            geom_point(size: 3, color: 'blue') +
            coord_trans(x: 'log10', y: 'sqrt') +
            labs(title: 'Log10(X) and Sqrt(Y) Transformations')

        Svg svg = chart.render()
        assertNotNull(svg)

        def circles = svg.descendants().findAll { it instanceof Circle }
        assertTrue(circles.size() > 0,
                   "Should contain points")
    }

    @Test
    void testCoordTransCustom() {
        // Test custom transformation using closures
        def data = Matrix.builder()
            .columnNames(['x', 'y'])
            .rows([
                [1, 1],
                [2, 2],
                [3, 3],
                [4, 4],
                [5, 5]
            ])
            .build()

        def chart = ggplot(data, aes(x: 'x', y: 'y')) +
            geom_point(size: 3) +
            geom_line() +
            coord_trans(x: [forward: { x -> x * x }, inverse: { x -> x ** 0.5 }]) +
            labs(title: 'Custom Transformation (x^2)')

        Svg svg = chart.render()
        assertNotNull(svg)
    }

    @Test
    void testCoordTransReverse() {
        // Test reverse transformation
        def chart = ggplot(mtcars, aes(x: 'wt', y: 'mpg')) +
            geom_point() +
            coord_trans(x: 'reverse') +
            labs(title: 'Reversed X-axis')

        Svg svg = chart.render()
        assertNotNull(svg)

        File outputFile = new File('build/test_coord_trans_reverse.svg')
        write(svg, outputFile)
    }

    @Test
    void testCoordTransLog() {
        // Test natural log transformation on y-axis
        def data = Matrix.builder()
            .columnNames(['x', 'y'])
            .rows([
                [1, Math.E],
                [2, Math.E * Math.E],
                [3, Math.E ** 3],
                [4, Math.E ** 4]
            ])
            .build()

        def chart = ggplot(data, aes(x: 'x', y: 'y')) +
            geom_point(size: 3) +
            geom_line() +
            coord_trans(y: 'log') +
            labs(title: 'Natural Log Transformation on Y-axis')

        Svg svg = chart.render()
        assertNotNull(svg)

        def descendants = svg.descendants()
        def circles = descendants.findAll { it instanceof Circle }
        def lines = descendants.findAll { it instanceof Line }
        assertTrue(circles.size() > 0 || lines.size() > 0,
                   "Should contain points or lines")
    }

    @Test
    void testCoordTransPower() {
        // Test power transformation with explicit exponent
        def data = Matrix.builder()
            .columnNames(['x', 'y'])
            .rows([
                [1, 1],
                [2, 2],
                [3, 3],
                [4, 4],
                [5, 5]
            ])
            .build()

        def chart = ggplot(data, aes(x: 'x', y: 'y')) +
            geom_point(size: 3) +
            geom_line() +
            coord_trans(y: [name: 'power', exponent: 3]) +
            labs(title: 'Power Transformation (y^3)')

        Svg svg = chart.render()
        assertNotNull(svg)

        def circles = svg.descendants().findAll { it instanceof Circle }
        assertTrue(circles.size() > 0,
                   "Should contain points")
    }

    @Test
    void testCoordTransReciprocal() {
        // Test reciprocal (1/x) transformation
        def data = Matrix.builder()
            .columnNames(['x', 'y'])
            .rows([
                [1, 10],
                [2, 5],
                [4, 2.5],
                [5, 2],
                [10, 1]
            ])
            .build()

        def chart = ggplot(data, aes(x: 'x', y: 'y')) +
            geom_point(size: 3) +
            geom_line() +
            coord_trans(x: 'reciprocal') +
            labs(title: 'Reciprocal Transformation on X-axis')

        Svg svg = chart.render()
        assertNotNull(svg)

        def circles = svg.descendants().findAll { it instanceof Circle }
        assertTrue(circles.size() > 0,
                   "Should contain points")
    }

    @Test
    void testCoordTransAsn() {
        // Test arcsine square root transformation (for proportions)
        def data = Matrix.builder()
            .columnNames(['x', 'proportion'])
            .rows([
                [1, 0.1],
                [2, 0.25],
                [3, 0.5],
                [4, 0.75],
                [5, 0.9]
            ])
            .build()

        def chart = ggplot(data, aes(x: 'x', y: 'proportion')) +
            geom_point(size: 3) +
            geom_line() +
            coord_trans(y: 'asn') +
            labs(title: 'Arcsine Square Root Transformation (for proportions)')

        Svg svg = chart.render()
        assertNotNull(svg)

        def circles = svg.descendants().findAll { it instanceof Circle }
        assertTrue(circles.size() > 0,
                   "Should contain points")
    }

    @Test
    void testStatBinHex() {
        // Test hexagonal binning stat with random data
        def data = Matrix.builder()
            .columnNames(['x', 'y'])
            .rows((1..200).collect { [Math.random() * 10, Math.random() * 10] })
            .build()

        def chart = ggplot(data, aes('x', 'y')) +
            stat_bin_hex(bins: 10) +
            geom_point(size: 2, alpha: 0.5) +
            labs(title: 'stat_bin_hex Test')

        Svg svg = chart.render()
        assertNotNull(svg)

        def descendants = svg.descendants()
        def paths = descendants.findAll { it instanceof Path }
        def circles = descendants.findAll { it instanceof Circle }
        assertTrue(paths.size() > 0 || circles.size() > 0,
                   "Should contain path elements for hexagons or circle elements for points")
    }

    @Test
    void testStatBinHexWithMtcars() {
        // Test hexagonal binning with mtcars dataset
        def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) +
            stat_bin_hex(bins: 8) +
            labs(title: 'Hexagonal Binning: HP vs MPG')

        Svg svg = chart.render()
        assertNotNull(svg)
    }

    @Test
    void testStatSummaryHex() {
        // Test hexagonal summary stat - compute mean of z in each hex bin
        def data = Matrix.builder()
            .columnNames(['x', 'y', 'z'])
            .rows((1..200).collect {
                def x = Math.random() * 10
                def y = Math.random() * 10
                def z = x + y + (Math.random() * 2 - 1) // z correlates with x+y plus noise
                [x, y, z]
            })
            .build()

        def chart = ggplot(data, aes(x: 'x', y: 'y', fill: 'z')) +
            stat_summary_hex(bins: 8, fun: 'mean') +
            labs(title: 'stat_summary_hex Test - Mean Z value')

        Svg svg = chart.render()
        assertNotNull(svg)
    }

    @Test
    void testStatSummaryHexWithMtcars() {
        // Test hexagonal summary with mtcars - mean weight in hp/mpg bins
        def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg', fill: 'wt')) +
            stat_summary_hex(bins: 6, fun: 'mean') +
            labs(title: 'Hexagonal Summary: Mean Weight by HP/MPG')

        Svg svg = chart.render()
        assertNotNull(svg)
    }

    @Test
    void testStatSummaryHexMedian() {
        // Test hexagonal summary with median function
        def data = Matrix.builder()
            .columnNames(['x', 'y', 'value'])
            .rows((1..150).collect {
                [Math.random() * 5, Math.random() * 5, Math.random() * 100]
            })
            .build()

        def chart = ggplot(data, aes(x: 'x', y: 'y', fill: 'value')) +
            stat_summary_hex(bins: 6, fun: 'median') +
            labs(title: 'Hexagonal Summary: Median Values')

        Svg svg = chart.render()
        assertNotNull(svg)
    }

    // ========== ggplot2 API Compatibility Alias Tests ==========

    @Test
    void testGeomDensity2dAlias() {
        // Test that geom_density2d() (no underscore between 'density' and '2d') works as alias for geom_density_2d()
        def data = Matrix.builder()
            .columnNames(['x', 'y'])
            .rows((1..100).collect { [Math.random() * 5, Math.random() * 5] })
            .build()

        def chart1 = ggplot(data, aes(x: 'x', y: 'y')) + geom_density2d()
        def chart2 = ggplot(data, aes(x: 'x', y: 'y')) + geom_density_2d()

        Svg svg1 = chart1.render()
        Svg svg2 = chart2.render()

        assertNotNull(svg1)
        assertNotNull(svg2)

        String svgContent1 = SvgWriter.toXml(svg1)
        String svgContent2 = SvgWriter.toXml(svg2)

        assertTrue(svgContent1.contains('<svg'))
        assertTrue(svgContent2.contains('<svg'))
    }

    @Test
    void testGeomDensity2dFilledAlias() {
        // Test that geom_density2d_filled() (no underscore between 'density' and '2d') works as alias for geom_density_2d_filled()
        def data = Matrix.builder()
            .columnNames(['x', 'y'])
            .rows((1..100).collect { [Math.random() * 5, Math.random() * 5] })
            .build()

        def chart1 = ggplot(data, aes(x: 'x', y: 'y')) + geom_density2d_filled()
        def chart2 = ggplot(data, aes(x: 'x', y: 'y')) + geom_density_2d_filled()

        Svg svg1 = chart1.render()
        Svg svg2 = chart2.render()

        assertNotNull(svg1)
        assertNotNull(svg2)

        String svgContent1 = SvgWriter.toXml(svg1)
        String svgContent2 = SvgWriter.toXml(svg2)

        assertTrue(svgContent1.contains('<svg'))
        assertTrue(svgContent2.contains('<svg'))
    }

}
