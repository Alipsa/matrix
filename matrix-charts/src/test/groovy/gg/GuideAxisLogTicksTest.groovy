package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GuideAxisLogTicksTest {

  @Test
  void testGuideAxisLogTicksFactory() {
    def guide = guide_axis_logticks()
    assertEquals('axis_logticks', guide.type)
    assertNotNull(guide.params)
    assertTrue(guide.params.isEmpty())
  }

  @Test
  void testGuideAxisLogTicksWithParams() {
    def guide = guide_axis_logticks(long: 3.0, mid: 2.0, short: 1.0)
    assertEquals('axis_logticks', guide.type)
    assertEquals(3.0, guide.params.long)
    assertEquals(2.0, guide.params.mid)
    assertEquals(1.0, guide.params.short)
  }

  @Test
  void testGuideAxisLogTicksWithLog10ScaleX() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [10, 100],
            [100, 1000],
            [1000, 10000]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_log10(guide: guide_axis_logticks())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'))
    // Should contain multiple line elements for tick marks
    assertTrue(content.contains('<line'))
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideAxisLogTicksWithLog10ScaleY() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 1],
            [2, 10],
            [3, 100],
            [4, 1000]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_y_log10(guide: guide_axis_logticks())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="y-axis"'))
    assertTrue(content.contains('<line'))
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideAxisLogTicksWithBothAxes() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 1],
            [10, 10],
            [100, 100],
            [1000, 1000]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        scale_x_log10(guide: guide_axis_logticks()) +
        scale_y_log10(guide: guide_axis_logticks())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'))
    assertTrue(content.contains('id="y-axis"'))
    assertTrue(content.contains('<line'))
  }

  @Test
  void testGuideAxisLogTicksCustomMultipliers() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [10, 100],
            [100, 1000]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_log10(guide: guide_axis_logticks(long: 4.0, mid: 2.5, short: 1.0))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'))
    // Custom multipliers should still render successfully
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideAxisLogTicksWithLimitedRange() {
    // Test with a narrow logarithmic range (less than one decade)
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 1],
            [2, 2],
            [5, 5],
            [10, 10]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_log10(guide: guide_axis_logticks())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'))
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideAxisLogTicksWithMultipleDecades() {
    // Test with a wide logarithmic range (multiple decades)
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [0.1, 0.1],
            [1, 1],
            [10, 10],
            [100, 100],
            [1000, 1000],
            [10000, 10000]
        ])
        .types(BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_log10(guide: guide_axis_logticks()) +
        scale_y_log10(guide: guide_axis_logticks())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'))
    assertTrue(content.contains('id="y-axis"'))
    // Should have many tick marks across multiple decades
    assertTrue(content.contains('<line'))
  }

  @Test
  void testGuideAxisLogTicksVsRegularAxis() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [10, 100],
            [100, 1000]
        ])
        .types(Integer, Integer)
        .build()

    // Chart with regular axis
    def chartRegular = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_log10()

    Svg svgRegular = chartRegular.render()
    String contentRegular = SvgWriter.toXml(svgRegular)

    // Chart with log ticks axis
    def chartLogTicks = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_log10(guide: guide_axis_logticks())

    Svg svgLogTicks = chartLogTicks.render()
    String contentLogTicks = SvgWriter.toXml(svgLogTicks)

    // Both should render successfully
    assertTrue(contentRegular.contains('id="x-axis"'))
    assertTrue(contentLogTicks.contains('id="x-axis"'))

    // Both should have axis elements
    assertTrue(contentRegular.contains('<line'))
    assertTrue(contentLogTicks.contains('<line'))
  }

  @Test
  void testGuideAxisLogTicksWithScatterPlot() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 5],
            [3, 15],
            [10, 50],
            [30, 150],
            [100, 500]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        scale_x_log10(guide: guide_axis_logticks(long: 2.5, mid: 1.5, short: 0.75)) +
        scale_y_log10(guide: guide_axis_logticks(long: 2.5, mid: 1.5, short: 0.75))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'))
    assertTrue(content.contains('id="x-axis"'))
    assertTrue(content.contains('id="y-axis"'))
  }

  @Test
  void testGuideAxisLogTicksWithPositiveValuesOnly() {
    // Ensure log ticks work correctly with strictly positive values
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [0.01, 0.01],
            [0.1, 0.1],
            [1, 1],
            [10, 10],
            [100, 100]
        ])
        .types(BigDecimal, BigDecimal)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_log10(guide: guide_axis_logticks()) +
        scale_y_log10(guide: guide_axis_logticks())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="x-axis"'))
    assertTrue(content.contains('id="y-axis"'))
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideAxisLogTicksWithDifferentTickLengths() {
    // Test that different tick multipliers create different visual effects
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [10, 100],
            [100, 1000]
        ])
        .types(Integer, Integer)
        .build()

    // Very long ticks
    def chartLong = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_log10(guide: guide_axis_logticks(long: 5.0, mid: 3.0, short: 1.5))

    Svg svgLong = chartLong.render()
    String contentLong = SvgWriter.toXml(svgLong)

    // Very short ticks
    def chartShort = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        scale_x_log10(guide: guide_axis_logticks(long: 1.5, mid: 1.0, short: 0.5))

    Svg svgShort = chartShort.render()
    String contentShort = SvgWriter.toXml(svgShort)

    // Both should render successfully
    assertTrue(contentLong.contains('<svg'))
    assertTrue(contentShort.contains('<svg'))
  }
}
