package gg

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.GgChart
import se.alipsa.matrix.gg.scale.ScaleXDate
import se.alipsa.matrix.gg.scale.ScaleYDate
import se.alipsa.matrix.gg.scale.ScaleXDatetime
import se.alipsa.matrix.gg.scale.ScaleYDatetime

import java.time.LocalDate
import java.time.LocalDateTime

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

class ScaleDateTest {

  static File outputDir

  @BeforeAll
  static void setup() {
    outputDir = new File('build/test-output/scale-date')
    outputDir.mkdirs()
  }

  @Test
  void testScaleXDateBasic() {
    // Create test data with dates
    def dates = [
      LocalDate.of(2024, 1, 1),
      LocalDate.of(2024, 2, 1),
      LocalDate.of(2024, 3, 1),
      LocalDate.of(2024, 4, 1),
      LocalDate.of(2024, 5, 1),
      LocalDate.of(2024, 6, 1)
    ]
    def values = [10, 15, 12, 18, 22, 20]

    Matrix df = Matrix.builder()
      .columnNames('date', 'value')
      .rows([dates, values].transpose())
      .types(LocalDate, Number)
      .build()

    GgChart chart = ggplot(df, aes('date', 'value')) +
        geom_line() +
        geom_point() +
        scale_x_date() +
        labs(title: 'Date Scale Test', x: 'Date', y: 'Value')

    def svg = chart.render()
    assertNotNull(svg, 'Chart should render')

    File outFile = new File(outputDir, 'scale_x_date_basic.svg')
    write(svg, outFile)
    assertTrue(outFile.exists(), 'Output file should exist')
  }

  @Test
  void testScaleXDateWithFormat() {
    def dates = [
      LocalDate.of(2024, 1, 15),
      LocalDate.of(2024, 2, 15),
      LocalDate.of(2024, 3, 15)
    ]
    def values = [100, 120, 115]

    Matrix df = Matrix.builder()
      .columnNames('date', 'value')
      .rows([dates, values].transpose())
      .types(LocalDate, Number)
      .build()

    GgChart chart = ggplot(df, aes('date', 'value')) +
        geom_line() +
        scale_x_date(date_format: 'MMM yyyy') +
        labs(title: 'Custom Date Format')

    def svg = chart.render()
    assertNotNull(svg, 'Chart should render')

    File outFile = new File(outputDir, 'scale_x_date_format.svg')
    write(svg, outFile)
    assertTrue(outFile.exists(), 'Output file should exist')
  }

  @Test
  void testScaleXDateWithBreaks() {
    def dates = (0..11).collect { LocalDate.of(2024, 1, 1).plusMonths(it) }
    def values = (0..11).collect { (Math.random() * 100) as int }

    Matrix df = Matrix.builder()
      .columnNames('date', 'value')
      .rows([dates, values].transpose())
      .types(LocalDate, Number)
      .build()

    GgChart chart = ggplot(df, aes('date', 'value')) +
        geom_line() +
        geom_point() +
        scale_x_date(date_breaks: '3 months', date_format: 'MMM') +
        labs(title: 'Quarterly Breaks')

    def svg = chart.render()
    assertNotNull(svg, 'Chart should render')

    File outFile = new File(outputDir, 'scale_x_date_breaks.svg')
    write(svg, outFile)
    assertTrue(outFile.exists(), 'Output file should exist')
  }

  @Test
  void testScaleYDate() {
    // Create test data with y-axis dates (unusual but valid)
    def categories = ['A', 'B', 'C', 'D']
    def dates = [
      LocalDate.of(2024, 1, 1),
      LocalDate.of(2024, 3, 15),
      LocalDate.of(2024, 6, 1),
      LocalDate.of(2024, 9, 30)
    ]

    Matrix df = Matrix.builder()
      .columnNames('category', 'date')
      .rows([categories, dates].transpose())
      .types(String, LocalDate)
      .build()

    GgChart chart = ggplot(df, aes('category', 'date')) +
        geom_point(size: 5) +
        scale_y_date(date_format: 'MMM dd') +
        labs(title: 'Y-Axis Date Scale')

    def svg = chart.render()
    assertNotNull(svg, 'Chart should render')

    File outFile = new File(outputDir, 'scale_y_date.svg')
    write(svg, outFile)
    assertTrue(outFile.exists(), 'Output file should exist')
  }

