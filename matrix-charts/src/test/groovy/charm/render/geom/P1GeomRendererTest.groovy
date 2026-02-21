package charm.render.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.charm.Chart
import se.alipsa.matrix.charm.CssAttributesSpec
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.charm.Charts.plot

class P1GeomRendererTest {

  @Test
  void testP1GeomRendering() {
    Matrix data = p1Data()
    List<Map<String, Object>> cases = [
        [geom: CharmGeomType.JITTER, aes: [x: 'x', y: 'y'], options: [position: CharmPositionType.JITTER], css: 'charm-point'],
        [geom: CharmGeomType.STEP, aes: [x: 'x', y: 'y', group: 'group'], options: [:], css: 'charm-step'],
        [geom: CharmGeomType.ERRORBAR, aes: [x: 'x', ymin: 'ymin', ymax: 'ymax'], options: [:], css: 'charm-errorbar'],
        [geom: CharmGeomType.ERRORBARH, aes: [y: 'y', xmin: 'xmin', xmax: 'xmax'], options: [:], css: 'charm-errorbarh'],
        [geom: CharmGeomType.RIBBON, aes: [x: 'x', ymin: 'ymin', ymax: 'ymax', group: 'group'], options: [:], css: 'charm-ribbon'],
        [geom: CharmGeomType.SEGMENT, aes: [x: 'x', y: 'y', xend: 'xend', yend: 'yend'], options: [:], css: 'charm-segment'],
        [geom: CharmGeomType.HLINE, aes: [x: 'x', y: 'y'], options: [yintercept: 2.1], css: 'charm-segment'],
        [geom: CharmGeomType.VLINE, aes: [x: 'x', y: 'y'], options: [xintercept: 2.1], css: 'charm-segment'],
        [geom: CharmGeomType.ABLINE, aes: [x: 'x', y: 'y'], options: [intercept: 0.5, slope: 0.7], css: 'charm-segment'],
        [geom: CharmGeomType.LABEL, aes: [x: 'x', y: 'y', label: 'label'], options: [:], css: 'charm-label-box'],
        [geom: CharmGeomType.RUG, aes: [x: 'x', y: 'y'], options: [sides: 'bl'], css: 'charm-rug'],
        [geom: CharmGeomType.FREQPOLY, aes: [x: 'x', y: 'y', group: 'group'], options: [:], css: 'charm-line'],
        [geom: CharmGeomType.PATH, aes: [x: 'x', y: 'y', group: 'group'], options: [:], css: 'charm-path'],
        [geom: CharmGeomType.RECT, aes: [xmin: 'xmin', xmax: 'xmax', ymin: 'ymin', ymax: 'ymax'], options: [:], css: 'charm-rect'],
        [geom: CharmGeomType.POLYGON, aes: [x: 'x', y: 'y', group: 'polyGroup'], options: [:], css: 'charm-polygon'],
        [geom: CharmGeomType.CROSSBAR, aes: [x: 'x', y: 'y', ymin: 'ymin', ymax: 'ymax'], options: [:], css: 'charm-crossbar'],
        [geom: CharmGeomType.LINERANGE, aes: [x: 'x', ymin: 'ymin', ymax: 'ymax'], options: [:], css: 'charm-linerange'],
        [geom: CharmGeomType.POINTRANGE, aes: [x: 'x', y: 'y', ymin: 'ymin', ymax: 'ymax'], options: [:], css: 'charm-pointrange'],
        [geom: CharmGeomType.HEX, aes: [x: 'x', y: 'y'], options: [:], css: 'charm-hex'],
        [geom: CharmGeomType.CONTOUR, aes: [x: 'x', y: 'y', group: 'group'], options: [:], css: 'charm-contour']
    ]

    cases.each { Map<String, Object> tc ->
      Chart chart = plot(data) {
        aes(tc.aes as Map<String, String>)
        layer(tc.geom as CharmGeomType, tc.options as Map<String, Object>)
        theme {
          legend { position = 'none' }
        }
      }.build()

      String svg = SvgWriter.toXml(chart.render())
      String token = "class=\"${tc.css}\""
      assertTrue(svg.contains(token), "Expected ${tc.geom} to render ${token}")
    }
  }

