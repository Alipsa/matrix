package charm.render.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.render.scale.ColorCharmScale

import static org.junit.jupiter.api.Assertions.*

class ColorCharmScaleTest {

  @Test
  void testDefaultPaletteAssignsColorsToLevels() {
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues(['A', 'B', 'C'], Scale.discrete())

    assertEquals('default', scale.colorType)
    assertEquals(3, scale.levels.size())
    assertNotNull(scale.colorFor('A'))
    assertNotNull(scale.colorFor('B'))
    assertNotNull(scale.colorFor('C'))
    // Each level should get a different color
    assertNotEquals(scale.colorFor('A'), scale.colorFor('B'))
    assertNotEquals(scale.colorFor('B'), scale.colorFor('C'))
  }

  @Test
  void testDefaultPaletteWrapsAfter10() {
    List<Object> values = (1..12).collect { "level$it" as Object }
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues(values, Scale.discrete())

    // First and eleventh should share a color (wrap around)
    assertEquals(scale.colorFor('level1'), scale.colorFor('level11'))
  }

  @Test
  void testManualColorList() {
    Scale spec = Scale.manual(['#ff0000', '#00ff00', '#0000ff'])
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues(['X', 'Y', 'Z'], spec)

    assertEquals('manual', scale.colorType)
    assertEquals('#ff0000', scale.colorFor('X'))
    assertEquals('#00ff00', scale.colorFor('Y'))
    assertEquals('#0000ff', scale.colorFor('Z'))
  }

  @Test
  void testManualNamedMap() {
    Scale spec = Scale.manual([A: '#aaaaaa', B: '#bbbbbb'])
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues(['A', 'B'], spec)

    assertEquals('manual', scale.colorType)
    assertEquals('#aaaaaa', scale.colorFor('A'))
    assertEquals('#bbbbbb', scale.colorFor('B'))
  }

  @Test
  void testManualAutoHuePalette() {
    // No values or namedValues -> auto-generate HCL hue palette
    Scale spec = new Scale()
    spec.params['colorType'] = 'manual'

    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues(['A', 'B', 'C'], spec)

    assertEquals('manual', scale.colorType)
    String colorA = scale.colorFor('A')
    String colorB = scale.colorFor('B')
    String colorC = scale.colorFor('C')
    assertNotNull(colorA)
    assertNotNull(colorB)
    assertNotNull(colorC)
    // Auto-generated hue colors should be valid hex
    assertTrue(colorA.startsWith('#'), "Expected hex color, got: $colorA")
    // Each should be different
    assertNotEquals(colorA, colorB)
    assertNotEquals(colorB, colorC)
  }

  @Test
  void testBrewerPalette() {
    Scale spec = Scale.brewer('Set1')
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues(['A', 'B', 'C'], spec)

    assertEquals('brewer', scale.colorType)
    String colorA = scale.colorFor('A')
    String colorB = scale.colorFor('B')
    assertNotNull(colorA)
    assertNotNull(colorB)
    assertNotEquals(colorA, colorB)
  }

  @Test
  void testBrewerReversedDirection() {
    Scale specNormal = Scale.brewer('Set1', 1)
    Scale specReversed = Scale.brewer('Set1', -1)

    ColorCharmScale normalScale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    normalScale.trainFromValues(['A', 'B', 'C'], specNormal)

    ColorCharmScale reversedScale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    reversedScale.trainFromValues(['A', 'B', 'C'], specReversed)

    // Normal and reversed should assign different colors to 'A'
    assertNotEquals(normalScale.colorFor('A'), reversedScale.colorFor('A'))
  }

  @Test
  void testGradientTwoColor() {
    Scale spec = Scale.gradient('#000000', '#ffffff')
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues([0, 50, 100], spec)

    assertEquals('gradient', scale.colorType)

    // Low value should be near black
    String lowColor = scale.colorFor(0)
    assertNotNull(lowColor)
    assertTrue(lowColor.startsWith('#'))

    // High value should be near white
    String highColor = scale.colorFor(100)
    assertNotNull(highColor)
    assertTrue(highColor.startsWith('#'))

    // They should be different
    assertNotEquals(lowColor, highColor)
  }