  @Test
  void testScaleXDatetimeBasic() {
    // Create test data with datetime values
    def datetimes = [
      LocalDateTime.of(2024, 1, 15, 8, 0),
      LocalDateTime.of(2024, 1, 15, 10, 0),
      LocalDateTime.of(2024, 1, 15, 12, 0),
      LocalDateTime.of(2024, 1, 15, 14, 0),
      LocalDateTime.of(2024, 1, 15, 16, 0),
      LocalDateTime.of(2024, 1, 15, 18, 0)
    ]
    def values = [10, 25, 30, 28, 35, 20]

    Matrix df = Matrix.builder()
      .columnNames('datetime', 'value')
      .rows([datetimes, values].transpose())
      .types(LocalDateTime, Number)
      .build()

    GgChart chart = ggplot(df, aes('datetime', 'value')) +
        geom_line() +
        geom_point() +
        scale_x_datetime() +
        labs(title: 'DateTime Scale Test', x: 'Time', y: 'Value')

    def svg = chart.render()
    assertNotNull(svg, 'Chart should render')

    File outFile = new File(outputDir, 'scale_x_datetime_basic.svg')
    write(svg, outFile)
    assertTrue(outFile.exists(), 'Output file should exist')
  }

  @Test
  void testScaleXDatetimeWithHourBreaks() {
    def datetimes = (0..23).collect { LocalDateTime.of(2024, 1, 15, it, 0) }
    def values = (0..23).collect { Math.sin(it * Math.PI / 12) * 50 + 50 }

    Matrix df = Matrix.builder()
      .columnNames('datetime', 'value')
      .rows([datetimes, values].transpose())
      .types(LocalDateTime, Number)
      .build()

    GgChart chart = ggplot(df, aes('datetime', 'value')) +
        geom_line() +
        scale_x_datetime(date_breaks: '4 hours', date_labels: 'HH:mm') +
        labs(title: '24-Hour Data with 4-Hour Breaks')

    def svg = chart.render()
    assertNotNull(svg, 'Chart should render')

    File outFile = new File(outputDir, 'scale_x_datetime_hours.svg')
    write(svg, outFile)
    assertTrue(outFile.exists(), 'Output file should exist')
  }

  @Test
  void testScaleXDatetimeWithMinuteBreaks() {
    def datetimes = (0..59).collect { LocalDateTime.of(2024, 1, 15, 14, it) }
    def values = (0..59).collect { Math.random() * 100 }

    Matrix df = Matrix.builder()
      .columnNames('datetime', 'value')
      .rows([datetimes, values].transpose())
      .types(LocalDateTime, Number)
      .build()

    GgChart chart = ggplot(df, aes('datetime', 'value')) +
        geom_point(size: 2) +
        scale_x_datetime(date_breaks: '15 minutes', date_labels: 'HH:mm') +
        labs(title: 'Minute-Level Data with 15-Min Breaks')

    def svg = chart.render()
    assertNotNull(svg, 'Chart should render')

    File outFile = new File(outputDir, 'scale_x_datetime_minutes.svg')
    write(svg, outFile)
    assertTrue(outFile.exists(), 'Output file should exist')
  }

  @Test
  void testScaleYDatetime() {
    def categories = ['Event A', 'Event B', 'Event C']
    def datetimes = [
      LocalDateTime.of(2024, 1, 15, 9, 30),
      LocalDateTime.of(2024, 1, 15, 14, 0),
      LocalDateTime.of(2024, 1, 15, 17, 45)
    ]

    Matrix df = Matrix.builder()
      .columnNames('event', 'time')
      .rows([categories, datetimes].transpose())
      .types(String, LocalDateTime)
      .build()

    GgChart chart = ggplot(df, aes('event', 'time')) +
        geom_point(size: 5) +
        scale_y_datetime(date_labels: 'HH:mm') +
        labs(title: 'Y-Axis DateTime Scale')

    def svg = chart.render()
    assertNotNull(svg, 'Chart should render')

    File outFile = new File(outputDir, 'scale_y_datetime.svg')
    write(svg, outFile)
    assertTrue(outFile.exists(), 'Output file should exist')
  }

