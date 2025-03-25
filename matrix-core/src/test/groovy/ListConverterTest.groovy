import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.ListConverter

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

import static org.junit.jupiter.api.Assertions.*

class ListConverterTest {

  @Test
  void testConvertStringsToDoubles() {
    assertIterableEquals([5.0d, 1.0d, 25.5d], ListConverter.toDoubles(['5.0', '1.0', '25.5']))
    NumberFormat numberFormat = NumberFormat.getInstance(Locale.of('sv', 'SE'))

    def expected = [50d, 2000d, -145292d, -4.0d, 300d, 3.14d]
    // using hyphens as negative symbol (US format), should be converted
    // to minus
    assertIterableEquals(expected,
    ListConverter.convert(['50', '2000', '-145292', '-4.0', '300', '3,14'], Double, null,null, numberFormat))
    numberFormat = NumberFormat.getInstance(Locale.of('en', 'US'))
    assertIterableEquals(expected, ListConverter.convert(
        ['50', '2000', '-145292', '-4.0', '300', '3.14'],
        Double,
        null,
        null, numberFormat
    ))
  }

  @Test
  void testConvertNumbersToDoubles() {
    assertIterableEquals([50d, 2000d, -145292d, 4d, 300d, 3.14d],
        ListConverter.toDoubles([50i, 2000g, -145292, 4 as short, 300, 3.14f]))
  }

  @Test
  void testToDates() {
    def dates = ['2023-01-01', '2023-12-31']
    def expected = [
        new SimpleDateFormat('yyyy-MM-dd').parse('2023-01-01'),
        new SimpleDateFormat('yyyy-MM-dd').parse('2023-12-31')
    ]
    assertIterableEquals(expected, ListConverter.toDates(dates))
  }

  @Test
  void testToSqlDates() {
    def dates = ['2023-01-01', '2023-12-31']
    def expected = [
        java.sql.Date.valueOf('2023-01-01'),
        java.sql.Date.valueOf('2023-12-31')
    ]
    assertIterableEquals(expected, ListConverter.toSqlDates(dates))
  }

  @Test
  void testToLocalDates() {
    def dates = ['2023-01-01', '2023-12-31']
    def expected = [
        LocalDate.parse('2023-01-01'),
        LocalDate.parse('2023-12-31')
    ]
    assertIterableEquals(expected, ListConverter.toLocalDates(dates))
  }

  @Test
  void testToLocalDateTimes() {
    def dates = ['2023-01-01T10:15:30', '2023-12-31T23:59:59']
    def expected = [
        LocalDateTime.parse('2023-01-01T10:15:30'),
        LocalDateTime.parse('2023-12-31T23:59:59')
    ]
    assertIterableEquals(expected, ListConverter.toLocalDateTimes(dates))
  }

  @Test
  void testToYearMonths() {
    def dates = [LocalDate.of(2023, 5, 10), LocalDate.of(2024, 6, 15)]
    def expected = [
        YearMonth.of(2023, 5),
        YearMonth.of(2024, 6)
    ]
    assertIterableEquals(expected, ListConverter.toYearMonths(dates))
  }

  @Test
  void testToStrings() {
    def objects = [123, true, LocalDate.of(2023, 1, 1)]
    def expected = ['123', 'true', '2023-01-01']
    assertIterableEquals(expected, ListConverter.toStrings(objects))
  }

  @Test
  void testToFloats() {
    def numbers = [1.1, 2.2, 3.3]
    def expected = [1.1f, 2.2f, 3.3f]
    assertIterableEquals(expected, ListConverter.toFloats(numbers))
  }

  @Test
  void testToDoubleArray() {
    def numbers = [1.1, 2.2, 3.3]
    def expected = [1.1d, 2.2d, 3.3d] as double[]
    assertArrayEquals(expected, ListConverter.toDoubleArray(numbers))
  }

  @Test
  void testToBigDecimals() {
    def numbers = [1.1, 2.2, 3.3]
    def expected = [1.1G, 2.2G, 3.3G]
    assertIterableEquals(expected, ListConverter.toBigDecimals(numbers))
  }
}
