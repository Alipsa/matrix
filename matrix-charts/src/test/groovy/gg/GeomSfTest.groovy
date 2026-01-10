package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.sf.SfType
import se.alipsa.matrix.gg.sf.WktReader

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertFalse
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for simple feature (sf) rendering and WKT parsing.
 */
class GeomSfTest {

  @Test
  void testWktReaderPoint() {
    def geom = WktReader.parse('POINT (1 2)')
    assertEquals(SfType.POINT, geom.type)
    assertFalse(geom.empty)
    assertEquals(1, geom.shapes.size())
    assertEquals(1, geom.shapes[0].rings.size())
    assertEquals(1, geom.shapes[0].rings[0].points.size())
  }

  @Test
  void testWktReaderPolygon() {
    def geom = WktReader.parse('POLYGON ((0 0, 1 0, 1 1, 0 1, 0 0))')
    assertEquals(SfType.POLYGON, geom.type)
    assertFalse(geom.empty)
    assertEquals(1, geom.shapes.size())
    assertEquals(1, geom.shapes[0].rings.size())
  }

  @Test
  void testWktReaderGeometryCollection() {
    def geom = WktReader.parse('GEOMETRYCOLLECTION (POINT (0 0), LINESTRING (0 0, 1 1))')
    assertEquals(SfType.GEOMETRYCOLLECTION, geom.type)
    assertFalse(geom.empty)
    assertEquals(2, geom.shapes.size())
    assertEquals(SfType.POINT, geom.shapes[0].type)
    assertEquals(SfType.LINESTRING, geom.shapes[1].type)
  }

  @Test
  void testGeomSfRendersMixedTypes() {
    def data = Matrix.builder()
        .data([
            geometry: [
                'POINT (0 0)',
                'LINESTRING (0 0, 1 1)',
                'POLYGON ((0 0, 1 0, 1 1, 0 1, 0 0))'
            ]
        ])
        .build()

    def chart = ggplot(data, aes(geometry: 'geometry')) +
        geom_sf()

    Svg svg = chart.render()
    assertNotNull(svg, 'geom_sf should render mixed geometry types')
  }

  @Test
  void testGeomSfTextRendersLabels() {
    def data = Matrix.builder()
        .data([
            geometry: ['POINT (0 0)', 'POINT (1 1)'],
            label: ['A', 'B']
        ])
        .build()

    def chart = ggplot(data, aes(geometry: 'geometry', label: 'label')) +
        geom_sf_text()

    Svg svg = chart.render()
    assertNotNull(svg, 'geom_sf_text should render labels')
  }

  @Test
  void testGeomMapRendersPolygons() {
    def mapData = Matrix.builder()
        .data([
            long: [0, 1, 1, 0, 0],
            lat: [0, 0, 1, 1, 0],
            group: [1, 1, 1, 1, 1],
            region: ['A', 'A', 'A', 'A', 'A']
        ])
        .build()

    def data = Matrix.builder()
        .data([
            region: ['A'],
            value: [10]
        ])
        .build()

    def chart = ggplot(data, aes(map_id: 'region', fill: 'value')) +
        geom_map(map: mapData)

    Svg svg = chart.render()
    assertNotNull(svg, 'geom_map should render polygon data')
  }
}