  @Test
  void testDateScaleTraining() {
    ScaleXDate scale = new ScaleXDate()

    def dates = [
      LocalDate.of(2024, 1, 1),
      LocalDate.of(2024, 6, 15),
      LocalDate.of(2024, 12, 31)
    ]

    scale.train(dates)

    assertTrue(scale.trained, 'Scale should be trained')
    assertNotNull(scale.computedDomain, 'Domain should be computed')
    assertEquals(2, scale.computedDomain.size(), 'Domain should have min and max')

    // Verify breaks are generated
    def breaks = scale.getComputedBreaks()
    assertNotNull(breaks, 'Breaks should be generated')
    assertTrue(breaks.size() > 0, 'Should have at least one break')
    assertTrue(breaks[0] instanceof LocalDate, 'Breaks should be LocalDate')

    // Verify labels are generated
    def labels = scale.getComputedLabels()
    assertNotNull(labels, 'Labels should be generated')
    assertEquals(breaks.size(), labels.size(), 'Should have same number of labels as breaks')
  }

  @Test
  void testDatetimeScaleTraining() {
    ScaleXDatetime scale = new ScaleXDatetime()

    def datetimes = [
      LocalDateTime.of(2024, 1, 15, 8, 0),
      LocalDateTime.of(2024, 1, 15, 12, 0),
      LocalDateTime.of(2024, 1, 15, 18, 0)
    ]

    scale.train(datetimes)

    assertTrue(scale.trained, 'Scale should be trained')
    assertNotNull(scale.computedDomain, 'Domain should be computed')

    // Verify breaks are generated
    def breaks = scale.getComputedBreaks()
    assertNotNull(breaks, 'Breaks should be generated')
    assertTrue(breaks.size() > 0, 'Should have at least one break')
    assertTrue(breaks[0] instanceof LocalDateTime, 'Breaks should be LocalDateTime')

    // Verify labels are generated
    def labels = scale.getComputedLabels()
    assertNotNull(labels, 'Labels should be generated')
    assertEquals(breaks.size(), labels.size(), 'Should have same number of labels as breaks')
  }

  @Test
  void testDateScaleTransform() {
    ScaleXDate scale = new ScaleXDate()
    scale.range = [0, 100]

    def dates = [
      LocalDate.of(2024, 1, 1),
      LocalDate.of(2024, 12, 31)
    ]

    scale.train(dates)

    // Transform the midpoint date
    LocalDate midDate = LocalDate.of(2024, 7, 1)
    def transformed = scale.transform(midDate) as double

    // Should be roughly in the middle
    assertTrue(transformed > 40 && transformed < 60, "Midpoint should transform to roughly middle of range: $transformed")

    // Inverse should return close to original date
    def inverse = scale.inverse(transformed)
    assertTrue(inverse instanceof LocalDate, 'Inverse should return LocalDate')
  }

  @Test
  void testDatetimeScaleTransform() {
    ScaleXDatetime scale = new ScaleXDatetime()
    scale.range = [0, 100]

    def datetimes = [
      LocalDateTime.of(2024, 1, 15, 0, 0),
      LocalDateTime.of(2024, 1, 16, 0, 0)  // Next day midnight
    ]

    scale.train(datetimes)

    // Transform noon
    LocalDateTime noon = LocalDateTime.of(2024, 1, 15, 12, 0)
    def transformed = scale.transform(noon) as double

    // Should be roughly in the middle
    assertTrue(transformed > 40 && transformed < 60, "Noon should transform to roughly middle of range: $transformed")

    // Inverse should return close to original datetime
    def inverse = scale.inverse(transformed)
    assertTrue(inverse instanceof LocalDateTime, 'Inverse should return LocalDateTime')
  }

  @Test
  void testDateScaleWithJavaUtilDate() {
    // Test compatibility with java.util.Date
    def dates = [
      new Date(124, 0, 1),   // Jan 1, 2024
      new Date(124, 5, 15),  // Jun 15, 2024
      new Date(124, 11, 31)  // Dec 31, 2024
    ]
    def values = [10, 20, 15]

    Matrix df = Matrix.builder()
      .columnNames('date', 'value')
      .rows([dates, values].transpose())
      .types(Date, Number)
      .build()

    GgChart chart = ggplot(df, aes('date', 'value')) +
        geom_line() +
        geom_point() +
        scale_x_date() +
        labs(title: 'java.util.Date Compatibility')

    def svg = chart.render()
    assertNotNull(svg, 'Chart should render with java.util.Date')

    File outFile = new File(outputDir, 'scale_x_date_util_date.svg')
    write(svg, outFile)
    assertTrue(outFile.exists(), 'Output file should exist')
  }

