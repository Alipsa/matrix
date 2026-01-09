package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.facet.Labeller

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for facet labeller functions.
 */
class FacetLabellerTest {

  @Test
  void testLabelValueFunction() {
    def labeller = label_value()
    assertEquals('Labeller', labeller.getClass().simpleName)

    // Single variable
    String label1 = labeller.label([category: 'A'])
    assertEquals('A', label1)

    // Multiple variables
    String label2 = labeller.label([category: 'A', type: 'X'])
    assertEquals('A\nX', label2)
  }

  @Test
  void testLabelValueSingleLine() {
    def labeller = label_value(false)
    assertFalse(labeller.multiLine)

    // Multiple variables on single line
    String label = labeller.label([category: 'A', type: 'X'])
    assertEquals('A, X', label)
  }

  @Test
  void testLabelBothFunction() {
    def labeller = label_both()

    // Single variable
    String label1 = labeller.label([category: 'A'])
    assertEquals('category: A', label1)

    // Multiple variables
    String label2 = labeller.label([category: 'A', type: 'X'])
    assertEquals('category: A\ntype: X', label2)
  }

  @Test
  void testLabelBothCustomSeparator() {
    def labeller = label_both(' = ')

    String label = labeller.label([category: 'A'])
    assertEquals('category = A', label)
  }

  @Test
  void testLabelBothSingleLine() {
    def labeller = label_both(': ', false)
    assertFalse(labeller.multiLine)

    String label = labeller.label([category: 'A', type: 'X'])
    assertEquals('category: A, type: X', label)
  }

  @Test
  void testLabelContextSingleVariable() {
    def labeller = label_context()

    // Single variable - should show value only
    String label = labeller.label([category: 'A'])
    assertEquals('A', label)
  }

  @Test
  void testLabelContextMultipleVariables() {
    def labeller = label_context()

    // Multiple variables - should show both name and value
    String label = labeller.label([category: 'A', type: 'X'])
    assertEquals('category: A\ntype: X', label)
  }

  @Test
  void testLabelWrapGen() {
    def labeller = label_wrap_gen(10)

    // Short label - no wrapping
    String shortLabel = labeller.label([cat: 'Short'])
    assertEquals('Short', shortLabel)

    // Long label - should wrap
    String longLabel = labeller.label([cat: 'This is a very long label that should wrap'])
    assertTrue(longLabel.contains('\n'), "Long label should be wrapped")
    // Each line should be <= 10 characters (approximately, depending on word breaks)
    String[] lines = longLabel.split('\n')
    assertTrue(lines.length > 1, "Long label should be split into multiple lines")
  }

  @Test
  void testLabelWrapGenWithLongWord() {
    def labeller = label_wrap_gen(10)

    // Word longer than width - should break the word
    String longWord = labeller.label([cat: 'Supercalifragilisticexpialidocious'])
    assertTrue(longWord.contains('\n'), "Long word should be broken into chunks")
    String[] lines = longWord.split('\n')
    // Each line should be exactly 10 characters (except possibly the last one)
    for (int i = 0; i < lines.length - 1; i++) {
      assertTrue(lines[i].length() <= 10, "Line ${i} should be <= 10 characters: '${lines[i]}'")
    }
    assertTrue(lines[lines.length - 1].length() <= 10, "Last line should be <= 10 characters")
  }

  @Test
  void testLabelWrapGenMixedContent() {
    def labeller = label_wrap_gen(15)

    // Mix of normal words and long words
    String mixed = labeller.label([cat: 'Short words and Supercalifragilisticexpialidocious combined'])
    assertTrue(mixed.contains('\n'), "Mixed content should wrap")
    String[] lines = mixed.split('\n')
    // Verify no line exceeds the width
    for (int i = 0; i < lines.length; i++) {
      assertTrue(lines[i].length() <= 15, "Line ${i} should be <= 15 characters: '${lines[i]}'")
    }
  }

  @Test
  void testLabelParsed() {
    def labeller = label_parsed()

    // Currently just returns the value as-is
    String label = labeller.label([var: 'alpha'])
    assertEquals('alpha', label)
  }

  @Test
  void testLabellerCompositeSimple() {
    def composite = labeller(category: label_both())

    // Variable with custom labeller
    String label1 = composite.label([category: 'A'])
    assertEquals('category: A', label1)
  }

  @Test
  void testLabellerCompositeMultiple() {
    def composite = labeller(
        category: label_both(),
        type: label_value()
    )

    Map<String, Object> values = [category: 'A', type: 'X']
    String label = composite.label(values)

    // Should contain both formats
    assertTrue(label.contains('category: A'), "Should contain 'category: A'")
    assertTrue(label.contains('X'), "Should contain 'X'")
  }

