package charm.render.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.CssAttributesSpec
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot

class P2GeomRendererTest {

  @Test
  void testP2GeomRendering() {
    Matrix data = p2Data()
    List<Map<String, Object>> cases = [
        [geom: CharmGeomType.BIN2D, aes: [x: 'x', y: 'y'], options: [:], token: 'gg-bin2d'],
        [geom: CharmGeomType.CONTOUR_FILLED, aes: [x: 'x', y: 'y', group: 'group'], options: [:], token: 'gg-contour-filled'],
        [geom: CharmGeomType.COUNT, aes: [x: 'x', y: 'y'], options: [:], token: 'gg-count'],
        [geom: CharmGeomType.CURVE, aes: [x: 'x', y: 'y', xend: 'xend', yend: 'yend'], options: [curvature: 0.3], token: 'gg-curve'],
        [geom: CharmGeomType.DENSITY_2D, aes: [x: 'x', y: 'y'], options: [stat: CharmStatType.DENSITY_2D], token: null],
        [geom: CharmGeomType.DENSITY_2D_FILLED, aes: [x: 'x', y: 'y'], options: [stat: CharmStatType.DENSITY_2D], token: null],
        [geom: CharmGeomType.DOTPLOT, aes: [x: 'x', y: 'y'], options: [:], token: 'gg-dotplot'],
        [geom: CharmGeomType.FUNCTION, aes: [x: 'x'], options: [stat: CharmStatType.FUNCTION, fun: { Number x -> (x as BigDecimal) * 0.5 }], token: 'gg-function'],
        [geom: CharmGeomType.LOGTICKS, aes: [x: 'x', y: 'y'], options: [:], token: 'gg-logticks'],
        [geom: CharmGeomType.MAG, aes: [x: 'x', y: 'y', color: 'group'], options: [:], token: 'gg-mag'],
        [geom: CharmGeomType.MAP, aes: [x: 'x', y: 'y', group: 'group'], options: [:], token: 'gg-map'],
        [geom: CharmGeomType.PARALLEL, aes: [x: 'x', y: 'y', group: 'group'], options: [:], token: 'gg-parallel'],
        [geom: CharmGeomType.QQ, aes: [x: 'x', y: 'y'], options: [:], token: 'gg-qq'],
        [geom: CharmGeomType.QQ_LINE, aes: [x: 'x', y: 'y', group: 'group'], options: [:], token: 'gg-qq-line'],
        [geom: CharmGeomType.QUANTILE, aes: [x: 'x', y: 'y', group: 'group'], options: [:], token: 'gg-quantile'],
        [geom: CharmGeomType.RASTER, aes: [x: 'x', y: 'y', fill: 'y'], options: [:], token: 'gg-raster'],
        [geom: CharmGeomType.RASTER_ANN, aes: [x: 'x', y: 'y', fill: 'y'], options: [:], token: 'gg-raster-ann'],
        [geom: CharmGeomType.SPOKE, aes: [x: 'x', y: 'y'], options: [angle: 'angle', radius: 'radius'], token: 'gg-spoke'],
        [geom: CharmGeomType.SF, aes: [label: 'label'], options: [stat: CharmStatType.SF, geometry: 'geometry'], token: 'gg-sf'],
        [geom: CharmGeomType.SF_LABEL, aes: [label: 'label'], options: [stat: CharmStatType.SF_COORDINATES, geometry: 'geometry'], token: 'gg-sf-label'],
        [geom: CharmGeomType.SF_TEXT, aes: [label: 'label'], options: [stat: CharmStatType.SF_COORDINATES, geometry: 'geometry'], token: 'gg-sf-text']
    ]

    cases.each { Map<String, Object> tc ->
      Chart chart = plot(data) {
        aes(tc.aes as Map<String, String>)
        layer(tc.geom as CharmGeomType, tc.options as Map<String, Object>)
      }.build()

      String svg = SvgWriter.toXml(withCssClasses(chart).render())
      assertTrue(svg.contains('<svg'))
      if (tc.token != null) {
        assertTrue(svg.contains("class=\"${tc.token}\""), "Expected ${tc.geom} to emit ${tc.token}")
      }
    }
  }

  @Test
  void testBlankAndCustomGeomDoNotRenderElements() {
    Matrix data = p2Data()

    [CharmGeomType.BLANK, CharmGeomType.CUSTOM].each { CharmGeomType geom ->
      Chart chart = plot(data) {
        aes([x: 'x', y: 'y'])
        layer(geom, [:])
      }.build()

      String svg = SvgWriter.toXml(withCssClasses(chart).render())
      assertTrue(svg.contains('<svg'))
      assertFalse(svg.contains("gg-${geom.name().toLowerCase().replace('_', '-')}"))
    }
  }

  private static Chart withCssClasses(Chart chart) {
    new Chart(
        chart.data,
        chart.aes,
        chart.layers,
        chart.scale,
        chart.theme,
        chart.facet,
        chart.coord,
        chart.labels,
        chart.guides,
        chart.annotations,
        new CssAttributesSpec(enabled: true, includeIds: false, includeClasses: true, includeDataAttributes: false)
    )
  }

  private static Matrix p2Data() {
    Matrix.builder()
        .columnNames('x', 'y', 'xend', 'yend', 'group', 'label', 'angle', 'radius', 'geometry')
        .rows([
            [1.0, 1.0, 1.5, 1.4, 'g1', 'A', 0.0, 0.7, 'POLYGON((0 0, 1 0, 1 1, 0 0))'],
            [2.0, 1.7, 2.5, 2.1, 'g1', 'B', 0.4, 0.8, 'POLYGON((2 0, 3 0, 3 1, 2 0))'],
            [3.0, 2.4, 3.4, 2.8, 'g1', 'C', 0.8, 0.9, 'POLYGON((4 0, 5 0, 5 1, 4 0))'],
            [1.3, 2.8, 1.8, 3.3, 'g2', 'D', 1.2, 1.0, 'POINT(1 3)'],
            [2.3, 3.3, 2.8, 3.8, 'g2', 'E', 1.6, 1.1, 'LINESTRING(0 4, 1 5, 2 6)'],
            [3.3, 3.9, 3.8, 4.2, 'g2', 'F', 2.0, 1.2, 'MULTIPOINT((2 2), (3 3))']
        ])
        .build()
  }
}
