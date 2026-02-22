package charm.render.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.core.Matrix

import static charm.render.CharmRenderTestUtil.loadSimpleCsvResource
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot

import testutil.Slow

@Slow
class P0GeomRendererTest {

  @Test
  void testPointRepresentativeAndEmptyEdge() {
    Chart chart = buildChart('p0/geoms/point_identity_cartesian.csv', CharmGeomType.POINT, [x: 'x', y: 'y', group: 'group'])
    assertTrue(countClass(chart.render(), 'charm-point') > 0)

    Chart empty = buildEmptyLike('p0/geoms/point_identity_cartesian.csv', CharmGeomType.POINT, [x: 'x', y: 'y', group: 'group'])
    assertEquals(0, countClass(empty.render(), 'charm-point'))
  }

  @Test
  void testLineRepresentativeAndEmptyEdge() {
    Chart chart = buildChart('p0/geoms/line_identity_cartesian.csv', CharmGeomType.LINE, [x: 'x', y: 'y', group: 'group'])
    assertTrue(countClass(chart.render(), 'charm-line') > 0)

    Chart empty = buildEmptyLike('p0/geoms/line_identity_cartesian.csv', CharmGeomType.LINE, [x: 'x', y: 'y', group: 'group'])
    assertEquals(0, countClass(empty.render(), 'charm-line'))
  }

  @Test
  void testBarRepresentativeAndEmptyEdge() {
    Chart chart = buildChart('p0/geoms/bar_count_cartesian.csv', CharmGeomType.BAR, [x: 'category'])
    assertTrue(countClass(chart.render(), 'charm-bar') > 0)

    Chart empty = buildEmptyLike('p0/geoms/bar_count_cartesian.csv', CharmGeomType.BAR, [x: 'category'])
    assertEquals(0, countClass(empty.render(), 'charm-bar'))
  }

  @Test
  void testColRepresentativeAndEmptyEdge() {
    Chart chart = buildChart('p0/geoms/col_identity_cartesian.csv', CharmGeomType.COL, [x: 'category', y: 'value', group: 'group'])
    assertTrue(countClass(chart.render(), 'charm-bar') > 0)

    Chart empty = buildEmptyLike('p0/geoms/col_identity_cartesian.csv', CharmGeomType.COL, [x: 'category', y: 'value', group: 'group'])
    assertEquals(0, countClass(empty.render(), 'charm-bar'))
  }

  @Test
  void testHistogramRepresentativeAndEmptyEdge() {
    Chart chart = buildChart('p0/geoms/histogram_bin_cartesian.csv', CharmGeomType.HISTOGRAM, [x: 'x'])
    assertTrue(countClass(chart.render(), 'charm-histogram') > 0)

    Chart empty = buildEmptyLike('p0/geoms/histogram_bin_cartesian.csv', CharmGeomType.HISTOGRAM, [x: 'x'])
    assertEquals(0, countClass(empty.render(), 'charm-histogram'))
  }

  @Test
  void testBoxplotRepresentativeAndEmptyEdge() {
    Chart chart = buildChart('p0/geoms/boxplot_boxplot_cartesian.csv', CharmGeomType.BOXPLOT, [x: 'group', y: 'value'])
    assertTrue(countClass(chart.render(), 'charm-boxplot-box') > 0)

    Chart empty = buildEmptyLike('p0/geoms/boxplot_boxplot_cartesian.csv', CharmGeomType.BOXPLOT, [x: 'group', y: 'value'])
    assertEquals(0, countClass(empty.render(), 'charm-boxplot-box'))
  }

  @Test
  void testAreaRepresentativeAndEmptyEdge() {
    Chart chart = buildChart('p0/geoms/area_identity_cartesian.csv', CharmGeomType.AREA, [x: 'x', y: 'y', group: 'series'])
    assertTrue(countClass(chart.render(), 'charm-area') > 0)

    Chart empty = buildEmptyLike('p0/geoms/area_identity_cartesian.csv', CharmGeomType.AREA, [x: 'x', y: 'y', group: 'series'])
    assertEquals(0, countClass(empty.render(), 'charm-area'))
  }

  @Test
  void testSmoothRepresentativeAndEmptyEdge() {
    Chart chart = buildChart('p0/geoms/smooth_smooth_cartesian.csv', CharmGeomType.SMOOTH, [x: 'x', y: 'y', group: 'group'])
    assertTrue(countClass(chart.render(), 'charm-smooth') > 0)

    Chart empty = buildEmptyLike('p0/geoms/smooth_smooth_cartesian.csv', CharmGeomType.SMOOTH, [x: 'x', y: 'y', group: 'group'])
    assertEquals(0, countClass(empty.render(), 'charm-smooth'))
  }

