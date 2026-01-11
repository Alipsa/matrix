package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleXBinned

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Test suite for ScaleXBinned - binned position scale for x-axis.
 */
class ScaleXBinnedTest {

  @Test
  void testDefaultBins() {
    def scale = new ScaleXBinned()
    assertEquals(10, scale.bins)
  }

  @Test
  void testCustomBins() {
    def scale = new ScaleXBinned(bins: 5)
    assertEquals(5, scale.bins)
  }

  @Test
  void testDefaultRight() {
    def scale = new ScaleXBinned()
    assertTrue(scale.right)
  }

  @Test
  void testCustomRight() {
    def scale = new ScaleXBinned(right: false)
    assertFalse(scale.right)
  }

  @Test
  void testDefaultShowLimits() {
    def scale = new ScaleXBinned()
    assertFalse(scale.showLimits)
  }

  @Test
  void testCustomShowLimits() {
    def scale = new ScaleXBinned(showLimits: true)
    assertTrue(scale.showLimits)
  }

  @Test
  void testBinBoundariesComputation() {
    def scale = new ScaleXBinned(bins: 4)
    scale.train([0, 100])

    // Should create boundaries: [0, 25, 50, 75, 100] (with expansion)
    // Actually with default expansion of 0.05, domain becomes [-5, 105]
    // Then bins are: [-5, 22.5, 50, 77.5, 105]
    assertEquals(5, scale.binBoundaries.size())
    assertTrue(scale.binBoundaries[0] < 0)  // Expanded below 0
    assertTrue(scale.binBoundaries[4] > 100)  // Expanded above 100
  }

  @Test
  void testBinBoundariesNoExpansion() {
    def scale = new ScaleXBinned(bins: 4, expand: [0, 0])
    scale.train([0, 100])

    // Without expansion: [0, 25, 50, 75, 100]
    assertEquals(5, scale.binBoundaries.size())
    assertEquals(0, scale.binBoundaries[0] as double, 0.01)
    assertEquals(25, scale.binBoundaries[1] as double, 0.01)
    assertEquals(50, scale.binBoundaries[2] as double, 0.01)
    assertEquals(75, scale.binBoundaries[3] as double, 0.01)
    assertEquals(100, scale.binBoundaries[4] as double, 0.01)
  }

  @Test
  void testBinCentersComputation() {
    def scale = new ScaleXBinned(bins: 4, expand: [0, 0])
    scale.train([0, 100])

    // Should create centers: [12.5, 37.5, 62.5, 87.5]
    assertEquals(4, scale.binCenters.size())
    assertEquals(12.5, scale.binCenters[0] as double, 0.01)
    assertEquals(37.5, scale.binCenters[1] as double, 0.01)
    assertEquals(62.5, scale.binCenters[2] as double, 0.01)
    assertEquals(87.5, scale.binCenters[3] as double, 0.01)
  }

  @Test
  void testTransformMapsToBinCenter() {
    def scale = new ScaleXBinned(bins: 4, expand: [0, 0])
    scale.train([0, 100])
    scale.setRange([0, 400])  // 0-100 data maps to 0-400 pixels

    // Value 10 is in bin [0, 25], center at 12.5
    // 12.5 in [0, 100] maps to 50 in [0, 400]
    def pos1 = scale.transform(10) as double
    def pos2 = scale.transform(20) as double
    assertEquals(pos1, pos2, 0.01)  // Same bin, same position
    assertEquals(50, pos1, 1.0)  // Bin center at 12.5 → pixel 50
  }

  @Test
  void testTransformDifferentBins() {
    def scale = new ScaleXBinned(bins: 4, expand: [0, 0])
    scale.train([0, 100])
    scale.setRange([0, 400])

    // Value 10 in bin 0 (center 12.5 → pixel 50)
    def pos1 = scale.transform(10) as double

    // Value 30 in bin 1 (center 37.5 → pixel 150)
    def pos2 = scale.transform(30) as double

    assertNotEquals(pos1, pos2)
    assertEquals(50, pos1, 1.0)
    assertEquals(150, pos2, 1.0)
  }

