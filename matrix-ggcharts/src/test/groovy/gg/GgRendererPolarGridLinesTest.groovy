package gg

import groovy.xml.XmlSlurper
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.coord.CoordPolar

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*
import static se.alipsa.groovy.svg.io.SvgWriter.toXml

class GgRendererPolarGridLinesTest {

  @Test
  void testPolarGridLinesWithDiscreteTheta() {
    def data = Matrix.builder()
        .columnNames(['group', 'value'])
        .rows([
            ['A', 1],
            ['B', 2],
            ['C', 3]
        ])
        .build()

    GgChart chart = ggplot(data, aes('group', 'value')) + geom_bar(stat: 'identity') + coord_polar()
    def svg = chart.render()
    def xml = new XmlSlurper().parseText(toXml(svg))

    def grid = xml.'**'.find { it.name() == 'g' && it.@id == 'grid' }
    assertNotNull(grid)
    assertEquals(3, grid.line.size())
    assertTrue(grid.circle.size() >= 1)

    def labels = xml.'**'.findAll { it.name() == 'text' }.collect { it.text() }
    assertTrue(labels.containsAll(['A', 'B', 'C']))
  }

  @Test
  void testPolarGridLinesThetaYPositions() {
    def data = Matrix.builder()
        .columnNames(['value'])
        .rows([[0], [50], [100]])
        .build()

    GgChart chart = ggplot(data, aes(factor(1), 'value')) +
        geom_bar(stat: 'identity') +
        coord_polar(theta: 'y', start: 0) +
        scale_y_continuous(breaks: [0, 50, 100])

    chart.width = 400
    chart.height = 400

    def svg = chart.render()
    def xml = new XmlSlurper().parseText(toXml(svg))

    def zeroLabel = xml.'**'.find { it.name() == 'text' && it.text() == '0' }
    assertNotNull(zeroLabel)

    double x = zeroLabel.@x.toString() as double
    double y = zeroLabel.@y.toString() as double
    def center = (chart.coord as CoordPolar).getCenter()
    double cx = center[0] as double
    double cy = center[1] as double

    double dist = Math.sqrt(Math.pow(x - cx, 2.0d) + Math.pow(y - cy, 2.0d))
    double maxRadius = (chart.coord as CoordPolar).getMaxRadius()
    assertTrue(dist > maxRadius)
  }
}
