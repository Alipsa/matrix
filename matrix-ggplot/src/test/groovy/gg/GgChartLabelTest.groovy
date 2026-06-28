package gg

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Text
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.GgPlot
import se.alipsa.matrix.gg.Label

@SuppressWarnings('ExplicitCallToPlusMethod')
class GgChartLabelTest {

  private final Matrix legendData = Matrix.builder()
      .columnNames(['x', 'y', 'group', 'kind'])
      .rows([
          [1, 2, 'one', 'alpha'],
          [2, 4, 'two', 'beta'],
          [3, 6, 'one', 'alpha'],
          [4, 8, 'two', 'beta'],
      ])
      .types([int, int, String, String])
      .build()

  @Test
  void testLabelSettersToggleFlags() {
    def label = new Label()
    label.x = 'X'
    label.y = 'Y'

    assertTrue(label.xSet)
    assertTrue(label.ySet)
  }

  @Test
  void testLabelMergeRespectsExplicitBlank() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 2]])
        .build()
    GgChart chart = GgPlot.ggplot(data, GgPlot.aes('x', 'y'))

    def initial = new Label()
    initial.x = 'X'
    initial.y = 'Y'
    chart.plus(initial)

    def blank = new Label()
    blank.x = null
    chart.plus(blank)

    assertNull(chart.labels.x)
    assertEquals('Y', chart.labels.y)
  }

  @Test
  void testLabelMergeIgnoresUnsetAxes() {
    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 2]])
        .build()
    GgChart chart = GgPlot.ggplot(data, GgPlot.aes('x', 'y'))

    def initial = new Label()
    initial.x = 'X'
    initial.y = 'Y'
    chart.plus(initial)

    def update = new Label()
    update.title = 'Title'
    chart.plus(update)

    assertEquals('X', chart.labels.x)
    assertEquals('Y', chart.labels.y)
    assertEquals('Title', chart.labels.title)
  }

  @Test
  void testLabsPreservesColorAndFillLegendTitles() {
    Label label = GgPlot.labs(color: 'Speed', fill: 'Count')

    assertEquals('Speed', label.legendTitles['color'])
    assertEquals('Count', label.legendTitles['fill'])
  }

  @Test
  void testSeparateLabsCallsPreserveLegendTitles() {
    GgChart chart = GgPlot.ggplot(legendData, GgPlot.aes(x: 'x', y: 'y', color: 'group', fill: 'kind')) +
        GgPlot.labs(color: 'Speed') +
        GgPlot.labs(fill: 'Count')

    assertEquals('Speed', chart.labels.legendTitles['color'])
    assertEquals('Count', chart.labels.legendTitles['fill'])
  }

  @Test
  void testSingleAestheticLegendTitleStillWorks() {
    Label label = GgPlot.labs(color: 'Hue')

    assertEquals('Hue', label.legendTitles['color'])
  }

  @Test
  void testAxisLabelAndLegendTitleAreIndependent() {
    Label label = GgPlot.labs(x: 'X Axis', color: 'Speed')

    assertEquals('X Axis', label.x)
    assertEquals('Speed', label.legendTitles['color'])
    assertFalse(label.legendTitles.containsKey('x'))
  }

  @Test
  void testLabelLevelKeysAreNotStoredAsLegendTitles() {
    Label label = GgPlot.labs(title: 'T', subtitle: 'S', caption: 'C', x: 'X', y: 'Y', color: 'Hue')

    assertEquals('Hue', label.legendTitles['color'])
    assertFalse(label.legendTitles.containsKey('title'))
    assertFalse(label.legendTitles.containsKey('subtitle'))
    assertFalse(label.legendTitles.containsKey('caption'))
    assertFalse(label.legendTitles.containsKey('x'))
    assertFalse(label.legendTitles.containsKey('y'))
  }

  @Test
  void testGuideLegendTitleWinsOverLabsTitle() {
    Svg svg = (GgPlot.ggplot(legendData, GgPlot.aes(x: 'x', y: 'y', color: 'group')) +
        GgPlot.geom_point() +
        GgPlot.labs(color: 'Labs Legend') +
        GgPlot.guides(color: GgPlot.guide_legend(title: 'Guide Legend'))).render()

    String text = svgText(svg)
    assertTrue(text.contains('Guide Legend'), text)
    assertFalse(text.contains('Labs Legend'), text)
  }

  @Test
  void testScaleLegendTitleWinsOverLabsTitle() {
    Svg svg = (GgPlot.ggplot(legendData, GgPlot.aes(x: 'x', y: 'y', color: 'group')) +
        GgPlot.geom_point() +
        GgPlot.labs(color: 'Labs Legend') +
        GgPlot.scale_color_manual(name: 'Scale Legend', values: [one: '#336699', two: '#CC6633'])).render()

    String text = svgText(svg)
    assertTrue(text.contains('Scale Legend'), text)
    assertFalse(text.contains('Labs Legend'), text)
  }

  private static String svgText(Svg svg) {
    svg.descendants()
        .findAll { it instanceof Text }
        .collect { (it as Text).content }
        .join(' ')
  }
}
