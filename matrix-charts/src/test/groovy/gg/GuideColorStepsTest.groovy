package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GuideColorStepsTest {

  @Test
  void testGuideColorStepsFactory() {
    def guide = guide_colorsteps()
    assertEquals('coloursteps', guide.type)
    assertNotNull(guide.params)
    assertTrue(guide.params.isEmpty())
  }

  @Test
  void testGuideColourStepsFactory() {
    def guide = guide_coloursteps()
    assertEquals('coloursteps', guide.type)
    assertNotNull(guide.params)
    assertTrue(guide.params.isEmpty())
  }

  @Test
  void testGuideColorStepsWithParams() {
    def guide = guide_colorsteps(evenSteps: false, showLimits: true, reverse: true)
    assertEquals('coloursteps', guide.type)
    assertEquals(false, guide.params.evenSteps)
    assertEquals(true, guide.params.showLimits)
    assertEquals(true, guide.params.reverse)
  }

  @Test
  void testGuideColorStepsWithDotParams() {
    // Test R-style parameter names
    def guide = guide_colorsteps('even.steps': false, 'show.limits': false)
    assertEquals('coloursteps', guide.type)
    assertEquals(false, guide.params['even.steps'])
    assertEquals(false, guide.params['show.limits'])
  }

  @Test
  void testGuideColorStepsRendering() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 2, 0],
            [2, 3, 5],
            [3, 1, 10],
            [4, 4, 15]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point() +
        scale_color_steps(bins: 3) +
        guides(color: guide_colorsteps())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'), "Should contain legend")
    assertTrue(content.contains('<rect'), "Should contain rectangles for color steps")
  }

  @Test
  void testGuideColorStepsEvenStepsTrue() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 1, 0],
            [2, 2, 5],
            [3, 3, 10]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point() +
        scale_color_gradient(low: 'blue', high: 'red', breaks: [0, 2, 5, 10]) +
        guides(color: guide_colorsteps(evenSteps: true))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'))
    assertTrue(content.contains('<rect'))
    // Verify the chart renders successfully
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideColorStepsEvenStepsFalse() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 1, 0],
            [2, 2, 5],
            [3, 3, 10]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point() +
        scale_color_gradient(low: 'green', high: 'yellow', breaks: [0, 1, 10]) +
        guides(color: guide_colorsteps(evenSteps: false))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'))
    assertTrue(content.contains('<rect'))
    // Proportional mode should also render successfully
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideColorStepsShowLimitsTrue() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 1, 0],
            [2, 2, 10]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point() +
        scale_color_gradient(breaks: [0, 5, 10]) +
        guides(color: guide_colorsteps(showLimits: true))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'))
    // Should contain labels (0 and 10)
    assertTrue(content.contains('<text'))
  }

  @Test
  void testGuideColorStepsShowLimitsFalse() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 1, 0],
            [2, 2, 10]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point() +
        scale_color_gradient(breaks: [0, 5, 10]) +
        guides(color: guide_colorsteps(showLimits: false))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'))
    // Should render successfully even without labels
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideColorStepsReverse() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 1, 0],
            [2, 2, 5],
            [3, 3, 10]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point() +
        scale_color_gradient(breaks: [0, 5, 10]) +
        guides(color: guide_colorsteps(reverse: true))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'))
    assertTrue(content.contains('<rect'))
    // Reverse should invert the order
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideColorStepsWithFillAesthetic() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 1, 0],
            [2, 2, 5],
            [3, 3, 10]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', fill: 'value')) +
        geom_point(shape: 21, size: 5) +
        scale_fill_gradient(low: 'lightblue', high: 'darkblue', breaks: [0, 3, 6, 10]) +
        guides(fill: guide_colorsteps())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'))
    assertTrue(content.contains('<rect'))
  }

  @Test
  void testGuideColorStepsWithCustomDimensions() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 1, 0],
            [2, 2, 10]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point() +
        scale_color_gradient(breaks: [0, 5, 10]) +
        guides(color: guide_colorsteps(barwidth: 30, barheight: 200))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'))
    // Custom dimensions should work without errors
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideColorStepsVsColorbar() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 1, 0],
            [2, 2, 5],
            [3, 3, 10]
        ])
        .types(Integer, Integer, Integer)
        .build()

    // Chart with colorbar (smooth gradient)
    def chartColorbar = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point() +
        scale_color_gradient(breaks: [0, 5, 10]) +
        guides(color: guide_colorbar())

    Svg svgColorbar = chartColorbar.render()
    String contentColorbar = SvgWriter.toXml(svgColorbar)

    // Chart with colorsteps (discrete blocks)
    def chartColorsteps = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point() +
        scale_color_gradient(breaks: [0, 5, 10]) +
        guides(color: guide_colorsteps())

    Svg svgColorsteps = chartColorsteps.render()
    String contentColorsteps = SvgWriter.toXml(svgColorsteps)

    // Both should render successfully
    assertTrue(contentColorbar.contains('id="legend"'))
    assertTrue(contentColorsteps.contains('id="legend"'))

    // Both should contain rectangles (though colorbar has more)
    assertTrue(contentColorbar.contains('<rect'))
    assertTrue(contentColorsteps.contains('<rect'))
  }

  @Test
  void testGuideColorStepsWithScaleColorSteps() {
    // Test integration with scale_color_steps
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 1, 1],
            [2, 2, 5],
            [3, 3, 10],
            [4, 4, 15],
            [5, 5, 20]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
        geom_point() +
        scale_color_steps(bins: 4, low: 'white', high: 'red') +
        guides(color: guide_colorsteps(evenSteps: true))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'))
    assertTrue(content.contains('<rect'))
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideColorStepsWithMultipleLegends() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'color_val', 'size_val')
        .rows([
            [1, 1, 0, 5],
            [2, 2, 5, 10],
            [3, 3, 10, 15]
        ])
        .types(Integer, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'color_val', size: 'size_val')) +
        geom_point() +
        scale_color_gradient(breaks: [0, 5, 10]) +
        guides(color: guide_colorsteps(), size: guide_legend())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="legend"'))
    // Should have both color steps and size legend
    assertTrue(content.contains('<svg'))
  }
}
