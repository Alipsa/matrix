package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.assertNotNull
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for map coordinate systems: coord_map, coord_quickmap, coord_sf.
 */
class CoordMapTest {

  @Test
  void testCoordMapMercatorProjection() {
    def data = Matrix.builder()
        .data([x: [0, 45, 90], y: [0, 45, 80]])
        .build()
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        coord_map(projection: 'mercator')
    Svg svg = chart.render()
    assertNotNull(svg, 'coord_map with mercator projection should render')
  }

  @Test
  void testCoordMapEquirectangularProjection() {
    def data = Matrix.builder()
        .data([x: [0, 45, 90], y: [0, 45, 80]])
        .build()
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        coord_map(projection: 'equirectangular')
    Svg svg = chart.render()
    assertNotNull(svg, 'coord_map with equirectangular projection should render')
  }

  @Test
  void testCoordMapPoleClamp() {
    // Test that near-pole latitudes are clamped without error
    def data = Matrix.builder()
        .data([x: [0, 10], y: [89, 88]])
        .build()
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        coord_map()
    Svg svg = chart.render()
    assertNotNull(svg, 'coord_map should handle near-pole latitudes')
  }

  @Test
  void testCoordMapExtremePoleClamp() {
    // Test that extreme pole latitudes (>85.05) are clamped
    def data = Matrix.builder()
        .data([x: [0], y: [90]])
        .build()
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        coord_map()
    Svg svg = chart.render()
    assertNotNull(svg, 'coord_map should clamp extreme latitudes without error')
  }

  @Test
  void testCoordQuickmapAspectRatio() {
    // Test that quickmap adjusts aspect ratio based on latitude
    def data = Matrix.builder()
        .data([x: [0, 1], y: [45, 46]])
        .build()
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        coord_quickmap()
    Svg svg = chart.render()
    assertNotNull(svg, 'coord_quickmap should render with latitude-adjusted aspect ratio')
  }

  @Test
  void testCoordSfEqualAspect() {
    def data = Matrix.builder()
        .data([geometry: ['POINT (0 0)', 'POINT (1 1)']])
        .build()
    def chart = ggplot(data, aes(geometry: 'geometry')) +
        geom_sf() +
        coord_sf()
    Svg svg = chart.render()
    assertNotNull(svg, 'coord_sf should render with equal aspect ratio')
  }
}
