package test.alipsa.matrix.gsheets

import com.google.auth.oauth2.GoogleCredentials
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gsheets.GsheetsReader

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for GsheetsReader class.
 *
 * These tests verify that GsheetsReader properly delegates to GsImporter
 * and maintains backward compatibility.
 */
class GsheetsReaderTest {

  @Test
  void testReadInvalidSpreadsheetIdThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      GsheetsReader.read(null, "Sheet1!A1:D10", true)
    }, "Should throw on null spreadsheet ID")

    assertThrows(IllegalArgumentException.class, () -> {
      GsheetsReader.read("", "Sheet1!A1:D10", true)
    }, "Should throw on empty spreadsheet ID")
  }

  @Test
  void testReadInvalidRangeThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      GsheetsReader.read("some-id", null, true)
    }, "Should throw on null range")

    assertThrows(IllegalArgumentException.class, () -> {
      GsheetsReader.read("some-id", "", true)
    }, "Should throw on empty range")
  }

  @Test
  void testReadAsObjectInvalidSpreadsheetIdThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      GsheetsReader.readAsObject(null, "Sheet1!A1:D10", true)
    }, "Should throw on null spreadsheet ID")
  }

  @Test
  void testReadAsObjectInvalidRangeThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      GsheetsReader.readAsObject("some-id", null, true)
    }, "Should throw on null range")
  }

  @Test
  void testReadAsStringsInvalidSpreadsheetIdThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      GsheetsReader.readAsStrings(null, "Sheet1!A1:D10", true)
    }, "Should throw on null spreadsheet ID")
  }

  @Test
  void testReadAsStringsInvalidRangeThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      GsheetsReader.readAsStrings("some-id", null, true)
    }, "Should throw on null range")
  }

  @Test
  void testMethodsDelegateToGsImporter() {
    // These tests verify that methods exist and have correct signatures
    // Actual functionality is tested in GsImporterTest since we delegate to it

    // Verify read method exists with correct parameter types
    def readMethod = GsheetsReader.class.getDeclaredMethod(
        "read", String, String, boolean, GoogleCredentials
    )
    assertNotNull(readMethod)
    assertTrue(readMethod.returnType == Matrix)

    // Verify readAsObject method exists with correct parameter types
    def readAsObjectMethod = GsheetsReader.class.getDeclaredMethod(
        "readAsObject", String, String, boolean, GoogleCredentials, boolean
    )
    assertNotNull(readAsObjectMethod)
    assertTrue(readAsObjectMethod.returnType == Matrix)

    // Verify readAsStrings method exists with correct parameter types
    def readAsStringsMethod = GsheetsReader.class.getDeclaredMethod(
        "readAsStrings", String, String, boolean, GoogleCredentials
    )
    assertNotNull(readAsStringsMethod)
    assertTrue(readAsStringsMethod.returnType == Matrix)
  }
}