  @Test
  void testDateScaleWithStringDates() {
    // Test compatibility with ISO date strings
    def dates = ['2024-01-01', '2024-06-15', '2024-12-31']
    def values = [10, 20, 15]

    Matrix df = Matrix.builder()
      .columnNames('date', 'value')
      .rows([dates, values].transpose())
      .types(String, Number)
      .build()

    GgChart chart = ggplot(df, aes('date', 'value')) +
        geom_line() +
        geom_point() +
        scale_x_date() +
        labs(title: 'String Date Parsing')

    def svg = chart.render()
    assertNotNull(svg, 'Chart should render with string dates')

    File outFile = new File(outputDir, 'scale_x_date_string.svg')
    write(svg, outFile)
    assertTrue(outFile.exists(), 'Output file should exist')
  }

  @Test
  void testDatetimeScaleWithStringDates() {
    // Test compatibility with ISO datetime strings
    def datetimes = ['2024-01-15T08:00:00', '2024-01-15T12:00:00', '2024-01-15T18:00:00']
    def values = [10, 25, 15]

    Matrix df = Matrix.builder()
      .columnNames('datetime', 'value')
      .rows([datetimes, values].transpose())
      .types(String, Number)
      .build()

    GgChart chart = ggplot(df, aes('datetime', 'value')) +
        geom_line() +
        geom_point() +
        scale_x_datetime() +
        labs(title: 'String DateTime Parsing')

    def svg = chart.render()
    assertNotNull(svg, 'Chart should render with string datetimes')

    File outFile = new File(outputDir, 'scale_x_datetime_string.svg')
    write(svg, outFile)
    assertTrue(outFile.exists(), 'Output file should exist')
  }

  @Test
  void testDateScaleAutoBreaks() {
    // Test auto break generation for different time spans

    // Short span: days
    ScaleXDate shortScale = new ScaleXDate()
    shortScale.train([
      LocalDate.of(2024, 1, 1),
      LocalDate.of(2024, 1, 10)
    ])
    def shortBreaks = shortScale.getComputedBreaks()
    assertTrue(shortBreaks.size() >= 2, "Short span should have multiple breaks")

    // Medium span: months
    ScaleXDate mediumScale = new ScaleXDate()
    mediumScale.train([
      LocalDate.of(2024, 1, 1),
      LocalDate.of(2024, 6, 30)
    ])
    def mediumBreaks = mediumScale.getComputedBreaks()
    assertTrue(mediumBreaks.size() >= 2, "Medium span should have multiple breaks")

    // Long span: years
    ScaleXDate longScale = new ScaleXDate()
    longScale.train([
      LocalDate.of(2020, 1, 1),
      LocalDate.of(2024, 12, 31)
    ])
    def longBreaks = longScale.getComputedBreaks()
    assertTrue(longBreaks.size() >= 2, "Long span should have multiple breaks")
  }

  @Test
  void testDatetimeScaleAutoBreaks() {
    // Test auto break generation for different time spans

    // Short span: minutes
    ScaleXDatetime shortScale = new ScaleXDatetime()
    shortScale.train([
      LocalDateTime.of(2024, 1, 15, 12, 0),
      LocalDateTime.of(2024, 1, 15, 12, 30)
    ])
    def shortBreaks = shortScale.getComputedBreaks()
    assertTrue(shortBreaks.size() >= 2, "Short span should have multiple breaks")

    // Medium span: hours
    ScaleXDatetime mediumScale = new ScaleXDatetime()
    mediumScale.train([
      LocalDateTime.of(2024, 1, 15, 8, 0),
      LocalDateTime.of(2024, 1, 15, 18, 0)
    ])
    def mediumBreaks = mediumScale.getComputedBreaks()
    assertTrue(mediumBreaks.size() >= 2, "Medium span should have multiple breaks")

    // Long span: days
    ScaleXDatetime longScale = new ScaleXDatetime()
    longScale.train([
      LocalDateTime.of(2024, 1, 1, 0, 0),
      LocalDateTime.of(2024, 1, 7, 0, 0)
    ])
    def longBreaks = longScale.getComputedBreaks()
    assertTrue(longBreaks.size() >= 2, "Long span should have multiple breaks")
  }
}
