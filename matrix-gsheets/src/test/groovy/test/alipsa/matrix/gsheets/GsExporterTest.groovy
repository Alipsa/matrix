package test.alipsa.matrix.gsheets

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gsheets.GsExporter

/**
 * Unit tests for GsExporter delegation to GsheetsWriter.
 */
class GsExporterTest {

  @Test
  void testExportSheetWithNullMatrix() {
    assertThrows(IllegalArgumentException, () -> GsExporter.exportSheet(null))
  }

  @Test
  void testExportSheetWithEmptyColumns() {
    // Create a matrix with no columns
    def matrix = Matrix.builder('test').data([:]).build()
    assertThrows(IllegalArgumentException, () -> GsExporter.exportSheet(matrix))
  }

  @Test
  void testExportSheetWithEmptyRows() {
    // Create a matrix with columns but no rows
    def matrix = Matrix.builder('test').columnNames(['col1', 'col2']).types([String, String]).build()
    assertThrows(IllegalArgumentException, () -> GsExporter.exportSheet(matrix))
  }

}
