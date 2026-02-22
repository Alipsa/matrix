package gg.scale

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleXBinned
import se.alipsa.matrix.gg.scale.ScaleYBinned
import testutil.Slow

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Parameterised test suite for ScaleXBinned and ScaleYBinned.
 * Each test runs for both 'x' and 'y' aesthetics.
 * Replaces ScaleXBinnedTest and ScaleYBinnedTest.
 */
class ScaleBinnedTest {

  private def newScale(String aesthetic, Map params = [:]) {
    aesthetic == 'x' ? new ScaleXBinned(params) : new ScaleYBinned(params)
  }

  private def scaleFactory(String aesthetic, Map params = [:]) {
    aesthetic == 'x' ? scale_x_binned(params) : scale_y_binned(params)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testDefaultBins(String aesthetic) {
    def scale = newScale(aesthetic)
    assertEquals(10, scale.bins)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testCustomBins(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 5])
    assertEquals(5, scale.bins)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testBinsValidationRejectsZero(String aesthetic) {
    def exception = assertThrows(IllegalArgumentException) {
      newScale(aesthetic, [bins: 0])
    }
    assertTrue(exception.message.contains('bins must be at least 1'))
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testBinsValidationRejectsNegative(String aesthetic) {
    def exception = assertThrows(IllegalArgumentException) {
      newScale(aesthetic, [bins: -5])
    }
    assertTrue(exception.message.contains('bins must be at least 1'))
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testDefaultRight(String aesthetic) {
    def scale = newScale(aesthetic)
    assertTrue(scale.right)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testCustomRight(String aesthetic) {
    def scale = newScale(aesthetic, [right: false])
    assertFalse(scale.right)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testDefaultShowLimits(String aesthetic) {
    def scale = newScale(aesthetic)
    assertFalse(scale.showLimits)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testCustomShowLimits(String aesthetic) {
    def scale = newScale(aesthetic, [showLimits: true])
    assertTrue(scale.showLimits)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testBinBoundariesComputation(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 4])
    scale.train([0, 100])

    assertEquals(5, scale.binBoundaries.size())
    assertTrue(scale.binBoundaries[0] < 0)   // Expanded below 0
    assertTrue(scale.binBoundaries[4] > 100) // Expanded above 100
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testBinBoundariesNoExpansion(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 4, expand: [0, 0]])
    scale.train([0, 100])

    assertEquals(5, scale.binBoundaries.size())
    assertEquals(0, scale.binBoundaries[0] as double, 0.01)
    assertEquals(25, scale.binBoundaries[1] as double, 0.01)
    assertEquals(50, scale.binBoundaries[2] as double, 0.01)
    assertEquals(75, scale.binBoundaries[3] as double, 0.01)
    assertEquals(100, scale.binBoundaries[4] as double, 0.01)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testBinCentersComputation(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 4, expand: [0, 0]])
    scale.train([0, 100])

    assertEquals(4, scale.binCenters.size())
    assertEquals(12.5, scale.binCenters[0] as double, 0.01)
    assertEquals(37.5, scale.binCenters[1] as double, 0.01)
    assertEquals(62.5, scale.binCenters[2] as double, 0.01)
    assertEquals(87.5, scale.binCenters[3] as double, 0.01)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testTransformMapsToBinCenter(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 4, expand: [0, 0]])
    scale.train([0, 100])
    scale.setRange([0, 400])

    def pos1 = scale.transform(10) as double
    def pos2 = scale.transform(20) as double
    assertEquals(pos1, pos2, 0.01)  // Same bin, same position
    assertEquals(50, pos1, 1.0)     // Bin center at 12.5 → pixel 50
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testTransformDifferentBins(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 4, expand: [0, 0]])
    scale.train([0, 100])
    scale.setRange([0, 400])

    def pos1 = scale.transform(10) as double  // bin 0 (center 12.5 → pixel 50)
    def pos2 = scale.transform(30) as double  // bin 1 (center 37.5 → pixel 150)

    assertNotEquals(pos1, pos2)
    assertEquals(50, pos1, 1.0)
    assertEquals(150, pos2, 1.0)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testRightParameterClosedRight(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 2, right: true, expand: [0, 0]])
    scale.train([0, 10])
    scale.setRange([0, 100])

    // Bins: (0, 5], (5, 10]
    def pos5 = scale.transform(5) as double    // in bin 0 (center 2.5 → pixel 25)
    def pos51 = scale.transform(5.1) as double // in bin 1 (center 7.5 → pixel 75)

    assertEquals(25, pos5, 1.0)
    assertEquals(75, pos51, 1.0)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testRightParameterClosedLeft(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 2, right: false, expand: [0, 0]])
    scale.train([0, 10])
    scale.setRange([0, 100])