  @Test
  void testRightParameterClosedRight() {
    def scale = new ScaleXBinned(bins: 2, right: true, expand: [0, 0])
    scale.train([0, 10])
    scale.setRange([0, 100])

    // Bins: (0, 5], (5, 10]
    // Value 5 should be in first bin (closed on right)
    def pos5 = scale.transform(5) as double
    def pos51 = scale.transform(5.1) as double

    // 5 in bin 0 (center 2.5 → pixel 25)
    // 5.1 in bin 1 (center 7.5 → pixel 75)
    assertEquals(25, pos5, 1.0)
    assertEquals(75, pos51, 1.0)
  }

  @Test
  void testRightParameterClosedLeft() {
    def scale = new ScaleXBinned(bins: 2, right: false, expand: [0, 0])
    scale.train([0, 10])
    scale.setRange([0, 100])

    // Bins: [0, 5), [5, 10)
    // Value 5 should be in second bin (closed on left)
    def pos49 = scale.transform(4.9) as double
    def pos5 = scale.transform(5) as double

    // 4.9 in bin 0 (center 2.5 → pixel 25)
    // 5 in bin 1 (center 7.5 → pixel 75)
    assertEquals(25, pos49, 1.0)
    assertEquals(75, pos5, 1.0)
  }

  @Test
  void testComputedBreaks() {
    def scale = new ScaleXBinned(bins: 4, expand: [0, 0])
    scale.train([0, 100])

    // Default: don't show limits
    List breaks = scale.getComputedBreaks()
    assertEquals(3, breaks.size())  // Interior boundaries only
    assertEquals(25, breaks[0] as double, 0.01)
    assertEquals(50, breaks[1] as double, 0.01)
    assertEquals(75, breaks[2] as double, 0.01)
  }

  @Test
  void testComputedBreaksWithShowLimits() {
    def scale = new ScaleXBinned(bins: 4, showLimits: true, expand: [0, 0])
    scale.train([0, 100])

    List breaks = scale.getComputedBreaks()
    assertEquals(5, breaks.size())  // All boundaries
    assertEquals(0, breaks[0] as double, 0.01)
    assertEquals(25, breaks[1] as double, 0.01)
    assertEquals(50, breaks[2] as double, 0.01)
    assertEquals(75, breaks[3] as double, 0.01)
    assertEquals(100, breaks[4] as double, 0.01)
  }

  @Test
  void testUserSpecifiedBreaks() {
    def scale = new ScaleXBinned(bins: 4, breaks: [10, 20, 30], expand: [0, 0])
    scale.train([0, 100])

    List breaks = scale.getComputedBreaks()
    assertEquals(3, breaks.size())
    assertEquals(10, breaks[0])
    assertEquals(20, breaks[1])
    assertEquals(30, breaks[2])
  }

  @Test
  void testConstantDomain() {
    def scale = new ScaleXBinned(bins: 5)
    scale.train([50, 50, 50])

    assertEquals(2, scale.binBoundaries.size())  // [50, 50]
    assertEquals(1, scale.binCenters.size())  // [50]

    scale.setRange([0, 100])
    def pos = scale.transform(50)
    assertNotNull(pos)
  }

  @Test
  void testOutOfBoundsValueBelow() {
    def scale = new ScaleXBinned(bins: 4, expand: [0, 0])
    scale.train([0, 100])
    scale.setRange([0, 400])

    assertNull(scale.transform(-10))
  }

  @Test
  void testOutOfBoundsValueAbove() {
    def scale = new ScaleXBinned(bins: 4, expand: [0, 0])
    scale.train([0, 100])
    scale.setRange([0, 400])

    assertNull(scale.transform(110))
  }

  @Test
  void testNullValue() {
    def scale = new ScaleXBinned()
    scale.train([0, 100])
    scale.setRange([0, 400])

    assertNull(scale.transform(null))
  }

