package gg

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Rect
import se.alipsa.groovy.svg.Line
import se.alipsa.groovy.svg.Path
import se.alipsa.groovy.svg.Text
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.gg.aes.Factor
import se.alipsa.matrix.gg.coord.CoordPolar

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.alipsa.matrix.datasets.Dataset
import static se.alipsa.matrix.gg.GgPlot.*

class GgPlotTest {

    def iris = Dataset.iris()
    def mtcars = Dataset.mtcars()

    @Test
    void testAes() {
        def a = aes(x:"Sepal Length", y:"Petal Length", col:"Species")
        assertEquals('Aes(xCol=Sepal Length, yCol=Petal Length, colorCol=Species)', a.toString())
    }

    @Test
    void testAesPositionalWithNamedParams() {
        // Test the new constructor: aes('x', 'y', colour: 'z')
        def a = aes('cty', 'hwy', colour: 'class')
        assertEquals('cty', a.x)
        assertEquals('hwy', a.y)
        assertEquals('class', a.color)

        // Test that positional params override map params
        def b = aes('hp', 'mpg', x: 'ignored', y: 'also_ignored', color: 'cyl')
        assertEquals('hp', b.x)
        assertEquals('mpg', b.y)
        assertEquals('cyl', b.color)

        // Test with multiple named parameters
        def c = aes('Sepal Length', 'Petal Length', col: 'Species', size: 'Petal Width', alpha: 0.8)
        assertEquals('Sepal Length', c.x)
        assertEquals('Petal Length', c.y)
        assertEquals('Species', c.color)
        assertEquals('Petal Width', c.size)
        assertEquals(0.8, c.alpha)

        // Test integration with ggplot
        def chart = ggplot(Dataset.mpg(), aes('cty', 'hwy', colour: 'class')) + geom_point()
        assertNotNull(chart)
    }
    @Test
    void testPoint(){
        ggplot(iris,
                aes(x:"Sepal Length", y:"Petal Length", col:"Species")
        ) + geom_point()

        ggplot(iris, aes(x:'Sepal Length', y:'Petal Length', col:'Species')) \
            + geom_point() + geom_smooth()

        // same thing with colors
        ggplot(iris, aes(x:'Sepal Length', y:'Petal Length', col:'Species')) \
            + geom_point(color:"blue") + geom_smooth(color:"red")
    }


    @Test
    void testVerticalBarPlot() {
        ggplot(mtcars, aes(x: 'gear')) + geom_bar()
    }

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
    void testHistogram() {
        ggplot(mtcars,aes(x:'mpg')) + geom_histogram()
    }

