package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Circle
import se.alipsa.groovy.svg.Line
import se.alipsa.groovy.svg.Path
import se.alipsa.groovy.svg.Rect
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Text
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.gg.aes.Factor
import se.alipsa.matrix.gg.coord.CoordPolar

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GgPlotTest {

  def iris = Dataset.iris()
  def mtcars = Dataset.mtcars()

  @Test
  void testAes() {
    def a = aes(x: 'Sepal Length', y: 'Petal Length', col: 'Species')
    assertEquals('Aes(xCol=Sepal Length, yCol=Petal Length, colorCol=Species)', a.toString())
  }

  @Test
  void testAesPositionalWithNamedParams() {
    def a = aes('cty', 'hwy', colour: 'class')
    assertEquals('cty', a.x)
    assertEquals('hwy', a.y)
    assertEquals('class', a.color)

    def b = aes('hp', 'mpg', x: 'ignored', y: 'also_ignored', color: 'cyl')
    assertEquals('hp', b.x)
    assertEquals('mpg', b.y)
    assertEquals('cyl', b.color)

    def c = aes('Sepal Length', 'Petal Length', col: 'Species', size: 'Petal Width', alpha: 0.8)
    assertEquals('Sepal Length', c.x)
    assertEquals('Petal Length', c.y)
    assertEquals('Species', c.color)
    assertEquals('Petal Width', c.size)
    assertEquals(0.8, c.alpha)

    def chart = ggplot(Dataset.mpg(), aes('cty', 'hwy', colour: 'class')) + geom_point()
    assertNotNull(chart)
  }

  @Test
  void testPoint() {
    ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length', col: 'Species')) + geom_point()

    ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length', col: 'Species')) +
        geom_point() + geom_smooth()

    ggplot(iris, aes(x: 'Sepal Length', y: 'Petal Length', col: 'Species')) +
        geom_point(color: 'blue') + geom_smooth(color: 'red')
  }

  @Test
  void testVerticalBarPlot() {
    ggplot(mtcars, aes(x: 'gear')) + geom_bar()
  }

  @Test
  void testHistogram() {
    ggplot(mtcars, aes(x: 'mpg')) + geom_histogram()
  }

  @Test
  void testBoxPlot() {
    ggplot(mtcars, aes(x: As.factor(mtcars['cyl']), y: 'mpg')) + geom_boxplot()

    def cyl = As.factor(mtcars['cyl'])
    ggplot(mtcars, aes(x: cyl, y: 'mpg', color: 'cyl')) +
        geom_boxplot() +
        scale_color_manual(values: ['#3a0ca3', '#c9184a', '#3a5a40'])
  }

  @Test
  void testPieChart() {
    def chart = ggplot(mtcars, aes(factor(1), fill: 'cyl')) +
        geom_bar(width: 1) +
        coord_polar(theta: 'y', start: 0)
    assertTrue(chart.coord instanceof CoordPolar)

    Svg svg = chart.render()
    assertNotNull(svg)

    def paths = svg.descendants().findAll { it instanceof Path }
    assertTrue(paths.size() > 0, 'Pie chart should render arc paths')

    Stat.medianBy(mtcars, 'cyl', 'mpg')
  }

  @Test
  void testScatterWithSmooth() {
    def mpg = Dataset.mpg()

    def chart = ggplot(mpg, aes(x: 'cty', y: 'hwy')) +
        geom_point() +
        geom_smooth(method: 'lm') +
        labs(title: 'City vs Highway MPG', x: 'City MPG', y: 'Highway MPG')

    Svg svg = chart.render()
    assertNotNull(svg)

    def circles = svg.descendants().findAll { it instanceof Circle }
    def lines = svg.descendants().findAll { it instanceof Line }

    assertTrue(circles.size() > 0, 'Should contain circle elements for points')
    assertTrue(lines.size() > 0, 'Should contain line elements for smooth')
  }

  @Test
  void testGeomFunction() {
    def chart = ggplot(null, null) +
        xlim(0, 2 * Math.PI) +
        geom_function(fun: { x -> Math.sin(x) }, color: 'steelblue')

    Svg svg = chart.render()
    assertNotNull(svg)

    def paths = svg.descendants().findAll { it instanceof Path }
    assertTrue(paths.size() > 0, 'Should contain path element for function')
  }

  @Test
  void testCoordTransLog10() {
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
    assertTrue(circles.size() > 0 || lines.size() > 0, 'Should contain points or lines')
  }

  @Test
  void testGeomBin2dAlias() {
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

    String svgContent1 = SvgWriter.toXml(svg1)
    String svgContent2 = SvgWriter.toXml(svg2)

    assertTrue(svgContent1.contains('<svg'))
    assertTrue(svgContent2.contains('<svg'))
  }

  @Test
  void testStatBin2dAlias() {
    def statAlias = stat_bin2d()
    def statOriginal = stat_bin_2d()

    assertNotNull(statAlias)
    assertNotNull(statOriginal)
    assertTrue(statAlias.class.name.contains('StatsBin2D'))
    assertTrue(statOriginal.class.name.contains('StatsBin2D'))
  }
}