  @Test
  void testCssIdsRemainUniqueForMultiPrimitiveGeoms() {
    Matrix data = Matrix.builder()
        .columnNames('x', 'y', 'xmin', 'xmax', 'ymin', 'ymax', 'label')
        .rows([[1.0, 2.0, 0.7, 1.3, 1.5, 2.5, 'p1']])
        .build()
    List<Map<String, Object>> cases = [
        [geom: CharmGeomType.ERRORBAR, aes: [x: 'x', ymin: 'ymin', ymax: 'ymax'], options: [:], idToken: '-errorbar-', expected: 3],
        [geom: CharmGeomType.ERRORBARH, aes: [y: 'y', xmin: 'xmin', xmax: 'xmax'], options: [:], idToken: '-errorbarh-', expected: 3],
        [geom: CharmGeomType.CROSSBAR, aes: [x: 'x', y: 'y', ymin: 'ymin', ymax: 'ymax'], options: [:], idToken: '-crossbar-', expected: 2],
        [geom: CharmGeomType.POINTRANGE, aes: [x: 'x', y: 'y', ymin: 'ymin', ymax: 'ymax'], options: [:], idToken: '-pointrange-', expected: 2],
        [geom: CharmGeomType.LABEL, aes: [x: 'x', y: 'y', label: 'label'], options: [:], idToken: '-label-', expected: 2],
        [geom: CharmGeomType.RUG, aes: [x: 'x', y: 'y'], options: [sides: 'bltr'], idToken: '-rug-', expected: 4]
    ]

    cases.each { Map<String, Object> tc ->
      Chart chart = plot(data) {
        aes(tc.aes as Map<String, String>)
        layer(tc.geom as CharmGeomType, tc.options as Map<String, Object>)
      }.build()

      String svg = SvgWriter.toXml(withCssIdsEnabled(chart).render())
      List<String> ids = idsForToken(svg, tc.idToken as String)
      assertEquals(tc.expected as int, ids.size(), "Unexpected element count for ${tc.geom}")
      assertEquals(ids.size(), ids.toSet().size(), "Expected unique IDs for ${tc.geom}")
    }
  }

  private static Chart withCssIdsEnabled(Chart chart) {
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
        new CssAttributesSpec(enabled: true, includeIds: true, includeClasses: true, includeDataAttributes: false)
    )
  }

  private static List<String> idsForToken(String xml, String token) {
    (xml =~ /id=['"]([^'"]+)['"]/)
        .collect { it[1] as String }
        .findAll { String id -> id.contains(token) }
  }

  private static Matrix p1Data() {
    Matrix.builder()
        .columnNames('x', 'y', 'xend', 'yend', 'xmin', 'xmax', 'ymin', 'ymax', 'label', 'group', 'polyGroup')
        .rows([
            [1.0, 1.5, 1.5, 2.0, 0.8, 1.2, 1.2, 1.8, 'a', 'g1', 'p1'],
            [2.0, 2.0, 2.5, 2.4, 1.8, 2.2, 1.6, 2.3, 'b', 'g1', 'p1'],
            [3.0, 1.7, 3.3, 1.3, 2.8, 3.2, 1.4, 2.0, 'c', 'g1', 'p1'],
            [1.2, 2.8, 1.7, 2.3, 1.0, 1.4, 2.4, 3.1, 'd', 'g2', 'p2'],
            [2.2, 3.1, 2.7, 2.9, 2.0, 2.4, 2.7, 3.4, 'e', 'g2', 'p2'],
            [3.2, 2.6, 3.7, 2.2, 3.0, 3.4, 2.2, 3.0, 'f', 'g2', 'p2']
        ])
        .build()
  }
}
