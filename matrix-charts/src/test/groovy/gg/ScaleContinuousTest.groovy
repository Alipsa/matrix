package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.scale.ScaleContinuous
import se.alipsa.matrix.gg.scale.ScaleXContinuous
import se.alipsa.matrix.gg.scale.ScaleYContinuous

import static org.junit.jupiter.api.Assertions.*

class ScaleContinuousTest {

  @Test
  void testTrainComputesDomain() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<Number>  // No expansion for cleaner tests
    scale.train([10, 20, 30, 40, 50])

    assertTrue(scale.isTrained())
  }

  @Test
  void testTrainFiltersNonNumeric() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<Number>
    scale.train([10, 'not a number', 20, null, 30])

    assertTrue(scale.isTrained())
    // Should still work, just ignoring non-numeric values
    Double transformed = scale.transform(20) as Double
    assertNotNull(transformed)
  }

  @Test
  void testTransformLinearInterpolation() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<Number>
    scale.range = [0, 100] as List<Number>
    scale.train([0, 100])

    assertEquals(0.0, scale.transform(0) as double, 0.01)
    assertEquals(50.0, scale.transform(50) as double, 0.01)
    assertEquals(100.0, scale.transform(100) as double, 0.01)
  }

  @Test
  void testTransformWithDifferentRange() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<Number>
    scale.range = [100, 500] as List<Number>
    scale.train([0, 10])

    // Domain: [0, 10] -> Range: [100, 500]
    assertEquals(100.0, scale.transform(0) as double, 0.01)
    assertEquals(300.0, scale.transform(5) as double, 0.01)  // Midpoint
    assertEquals(500.0, scale.transform(10) as double, 0.01)
  }

  @Test
  void testTransformWithNegativeValues() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<Number>
    scale.range = [0, 200] as List<Number>
    scale.train([-50, 50])

    assertEquals(0.0, scale.transform(-50) as double, 0.01)
    assertEquals(100.0, scale.transform(0) as double, 0.01)
    assertEquals(200.0, scale.transform(50) as double, 0.01)
  }

  @Test
  void testTransformReturnsNullForNull() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.train([0, 100])

    assertNull(scale.transform(null))
  }

  @Test
  void testTransformReturnsNullForNonNumeric() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.train([0, 100])

    assertNull(scale.transform('not a number'))
  }

  @Test
  void testInverseTransform() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<Number>
    scale.range = [0, 100] as List<Number>
    scale.train([0, 50])

    assertEquals(0.0, scale.inverse(0) as double, 0.01)
    assertEquals(25.0, scale.inverse(50) as double, 0.01)
    assertEquals(50.0, scale.inverse(100) as double, 0.01)
  }

  @Test
  void testInverseRoundTrip() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<Number>
    scale.range = [0, 400] as List<Number>
    scale.train([10, 90])

    double original = 45.0
    double transformed = scale.transform(original) as double
    double recovered = scale.inverse(transformed) as double

    assertEquals(original, recovered, 0.01)
  }

  @Test
  void testExplicitLimitsOverrideData() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<Number>
    scale.limits = [0, 100]
    scale.range = [0, 200] as List<Number>
    scale.train([20, 30, 40])  // Data within narrower range

    // Limits should override, so domain is [0, 100]
    assertEquals(0.0, scale.transform(0) as double, 0.01)
    assertEquals(100.0, scale.transform(50) as double, 0.01)
    assertEquals(200.0, scale.transform(100) as double, 0.01)
  }

  @Test
  void testExpansionExpandsDomain() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0.1, 0] as List<Number>  // 10% expansion on each side
    scale.range = [0, 100] as List<Number>
    scale.train([0, 100])

    // With 10% expansion: domain becomes [-10, 110] (roughly)
    // So transform(0) should be > 0 and transform(100) should be < 100
    double at0 = scale.transform(0) as double
    double at100 = scale.transform(100) as double

    assertTrue(at0 > 0, "Value at 0 should be offset due to expansion")
    assertTrue(at100 < 100, "Value at 100 should be offset due to expansion")
  }

  @Test
  void testGetComputedBreaksGeneratesNiceNumbers() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<Number>
    scale.train([0, 100])

    List breaks = scale.getComputedBreaks()

    assertFalse(breaks.isEmpty())
    // Should contain nice round numbers
    assertTrue(breaks.every { it instanceof Number })
  }

  @Test
  void testGetComputedBreaksWithExplicitBreaks() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.breaks = [0, 25, 50, 75, 100]
    scale.train([0, 100])

    assertEquals([0, 25, 50, 75, 100], scale.getComputedBreaks())
  }

  @Test
  void testGetComputedLabels() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<Number>
    scale.train([0, 100])

    List<String> labels = scale.getComputedLabels()

    assertFalse(labels.isEmpty())
    assertTrue(labels.every { it instanceof String })
  }

  @Test
  void testGetComputedLabelsWithExplicitLabels() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.labels = ['Low', 'Mid', 'High']
    scale.train([0, 100])

    assertEquals(['Low', 'Mid', 'High'], scale.getComputedLabels())
  }

  @Test
  void testMapBatchTransform() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<Number>
    scale.range = [0, 100] as List<Number>
    scale.train([0, 100])

    List result = scale.map([0, 25, 50, 75, 100])

    assertEquals(5, result.size())
    assertEquals(0.0, result[0] as double, 0.01)
    assertEquals(25.0, result[1] as double, 0.01)
    assertEquals(50.0, result[2] as double, 0.01)
    assertEquals(75.0, result[3] as double, 0.01)
    assertEquals(100.0, result[4] as double, 0.01)
  }

  @Test
  void testHandlesEqualMinMax() {
    ScaleContinuous scale = new ScaleContinuous()
    scale.expand = [0, 0] as List<Number>
    scale.range = [0, 100] as List<Number>
    scale.train([50, 50, 50])  // All same value

    // Should return midpoint of range
    assertEquals(50.0, scale.transform(50) as double, 0.01)
  }

  // --- ScaleXContinuous Tests ---

  @Test
  void testScaleXContinuousHasCorrectAesthetic() {
    ScaleXContinuous scale = new ScaleXContinuous()
    assertEquals('x', scale.aesthetic)
  }

  @Test
  void testScaleXContinuousWithParams() {
    ScaleXContinuous scale = new ScaleXContinuous(
        name: 'X Axis',
        limits: [0, 100],
        nBreaks: 10,
        position: 'top'
    )

    assertEquals('X Axis', scale.name)
    assertEquals([0, 100], scale.limits)
    assertEquals(10, scale.nBreaks)
    assertEquals('top', scale.position)
  }

  @Test
  void testScaleXContinuousFluentApi() {
    ScaleXContinuous scale = new ScaleXContinuous()
        .limits(0, 100)
        .breaks([0, 50, 100])
        .labels(['Start', 'Middle', 'End'])
        .expand(0.05, 0)

    assertEquals([0, 100], scale.limits)
    assertEquals([0, 50, 100], scale.breaks)
    assertEquals(['Start', 'Middle', 'End'], scale.labels)
    assertEquals([0.05, 0], scale.expand)
  }

  // --- ScaleYContinuous Tests ---

  @Test
  void testScaleYContinuousHasCorrectAesthetic() {
    ScaleYContinuous scale = new ScaleYContinuous()
    assertEquals('y', scale.aesthetic)
  }

  @Test
  void testScaleYContinuousWithInvertedRange() {
    // SVG has y=0 at top, so typical range is [height, 0]
    ScaleYContinuous scale = new ScaleYContinuous()
    scale.expand = [0, 0] as List<Number>
    scale.range = [400, 0] as List<Number>  // Inverted
    scale.train([0, 100])

    // Higher data values should map to lower y pixel values
    double at0 = scale.transform(0) as double
    double at100 = scale.transform(100) as double

    assertEquals(400.0, at0, 0.01)  // Data 0 at bottom (y=400)
    assertEquals(0.0, at100, 0.01)  // Data 100 at top (y=0)
  }

  @Test
  void testScaleYContinuousFluentApi() {
    ScaleYContinuous scale = new ScaleYContinuous()
        .limits(0, 50)
        .expand(0.1, 5)

    assertEquals([0, 50], scale.limits)
    assertEquals([0.1, 5], scale.expand)
  }
}
