package gg.scale

import gg.BaseTest
import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleColorViridisC

import static org.junit.jupiter.api.Assertions.*

class ScaleColorViridisCTest {

  @Test
  void testDefaultValues() {
    ScaleColorViridisC scale = new ScaleColorViridisC()
    assertEquals('color', scale.aesthetic)
    assertEquals('viridis', scale.option)
    assertEquals(0.0d, scale.begin, 0.001d)
    assertEquals(1.0d, scale.end, 0.001d)
    assertEquals(1, scale.direction)
    assertEquals(1.0d, scale.alpha, 0.001d)
    assertEquals('grey50', scale.naValue)
    BaseTest.assertEquals([0, 0], scale.expand)
  }

  @Test
  void testPaletteOptionsProduceColors() {
    ['viridis', 'magma', 'inferno', 'plasma', 'cividis', 'rocket', 'mako', 'turbo'].each { option ->
      ScaleColorViridisC scale = new ScaleColorViridisC(option: option)
      scale.train([0, 1])
      String color = scale.transform(0.5d) as String
      assertNotNull(color, "Palette ${option} should produce a color")
      assertTrue(color.startsWith('#'), "Palette ${option} should produce hex colors")
    }
  }

  @Test
  void testBeginEndAffectsRange() {
    ScaleColorViridisC defaultScale = new ScaleColorViridisC()
    defaultScale.train([0, 1])
    String defaultMin = defaultScale.transform(0) as String

    ScaleColorViridisC partialScale = new ScaleColorViridisC(begin: 0.2, end: 0.8)
    partialScale.train([0, 1])
    String partialMin = partialScale.transform(0) as String

    assertNotEquals(defaultMin, partialMin)
  }

  @Test
  void testDirectionReversalMatchesEnds() {
    ScaleColorViridisC forward = new ScaleColorViridisC(direction: 1)
    forward.train([0, 1])
    String forwardMin = forward.transform(0) as String
    String forwardMax = forward.transform(1) as String

    ScaleColorViridisC reverse = new ScaleColorViridisC(direction: -1)
    reverse.train([0, 1])
    String reverseMin = reverse.transform(0) as String
    String reverseMax = reverse.transform(1) as String

    assertEquals(forwardMax, reverseMin)
    assertEquals(forwardMin, reverseMax)
  }

  @Test
  void testEqualMinMaxReturnsStableColor() {
    ScaleColorViridisC scale = new ScaleColorViridisC()
    scale.train([5, 5])
    String colorA = scale.transform(5) as String
    String colorB = scale.transform(10) as String

    assertNotNull(colorA)
    assertTrue(colorA.startsWith('#'))
    assertEquals(colorA, colorB)
  }

  @Test
  void testInterpolationAcrossDomain() {
    ScaleColorViridisC scale = new ScaleColorViridisC()
    scale.train([0, 10])

    String minColor = scale.transform(0) as String
    String midColor = scale.transform(5) as String
    String maxColor = scale.transform(10) as String

    assertNotEquals(minColor, midColor)
    assertNotEquals(midColor, maxColor)
  }

  @Test
  void testBeginGreaterThanEndThrowsException() {
    Exception exception = assertThrows(IllegalArgumentException.class, {
      new ScaleColorViridisC(begin: 0.8, end: 0.2)
    })
    assertTrue(exception.message.contains('begin'))
    assertTrue(exception.message.contains('end'))
  }
}
