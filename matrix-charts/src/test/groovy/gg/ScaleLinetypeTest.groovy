package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleLinetype
import se.alipsa.matrix.gg.scale.ScaleLinetypeManual

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for linetype scales.
 */
class ScaleLinetypeTest {

  @Test
  void testScaleLinetypeBasic() {
    ScaleLinetype scale = new ScaleLinetype()
    scale.train(['A', 'B', 'C'])

    assertEquals('solid', scale.transform('A'))
    assertEquals('dashed', scale.transform('B'))
    assertEquals('dotted', scale.transform('C'))
  }

  @Test
  void testScaleLinetypeCustomLinetypes() {
    ScaleLinetype scale = new ScaleLinetype(linetypes: ['dashed', 'dotted'])
    scale.train(['A', 'B', 'C'])

    assertEquals('dashed', scale.transform('A'))
    assertEquals('dotted', scale.transform('B'))
    // Should cycle back to first linetype
    assertEquals('dashed', scale.transform('C'))
  }

  @Test
  void testScaleLinetypeNullValue() {
    ScaleLinetype scale = new ScaleLinetype()
    scale.train(['A', 'B'])

    assertNull(scale.transform(null))
  }

  @Test
  void testScaleLinetypeUnknownValue() {
    ScaleLinetype scale = new ScaleLinetype()
    scale.train(['A', 'B'])

    // Unknown value should return null
    assertNull(scale.transform('C'))
  }

  @Test
  void testScaleLinetypeWithLimits() {
    ScaleLinetype scale = new ScaleLinetype(limits: ['B', 'C'])
    scale.train(['A', 'B', 'C'])

    // Only B and C should be in levels due to limits
    assertEquals('solid', scale.transform('B'))
    assertEquals('dashed', scale.transform('C'))
  }

  @Test
  void testScaleLinetypeManualListValues() {
    ScaleLinetypeManual scale = new ScaleLinetypeManual(values: ['dotted', 'longdash'])
    scale.train(['A', 'B', 'C'])

    assertEquals('dotted', scale.transform('A'))
    assertEquals('longdash', scale.transform('B'))
    // Should cycle
    assertEquals('dotted', scale.transform('C'))
  }

  @Test
  void testScaleLinetypeManualNamedValues() {
    ScaleLinetypeManual scale = new ScaleLinetypeManual(values: [A: 'longdash', B: 'twodash', C: 'dotdash'])
    scale.train(['A', 'B', 'C'])

    assertEquals('longdash', scale.transform('A'))
    assertEquals('twodash', scale.transform('B'))
    assertEquals('dotdash', scale.transform('C'))
  }

  @Test
  void testScaleLinetypeManualNullValue() {
    ScaleLinetypeManual scale = new ScaleLinetypeManual(values: ['dashed'], naValue: 'dotted')
    scale.train(['A', 'B'])

    assertEquals('dotted', scale.transform(null))
  }

  @Test
  void testScaleLinetypeManualDefaultLinetypes() {
    ScaleLinetypeManual scale = new ScaleLinetypeManual()
    scale.train(['A', 'B', 'C'])

    // Should use default linetypes when no values provided
    assertEquals('solid', scale.transform('A'))
    assertEquals('dashed', scale.transform('B'))
    assertEquals('dotted', scale.transform('C'))
  }

  @Test
  void testScaleLinetypeManualInverse() {
    ScaleLinetypeManual scale = new ScaleLinetypeManual(values: ['dotted', 'longdash'])
    scale.train(['A', 'B'])

    assertEquals('A', scale.inverse('dotted'))
    assertEquals('B', scale.inverse('longdash'))
    assertNull(scale.inverse('solid'))
  }

  @Test
  void testScaleLinetypeManualInverseNamed() {
    ScaleLinetypeManual scale = new ScaleLinetypeManual(values: [cat1: 'longdash', cat2: 'twodash'])
    scale.train(['cat1', 'cat2'])

    assertEquals('cat1', scale.inverse('longdash'))
    assertEquals('cat2', scale.inverse('twodash'))
  }

