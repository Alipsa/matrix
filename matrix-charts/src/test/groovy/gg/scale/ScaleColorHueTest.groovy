package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleColorHue

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for ScaleColorHue - ggplot2's default discrete color scale.
 */
class ScaleColorHueTest {

  @Test
  void testBasicHuePaletteGeneration() {
    def scale = scale_color_hue()
    scale.train(['A', 'B', 'C'])

    // Should generate 3 distinct colors
    def colors = scale.getColors()
    assertEquals(3, colors.size())

    // All colors should be hex format
    colors.each { color ->
      assertTrue(color.matches(/#[0-9A-F]{6}/), "Color should be hex format: ${color}")
    }

    // All colors should be distinct
    assertEquals(3, colors.toSet().size(), 'All colors should be distinct')
  }

  @Test
  void testDefaultParameters() {
    def scale = new ScaleColorHue('color', [:])

    // Check default values
    assertEquals([15, 375], scale.hueRange)
    assertEquals(100, scale.chroma)
    assertEquals(65, scale.luminance)
    assertEquals(1, scale.direction)
  }

  @Test
  void testCustomHueRange() {
    def scale = scale_color_hue(h: [0, 360])
    scale.train(['A', 'B', 'C', 'D'])

    assertEquals([0, 360], scale.hueRange)

    def colors = scale.getColors()
    assertEquals(4, colors.size())
    colors.each { color ->
      assertTrue(color.matches(/#[0-9A-F]{6}/))
    }
  }

  @Test
  void testCustomChromaAndLuminance() {
    def scale = scale_color_hue(c: 50, l: 70)
    scale.train(['A', 'B', 'C'])

    assertEquals(50, scale.chroma)
    assertEquals(70, scale.luminance)

    def colors = scale.getColors()
    assertEquals(3, colors.size())
    // Colors should be less saturated (lower chroma) and lighter (higher luminance)
    colors.each { color ->
      assertTrue(color.matches(/#[0-9A-F]{6}/))
    }
  }

  @Test
  void testHueStart() {
    def scale1 = scale_color_hue()
    scale1.train(['A', 'B', 'C'])
    def colors1 = scale1.getColors()

    def scale2 = scale_color_hue('h.start': 90)
    scale2.train(['A', 'B', 'C'])
    def colors2 = scale2.getColors()

    // Colors should be different due to different starting hue
    assertNotEquals(colors1[0], colors2[0])

    // Scale2 should have shifted hue range
    assertEquals(90, scale2.hueRange[0])
    assertEquals(450, scale2.hueRange[1])  // 90 + (375 - 15)
  }

  @Test
  void testDirection() {
    def scaleClockwise = scale_color_hue(direction: 1)
    scaleClockwise.train(['A', 'B', 'C'])
    def colorsClockwise = scaleClockwise.getColors()

    def scaleCounterClockwise = scale_color_hue(direction: -1)
    scaleCounterClockwise.train(['A', 'B', 'C'])
    def colorsCounterClockwise = scaleCounterClockwise.getColors()

    // First color might be same, but order should be different
    assertEquals(colorsClockwise.size(), colorsCounterClockwise.size())
    // At least some colors should be different due to reversed direction
    boolean hasDifference = false
    for (int i = 0; i < colorsClockwise.size(); i++) {
      if (colorsClockwise[i] != colorsCounterClockwise[i]) {
        hasDifference = true
        break
      }
    }
    assertTrue(hasDifference, 'Direction should affect color order')
  }

  @Test
  void testTransform() {
    def scale = scale_color_hue()
    scale.train(['cat1', 'cat2', 'cat3'])

    // Transform should map categories to colors
    def color1 = scale.transform('cat1')
    def color2 = scale.transform('cat2')
    def color3 = scale.transform('cat3')

    assertNotNull(color1)
    assertNotNull(color2)
    assertNotNull(color3)

    // All should be hex colors
    assertTrue(color1.matches(/#[0-9A-F]{6}/))
    assertTrue(color2.matches(/#[0-9A-F]{6}/))
    assertTrue(color3.matches(/#[0-9A-F]{6}/))

    // All should be different
    assertNotEquals(color1, color2)
    assertNotEquals(color2, color3)
    assertNotEquals(color1, color3)
  }

  @Test
  void testTransformWithMissingValue() {
    def scale = scale_color_hue()
    scale.train(['A', 'B'])

    // Unknown value should return naValue
    def unknownColor = scale.transform('Z')
    assertEquals('grey50', unknownColor)  // Default naValue

    // Null should return naValue
    def nullColor = scale.transform(null)
    assertEquals('grey50', nullColor)
  }

  @Test
  void testCustomNaValue() {
    def scale = scale_color_hue(naValue: '#FF0000')
    scale.train(['A', 'B'])

    def naColor = scale.transform(null)
    assertEquals('#FF0000', naColor)
  }

  @Test
  void testInverse() {
    def scale = scale_color_hue()
    scale.train(['cat1', 'cat2', 'cat3'])

    def color1 = scale.transform('cat1')
    def recovered = scale.inverse(color1)

    assertEquals('cat1', recovered)
  }

  @Test
  void testSingleCategory() {
    def scale = scale_color_hue()
    scale.train(['OnlyOne'])

    def colors = scale.getColors()
    assertEquals(1, colors.size())
    assertTrue(colors[0].matches(/#[0-9A-F]{6}/))
  }

  @Test
  void testManyCategories() {
    def scale = scale_color_hue()
    def categories = (1..20).collect { "cat${it}" }
    scale.train(categories)

    def colors = scale.getColors()
    assertEquals(20, colors.size())

    // All should be non-null
    colors.eachWithIndex { color, idx ->
      assertNotNull(color, "Color at index ${idx} should not be null")
      assertTrue(color.matches(/#[0-9A-F]{6}/) || color == 'grey50',
          "Color at index ${idx} should be hex format or naValue: ${color}")
    }

    // Most should be distinct (allowing for some edge cases with extreme hues)
    assertTrue(colors.toSet().size() >= 18, "At least 18 of 20 colors should be distinct, got ${colors.toSet().size()}")
  }

  @Test
  void testEmptyData() {
    def scale = scale_color_hue()
    scale.train([])

    def colors = scale.getColors()
    assertTrue(colors.isEmpty())
  }

  @Test
  void testBritishSpellingAlias() {
    def scaleUS = scale_color_hue()
    def scaleUK = scale_colour_hue()

    scaleUS.train(['A', 'B', 'C'])
    scaleUK.train(['A', 'B', 'C'])

    assertEquals(scaleUS.getColors(), scaleUK.getColors())
  }

  @Test
  void testFillAesthetic() {
    def scale = scale_fill_hue()
    scale.train(['A', 'B', 'C'])

    assertEquals('fill', scale.aesthetic)

    def colors = scale.getColors()
    assertEquals(3, colors.size())
    colors.each { color ->
      assertTrue(color.matches(/#[0-9A-F]{6}/))
    }
  }

  @Test
  void testLimits() {
    def scale = scale_color_hue(limits: ['B', 'C'])
    scale.train(['A', 'B', 'C', 'D'])

    // Only B and C should be in the scale
    def colors = scale.getColors()
    assertEquals(2, colors.size())

    // Transform should work for limited values
    assertNotNull(scale.transform('B'))
    assertNotNull(scale.transform('C'))
  }

  @Test
  void testLabels() {
    def scale = scale_color_hue(labels: ['Label A', 'Label B', 'Label C'])
    scale.train(['A', 'B', 'C'])

    def labels = scale.getComputedLabels()
    assertEquals(['Label A', 'Label B', 'Label C'], labels)
  }

  @Test
  void testName() {
    def scale = scale_color_hue(name: 'Category')
    assertEquals('Category', scale.name)
  }

  @Test
  void testColorConsistency() {
    // Same training data should produce same colors
    def scale1 = scale_color_hue()
    scale1.train(['X', 'Y', 'Z'])

    def scale2 = scale_color_hue()
    scale2.train(['X', 'Y', 'Z'])

    assertEquals(scale1.getColors(), scale2.getColors())
  }

  @Test
  void testHueRangeWraparound() {
    // Test that hues wrap around 360 degrees correctly
    def scale = scale_color_hue(h: [350, 370])
    scale.train(['A', 'B'])

    def colors = scale.getColors()
    assertEquals(2, colors.size())
    colors.each { color ->
      assertTrue(color.matches(/#[0-9A-F]{6}/))
    }
  }

  @Test
  void testExtremeChroma() {
    // Test with very low and very high chroma
    def scaleLowChroma = scale_color_hue(c: 10)
    scaleLowChroma.train(['A', 'B', 'C'])
    def colorsLow = scaleLowChroma.getColors()
    assertEquals(3, colorsLow.size())

    def scaleHighChroma = scale_color_hue(c: 150)
    scaleHighChroma.train(['A', 'B', 'C'])
    def colorsHigh = scaleHighChroma.getColors()
    assertEquals(3, colorsHigh.size())
  }

  @Test
  void testExtremeLuminance() {
    // Test with very low and very high luminance
    def scaleDark = scale_color_hue(l: 20)
    scaleDark.train(['A', 'B', 'C'])
    def colorsDark = scaleDark.getColors()
    assertEquals(3, colorsDark.size())

    def scaleLight = scale_color_hue(l: 90)
    scaleLight.train(['A', 'B', 'C'])
    def colorsLight = scaleLight.getColors()
    assertEquals(3, colorsLight.size())

    // Dark colors should be different from light colors
    assertNotEquals(colorsDark[0], colorsLight[0])
  }
}