  @Test
  void testDensityRepresentativeAndEmptyEdge() {
    Chart chart = buildChart('p0/geoms/density_density_cartesian.csv', CharmGeomType.DENSITY, [x: 'x', group: 'group'])
    assertTrue(countClass(chart.render(), 'charm-density') > 0)

    Chart empty = buildEmptyLike('p0/geoms/density_density_cartesian.csv', CharmGeomType.DENSITY, [x: 'x', group: 'group'])
    assertEquals(0, countClass(empty.render(), 'charm-density'))
  }

  @Test
  void testViolinRepresentativeAndEmptyEdge() {
    Chart chart = buildChart('p0/geoms/violin_ydensity_cartesian.csv', CharmGeomType.VIOLIN, [x: 'group', y: 'y'])
    assertTrue(countClass(chart.render(), 'charm-violin') > 0)

    Chart empty = buildEmptyLike('p0/geoms/violin_ydensity_cartesian.csv', CharmGeomType.VIOLIN, [x: 'group', y: 'y'])
    assertEquals(0, countClass(empty.render(), 'charm-violin'))
  }

  @Test
  void testTileRepresentativeAndEmptyEdge() {
    Chart chart = buildChart('p0/geoms/tile_identity_cartesian.csv', CharmGeomType.TILE, [x: 'x', y: 'y', fill: 'fill'])
    assertTrue(countClass(chart.render(), 'charm-tile') > 0)

    Chart empty = buildEmptyLike('p0/geoms/tile_identity_cartesian.csv', CharmGeomType.TILE, [x: 'x', y: 'y', fill: 'fill'])
    assertEquals(0, countClass(empty.render(), 'charm-tile'))
  }

  @Test
  void testTextRepresentativeAndEmptyEdge() {
    Chart chart = buildChart('p0/geoms/text_identity_cartesian.csv', CharmGeomType.TEXT, [x: 'x', y: 'y', label: 'label'])
    assertTrue(countClass(chart.render(), 'charm-text') > 0)

    Chart empty = buildEmptyLike('p0/geoms/text_identity_cartesian.csv', CharmGeomType.TEXT, [x: 'x', y: 'y', label: 'label'])
    assertEquals(0, countClass(empty.render(), 'charm-text'))
  }

  private static Chart buildChart(String fixturePath, CharmGeomType geomType, Map<String, String> mapping) {
    Matrix data = loadFixtureMatrix("charm-parity/${fixturePath}")
    plot(data) {
      aes(mapping)
      layer(geomType, [:])
      theme {
        legend { position = 'none' }
      }
    }.build()
  }

  private static Chart buildEmptyLike(String fixturePath, CharmGeomType geomType, Map<String, String> mapping) {
    Matrix data = loadFixtureMatrix("charm-parity/${fixturePath}")
    Matrix empty = Matrix.builder()
        .columnNames(data.columnNames())
        .rows([])
        .build()
    plot(empty) {
      aes(mapping)
      layer(geomType, [:])
      theme {
        legend { position = 'none' }
      }
    }.build()
  }

  private static int countClass(Svg svg, String className) {
    String xml = SvgWriter.toXml(svg)
    String token = "class=\"${className}\""
    int idx = 0
    int count = 0
    while (true) {
      idx = xml.indexOf(token, idx)
      if (idx < 0) {
        break
      }
      count++
      idx += token.length()
    }
    count
  }

  private static Matrix loadFixtureMatrix(String resourcePath) {
    List<List<String>> rows = loadSimpleCsvResource(resourcePath)
    List<String> header = rows.first()
    List<List<Object>> dataRows = rows.tail().collect { List<String> row ->
      row.collect { String raw ->
        coerceValue(raw)
      } as List<Object>
    }

    Matrix.builder()
        .columnNames(header)
        .rows(dataRows)
        .build()
  }

  private static Object coerceValue(String raw) {
    if (raw == null || raw.isBlank()) {
      return null
    }
    if (raw ==~ /^-?\d+$/) {
      return Integer.valueOf(raw)
    }
    if (raw ==~ /^-?\d+\.\d+$/) {
      return new BigDecimal(raw)
    }
    raw
  }
}