  @Test
  void testScaleLinetypeManualInverseWithCycledLinetypes() {
    // Test that inverse correctly handles cycled linetypes (more levels than linetypes)
    ScaleLinetypeManual scale = new ScaleLinetypeManual(values: ['dotted', 'longdash'])
    scale.train(['A', 'B', 'C', 'D'])

    // A maps to dotted (index 0 % 2 = 0)
    // B maps to longdash (index 1 % 2 = 1)
    // C maps to dotted (index 2 % 2 = 0) - cycled
    // D maps to longdash (index 3 % 2 = 1) - cycled

    // Verify transformations
    assertEquals('dotted', scale.transform('A'))
    assertEquals('longdash', scale.transform('B'))
    assertEquals('dotted', scale.transform('C'))
    assertEquals('longdash', scale.transform('D'))

    // Inverse should return the FIRST level that maps to each linetype
    assertEquals('A', scale.inverse('dotted'))  // Not 'C'
    assertEquals('B', scale.inverse('longdash'))   // Not 'D'
  }

  @Test
  void testScaleLinetypeManualGetLinetypeForIndex() {
    ScaleLinetypeManual scale = new ScaleLinetypeManual(values: ['dashed', 'dotted'])
    scale.train(['A', 'B', 'C'])

    assertEquals('dashed', scale.getLinetypeForIndex(0))
    assertEquals('dotted', scale.getLinetypeForIndex(1))
    assertEquals('dashed', scale.getLinetypeForIndex(2))
  }

  @Test
  void testScaleLinetypeManualGetLinetypes() {
    ScaleLinetypeManual scale = new ScaleLinetypeManual(values: ['dotted', 'longdash'])
    scale.train(['A', 'B', 'C'])

    List<String> linetypes = scale.getLinetypes()
    assertEquals(3, linetypes.size())
    assertEquals('dotted', linetypes[0])
    assertEquals('longdash', linetypes[1])
    assertEquals('dotted', linetypes[2])
  }

  @Test
  void testScaleLinetypeFactory() {
    def scale = scale_linetype()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleLinetype)
  }

  @Test
  void testScaleLinetypeDiscreteFactory() {
    def scale = scale_linetype_discrete()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleLinetype)
  }

  @Test
  void testScaleLinetypeManualFactory() {
    def scale = scale_linetype_manual()
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleLinetypeManual)
  }

  @Test
  void testScaleLinetypeWithGeomLine() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'category')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'A'],
            [1, 1, 'B'],
            [2, 4, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', linetype: 'category', group: 'category')) +
        geom_line() +
        scale_linetype()

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<svg'))
    // Lines should be rendered
    assertTrue(content.contains('<line'))
  }

  @Test
  void testScaleLinetypeManualWithGeomLine() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'type')
        .rows([
            [1, 2, 'cat1'],
            [2, 3, 'cat1'],
            [1, 1, 'cat2'],
            [2, 4, 'cat2']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', linetype: 'type', group: 'type')) +
        geom_line() +
        scale_linetype_manual(values: ['dashed', 'dotted'])

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<svg'))
    // Lines should be rendered
    assertTrue(content.contains('<line'))
  }

  @Test
  void testScaleLinetypeWithCustomName() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'A'],
            [1, 1, 'B'],
            [2, 4, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', linetype: 'cat', group: 'cat')) +
        geom_line() +
        scale_linetype(name: 'Line Type')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Should contain the custom legend title
    assertTrue(content.contains('Line Type'))
  }

  @Test
  void testScaleLinetypeManualWithNamedMapping() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
            [1, 2, 'control'],
            [2, 3, 'control'],
            [1, 1, 'treatment'],
            [2, 4, 'treatment']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', linetype: 'group', group: 'group')) +
        geom_line() +
        scale_linetype_manual(values: [control: 'solid', treatment: 'dashed'])

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<svg'))
  }

  @Test
  void testAllDefaultLinetypes() {
    ScaleLinetype scale = new ScaleLinetype()
    scale.train(['A', 'B', 'C', 'D', 'E', 'F'])

    // Test all 6 default linetypes
    assertEquals('solid', scale.transform('A'))
    assertEquals('dashed', scale.transform('B'))
    assertEquals('dotted', scale.transform('C'))
    assertEquals('dotdash', scale.transform('D'))
    assertEquals('longdash', scale.transform('E'))
    assertEquals('twodash', scale.transform('F'))
  }

  @Test
  void testLinetypeCycling() {
    ScaleLinetype scale = new ScaleLinetype()
    scale.train(['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'])

    // After 6 linetypes, should cycle back
    assertEquals('solid', scale.transform('G'))  // Index 6 % 6 = 0
    assertEquals('dashed', scale.transform('H')) // Index 7 % 6 = 1
  }
}
