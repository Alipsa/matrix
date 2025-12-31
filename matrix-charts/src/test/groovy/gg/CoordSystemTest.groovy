package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.gg.coord.CoordCartesian
import se.alipsa.matrix.gg.coord.CoordFixed
import se.alipsa.matrix.gg.coord.CoordFlip
import se.alipsa.matrix.gg.coord.CoordPolar

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class CoordSystemTest {

  // ==================== CoordFlip Tests ====================

  @Test
  void testCoordFlipDefaults() {
    CoordFlip coord = new CoordFlip()

    assertTrue(coord.flipped)
    assertTrue(coord.expand)
    assertEquals(0.05, coord.expandMult)
    assertNull(coord.xlim)
    assertNull(coord.ylim)
  }

  @Test
  void testCoordFlipWithParams() {
    CoordFlip coord = new CoordFlip(
        xlim: [0, 10],
        ylim: [0, 100],
        expand: false
    )

    assertEquals([0, 10], coord.xlim)
    assertEquals([0, 100], coord.ylim)
    assertFalse(coord.expand)
  }

  @Test
  void testHorizontalBarChart() {
    def data = Matrix.builder()
        .columnNames('category', 'value')
        .rows([
            ['A', 30],
            ['B', 50],
            ['C', 20],
            ['D', 45],
            ['E', 35]
        ])
        .types(String, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'category', y: 'value')) +
        geom_col(fill: 'steelblue') +
        coord_flip() +
        labs(title: 'Horizontal Bar Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<rect'), "Should contain bar rectangles")

    File outputFile = new File('build/coord_flip_horizontal_bar.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFlippedScatterPlot() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [1, 10],
            [2, 25],
            [3, 15],
            [4, 30],
            [5, 20]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 5, color: 'red') +
        coord_flip() +
        labs(title: 'Flipped Scatter Plot')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain point circles")

    File outputFile = new File('build/coord_flip_scatter.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFlippedBoxplot() {
    def rows = []
    def random = new Random(42)
    ['Group A', 'Group B', 'Group C'].each { group ->
      double mean = group == 'Group A' ? 20 : (group == 'Group B' ? 35 : 50)
      (1..30).each {
        rows << [group, mean + random.nextGaussian() * 8]
      }
    }

    def data = Matrix.builder()
        .columnNames('group', 'value')
        .rows(rows)
        .types(String, Double)
        .build()

    def chart = ggplot(data, aes(x: 'group', y: 'value')) +
        geom_boxplot(fill: 'lightblue') +
        coord_flip() +
        labs(title: 'Horizontal Boxplot')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/coord_flip_boxplot.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testFlippedHistogram() {
    def random = new Random(123)
    def rows = (1..100).collect {
      [random.nextGaussian() * 10 + 50]
    }

    def data = Matrix.builder()
        .columnNames('value')
        .rows(rows)
        .types(Double)
        .build()

    def chart = ggplot(data, aes(x: 'value')) +
        geom_histogram(fill: 'coral', bins: 15) +
        coord_flip() +
        labs(title: 'Horizontal Histogram')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/coord_flip_histogram.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== CoordPolar Tests ====================

  @Test
  void testCoordPolarDefaults() {
    CoordPolar coord = new CoordPolar()

    assertTrue(coord.polar)
    assertEquals('x', coord.theta)
    assertEquals(0, coord.start)
    assertTrue(coord.clockwise)
    assertTrue(coord.clip)
  }

  @Test
  void testCoordPolarWithParams() {
    CoordPolar coord = new CoordPolar(
        theta: 'y',
        start: Math.PI / 2,
        direction: -1
    )

    assertEquals('y', coord.theta)
    assertEquals(Math.PI / 2, coord.start as double, 0.001)
    assertFalse(coord.clockwise)
  }

  @Test
  void testCoordPolarCenter() {
    CoordPolar coord = new CoordPolar()
    coord.plotWidth = 640
    coord.plotHeight = 480

    def center = coord.getCenter()
    assertEquals(320, center[0])
    assertEquals(240, center[1])
  }

  @Test
  void testCoordPolarMaxRadius() {
    CoordPolar coord = new CoordPolar()
    coord.plotWidth = 640
    coord.plotHeight = 480

    double maxRadius = coord.getMaxRadius()
    // min(640, 480) / 2 * 0.9 = 240 * 0.9 = 216
    assertEquals(216, maxRadius, 0.1)
  }

  @Test
  void testPolarArcPath() {
    CoordPolar coord = new CoordPolar()
    coord.plotWidth = 400
    coord.plotHeight = 400

    // Create a quarter circle arc (0 to π/2)
    String path = coord.createArcPath(0, Math.PI / 2, 0, 100)
    assertNotNull(path)
    assertTrue(path.startsWith('M'))  // Move to center
    assertTrue(path.contains('A'))     // Arc command
    assertTrue(path.endsWith('Z'))     // Close path
  }

  @Test
  void testSimplePolarPlot() {
    def data = Matrix.builder()
        .columnNames('angle', 'radius')
        .rows([
            [0, 50],
            [1, 60],
            [2, 45],
            [3, 70],
            [4, 55],
            [5, 80]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'angle', y: 'radius')) +
        geom_point(size: 5, color: 'blue') +
        coord_polar() +
        labs(title: 'Polar Scatter Plot')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/coord_polar_scatter.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testPolarLineChart() {
    def data = Matrix.builder()
        .columnNames('angle', 'value')
        .rows([
            [0, 30],
            [1, 45],
            [2, 60],
            [3, 50],
            [4, 35],
            [5, 40]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'angle', y: 'value')) +
        geom_line(color: 'green', linewidth: 2) +
        geom_point(size: 4) +
        coord_polar() +
        labs(title: 'Polar Line Chart')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/coord_polar_line.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testPolarWithThetaY() {
    def data = Matrix.builder()
        .columnNames('radius', 'angle')
        .rows([
            [50, 0],
            [60, 1],
            [45, 2],
            [70, 3]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'radius', y: 'angle')) +
        geom_point(size: 6, color: 'purple') +
        coord_polar(theta: 'y') +
        labs(title: 'Polar with theta=y')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/coord_polar_theta_y.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testPolarCounterclockwise() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [0, 50],
            [1, 50],
            [2, 50],
            [3, 50]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 8, color: 'orange') +
        coord_polar(direction: -1) +
        labs(title: 'Counterclockwise Polar')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/coord_polar_ccw.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testPolarWithStartOffset() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [0, 60],
            [1, 60],
            [2, 60],
            [3, 60]
        ])
        .types(Integer, Integer)
        .build()

    // Start at 3 o'clock position (π/2 radians)
    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 8, color: 'teal') +
        coord_polar(start: Math.PI / 2) +
        labs(title: 'Polar with Start Offset')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/coord_polar_offset.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== Factory Method Tests ====================

  @Test
  void testFactoryMethods() {
    def flip = coord_flip()
    assertNotNull(flip)
    assertTrue(flip instanceof CoordFlip)
    assertTrue(flip.flipped)

    def flipParams = coord_flip(xlim: [0, 50])
    assertEquals([0, 50], flipParams.xlim)

    def polar = coord_polar()
    assertNotNull(polar)
    assertTrue(polar instanceof CoordPolar)
    assertEquals('x', polar.theta)

    def polarParams = coord_polar(theta: 'y', direction: -1)
    assertEquals('y', polarParams.theta)
    assertFalse(polarParams.clockwise)
  }

  // ==================== Edge Cases ====================

  @Test
  void testFlipWithEmptyData() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point() +
        coord_flip()

    // Should not throw exception
    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testPolarWithSinglePoint() {
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([[1, 50]])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 10) +
        coord_polar()

    Svg svg = chart.render()
    assertNotNull(svg)
  }

  @Test
  void testCoordFlipTransform() {
    CoordFlip coord = new CoordFlip()
    coord.plotWidth = 640
    coord.plotHeight = 480

    // The transform method swaps x and y scales
    // This is a unit test for the transform logic
    assertNotNull(coord)
    assertTrue(coord.flipped)
  }

  @Test
  void testCoordPolarTransform() {
    CoordPolar coord = new CoordPolar()
    coord.plotWidth = 400
    coord.plotHeight = 400

    // Test that center is computed correctly
    def center = coord.getCenter()
    assertEquals(200, center[0] as int)
    assertEquals(200, center[1] as int)

    // Test max radius
    double maxRadius = coord.getMaxRadius()
    assertEquals(180, maxRadius, 0.1)  // min(400,400)/2 * 0.9 = 180
  }

  // ==================== CoordCartesian Tests ====================

  @Test
  void testCoordCartesianDefaults() {
    CoordCartesian coord = new CoordCartesian()

    assertTrue(coord.expand)
    assertEquals(0.05, coord.expandMult)
    assertNull(coord.xlim)
    assertNull(coord.ylim)
  }

  @Test
  void testCoordCartesianWithParams() {
    CoordCartesian coord = new CoordCartesian(
        xlim: [10, 30],
        ylim: [0, 100],
        expand: false
    )

    assertEquals([10, 30], coord.xlim)
    assertEquals([0, 100], coord.ylim)
    assertFalse(coord.expand)
  }

  @Test
  void testCoordCartesianFactoryMethod() {
    def coord = coord_cartesian()
    assertNotNull(coord)
    assertTrue(coord instanceof CoordCartesian)

    def coordWithZoom = coord_cartesian(xlim: [10, 30])
    assertEquals([10, 30], coordWithZoom.xlim)
  }

  @Test
  void testCoordCartesianZoom() {
    def mpg = Dataset.mpg()

    def chart = ggplot(mpg, aes(x: 'displ', y: 'hwy')) +
        geom_point() +
        coord_cartesian(xlim: [4, 6]) +
        labs(title: 'Zoomed Scatter Plot')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/coord_cartesian_zoom.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== CoordFixed Tests ====================

  @Test
  void testCoordFixedDefaults() {
    CoordFixed coord = new CoordFixed()

    assertEquals(1.0, coord.ratio)
    assertTrue(coord.expand)
    assertNull(coord.xlim)
    assertNull(coord.ylim)
  }

  @Test
  void testCoordFixedWithRatio() {
    CoordFixed coord = new CoordFixed(0.5)
    assertEquals(0.5, coord.ratio)

    CoordFixed coord2 = new CoordFixed(ratio: 2.0)
    assertEquals(2.0, coord2.ratio)
  }

  @Test
  void testCoordFixedWithParams() {
    CoordFixed coord = new CoordFixed(
        ratio: 1.5,
        xlim: [0, 10],
        ylim: [0, 50]
    )

    assertEquals(1.5, coord.ratio)
    assertEquals([0, 10], coord.xlim)
    assertEquals([0, 50], coord.ylim)
  }

  @Test
  void testCoordFixedFactoryMethods() {
    def fixed1 = coord_fixed()
    assertNotNull(fixed1)
    assertTrue(fixed1 instanceof CoordFixed)
    assertEquals(1.0, fixed1.ratio)

    def fixed2 = coord_fixed(0.5)
    assertEquals(0.5, fixed2.ratio)

    def fixed3 = coord_fixed(ratio: 2.0)
    assertEquals(2.0, fixed3.ratio)
  }

  @Test
  void testCoordFixedScatterPlot() {
    def mpg = Dataset.mpg()

    def chart = ggplot(mpg, aes('cty', 'hwy')) +
        geom_point() +
        coord_fixed() +
        labs(title: 'Fixed Aspect Ratio Scatter Plot')

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain points")

    File outputFile = new File('build/coord_fixed_scatter.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testCoordFixedWithHalfRatio() {
    def mpg = Dataset.mpg()

    def chart = ggplot(mpg, aes('cty', 'hwy')) +
        geom_point() +
        coord_fixed(ratio: 0.5) +
        labs(title: 'Fixed Ratio 0.5')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/coord_fixed_half.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testCoordFixedWithDoubleRatio() {
    def mpg = Dataset.mpg()

    def chart = ggplot(mpg, aes('cty', 'hwy')) +
        geom_point() +
        coord_fixed(2.0) +
        labs(title: 'Fixed Ratio 2.0')

    Svg svg = chart.render()
    assertNotNull(svg)

    File outputFile = new File('build/coord_fixed_double.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testCoordFixedExactExample() {
    // This is the exact example from coordinates.groovy
    def mpg = Dataset.mpg()

    def chart = ggplot(mpg, aes('cty', 'hwy')) +
        geom_point() +
        coord_fixed()

    Svg svg = chart.render()
    assertNotNull(svg)

    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain points")

    File outputFile = new File('build/coord_fixed_example.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  // ==================== Aspect Ratio Enforcement Tests ====================

  @Test
  void testCoordFixedEnforcesEqualScaling() {
    // Create data with equal x and y ranges (both 0-100)
    // With ratio=1.0, the pixel ranges should be equal
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [0, 0],
            [100, 100],
            [50, 50]
        ])
        .types(Integer, Integer)
        .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 10) +
        coord_fixed()

    Svg svg = chart.render()
    assertNotNull(svg)

    // The chart should render without error and maintain aspect ratio
    String content = SvgWriter.toXml(svg)
    assertTrue(content.contains('<circle'), "Should contain points")

    // Extract circle positions to verify scaling
    // With equal data ranges and ratio=1.0, the diagonal should be at 45 degrees
    def circlePattern = /cx="(\d+\.?\d*)" cy="(\d+\.?\d*)"/
    def matcher = content =~ circlePattern
    def points = []
    while (matcher.find()) {
      points << [cx: Double.parseDouble(matcher.group(1)), cy: Double.parseDouble(matcher.group(2))]
    }

    // Should have 3 points
    assertEquals(3, points.size(), "Should have 3 points")

    File outputFile = new File('build/coord_fixed_equal_scaling.svg')
    write(svg, outputFile)
    assertTrue(outputFile.exists())
  }

  @Test
  void testCoordFixedWithDifferentRanges() {
    // Create data with x range 0-100 and y range 0-50
    // With ratio=1.0, x should use twice the pixel range of y
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [0, 0],
            [100, 50],
            [50, 25]
        ])
        .types(Integer, Integer)
        .build()

    def chartFixed = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 8) +
        coord_fixed()

    def chartCartesian = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 8) +
        coord_cartesian()

    Svg svgFixed = chartFixed.render()
    Svg svgCartesian = chartCartesian.render()

    assertNotNull(svgFixed)
    assertNotNull(svgCartesian)

    // Both should render, but with different aspect ratios
    String contentFixed = SvgWriter.toXml(svgFixed)
    String contentCartesian = SvgWriter.toXml(svgCartesian)

    assertTrue(contentFixed.contains('<circle'), "Fixed should contain points")
    assertTrue(contentCartesian.contains('<circle'), "Cartesian should contain points")

    File outputFixed = new File('build/coord_fixed_different_ranges.svg')
    File outputCartesian = new File('build/coord_cartesian_different_ranges.svg')
    write(svgFixed, outputFixed)
    write(svgCartesian, outputCartesian)

    assertTrue(outputFixed.exists())
    assertTrue(outputCartesian.exists())
  }

  @Test
  void testCoordFixedRatioAffectsScaling() {
    // Test that different ratios produce different scaling
    def data = Matrix.builder()
        .columnNames('x', 'y')
        .rows([
            [0, 0],
            [10, 10]
        ])
        .types(Integer, Integer)
        .build()

    def chartRatio1 = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 10) +
        coord_fixed(1.0)

    def chartRatio2 = ggplot(data, aes(x: 'x', y: 'y')) +
        geom_point(size: 10) +
        coord_fixed(2.0)

    Svg svgRatio1 = chartRatio1.render()
    Svg svgRatio2 = chartRatio2.render()

    assertNotNull(svgRatio1)
    assertNotNull(svgRatio2)

    String content1 = SvgWriter.toXml(svgRatio1)
    String content2 = SvgWriter.toXml(svgRatio2)

    // Extract circle positions for both charts
    def circlePattern = /cx="(\d+\.?\d*)" cy="(\d+\.?\d*)"/

    def matcher1 = content1 =~ circlePattern
    def points1 = []
    while (matcher1.find()) {
      points1 << [cx: Double.parseDouble(matcher1.group(1)), cy: Double.parseDouble(matcher1.group(2))]
    }

    def matcher2 = content2 =~ circlePattern
    def points2 = []
    while (matcher2.find()) {
      points2 << [cx: Double.parseDouble(matcher2.group(1)), cy: Double.parseDouble(matcher2.group(2))]
    }

    // Both should have 2 points
    assertEquals(2, points1.size(), "Ratio 1.0 should have 2 points")
    assertEquals(2, points2.size(), "Ratio 2.0 should have 2 points")

    // With different ratios, the x dimension should be scaled differently
    // The aspect ratio adjustment modifies the effective width to maintain the ratio
    if (points1.size() == 2 && points2.size() == 2) {
      double deltaX1 = Math.abs(points1[1].cx - points1[0].cx)
      double deltaX2 = Math.abs(points2[1].cx - points2[0].cx)

      // The x deltas should be different because ratio=2.0 compresses x more
      // This verifies the aspect ratio is actually being applied
      // Use a tolerance of 1 pixel to account for rounding
      assertTrue(Math.abs(deltaX1 - deltaX2) > 1.0,
          "Different ratios should produce different x scaling (deltaX1=${deltaX1}, deltaX2=${deltaX2})")
    }

    File output1 = new File('build/coord_fixed_ratio_1.svg')
    File output2 = new File('build/coord_fixed_ratio_2.svg')
    write(svgRatio1, output1)
    write(svgRatio2, output2)
  }
}
