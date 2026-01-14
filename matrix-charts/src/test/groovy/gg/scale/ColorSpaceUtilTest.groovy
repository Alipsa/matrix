package gg.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ColorSpaceUtil

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for ColorSpaceUtil - HCL color space conversion utilities.
 */
class ColorSpaceUtilTest {

  @Test
  void testHclToHexBasic() {
    // Test that HCL conversion produces valid hex colors
    String color = ColorSpaceUtil.hclToHex(0.0d, 100.0d, 65.0d)
    assertNotNull(color)
    assertTrue(color.matches(/#[0-9A-F]{6}/), "Color should be hex format: ${color}")
  }

  @Test
  void testHclToHexLuminanceZero() {
    // Luminance of 0 should produce black
    String color = ColorSpaceUtil.hclToHex(0.0d, 100.0d, 0.0d)
    assertEquals('#000000', color)
  }

  @Test
  void testHclToHexVariousHues() {
    // Test several hues around the color wheel
    String red = ColorSpaceUtil.hclToHex(0.0d, 100.0d, 65.0d)
    String green = ColorSpaceUtil.hclToHex(120.0d, 100.0d, 65.0d)
    String blue = ColorSpaceUtil.hclToHex(240.0d, 100.0d, 65.0d)

    assertNotNull(red)
    assertNotNull(green)
    assertNotNull(blue)

    // All should be valid hex colors
    assertTrue(red.matches(/#[0-9A-F]{6}/))
    assertTrue(green.matches(/#[0-9A-F]{6}/))
    assertTrue(blue.matches(/#[0-9A-F]{6}/))

    // All should be different
    assertNotEquals(red, green)
    assertNotEquals(green, blue)
    assertNotEquals(blue, red)
  }

  @Test
  void testHclToHexChromaVariation() {
    // Test different chroma values
    String lowChroma = ColorSpaceUtil.hclToHex(15.0d, 10.0d, 65.0d)
    String highChroma = ColorSpaceUtil.hclToHex(15.0d, 150.0d, 65.0d)

    assertNotNull(lowChroma)
    assertNotNull(highChroma)

    assertTrue(lowChroma.matches(/#[0-9A-F]{6}/))
    assertTrue(highChroma.matches(/#[0-9A-F]{6}/))

    // Different chroma should produce different colors
    assertNotEquals(lowChroma, highChroma)
  }

  @Test
  void testHclToHexLuminanceVariation() {
    // Test different luminance values
    String dark = ColorSpaceUtil.hclToHex(15.0d, 100.0d, 20.0d)
    String light = ColorSpaceUtil.hclToHex(15.0d, 100.0d, 90.0d)

    assertNotNull(dark)
    assertNotNull(light)

    assertTrue(dark.matches(/#[0-9A-F]{6}/))
    assertTrue(light.matches(/#[0-9A-F]{6}/))

    // Different luminance should produce different colors
    assertNotEquals(dark, light)
  }

  @Test
  void testHclToHexConsistency() {
    // Same inputs should produce same output
    String color1 = ColorSpaceUtil.hclToHex(15.0d, 100.0d, 65.0d)
    String color2 = ColorSpaceUtil.hclToHex(15.0d, 100.0d, 65.0d)

    assertEquals(color1, color2)
  }

  @Test
  void testHclToHexGgplot2DefaultFirstColor() {
    // Test the first color from ggplot2's default hue scale
    // ggplot2 default: h=[15,375], c=100, l=65, first color at h=15
    String color = ColorSpaceUtil.hclToHex(15.0d, 100.0d, 65.0d)

    // Should be a pinkish-red color (ggplot2's first default color)
    assertNotNull(color)
    assertTrue(color.matches(/#[0-9A-F]{6}/))

    // ggplot2 first color is #F8766D - verify exact match
    assertEquals('#F8766D', color.toUpperCase())
  }
}