  @Test
  void testSingleBin() {
    def scale = new ScaleXBinned(bins: 1, expand: [0, 0])
    scale.train([0, 100])
    scale.setRange([0, 400])

    // All values should map to the center of the single bin (50)
    def pos1 = scale.transform(10) as double
    def pos2 = scale.transform(50) as double
    def pos3 = scale.transform(90) as double

    // Center at 50 → pixel 200
    assertEquals(pos1, pos2, 0.01)
    assertEquals(pos2, pos3, 0.01)
    assertEquals(200, pos1, 1.0)
  }

  @Test
  void testFactoryMethod() {
    def scale = scale_x_binned(bins: 7)
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleXBinned)
    assertEquals('x', scale.aesthetic)
    assertEquals(7, scale.bins)
  }

  @Test
  void testAestheticIsX() {
    def scale = new ScaleXBinned()
    assertEquals('x', scale.aesthetic)
  }

  @Test
  void testDefaultPosition() {
    def scale = new ScaleXBinned()
    assertEquals('bottom', scale.position)
  }

  @Test
  void testCustomPosition() {
    def scale = new ScaleXBinned(position: 'top')
    assertEquals('top', scale.position)
  }

  // Integration tests

  @Test
  void testBinnedScaleWithGeomPoint() {
    def data = Matrix.builder()
      .columnNames(['value', 'count'])
      .rows([
        [5, 10],
        [15, 20],
        [25, 30],
        [35, 40]
      ])
      .build()

    def chart = ggplot(data, aes(x: 'value', y: 'count')) +
      geom_point() +
      scale_x_binned(bins: 4)

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'))
    assertTrue(svgXml.contains('<circle'))  // Points rendered
  }

  @Test
  void testBinnedScaleBreakRendering() {
    def data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([[0, 1], [100, 2]])
      .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
      geom_point() +
      scale_x_binned(bins: 4, showLimits: true, expand: [0, 0])

    Svg svg = chart.render()
    String svgXml = SvgWriter.toXml(svg)

    // Should render axis with bin boundaries as tick marks
    assertTrue(svgXml.contains('<text'))  // Axis labels
  }

  @Test
  void testBinnedScaleWithCoordTrans() {
    def data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([[1, 1], [10, 2], [100, 3]])
      .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
      geom_point() +
      scale_x_binned(bins: 5) +
      coord_trans(x: 'log10')

    Svg svg = chart.render()
    String svgXml = SvgWriter.toXml(svg)

    // Transformation should be applied before binning
    assertTrue(svgXml.contains('<circle'))
  }

  @Test
  void testWithLimits() {
    def scale = new ScaleXBinned(bins: 4, limits: [10, 90], expand: [0, 0])
    scale.train([0, 100])
    scale.setRange([0, 400])

    // Limits override data domain
    // Bins: [10, 30, 50, 70, 90]
    assertEquals(10, scale.binBoundaries[0] as double, 0.01)
    assertEquals(90, scale.binBoundaries[4] as double, 0.01)
  }

  @Test
  void testMinimumValueWithRightTrue() {
    def scale = new ScaleXBinned(bins: 2, right: true, expand: [0, 0])
    scale.train([0, 10])
    scale.setRange([0, 100])

    // Bins: (0, 5], (5, 10]
    // Value 0 should be in first bin (edge case handling)
    def pos = scale.transform(0) as double
    assertEquals(25, pos, 1.0)  // Bin 0 center at 2.5
  }

  @Test
  void testBinBoundariesWithOddBinCount() {
    def scale = new ScaleXBinned(bins: 5, expand: [0, 0])
    scale.train([0, 50])

    // Bins: [0, 10, 20, 30, 40, 50]
    assertEquals(6, scale.binBoundaries.size())
    assertEquals(5, scale.binCenters.size())

    // Centers: [5, 15, 25, 35, 45]
    assertEquals(5, scale.binCenters[0] as double, 0.01)
    assertEquals(15, scale.binCenters[1] as double, 0.01)
    assertEquals(25, scale.binCenters[2] as double, 0.01)
    assertEquals(35, scale.binCenters[3] as double, 0.01)
    assertEquals(45, scale.binCenters[4] as double, 0.01)
  }
}
