package test.alipsa.matrix.gsheets

import com.google.auth.oauth2.GoogleCredentials
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gsheets.GsheetsWriter

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for GsheetsWriter class.
 *
 * These tests verify that GsheetsWriter properly delegates to GsExporter
 * and maintains backward compatibility.
 */
class GsheetsWriterTest {

  @Test
  void testWriteNullMatrixThrows() {
    assertThrows(IllegalArgumentException.class, () -> {
      GsheetsWriter.write(null)
    }, "Should throw on null matrix")
  }

  @Test
  void testWriteEmptyMatrixThrows() {
    Matrix emptyColumns = Matrix.builder().build()
    assertThrows(IllegalArgumentException.class, () -> {
      GsheetsWriter.write(emptyColumns)
    }, "Should throw on matrix with no columns")
  }

  @Test
  void testWriteMatrixWithNoRowsThrows() {
    Matrix noRows = Matrix.builder()
        .data(id: [], name: [])
        .build()

    assertThrows(IllegalArgumentException.class, () -> {
      GsheetsWriter.write(noRows)
    }, "Should throw on matrix with no rows")
  }

  @Test
  void testMethodDelegatesToGsExporter() {
    // This test verifies that the write method exists and has correct signature
    // Actual functionality is tested in GsExporterTest since we delegate to it

    // Verify write method exists with correct parameter types
    def writeMethod = GsheetsWriter.class.getDeclaredMethod(
        "write", Matrix, GoogleCredentials, boolean, boolean
    )
    assertNotNull(writeMethod)
    assertTrue(writeMethod.returnType == String)
  }
}
