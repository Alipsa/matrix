package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.theme.Theme

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class LegendTest {

  // ============== Discrete Color Legend Tests ==============

  @Test
  void testDiscreteColorLegend() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'category')
        .rows([
            [1, 2, 'A'], [2, 4, 'A'], [3, 3, 'A'],
            [1, 3, 'B'], [2, 5, 'B'], [3, 4, 'B'],
            [1, 1, 'C'], [2, 2, 'C'], [3, 2, 'C']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'category')) +
        geom_point() +
        labs(title: 'Scatter with Color Legend', colour: 'Category')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)

    // Should contain legend group
    assertTrue(content.contains('id="legend"'), "Should contain legend group")

    // Should contain legend title
    assertTrue(content.contains('Category'), "Should contain legend title")

    // Should contain legend entries for A, B, C
    assertTrue(content.contains('>A<'), "Should contain label A")
    assertTrue(content.contains('>B<'), "Should contain label B")
    assertTrue(content.contains('>C<'), "Should contain label C")

    File outputFile = new File('build/legend_discrete_color.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testDiscreteFillLegend() {
    def data = Matrix.builder()
        .columnNames('group', 'count')
        .rows([
            ['Red', 10], ['Green', 15], ['Blue', 8]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'group', y: 'count', fill: 'group')) +
        geom_col() +
        labs(title: 'Bar Chart with Fill Legend', fill: 'Group')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'), "Should contain legend group")

    File outputFile = new File('build/legend_discrete_fill.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testManualColorLegend() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'type')
        .rows([
            [1, 2, 'Success'], [2, 4, 'Success'], [3, 3, 'Success'],
            [1, 1, 'Failure'], [2, 1.5, 'Failure'], [3, 2, 'Failure']
        ])
        .types(Integer, Double, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'type')) +
        geom_point() +
        scale_color_manual(values: ['green', 'red']) +
        labs(title: 'Manual Colors Legend')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('id="legend"'), "Should contain legend group")

    File outputFile = new File('build/legend_manual_colors.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============== Continuous Color Legend Tests ==============

  @Test
  void testContinuousColorLegend() {
    def random = new Random(42)
    def rows = []
    50.times {
      double x = random.nextDouble() * 10
      double y = random.nextDouble() * 10
      double value = x + y  // Continuous value
      rows << [x, y, value]
    }

    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows(rows)
        .types(Double, Double, Double)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point() +
        scale_color_gradient(low: 'blue', high: 'red') +
        labs(title: 'Continuous Color Legend')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('id="legend"'), "Should contain legend group")

    File outputFile = new File('build/legend_continuous_color.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============== Legend Position Tests ==============

  @Test
  void testLegendPositionRight() {
    def data = createCategoryData()
    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'cat')) +
        geom_point() +
        theme(legendPosition: 'right') +
        labs(title: 'Legend Right')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('id="legend"'), "Should contain legend")

    File outputFile = new File('build/legend_position_right.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testLegendPositionLeft() {
    def data = createCategoryData()
    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'cat')) +
        geom_point() +
        theme(legendPosition: 'left') +
        labs(title: 'Legend Left')

    Svg svg = chart.render()

    File outputFile = new File('build/legend_position_left.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testLegendPositionTop() {
    def data = createCategoryData()
    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'cat')) +
        geom_point() +
        theme(legendPosition: 'top', legendDirection: 'horizontal') +
        labs(title: 'Legend Top')

    Svg svg = chart.render()

    File outputFile = new File('build/legend_position_top.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testLegendPositionBottom() {
    def data = createCategoryData()
    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'cat')) +
        geom_point() +
        theme(legendPosition: 'bottom', legendDirection: 'horizontal') +
        labs(title: 'Legend Bottom')

    Svg svg = chart.render()

    File outputFile = new File('build/legend_position_bottom.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testLegendPositionNone() {
    def data = createCategoryData()
    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'cat')) +
        geom_point() +
        theme(legendPosition: 'none') +
        labs(title: 'No Legend')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Should NOT contain legend group
    assertFalse(content.contains('id="legend"'), "Should not contain legend when position is 'none'")

    File outputFile = new File('build/legend_position_none.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============== Legend with Different Geoms ==============

  @Test
  void testLegendWithLine() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'series')
        .rows([
            [1, 2, 'A'], [2, 4, 'A'], [3, 3, 'A'], [4, 5, 'A'],
            [1, 1, 'B'], [2, 3, 'B'], [3, 2, 'B'], [4, 4, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'series')) +
        geom_line() +
        labs(title: 'Line Chart with Legend')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('id="legend"'), "Should contain legend")

    File outputFile = new File('build/legend_with_line.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testLegendWithPointAndLine() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
            [1, 2, 'Treatment'], [2, 4, 'Treatment'], [3, 5, 'Treatment'],
            [1, 1, 'Control'], [2, 2, 'Control'], [3, 3, 'Control']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'group')) +
        geom_line() +
        geom_point() +
        labs(title: 'Point + Line with Legend')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('id="legend"'), "Should contain legend")

    File outputFile = new File('build/legend_point_and_line.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============== Legend Title Tests ==============

  @Test
  void testLegendWithCustomTitle() {
    def data = createCategoryData()
    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'cat')) +
        geom_point() +
        labs(title: 'Chart Title', colour: 'My Custom Legend')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('My Custom Legend'), "Should contain custom legend title")

    File outputFile = new File('build/legend_custom_title.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testLegendWithScaleName() {
    def data = createCategoryData()
    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'cat')) +
        geom_point() +
        scale_color_manual(values: ['red', 'green', 'blue'], name: 'Scale Name')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Scale name should be used as legend title
    assertTrue(content.contains('Scale Name'), "Should contain scale name as legend title")

    File outputFile = new File('build/legend_scale_name.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============== Helper Methods ==============

  private Matrix createCategoryData() {
    def rows = []
    ['X', 'Y', 'Z'].each { cat ->
      def random = new Random(cat.hashCode())
      10.times {
        rows << [random.nextDouble() * 10, random.nextDouble() * 10, cat]
      }
    }

    return Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows(rows)
        .types(Double, Double, String)
        .build()
  }
}
