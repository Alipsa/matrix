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
    boolean previous = CsvReadOptions.resetRecordSeparatorWarning(false)
    try {
      CsvReadOptions.fromMap([recordSeparator: '|'])
      assertTrue(CsvReadOptions.RECORD_SEPARATOR_WARNING_EMITTED.get())
      CsvReadOptions.fromMap([recordSeparator: ';'])
      assertTrue(CsvReadOptions.RECORD_SEPARATOR_WARNING_EMITTED.get())
    } finally {
      CsvReadOptions.resetRecordSeparatorWarning(previous)
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
  void urlNamesUseDecodedPathWithoutFormDecoding() {
    assertEquals('sales report', CsvReader.tableName(new URI('https://example.test/a/sales%20report.csv?version=1.2#part').toURL()))
    assertEquals('a+b c', CsvReader.tableName(new URI('https://example.test/a+b%20c.csv').toURL()))
    assertEquals('archive.data', CsvReader.tableName(new URI('https://example.test/archive.data.csv').toURL()))
    assertEquals('', CsvReader.tableName(new URI('https://example.test/').toURL()))
    assertEquals('', CsvReader.tableName(new URI('https://example.test/path/').toURL()))
    assertEquals('a b', CsvReader.tableName(new URL('https://example.test/a b.csv?version=1.2#part')))
    assertEquals('a b%20c', CsvReader.tableName(new URL('https://example.test/a b%20c.csv')))
  }
}
