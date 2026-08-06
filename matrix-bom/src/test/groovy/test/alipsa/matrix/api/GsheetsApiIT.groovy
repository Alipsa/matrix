package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.alipsa.matrix.gsheets.GsUtil

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

/** Covers the offline-safe A1 range and column helpers; credentialed calls stay external-tagged. */
@Tag('gsheets')
class GsheetsApiIT implements ApiItSupport {

  @Test
  void parsesA1RangesWithoutCredentials() {
    assertEquals(3, GsUtil.columnCountForRange('Sheet1!B2:D10'))
    assertEquals(1, GsUtil.columnCountForRange('A1'))
    assertEquals(27, GsUtil.asColumnNumber('AA'))
    assertThrows(IllegalArgumentException) { GsUtil.columnCountForRange('not-a-range') }
  }
}
