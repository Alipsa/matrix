package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.facet.Facet
import se.alipsa.matrix.gg.facet.FacetGrid
import se.alipsa.matrix.gg.facet.FacetWrap

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class FacetTest {

  // ==================== FacetWrap Tests ====================

  @Test
  void testFacetWrapDefaults() {
    FacetWrap facet = new FacetWrap()

    assertTrue(facet.facets.isEmpty())
    assertNull(facet.ncol)
    assertNull(facet.nrow)
    assertEquals('h', facet.dir)
    assertTrue(facet.drop)
    assertEquals('fixed', facet.scales)
    assertTrue(facet.strip)
  }

  @Test
  void testFacetWrapWithString() {
    FacetWrap facet = new FacetWrap('category')

    assertEquals(['category'], facet.facets)
    assertEquals(['category'], facet.getFacetVariables())
  }

  @Test
  void testFacetWrapWithList() {
    FacetWrap facet = new FacetWrap(['var1', 'var2'])

    assertEquals(['var1', 'var2'], facet.facets)
    assertEquals(['var1', 'var2'], facet.getFacetVariables())
  }

  @Test
  void testFacetWrapWithParams() {
    FacetWrap facet = new FacetWrap(
        facets: 'category',
        ncol: 3,
        scales: 'free',
        dir: 'v'
    )

    assertEquals(['category'], facet.facets)
    assertEquals(3, facet.ncol)
    assertEquals('free', facet.scales)
    assertEquals('v', facet.dir)
  }

  @Test
  void testFacetWrapComputeLayout() {
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([
            ['A', 10], ['A', 20],
            ['B', 15], ['B', 25],
            ['C', 30], ['C', 35],
            ['D', 40]
        ])
        .types(String, Integer)
        .build()

    // Auto layout (4 panels -> 2x2)
    FacetWrap facetAuto = new FacetWrap('category')
    Map<String, Integer> layoutAuto = facetAuto.computeLayout(data)
    assertEquals(2, layoutAuto.nrow)
    assertEquals(2, layoutAuto.ncol)

    // Specified ncol
    FacetWrap facetNcol = new FacetWrap(facets: 'category', ncol: 2)
    Map<String, Integer> layoutNcol = facetNcol.computeLayout(data)
    assertEquals(2, layoutNcol.nrow)
    assertEquals(2, layoutNcol.ncol)

    // Specified nrow
    FacetWrap facetNrow = new FacetWrap(facets: 'category', nrow: 1)
    Map<String, Integer> layoutNrow = facetNrow.computeLayout(data)
    assertEquals(1, layoutNrow.nrow)
    assertEquals(4, layoutNrow.ncol)
  }

  @Test
  void testFacetWrapGetPanelValues() {
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([
            ['A', 10], ['A', 20],
            ['B', 15], ['C', 30]
        ])
        .types(String, Integer)
        .build()

    FacetWrap facet = new FacetWrap('category')
    List<Map<String, Object>> panels = facet.getPanelValues(data)

    assertEquals(3, panels.size())
    // Should be sorted
    assertEquals('A', panels[0].category)
    assertEquals('B', panels[1].category)
    assertEquals('C', panels[2].category)
  }

  @Test
  void testFacetWrapFilterData() {
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([
            ['A', 10], ['A', 20],
            ['B', 15], ['B', 25]
        ])
        .types(String, Integer)
        .build()

    FacetWrap facet = new FacetWrap('category')
    Matrix filtered = facet.filterDataForPanel(data, [category: 'A'])

    assertEquals(2, filtered.rowCount())
    filtered.each { row ->
      assertEquals('A', row['category'])
    }
  }

  @Test
  void testFacetWrapPanelPosition() {
    FacetWrap facet = new FacetWrap(facets: 'cat', dir: 'h')
    Map<String, Integer> layout = [nrow: 2, ncol: 3]

    // Horizontal fill: 0,1,2 in first row; 3,4,5 in second row
    assertEquals([row: 0, col: 0], facet.getPanelPosition(0, layout))
    assertEquals([row: 0, col: 1], facet.getPanelPosition(1, layout))
    assertEquals([row: 0, col: 2], facet.getPanelPosition(2, layout))
    assertEquals([row: 1, col: 0], facet.getPanelPosition(3, layout))
  }

  @Test
  void testFacetWrapVerticalFill() {
    FacetWrap facet = new FacetWrap(facets: 'cat', dir: 'v')
    Map<String, Integer> layout = [nrow: 2, ncol: 3]

    // Vertical fill: 0,1 in first column; 2,3 in second column; etc.
    assertEquals([row: 0, col: 0], facet.getPanelPosition(0, layout))
    assertEquals([row: 1, col: 0], facet.getPanelPosition(1, layout))
    assertEquals([row: 0, col: 1], facet.getPanelPosition(2, layout))
    assertEquals([row: 1, col: 1], facet.getPanelPosition(3, layout))
  }

  // ==================== FacetGrid Tests ====================

  @Test
  void testFacetGridDefaults() {
    FacetGrid facet = new FacetGrid()

    assertTrue(facet.rows.isEmpty())
    assertTrue(facet.cols.isEmpty())
    assertFalse(facet.margins)
    assertEquals('fixed', facet.scales)
    assertTrue(facet.strip)
  }

  @Test
  void testFacetGridWithRows() {
    FacetGrid facet = new FacetGrid(rows: 'category')

    assertEquals(['category'], facet.rows)
    assertTrue(facet.cols.isEmpty())
    assertEquals(['category'], facet.getFacetVariables())
  }

  @Test
  void testFacetGridWithCols() {
    FacetGrid facet = new FacetGrid(cols: 'group')

    assertTrue(facet.rows.isEmpty())
    assertEquals(['group'], facet.cols)
    assertEquals(['group'], facet.getFacetVariables())
  }

  @Test
  void testFacetGridWithRowsAndCols() {
    FacetGrid facet = new FacetGrid(rows: 'row_var', cols: 'col_var')

    assertEquals(['row_var'], facet.rows)
    assertEquals(['col_var'], facet.cols)
    assertEquals(['row_var', 'col_var'], facet.getFacetVariables())
  }

  @Test
  void testFacetGridComputeLayout() {
    def data = Matrix.builder()
        .columnNames('row_var', 'col_var', 'value')
        .rows([
            ['R1', 'C1', 10], ['R1', 'C2', 20],
            ['R2', 'C1', 15], ['R2', 'C2', 25],
            ['R3', 'C1', 30], ['R3', 'C2', 35]
        ])
        .types(String, String, Integer)
        .build()

    FacetGrid facet = new FacetGrid(rows: 'row_var', cols: 'col_var')
    Map<String, Integer> layout = facet.computeLayout(data)

    assertEquals(3, layout.nrow)  // 3 unique row values
    assertEquals(2, layout.ncol)  // 2 unique column values
  }

  @Test
  void testFacetGridGetPanelValues() {
    def data = Matrix.builder()
        .columnNames('row_var', 'col_var', 'value')
        .rows([
            ['R1', 'C1', 10], ['R1', 'C2', 20],
            ['R2', 'C1', 15], ['R2', 'C2', 25]
        ])
        .types(String, String, Integer)
        .build()

    FacetGrid facet = new FacetGrid(rows: 'row_var', cols: 'col_var')
    List<Map<String, Object>> panels = facet.getPanelValues(data)

    assertEquals(4, panels.size())  // 2 rows × 2 cols
  }

  @Test
  void testFacetGridRowsOnly() {
    def data = Matrix.builder()
        .columnNames('row_var', 'value')
        .rows([
            ['R1', 10], ['R1', 20],
            ['R2', 15], ['R2', 25]
        ])
        .types(String, Integer)
        .build()

    FacetGrid facet = new FacetGrid(rows: 'row_var')
    Map<String, Integer> layout = facet.computeLayout(data)

    assertEquals(2, layout.nrow)
    assertEquals(1, layout.ncol)
  }

  @Test
  void testFacetGridColsOnly() {
    def data = Matrix.builder()
        .columnNames('col_var', 'value')
        .rows([
            ['C1', 10], ['C1', 20],
            ['C2', 15], ['C2', 25]
        ])
        .types(String, Integer)
        .build()

    FacetGrid facet = new FacetGrid(cols: 'col_var')
    Map<String, Integer> layout = facet.computeLayout(data)

    assertEquals(1, layout.nrow)
    assertEquals(2, layout.ncol)
  }

  @Test
  void testFacetGridLabels() {
    def data = Matrix.builder()
        .columnNames('row_var', 'col_var', 'value')
        .rows([
            ['R1', 'C1', 10], ['R1', 'C2', 20],
            ['R2', 'C1', 15], ['R2', 'C2', 25]
        ])
        .types(String, String, Integer)
        .build()

    FacetGrid facet = new FacetGrid(rows: 'row_var', cols: 'col_var')

    assertEquals('R1', facet.getRowLabel(0, data))
    assertEquals('R2', facet.getRowLabel(1, data))
    assertEquals('C1', facet.getColLabel(0, data))
    assertEquals('C2', facet.getColLabel(1, data))
  }

  @Test
  void testFacetGridLabelerBoth() {
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([
            ['A', 10], ['B', 20]
        ])
        .types(String, Integer)
        .build()

    FacetGrid facet = new FacetGrid(rows: 'category', labeller: 'both')

    assertEquals('category: A', facet.getRowLabel(0, data))
    assertEquals('category: B', facet.getRowLabel(1, data))
  }

  // ==================== Integration Tests ====================

  @Test
  void testFacetWrapScatterPlot() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'category')
        .rows([
            [1, 10, 'A'], [2, 20, 'A'], [3, 15, 'A'],
            [1, 25, 'B'], [2, 30, 'B'], [3, 35, 'B'],
            [1, 5, 'C'], [2, 8, 'C'], [3, 12, 'C']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(fill: 'steelblue') +
        facet_wrap(facets: 'category', ncol: 3) +
        labs(title: 'Faceted Scatter Plot', x: 'X', y: 'Y')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain points")
    assertTrue(content.contains('panel-'), "Should contain panel groups")

    File outputFile = new File('build/facet_wrap_scatter.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFacetWrapBarChart() {
    def data = Matrix.builder()
        .columnNames('item', 'value', 'category')
        .rows([
            ['X', 10, 'A'], ['Y', 20, 'A'], ['Z', 15, 'A'],
            ['X', 25, 'B'], ['Y', 30, 'B'], ['Z', 35, 'B']
        ])
        .types(String, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'item', y: 'value')) +
        geom_col(fill: 'steelblue') +
        facet_wrap(facets: 'category', ncol: 2) +
        labs(title: 'Faceted Bar Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<rect'), "Should contain bar rectangles")

    File outputFile = new File('build/facet_wrap_bar.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFacetGridScatterPlot() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'row_var', 'col_var')
        .rows([
            [1, 10, 'R1', 'C1'], [2, 20, 'R1', 'C1'],
            [1, 15, 'R1', 'C2'], [2, 25, 'R1', 'C2'],
            [1, 30, 'R2', 'C1'], [2, 35, 'R2', 'C1'],
            [1, 5, 'R2', 'C2'], [2, 10, 'R2', 'C2']
        ])
        .types(Integer, Integer, String, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(fill: 'steelblue') +
        facet_grid(rows: 'row_var', cols: 'col_var') +
        labs(title: 'Facet Grid Scatter Plot')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain points")

    File outputFile = new File('build/facet_grid_scatter.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFacetWrapWithLine() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'group')
        .rows([
            [1, 10, 'A'], [2, 15, 'A'], [3, 20, 'A'], [4, 18, 'A'],
            [1, 5, 'B'], [2, 12, 'B'], [3, 8, 'B'], [4, 15, 'B'],
            [1, 20, 'C'], [2, 25, 'C'], [3, 22, 'C'], [4, 28, 'C']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line(color: 'steelblue') +
        geom_point(fill: 'steelblue') +
        facet_wrap(facets: 'group', nrow: 1) +
        labs(title: 'Faceted Line Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<line'), "Should contain line segments")
    assertTrue(content.contains('<circle'), "Should contain points")

    File outputFile = new File('build/facet_wrap_line.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFacetWithFreeScales() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'category')
        .rows([
            [1, 10, 'A'], [2, 15, 'A'],
            [10, 100, 'B'], [20, 150, 'B']  // Different scale
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(fill: 'steelblue') +
        facet_wrap(facets: 'category', scales: 'free') +
        labs(title: 'Faceted with Free Scales')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/facet_free_scales.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFacetWrapNoStrip() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'category')
        .rows([
            [1, 10, 'A'], [2, 20, 'A'],
            [1, 15, 'B'], [2, 25, 'B']
        ])
        .types(Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(fill: 'steelblue') +
        facet_wrap(facets: 'category', strip: false) +
        labs(title: 'Faceted without Strips')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/facet_no_strip.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFacetGridRowsOnlyWithHistogram() {
    def data = Matrix.builder()
        .columnNames('value', 'group')
        .rows([
            [1.0, 'A'], [1.5, 'A'], [2.0, 'A'], [2.5, 'A'], [3.0, 'A'],
            [4.0, 'B'], [4.5, 'B'], [5.0, 'B'], [5.5, 'B'], [6.0, 'B']
        ])
        .types(Double, String)
        .build()

    def chart = ggplot(data, aes(x: 'value')) +
        geom_histogram(bins: 5, fill: 'steelblue') +
        facet_grid(rows: 'group') +
        labs(title: 'Faceted Histogram')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<rect'), "Should contain histogram bars")

    File outputFile = new File('build/facet_grid_histogram.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFactoryMethods() {
    // Test facet_wrap(String)
    def fw1 = facet_wrap('category')
    assertTrue(fw1 instanceof FacetWrap)
    assertEquals(['category'], (fw1 as FacetWrap).facets)

    // Test facet_wrap(List)
    def fw2 = facet_wrap(['var1', 'var2'])
    assertTrue(fw2 instanceof FacetWrap)
    assertEquals(['var1', 'var2'], (fw2 as FacetWrap).facets)

    // Test facet_wrap(Map)
    def fw3 = facet_wrap(facets: 'cat', ncol: 3)
    assertTrue(fw3 instanceof FacetWrap)
    assertEquals(3, (fw3 as FacetWrap).ncol)

    // Test facet_grid(Map)
    def fg = facet_grid(rows: 'row', cols: 'col')
    assertTrue(fg instanceof FacetGrid)
    assertEquals(['row'], (fg as FacetGrid).rows)
    assertEquals(['col'], (fg as FacetGrid).cols)
  }

  @Test
  void testFacetScaleOptions() {
    FacetWrap facet1 = new FacetWrap(facets: 'cat', scales: 'fixed')
    assertFalse(facet1.isFreeX())
    assertFalse(facet1.isFreeY())

    FacetWrap facet2 = new FacetWrap(facets: 'cat', scales: 'free')
    assertTrue(facet2.isFreeX())
    assertTrue(facet2.isFreeY())

    FacetWrap facet3 = new FacetWrap(facets: 'cat', scales: 'free_x')
    assertTrue(facet3.isFreeX())
    assertFalse(facet3.isFreeY())

    FacetWrap facet4 = new FacetWrap(facets: 'cat', scales: 'free_y')
    assertFalse(facet4.isFreeX())
    assertTrue(facet4.isFreeY())
  }
}
