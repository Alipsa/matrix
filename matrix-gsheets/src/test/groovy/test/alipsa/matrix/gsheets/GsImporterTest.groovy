package test.alipsa.matrix.gsheets

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gsheets.GsImporter

import static org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for GsImporter utility methods.
 */
class GsImporterTest {

  @Test
  void testBuildHeaderWithAllValues() {
    def firstRow = ["Name", "Age", "City"]
    def headers = GsImporter.buildHeader(3, firstRow)

    assertEquals(3, headers.size())
    assertEquals("Name", headers[0])
    assertEquals("Age", headers[1])
    assertEquals("City", headers[2])
  }

  @Test
  void testBuildHeaderWithNullValues() {
    def firstRow = ["Name", null, "City"]
    def headers = GsImporter.buildHeader(3, firstRow)

    assertEquals(3, headers.size())
    assertEquals("Name", headers[0])
    assertEquals("c2", headers[1])  // Null should become "c2"
    assertEquals("City", headers[2])
  }

  @Test
  void testBuildHeaderWithEmptyStrings() {
    def firstRow = ["Name", "", "City"]
    def headers = GsImporter.buildHeader(3, firstRow)

    assertEquals(3, headers.size())
    assertEquals("Name", headers[0])
    assertEquals("c2", headers[1])  // Empty string should become "c2"
    assertEquals("City", headers[2])
  }

  @Test
  void testBuildHeaderWithWhitespaceOnly() {
    def firstRow = ["Name", "   ", "City"]
    def headers = GsImporter.buildHeader(3, firstRow)

    assertEquals(3, headers.size())
    assertEquals("Name", headers[0])
    assertEquals("c2", headers[1])  // Whitespace should become "c2"
    assertEquals("City", headers[2])
  }

  @Test
  void testBuildHeaderWithMoreColumnsThanValues() {
    // When ncol > firstRow.size(), should generate column names
    def firstRow = ["Name", "Age"]
    def headers = GsImporter.buildHeader(5, firstRow)

    assertEquals(5, headers.size())
    assertEquals("Name", headers[0])
    assertEquals("Age", headers[1])
    assertEquals("c3", headers[2])  // Missing columns get generated names
    assertEquals("c4", headers[3])
    assertEquals("c5", headers[4])
  }

  @Test
  void testBuildHeaderWithEmptyFirstRow() {
    def firstRow = []
    def headers = GsImporter.buildHeader(3, firstRow)

    assertEquals(3, headers.size())
    assertEquals("c1", headers[0])
    assertEquals("c2", headers[1])
    assertEquals("c3", headers[2])
  }

  @Test
  void testBuildHeaderWithFewerColumnsThanValues() {
    // When ncol < firstRow.size(), should only use ncol headers
    def firstRow = ["Name", "Age", "City", "Country", "Zip"]
    def headers = GsImporter.buildHeader(3, firstRow)

    assertEquals(3, headers.size())
    assertEquals("Name", headers[0])
    assertEquals("Age", headers[1])
    assertEquals("City", headers[2])
  }

  @Test
  void testBuildHeaderWithNumericValues() {
    def firstRow = [100, 200, 300]
    def headers = GsImporter.buildHeader(3, firstRow)

    assertEquals(3, headers.size())
    assertEquals("100", headers[0])
    assertEquals("200", headers[1])
    assertEquals("300", headers[2])
  }

  @Test
  void testFillListToSizeAlreadyCorrectSize() {
    def list = ["A", "B", "C"]
    def result = GsImporter.fillListToSize(list, 3)

    assertEquals(3, result.size())
    assertEquals("A", result[0])
    assertEquals("B", result[1])
    assertEquals("C", result[2])
  }

  @Test
  void testFillListToSizeLargerThanDesired() {
    // If list is already larger than desired size, should return unchanged
    def list = ["A", "B", "C", "D", "E"]
    def result = GsImporter.fillListToSize(list, 3)

    assertEquals(5, result.size())  // Should not truncate
    assertEquals("A", result[0])
    assertEquals("E", result[4])
  }

  @Test
  void testFillListToSizeSmallerThanDesired() {
    // Should pad with nulls
    def list = ["A", "B"]
    def result = GsImporter.fillListToSize(list, 5)

    assertEquals(5, result.size())
    assertEquals("A", result[0])
    assertEquals("B", result[1])
    assertNull(result[2])
    assertNull(result[3])
    assertNull(result[4])
  }

  @Test
  void testFillListToSizeEmptyList() {
    def list = []
    def result = GsImporter.fillListToSize(list, 3)

    assertEquals(3, result.size())
    assertNull(result[0])
    assertNull(result[1])
    assertNull(result[2])
  }

  @Test
  void testFillListToSizeWithZeroDesiredSize() {
    def list = ["A", "B", "C"]
    def result = GsImporter.fillListToSize(list, 0)

    // Should return the list unchanged since it's already >= desired size
    assertEquals(3, result.size())
  }

  @Test
  void testImportSheetWithNullSheetId() {
    assertThrows(IllegalArgumentException, () ->
      GsImporter.importSheet(null, "Sheet1!A1:D10", true)
    )
  }

  @Test
  void testImportSheetWithEmptySheetId() {
    assertThrows(IllegalArgumentException, () ->
      GsImporter.importSheet("", "Sheet1!A1:D10", true)
    )
  }

  @Test
  void testImportSheetWithNullRange() {
    assertThrows(IllegalArgumentException, () ->
      GsImporter.importSheet("some-sheet-id", null, true)
    )
  }

  @Test
  void testImportSheetWithEmptyRange() {
    assertThrows(IllegalArgumentException, () ->
      GsImporter.importSheet("some-sheet-id", "", true)
    )
  }
}
