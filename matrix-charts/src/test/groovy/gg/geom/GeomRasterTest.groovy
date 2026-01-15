package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for GeomRaster - fast rectangular tiles on regular grids.
 */
class GeomRasterTest {

  @Test
  void testBasicRaster() {
    // Simple 3x3 grid
    def x = [1, 2, 3, 1, 2, 3, 1, 2, 3]
    def y = [1, 1, 1, 2, 2, 2, 3, 3, 3]
    def values = [1, 2, 3, 4, 5, 6, 7, 8, 9]

    def data = Matrix.builder()
        .data([x: x, y: y, value: values])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y', fill: 'value')) +
        geom_raster()

    Svg svg = chart.render()
    assertNotNull(svg, 'Basic raster should render')
  }

  @Test
  void testRasterWithCustomColors() {
    def x = [0, 1, 0, 1]
    def y = [0, 0, 1, 1]
    def values = [1.0, 2.0, 3.0, 4.0]

    def data = Matrix.builder()
        .data([x: x, y: y, z: values])
        .types(Integer, Integer, BigDecimal)
        .build()

    def chart = ggplot(data, aes('x', 'y', fill: 'z')) +
        geom_raster() +
        scale_fill_gradient(low: 'white', high: 'blue')

    Svg svg = chart.render()
    assertNotNull(svg, 'Raster with gradient should render')
  }

  @Test
  void testRasterWithAlpha() {
    def x = [1, 2, 1, 2]
    def y = [1, 1, 2, 2]

    def data = Matrix.builder()
        .data([x: x, y: y])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_raster(fill: 'red', alpha: 0.5)

    Svg svg = chart.render()
    assertNotNull(svg, 'Raster with alpha should render')
  }

  @Test
  void testLargeRaster() {
    // 10x10 grid
    def x = []
    def y = []
    def values = []

    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 10; j++) {
        x << i
        y << j
        values << (i + j)
      }
    }

    def data = Matrix.builder()
        .data([x: x, y: y, value: values])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y', fill: 'value')) +
        geom_raster()

    Svg svg = chart.render()
    assertNotNull(svg, 'Large raster grid should render')
  }

  @Test
  void testRasterWithFixedDimensions() {
    def x = [0, 1, 2]
    def y = [0, 0, 0]

    def data = Matrix.builder()
        .data([x: x, y: y])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_raster(width: 1.0, height: 1.0, fill: 'blue')

    Svg svg = chart.render()
    assertNotNull(svg, 'Raster with fixed dimensions should render')
  }

  @Test
  void testImageLikeRaster() {
    // Simulating image data (grayscale)
    def width = 5
    def height = 5
    def x = []
    def y = []
    def intensity = []

    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        x.add(i)
        y.add(j)
        // Create a diagonal gradient
        int val = ((i + j) * 255.0 / (width + height - 2)) as int
        intensity.add(val)
      }
    }

    def data = Matrix.builder()
        .data([x: x, y: y, intensity: intensity])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y', fill: 'intensity')) +
        geom_raster() +
        scale_fill_gradient(low: 'black', high: 'white') +
        coord_fixed()

    Svg svg = chart.render()
    assertNotNull(svg, 'Image-like raster should render')
  }

  @Test
  void testEmptyData() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
        geom_raster()

    Svg svg = chart.render()
    assertNotNull(svg, 'Should render without error on empty data')
  }

  @Test
  void testRasterVsTileComparison() {
    // Same data should produce similar results
    def x = [1, 2, 1, 2]
    def y = [1, 1, 2, 2]
    def values = [1, 2, 3, 4]

    def data = Matrix.builder()
        .data([x: x, y: y, value: values])
        .types(Integer, Integer, Integer)
        .build()

    def rasterChart = ggplot(data, aes('x', 'y', fill: 'value')) +
        geom_raster()

    def tileChart = ggplot(data, aes('x', 'y', fill: 'value')) +
        geom_tile()

    Svg rasterSvg = rasterChart.render()
    Svg tileSvg = tileChart.render()

    assertNotNull(rasterSvg, 'Raster should render')
    assertNotNull(tileSvg, 'Tile should render')
    // Both should produce valid SVG
  }
}
