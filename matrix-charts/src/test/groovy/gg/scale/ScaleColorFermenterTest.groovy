package gg.scale

import org.junit.jupiter.api.Test
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
    assertEquals(9, colors.size(), "Blues palette should have exactly 9 colors")
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
  void testWarningWhenNBreaksExceedsPaletteSize() {
    // Test: Warning is issued when nBreaks exceeds palette size
    // Capture stderr output
    def originalErr = System.err
    def errStream = new ByteArrayOutputStream()
    def scale = null
    def errOutput = null

    try {
      System.err = new PrintStream(errStream)

      // Blues palette has 9 colors - request 15 breaks
      scale = new ScaleColorFermenter([palette: 'Blues', 'n.breaks': 15])

      // Convert stderr output to string
      errOutput = errStream.toString()

    } finally {
      // Restore stderr BEFORE assertions
      System.err = originalErr
    }

    // Perform assertions after System.err is restored
    // This ensures proper cleanup even if assertions fail
    assertTrue(errOutput.contains('Warning'), "Should contain warning message")
    assertTrue(errOutput.contains('Number of breaks (15)'), "Should mention requested 15 breaks")
    assertTrue(errOutput.contains('palette size (9)'), "Should mention palette size of 9")
    assertTrue(errOutput.contains('Using maximum of 9 colors'), "Should mention using maximum of 9 colors")

    // Should still work correctly - using max palette size
    def colors = scale.getColors()
    assertEquals(9, colors.size(), "Should use maximum palette size (9) instead of requested 15")
  }

  @Test
  void testNumericPaletteIndex() {
    // Test: Numeric palette index (1-based) works
    def scaleByName = new ScaleColorFermenter([type: 'seq', palette: 'Blues'])
    def scaleByIndex = new ScaleColorFermenter([type: 'seq', palette: 1])

    // Index 1 for sequential should give Blues
    assertEquals(scaleByName.getColors(), scaleByIndex.getColors(),
                 "Palette index 1 should resolve to 'blues' for sequential type")
  }

  @Test
  void testNumericPaletteIndexDiverging() {
    // Test: Numeric index for diverging palettes
    def scale = new ScaleColorFermenter([type: 'div', palette: 3])

    // Index 3 for diverging should give PrGn
    def colors = scale.getColors()
    assertNotNull(colors)
    assertFalse(colors.isEmpty())
  }

  @Test
  void testInvalidNumericPaletteIndex() {
    // Test: Invalid numeric palette index shows warning and uses default
    def originalErr = System.err
    def errStream = new ByteArrayOutputStream()
    def scale = null
    def errOutput = null

    try {
      System.err = new PrintStream(errStream)

      // Index 999 is out of range for any type
      scale = new ScaleColorFermenter([type: 'seq', palette: 999])

      errOutput = errStream.toString()

    } finally {
      System.err = originalErr
    }

    // Should have warning about out of range
    assertTrue(errOutput.contains('Warning'), "Should warn about invalid palette index")
    assertTrue(errOutput.contains('out of range'), "Should mention index is out of range")

    // Should still create valid scale with default palette
    def colors = scale.getColors()
    assertNotNull(colors)
    assertFalse(colors.isEmpty())
  }

}
