package se.alipsa.matrix.csv

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.QuoteMode
import org.junit.jupiter.api.Test

@CompileStatic
class CsvFormatInternalTest {

  @Test
  void excelFormatMatchesApacheExcelDefaultsForSupportedFields() {
    CSVFormat apacheExcel = CSVFormat.EXCEL
    CSVFormat format = CsvFormat.EXCEL.toCSVFormat()

    assertEquals(apacheExcel.getRecordSeparator(), format.getRecordSeparator())
    assertEquals(apacheExcel.getTrim(), format.getTrim())
    assertEquals(apacheExcel.getIgnoreEmptyLines(), format.getIgnoreEmptyLines())
    assertEquals(apacheExcel.getIgnoreSurroundingSpaces(), format.getIgnoreSurroundingSpaces())
    assertEquals(QuoteMode.ALL_NON_NULL, format.getQuoteMode())
    assertTrue(format.getAllowMissingColumnNames())
  }

  @Test
  void builderExposesQuoteModeAndAllowMissingColumnNamesWithoutPresetHelpers() {
    CSVFormat format = CsvFormat.builder()
        .quoteMode(QuoteMode.ALL_NON_NULL)
        .allowMissingColumnNames(true)
        .build()
        .toCSVFormat()

    assertEquals(QuoteMode.ALL_NON_NULL, format.getQuoteMode())
    assertTrue(format.getAllowMissingColumnNames())
  }
}
