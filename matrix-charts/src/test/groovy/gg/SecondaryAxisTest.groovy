package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.gg.scale.ScaleXContinuous
import se.alipsa.matrix.gg.scale.ScaleYContinuous
import se.alipsa.matrix.gg.scale.SecondaryAxis

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for secondary axes (sec_axis and dup_axis).
 */
class SecondaryAxisTest {

  @Test
  void testSecAxisCreation() {
    // Test creating a secondary axis with transformation
    SecondaryAxis secAxis = sec_axis({ it * 1.8 + 32 }, name: "Fahrenheit")

    assertNotNull(secAxis)
    assertEquals("Fahrenheit", secAxis.name)
    assertNotNull(secAxis.transform)
  }

  @Test
  void testSecAxisTransformation() {
    // Test that the transformation works correctly
    SecondaryAxis secAxis = sec_axis({ it * 2 }, name: "Doubled")

    assertEquals(20, secAxis.applyTransform(10))
    assertEquals(100, secAxis.applyTransform(50))
    assertEquals(0, secAxis.applyTransform(0))
  }

  @Test
  void testSecAxisTransformationCelsiusToFahrenheit() {
    // Real-world example: Celsius to Fahrenheit
    SecondaryAxis secAxis = sec_axis({ it * 1.8 + 32 }, name: "Fahrenheit")

    assertEquals(32.0, secAxis.applyTransform(0), 0.01)  // 0°C = 32°F
    assertEquals(212.0, secAxis.applyTransform(100), 0.01)  // 100°C = 212°F
    assertEquals(98.6, secAxis.applyTransform(37), 0.01)  // 37°C ≈ 98.6°F
  }

  @Test
  void testSecAxisTransformationHandlesNull() {
    // Test that null values are handled gracefully
    SecondaryAxis secAxis = sec_axis({ it * 2 }, name: "Doubled")

    assertNull(secAxis.applyTransform(null))
  }

  @Test
  void testDupAxisCreation() {
    // Test creating a duplicate axis
    SecondaryAxis dupAxis = dup_axis()

    assertNotNull(dupAxis)
    assertNotNull(dupAxis.transform)
  }

  @Test
  void testDupAxisIdentityTransformation() {
    // Test that dup_axis uses identity transformation
    SecondaryAxis dupAxis = dup_axis()

    assertEquals(10, dupAxis.applyTransform(10))
    assertEquals(50, dupAxis.applyTransform(50))
    assertEquals(-25, dupAxis.applyTransform(-25))
  }

  @Test
  void testDupAxisWithName() {
    // Test dup_axis with custom name
    SecondaryAxis dupAxis = dup_axis(name: "Repeated Axis")

    assertEquals("Repeated Axis", dupAxis.name)
    assertEquals(42, dupAxis.applyTransform(42))
  }

  @Test
  void testSecAxisWithExplicitBreaks() {
    // Test sec_axis with explicit breaks
    SecondaryAxis secAxis = sec_axis(
      { it * 1000 },
      name: "Kilometers",
      breaks: [0, 5, 10, 15, 20]
    )

    assertEquals("Kilometers", secAxis.name)
    assertNotNull(secAxis.breaks)
    assertEquals(5, secAxis.breaks.size())
  }

  @Test
  void testSecAxisWithLabels() {
    // Test sec_axis with custom labels
    SecondaryAxis secAxis = sec_axis(
      { it / 1000 },
      name: "Thousands",
      labels: ["0", "1k", "2k", "3k", "4k"]
    )

    assertNotNull(secAxis.labels)
    assertEquals(5, secAxis.labels.size())
    assertEquals("1k", secAxis.labels[1])
  }

  @Test
  void testScaleXContinuousWithSecAxis() {
    // Test that ScaleXContinuous accepts sec.axis parameter
    ScaleXContinuous scale = scale_x_continuous(
      'sec.axis': sec_axis({ it * 2 }, name: "Doubled")
    )

    assertNotNull(scale)
    assertNotNull(scale.secAxis)
    assertEquals("Doubled", scale.secAxis.name)
    assertEquals("top", scale.secAxis.position)  // Secondary x-axis should be on top
  }

  @Test
  void testScaleYContinuousWithSecAxis() {
    // Test that ScaleYContinuous accepts sec.axis parameter
    ScaleYContinuous scale = scale_y_continuous(
      'sec.axis': sec_axis({ it * 1.8 + 32 }, name: "Fahrenheit")
    )

    assertNotNull(scale)
    assertNotNull(scale.secAxis)
    assertEquals("Fahrenheit", scale.secAxis.name)
    assertEquals("right", scale.secAxis.position)  // Secondary y-axis should be on right
  }

  @Test
  void testScaleXContinuousWithDupAxis() {
    // Test that ScaleXContinuous works with dup_axis
    ScaleXContinuous scale = scale_x_continuous(
      'sec.axis': dup_axis(name: "Time (repeated)")
    )

    assertNotNull(scale.secAxis)
    assertEquals("Time (repeated)", scale.secAxis.name)
  }

  @Test
  void testSecondaryAxisPositions() {
    // Test that secondary axes have correct positions
    ScaleXContinuous xScale = scale_x_continuous(
      position: 'bottom',
      'sec.axis': dup_axis()
    )
    ScaleYContinuous yScale = scale_y_continuous(
      position: 'left',
      'sec.axis': dup_axis()
    )

    // Primary axis positions
    assertEquals('bottom', xScale.position)
    assertEquals('left', yScale.position)

    // Secondary axis positions (opposite of primary)
    assertEquals('top', xScale.secAxis.position)
    assertEquals('right', yScale.secAxis.position)
  }