  @Test
  void testLabellerWithDefault() {
    def composite = labeller(
        category: label_both(),
        '.default': label_value()
    )

    // category uses label_both
    String label1 = composite.label([category: 'A'])
    assertEquals('category: A', label1)

    // type uses default (label_value)
    String label2 = composite.label([type: 'X'])
    assertEquals('X', label2)
  }

  @Test
  void testFacetWrapWithLabelValue() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'category')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B'],
            [3, 1, 'A'],
            [4, 4, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        facet_wrap(facets: 'category', labeller: label_value())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Should contain facet labels
    assertTrue(content.contains('>A<'), "Should contain label 'A'")
    assertTrue(content.contains('>B<'), "Should contain label 'B'")
    // Should NOT contain "category:" prefix
    assertFalse(content.contains('category:'), "Should not contain 'category:' prefix")
  }

  @Test
  void testFacetWrapWithLabelBoth() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'category')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B'],
            [3, 1, 'A']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        facet_wrap(facets: 'category', labeller: label_both())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Should contain facet labels with variable name
    assertTrue(content.contains('category: A'), "Should contain 'category: A'")
    assertTrue(content.contains('category: B'), "Should contain 'category: B'")
  }

  @Test
  void testFacetWrapWithLabelContext() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'category')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    // Single variable - should show value only
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        facet_wrap(facets: 'category', labeller: label_context())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Should contain labels without "category:" prefix (single variable)
    assertTrue(content.contains('>A<'), "Should contain label 'A'")
    assertTrue(content.contains('>B<'), "Should contain label 'B'")
  }

  @Test
  void testFacetGridWithLabelBoth() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'cyl', 'gear')
        .rows([
            [1, 2, 4, 3],
            [2, 3, 4, 4],
            [3, 1, 6, 3],
            [4, 4, 6, 4]
        ])
        .types(Integer, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        facet_grid(rows: 'cyl', cols: 'gear', labeller: label_both())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Should contain labels with variable names
    assertTrue(content.contains('cyl: 4') || content.contains('cyl: 6'), "Should contain 'cyl:' prefix")
    assertTrue(content.contains('gear: 3') || content.contains('gear: 4'), "Should contain 'gear:' prefix")
  }

  @Test
  void testFacetGridWithCompositeLabeller() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'cyl', 'gear')
        .rows([
            [1, 2, 4, 3],
            [2, 3, 4, 4],
            [3, 1, 6, 3]
        ])
        .types(Integer, Integer, Integer, Integer)
        .build()

    // Use label_both for cyl, label_value for gear
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        facet_grid(rows: 'cyl', cols: 'gear',
            labeller: labeller(cyl: label_both(), gear: label_value()))

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // cyl should have "cyl:" prefix
    assertTrue(content.contains('cyl: 4') || content.contains('cyl: 6'), "Row labels should have 'cyl:' prefix")

    // gear should NOT have "gear:" prefix (just values)
    assertTrue(content.contains('>3<') || content.contains('>4<'), "Column labels should be value only")
  }

  @Test
  void testBackwardCompatibilityStringLabeller() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'category')
        .rows([
            [1, 2, 'A'],
            [2, 3, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    // Old string-based labeller should still work
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        facet_wrap(facets: 'category', labeller: 'both')

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Should contain labels with variable name (backward compatibility)
    assertTrue(content.contains('category:'), "Should contain 'category:' prefix")
  }

  @Test
  void testLabelWrapGenWithFacet() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'category')
        .rows([
            [1, 2, 'This is a very long category name'],
            [2, 3, 'Short']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        facet_wrap(facets: 'category', labeller: label_wrap_gen(15))

    Svg svg = chart.render()
    assertNotNull(svg, "Chart should render successfully")

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<svg'), "Should contain SVG root element")
    // Note: SVG text wrapping is complex and depends on implementation
    // This test just verifies the labeller is applied without errors
  }

  @Test
  void testMultiVariableFacetWithLabelBoth() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'cat1', 'cat2')
        .rows([
            [1, 2, 'A', 'X'],
            [2, 3, 'A', 'Y'],
            [3, 1, 'B', 'X']
        ])
        .types(Integer, Integer, String, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        facet_wrap(facets: ['cat1', 'cat2'], labeller: label_both())

    Svg svg = chart.render()
    String content = SvgWriter.toXml(svg)

    // Should contain labels with both variable names
    assertTrue(content.contains('cat1:'), "Should contain 'cat1:' prefix")
    assertTrue(content.contains('cat2:'), "Should contain 'cat2:' prefix")
  }
}
