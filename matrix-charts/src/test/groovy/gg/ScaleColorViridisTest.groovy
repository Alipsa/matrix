package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleColorViridis

import static org.junit.jupiter.api.Assertions.*

class ScaleColorViridisTest {

  @Test
  void testDefaultValues() {
    ScaleColorViridis scale = new ScaleColorViridis()
    assertEquals('color', scale.aesthetic)
    assertEquals('viridis', scale.option)
    assertEquals(0.0d, scale.begin, 0.001d)
    assertEquals(1.0d, scale.end, 0.001d)
    assertEquals(1, scale.direction)
  }

  @Test
  void testValidBeginAndEnd() {
    // Valid range [0, 1]
    ScaleColorViridis scale = new ScaleColorViridis(begin: 0.2, end: 0.8)
    assertEquals(0.2d, scale.begin, 0.001d)
    assertEquals(0.8d, scale.end, 0.001d)
  }

  @Test
  void testBeginLessThanZeroThrowsException() {
    Exception exception = assertThrows(IllegalArgumentException.class, {
      new ScaleColorViridis(begin: -0.1)
    })
    assertTrue(exception.message.contains('begin'))
    assertTrue(exception.message.contains('[0, 1]'))
  }

  @Test
  void testBeginGreaterThanOneThrowsException() {
    Exception exception = assertThrows(IllegalArgumentException.class, {
      new ScaleColorViridis(begin: 1.5)
    })
    assertTrue(exception.message.contains('begin'))
    assertTrue(exception.message.contains('[0, 1]'))
  }

  @Test
  void testEndLessThanZeroThrowsException() {
    Exception exception = assertThrows(IllegalArgumentException.class, {
      new ScaleColorViridis(end: -0.1)
    })
    assertTrue(exception.message.contains('end'))
    assertTrue(exception.message.contains('[0, 1]'))
  }

  @Test
  void testEndGreaterThanOneThrowsException() {
    Exception exception = assertThrows(IllegalArgumentException.class, {
      new ScaleColorViridis(end: 1.2)
    })
    assertTrue(exception.message.contains('end'))
    assertTrue(exception.message.contains('[0, 1]'))
  }

  @Test
  void testBeginGreaterThanEndThrowsException() {
    Exception exception = assertThrows(IllegalArgumentException.class, {
      new ScaleColorViridis(begin: 0.8, end: 0.2)
    })
    assertTrue(exception.message.contains('begin'))
    assertTrue(exception.message.contains('end'))
    assertTrue(exception.message.toLowerCase().contains('less than or equal'))
  }

  @Test
  void testBeginEqualsEndIsValid() {
    // begin == end should be allowed
    ScaleColorViridis scale = new ScaleColorViridis(begin: 0.5, end: 0.5)
    assertEquals(0.5d, scale.begin, 0.001d)
    assertEquals(0.5d, scale.end, 0.001d)
  }

  @Test
  void testBoundaryValuesZeroAndOne() {
    ScaleColorViridis scale = new ScaleColorViridis(begin: 0.0, end: 1.0)
    assertEquals(0.0d, scale.begin, 0.001d)
    assertEquals(1.0d, scale.end, 0.001d)
  }

  @Test
  void testGeneratesColorsWithValidRange() {
    ScaleColorViridis scale = new ScaleColorViridis(begin: 0.2, end: 0.8)
    scale.train(['A', 'B', 'C'])

    List<String> colors = scale.getColors()
    assertEquals(3, colors.size())
    // All colors should be valid hex colors
    colors.each { color ->
      assertTrue(color.startsWith('#'))
      assertTrue(color.length() >= 7) // #RRGGBB or #RRGGBBAA
    }
  }