  @Test
  void testChartWithSecondaryYAxis() {
    // Integration test: create a chart with secondary y-axis
    Matrix mtcars = Dataset.mtcars()

    def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) +
      geom_point() +
      scale_y_continuous('sec.axis': sec_axis({ it * 1.8 }, name: "MPG x 1.8"))

    assertNotNull(chart)
    Svg svg = chart.render()
    assertNotNull(svg)

    // Basic check that the SVG is generated
    String svgString = svg.toString()
    assertTrue(svgString.contains('svg'))
  }

  @Test
  void testChartWithDuplicateXAxis() {
    // Integration test: create a chart with duplicate x-axis
    Matrix mtcars = Dataset.mtcars()

    def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) +
      geom_point() +
      scale_x_continuous('sec.axis': dup_axis())

    assertNotNull(chart)
    Svg svg = chart.render()
    assertNotNull(svg)

    // Check that the SVG is generated (basic sanity check)
    String svgString = svg.toString()
    assertTrue(svgString.contains('svg'))
  }

  @Test
  void testSecondaryAxisBreaksAndLabels() {
    // Test that secondary axis computes breaks and labels correctly
    ScaleYContinuous scale = scale_y_continuous(
      'sec.axis': sec_axis({ it * 1000 }, name: "Meters to Kilometers")
    )

    // Train the scale with some data
    scale.train([0, 1, 2, 3, 4, 5])
    scale.range = [0, 100]

    // Update the secondary scale
    scale.updateSecondaryScale()

    assertNotNull(scale.secAxis)
    assertTrue(scale.secAxis.isTrained())

    // Get breaks and labels from secondary axis
    def breaks = scale.secAxis.getComputedBreaks()
    def labels = scale.secAxis.getComputedLabels()

    assertNotNull(breaks)
    assertNotNull(labels)
    assertTrue(breaks.size() > 0)
    assertEquals(breaks.size(), labels.size())
  }

  @Test
  void testAlternativeParameterNames() {
    // Test that sec.axis, sec_axis, and secAxis all work
    ScaleXContinuous scale1 = scale_x_continuous(
      'sec.axis': dup_axis()
    )
    ScaleXContinuous scale2 = scale_x_continuous(
      sec_axis: dup_axis()
    )
    ScaleXContinuous scale3 = scale_x_continuous(
      secAxis: dup_axis()
    )

    assertNotNull(scale1.secAxis)
    assertNotNull(scale2.secAxis)
    assertNotNull(scale3.secAxis)
  }

  @Test
  void testSecondaryAxisWithNegativeTransformation() {
    // Test transformation that can produce negative values
    SecondaryAxis secAxis = sec_axis({ -it }, name: "Negated")

    assertEquals(-10, secAxis.applyTransform(10))
    assertEquals(10, secAxis.applyTransform(-10))
    assertEquals(0, secAxis.applyTransform(0))
  }

  @Test
  void testSecondaryAxisWithComplexTransformation() {
    // Test more complex transformation (e.g., logarithmic scale conversion)
    SecondaryAxis secAxis = sec_axis({ it ** 2 }, name: "Squared")

    assertEquals(4, secAxis.applyTransform(2))
    assertEquals(9, secAxis.applyTransform(3))
    assertEquals(100, secAxis.applyTransform(10))
  }

  @Test
  void testIdiomaticSyntaxSimple() {
    // Test the idiomatic Groovy syntax: sec_axis("name") { closure }
    SecondaryAxis secAxis = sec_axis("Fahrenheit") { it * 1.8 + 32 }

    assertEquals("Fahrenheit", secAxis.name)
    assertEquals(32.0, secAxis.applyTransform(0), 0.01)
    assertEquals(212.0, secAxis.applyTransform(100), 0.01)
  }

  @Test
  void testIdiomaticSyntaxWithParams() {
    // Test the idiomatic syntax with additional parameters
    SecondaryAxis secAxis = sec_axis("Kilometers", breaks: [0, 5, 10]) { it / 1000 }

    assertEquals("Kilometers", secAxis.name)
    assertNotNull(secAxis.breaks)
    assertEquals(3, secAxis.breaks.size())
    assertEquals(0.01, secAxis.applyTransform(10), 0.001)
  }

  @Test
  void testDupAxisIdiomaticSyntax() {
    // Test dup_axis with simple string parameter
    SecondaryAxis dupAxis = dup_axis("Repeated")

    assertEquals("Repeated", dupAxis.name)
    assertEquals(42, dupAxis.applyTransform(42))
  }

  @Test
  void testChartWithIdiomaticSecAxis() {
    // Integration test using the idiomatic syntax
    Matrix mtcars = Dataset.mtcars()

    def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) +
      geom_point() +
      scale_y_continuous('sec.axis': sec_axis("MPG x 1.8") { it * 1.8 })

    assertNotNull(chart)
    Svg svg = chart.render()
    assertNotNull(svg)
    assertTrue(svg.toString().contains('svg'))
  }

  @Test
  void testChartWithIdiomaticDupAxis() {
    // Integration test using dup_axis with idiomatic syntax
    Matrix mtcars = Dataset.mtcars()

    def chart = ggplot(mtcars, aes(x: 'hp', y: 'mpg')) +
      geom_point() +
      scale_x_continuous('sec.axis': dup_axis("Horsepower (repeated)"))

    assertNotNull(chart)
    Svg svg = chart.render()
    assertNotNull(svg)
  }
}
