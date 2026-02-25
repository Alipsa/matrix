package charm.render

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.FacetType
import se.alipsa.matrix.charm.Theme
import se.alipsa.matrix.charm.theme.CharmThemes
import se.alipsa.matrix.charm.theme.ElementRect
import se.alipsa.matrix.charm.theme.ElementText
import se.alipsa.matrix.charm.render.FacetRenderer
import se.alipsa.matrix.charm.render.PanelSpec
import se.alipsa.matrix.charm.ColumnRef
import se.alipsa.matrix.charm.facet.Labeller
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.charm.Charts.plot

class CharmFacetThemeTest {

  private static Matrix facetData() {
    Matrix.builder()
        .columnNames('x', 'y', 'cat', 'grp')
        .rows([
            [1, 2, 'A', 'G1'],
            [2, 4, 'A', 'G1'],
            [3, 6, 'B', 'G1'],
            [4, 8, 'B', 'G2'],
            [5, 10, 'C', 'G2'],
            [6, 12, 'C', 'G2']
        ])
        .build()
  }

  @Test
  void testWrapSingleVariable() {
    FacetRenderer renderer = new FacetRenderer()
    Matrix data = facetData()

    List<PanelSpec> panels = renderer.computePanels(
        data, FacetType.WRAP,
        [], [], [new ColumnRef('cat')],
        null, null, [:]
    )
    assertEquals(3, panels.size(), 'Should have 3 panels for 3 cat levels')
    panels.each { PanelSpec p ->
      assertTrue(p.label != null && !p.label.isEmpty())
      assertTrue(!p.rowIndexes.isEmpty())
    }
  }

  @Test
  void testWrapMultiVariable() {
    FacetRenderer renderer = new FacetRenderer()
    Matrix data = facetData()

    List<PanelSpec> panels = renderer.computePanels(
        data, FacetType.WRAP,
        [], [], [new ColumnRef('cat'), new ColumnRef('grp')],
        null, null, [:]
    )
    // Should create composite panels for unique cat+grp combos
    assertTrue(panels.size() >= 3, 'Should have panels for cat+grp combos')
    panels.each { PanelSpec p ->
      assertNotNull(p.facetValues)
      assertTrue(p.facetValues.containsKey('cat'))
      assertTrue(p.facetValues.containsKey('grp'))
    }
  }

  @Test
  void testWrapWithNcol() {
    FacetRenderer renderer = new FacetRenderer()
    Matrix data = facetData()

    List<PanelSpec> panels = renderer.computePanels(
        data, FacetType.WRAP,
        [], [], [new ColumnRef('cat')],
        2, null, [:]
    )
    assertEquals(3, panels.size())
    // With ncol=2, first 2 should be in row 0
    assertTrue(panels[0].row == 0 && panels[0].col == 0)
    assertTrue(panels[1].row == 0 && panels[1].col == 1)
    assertTrue(panels[2].row == 1 && panels[2].col == 0)
  }

  @Test
  void testWrapWithNrow() {
    FacetRenderer renderer = new FacetRenderer()
    Matrix data = facetData()

    List<PanelSpec> panels = renderer.computePanels(
        data, FacetType.WRAP,
        [], [], [new ColumnRef('cat')],
        null, 1, [:]
    )
    assertEquals(3, panels.size())
    // With nrow=1, all should be in row 0
    panels.each { PanelSpec p ->
      assertEquals(0, p.row)
    }
  }

  @Test
  void testWrapVerticalDir() {
    FacetRenderer renderer = new FacetRenderer()
    Matrix data = facetData()

    List<PanelSpec> panels = renderer.computePanels(
        data, FacetType.WRAP,
        [], [], [new ColumnRef('cat')],
        null, null, [dir: 'v']
    )
    assertEquals(3, panels.size())
    // With vertical fill and auto-computed 2 columns, 2 rows
    // Panels should fill vertically
    assertTrue(panels[0].col == 0)
  }

  @Test
  void testWrapWithLabeller() {
    FacetRenderer renderer = new FacetRenderer()
    Matrix data = facetData()

    Labeller lab = new Labeller({ Map<String, Object> vals ->
      vals.collect { k, v -> "${k}: ${v}" }.join(', ')
    })

    List<PanelSpec> panels = renderer.computePanels(
        data, FacetType.WRAP,
        [], [], [new ColumnRef('cat')],
        null, null, [labeller: lab]
    )
    panels.each { PanelSpec p ->
      assertTrue(p.label.contains('cat:'), "Label should use labeller format: ${p.label}")
    }
  }

