package gg.coord

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.coord.CoordRadial
import se.alipsa.matrix.gg.scale.ScaleContinuous

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class CoordRadialTest {

  @Test
  void testDefaultAngularSpan() {
    CoordRadial coord = new CoordRadial()
    assertEquals(2 * Math.PI, coord.getAngularSpan() as double, 0.0001)
  }

  @Test
  void testPartialCircleRange() {
    CoordRadial coord = new CoordRadial(start: 0, end: Math.PI)
    assertEquals(Math.PI, coord.getAngularSpan() as double, 0.0001)
    List<BigDecimal> range = coord.getAngularRange()
    assertEquals(0.0, range[0] as double, 0.0001)
    assertEquals(Math.PI, range[1] as double, 0.0001)
  }

  @Test
  void testInnerRadiusPixels() {
    CoordRadial coord = new CoordRadial(innerRadius: 0.25)
    coord.plotWidth = 400
    coord.plotHeight = 400
    BigDecimal expected = coord.getMaxRadius() * 0.25
    assertEquals(expected as double, coord.getInnerRadiusPx() as double, 0.01)
  }

  @Test
  void testDirectionAffectsTransform() {
    CoordRadial clockwise = new CoordRadial(theta: 'x')
    CoordRadial anticlockwise = new CoordRadial(theta: 'x', direction: -1)
    clockwise.plotWidth = 200
    clockwise.plotHeight = 200
    anticlockwise.plotWidth = 200
    anticlockwise.plotHeight = 200

    List<Number> cw = clockwise.transform(0.25, 0.5, [:])
    List<Number> ccw = anticlockwise.transform(0.25, 0.5, [:])

    assertNotEquals(cw[0] as double, ccw[0] as double)
  }

  @Test
  void testTextRotation() {
    CoordRadial coord = new CoordRadial(rotateAngle: true)
    BigDecimal rotation = coord.getTextRotation(0.25)
    assertEquals(90.0, rotation as double, 0.0001)
  }

  @Test
  void testTransformInverseRoundTrip() {
    CoordRadial coord = new CoordRadial()
    coord.plotWidth = 300
    coord.plotHeight = 300

    ScaleContinuous xScale = new ScaleContinuous()
    xScale.train([0, 10])
    xScale.setRange([0, 300])

    ScaleContinuous yScale = new ScaleContinuous()
    yScale.train([0, 10])
    yScale.setRange([300, 0])

    Map<String, Object> scales = [x: xScale, y: yScale]
    List<Number> point = coord.transform(5, 5, scales)
    List<Number> inverse = coord.inverse(point[0], point[1], scales)

    assertEquals(5.0, inverse[0] as double, 0.1)
    assertEquals(5.0, inverse[1] as double, 0.1)
  }

  @Test
  void testRenderRadialPoint() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [0, 1],
            [1, 2],
            [2, 3]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        coord_radial()

    String svgContent = SvgWriter.toXml(chart.render())
    assertTrue(svgContent.contains('<svg'))
  }

  @Test
  void testRenderRadialLine() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [0, 1],
            [1, 3],
            [2, 2]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line() +
        coord_radial()

    String svgContent = SvgWriter.toXml(chart.render())
    assertTrue(svgContent.contains('<svg'))
  }

  @Test
  void testRenderRadialBar() {
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([
            ['A', 10],
            ['B', 20],
            ['C', 30]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'value', fill: 'category')) +
        geom_bar(stat: 'identity') +
        coord_radial(theta: 'y', innerRadius: 0.3)

    String svgContent = SvgWriter.toXml(chart.render())
    assertTrue(svgContent.contains('<path'))
  }
}
