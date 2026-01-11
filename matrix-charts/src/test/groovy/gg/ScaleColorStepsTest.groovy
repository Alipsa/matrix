package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleColorSteps

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class ScaleColorStepsTest {

  @Test
  void testDefaultBins() {
    def scale = new ScaleColorSteps()
    assertEquals(5, scale.bins)
  }

  @Test
  void testCustomBins() {
    def scale = new ScaleColorSteps(bins: 10)
    assertEquals(10, scale.bins)
  }

  @Test
  void testCustomColors() {
    def colors = ['red', 'yellow', 'green']
    def scale = new ScaleColorSteps(colors: colors)
    assertEquals(colors, scale.colors)
  }

  @Test
  void testTransformMinValue() {
    def scale = new ScaleColorSteps(bins: 5)
    scale.train([0, 100])

    def minColor = scale.transform(0)
    assertNotNull(minColor)
    // Should be the first bin's color
  }

  @Test
  void testTransformMaxValue() {
    def scale = new ScaleColorSteps(bins: 5)
    scale.train([0, 100])

    def maxColor = scale.transform(100)
    assertNotNull(maxColor)
    // Should be the last bin's color
  }

  @Test
  void testTransformMidValue() {
    def scale = new ScaleColorSteps(bins: 5)
    scale.train([0, 100])

    def midColor = scale.transform(50)
    assertNotNull(midColor)
    // Should be a middle bin's color
  }

  @Test
  void testTransformReturnsDiscreteColors() {
    def scale = new ScaleColorSteps(bins: 3)
    scale.train([0, 100])

    // Values in same bin should have same color
    def color1 = scale.transform(10)
    def color2 = scale.transform(20)
    assertEquals(color1, color2)  // Both in bin 0

    def color3 = scale.transform(50)
    assertNotEquals(color1, color3)  // Different bin
  }

  @Test
  void testNullValue() {
    def scale = new ScaleColorSteps()
    scale.train([0, 100])

    def naColor = scale.transform(null)
    assertEquals('grey50', naColor)
  }

  @Test
  void testCustomNaValue() {
    def scale = new ScaleColorSteps(naValue: 'red')
    scale.train([0, 100])

    def naColor = scale.transform(null)
    assertEquals('red', naColor)
  }

  @Test
  void testFactoryMethod() {
    def scale = scale_color_steps()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleColorSteps)
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testBritishSpelling() {
    def scale = scale_colour_steps()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleColorSteps)
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testFillVariant() {
    def scale = scale_fill_steps()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleColorSteps)
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
      scale_color_steps(bins: 3)

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
      scale_fill_steps(bins: 5)

    Svg svg = chart.render()
    assertNotNull(svg)
    def svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'))
  }

  @Test
  void testLowHighColors() {
    def scale = new ScaleColorSteps(low: 'white', high: 'darkblue', bins: 5)
    assertEquals('white', scale.low)
    assertEquals('darkblue', scale.high)
    assertNotNull(scale.colors)
    assertEquals(5, scale.colors.size())
  }

  @Test
  void testConstantDomain() {
    def scale = new ScaleColorSteps(bins: 5)
    scale.train([50, 50])  // All values are the same

    def color = scale.transform(50)
    assertNotNull(color)
    // Should return middle color when domain is constant
  }

  @Test
  void testFluentAPI() {
    def scale = new ScaleColorSteps()
      .bins(7)
      .colors(['red', 'yellow', 'green'])

    assertEquals(7, scale.bins)
    assertEquals(['red', 'yellow', 'green'], scale.colors)
  }
}