    @Test
    void testBoxPlot() {
        //ggplot(mtcars, aes(x=as.factor(cyl), y=mpg)) + geom_boxplot()
        ggplot(mtcars, aes(x:As.factor(mtcars['cyl']), y:'mpg')) + geom_boxplot()

        // Colored boxplot
        def cyl = As.factor(mtcars['cyl'])
        ggplot(mtcars, aes(x:(cyl), y:'mpg',color: 'cyl')) \
        + geom_boxplot() \
        + scale_color_manual(values: ["#3a0ca3", "#c9184a", "#3a5a40"])
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
    void testPieChart() {
        def chart = ggplot(mtcars, aes(factor(1), fill: 'cyl')) \
            + geom_bar(width:1) \
            + coord_polar(theta: "y", start:0)
        assertTrue(chart.coord instanceof CoordPolar)
        Svg svg = chart.render()
        assertNotNull(svg)

        // Use direct object access for assertions
        def paths = svg.descendants().findAll { it instanceof Path }
        assertTrue(paths.size() > 0, "Pie chart should render arc paths")

        // Keep file write for visual inspection
        File outputFile = new File('build/pie_chart_test.svg')
        write(svg, outputFile)

        //mtcars %>%
        //dplyr::group_by(cyl) %>%
        //dplyr::summarize(mpg = median(mpg)) %>%
        //ggplot(aes(x = cyl, y = mpg)) + geom_col(aes(fill =cyl), color = NA) + labs(x = "", y = "Median mpg")  + coord_polar()
        Stat.medianBy(mtcars, 'cyl', 'mpg')
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
        //println("Wrote scatter plot to ${outputFile.absolutePath}")
    }

    @Test
    void testScatterWithSmooth() {
        // Equivalent to R:
        // ggplot(mpg, aes(cty, hwy)) + geom_point() + geom_smooth(method = "lm")
        def mpg = Dataset.mpg()

        def chart = ggplot(mpg, aes(x: 'cty', y: 'hwy')) +
            geom_point() +
            geom_smooth(method: 'lm') +
            labs(title: 'City vs Highway MPG', x: 'City MPG', y: 'Highway MPG')

        Svg svg = chart.render()
        assertNotNull(svg)

        // Use direct object access - no serialization needed
        def circles = svg.descendants().findAll { it instanceof Circle }
        def lines = svg.descendants().findAll { it instanceof Line }

        // Verify points are rendered
        assertTrue(circles.size() > 0, "Should contain circle elements for points")

        // Verify smooth line is rendered (multiple connected line segments)
        assertTrue(lines.size() > 0, "Should contain line elements for smooth")
        // Note: Stroke color verification would require accessing element attributes
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
    void testGeomFunction() {
        // Test geom_function for drawing mathematical functions
        def chart = ggplot(null, null) +
            xlim(0, 2*Math.PI) +
            geom_function(fun: { x -> Math.sin(x) }, color: 'steelblue')

        Svg svg = chart.render()
        assertNotNull(svg)

        // Use direct object access
        def paths = svg.descendants().findAll { it instanceof Path }
        assertTrue(paths.size() > 0, "Should contain path element for function")

        // Write for inspection
        File outputFile = new File('build/function_sine.svg')
        write(svg, outputFile)
        //println("Wrote sine function plot to ${outputFile.absolutePath}")
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
        //println("Wrote hexagonal binning plot to ${outputFile.absolutePath}")
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
        //println("Wrote multi-level ellipse plot to ${outputFile.absolutePath}")
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
    void testCoordTransLog10() {
        // Test log10 transformation on x-axis
        def data = Matrix.builder()
            .columnNames(['x', 'y'])
            .rows([
                [1, 1],
                [10, 2],
                [100, 3],
                [1000, 4],
                [10000, 5]
            ])
            .build()

        def chart = ggplot(data, aes(x: 'x', y: 'y')) +
            geom_point(size: 3) +
            geom_line() +
            coord_trans(x: 'log10') +
            labs(title: 'Log10 Transformation on X-axis')

        Svg svg = chart.render()
        assertNotNull(svg)

        def descendants = svg.descendants()
        def circles = descendants.findAll { it instanceof Circle }
        def lines = descendants.findAll { it instanceof Line }
        assertTrue(circles.size() > 0 || lines.size() > 0,
                   "Should contain points or lines")
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
        //println("Wrote coord_trans reverse plot to ${outputFile.absolutePath}")
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

    // ========== Phase 1: ggplot2 API Compatibility Alias Tests ==========

    @Test
    void testGeomBin2dAlias() {
        // Test that geom_bin2d() (ggplot2-style name with no underscore between 'bin' and '2d') works as alias for geom_bin_2d()
        def data = Matrix.builder()
            .columnNames(['x', 'y'])
            .rows((1..100).collect { [Math.random() * 5, Math.random() * 5] })
            .build()

        def chart1 = ggplot(data, aes(x: 'x', y: 'y')) + geom_bin2d()
        def chart2 = ggplot(data, aes(x: 'x', y: 'y')) + geom_bin_2d()

        Svg svg1 = chart1.render()
        Svg svg2 = chart2.render()

        assertNotNull(svg1)
        assertNotNull(svg2)

        // Both should produce similar output (just check they render without error)
        String svgContent1 = SvgWriter.toXml(svg1)
        String svgContent2 = SvgWriter.toXml(svg2)

        assertTrue(svgContent1.contains('<svg'))
        assertTrue(svgContent2.contains('<svg'))
    }

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

    @Test
    void testStatBin2dAlias() {
        // Test that stat_bin2d() (no underscore) works as alias for stat_bin_2d()
        def statAlias = stat_bin2d()
        def statOriginal = stat_bin_2d()

        assertNotNull(statAlias)
        assertNotNull(statOriginal)

        // Both should be StatsBin2D instances
        assertTrue(statAlias.class.name.contains('StatsBin2D'))
        assertTrue(statOriginal.class.name.contains('StatsBin2D'))
    }

    // ========== ggsave File Type Detection Tests ==========

    @Test
    void testGgsaveSvgExtension() {
        // Test that ggsave correctly saves SVG files based on .svg extension
        def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length')) +
            geom_point() +
            labs(title: 'SVG Export Test')

        File outputFile = new File('build/test_ggsave.svg')
        ggsave(chart, outputFile.path)

        assertTrue(outputFile.exists(), "SVG file should be created")
        assertTrue(outputFile.length() > 0, "SVG file should not be empty")

        // Verify it's actually an SVG file
        String content = outputFile.text
        assertTrue(content.contains('<svg'), "File should contain SVG content")
        assertTrue(content.contains('</svg>'), "File should have closing SVG tag")

        outputFile.delete()
    }

    @Test
    void testGgsavePngExtension() {
        // Test that ggsave correctly saves PNG files based on .png extension
        def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length')) +
            geom_point() +
            labs(title: 'PNG Export Test')

        File outputFile = new File('build/test_ggsave.png')
        ggsave(chart, outputFile.path)

        assertTrue(outputFile.exists(), "PNG file should be created")
        assertTrue(outputFile.length() > 0, "PNG file should not be empty")

        // Verify it's a PNG file by checking the magic number (first 8 bytes)
        byte[] header = new byte[8]
        new FileInputStream(outputFile).withCloseable { fis ->
            fis.read(header)
        }
        // PNG magic number: 89 50 4E 47 0D 0A 1A 0A
        assertEquals((byte) 0x89, header[0], "PNG file should have correct magic number")
        assertEquals((byte) 0x50, header[1], "PNG file should have correct magic number")
        assertEquals((byte) 0x4E, header[2], "PNG file should have correct magic number")
        assertEquals((byte) 0x47, header[3], "PNG file should have correct magic number")

        outputFile.delete()
    }

    @Test
    void testGgsaveJpgExtension() {
        // Test that ggsave correctly saves JPEG files based on .jpg extension
        def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length')) +
            geom_point() +
            labs(title: 'JPG Export Test')

        File outputFile = new File('build/test_ggsave.jpg')
        ggsave(chart, outputFile.path)

        assertTrue(outputFile.exists(), "JPG file should be created")
        assertTrue(outputFile.length() > 0, "JPG file should not be empty")

        // Verify it's a JPEG file by checking the magic number (first 3 bytes)
        byte[] header = new byte[3]
        new FileInputStream(outputFile).withCloseable { fis ->
            fis.read(header)
        }
        // JPEG magic number: FF D8 FF
        assertEquals((byte) 0xFF, header[0], "JPEG file should have correct magic number")
        assertEquals((byte) 0xD8, header[1], "JPEG file should have correct magic number")
        assertEquals((byte) 0xFF, header[2], "JPEG file should have correct magic number")

        outputFile.delete()
    }

    @Test
    void testGgsaveJpegExtension() {
        // Test that ggsave correctly saves JPEG files based on .jpeg extension
        def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length')) +
            geom_point() +
            labs(title: 'JPEG Export Test')

        File outputFile = new File('build/test_ggsave.jpeg')
        ggsave(chart, outputFile.path)

        assertTrue(outputFile.exists(), "JPEG file should be created")
        assertTrue(outputFile.length() > 0, "JPEG file should not be empty")

        // Verify it's a JPEG file by checking the magic number
        byte[] header = new byte[3]
        new FileInputStream(outputFile).withCloseable { fis ->
            fis.read(header)
        }
        // JPEG magic number: FF D8 FF
        assertEquals((byte) 0xFF, header[0], "JPEG file should have correct magic number")
        assertEquals((byte) 0xD8, header[1], "JPEG file should have correct magic number")
        assertEquals((byte) 0xFF, header[2], "JPEG file should have correct magic number")

        outputFile.delete()
    }

    @Test
    void testGgsaveJpegWithQuality() {
        // Test that ggsave correctly handles quality parameter for JPEG files
        def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length')) +
            geom_point() +
            labs(title: 'JPEG Quality Test')

        File outputFile = new File('build/test_ggsave_quality.jpeg')
        ggsave(chart, outputFile.path, quality: 0.5)

        assertTrue(outputFile.exists(), "JPEG file with quality parameter should be created")
        assertTrue(outputFile.length() > 0, "JPEG file should not be empty")

        outputFile.delete()
    }

    @Test
    void testGgsaveInvalidExtension() {
        // Test that ggsave throws exception for invalid file extension
        def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length')) +
            geom_point()

        def exception = assertThrows(IllegalArgumentException.class, {
            ggsave(chart, 'build/test_ggsave.txt')
        })

        assertTrue(exception.message.contains('extension'), "Exception message should mention extension")
    }

    @Test
    void testGgsaveNoExtension() {
        // Test that ggsave throws exception when no file extension is provided
        def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length')) +
            geom_point()

        def exception = assertThrows(IllegalArgumentException.class, {
            ggsave(chart, 'build/test_ggsave')
        })

        assertTrue(exception.message.contains('extension'), "Exception message should mention extension")
    }

    @Test
    void testGgsaveCaseInsensitiveExtension() {
        // Test that ggsave handles case-insensitive extensions (e.g., .PNG, .Svg)
        def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length')) +
            geom_point()

        // Test uppercase PNG
        File pngFile = new File('build/test_ggsave.PNG')
        ggsave(chart, pngFile.path)
        assertTrue(pngFile.exists(), "PNG file with uppercase extension should be created")
        pngFile.delete()

        // Test mixed case SVG
        File svgFile = new File('build/test_ggsave.SvG')
        ggsave(chart, svgFile.path)
        assertTrue(svgFile.exists(), "SVG file with mixed case extension should be created")
        svgFile.delete()

        // Test uppercase JPEG
        File jpegFile = new File('build/test_ggsave.JPEG')
        ggsave(chart, jpegFile.path)
        assertTrue(jpegFile.exists(), "JPEG file with uppercase extension should be created")
        jpegFile.delete()
    }

    @Test
    void testGgsaveWithWidthAndHeight() {
        // Test that ggsave correctly applies width and height parameters
        def chart = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length')) +
            geom_point() +
            labs(title: 'Size Parameter Test')

        File outputFile = new File('build/test_ggsave_size.svg')
        ggsave(chart, outputFile.path, width: 1200, height: 800, quality: 0.95)

        assertTrue(outputFile.exists(), "SVG file with custom size should be created")

        String content = outputFile.text
        assertTrue(content.contains('width'), "SVG should contain width attribute")
        assertTrue(content.contains('height'), "SVG should contain height attribute")

        outputFile.delete()
    }

    @Test
    void testGgsaveMultipleChartsSvg() {
        // Test that ggsave correctly saves multiple charts to a single SVG file
        def chart1 = ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length')) +
            geom_point() +
            labs(title: 'Chart 1')

        def chart2 = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) +
            geom_point() +
            labs(title: 'Chart 2')

        File outputFile = new File('build/test_ggsave_multiple.svg')
        ggsave(outputFile.path, chart1, chart2)

        assertTrue(outputFile.exists(), "Combined SVG file should be created")
        assertTrue(outputFile.length() > 0, "Combined SVG file should not be empty")

        String content = outputFile.text
        assertTrue(content.contains('Chart 1'), "Should contain first chart title")
        assertTrue(content.contains('Chart 2'), "Should contain second chart title")

        outputFile.delete()
    }

}
