package se.alipsa.matrix.csv

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Files
import java.nio.file.Path

class CsvCompatibilityInternalTest {

  @Test
  void readRecordSeparatorSurfacesAreDeprecatedAndUndiscoverable() {
    assertNotNull(CsvReadOptions.getDeclaredField('recordSeparator').getAnnotation(Deprecated))
    assertNotNull(CsvReadOptions.getDeclaredMethod('getRecordSeparator').getAnnotation(Deprecated))
    assertNotNull(CsvReadOptions.getDeclaredMethod('setRecordSeparator', String).getAnnotation(Deprecated))
    assertNotNull(CsvReadOptions.getDeclaredMethod('recordSeparator', String).getAnnotation(Deprecated))
    assertNotNull(CsvReader.ReadBuilder.getDeclaredMethod('recordSeparator', String).getAnnotation(Deprecated))
    assertNotNull(CsvOption.getDeclaredField('RecordSeparator').getAnnotation(Deprecated))
    assertFalse(CsvReadOptions.descriptors()*.name.contains('recordSeparator'))
  }

  @Test
  void recordSeparatorWarningGuardIsClaimedOnlyOnce() {
    boolean previous = CsvReadOptions.swapRecordSeparatorWarningFlag(false)
    try {
      CsvReadOptions.fromMap([recordSeparator: '|'])
      assertTrue(CsvReadOptions.RECORD_SEPARATOR_WARNING_EMITTED.get())
      CsvReadOptions.fromMap([recordSeparator: ';'])
      assertTrue(CsvReadOptions.RECORD_SEPARATOR_WARNING_EMITTED.get())
    } finally {
      CsvReadOptions.swapRecordSeparatorWarningFlag(previous)
    }
  }

  @Test
  void legacyRecordSeparatorMapKeyRemainsAcceptedAcrossRoutes(@TempDir Path tempDir) {
    String csv = 'a,b\n1,2\n'
    Map<String, ?> options = [recordSeparator: '|']
    File file = tempDir.resolve('input.csv').toFile()
    Files.writeString(file.toPath(), csv)

    assertEquals('|', CsvReadOptions.fromMap(options).recordSeparator)
    assertEquals(1, CsvReader.read(options, new StringReader(csv)).rowCount())
    assertEquals(1, CsvImporter.importCsv(options, new StringReader(csv)).rowCount())
    assertEquals(1, new CsvFormatProvider().read(file, options).rowCount())
  }

  @Test
  void readerNamesAreDerivedFromSharedSourceNameUtility(@TempDir Path tempDir) {
    File file = tempDir.resolve('sales report.csv').toFile()
    Files.writeString(file.toPath(), 'a\n1\n')

    assertEquals('sales report', CsvReader.read(file).matrixName)
    assertEquals('sales report', CsvReader.read(file.toURI().toURL()).matrixName)
  }
}
