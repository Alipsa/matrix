import static org.junit.jupiter.api.Assertions.*

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.DuplicateHeaderMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvImporter
import se.alipsa.matrix.csv.CsvReadOptions
import se.alipsa.matrix.csv.CsvReader

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class CsvReaderFixesTest {

  @TempDir
  Path tempDir

  @Test
  void rejectsHeaderWidthMismatchesAcrossEntryPoints() {
    String csv = 'a,b\n1,2,3\n'

    [
        { CsvReader.read().fromString(csv) },
        { CsvReader.readString(csv, new CsvReadOptions()) },
        { CsvReader.readString(csv, CSVFormat.DEFAULT, true) },
        { CsvImporter.importCsvString(csv) }
    ].each { Closure<?> read ->
      IllegalArgumentException exception = assertThrows(IllegalArgumentException, read)
      assertEquals('CSV record 2 has 3 columns; expected 2', exception.message)
    }

    IllegalArgumentException wider = assertThrows(IllegalArgumentException) {
      CsvReader.read().fromString('a,b,c\n1,2\n')
    }
    assertEquals('CSV record 2 has 2 columns; expected 3', wider.message)
  }

  @Test
  void validatesExplicitAndHeaderlessWidths() {
    IllegalArgumentException explicit = assertThrows(IllegalArgumentException) {
      CsvReader.read().header(['a', 'b']).fromString('1,2,3\n')
    }
    assertEquals('CSV record 1 has 3 columns; expected 2', explicit.message)

    IllegalArgumentException headerless = assertThrows(IllegalArgumentException) {
      CsvReader.read().firstRowAsHeader(false).fromString('1,2\n3,4,5\n')
    }
    assertEquals('CSV record 2 has 3 columns; expected 2', headerless.message)

    IllegalArgumentException multiline = assertThrows(IllegalArgumentException) {
      CsvReader.read().fromString('a,b\n"line one\nline two",2,3\n')
    }
    assertEquals('CSV record 2 has 3 columns; expected 2', multiline.message)
  }

  @Test
  void preservesDuplicateHeaderModeBehavior() {
    Matrix allowAll = CsvReader.read()
        .duplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL)
        .fromString('a,a,b\n1,2,3\n')
    assertEquals(['a', 'a', 'b'], allowAll.columnNames())

    Matrix allowEmpty = CsvReader.read()
        .excel()
        .duplicateHeaderMode(DuplicateHeaderMode.ALLOW_EMPTY)
        .fromString('a,,,b\n1,2,3,4\n')
    assertEquals(['a', '', '', 'b'], allowEmpty.columnNames())

    IllegalArgumentException allowEmptyDuplicate = assertThrows(IllegalArgumentException) {
      CsvReader.read().duplicateHeaderMode(DuplicateHeaderMode.ALLOW_EMPTY).fromString('a,a,b\n1,2,3\n')
    }
    assertTrue(allowEmptyDuplicate.message.contains('duplicate name: "a"'))
    IllegalArgumentException disallowedDuplicate = assertThrows(IllegalArgumentException) {
      CsvReader.read().duplicateHeaderMode(DuplicateHeaderMode.DISALLOW).fromString('a,a,b\n1,2,3\n')
    }
    assertTrue(disallowedDuplicate.message.contains('duplicate name: "a"'))
    IllegalArgumentException missingName = assertThrows(IllegalArgumentException) {
      CsvReader.read().duplicateHeaderMode(DuplicateHeaderMode.ALLOW_EMPTY).fromString('a,,b\n1,2,3\n')
    }
    assertTrue(missingName.message.contains('A header name is missing'))

    IllegalArgumentException duplicateWithNullString = assertThrows(IllegalArgumentException) {
      CsvReader.read()
          .nullString('NA')
          .duplicateHeaderMode(DuplicateHeaderMode.ALLOW_EMPTY)
          .fromString('a,a\n1,2\n')
    }
    assertTrue(duplicateWithNullString.message.contains('duplicate name: "a"'))

    Matrix repeatedNullHeaders = CsvReader.read()
        .excel()
        .nullString('NA')
        .duplicateHeaderMode(DuplicateHeaderMode.ALLOW_EMPTY)
        .fromString('NA,NA\n1,2\n')
    assertEquals(['1', '2'], repeatedNullHeaders.columnNames())
    assertTrue(repeatedNullHeaders.isEmpty())
  }

  @Test
  void preservesTrailingNullHeaderColumns() {
    Matrix fluent = CsvReader.read()
        .excel()
        .nullString('NA')
        .duplicateHeaderMode(DuplicateHeaderMode.ALLOW_EMPTY)
        .fromString('a,b,NA\n1,2,3\n')
    CSVFormat format = CSVFormat.Builder.create(CSVFormat.EXCEL)
        .setHeader()
        .setSkipHeaderRecord(true)
        .setNullString('NA')
        .build()
    Matrix deprecated = CsvReader.readString('a,b,NA\n1,2,3\n', format)

    [fluent, deprecated].each { Matrix matrix ->
      assertEquals(['a', 'b', ''], matrix.columnNames())
      assertEquals([['1', '2', '3']], matrix.rows())
    }
  }

  @Test
  void preservesNativeNullStringRecordSemanticsWithInferredHeaders() {
    Matrix quotedNull = CsvReader.read()
        .excel()
        .nullString('NA')
        .fromString('a,b\n"NA",NA\n')
    assertEquals(['NA', null], quotedNull.row(0))

    Matrix emptyField = CsvReader.read()
        .excel()
        .nullString('NA')
        .fromString('a,b,c\n1,,3\n')
    assertEquals(['1', '', '3'], emptyField.row(0))
  }

  @Test
  void ignoresNarrowAllNullRecordBeforeInferredHeader() {
    Matrix matrix = CsvReader.read()
        .excel()
        .nullString('NA')
        .fromString('\na,b\n1,2\n')

    assertEquals(['a', 'b'], matrix.columnNames())
    assertEquals([['1', '2']], matrix.rows())
  }

  @Test
  void ignoresSameWidthAllNullRecordBeforeInferredHeader() {
    Matrix matrix = CsvReader.read()
        .excel()
        .nullString('NA')
        .fromString('\na\n1\n')

    assertEquals(['a'], matrix.columnNames())
    assertEquals([['1']], matrix.rows())
  }

  @Test
  void validatesEmptyStringRecordAsInferredHeader() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      CsvReader.read()
          .ignoreEmptyLines(false)
          .nullString('NA')
          .fromString('\na,b\n1,2\n')
    }

    assertTrue(exception.message.contains('A header name is missing'))

    Matrix quotedEmptyHeader = CsvReader.read()
        .excel()
        .nullString('NA')
        .fromString('""\na\n')
    assertEquals([''], quotedEmptyHeader.columnNames())
    assertEquals([['a']], quotedEmptyHeader.rows())
  }

  @Test
  void inferredNullStringHeaderValidationHonorsIgnoreHeaderCase() {
    CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
        .setHeader()
        .setSkipHeaderRecord(true)
        .setNullString('NA')
        .setIgnoreHeaderCase(true)
        .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
        .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      CsvReader.readString('a,A\n1,2\n', format)
    }
    assertTrue(exception.message.contains('duplicate name: "A"'))
  }

  @Test
  void nullStringDoesNotHideOverWideRecords() {
    [
        { CsvReader.read().nullString('NA').fromString('a,b\n1,2,3,4\n') },
        { CsvReader.read().excel().nullString('NA').fromString('a,b,NA\n1,2,3,4\n') },
        { CsvReader.readString('a,b\n1,2,3,4\n', new CsvReadOptions().nullString('NA')) }
    ].each { Closure<?> read ->
      IllegalArgumentException exception = assertThrows(IllegalArgumentException, read)
      assertTrue(exception.message.startsWith('CSV record 2 has'))
    }

    CSVFormat format = CSVFormat.Builder.create(CSVFormat.EXCEL)
        .setHeader()
        .setSkipHeaderRecord(true)
        .setNullString('NA')
        .build()
    IllegalArgumentException deprecated = assertThrows(IllegalArgumentException) {
      CsvReader.readString('a,b,NA\n1,2,3,4\n', format)
    }
    assertEquals('CSV record 2 has 4 columns; expected 3', deprecated.message)
  }

  @Test
  void normalizesDuplicateHeaderModeNamesAndNulls() {
    ['allow_all', ' AlLoW_AlL '].each { String mode ->
      assertEquals(['a', 'a'], CsvReader.read().duplicateHeaderMode(mode).fromString('a,a\n1,2\n').columnNames())
      assertEquals(DuplicateHeaderMode.ALLOW_ALL, new CsvReadOptions().duplicateHeaderMode(mode).duplicateHeaderMode)
    }

    assertThrows(IllegalArgumentException) { CsvReader.read().duplicateHeaderMode(null) }
    assertThrows(IllegalArgumentException) { new CsvReadOptions().duplicateHeaderMode(null) }
    assertThrows(IllegalArgumentException) { CsvReader.read().duplicateHeaderMode('unknown') }
    assertThrows(IllegalArgumentException) { new CsvReadOptions().duplicateHeaderMode('unknown') }
    Matrix enumNull = CsvReader.read()
        .duplicateHeaderMode((DuplicateHeaderMode) null)
        .fromString('a,b\n1,2\n')
    assertEquals(['a', 'b'], enumNull.columnNames())
  }

  @Test
  void headerNullPreservesFirstRowSetting() {
    Matrix matrix = CsvReader.read()
        .firstRowAsHeader(false)
        .header((List<String>) null)
        .fromString('a,b\n1,2\n')

    assertEquals(['c0', 'c1'], matrix.columnNames())
    assertEquals(2, matrix.rowCount())
  }

  @Test
  void populatedAndHeaderOnlyDeprecatedStreamsUseFallbackName() {
    Matrix populated = CsvReader.read(new ByteArrayInputStream('a,b\n1,2\n'.bytes), CSVFormat.DEFAULT)
    Matrix headerOnly = CsvReader.read(new ByteArrayInputStream('a,b\n'.bytes), CSVFormat.DEFAULT)
    Matrix empty = CsvReader.read(new ByteArrayInputStream(new byte[0]), CSVFormat.DEFAULT)

    assertEquals('matrix', populated.matrixName)
    assertEquals('matrix', headerOnly.matrixName)
    assertEquals('matrix', empty.matrixName)
  }

  @Test
  void stripsMatchingBomsAcrossByteSources() {
    [
        (StandardCharsets.UTF_8)   : [0xEF, 0xBB, 0xBF] as byte[],
        (StandardCharsets.UTF_16LE): [0xFF, 0xFE] as byte[],
        (StandardCharsets.UTF_16BE): [0xFE, 0xFF] as byte[],
        (Charset.forName('UTF-32LE')): [0xFF, 0xFE, 0x00, 0x00] as byte[],
        (Charset.forName('UTF-32BE')): [0x00, 0x00, 0xFE, 0xFF] as byte[]
    ].each { charset, byte[] bom ->
      byte[] content = (bom.toList() + 'a,b\n1,2\n'.getBytes(charset).toList()) as byte[]
      File file = tempDir.resolve("bom-${charset.name()}.csv").toFile()
      Files.write(file.toPath(), content)
      CsvReadOptions options = new CsvReadOptions().charset(charset)

      assertEquals(['a', 'b'], CsvReader.read(file, options).columnNames())
      assertEquals(['a', 'b'], CsvReader.read(file.toPath(), options).columnNames())
      assertEquals(['a', 'b'], CsvReader.read(file.toURI().toURL(), options).columnNames())

      TrackingInputStream stream = new TrackingInputStream(content)
      assertEquals(['a', 'b'], CsvReader.read(stream, options).columnNames())
      assertFalse(stream.closed)

      assertEquals(['a', 'b'], CsvImporter.importCsv(file, CSVFormat.DEFAULT, true, charset).columnNames())
      assertEquals(['a', 'b'], CsvImporter.importCsv(file.toURI().toURL(), CSVFormat.DEFAULT, true, charset).columnNames())
      assertEquals(['a', 'b'],
          CsvImporter.importCsv(new ByteArrayInputStream(content), CSVFormat.DEFAULT, true, charset).columnNames())
    }
  }

  @Test
  void retainsBomForPlainUtf16AndUtf32AndCharacterSources() {
    [
        ['UTF-16LE', 'UTF-16', [0xFF, 0xFE] as byte[]],
        ['UTF-16BE', 'UTF-16', [0xFE, 0xFF] as byte[]],
        ['UTF-32LE', 'UTF-32', [0xFF, 0xFE, 0x00, 0x00] as byte[]],
        ['UTF-32BE', 'UTF-32', [0x00, 0x00, 0xFE, 0xFF] as byte[]]
    ].each { String encoding, String decoder, byte[] bom ->
      byte[] content = (bom.toList() + 'a,b\n1,2\n'.getBytes(encoding).toList()) as byte[]
      Matrix matrix = CsvReader.read(new ByteArrayInputStream(content), new CsvReadOptions().charset(decoder))
      assertEquals(['a', 'b'], matrix.columnNames())
    }

    String withBom = '\uFEFFa,b\n1,2\n'
    assertEquals('\uFEFFa', CsvReader.read().fromString(withBom).columnNames()[0])
    assertEquals('\uFEFFa', CsvReader.read().from(new StringReader(withBom)).columnNames()[0])

    byte[] unmatched = ([0xEF, 0xBB, 0xBF] + 'a,b\n1,2\n'.getBytes(StandardCharsets.ISO_8859_1).toList()) as byte[]
    Matrix unmatchedMatrix = CsvReader.read(new ByteArrayInputStream(unmatched),
        new CsvReadOptions().charset(StandardCharsets.ISO_8859_1))
    assertEquals('ï»¿a', unmatchedMatrix.columnNames()[0])

    byte[] ordinary = 'a,b\n1,2\n'.getBytes(StandardCharsets.UTF_8)
    assertEquals(['a', 'b'], CsvReader.read(new ByteArrayInputStream(ordinary), new CsvReadOptions()).columnNames())
  }

  @Test
  void legacyRecordSeparatorIsAcceptedButInert() {
    ['\n', '\r', '\r\n'].each { String separator ->
      String csv = "a,b${separator}1,2${separator}"
      assertEquals(1, CsvReader.read().recordSeparator('|').fromString(csv).rowCount())
      assertEquals(1, CsvReader.readString(csv, new CsvReadOptions().recordSeparator('|')).rowCount())
    }

    CsvReadOptions options = new CsvReadOptions().recordSeparator('|')
    Map<String, ?> serialized = options.toMap()
    CsvReadOptions reparsed = CsvReadOptions.fromMap(serialized)
    assertFalse(serialized.containsKey('recordSeparator'))
    assertEquals('\n', reparsed.recordSeparator)
  }

  @Test
  void readsOpaqueJarUrlsAndDerivesMatrixName() {
    Path jar = tempDir.resolve('csv-fixtures.jar')
    try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
      output.putNextEntry(new JarEntry('nested/data.csv'))
      output.write('a,b\n1,2\n'.getBytes(StandardCharsets.UTF_8))
      output.closeEntry()
    }
    URL url = new URI("jar:${jar.toUri()}!/nested/data.csv").toURL()

    [
        CsvReader.read(url, new CsvReadOptions()),
        CsvReader.read().from(url),
        CsvReader.read().fromUrl(url.toString()),
        CsvReader.read(url, CSVFormat.DEFAULT)
    ].each { Matrix matrix ->
      assertEquals('data', matrix.matrixName)
      assertEquals([['1', '2']], matrix.rows())
    }
  }

  @Test
  void typedOptionsMustBeNonNull() {
    assertThrows(IllegalArgumentException) {
      CsvReader.readString('a\n1\n', (CsvReadOptions) null)
    }
  }

  @Test
  void deprecatedShortReadRetainsCommonsCsvWhitespaceAndNullRemainsAmbiguous() {
    File file = tempDir.resolve('spaces.csv').toFile()
    file.text = 'a , b\n 1 , 2 \n'

    Matrix legacy = CsvReader.read(file)
    Matrix fluent = CsvReader.read().from(file)

    assertEquals(['a ', ' b'], legacy.columnNames())
    assertEquals([' 1 ', ' 2 '], legacy.row(0))
    assertEquals(['a', 'b'], fluent.columnNames())
    assertEquals(['1', '2'], fluent.row(0))
    assertThrows(GroovyRuntimeException) { CsvReader.read(file, null) }
  }

  private static class TrackingInputStream extends ByteArrayInputStream {
    boolean closed

    TrackingInputStream(byte[] bytes) {
      super(bytes)
    }

    @Override
    void close() throws IOException {
      closed = true
      super.close()
    }
  }
}
