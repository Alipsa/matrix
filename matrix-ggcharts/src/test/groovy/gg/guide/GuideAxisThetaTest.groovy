package gg.guide

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class GuideAxisThetaTest {

  @Test
  void testGuideAxisThetaFactory() {
    def guide = guide_axis_theta()
    assertEquals('axis_theta', guide.type)
    assertNotNull(guide.params)
  }

  @Test
  void testGuideAxisThetaWithParams() {
    def guide = guide_axis_theta(angle: 45, minorTicks: true, cap: 'round')
    assertEquals('axis_theta', guide.type)
    assertEquals(45, guide.params.angle)
    assertEquals(true, guide.params.minorTicks)
    assertEquals('round', guide.params.cap)
  }

  @Test
  void testGuideAxisThetaRendering() {
    def data = Matrix.builder()
        .columnNames('angle', 'radius')
        .rows([[0, 1], [90, 2], [180, 3], [270, 4]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'angle', y: 'radius')) +
        geom_point() +
        coord_polar(theta: 'x') +
        scale_x_continuous(guide: guide_axis_theta())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="theta-axis"'))
    assertTrue(content.contains('<line'))  // Tick marks
    assertTrue(content.contains('<text'))  // Labels
  }

  @Test
  void testGuideAxisThetaWithCap() {
    def data = Matrix.builder()
        .columnNames('angle', 'radius')
        .rows([[0, 1], [45, 2], [90, 3]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'angle', y: 'radius')) +
        geom_point() +
        coord_polar(theta: 'x') +
        scale_x_continuous(guide: guide_axis_theta(cap: 'round'))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'))  // Outer circle cap
  }

  @Test
  void testGuideAxisThetaWithClockwise() {
    // Test that clockwise direction works correctly
    def data = Matrix.builder()
        .columnNames('angle', 'radius')
        .rows([[0, 1], [90, 2], [180, 3]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'angle', y: 'radius')) +
        geom_point() +
        coord_polar(theta: 'x', clockwise: true) +
        scale_x_continuous(guide: guide_axis_theta())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="theta-axis"'))
  }

  @Test
  void testGuideAxisThetaWithYAsTheta() {
    // Test when theta is mapped to y instead of x
    def data = Matrix.builder()
        .columnNames('radius', 'angle')
        .rows([[1, 0], [2, 90], [3, 180]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'radius', y: 'angle')) +
        geom_point() +
        coord_polar(theta: 'y') +
        scale_y_continuous(guide: guide_axis_theta())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="theta-axis"'))
  }

  @Test
  void testGuideAxisThetaWithStartOffset() {
    // Test polar plot with non-zero start angle
    def data = Matrix.builder()
        .columnNames('angle', 'radius')
        .rows([[0, 1], [90, 2], [180, 3], [270, 4]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'angle', y: 'radius')) +
        geom_point() +
        coord_polar(theta: 'x', start: Math.PI / 2) +
        scale_x_continuous(guide: guide_axis_theta())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="theta-axis"'))
    assertTrue(content.contains('<line'))
  }

  @Test
  void testGuideAxisThetaNoCap() {
    def data = Matrix.builder()
        .columnNames('angle', 'radius')
        .rows([[0, 1], [90, 2]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'angle', y: 'radius')) +
        geom_point() +
        coord_polar(theta: 'x') +
        scale_x_continuous(guide: guide_axis_theta(cap: 'none'))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="theta-axis"'))
    // Should still have tick marks and labels
    assertTrue(content.contains('<line'))
    assertTrue(content.contains('<text'))
  }

  @Test
  void testGuideAxisThetaWithPolarBar() {
    // Test theta axis with bar chart in polar coordinates
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([['A', 10], ['B', 20], ['C', 15], ['D', 25]])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'value')) +
        geom_col() +
        coord_polar(theta: 'x') +
        scale_x_discrete(guide: guide_axis_theta())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="theta-axis"'))
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testGuideAxisThetaVsDefault() {
    def data = Matrix.builder()
        .columnNames('angle', 'radius')
        .rows([[0, 1], [90, 2], [180, 3]])
        .types(Integer, Integer)
        .build()

    // Chart without guide_axis_theta (default behavior)
    def chartDefault = ggplot(data, aes(x: 'angle', y: 'radius')) +
        geom_point() +
        coord_polar(theta: 'x')

    Svg svgDefault = chartDefault.render()
    String contentDefault = SvgWriter.toXml(svgDefault)

    // Chart with guide_axis_theta
    def chartTheta = ggplot(data, aes(x: 'angle', y: 'radius')) +
        geom_point() +
        coord_polar(theta: 'x') +
        scale_x_continuous(guide: guide_axis_theta())

    Svg svgTheta = chartTheta.render()
    String contentTheta = SvgWriter.toXml(svgTheta)

    // Default should not have theta-axis
    assertFalse(contentDefault.contains('id="theta-axis"'))

    // With guide_axis_theta should have theta-axis
    assertTrue(contentTheta.contains('id="theta-axis"'))
  }

  @Test
  void testGuideAxisThetaWithCustomAngle() {
    def data = Matrix.builder()
        .columnNames('angle', 'radius')
        .rows([[0, 1], [90, 2], [180, 3]])
        .types(Integer, Integer)
        .build()

    // Note: angle parameter is parsed but may not affect rendering in current implementation
    def chart = ggplot(data, aes(x: 'angle', y: 'radius')) +
        geom_point() +
        coord_polar(theta: 'x') +
        scale_x_continuous(guide: guide_axis_theta(angle: 30))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('id="theta-axis"'))
  }

  @Test
  void testGuideAxisThetaWithLineChart() {
    // Test theta axis with line chart in polar coordinates
    def data = Matrix.builder()
        .columnNames('angle', 'radius')
        .rows([[0, 1], [45, 1.5], [90, 2], [135, 1.8], [180, 1.2], [225, 1.6], [270, 2.2], [315, 1.9]])
        .types(Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes(x: 'angle', y: 'radius')) +
        geom_line() +
        coord_polar(theta: 'x') +
        scale_x_continuous(guide: guide_axis_theta(cap: 'round'))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    assertTrue(content.contains('id="theta-axis"'))
    assertTrue(content.contains('<circle'))  // Cap
    assertTrue(content.contains('<line'))    // Line chart renders as line elements in polar coords
  }
}
