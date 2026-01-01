package gg

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.gg.aes.Factor

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
        File outputFile = new File('build/pie_chart.svg')
        write(svg, outputFile)
        String svgContent = SvgWriter.toXml(svg)
        assertTrue(svgContent.contains('<rect'), "Should render bars for factor(1)")
    }

    @Test
    void testPieChart() {
        def chart = ggplot(mtcars, aes(factor(1), fill: 'cyl')) \
            + geom_bar(width:1) \
            + coord_polar(theta: "y", start:0)
        assertTrue(chart.coord instanceof se.alipsa.matrix.gg.coord.CoordPolar)
        Svg svg = chart.render()
        assertNotNull(svg)
        File outputFile = new File('build/pie_chart.svg')
        write(svg, outputFile)
        String svgContent = SvgWriter.toXml(svg)
        assertTrue(svgContent.contains('<path'), "Pie chart should render arc paths")

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

        // Get the SVG string representation using SvgWriter
        String svgContent = SvgWriter.toXml(svg)

        // Verify basic SVG structure
        assertTrue(svgContent.contains('<svg'), "Should contain SVG element")
        // Width may be larger than 800 to accommodate legend
        assertTrue(svgContent.contains('width="') && svgContent.contains('height="600"'),
            "Should have width and height attributes")
        def widthMatcher = (svgContent =~ /width="(\d+)"/)
        assertTrue(widthMatcher.find(), "Should include a numeric width attribute")
        int widthValue = (widthMatcher.group(1) as String).toInteger()
        assertTrue(widthValue >= 800, "Width should be at least the base 800px")
        def heightMatcher = (svgContent =~ /height="(\d+)"/)
        assertTrue(heightMatcher.find(), "Should include a numeric height attribute")
        int heightValue = (heightMatcher.group(1) as String).toInteger()
        assertEquals(600, heightValue, "Height should remain at 600px")

        // Verify there are circles (points)
        assertTrue(svgContent.contains('<circle'), "Should contain circle elements for points")

        // Verify title is present
        assertTrue(svgContent.contains('Iris Scatter Plot'), "Should contain the title")

        // Write to file for manual inspection
        File outputFile = new File('build/iris_scatter.svg')
        write(svg, outputFile)
        assertTrue(outputFile.exists(), "Output file should exist")
        println("Wrote scatter plot to ${outputFile.absolutePath}")
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

        String svgContent = SvgWriter.toXml(svg)

        // Verify points are rendered
        assertTrue(svgContent.contains('<circle'), "Should contain circle elements for points")

        // Verify smooth line is rendered (multiple connected line segments)
        assertTrue(svgContent.contains('<line'), "Should contain line elements for smooth")
        assertTrue(svgContent.contains('stroke="#3366FF"'), "Should have default smooth color")

        // Write for inspection
        File outputFile = new File('build/mpg_scatter_smooth.svg')
        write(svg, outputFile)
        println("Wrote mpg scatter+smooth plot to ${outputFile.absolutePath}")
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

        String svgContent = SvgWriter.toXml(svg)
        assertTrue(svgContent.contains('Horsepower vs MPG'))
        assertTrue(svgContent.contains('Horsepower'))
        assertTrue(svgContent.contains('Miles per Gallon'))

        // Write for inspection
        File outputFile = new File('build/mtcars_scatter.svg')
        write(svg, outputFile)
        println("Wrote mtcars scatter plot to ${outputFile.absolutePath}")
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

        String svgContent = SvgWriter.toXml(svg)

        // Verify points are rendered
        assertTrue(svgContent.contains('<circle'), "Should contain circle elements for points")

        // Verify smooth line is rendered
        assertTrue(svgContent.contains('<line'), "Should contain line elements for geom_lm")

        // Write for inspection
        File outputFile = new File('build/mpg_geom_lm.svg')
        write(svg, outputFile)
        println("Wrote mpg geom_lm plot to ${outputFile.absolutePath}")
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

        String svgContent = SvgWriter.toXml(svg)

        // Verify points and line are rendered
        assertTrue(svgContent.contains('<circle'), "Should contain circle elements for points")
        assertTrue(svgContent.contains('<line'), "Should contain line elements for polynomial fit")
        assertTrue(svgContent.contains('stroke="red"'), "Should have red color for polynomial line")

        // Write for inspection
        File outputFile = new File('build/mpg_polynomial.svg')
        write(svg, outputFile)
        println("Wrote mpg polynomial plot to ${outputFile.absolutePath}")
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

        String svgContent = SvgWriter.toXml(svg)

        // Verify points are rendered
        assertTrue(svgContent.contains('<circle'), "Should contain circle elements for points")

        // Write for inspection
        File outputFile = new File('build/mpg_expression.svg')
        write(svg, outputFile)
        println("Wrote mpg expression plot to ${outputFile.absolutePath}")
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

        String svgContent = SvgWriter.toXml(svg)

        // Verify both points and regression line are rendered
        assertTrue(svgContent.contains('<circle'), "Should contain circle elements for points")
        assertTrue(svgContent.contains('<line'), "Should contain line elements for regression")

        // Write for inspection
        File outputFile = new File('build/mpg_expr_lm.svg')
        write(svg, outputFile)
        println("Wrote mpg expression+lm plot to ${outputFile.absolutePath}")
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

        String svgContent = SvgWriter.toXml(svg)

        // Verify both points and regression line are rendered
        assertTrue(svgContent.contains('<circle'), "Should contain circle elements for points")
        assertTrue(svgContent.contains('<line'), "Should contain line elements for polynomial")
        assertTrue(svgContent.contains('stroke="red"'), "Should have red polynomial line")

        // Write for inspection
        File outputFile = new File('build/mpg_expr_polynomial.svg')
        write(svg, outputFile)
        println("Wrote mpg expression+polynomial plot to ${outputFile.absolutePath}")
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
        assertTrue(svgContent.contains('<circle'), "Should contain circle elements for points")
        assertTrue(svgContent.contains('<line'), "Should contain line elements for polynomial fit")
        assertTrue(svgContent.contains('stroke="blue"'), "Should have blue color for polynomial line")

        // Write for inspection
        File outputFile = new File('build/mpg_degree_param.svg')
        write(svg, outputFile)
        println("Wrote mpg degree parameter plot to ${outputFile.absolutePath}")
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

        assertTrue(svgContent.contains('<circle'), "Should contain circle elements for points")
        assertTrue(svgContent.contains('<line'), "Should contain line elements for polynomial")
        assertTrue(svgContent.contains('stroke="purple"'), "Should have purple polynomial line")

        // Write for inspection
        File outputFile = new File('build/mpg_degree_expr.svg')
        write(svg, outputFile)
        println("Wrote mpg degree+expression plot to ${outputFile.absolutePath}")
    }

}
