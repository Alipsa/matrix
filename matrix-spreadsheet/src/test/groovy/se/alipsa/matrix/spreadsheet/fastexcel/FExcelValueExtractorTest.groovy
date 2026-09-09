package se.alipsa.matrix.spreadsheet.fastexcel

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

import java.text.NumberFormat
import java.util.Locale

class FExcelValueExtractorTest {

  @Test
  void testParseFormulaNumberUsesSameLocaleForDetectionAndConversion() {
    NumberFormat format = NumberFormat.getInstance(Locale.forLanguageTag('sv-SE'))

    assertEquals(1234.5G, FExcelValueExtractor.parseFormulaNumber('1234,5', format))
    assertEquals(1234G, FExcelValueExtractor.parseFormulaNumber('1\u00a0234', format))
    assertEquals('not numeric', FExcelValueExtractor.parseFormulaNumber('not numeric', format))
  }
}