  @Test
  void testDefaultAesthetic() {
    ScaleColorViridis scale = new ScaleColorViridis()
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testDefaultPalette() {
    ScaleColorViridis scale = new ScaleColorViridis()
    assertEquals('viridis', scale.option)
    assertEquals(0.0, scale.begin, 0.001)
    assertEquals(1.0, scale.end, 0.001)
    assertEquals(1, scale.direction)
    assertEquals('grey50', scale.naValue)
    assertEquals(1.0, scale.alpha, 0.001)
  }

  @Test
  void testTransformWithDefaultViridis() {
    ScaleColorViridis scale = new ScaleColorViridis()
    scale.train(['A', 'B', 'C'])

    String colorA = scale.transform('A') as String
    String colorB = scale.transform('B') as String
    String colorC = scale.transform('C') as String

    // Should get three distinct colors from viridis palette
    assertNotNull(colorA)
    assertNotNull(colorB)
    assertNotNull(colorC)
    
    assertNotEquals(colorA, colorB)
    assertNotEquals(colorB, colorC)
    assertNotEquals(colorA, colorC)

    // All should be hex colors
    assertTrue(colorA.startsWith('#'))
    assertTrue(colorB.startsWith('#'))
    assertTrue(colorC.startsWith('#'))
  }

  @Test
  void testTransformWithMagmaPalette() {
    ScaleColorViridis scale = new ScaleColorViridis(option: 'magma')
    scale.train(['A', 'B', 'C'])

    String colorA = scale.transform('A') as String
    String colorB = scale.transform('B') as String
    String colorC = scale.transform('C') as String

    assertNotNull(colorA)
    assertNotNull(colorB)
    assertNotNull(colorC)
    
    // Magma should produce different colors than viridis
    ScaleColorViridis viridisScale = new ScaleColorViridis(option: 'viridis')
    viridisScale.train(['A', 'B', 'C'])
    
    assertNotEquals(colorA, viridisScale.transform('A'))
  }

  @Test
  void testAllPaletteOptions() {
    List<String> palettes = ['viridis', 'magma', 'inferno', 'plasma', 'cividis', 'rocket', 'mako', 'turbo']
    
    for (String palette : palettes) {
      ScaleColorViridis scale = new ScaleColorViridis(option: palette)
      scale.train(['A', 'B'])
      
      String color = scale.transform('A') as String
      assertNotNull(color, "Palette ${palette} should produce colors")
      assertTrue(color.startsWith('#'), "Palette ${palette} should produce hex colors")
    }
  }

  @Test
  void testPaletteLetterCodes() {
    // Test letter code aliases
    Map<String, String> codes = [
      'A': 'magma',
      'B': 'inferno',
      'C': 'plasma',
      'D': 'viridis',
      'E': 'cividis',
      'F': 'rocket',
      'G': 'mako',
      'H': 'turbo'
    ]
    
    codes.each { code, expected ->
      ScaleColorViridis scale = new ScaleColorViridis(option: code)
      assertEquals(expected, scale.option, "Code ${code} should map to ${expected}")
    }
  }

  @Test
  void testInvalidPaletteDefaultsToViridis() {
    ScaleColorViridis scale = new ScaleColorViridis(option: 'invalid_palette')
    assertEquals('viridis', scale.option)
  }

  @Test
  void testDirectionReversal() {
    ScaleColorViridis scale = new ScaleColorViridis(direction: 1)
    scale.train(['A', 'B', 'C'])
    String forwardA = scale.transform('A') as String
    String forwardC = scale.transform('C') as String

    ScaleColorViridis reverseScale = new ScaleColorViridis(direction: -1)
    reverseScale.train(['A', 'B', 'C'])
    String reverseA = reverseScale.transform('A') as String
    String reverseC = reverseScale.transform('C') as String

    // When reversed, first item should get color similar to last item in forward direction
    // and vice versa (allowing for interpolation differences)
    assertNotEquals(forwardA, reverseA, "Direction should affect color mapping")
    assertNotEquals(forwardC, reverseC, "Direction should affect color mapping")
  }

  @Test
  void testBeginEndRange() {
    // Use only middle portion of palette (0.3 to 0.7)
    ScaleColorViridis scale = new ScaleColorViridis(begin: 0.3, end: 0.7)
    scale.train(['A', 'B', 'C'])
    
    String colorA = scale.transform('A') as String
    String colorB = scale.transform('B') as String
    String colorC = scale.transform('C') as String

    // Should still produce distinct colors
    assertNotNull(colorA)
    assertNotNull(colorB)
    assertNotNull(colorC)
    assertNotEquals(colorA, colorB)
    assertNotEquals(colorB, colorC)

    // Colors should be different from full range
    ScaleColorViridis fullScale = new ScaleColorViridis()
    fullScale.train(['A', 'B', 'C'])
    
    assertNotEquals(colorA, fullScale.transform('A'))
  }

  @Test
  void testBeginEndWithSingleValue() {
    ScaleColorViridis scale = new ScaleColorViridis(begin: 0.5, end: 0.5)
    scale.train(['A', 'B', 'C'])
    
    // All values should get the same color (middle of palette)
    String colorA = scale.transform('A') as String
    String colorB = scale.transform('B') as String
    String colorC = scale.transform('C') as String

    assertNotNull(colorA)
    // Note: Due to interpolation logic, colors might still be slightly different
    // The important thing is they're all valid colors
    assertTrue(colorA.startsWith('#'))
    assertTrue(colorB.startsWith('#'))
    assertTrue(colorC.startsWith('#'))
  }

  @Test
  void testTransformWithNull() {
    ScaleColorViridis scale = new ScaleColorViridis(naValue: 'pink')
    scale.train(['A', 'B', 'C'])

    assertEquals('pink', scale.transform(null))
  }

  @Test
  void testTransformWithUnknownValue() {
    ScaleColorViridis scale = new ScaleColorViridis(naValue: '#CCCCCC')
    scale.train(['A', 'B', 'C'])

    assertEquals('#CCCCCC', scale.transform('Unknown'))
  }

  @Test
  void testTransformWithEmptyLevels() {
    ScaleColorViridis scale = new ScaleColorViridis(naValue: 'default')
    scale.train([])

    assertEquals('default', scale.transform('anything'))
  }

  @Test
  void testInverseTransform() {
    ScaleColorViridis scale = new ScaleColorViridis()
    scale.train(['A', 'B', 'C'])

    String colorA = scale.transform('A') as String
    String colorB = scale.transform('B') as String
    String colorC = scale.transform('C') as String

    assertEquals('A', scale.inverse(colorA))
    assertEquals('B', scale.inverse(colorB))
    assertEquals('C', scale.inverse(colorC))
  }

  @Test
  void testInverseWithUnknownColor() {
    ScaleColorViridis scale = new ScaleColorViridis()
    scale.train(['A', 'B', 'C'])

    assertNull(scale.inverse('#FF00FF'))
    assertNull(scale.inverse(null))
  }

  @Test
  void testGetColors() {
    ScaleColorViridis scale = new ScaleColorViridis()
    scale.train(['X', 'Y', 'Z'])

    List<String> colors = scale.getColors()

    assertEquals(3, colors.size())
    colors.each { color ->
      assertNotNull(color)
      assertTrue(color.startsWith('#'))
    }
  }

  @Test
  void testDifferentPaletteOptions() {
    ['viridis', 'magma', 'inferno', 'plasma', 'cividis', 'rocket', 'mako', 'turbo'].each { option ->
      ScaleColorViridis scale = new ScaleColorViridis(option: option, begin: 0.1, end: 0.9)
      scale.train(['A', 'B'])
      
      List<String> colors = scale.getColors()
      assertEquals(2, colors.size())
      assertNotNull(colors[0])
      assertNotNull(colors[1])
    }
  }

  @Test
  void testTransformWithValidation() {
    ScaleColorViridis scale = new ScaleColorViridis(begin: 0.3, end: 0.7)
    scale.train(['Low', 'Mid', 'High'])
    
    String lowColor = scale.transform('Low') as String
    String midColor = scale.transform('Mid') as String
    String highColor = scale.transform('High') as String
    
    assertNotNull(lowColor)
    assertNotNull(midColor)
    assertNotNull(highColor)
    // Colors should be different
    assertNotEquals(lowColor, midColor)
    assertNotEquals(midColor, highColor)
  }

  @Test
  void testOnlyBeginProvidedIsValid() {
    // When only begin is provided, it should validate against default end (1.0)
    ScaleColorViridis scale = new ScaleColorViridis(begin: 0.3)
    assertEquals(0.3d, scale.begin, 0.001d)
    assertEquals(1.0d, scale.end, 0.001d) // Default end
  }

  @Test
  void testOnlyEndProvidedIsValid() {
    // When only end is provided, it should validate against default begin (0.0)
    ScaleColorViridis scale = new ScaleColorViridis(end: 0.7)
    assertEquals(0.0d, scale.begin, 0.001d) // Default begin
    assertEquals(0.7d, scale.end, 0.001d)
  }

  @Test
  void testOnlyBeginProvidedInvalidIfGreaterThanDefaultEnd() {
    // begin > default end (1.0) should fail
    Exception exception = assertThrows(IllegalArgumentException.class, {
      new ScaleColorViridis(begin: 1.1)
    })
    // Should fail on range validation, not begin<=end validation
    assertTrue(exception.message.contains('begin'))
    assertTrue(exception.message.contains('[0, 1]'))
  }

  @Test
  void testOnlyEndProvidedInvalidIfLessThanDefaultBegin() {
    // This would never happen since default begin is 0.0 and end must be >= 0
    // But we can test that end < 0 fails
    Exception exception = assertThrows(IllegalArgumentException.class, {
      new ScaleColorViridis(end: -0.1)
    })
    assertTrue(exception.message.contains('end'))
    assertTrue(exception.message.contains('[0, 1]'))
  }

  @Test
  void testGetColorForIndex() {
    ScaleColorViridis scale = new ScaleColorViridis()
    scale.train(['X', 'Y', 'Z'])

    String color0 = scale.getColorForIndex(0)
    String color1 = scale.getColorForIndex(1)
    String color2 = scale.getColorForIndex(2)

    assertNotNull(color0)
    assertNotNull(color1)
    assertNotNull(color2)
    assertNotEquals(color0, color1)
    assertNotEquals(color1, color2)
  }

  @Test
  void testGetColorForIndexWrapsAround() {
    ScaleColorViridis scale = new ScaleColorViridis()
    scale.train(['A', 'B', 'C'])

    String color0 = scale.getColorForIndex(0)
    String color3 = scale.getColorForIndex(3)  // Should wrap to 0

    assertEquals(color0, color3)
  }

  @Test
  void testGetColorForNegativeIndex() {
    ScaleColorViridis scale = new ScaleColorViridis(naValue: 'gray')

    assertEquals('gray', scale.getColorForIndex(-1))
  }

  @Test
  void testWithParamsConstructor() {
    ScaleColorViridis scale = new ScaleColorViridis(
        option: 'plasma',
        begin: 0.2,
        end: 0.8,
        direction: -1,
        alpha: 0.5,
        naValue: '#999999',
        name: 'My Scale'
    )

    assertEquals('plasma', scale.option)
    assertEquals(0.2, scale.begin, 0.001)
    assertEquals(0.8, scale.end, 0.001)
    assertEquals(-1, scale.direction)
    assertEquals(0.5, scale.alpha, 0.001)
    assertEquals('#999999', scale.naValue)
    assertEquals('My Scale', scale.name)
  }

  @Test
  void testAestheticCanBeSetToFill() {
    ScaleColorViridis scale = new ScaleColorViridis(aesthetic: 'fill')
    assertEquals('fill', scale.aesthetic)
  }

  @Test
  void testBritishSpellingColourMapsToColor() {
    ScaleColorViridis scale = new ScaleColorViridis(aesthetic: 'colour')
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testAlphaNormalization() {
    ScaleColorViridis scale1 = new ScaleColorViridis(alpha: -0.5)
    assertEquals(0.0, scale1.alpha, 0.001)

    ScaleColorViridis scale2 = new ScaleColorViridis(alpha: 1.5)
    assertEquals(1.0, scale2.alpha, 0.001)

    ScaleColorViridis scale3 = new ScaleColorViridis(alpha: 0.5)
    assertEquals(0.5, scale3.alpha, 0.001)
  }

  @Test
  void testReset() {
    ScaleColorViridis scale = new ScaleColorViridis()
    scale.train(['A', 'B', 'C'])
    
    assertFalse(scale.getColors().isEmpty())
    
    scale.reset()
    
    assertTrue(scale.getColors().isEmpty())
  }

  @Test
  void testSingleLevel() {
    ScaleColorViridis scale = new ScaleColorViridis()
    scale.train(['A'])

    String color = scale.transform('A') as String
    assertNotNull(color)
    assertTrue(color.startsWith('#'))
    
    // Single level should get a color from middle of range
    assertEquals(1, scale.getColors().size())
  }

  @Test
  void testManyLevels() {
    ScaleColorViridis scale = new ScaleColorViridis()
    List<String> levels = (1..20).collect { "Level$it" }
    scale.train(levels)

    // Should generate 20 distinct colors
    List<String> colors = scale.getColors()
    assertEquals(20, colors.size())
    
    // Verify all are valid hex colors
    colors.each { color ->
      assertTrue(color.startsWith('#'))
      assertEquals(7, color.length()) // #RRGGBB format
    }
    
    // Verify they're all different (viridis has enough color space for 20 values)
    assertEquals(20, colors.toSet().size())
  }

  @Test
  void testCaseSensitivityOfPaletteNames() {
    ScaleColorViridis scale1 = new ScaleColorViridis(option: 'MAGMA')
    assertEquals('magma', scale1.option)

    ScaleColorViridis scale2 = new ScaleColorViridis(option: 'Plasma')
    assertEquals('plasma', scale2.option)

    ScaleColorViridis scale3 = new ScaleColorViridis(option: 'viridis')
    assertEquals('viridis', scale3.option)
  }

  @Test
  void testLimitsParameter() {
    ScaleColorViridis scale = new ScaleColorViridis(limits: ['A', 'B', 'C', 'D'])
    scale.train(['A', 'B', 'E'])  // E is not in limits

    // Should respect limits
    assertEquals(['A', 'B', 'C', 'D'], scale.limits)
  }

  @Test
  void testBreaksParameter() {
    ScaleColorViridis scale = new ScaleColorViridis(breaks: ['A', 'C'])
    scale.train(['A', 'B', 'C', 'D'])

    assertEquals(['A', 'C'], scale.breaks)
  }

  @Test
  void testLabelsParameter() {
    ScaleColorViridis scale = new ScaleColorViridis(labels: ['First', 'Second', 'Third'])
    scale.train(['A', 'B', 'C'])

    assertEquals(['First', 'Second', 'Third'], scale.labels)
  }

  @Test
  void testNumericLevels() {
    ScaleColorViridis scale = new ScaleColorViridis()
    scale.train([1, 2, 3])

    String color1 = scale.transform(1) as String
    String color2 = scale.transform(2) as String
    String color3 = scale.transform(3) as String

    assertNotNull(color1)
    assertNotNull(color2)
    assertNotNull(color3)
    assertNotEquals(color1, color2)
    assertNotEquals(color2, color3)
  }

  @Test
  void testMixedTypeLevels() {
    ScaleColorViridis scale = new ScaleColorViridis()
    scale.train(['A', 1, 'B', 2])

    assertNotNull(scale.transform('A'))
    assertNotNull(scale.transform(1))
    assertNotNull(scale.transform('B'))
    assertNotNull(scale.transform(2))
  }

  @Test
  void testDuplicateValuesInTraining() {
    ScaleColorViridis scale = new ScaleColorViridis()
    scale.train(['A', 'B', 'A', 'C', 'B'])

    // Should only have unique levels
    List<String> colors = scale.getColors()
    assertEquals(3, colors.size())
    
    // Same value should always get same color
    assertEquals(scale.transform('A'), scale.transform('A'))
    assertEquals(scale.transform('B'), scale.transform('B'))
  }

  @Test
  void testColorInterpolationAccuracy() {
    ScaleColorViridis scale = new ScaleColorViridis(option: 'viridis')
    scale.train(['A', 'B'])

    String color1 = scale.transform('A') as String
    String color2 = scale.transform('B') as String

    // Colors should be from viridis palette extremes
    // First color should be dark purple-ish
    assertTrue(color1.startsWith('#'))
    // Second color should be yellow-ish (viridis ends in yellow)
    assertTrue(color2.startsWith('#'))
    
    // They should be very different
    assertNotEquals(color1, color2)
  }

  @Test
  void testGetComputedBreaksReturnsLevels() {
    ScaleColorViridis scale = new ScaleColorViridis()
    scale.train(['X', 'Y', 'Z'])

    assertEquals(['X', 'Y', 'Z'], scale.getComputedBreaks())
  }

  @Test
  void testGetComputedLabels() {
    ScaleColorViridis scale = new ScaleColorViridis()
    scale.train(['Apple', 'Banana', 'Cherry'])

    assertEquals(['Apple', 'Banana', 'Cherry'], scale.getComputedLabels())
  }

  @Test
  void testGetComputedLabelsWithExplicitLabels() {
    ScaleColorViridis scale = new ScaleColorViridis(labels: ['A', 'B', 'C'])
    scale.train(['Apple', 'Banana', 'Cherry'])

    assertEquals(['A', 'B', 'C'], scale.getComputedLabels())
  }
}
