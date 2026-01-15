package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*
import static se.alipsa.groovy.svg.io.SvgWriter.toXml

class GeomBarPolarTest {

  @Test
  void testPolarSliceOrderThetaY() {
    def data = Matrix.builder()
        .columnNames(['group', 'fill', 'value'])
        .rows([
            ['all', 'A', 10],
            ['all', 'B', 20]
        ])
        .build()

    GgChart chart = ggplot(data, aes(x: 'group', y: 'value', fill: 'fill')) +
        geom_bar(stat: 'identity') +
        coord_polar(theta: 'y') +
        scale_fill_manual(values: ['#111111', '#222222'])

    def svg = chart.render()
    String svgText = toXml(svg)
    new File('build/polar_bar_order.svg').text = svgText
    def groupMatcher = (svgText =~ /(?s)<g[^>]*class=['"]geombar['"][^>]*>(.*?)<\/g>/)
    assertTrue(groupMatcher.find())
    String groupText = groupMatcher.group(1)
    def allFills = []
    def fillMatcher = (groupText =~ /fill=['"]([^'"]+)['"]/)
    fillMatcher.each { allFills << it[1].toLowerCase() }
    allFills = allFills.findAll { it && it != 'none' }

    def scale = scale_fill_manual(values: ['#111111', '#222222'])
    scale.train(['A', 'B'])
    def expected = [
      (scale.transform('B') as String).toLowerCase(),
      (scale.transform('A') as String).toLowerCase()
    ]

    assertEquals(2, allFills.size(), "fills=${allFills}")
    assertEquals(expected, allFills)
  }

  @Test
  void testPolarSlicesSkipZeroValues() {
    def data = Matrix.builder()
        .columnNames(['group', 'fill', 'value'])
        .rows([
            ['G1', 'A', 10],
            ['G1', 'B', 0],
            ['G2', 'A', 5],
            ['G2', 'B', 7]
        ])
        .build()

    GgChart chart = ggplot(data, aes(x: 'group', y: 'value', fill: 'fill')) +
        geom_bar(stat: 'identity') +
        coord_polar(theta: 'y') +
        scale_fill_manual(values: ['#111111', '#222222'])

    def svg = chart.render()
    String svgText = toXml(svg)
    new File('build/polar_bar_zero.svg').text = svgText
    def groupMatcher = (svgText =~ /(?s)<g[^>]*class=['"]geombar['"][^>]*>(.*?)<\/g>/)
    assertTrue(groupMatcher.find())
    String groupText = groupMatcher.group(1)
    def allFills = []
    def fillMatcher = (groupText =~ /fill=['"]([^'"]+)['"]/)
    fillMatcher.each { allFills << it[1].toLowerCase() }
    allFills = allFills.findAll { it && it != 'none' }
    assertEquals(3, allFills.size(), "fills=${allFills}")
  }
}
