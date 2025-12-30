package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleColorGradient

import static org.junit.jupiter.api.Assertions.*

class ScaleColorGradientTest {

  @Test
  void testDefaultAesthetic() {
    ScaleColorGradient scale = new ScaleColorGradient()
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testDefaultColors() {
    ScaleColorGradient scale = new ScaleColorGradient()

    // Default is dark blue to light blue
    assertEquals('#132B43', scale.low)
    assertEquals('#56B1F7', scale.high)
    assertNull(scale.mid)
  }

  @Test
  void testTransformReturnsLowColorAtMinimum() {
    ScaleColorGradient scale = new ScaleColorGradient(low: '#000000', high: '#FFFFFF')
    scale.train([0, 100])

    String color = scale.transform(0) as String

    assertEquals('#000000', color)
  }

  @Test
  void testTransformReturnsHighColorAtMaximum() {
    ScaleColorGradient scale = new ScaleColorGradient(low: '#000000', high: '#FFFFFF')
    scale.train([0, 100])

    String color = scale.transform(100) as String

    assertEquals('#FFFFFF', color)
  }

  @Test
  void testTransformReturnsMidpointColor() {
    ScaleColorGradient scale = new ScaleColorGradient(low: '#000000', high: '#FFFFFF')
    scale.train([0, 100])

    String color = scale.transform(50) as String

    // Midpoint between black and white should be gray (#808080 or close)
    assertEquals('#808080', color)
  }

  @Test
  void testTransformInterpolatesCorrectly() {
    // Red to blue gradient
    ScaleColorGradient scale = new ScaleColorGradient(low: '#FF0000', high: '#0000FF')
    scale.train([0, 100])

    String colorAt25 = scale.transform(25) as String
    String colorAt75 = scale.transform(75) as String

    // At 25%, should be more red than blue
    // At 75%, should be more blue than red
    assertNotEquals(colorAt25, colorAt75)

    // At 25%: R=191 (255*0.75), G=0, B=64 (255*0.25)
    assertEquals('#BF0040', colorAt25)

    // At 75%: R=64, G=0, B=191
    assertEquals('#4000BF', colorAt75)
  }

  @Test
  void testTransformWithDivergingScale() {
    ScaleColorGradient scale = new ScaleColorGradient(
        low: '#FF0000',   // Red for low
        mid: '#FFFFFF',   // White for midpoint
        high: '#0000FF',  // Blue for high
        midpoint: 50
    )
    scale.train([0, 100])

    String atLow = scale.transform(0) as String
    String atMid = scale.transform(50) as String
    String atHigh = scale.transform(100) as String

    assertEquals('#FF0000', atLow)
    assertEquals('#FFFFFF', atMid)
    assertEquals('#0000FF', atHigh)
  }

  @Test
  void testTransformWithDivergingScaleInterpolates() {
    ScaleColorGradient scale = new ScaleColorGradient(
        low: '#FF0000',
        mid: '#FFFFFF',
        high: '#0000FF',
        midpoint: 50
    )
    scale.train([0, 100])

    // At 25%, should be between red and white (pinkish)
    String at25 = scale.transform(25) as String
    assertTrue(at25.startsWith('#FF'), "Should have high red component")

    // At 75%, should be between white and blue (light blue)
    String at75 = scale.transform(75) as String
    assertTrue(at75.endsWith('FF'), "Should have high blue component")
  }

  @Test
  void testTransformClampsValues() {
    ScaleColorGradient scale = new ScaleColorGradient(low: '#000000', high: '#FFFFFF')
    scale.train([0, 100])

    // Values outside range should clamp
    String belowMin = scale.transform(-50) as String
    String aboveMax = scale.transform(150) as String

    assertEquals('#000000', belowMin)
    assertEquals('#FFFFFF', aboveMax)
  }

  @Test
  void testTransformWithNullReturnsNaColor() {
    ScaleColorGradient scale = new ScaleColorGradient(naValue: '#CCCCCC')
    scale.train([0, 100])

    assertEquals('#CCCCCC', scale.transform(null))
  }

  @Test
  void testTransformWithNonNumericReturnsNaColor() {
    ScaleColorGradient scale = new ScaleColorGradient(naValue: '#AAAAAA')
    scale.train([0, 100])

    assertEquals('#AAAAAA', scale.transform('not a number'))
  }

  @Test
  void testWithParamsConstructor() {
    ScaleColorGradient scale = new ScaleColorGradient(
        low: '#FF0000',
        high: '#00FF00',
        name: 'Temperature',
        limits: [0, 50]
    )

    assertEquals('#FF0000', scale.low)
    assertEquals('#00FF00', scale.high)
    assertEquals('Temperature', scale.name)
    assertEquals([0, 50], scale.limits)
  }

  @Test
  void testFluentApi() {
    ScaleColorGradient scale = new ScaleColorGradient()
        .low('#FF0000')
        .high('#0000FF')
        .mid('#FFFFFF', 0)
        .limits(-10, 10)

    assertEquals('#FF0000', scale.low)
    assertEquals('#0000FF', scale.high)
    assertEquals('#FFFFFF', scale.mid)
    assertEquals(0, scale.midpoint)
    assertEquals([-10, 10], scale.limits)
  }

  @Test
  void testParseHexColors() {
    ScaleColorGradient scale = new ScaleColorGradient(low: '#ABC', high: '#DEF')
    scale.train([0, 100])

    // Short hex should work
    String color = scale.transform(0) as String
    assertNotNull(color)
    assertTrue(color.startsWith('#'))
  }

  @Test
  void testParseNamedColors() {
    ScaleColorGradient scale = new ScaleColorGradient(low: 'red', high: 'blue')
    scale.train([0, 100])

    String atLow = scale.transform(0) as String
    String atHigh = scale.transform(100) as String

    assertEquals('#FF0000', atLow)
    assertEquals('#0000FF', atHigh)
  }

  @Test
  void testVariousNamedColors() {
    ScaleColorGradient scale = new ScaleColorGradient()
    scale.train([0, 100])

    // Test various named colors work
    scale.low = 'white'
    scale.high = 'black'
    assertEquals('#FFFFFF', scale.transform(0))
    assertEquals('#000000', scale.transform(100))

    scale.low = 'green'
    scale.high = 'yellow'
    String greenResult = scale.transform(0) as String
    assertNotNull(greenResult)
  }

  @Test
  void testAestheticCanBeSetToFill() {
    ScaleColorGradient scale = new ScaleColorGradient(aesthetic: 'fill')
    assertEquals('fill', scale.aesthetic)
  }

  @Test
  void testBritishSpellingColourMapsToColor() {
    ScaleColorGradient scale = new ScaleColorGradient(aesthetic: 'colour')
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testMapBatchTransform() {
    ScaleColorGradient scale = new ScaleColorGradient(low: '#000000', high: '#FFFFFF')
    scale.train([0, 100])

    List colors = scale.map([0, 50, 100])

    assertEquals(3, colors.size())
    assertEquals('#000000', colors[0])
    assertEquals('#808080', colors[1])
    assertEquals('#FFFFFF', colors[2])
  }

  @Test
  void testHandlesEqualDomain() {
    ScaleColorGradient scale = new ScaleColorGradient(low: '#000000', high: '#FFFFFF')
    scale.train([50, 50, 50])  // All same value

    // Should return interpolated color (likely mid or one of low/high)
    String color = scale.transform(50) as String
    assertNotNull(color)
  }
}
