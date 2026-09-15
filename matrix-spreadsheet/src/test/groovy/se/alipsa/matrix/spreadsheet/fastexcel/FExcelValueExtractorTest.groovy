package se.alipsa.matrix.spreadsheet.fastexcel

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

class FExcelValueExtractorTest {

  @Test
  void testParseFormulaNumberUsesLocaleInvariantOoxmlFormatWithoutPrecisionLoss() {
    assertEquals(1234.5G, FExcelValueExtractor.parseFormulaNumber('1234.5'))
    assertEquals(123456789012345678901G,
        FExcelValueExtractor.parseFormulaNumber('123456789012345678901'))
    assertEquals(0.1234567890123456789G,
        FExcelValueExtractor.parseFormulaNumber('0.1234567890123456789'))
    assertEquals('1234,5', FExcelValueExtractor.parseFormulaNumber('1234,5'))
    assertEquals('1,234.5', FExcelValueExtractor.parseFormulaNumber('1,234.5'))
    assertEquals('not numeric', FExcelValueExtractor.parseFormulaNumber('not numeric'))
  }

  @Test
  void testHasTimeComponent() {
    assertTrue(FExcelValueExtractor.hasTimeComponent('yyyy-mm-dd h:mm'))
    assertTrue(FExcelValueExtractor.hasTimeComponent('[h]:mm'))
    assertTrue(FExcelValueExtractor.hasTimeComponent('h:mm AM/PM'))
    assertTrue(FExcelValueExtractor.hasTimeComponent('yyyy-mm-dd hh:mm:ss'))
    assertFalse(FExcelValueExtractor.hasTimeComponent('yyyy-mm-dd'))
    assertFalse(FExcelValueExtractor.hasTimeComponent('yyyy-mm-dd "status"'))
    assertFalse(FExcelValueExtractor.hasTimeComponent('yyyy-mm-dd\\h'))
    assertFalse(FExcelValueExtractor.hasTimeComponent('yyyy-mm-dd "h:mm"'))
    assertFalse(FExcelValueExtractor.hasTimeComponent('[$-x-sysdate]dddd, mmmm dd, yyyy'))
    assertFalse(FExcelValueExtractor.hasTimeComponent('[$-en-US]d-mmm-yy'))
    assertFalse(FExcelValueExtractor.hasTimeComponent('[$-sv-SE]yyyy-mm-dd'))
    assertFalse(FExcelValueExtractor.hasTimeComponent('[White]yyyy-mm-dd'))
    assertFalse(FExcelValueExtractor.hasTimeComponent('[Holiday]yyyy-mm-dd'))
    assertFalse(FExcelValueExtractor.hasTimeComponent(null))
  }
}
