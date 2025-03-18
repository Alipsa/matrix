import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.ListConverter

import java.text.DecimalFormat
import java.text.NumberFormat

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
}
