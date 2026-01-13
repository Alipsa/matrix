package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleColorFermenter

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class ScaleColorFermenterTest {

  @Test
  void testBasicFermenterScale() {
    // Test: Basic fermenter scale with default palette
    def scale = new ScaleColorFermenter([palette: 'Blues'])

    assertNotNull(scale)
    assertEquals('color', scale.aesthetic)
    assertEquals('Blues', scale.palette)
    assertEquals(-1, scale.direction)
  }

  @Test
  void testSequentialPalette() {
    // Test: Sequential palette (Blues) loads correctly
    def scale = new ScaleColorFermenter([type: 'seq', palette: 'Blues'])
    def colors = scale.getColors()

    assertNotNull(colors)
    assertFalse(colors.isEmpty())
    // Blues palette has 9 colors
    assertTrue(colors.size() <= 9)
  }

  @Test
  void testDivergingPalette() {
    // Test: Diverging palette (Spectral) with default type
    def scale = new ScaleColorFermenter([type: 'div'])
    def colors = scale.getColors()

    assertNotNull(colors)
    assertFalse(colors.isEmpty())
    // Should default to Spectral for diverging
  }

  @Test
  void testDirectionReversal() {
    // Test: Direction parameter reverses color order
    def scaleNormal = new ScaleColorFermenter([palette: 'Reds', direction: 1])
    def scaleReversed = new ScaleColorFermenter([palette: 'Reds', direction: -1])

    def colorsNormal = scaleNormal.getColors()
    def colorsReversed = scaleReversed.getColors()

    assertEquals(colorsNormal.size(), colorsReversed.size())
    assertEquals(colorsNormal.first(), colorsReversed.last())
    assertEquals(colorsNormal.last(), colorsReversed.first())
  }

  @Test
  void testNBreaksParameter() {
    // Test: n.breaks parameter limits number of bins
    def scale = new ScaleColorFermenter([palette: 'Blues', 'n.breaks': 5])

    scale.train([1, 2, 3, 4, 5, 6, 7, 8, 9, 10])
    def colors = scale.getColors()

    assertEquals(5, colors.size(), "Should use exactly 5 colors when n.breaks=5")
  }

  @Test
  void testTransformMapsValuesToBins() {
    // Test: Transform maps continuous values to discrete color bins
    def scale = new ScaleColorFermenter([palette: 'Greens', 'n.breaks': 3])
    scale.train([0, 10, 20, 30])

    // Values in first bin
    String color1 = scale.transform(0)
    String color2 = scale.transform(5)
    assertEquals(color1, color2, "Values in same bin should have same color")

    // Values in different bins
    String color3 = scale.transform(15)
    assertNotEquals(color1, color3, "Values in different bins should have different colors")
  }

  @Test
  void testNAValue() {
    // Test: NA values return naValue color
    def scale = new ScaleColorFermenter([palette: 'Blues', naValue: '#FF00FF'])
    scale.train([1, 2, 3])

    assertEquals('#FF00FF', scale.transform(null))
  }

  @Test
  void testWithPlot() {
    // Test: Fermenter scale works in actual plot
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'value'])
        .rows([
            [1, 1, 10],
            [2, 1, 20],
            [3, 1, 30],
            [1, 2, 40],
            [2, 2, 50],
            [3, 2, 60]
        ])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', fill: 'value')) +
                geom_tile() +
                scale_fill_fermenter(palette: 'YlOrRd', type: 'seq')

    def svg = chart.render()
    String svgContent = SvgWriter.toXml(svg)

    assertTrue(svgContent.contains('<svg'))
    assertTrue(svgContent.contains('</svg>'))
  }

  @Test
  void testColorVsFillAesthetic() {
    // Test: Both color and fill aesthetics work
    def scaleColor = scale_color_fermenter([palette: 'Blues'])
    def scaleFill = scale_fill_fermenter([palette: 'Blues'])

    assertEquals('color', scaleColor.aesthetic)
    assertEquals('fill', scaleFill.aesthetic)
  }

  @Test
  void testBritishSpelling() {
    // Test: British spelling alias works
    def scale = scale_colour_fermenter([palette: 'Spectral'])

    assertNotNull(scale)
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testPaletteTypeDefaults() {
    // Test: Palette type defaults work correctly
    def scaleSeq = new ScaleColorFermenter([type: 'seq'])
    def scaleDiv = new ScaleColorFermenter([type: 'div'])
    def scaleQual = new ScaleColorFermenter([type: 'qual'])

    // Should default to type-appropriate palettes
    assertNotNull(scaleSeq.getColors())
    assertNotNull(scaleDiv.getColors())
    assertNotNull(scaleQual.getColors())
  }

  @Test
  void testWithDivergingData() {
    // Test: Diverging palette with data centered around zero
    def data = Matrix.builder()
        .columnNames(['x', 'y', 'value'])
        .rows([
            [1, 1, -10],
            [2, 1, -5],
            [3, 1, 0],
            [4, 1, 5],
            [5, 1, 10]
        ])
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', fill: 'value')) +
                geom_tile() +
                scale_fill_fermenter(type: 'div', palette: 'RdBu', direction: 1)

    def svg = chart.render()
    assertNotNull(svg)
  }
}
