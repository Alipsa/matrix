package se.alipsa.matrix.spreadsheet.fastexcel

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

import org.junit.jupiter.api.Test

class FDateUtilTest {

  private static final int CUSTOM_FORMAT_ID = 164

  @Test
  void testQuotedLiteralsDoNotHideDateFormats() {
    assertTrue(FDateUtil.isADateFormat(CUSTOM_FORMAT_ID, 'yyyy-mm-dd "status"'))
    assertTrue(FDateUtil.isADateFormat(CUSTOM_FORMAT_ID, '"Sold on" yyyy-mm-dd'))
    assertTrue(FDateUtil.isADateFormat(CUSTOM_FORMAT_ID, '[$-x-sysdate]dddd, mmmm dd, yyyy'))
  }

  @Test
  void testPunctuationOnlyFormatsAreNotDates() {
    assertFalse(FDateUtil.isADateFormat(CUSTOM_FORMAT_ID, '"Total" ,'))
    assertFalse(FDateUtil.isADateFormat(CUSTOM_FORMAT_ID, '"x" -'))
    assertFalse(FDateUtil.isADateFormat(CUSTOM_FORMAT_ID, '""'))
    assertFalse(FDateUtil.isADateFormat(CUSTOM_FORMAT_ID, '#,##0 "kr"'))
    assertFalse(FDateUtil.isADateFormat(CUSTOM_FORMAT_ID, '0.00 "m"'))
  }

}
