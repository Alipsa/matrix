package gg.geom

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.geom.*

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for Phase 1.1 geoms: ribbon, tile, rect, path, step, pointrange, linerange, crossbar
 */
class GeomPhase1Test {

  // ============ GeomRibbon Tests ============

  @Test
  void testGeomRibbonDefaults() {
    GeomRibbon geom = new GeomRibbon()

    assertEquals('gray', geom.fill)
    assertNull(geom.color)
    assertEquals(0, geom.linewidth)
    assertEquals(0.5, geom.alpha)
    assertEquals(['x', 'ymin', 'ymax'], geom.requiredAes)
  }

  @Test
  void testGeomRibbonWithParams() {
    GeomRibbon geom = new GeomRibbon(
        fill: 'blue',
        color: 'black',
        linewidth: 2,
        alpha: 0.3
    )

    assertEquals('blue', geom.fill)
    assertEquals('black', geom.color)
    assertEquals(2, geom.linewidth)
    assertEquals(0.3, geom.alpha)
  }

  @Test
  void testRibbonChartRender() {
    def data = Matrix.builder()
        .columnNames('x', 'y', 'ymin', 'ymax')
        .rows([
            [1, 10, 8, 12],
            [2, 15, 12, 18],
            [3, 12, 10, 14],
            [4, 18, 15, 21],
            [5, 20, 17, 23]
        ])
        .types(Integer, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_ribbon(ymin: 'ymin', ymax: 'ymax', fill: 'lightblue', alpha: 0.5) +
        geom_line() +
        labs(title: 'Ribbon Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<path'), "Should contain path element for ribbon")

    File outputFile = new File('build/ribbon_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============ GeomTile Tests ============

  @Test
  void testGeomTileDefaults() {
    GeomTile geom = new GeomTile()

    assertEquals('gray', geom.fill)
    assertEquals('white', geom.color)
    assertEquals(0.5, geom.linewidth)
    assertEquals(1.0, geom.alpha)
    assertEquals(['x', 'y'], geom.requiredAes)
  }

  @Test
  void testGeomTileWithParams() {
    GeomTile geom = new GeomTile(
        fill: 'red',
        color: 'black',
        linewidth: 1,
        alpha: 0.8
    )

    assertEquals('red', geom.fill)
    assertEquals('black', geom.color)
    assertEquals(1, geom.linewidth)
    assertEquals(0.8, geom.alpha)
  }

  @Test
  void testTileHeatmapRender() {
    // Create a simple heatmap data
    def data = Matrix.builder()
        .columnNames('x', 'y', 'value')
        .rows([
            [1, 1, 10],
            [1, 2, 20],
            [1, 3, 15],
            [2, 1, 25],
            [2, 2, 30],
            [2, 3, 5],
            [3, 1, 8],
            [3, 2, 12],
            [3, 3, 22]
        ])
        .types(Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y', fill: 'value')) +
        geom_tile() +
        scale_fill_gradient(low: 'white', high: 'steelblue') +
        labs(title: 'Heatmap with Tiles')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<rect'), "Should contain rect elements for tiles")

    File outputFile = new File('build/tile_heatmap.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============ GeomRect Tests ============

  @Test
  void testGeomRectDefaults() {
    GeomRect geom = new GeomRect()

    assertEquals('gray', geom.fill)
    assertEquals('black', geom.color)
    assertEquals(0.5, geom.linewidth)
    assertEquals(1.0, geom.alpha)
    assertEquals(['xmin', 'xmax', 'ymin', 'ymax'], geom.requiredAes)
  }

  @Test
  void testGeomRectWithParams() {
    GeomRect geom = new GeomRect(
        fill: 'green',
        color: 'darkgreen',
        alpha: 0.6
    )

    assertEquals('green', geom.fill)
    assertEquals('darkgreen', geom.color)
    assertEquals(0.6, geom.alpha)
  }

  @Test
  void testRectChartRender() {
    def data = Matrix.builder()
        .columnNames('xmin', 'xmax', 'ymin', 'ymax', 'category')
        .rows([
            [0, 2, 0, 3, 'A'],
            [3, 5, 1, 4, 'B'],
            [1, 4, 5, 8, 'C']
        ])
        .types(Integer, Integer, Integer, Integer, String)
        .build()

    def chart = ggplot(data, aes(fill: 'category')) +
        geom_rect(xmin: 'xmin', xmax: 'xmax', ymin: 'ymin', ymax: 'ymax', alpha: 0.5) +
        labs(title: 'Rectangle Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<rect'), "Should contain rect elements")

    File outputFile = new File('build/rect_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============ GeomPath Tests ============

  @Test
  void testGeomPathDefaults() {
    GeomPath geom = new GeomPath()

    assertEquals('black', geom.color)
    assertEquals(1, geom.size)
    assertEquals('solid', geom.linetype)
    assertEquals(1.0, geom.alpha)
    assertEquals(['x', 'y'], geom.requiredAes)
  }

  @Test
  void testGeomPathWithParams() {
    GeomPath geom = new GeomPath(
        color: 'red',
        linewidth: 2,
        linetype: 'dashed',
        alpha: 0.7
    )

    assertEquals('red', geom.color)
    assertEquals(2, geom.size)
    assertEquals('dashed', geom.linetype)
    assertEquals(0.7, geom.alpha)
  }

  @Test
  void testPathChartPreservesDataOrder() {
    // Data NOT sorted by x - path should connect in data order
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 1],
            [3, 3],  // out of order
            [2, 2],
            [4, 4]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_path(color: 'blue', linewidth: 2) +
        geom_point(color: 'red', size: 5) +
        labs(title: 'Path Chart (Data Order)')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<path'), "Should contain path element")

    File outputFile = new File('build/path_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testPathVsLineDifference() {
    // Same data, but path and line should produce different results
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 1],
            [4, 4],  // out of x-order
            [2, 2],
            [3, 3]
        ])
        .types(Integer, Integer)
        .build()

    // Line sorts by x
    def lineChart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_line(color: 'blue') +
        labs(title: 'Line (sorted by x)')

    // Path preserves data order
    def pathChart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_path(color: 'red') +
        labs(title: 'Path (data order)')

    Svg lineSvg = lineChart.render()
    Svg pathSvg = pathChart.render()

    assertNotNull(lineSvg)
    assertNotNull(pathSvg)

    write(lineSvg, new File('build/line_vs_path_line.svg'))
    write(pathSvg, new File('build/line_vs_path_path.svg'))
  }

  // ============ GeomStep Tests ============

  @Test
  void testGeomStepDefaults() {
    GeomStep geom = new GeomStep()

    assertEquals('black', geom.color)
    assertEquals(1, geom.size)
    assertEquals('solid', geom.linetype)
    assertEquals('hv', geom.direction)
    assertEquals(['x', 'y'], geom.requiredAes)
  }

  @Test
  void testGeomStepWithParams() {
    GeomStep geom = new GeomStep(
        color: 'green',
        linewidth: 2,
        direction: 'vh'
    )

    assertEquals('green', geom.color)
    assertEquals(2, geom.size)
    assertEquals('vh', geom.direction)
  }

  @Test
  void testStepChartHV() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 20],
            [3, 15],
            [4, 25],
            [5, 18]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_step(color: 'steelblue', linewidth: 2) +
        geom_point(color: 'red', size: 4) +
        labs(title: 'Step Chart (HV direction)')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<path'), "Should contain path element for step")

    File outputFile = new File('build/step_chart_hv.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testStepChartVH() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 20],
            [3, 15],
            [4, 25],
            [5, 18]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_step(color: 'darkgreen', linewidth: 2, direction: 'vh') +
        geom_point(color: 'orange', size: 4) +
        labs(title: 'Step Chart (VH direction)')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/step_chart_vh.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testStepChartMid() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 20],
            [3, 15],
            [4, 25],
            [5, 18]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_step(color: 'purple', linewidth: 2, direction: 'mid') +
        geom_point(color: 'black', size: 4) +
        labs(title: 'Step Chart (mid direction)')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/step_chart_mid.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============ GeomPointrange Tests ============

  @Test
  void testGeomPointrangeDefaults() {
    GeomPointrange geom = new GeomPointrange()

    assertEquals('black', geom.color)
    assertEquals(4, geom.size)
    assertEquals(1, geom.linewidth)
    assertEquals(1.0, geom.alpha)
    assertEquals(['x', 'y', 'ymin', 'ymax'], geom.requiredAes)
  }

  @Test
  void testGeomPointrangeWithParams() {
    GeomPointrange geom = new GeomPointrange(
        color: 'blue',
        size: 6,
        linewidth: 2
    )

    assertEquals('blue', geom.color)
    assertEquals(6, geom.size)
    assertEquals(2, geom.linewidth)
  }

  @Test
  void testPointrangeChartRender() {
    def data = Matrix.builder()
        .columnNames('group', 'mean', 'lower', 'upper')
        .rows([
            ['A', 10, 8, 12],
            ['B', 15, 12, 18],
            ['C', 12, 9, 15],
            ['D', 20, 17, 23]
        ])
        .types(String, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'group', y: 'mean')) +
        geom_pointrange(ymin: 'lower', ymax: 'upper', color: 'steelblue', size: 5) +
        labs(title: 'Point Range Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain circle elements for points")
    assertTrue(content.contains('<line'), "Should contain line elements for ranges")

    File outputFile = new File('build/pointrange_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============ GeomLinerange Tests ============

  @Test
  void testGeomLinerangeDefaults() {
    GeomLinerange geom = new GeomLinerange()

    assertEquals('black', geom.color)
    assertEquals(1, geom.linewidth)
    assertEquals('solid', geom.linetype)
    assertEquals(1.0, geom.alpha)
    assertEquals(['x', 'ymin', 'ymax'], geom.requiredAes)
  }

  @Test
  void testGeomLinerangeWithParams() {
    GeomLinerange geom = new GeomLinerange(
        color: 'red',
        linewidth: 3,
        linetype: 'dashed'
    )

    assertEquals('red', geom.color)
    assertEquals(3, geom.linewidth)
    assertEquals('dashed', geom.linetype)
  }

  @Test
  void testLinerangeChartRender() {
    def data = Matrix.builder()
        .columnNames('group', 'lower', 'upper')
        .rows([
            ['A', 5, 15],
            ['B', 10, 20],
            ['C', 8, 18],
            ['D', 12, 25]
        ])
        .types(String, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'group')) +
        geom_linerange(ymin: 'lower', ymax: 'upper', color: 'darkblue', linewidth: 2) +
        labs(title: 'Line Range Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<line'), "Should contain line elements")

    File outputFile = new File('build/linerange_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============ GeomCrossbar Tests ============

  @Test
  void testGeomCrossbarDefaults() {
    GeomCrossbar geom = new GeomCrossbar()

    assertNull(geom.fill)
    assertEquals('black', geom.color)
    assertEquals(1, geom.linewidth)
    assertEquals(2.5, geom.fatten)
    assertEquals(['x', 'y', 'ymin', 'ymax'], geom.requiredAes)
  }

  @Test
  void testGeomCrossbarWithParams() {
    GeomCrossbar geom = new GeomCrossbar(
        fill: 'lightblue',
        color: 'blue',
        linewidth: 2,
        fatten: 3
    )

    assertEquals('lightblue', geom.fill)
    assertEquals('blue', geom.color)
    assertEquals(2, geom.linewidth)
    assertEquals(3, geom.fatten)
  }

  @Test
  void testCrossbarChartRender() {
    def data = Matrix.builder()
        .columnNames('group', 'mean', 'lower', 'upper')
        .rows([
            ['A', 10, 7, 13],
            ['B', 15, 12, 18],
            ['C', 12, 8, 16],
            ['D', 18, 14, 22]
        ])
        .types(String, Integer, Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'group', y: 'mean')) +
        geom_crossbar(ymin: 'lower', ymax: 'upper', fill: 'lightgray', color: 'black', width: 0.5) +
        labs(title: 'Crossbar Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<rect'), "Should contain rect elements for boxes")
    assertTrue(content.contains('<line'), "Should contain line elements for middle bars")

    File outputFile = new File('build/crossbar_chart.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ============ Factory Method Tests ============

  @Test
  void testFactoryMethods() {
    // Test all factory methods create valid objects
    assertNotNull(geom_ribbon())
    assertTrue(geom_ribbon() instanceof GeomRibbon)

    assertNotNull(geom_tile())
    assertTrue(geom_tile() instanceof GeomTile)

    assertNotNull(geom_rect())
    assertTrue(geom_rect() instanceof GeomRect)

    assertNotNull(geom_path())
    assertTrue(geom_path() instanceof GeomPath)

    assertNotNull(geom_step())
    assertTrue(geom_step() instanceof GeomStep)

    assertNotNull(geom_pointrange())
    assertTrue(geom_pointrange() instanceof GeomPointrange)

    assertNotNull(geom_linerange())
    assertTrue(geom_linerange() instanceof GeomLinerange)

    assertNotNull(geom_crossbar())
    assertTrue(geom_crossbar() instanceof GeomCrossbar)
  }

  @Test
  void testFactoryMethodsWithParams() {
    def ribbon = geom_ribbon(fill: 'blue', alpha: 0.3)
    assertEquals('blue', ribbon.fill)
    assertEquals(0.3, ribbon.alpha)

    def tile = geom_tile(fill: 'red')
    assertEquals('red', tile.fill)

    def rect = geom_rect(fill: 'green')
    assertEquals('green', rect.fill)

    def path = geom_path(color: 'purple', linewidth: 3)
    assertEquals('purple', path.color)
    assertEquals(3, path.size)

    def step = geom_step(direction: 'vh')
    assertEquals('vh', step.direction)

    def pointrange = geom_pointrange(size: 8)
    assertEquals(8, pointrange.size)

    def linerange = geom_linerange(linewidth: 4)
    assertEquals(4, linerange.linewidth)

    def crossbar = geom_crossbar(fatten: 5)
    assertEquals(5, crossbar.fatten)
  }

  // ============ Combined Chart Tests ============

  @Test
  void testCombinedErrorVisualization() {
    // Create a chart showing different error visualization approaches
    def data = Matrix.builder()
        .columnNames('group', 'mean', 'lower', 'upper')
        .rows([
            ['A', 10, 7, 13],
            ['B', 15, 11, 19],
            ['C', 12, 9, 15]
        ])
        .types(String, Integer, Integer, Integer)
        .build()

    // Chart with errorbar
    def chart1 = ggplot(data, aes(x: 'group', y: 'mean')) +
        geom_errorbar(ymin: 'lower', ymax: 'upper', width: 0.3) +
        geom_point(size: 4) +
        labs(title: 'Error Bars')

    // Chart with pointrange
    def chart2 = ggplot(data, aes(x: 'group', y: 'mean')) +
        geom_pointrange(ymin: 'lower', ymax: 'upper') +
        labs(title: 'Point Range')

    // Chart with linerange
    def chart3 = ggplot(data, aes(x: 'group')) +
        geom_linerange(ymin: 'lower', ymax: 'upper') +
        labs(title: 'Line Range')

    // Chart with crossbar
    def chart4 = ggplot(data, aes(x: 'group', y: 'mean')) +
        geom_crossbar(ymin: 'lower', ymax: 'upper', fill: 'lightblue') +
        labs(title: 'Crossbar')

    [chart1, chart2, chart3, chart4].each { chart ->
      Svg svg = chart.render()
      assertNotNull(svg)
    }

    write(chart1.render(), new File('build/error_viz_errorbar.svg'))
    write(chart2.render(), new File('build/error_viz_pointrange.svg'))
    write(chart3.render(), new File('build/error_viz_linerange.svg'))
    write(chart4.render(), new File('build/error_viz_crossbar.svg'))
  }
}
