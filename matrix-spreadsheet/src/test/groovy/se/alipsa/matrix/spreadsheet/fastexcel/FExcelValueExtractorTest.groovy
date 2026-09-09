package se.alipsa.matrix.spreadsheet.fastexcel

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Test

import java.util.Locale

class FExcelValueExtractorTest {

  @Test
  void testParseFormulaNumberUsesLocaleInvariantOoxmlFormatWithoutPrecisionLoss() {
    Locale originalLocale = Locale.default
    try {
      Locale.default = Locale.forLanguageTag('sv-SE')
      assertEquals(1234.5G, FExcelValueExtractor.parseFormulaNumber('1234.5'))
      assertEquals(123456789012345678901G,
          FExcelValueExtractor.parseFormulaNumber('123456789012345678901'))
      assertEquals(0.1234567890123456789G,
          FExcelValueExtractor.parseFormulaNumber('0.1234567890123456789'))
      assertEquals('1234,5', FExcelValueExtractor.parseFormulaNumber('1234,5'))
      assertEquals('not numeric', FExcelValueExtractor.parseFormulaNumber('not numeric'))
    } finally {
      Locale.default = originalLocale
    }
  }
}
