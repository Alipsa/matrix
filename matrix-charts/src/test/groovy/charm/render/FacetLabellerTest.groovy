package charm.render

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.charm.Charts.plot

import org.junit.jupiter.api.Test

import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.Text
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.facet.Labeller
import se.alipsa.matrix.core.Matrix

/**
 * Tests for facet labeller convenience factories and DSL integration.
 */
class FacetLabellerTest {

  // ---- Labeller factory unit tests ----

  @Test
  void testValueLabellerShowsValueOnly() {
    Labeller labeller = Labeller.value()
    String result = labeller.label([Species: 'setosa'])
    assertEquals('setosa', result)
  }

  @Test
  void testValueLabellerMultiVariable() {
    Labeller labeller = Labeller.value()
    String result = labeller.label([Species: 'setosa', Year: '2020'])
    assertEquals('setosa\n2020', result)
  }

  @Test
  void testValueLabellerSingleLine() {
    Labeller labeller = Labeller.value(false)
    String result = labeller.label([Species: 'setosa', Year: '2020'])
    assertEquals('setosa, 2020', result)
  }

  @Test
  void testBothLabellerShowsVariableAndValue() {
    Labeller labeller = Labeller.both()
    String result = labeller.label([Species: 'setosa'])
    assertEquals('Species: setosa', result)
  }

  @Test
  void testBothLabellerMultiVariable() {
    Labeller labeller = Labeller.both()
    String result = labeller.label([Species: 'setosa', Year: '2020'])
    assertEquals('Species: setosa\nYear: 2020', result)
  }

  @Test
  void testBothLabellerCustomSeparator() {
    Labeller labeller = Labeller.both(' = ')
    String result = labeller.label([Species: 'setosa'])
    assertEquals('Species = setosa', result)
  }

  @Test
  void testCustomLabellerClosure() {
    Labeller labeller = Labeller.label { Map<String, Object> vals ->
      vals.collect { k, v -> "${v} [${k}]" }.join(' | ')
    }
    String result = labeller.label([Species: 'setosa', Year: '2020'])
    assertEquals('setosa [Species] | 2020 [Year]', result)
  }

  // ---- DSL integration tests ----

  @Test
  void testFacetDslWithValueLabeller() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'grp')
        .rows([
            [1, 2, 'A'], [2, 4, 'B'], [3, 6, 'A'],
            [4, 3, 'B'], [5, 5, 'A'], [6, 7, 'B']
        ])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      facet {
        wrap {
          vars = ['grp']
          ncol = 2
        }
        labeller = value()
      }
      layers { geomPoint() }
    }.build()

    Svg svg = chart.render()
    assertNotNull(svg)

    // Check that strip labels contain group values
    def textElements = svg.descendants().findAll { it instanceof Text }
    def allText = textElements.collect { (it as Text).content }.join(' ')
    assertTrue(allText.contains('A'), "Expected 'A' in facet strip labels")
    assertTrue(allText.contains('B'), "Expected 'B' in facet strip labels")
  }

  @Test
  void testFacetDslWithBothLabeller() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'grp')
        .rows([
            [1, 2, 'A'], [2, 4, 'B'], [3, 6, 'A'],
            [4, 3, 'B'], [5, 5, 'A'], [6, 7, 'B']
        ])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      facet {
        wrap {
          vars = ['grp']
          ncol = 2
        }
        labeller = both()
      }
      layers { geomPoint() }
    }.build()

    Svg svg = chart.render()
    assertNotNull(svg)

    def textElements = svg.descendants().findAll { it instanceof Text }
    def allText = textElements.collect { (it as Text).content }.join(' ')
    assertTrue(allText.contains('grp: A') || allText.contains('grp:'),
        "Expected 'grp: ...' format in facet strip labels, got: ${allText}")
  }

  @Test
  void testFacetDslWithCustomLabeller() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'grp')
        .rows([
            [1, 2, 'X'], [2, 4, 'Y'], [3, 6, 'X'],
            [4, 3, 'Y'], [5, 5, 'X'], [6, 7, 'Y']
        ])
        .build()

    Labeller custom = Labeller.label { Map<String, Object> vals ->
      "Group ${vals.values().first()}"
    }

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      facet {
        wrap {
          vars = ['grp']
          ncol = 2
        }
        labeller = custom
      }
      layers { geomPoint() }
    }.build()

    Svg svg = chart.render()
    assertNotNull(svg)

    def textElements = svg.descendants().findAll { it instanceof Text }
    def allText = textElements.collect { (it as Text).content }.join(' ')
    assertTrue(allText.contains('Group X'), "Expected 'Group X' in labels")
    assertTrue(allText.contains('Group Y'), "Expected 'Group Y' in labels")
  }

  @Test
  void testGridFacetWithBothLabeller() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'row_var', 'col_var')
        .rows([
            [1, 2, 'R1', 'C1'], [2, 4, 'R1', 'C2'],
            [3, 6, 'R2', 'C1'], [4, 3, 'R2', 'C2']
        ])
        .build()

    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      facet {
        rows = ['row_var']
        cols = ['col_var']
        labeller = both()
      }
      layers { geomPoint() }
    }.build()

    Svg svg = chart.render()
    assertNotNull(svg)

    def textElements = svg.descendants().findAll { it instanceof Text }
    def allText = textElements.collect { (it as Text).content }.join(' ')
    // Grid facets should show "variable: value" in strips
    assertTrue(allText.contains('R1') || allText.contains('row_var'),
        "Expected row strip labels")
    assertTrue(allText.contains('C1') || allText.contains('col_var'),
        "Expected col strip labels")
  }

  @Test
  void testWrapDslLabeller() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'cat')
        .rows([
            [1, 2, 'A'], [2, 4, 'B'], [3, 6, 'C']
        ])
        .build()

    // labeller set inside wrap {} block directly
    Chart chart = plot(data) {
      mapping { x = 'x'; y = 'y' }
      facet {
        wrap {
          vars = ['cat']
          ncol = 3
          labeller = Labeller.both()
        }
      }
      layers { geomPoint() }
    }.build()

    Svg svg = chart.render()
    assertNotNull(svg)

    def textElements = svg.descendants().findAll { it instanceof Text }
    def allText = textElements.collect { (it as Text).content }.join(' ')
    assertTrue(allText.contains('cat:') || allText.contains('cat'),
        "Expected labeller-formatted strips")
  }
}
