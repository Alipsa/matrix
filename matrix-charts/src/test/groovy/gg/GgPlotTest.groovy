package gg

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Stat

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
    void testPieChart() {
        ggplot(mtcars, aes(x:"", y:"mpg", fill:"cyl")) \
            + geom_bar(stat:"identity", width:1) \
            + coord_polar(theta: "y", start:0)

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

}