  @Test
  void testGridSingleRowCol() {
    FacetRenderer renderer = new FacetRenderer()
    Matrix data = facetData()

    List<PanelSpec> panels = renderer.computePanels(
        data, FacetType.GRID,
        [new ColumnRef('cat')], [new ColumnRef('grp')],
        [], null, null, [:]
    )
    // 3 cat levels x 2 grp levels = 6 panels
    assertEquals(6, panels.size())
    // Verify rowLabel and colLabel are set
    panels.each { PanelSpec p ->
      assertNotNull(p.rowLabel)
      assertNotNull(p.colLabel)
    }
  }

  @Test
  void testGridWithMargins() {
    FacetRenderer renderer = new FacetRenderer()
    Matrix data = facetData()

    List<PanelSpec> panels = renderer.computePanels(
        data, FacetType.GRID,
        [new ColumnRef('cat')], [new ColumnRef('grp')],
        [], null, null, [margins: true]
    )
    // 3 cat x 2 grp = 6 base panels
    // + 3 row margins + 2 col margins + 1 global = 6 margin panels
    assertTrue(panels.size() > 6, "With margins, should have more than 6 panels (got ${panels.size()})")
    assertTrue(panels.any { PanelSpec p -> p.label == '(all)' }, 'Should have margin panels')
  }

  @Test
  void testGridBothLabeller() {
    FacetRenderer renderer = new FacetRenderer()
    Matrix data = facetData()

    List<PanelSpec> panels = renderer.computePanels(
        data, FacetType.GRID,
        [new ColumnRef('cat')], [new ColumnRef('grp')],
        [], null, null, [labeller: 'both']
    )
    panels.each { PanelSpec p ->
      if (p.label && !p.label.isEmpty()) {
        assertTrue(p.label.contains(':'), "Label with 'both' should have 'key: value' format: ${p.label}")
      }
    }
  }

  @Test
  void testFacetedChartWithBwThemeRenders() {
    Chart chart = plot(facetData()) {
      mapping { x = col.x; y = col.y }
      points {}
      facet {
        wrap {
          vars = ['cat']
          ncol = 2
        }
      }
    }.build()

    // Build with bw theme manually
    se.alipsa.matrix.charm.ThemeSpec ts = new se.alipsa.matrix.charm.ThemeSpec()
    CharmThemes.bw().properties.each { key, val ->
      String k = key as String
      if (k != 'class' && ts.hasProperty(k)) {
        ts.setProperty(k, val)
      }
    }

    Chart themed = new Chart(
        facetData(),
        chart.mapping,
        chart.layers,
        chart.scale,
        ts,
        chart.facet,
        chart.coord,
        chart.labels,
        null,
        chart.annotations
    )

    Svg svg = themed.render()
    String svgText = se.alipsa.groovy.svg.io.SvgWriter.toXml(svg)
    assertTrue(svgText.contains('charm-strip'), 'Faceted chart with BW theme should have strip')
  }

  @Test
  void testFacetedChartWithCustomStripTheme() {
    // Theme with custom strip styling
    se.alipsa.matrix.charm.ThemeSpec ts = new se.alipsa.matrix.charm.ThemeSpec()
    ts.stripBackground = new ElementRect(fill: '#FF6600', color: '#CC5500')
    ts.stripText = new ElementText(color: 'white', size: 14, family: 'monospace')

    Chart chart = new Chart(
        facetData(),
        new se.alipsa.matrix.charm.MappingSpec().tap { apply([x: 'x', y: 'y']) },
        [new se.alipsa.matrix.charm.LayerSpec(
            se.alipsa.matrix.charm.GeomSpec.of(se.alipsa.matrix.charm.CharmGeomType.POINT),
            se.alipsa.matrix.charm.StatSpec.of(se.alipsa.matrix.charm.CharmStatType.IDENTITY),
            null, true,
            se.alipsa.matrix.charm.PositionSpec.of(se.alipsa.matrix.charm.CharmPositionType.IDENTITY),
            [:]
        )],
        new se.alipsa.matrix.charm.ScaleSpec(),
        ts,
        new se.alipsa.matrix.charm.FacetSpec(
            type: FacetType.WRAP,
            vars: [new ColumnRef('cat')]
        ),
        new se.alipsa.matrix.charm.CoordSpec(),
        new se.alipsa.matrix.charm.LabelsSpec(),
        null,
        []
    )

    Svg svg = chart.render()
    String svgText = se.alipsa.groovy.svg.io.SvgWriter.toXml(svg)
    assertTrue(svgText.contains('#FF6600'), 'Custom strip background color should be in SVG')
    assertTrue(svgText.contains('monospace'), 'Custom strip font family should be in SVG')
  }
}
