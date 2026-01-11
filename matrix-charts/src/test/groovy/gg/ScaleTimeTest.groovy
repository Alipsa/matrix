package gg

import org.junit.jupiter.api.Test
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.scale.ScaleXTime
import se.alipsa.matrix.gg.scale.ScaleYTime

import java.time.Duration
import java.time.LocalTime

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Test suite for time-of-day scales (ScaleXTime and ScaleYTime).
 */
class ScaleTimeTest {

  // --- Basic rendering tests ---

  @Test
  void testSimpleLineChartWithHourlyData() {
    def times = (0..23).collect { LocalTime.of(it, 0) }
    def values = (0..23).collect { Math.sin(it * Math.PI / 12) * 20 + 50 }

    def data = Matrix.builder()
      .columnNames(['time', 'value'])
      .rows([times, values].transpose())
      .build()

    def chart = ggplot(data, aes(x: 'time', y: 'value')) +
      geom_line() +
      scale_x_time()

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'))
    assertTrue(svgXml.contains('</svg>'))
  }

  @Test
  void testPointChartWithTimeAxis() {
    def data = Matrix.builder()
      .columnNames(['time', 'count'])
      .rows([
        [LocalTime.of(9, 0), 10],
        [LocalTime.of(12, 0), 15],
        [LocalTime.of(15, 0), 12],
        [LocalTime.of(18, 0), 20]
      ])
      .build()

    def chart = ggplot(data, aes(x: 'time', y: 'count')) +
      geom_point() +
      scale_x_time(time_breaks: '3 hours')

    Svg svg = chart.render()
    String svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<circle'))  // Points rendered
  }

  @Test
  void testYAxisTimeScale() {
    def data = Matrix.builder()
      .columnNames(['category', 'time'])
      .rows([
        ['A', LocalTime.of(8, 30)],
        ['B', LocalTime.of(12, 0)],
        ['C', LocalTime.of(17, 30)]
      ])
      .build()

    def chart = ggplot(data, aes(x: 'category', y: 'time')) +
      geom_point() +
      scale_y_time()

    Svg svg = chart.render()
    assertNotNull(svg)
    String svgXml = SvgWriter.toXml(svg)
    assertTrue(svgXml.contains('<svg'))
  }

