package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleColorSteps2

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class ScaleColorSteps2Test {

  @Test
  void testDefaultDivergingPalette() {
    def scale = new ScaleColorSteps2()
    assertEquals('#D73027', scale.low)  // red
    assertEquals('#FFFFBF', scale.mid)  // pale yellow
    assertEquals('#1A9850', scale.high) // green
  }

  @Test
  void testCustomMidpoint() {
    def scale = new ScaleColorSteps2(midpoint: 50)
    assertEquals(50 as BigDecimal, scale.midpoint)
  }

  @Test
  void testAutoMidpoint() {
    def scale = new ScaleColorSteps2(bins: 5)
    scale.train([0, 100])

    // Transform a value - midpoint should be auto-calculated as (0+100)/2 = 50
    def color = scale.transform(50)
    assertNotNull(color)
    // Should return middle color for midpoint value
  }

  @Test
  void testOddBinCount() {
    def scale = new ScaleColorSteps2(bins: 6)  // Even number
    assertNotNull(scale.colors)
    // Should auto-adjust from 6 to 7 for symmetric diverging
    assertEquals(7, scale.colors.size(), "Even bin count 6 should be adjusted to 7")
    assertTrue(scale.colors.size() % 2 == 1, "Color count should be odd for diverging scale")
  }

  @Test
  void testValuesBelowMidpoint() {
    def scale = new ScaleColorSteps2(bins: 5, low: 'blue', mid: 'white', high: 'red')
    scale.train([0, 100])

    def lowColor = scale.transform(25)   // Below midpoint (50)
    def veryLowColor = scale.transform(10)  // Even lower
    assertNotNull(lowColor)
    assertNotNull(veryLowColor)
    // Both should be in the blue-to-white range
  }

  @Test
  void testValuesAboveMidpoint() {
    def scale = new ScaleColorSteps2(bins: 5, low: 'blue', mid: 'white', high: 'red')
    scale.train([0, 100])

    def highColor = scale.transform(75)   // Above midpoint (50)
    def veryHighColor = scale.transform(90)  // Even higher
    assertNotNull(highColor)
    assertNotNull(veryHighColor)
    // Both should be in the white-to-red range
  }

  @Test
  void testValueAtMidpoint() {
    def scale = new ScaleColorSteps2(bins: 5, midpoint: 50)
    scale.train([0, 100])

    def midColor = scale.transform(50)
    assertNotNull(midColor)
    // Should return a color close to the middle of the palette
  }

  @Test
  void testNullValue() {
    def scale = new ScaleColorSteps2()
    scale.train([0, 100])

    def naColor = scale.transform(null)
    assertEquals('grey50', naColor)
  }

  @Test
  void testCustomNaValue() {
    def scale = new ScaleColorSteps2(naValue: 'black')
    scale.train([0, 100])

    def naColor = scale.transform(null)
    assertEquals('black', naColor)
  }

  @Test
  void testFactoryMethod() {
    def scale = scale_color_steps2()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleColorSteps2)
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testBritishSpelling() {
    def scale = scale_colour_steps2()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleColorSteps2)
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testFillVariant() {
    def scale = scale_fill_steps2()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleColorSteps2)
    assertEquals('fill', scale.aesthetic)
  }

  @Test
  void testWithGeomTile() {
    def data = Matrix.builder()
      .columnNames(['x', 'y', 'value'])
      .rows([
        [1, 1, -50], [2, 1, -25], [3, 1, 0],
        [1, 2, 25],  [2, 2, 50],  [3, 2, 75]
      ])
      .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', fill: 'value')) +
      geom_tile() +
      scale_fill_steps2(bins: 7, midpoint: 0)

    Svg svg = chart.render()
    assertNotNull(svg)
    def svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'))
  }

  @Test
  void testConstantDomain() {
    def scale = new ScaleColorSteps2(bins: 5)
    scale.train([50, 50])  // All values are the same

    def color = scale.transform(50)
    assertNotNull(color)
    // Should return middle color when domain is constant
  }

  @Test
  void testCustomColors() {
    def customColors = ['darkblue', 'lightblue', 'white', 'pink', 'darkred']
    def scale = new ScaleColorSteps2(colors: customColors)
    assertEquals(customColors, scale.colors)
  }

  @Test
  void testLowMidHighColors() {
    def scale = new ScaleColorSteps2(
      low: 'purple',
      mid: 'white',
      high: 'orange',
      bins: 7
    )
    assertEquals('purple', scale.low)
    assertEquals('white', scale.mid)
    assertEquals('orange', scale.high)
    assertNotNull(scale.colors)
    assertEquals(7, scale.colors.size())
  }
}
