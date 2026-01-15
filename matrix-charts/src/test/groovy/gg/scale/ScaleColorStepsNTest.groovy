package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleColorStepsN

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class ScaleColorStepsNTest {

  @Test
  void testCustomColorList() {
    def colors = ['red', 'yellow', 'green', 'blue']
    def scale = new ScaleColorStepsN(colors: colors)
    assertEquals(colors, scale.colors)
  }

  @Test
  void testCustomColorPositions() {
    def colors = ['red', 'yellow', 'green', 'blue']
    def values = [0.0, 0.25, 0.75, 1.0]
    def scale = new ScaleColorStepsN(colors: colors, values: values as List<BigDecimal>)
    assertEquals(colors, scale.colors)
    assertEquals(values.collect { it as BigDecimal }, scale.values)
  }

  @Test
  void testAutoPositions() {
    def colors = ['red', 'yellow', 'green']
    def scale = new ScaleColorStepsN(colors: colors)
    assertNotNull(scale.values)
    assertEquals(3, scale.values.size())
    // Should be equally spaced: [0.0, 0.5, 1.0]
    // Use compareTo for BigDecimal comparison to handle different scales
    assertEquals(0, (scale.values[0] as BigDecimal).compareTo(0.0 as BigDecimal))
    assertEquals(0, (scale.values[1] as BigDecimal).compareTo(0.5 as BigDecimal))
    assertEquals(0, (scale.values[2] as BigDecimal).compareTo(1.0 as BigDecimal))
  }

  @Test
  void testInterpolationBetweenColors() {
    def scale = new ScaleColorStepsN(
      bins: 5,
      colors: ['red', 'blue']
    )
    scale.train([0, 100])

    def color1 = scale.transform(0)
    def color5 = scale.transform(100)
    def color3 = scale.transform(50)

    assertNotNull(color1)
    assertNotNull(color3)
    assertNotNull(color5)
    // Colors should be interpolated between red and blue
  }

  @Test
  void testSingleColor() {
    def scale = new ScaleColorStepsN(colors: ['red'])
    scale.train([0, 100])

    def color = scale.transform(50)
    assertEquals('red', color)
  }

  @Test
  void testTwoColors() {
    def scale = new ScaleColorStepsN(
      bins: 5,
      colors: ['red', 'blue']
    )
    scale.train([0, 100])

    def minColor = scale.transform(0)
    def maxColor = scale.transform(100)
    assertNotNull(minColor)
    assertNotNull(maxColor)
  }

  @Test
  void testNullValue() {
    def scale = new ScaleColorStepsN()
    scale.train([0, 100])

    def naColor = scale.transform(null)
    assertEquals('grey50', naColor)
  }

  @Test
  void testCustomNaValue() {
    def scale = new ScaleColorStepsN(naValue: 'pink')
    scale.train([0, 100])

    def naColor = scale.transform(null)
    assertEquals('pink', naColor)
  }

  @Test
  void testFactoryMethod() {
    def scale = scale_color_stepsn()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleColorStepsN)
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testBritishSpelling() {
    def scale = scale_colour_stepsn()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleColorStepsN)
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testFillVariant() {
    def scale = scale_fill_stepsn()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleColorStepsN)
    assertEquals('fill', scale.aesthetic)
  }

  @Test
  void testWithGeomPoint() {
    def data = Matrix.builder()
      .columnNames(['x', 'y', 'value'])
      .rows([
        [1, 2, 10],
        [2, 4, 30],
        [3, 6, 50],
        [4, 8, 70],
        [5, 10, 90]
      ])
      .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', color: 'value')) +
      geom_point() +
      scale_color_stepsn(bins: 5, colors: ['red', 'yellow', 'green', 'blue'])

    Svg svg = chart.render()
    assertNotNull(svg)
    def svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'))
  }

  @Test
  void testWithGeomTile() {
    def data = Matrix.builder()
      .columnNames(['x', 'y', 'value'])
      .rows([
        [1, 1, 10], [2, 1, 30], [3, 1, 50],
        [1, 2, 70], [2, 2, 90], [3, 2, 100]
      ])
      .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', fill: 'value')) +
      geom_tile() +
      scale_fill_stepsn(bins: 6, colors: ['#440154', '#31688e', '#35b779', '#fde724'])

    Svg svg = chart.render()
    assertNotNull(svg)
    def svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'))
  }

  @Test
  void testConstantDomain() {
    def scale = new ScaleColorStepsN(bins: 5, colors: ['red', 'yellow', 'green'])
    scale.train([50, 50])  // All values are the same

    def color = scale.transform(50)
    assertNotNull(color)
    // Should return middle color when domain is constant
  }

  @Test
  void testEmptyColors() {
    def scale = new ScaleColorStepsN(colors: [])
    scale.train([0, 100])

    def color = scale.transform(50)
    assertEquals('grey50', color)  // Should return naValue
  }

  @Test
  void testFluentAPI() {
    def scale = new ScaleColorStepsN()
      .colors(['purple', 'pink', 'orange'])
      .values([0.0, 0.3, 1.0] as List<BigDecimal>)

    assertEquals(['purple', 'pink', 'orange'], scale.colors)
    assertEquals([0.0, 0.3, 1.0].collect { it as BigDecimal }, scale.values)
  }

  @Test
  void testViridisPalette() {
    // Test with a Viridis-like palette
    def scale = new ScaleColorStepsN(
      bins: 10,
      colors: ['#440154', '#31688e', '#35b779', '#fde724']
    )
    scale.train([0, 100])

    def color = scale.transform(50)
    assertNotNull(color)
  }
}