  @Test
  void testGradientDiverging() {
    Scale spec = Scale.gradient('#ff0000', '#0000ff', '#ffffff', 50)
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues([0, 50, 100], spec)

    assertEquals('gradient', scale.colorType)

    // Midpoint value should be near white (the mid color)
    String midColor = scale.colorFor(50)
    assertNotNull(midColor)

    String lowColor = scale.colorFor(0)
    String highColor = scale.colorFor(100)
    // All three should be different
    assertNotEquals(lowColor, midColor)
    assertNotEquals(midColor, highColor)
  }

  @Test
  void testGradientNullReturnsNaValue() {
    Scale spec = Scale.gradient('#000000', '#ffffff')
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues([0, 100], spec)

    assertEquals('#999999', scale.colorFor(null))
  }

  @Test
  void testViridisDiscreteAssignsColors() {
    Scale spec = Scale.viridis('viridis')
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues(['A', 'B', 'C', 'D'], spec)

    assertEquals('viridis_d', scale.colorType)
    assertEquals(4, scale.levels.size())

    String colorA = scale.colorFor('A')
    String colorD = scale.colorFor('D')
    assertNotNull(colorA)
    assertNotNull(colorD)
    assertTrue(colorA.startsWith('#'))
    assertTrue(colorD.startsWith('#'))
    assertNotEquals(colorA, colorD)
  }

  @Test
  void testViridisWithDifferentOptions() {
    ['viridis', 'magma', 'inferno', 'plasma'].each { String option ->
      Scale spec = Scale.viridis(option)
      ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
      scale.trainFromValues(['A', 'B'], spec)

      assertNotNull(scale.colorFor('A'), "Option $option should produce a color for 'A'")
    }
  }

  @Test
  void testIdentityPassThrough() {
    Scale spec = Scale.identity()
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues(['red', '#00ff00', 'blue'], spec)

    assertEquals('identity', scale.colorType)
    // Identity scale returns the value itself as a color
    String colorRed = scale.colorFor('red')
    assertNotNull(colorRed)
    // 'red' should be normalized to a hex color
    assertTrue(colorRed.startsWith('#') || colorRed == 'red')
  }

  @Test
  void testIdentityNullReturnsNaValue() {
    Scale spec = Scale.identity('#cccccc')
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues(['red'], spec)

    assertEquals('#cccccc', scale.colorFor(null))
  }

  @Test
  void testColorForUnknownLevelReturnsNaValue() {
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues(['A', 'B'], Scale.discrete())

    assertEquals('#999999', scale.colorFor('unknown'))
  }

  @Test
  void testIsDiscreteForCategoricalTypes() {
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues(['A', 'B'], Scale.discrete())

    assertTrue(scale.isDiscrete())
  }

  @Test
  void testIsDiscreteReturnsFalseForGradient() {
    Scale spec = Scale.gradient('#000000', '#ffffff')
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues([0, 100], spec)

    assertFalse(scale.isDiscrete())
  }

  @Test
  void testGradientNMultiStopColors() {
    Scale spec = Scale.gradientN(['#ff0000', '#00ff00', '#0000ff'])
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues([0, 50, 100], spec)

    assertEquals('gradientN', scale.colorType)

    String low = scale.colorFor(0)
    String mid = scale.colorFor(50)
    String high = scale.colorFor(100)

    assertNotNull(low)
    assertNotNull(mid)
    assertNotNull(high)
    // All should be different colors
    assertNotEquals(low, mid)
    assertNotEquals(mid, high)
  }

  @Test
  void testDuplicateValuesProduceSingleLevel() {
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues(['A', 'A', 'B', 'B', 'B'], Scale.discrete())

    assertEquals(2, scale.levels.size())
    assertEquals(['A', 'B'], scale.levels)
  }

  @Test
  void testNullValuesAreFilteredFromLevels() {
    ColorCharmScale scale = new ColorCharmScale(rangeStart: 0.0, rangeEnd: 1.0)
    scale.trainFromValues([null, 'A', null, 'B'], Scale.discrete())

    assertEquals(2, scale.levels.size())
    assertEquals(['A', 'B'], scale.levels)
  }
}
