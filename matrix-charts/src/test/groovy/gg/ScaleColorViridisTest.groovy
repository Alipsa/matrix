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
}