  @Test
  void testFactoryMethodScaleXTime() {
    def scale = scale_x_time(time_breaks: '1 hour')
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleXTime)
    assertEquals('x', scale.aesthetic)
    assertEquals('1 hour', scale.timeBreaks)
  }

  @Test
  void testFactoryMethodScaleYTime() {
    def scale = scale_y_time(time_format: 'HH:mm:ss')
    assertNotNull(scale)
    assertTrue(scale instanceof ScaleYTime)
    assertEquals('y', scale.aesthetic)
    assertEquals('HH:mm:ss', scale.timeFormat)
  }

  // --- Data type conversion tests ---

  @Test
  void testLocalTimeValues() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(9, 0), LocalTime.of(12, 0), LocalTime.of(15, 0)]
    scale.train(times)
    scale.setRange([0, 100])

    assertEquals(9 * 3600, scale.minSecondsSinceMidnight)
    assertEquals(15 * 3600, scale.maxSecondsSinceMidnight)

    def pos = scale.transform(LocalTime.of(12, 0))
    assertNotNull(pos)
    assertEquals(50, pos as double, 1.0)  // Midpoint
  }

  @Test
  void testISOStringTimes() {
    def scale = new ScaleXTime()
    def times = ['09:00:00', '12:00:00', '15:00:00']
    scale.train(times)
    scale.setRange([0, 100])

    assertEquals(9 * 3600, scale.minSecondsSinceMidnight)
    assertEquals(15 * 3600, scale.maxSecondsSinceMidnight)

    def pos = scale.transform('12:00:00')
    assertNotNull(pos)
    assertEquals(50, pos as double, 1.0)
  }

  @Test
  void testISOStringTimesShortFormat() {
    def scale = new ScaleXTime()
    def times = ['09:00', '12:00', '15:00']
    scale.train(times)

    assertEquals(9 * 3600, scale.minSecondsSinceMidnight)
    assertEquals(15 * 3600, scale.maxSecondsSinceMidnight)
  }

  @Test
  void testNumericSeconds() {
    def scale = new ScaleXTime()
    def times = [0, 43200, 86399]  // Midnight, noon, end of day
    scale.train(times)
    scale.setRange([0, 100])

    assertEquals(0, scale.minSecondsSinceMidnight)
    assertEquals(86399, scale.maxSecondsSinceMidnight)

    def pos = scale.transform(43200)
    assertNotNull(pos)
    assertEquals(50, pos as double, 1.0)
  }

  @Test
  void testDurationObjects() {
    def scale = new ScaleXTime()
    def times = [Duration.ofHours(9), Duration.ofHours(12), Duration.ofHours(15)]
    scale.train(times)

    assertEquals(9 * 3600, scale.minSecondsSinceMidnight)
    assertEquals(15 * 3600, scale.maxSecondsSinceMidnight)
  }

  @Test
  void testMixedTypes() {
    def scale = new ScaleXTime()
    def times = [
      LocalTime.of(9, 0),
      '12:00:00',
      54000  // 15:00 in seconds
    ]
    scale.train(times)

    assertEquals(9 * 3600, scale.minSecondsSinceMidnight)
    assertEquals(15 * 3600, scale.maxSecondsSinceMidnight)
  }

  @Test
  void testNullAndInvalidValues() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(9, 0), null, 'invalid', LocalTime.of(15, 0)]
    scale.train(times)

    // Should ignore null and invalid
    assertEquals(9 * 3600, scale.minSecondsSinceMidnight)
    assertEquals(15 * 3600, scale.maxSecondsSinceMidnight)
  }

  @Test
  void testSecondsClamping() {
    def scale = new ScaleXTime()
    def times = [-1000, 50000, 100000]  // Out of range values
    scale.train(times)

    // Values should be clamped to [0, 86399]
    assertEquals(0, scale.minSecondsSinceMidnight)
    assertEquals(86399, scale.maxSecondsSinceMidnight)
  }

  // --- Break generation tests ---

  @Test
  void testAutoBreaksVeryShortSpan() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(12, 0, 0), LocalTime.of(12, 0, 50)]  // 50 seconds
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertFalse(breaks.isEmpty())
    assertTrue(breaks.every { it instanceof LocalTime })
    // Should have 10-second breaks
  }

  @Test
  void testAutoBreaksOneMinute() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(12, 0), LocalTime.of(12, 1)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertFalse(breaks.isEmpty())
  }

  @Test
  void testAutoBreaksFiveMinutes() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(12, 0), LocalTime.of(12, 5)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertFalse(breaks.isEmpty())
    // Should have 30-second or 1-minute breaks
  }

  @Test
  void testAutoBreaksOneHour() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(12, 0), LocalTime.of(13, 0)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertFalse(breaks.isEmpty())
    // Should have 10-minute breaks
  }

  @Test
  void testAutoBreaksTwoHours() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(10, 0), LocalTime.of(12, 0)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertFalse(breaks.isEmpty())
    // Should have 15-minute breaks
  }

  @Test
  void testAutoBreaksSixHours() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(9, 0), LocalTime.of(15, 0)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertFalse(breaks.isEmpty())
    // Should have 30-minute breaks
  }

  @Test
  void testAutoBreaksTwelveHours() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(6, 0), LocalTime.of(18, 0)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertFalse(breaks.isEmpty())
    // Should have 1-hour breaks
  }

  @Test
  void testAutoBreaksFullDay() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(0, 0), LocalTime.of(23, 59)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertFalse(breaks.isEmpty())
    // Should have 2-hour breaks
  }

  @Test
  void testCustomBreaksThirtySeconds() {
    def scale = new ScaleXTime(time_breaks: '30 seconds')
    def times = [LocalTime.of(12, 0, 0), LocalTime.of(12, 2, 0)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertFalse(breaks.isEmpty())
    assertTrue(breaks.every { it instanceof LocalTime })
  }

  @Test
  void testCustomBreaksFifteenMinutes() {
    def scale = new ScaleXTime(time_breaks: '15 minutes')
    def times = [LocalTime.of(9, 0), LocalTime.of(12, 0)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertFalse(breaks.isEmpty())
    // Should have breaks at 9:00, 9:15, 9:30, ..., 12:00
  }

  @Test
  void testCustomBreaksTwoHours() {
    def scale = new ScaleXTime(time_breaks: '2 hours')
    def times = [LocalTime.of(8, 0), LocalTime.of(18, 0)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertFalse(breaks.isEmpty())
    // Should have breaks at 8:00, 10:00, 12:00, 14:00, 16:00, 18:00
  }

  @Test
  void testCustomBreaksOneHour() {
    def scale = new ScaleXTime(time_breaks: '1 hour')
    def times = [LocalTime.of(9, 30), LocalTime.of(15, 30)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertTrue(breaks.size() >= 5)  // At least 10:00, 11:00, 12:00, 13:00, 14:00, 15:00
  }

  @Test
  void testUserSpecifiedBreaks() {
    def scale = new ScaleXTime(breaks: [LocalTime.of(10, 0), LocalTime.of(14, 0)])
    def times = [LocalTime.of(9, 0), LocalTime.of(17, 0)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertEquals(2, breaks.size())
    assertEquals(LocalTime.of(10, 0), breaks[0])
    assertEquals(LocalTime.of(14, 0), breaks[1])
  }

  @Test
  void testInvalidTimeBreaksSpecification() {
    // Invalid unit should throw IllegalArgumentException
    def exception = assertThrows(IllegalArgumentException) {
      def scale = new ScaleXTime(time_breaks: '5 days')
      def times = [LocalTime.of(9, 0), LocalTime.of(17, 0)]
      scale.train(times)
      scale.getComputedBreaks()  // Triggers break generation
    }
    assertTrue(exception.message.contains('Invalid time unit'))
    assertTrue(exception.message.contains('days'))
  }

  @Test
  void testInvalidTimeBreaksUnit() {
    // Another invalid unit
    def exception = assertThrows(IllegalArgumentException) {
      def scale = new ScaleXTime(time_breaks: '30 milliseconds')
      def times = [LocalTime.of(9, 0), LocalTime.of(17, 0)]
      scale.train(times)
      scale.getComputedBreaks()
    }
    assertTrue(exception.message.contains('Invalid time unit'))
  }

  @Test
  void testOutOfOrderCustomBreaks() {
    // User-specified breaks don't need to be sorted - they're used as-is
    def scale = new ScaleXTime(breaks: [LocalTime.of(15, 0), LocalTime.of(9, 0), LocalTime.of(12, 0)])
    def times = [LocalTime.of(8, 0), LocalTime.of(16, 0)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    // Should return breaks in the order specified by user
    assertEquals(3, breaks.size())
    assertEquals(LocalTime.of(15, 0), breaks[0])
    assertEquals(LocalTime.of(9, 0), breaks[1])
    assertEquals(LocalTime.of(12, 0), breaks[2])
  }

  @Test
  void testTimeSpanNearMidnight() {
    // Test break generation when times are close to end of day (23:00 to 23:59)
    def scale = new ScaleXTime(time_breaks: '15 minutes')
    def times = [LocalTime.of(23, 0), LocalTime.of(23, 59)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertFalse(breaks.isEmpty())
    // Should have breaks at 23:00, 23:15, 23:30, 23:45
    assertTrue(breaks.size() >= 3)
    assertEquals(LocalTime.of(23, 0), breaks[0])
    assertTrue(breaks.every { (it as LocalTime).hour == 23 })  // All should be in hour 23
    // Verify no wrapping past midnight
    assertFalse(breaks.any { (it as LocalTime) == LocalTime.MIDNIGHT })
  }

  @Test
  void testAutoBreaksNearMidnight() {
    // Test auto break generation near end of day
    def scale = new ScaleXTime()
    def times = [LocalTime.of(22, 0), LocalTime.of(23, 59)]
    scale.train(times)

    List breaks = scale.getComputedBreaks()
    assertFalse(breaks.isEmpty())
    // All breaks should be within the 22:00-23:59 range
    assertTrue(breaks.every {
      LocalTime t = it as LocalTime
      !t.isBefore(LocalTime.of(22, 0)) && !t.isAfter(LocalTime.of(23, 59))
    })
  }

  // --- Formatting and labels tests ---

  @Test
  void testDefaultFormat24Hour() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(9, 30), LocalTime.of(14, 45)]
    scale.train(times)

    List<String> labels = scale.getComputedLabels()
    assertFalse(labels.isEmpty())
    // Default format is 'HH:mm'
    assertTrue(labels.every { it.matches('\\d{2}:\\d{2}') })
  }

  @Test
  void testFormat12HourWithAMPM() {
    def scale = new ScaleXTime(time_format: 'h:mm a')
    def times = [LocalTime.of(9, 30), LocalTime.of(14, 45)]
    scale.train(times)

    List<String> labels = scale.getComputedLabels()
    assertFalse(labels.isEmpty())
    // Should contain AM/PM or am/pm (locale-dependent)
    assertTrue(labels.any {
      String label = it.toString()
      label.contains('AM') || label.contains('PM') || label.contains('am') || label.contains('pm')
    })
  }

  @Test
  void testFormatWithSeconds() {
    def scale = new ScaleXTime(time_format: 'HH:mm:ss')
    def times = [LocalTime.of(12, 30, 15), LocalTime.of(14, 45, 30)]
    scale.train(times)

    List<String> labels = scale.getComputedLabels()
    assertFalse(labels.isEmpty())
    // Should have seconds
    assertTrue(labels.every { it.matches('\\d{2}:\\d{2}:\\d{2}') })
  }

  @Test
  void testFormatHourOnly() {
    def scale = new ScaleXTime(time_format: 'h a')
    def times = [LocalTime.of(9, 0), LocalTime.of(15, 0)]
    scale.train(times)

    List<String> labels = scale.getComputedLabels()
    assertFalse(labels.isEmpty())
  }

  @Test
  void testUserSpecifiedLabels() {
    def scale = new ScaleXTime(
      breaks: [LocalTime.of(9, 0), LocalTime.of(12, 0), LocalTime.of(15, 0)],
      labels: ['Morning', 'Noon', 'Afternoon']
    )
    def times = [LocalTime.of(8, 0), LocalTime.of(16, 0)]
    scale.train(times)

    List<String> labels = scale.getComputedLabels()
    assertEquals(['Morning', 'Noon', 'Afternoon'], labels)
  }

  @Test
  void testDateFormatAlias() {
    // Test that date_format parameter works as alias for time_format
    def scale = new ScaleXTime(date_format: 'HH:mm:ss')
    assertEquals('HH:mm:ss', scale.timeFormat)
  }

  @Test
  void testDateBreaksAlias() {
    // Test that date_breaks parameter works as alias for time_breaks
    def scale = new ScaleXTime(date_breaks: '30 minutes')
    assertEquals('30 minutes', scale.timeBreaks)
  }

  // --- Transform operations tests ---

  @Test
  void testForwardTransform() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(0, 0), LocalTime.of(12, 0), LocalTime.of(23, 59, 59)]
    scale.train(times)
    scale.setRange([0, 1000])

    def pos1 = scale.transform(LocalTime.of(0, 0)) as double
    def pos2 = scale.transform(LocalTime.of(12, 0)) as double
    def pos3 = scale.transform(LocalTime.of(23, 59, 59)) as double

    assertEquals(0, pos1, 5.0)
    assertEquals(500, pos2, 5.0)
    assertEquals(1000, pos3, 5.0)
  }

  @Test
  void testInverseTransform() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(6, 0), LocalTime.of(18, 0)]
    scale.train(times)
    scale.setRange([0, 100])

    def time1 = scale.inverse(0)
    def time2 = scale.inverse(50)
    def time3 = scale.inverse(100)

    assertTrue(time1 instanceof LocalTime)
    assertTrue(time2 instanceof LocalTime)
    assertTrue(time3 instanceof LocalTime)

    assertEquals(LocalTime.of(6, 0), time1)
    assertEquals(LocalTime.of(12, 0), time2)
    assertEquals(LocalTime.of(18, 0), time3)
  }

  @Test
  void testLinearInterpolation() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(10, 0), LocalTime.of(14, 0)]  // 4-hour span
    scale.train(times)
    scale.setRange([0, 400])

    def pos1 = scale.transform(LocalTime.of(11, 0)) as double
    def pos2 = scale.transform(LocalTime.of(12, 0)) as double
    def pos3 = scale.transform(LocalTime.of(13, 0)) as double

    assertEquals(100, pos1, 1.0)  // 1 hour in = 100 pixels
    assertEquals(200, pos2, 1.0)  // 2 hours in = 200 pixels
    assertEquals(300, pos3, 1.0)  // 3 hours in = 300 pixels
  }

  @Test
  void testZeroRangeDomain() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(12, 0), LocalTime.of(12, 0)]
    scale.train(times)
    scale.setRange([0, 100])

    def pos = scale.transform(LocalTime.of(12, 0))
    assertNotNull(pos)
    assertEquals(50, pos as double, 1.0)  // Should return midpoint
  }

  @Test
  void testNullTransform() {
    def scale = new ScaleXTime()
    def times = [LocalTime.of(9, 0), LocalTime.of(17, 0)]
    scale.train(times)
    scale.setRange([0, 100])

    assertNull(scale.transform(null))
  }

  // --- Limits and expansion tests ---

  @Test
  void testExplicitLimitsWithLocalTime() {
    def scale = new ScaleXTime(limits: [LocalTime.of(8, 0), LocalTime.of(18, 0)])
    def times = [LocalTime.of(10, 0), LocalTime.of(16, 0)]
    scale.train(times)

    assertEquals(8 * 3600, scale.minSecondsSinceMidnight)
    assertEquals(18 * 3600, scale.maxSecondsSinceMidnight)
  }

  @Test
  void testExplicitLimitsWithStrings() {
    def scale = new ScaleXTime(limits: ['08:00', '18:00'])
    def times = [LocalTime.of(10, 0), LocalTime.of(16, 0)]
    scale.train(times)

    assertEquals(8 * 3600, scale.minSecondsSinceMidnight)
    assertEquals(18 * 3600, scale.maxSecondsSinceMidnight)
  }

  @Test
  void testExpansionWithSeconds() {
    def scale = new ScaleXTime(expand: [0, 3600])  // Add 1 hour on each side
    def times = [LocalTime.of(10, 0), LocalTime.of(14, 0)]
    scale.train(times)

    assertEquals(9 * 3600, scale.minSecondsSinceMidnight)  // 10:00 - 1 hour = 9:00
    assertEquals(15 * 3600, scale.maxSecondsSinceMidnight)  // 14:00 + 1 hour = 15:00
  }

  @Test
  void testExpansionMultiplicative() {
    def scale = new ScaleXTime(expand: [0.1, 0])  // 10% expansion
    def times = [LocalTime.of(10, 0), LocalTime.of(14, 0)]
    scale.train(times)

    long span = 4 * 3600  // 4 hours
    long expansion = (long)(span * 0.1)

    assertEquals(10 * 3600 - expansion, scale.minSecondsSinceMidnight)
    assertEquals(14 * 3600 + expansion, scale.maxSecondsSinceMidnight)
  }

  @Test
  void testNoExpansion() {
    def scale = new ScaleXTime(expand: [0, 0])
    def times = [LocalTime.of(10, 0), LocalTime.of(14, 0)]
    scale.train(times)

    assertEquals(10 * 3600, scale.minSecondsSinceMidnight)
    assertEquals(14 * 3600, scale.maxSecondsSinceMidnight)
  }

  @Test
  void testExpansionClampedToValidRange() {
    def scale = new ScaleXTime(expand: [0, 10000])  // Large expansion
    def times = [LocalTime.of(1, 0), LocalTime.of(2, 0)]
    scale.train(times)

    // Should be clamped to [0, 86399]
    assertEquals(0, scale.minSecondsSinceMidnight)
    assertTrue(scale.maxSecondsSinceMidnight <= 86399)
  }

  @Test
  void testInvalidExpandEmptyList() {
    // expand must have exactly 2 elements
    def exception = assertThrows(IllegalArgumentException) {
      new ScaleXTime(expand: [])
    }
    assertTrue(exception.message.contains('expand must have exactly 2 elements'))
    assertTrue(exception.message.contains('got 0'))
  }

  @Test
  void testInvalidExpandSingleElement() {
    // expand must have exactly 2 elements, not 1
    def exception = assertThrows(IllegalArgumentException) {
      new ScaleXTime(expand: [0.1])
    }
    assertTrue(exception.message.contains('expand must have exactly 2 elements'))
    assertTrue(exception.message.contains('got 1'))
  }

  @Test
  void testInvalidExpandTooManyElements() {
    // expand must have exactly 2 elements, not 3
    def exception = assertThrows(IllegalArgumentException) {
      new ScaleXTime(expand: [0.1, 100, 200])
    }
    assertTrue(exception.message.contains('expand must have exactly 2 elements'))
    assertTrue(exception.message.contains('got 3'))
  }

  @Test
  void testAestheticIsX() {
    def scale = new ScaleXTime()
    assertEquals('x', scale.aesthetic)
  }

  @Test
  void testAestheticIsY() {
    def scale = new ScaleYTime()
    assertEquals('y', scale.aesthetic)
  }

  @Test
  void testDefaultPosition() {
    def scaleX = new ScaleXTime()
    def scaleY = new ScaleYTime()
    assertEquals('bottom', scaleX.position)
    assertEquals('left', scaleY.position)
  }

  @Test
  void testCustomPosition() {
    def scaleX = new ScaleXTime(position: 'top')
    def scaleY = new ScaleYTime(position: 'right')
    assertEquals('top', scaleX.position)
    assertEquals('right', scaleY.position)
  }
}