    // Bins: [0, 5), [5, 10)
    def pos49 = scale.transform(4.9) as double // in bin 0 (center 2.5 → pixel 25)
    def pos5 = scale.transform(5) as double    // in bin 1 (center 7.5 → pixel 75)

    assertEquals(25, pos49, 1.0)
    assertEquals(75, pos5, 1.0)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testComputedBreaks(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 4, expand: [0, 0]])
    scale.train([0, 100])

    List breaks = scale.getComputedBreaks()
    assertEquals(3, breaks.size())  // Interior boundaries only
    assertEquals(25, breaks[0] as double, 0.01)
    assertEquals(50, breaks[1] as double, 0.01)
    assertEquals(75, breaks[2] as double, 0.01)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testComputedBreaksWithShowLimits(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 4, showLimits: true, expand: [0, 0]])
    scale.train([0, 100])

    List breaks = scale.getComputedBreaks()
    assertEquals(5, breaks.size())  // All boundaries
    assertEquals(0, breaks[0] as double, 0.01)
    assertEquals(25, breaks[1] as double, 0.01)
    assertEquals(50, breaks[2] as double, 0.01)
    assertEquals(75, breaks[3] as double, 0.01)
    assertEquals(100, breaks[4] as double, 0.01)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testUserSpecifiedBreaks(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 4, breaks: [10, 20, 30], expand: [0, 0]])
    scale.train([0, 100])

    List breaks = scale.getComputedBreaks()
    assertEquals(3, breaks.size())
    assertEquals(10, breaks[0])
    assertEquals(20, breaks[1])
    assertEquals(30, breaks[2])
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testConstantDomain(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 5])
    scale.train([50, 50, 50])

    assertEquals(2, scale.binBoundaries.size())  // [50, 50]
    assertEquals(1, scale.binCenters.size())      // [50]

    scale.setRange([0, 100])
    def pos = scale.transform(50)
    assertNotNull(pos)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testOutOfBoundsValueBelow(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 4, expand: [0, 0]])
    scale.train([0, 100])
    scale.setRange([0, 400])

    assertNull(scale.transform(-10))
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testOutOfBoundsValueAbove(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 4, expand: [0, 0]])
    scale.train([0, 100])
    scale.setRange([0, 400])

    assertNull(scale.transform(110))
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testNullValue(String aesthetic) {
    def scale = newScale(aesthetic)
    scale.train([0, 100])
    scale.setRange([0, 400])

    assertNull(scale.transform(null))
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testSingleBin(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 1, expand: [0, 0]])
    scale.train([0, 100])
    scale.setRange([0, 400])

    def pos1 = scale.transform(10) as double
    def pos2 = scale.transform(50) as double
    def pos3 = scale.transform(90) as double

    assertEquals(pos1, pos2, 0.01)
    assertEquals(pos2, pos3, 0.01)
    assertEquals(200, pos1, 1.0)  // Center at 50 → pixel 200
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testFactoryMethod(String aesthetic) {
    def scale = scaleFactory(aesthetic, [bins: 7])
    assertNotNull(scale)
    assertTrue(aesthetic == 'x' ? scale instanceof ScaleXBinned : scale instanceof ScaleYBinned)
    assertEquals(aesthetic, scale.aesthetic)
    assertEquals(7, scale.bins)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testAesthetic(String aesthetic) {
    def scale = newScale(aesthetic)
    assertEquals(aesthetic, scale.aesthetic)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testDefaultPosition(String aesthetic) {
    def scale = newScale(aesthetic)
    assertEquals(aesthetic == 'x' ? 'bottom' : 'left', scale.position)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testCustomPosition(String aesthetic) {
    def altPosition = aesthetic == 'x' ? 'top' : 'right'
    def scale = newScale(aesthetic, [position: altPosition])
    assertEquals(altPosition, scale.position)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testWithLimits(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 4, limits: [10, 90], expand: [0, 0]])
    scale.train([0, 100])
    scale.setRange([0, 400])

    assertEquals(10, scale.binBoundaries[0] as double, 0.01)
    assertEquals(90, scale.binBoundaries[4] as double, 0.01)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testMinimumValueWithRightTrue(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 2, right: true, expand: [0, 0]])
    scale.train([0, 10])
    scale.setRange([0, 100])

    // Bins: (0, 5], (5, 10] — value 0 should be in first bin (edge case)
    def pos = scale.transform(0) as double
    assertEquals(25, pos, 1.0)  // Bin 0 center at 2.5
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testMaximumValueWithRightFalse(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 2, right: false, expand: [0, 0]])
    scale.train([0, 10])
    scale.setRange([0, 100])

    // Bins: [0, 5), [5, 10) — value 10 should be in last bin (edge case)
    def pos = scale.transform(10) as double
    assertEquals(75, pos, 1.0)  // Bin 1 center at 7.5
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testShowLimitsFalseIsRespected(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 4, showLimits: false, expand: [0, 0]])
    scale.train([0, 100])

    assertFalse(scale.showLimits)
    List breaks = scale.getComputedBreaks()
    assertEquals(3, breaks.size())  // Interior boundaries only, not limits
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testShowLimitsFalseNotOverriddenByDotNotation(String aesthetic) {
    def params = [
      bins: 4,
      showLimits: false,
      'show.limits': true,  // Should be ignored since showLimits is present
      expand: [0, 0]
    ]
    def scale = newScale(aesthetic, params)
    scale.train([0, 100])

    assertFalse(scale.showLimits)
  }

  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testBinBoundariesWithOddBinCount(String aesthetic) {
    def scale = newScale(aesthetic, [bins: 5, expand: [0, 0]])
    scale.train([0, 50])

    assertEquals(6, scale.binBoundaries.size())
    assertEquals(5, scale.binCenters.size())

    assertEquals(5, scale.binCenters[0] as double, 0.01)
    assertEquals(15, scale.binCenters[1] as double, 0.01)
    assertEquals(25, scale.binCenters[2] as double, 0.01)
    assertEquals(35, scale.binCenters[3] as double, 0.01)
    assertEquals(45, scale.binCenters[4] as double, 0.01)
  }

  // Integration tests

  @Slow
  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testBinnedScaleWithGeomPoint(String aesthetic) {
    def data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows([[5, 10], [15, 20], [25, 30], [35, 40]])
      .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
      geom_point() +
      (aesthetic == 'x' ? scale_x_binned(bins: 4) : scale_y_binned(bins: 4))

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'))
    assertTrue(svgXml.contains('<circle'))  // Points rendered
  }

  @Slow
  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testBinnedScaleBreakRendering(String aesthetic) {
    // X variant: x spans 0-100 for scale_x_binned; Y variant: y spans 0-100 for scale_y_binned
    def rows = aesthetic == 'x' ? [[0, 1], [100, 2]] : [[1, 0], [2, 100]]
    def data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows(rows)
      .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
      geom_point() +
      (aesthetic == 'x'
        ? scale_x_binned(bins: 4, showLimits: true, expand: [0, 0])
        : scale_y_binned(bins: 4, showLimits: true, expand: [0, 0]))

    Svg svg = chart.render()
    String svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<text'))  // Axis labels rendered
  }

  @Slow
  @ParameterizedTest
  @ValueSource(strings = ['x', 'y'])
  void testBinnedScaleWithCoordTrans(String aesthetic) {
    // X variant: x spans 1-100 for log10 coord_trans; Y variant: y spans 1-100
    def rows = aesthetic == 'x' ? [[1, 1], [10, 2], [100, 3]] : [[1, 1], [2, 10], [3, 100]]
    def data = Matrix.builder()
      .columnNames(['x', 'y'])
      .rows(rows)
      .build()

    def chart = ggplot(data, aes(x: 'x', y: 'y')) +
      geom_point() +
      (aesthetic == 'x' ? scale_x_binned(bins: 5) : scale_y_binned(bins: 5)) +
      (aesthetic == 'x' ? coord_trans(x: 'log10') : coord_trans(y: 'log10'))

    Svg svg = chart.render()
    String svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<circle'))  // Transformation applied before binning
  }
}
