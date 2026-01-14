package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleColorManual

import static org.junit.jupiter.api.Assertions.*

class ScaleColorManualTest {

  @Test
  void testDefaultAesthetic() {
    ScaleColorManual scale = new ScaleColorManual()
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testTransformWithPositionalColors() {
    ScaleColorManual scale = new ScaleColorManual(values: ['red', 'green', 'blue'])
    scale.train(['A', 'B', 'C'])

    assertEquals('red', scale.transform('A'))
    assertEquals('green', scale.transform('B'))
    assertEquals('blue', scale.transform('C'))
  }

  @Test
  void testTransformWithNamedColors() {
    ScaleColorManual scale = new ScaleColorManual(values: [A: 'red', B: 'green', C: 'blue'])
    scale.train(['A', 'B', 'C'])

    assertEquals('red', scale.transform('A'))
    assertEquals('green', scale.transform('B'))
    assertEquals('blue', scale.transform('C'))
  }

  @Test
  void testTransformUsesDefaultPaletteWhenNoValues() {
    ScaleColorManual scale = new ScaleColorManual()
    scale.train(['A', 'B', 'C'])

    String colorA = scale.transform('A') as String
    String colorB = scale.transform('B') as String
    String colorC = scale.transform('C') as String

    // Should get colors from default palette
    assertNotNull(colorA)
    assertNotNull(colorB)
    assertNotNull(colorC)

    // All should be different
    assertNotEquals(colorA, colorB)
    assertNotEquals(colorB, colorC)
    assertNotEquals(colorA, colorC)

    // Default palette starts with these colors
    assertEquals('#F8766D', colorA)
    assertEquals('#00BA38', colorB)
    assertEquals('#619CFF', colorC)
  }

  @Test
  void testDefaultPaletteExpandsWithoutCycling() {
    ScaleColorManual scale = new ScaleColorManual()
    List<String> levels = (1..12).collect { "L${it}" }
    scale.train(levels)

    List<String> colors = levels.collect { scale.transform(it) as String }

    assertEquals(12, colors.size())
    assertEquals(12, colors.toSet().size())
  }

  @Test
  void testTransformWithUnknownValueReturnsNaColor() {
    ScaleColorManual scale = new ScaleColorManual(values: ['red', 'blue'], naValue: 'gray')
    scale.train(['A', 'B'])

    assertEquals('gray', scale.transform('Unknown'))
  }

  @Test
  void testTransformWithNullReturnsNaColor() {
    ScaleColorManual scale = new ScaleColorManual(naValue: '#CCCCCC')
    scale.train(['A', 'B'])

    assertEquals('#CCCCCC', scale.transform(null))
  }

  @Test
  void testColorsCycleWhenFewerColorsThanLevels() {
    ScaleColorManual scale = new ScaleColorManual(values: ['red', 'blue'])
    scale.train(['A', 'B', 'C', 'D'])

    assertEquals('red', scale.transform('A'))
    assertEquals('blue', scale.transform('B'))
    assertEquals('red', scale.transform('C'))  // Cycles back
    assertEquals('blue', scale.transform('D'))
  }

  @Test
  void testGetColors() {
    ScaleColorManual scale = new ScaleColorManual(values: ['#FF0000', '#00FF00', '#0000FF'])
    scale.train(['X', 'Y', 'Z'])

    List<String> colors = scale.getColors()

    assertEquals(3, colors.size())
    assertEquals('#FF0000', colors[0])
    assertEquals('#00FF00', colors[1])
    assertEquals('#0000FF', colors[2])
  }

  @Test
  void testGetColorForIndex() {
    ScaleColorManual scale = new ScaleColorManual(values: ['a', 'b', 'c'])
    scale.train(['X', 'Y', 'Z'])

    assertEquals('a', scale.getColorForIndex(0))
    assertEquals('b', scale.getColorForIndex(1))
    assertEquals('c', scale.getColorForIndex(2))
    assertEquals('a', scale.getColorForIndex(3))  // Wraps around
  }

  @Test
  void testGetColorForNegativeIndexReturnsNaValue() {
    ScaleColorManual scale = new ScaleColorManual(naValue: 'default')

    assertEquals('default', scale.getColorForIndex(-1))
  }

  @Test
  void testInverseWithPositionalColors() {
    ScaleColorManual scale = new ScaleColorManual(values: ['red', 'green', 'blue'])
    scale.train(['A', 'B', 'C'])

    assertEquals('A', scale.inverse('red'))
    assertEquals('B', scale.inverse('green'))
    assertEquals('C', scale.inverse('blue'))
  }

  @Test
  void testInverseWithNamedColors() {
    ScaleColorManual scale = new ScaleColorManual(values: [first: 'red', second: 'blue'])
    scale.train(['first', 'second'])

    assertEquals('first', scale.inverse('red'))
    assertEquals('second', scale.inverse('blue'))
  }

  @Test
  void testInverseWithUnknownColorReturnsNull() {
    ScaleColorManual scale = new ScaleColorManual(values: ['red', 'blue'])
    scale.train(['A', 'B'])

    assertNull(scale.inverse('purple'))
    assertNull(scale.inverse(null))
  }

  @Test
  void testFluentApiWithList() {
    ScaleColorManual scale = new ScaleColorManual()
        .values(['#FF0000', '#00FF00', '#0000FF'])

    assertEquals(['#FF0000', '#00FF00', '#0000FF'], scale.values)
  }

  @Test
  void testFluentApiWithMap() {
    ScaleColorManual scale = new ScaleColorManual()
        .values([cat1: '#FF0000', cat2: '#00FF00'])

    assertEquals('#FF0000', scale.namedValues['cat1'])
    assertEquals('#00FF00', scale.namedValues['cat2'])
  }

  @Test
  void testWithParamsConstructor() {
    ScaleColorManual scale = new ScaleColorManual(
        values: ['red', 'green'],
        name: 'Category Colors',
        naValue: 'gray'
    )

    assertEquals(['red', 'green'], scale.values)
    assertEquals('Category Colors', scale.name)
    assertEquals('gray', scale.naValue)
  }

  @Test
  void testAestheticCanBeSetToFill() {
    ScaleColorManual scale = new ScaleColorManual(aesthetic: 'fill')
    assertEquals('fill', scale.aesthetic)
  }

  @Test
  void testBritishSpellingColourMapsToColor() {
    ScaleColorManual scale = new ScaleColorManual(aesthetic: 'colour')
    assertEquals('color', scale.aesthetic)
  }

  @Test
  void testGetComputedBreaksReturnsLevels() {
    ScaleColorManual scale = new ScaleColorManual()
    scale.train(['X', 'Y', 'Z'])

    assertEquals(['X', 'Y', 'Z'], scale.getComputedBreaks())
  }

  @Test
  void testGetComputedLabels() {
    ScaleColorManual scale = new ScaleColorManual()
    scale.train(['Apple', 'Banana', 'Cherry'])

    assertEquals(['Apple', 'Banana', 'Cherry'], scale.getComputedLabels())
  }

  @Test
  void testGetComputedLabelsWithExplicitLabels() {
    ScaleColorManual scale = new ScaleColorManual(labels: ['A', 'B', 'C'])
    scale.train(['Apple', 'Banana', 'Cherry'])

    assertEquals(['A', 'B', 'C'], scale.getComputedLabels())
  }

  @Test
  void testNamedValuesTakePrecedenceOverPositional() {
    ScaleColorManual scale = new ScaleColorManual(values: ['red', 'blue'])
    scale.namedValues = [A: 'green']  // Override A specifically
    scale.train(['A', 'B'])

    assertEquals('green', scale.transform('A'))  // Named takes precedence
    assertEquals('blue', scale.transform('B'))   // Falls back to positional
  }

  @Test
  void testWorksWithNumericLevels() {
    ScaleColorManual scale = new ScaleColorManual(values: ['#FF0000', '#00FF00', '#0000FF'])
    scale.train([1, 2, 3])

    assertEquals('#FF0000', scale.transform(1))
    assertEquals('#00FF00', scale.transform(2))
    assertEquals('#0000FF', scale.transform(3))
  }

  @Test
  void testEmptyTrainingDataHandled() {
    ScaleColorManual scale = new ScaleColorManual(naValue: 'default')
    scale.train([])

    assertEquals('default', scale.transform('anything'))
    assertEquals(0, scale.getLevelCount())
  }

  @Test
  void testGenerateHuePaletteSizes() {
    def method = ScaleColorManual.class.getDeclaredMethod('generateHuePalette', int)
    method.accessible = true

    List<String> empty = method.invoke(null, 0) as List<String>
    List<String> one = method.invoke(null, 1) as List<String>
    List<String> seven = method.invoke(null, 7) as List<String>
    List<String> twelve = method.invoke(null, 12) as List<String>
    List<String> hundred = method.invoke(null, 100) as List<String>

    assertTrue(empty.isEmpty())
    assertEquals(1, one.size())
    assertEquals(7, seven.size())
    assertEquals(12, twelve.size())
    assertEquals(100, hundred.size())

    assertTrue(one[0].matches(/#[0-9A-F]{6}/))
    assertTrue(seven.every { it.matches(/#[0-9A-F]{6}/) })
  }

}
