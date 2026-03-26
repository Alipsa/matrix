import static org.junit.jupiter.api.Assertions.*

import org.apache.commons.csv.DuplicateHeaderMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.csv.CsvReadOptions
import se.alipsa.matrix.csv.CsvReader
import se.alipsa.matrix.csv.CsvWriteOptions
import se.alipsa.matrix.csv.CsvWriter

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.text.NumberFormat
import java.util.Locale

class CsvTypedOptionsTest {

  @TempDir
  Path tempDir

  @Test
  void readOptionsRoundTripThroughMap() {
    CsvReadOptions options = CsvReadOptions.fromMap([
        delimiter              : ';',
        quote                  : '"',
        escape                 : '\\',
        commentMarker          : '#',
        header                 : ['id', 'name'],
        charset                : 'ISO-8859-1',
        tableName              : 'people',
        types                  : [Integer, String],
        dateTimeFormat         : 'yyyy-MM-dd',
        numberFormat           : NumberFormat.getInstance(Locale.GERMANY),
        trim                   : false,
        ignoreEmptyLines       : false,
        ignoreSurroundingSpaces: false,
        nullString             : 'NULL',
        duplicateHeaderMode    : 'ALLOW_ALL',
        recordSeparator        : '\r\n'
    ])

    Map<String, ?> serialized = options.toMap()
    CsvReadOptions reparsed = CsvReadOptions.fromMap(serialized)

    assertEquals(';' as Character, serialized.delimiter)
    assertEquals(DuplicateHeaderMode.ALLOW_ALL, serialized.duplicateHeaderMode)
    assertEquals(['id', 'name'], reparsed.header)
    assertEquals('people', reparsed.tableName)
    assertEquals(DuplicateHeaderMode.ALLOW_ALL, reparsed.duplicateHeaderMode)
    assertFalse(reparsed.firstRowAsHeader)
    assertFalse(reparsed.trim)
    assertFalse(reparsed.ignoreEmptyLines)
    assertFalse(reparsed.ignoreSurroundingSpaces)
    assertEquals('\r\n', reparsed.recordSeparator)
  }

  @Test
  void writeOptionsRoundTripThroughMap() {
    CsvWriteOptions options = CsvWriteOptions.fromMap([
        delimiter      : ';',
        quote          : '\'',
        escape         : '\\',
        withHeader     : false,
        charset        : 'ISO-8859-1',
        recordSeparator: '\r\n',
        nullString     : 'NULL'
    ])

    Map<String, ?> serialized = options.toMap()
    CsvWriteOptions reparsed = CsvWriteOptions.fromMap(serialized)

    assertEquals(';' as Character, serialized.delimiter)
    assertEquals('\'' as Character, reparsed.quote)
    assertEquals('\\' as Character, reparsed.escape)
    assertFalse(reparsed.withHeader)
    assertEquals(StandardCharsets.ISO_8859_1, reparsed.charset)
    assertEquals('\r\n', reparsed.recordSeparator)
    assertEquals('NULL', reparsed.nullString)
  }

  @Test
  void readOptionsRejectUnknownKeys() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      CsvReadOptions.fromMap([unknownOption: true])
    }

    assertTrue(exception.message.contains("Unknown CsvReadOptions option: 'unknownoption'"))
  }

  @Test
  void writeOptionsRejectUnknownKeys() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      CsvWriteOptions.fromMap([unknownOption: true])
    }

    assertTrue(exception.message.contains("Unknown CsvWriteOptions option: 'unknownoption'"))
  }

  @Test
  void writeOptionsRejectParseOnlyKeys() {
    ['trim', 'ignoreEmptyLines', 'ignoreSurroundingSpaces', 'commentMarker'].each { String optionName ->
      IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
        CsvWriteOptions.fromMap([(optionName): true])
      }

      assertTrue(exception.message.contains("Unknown CsvWriteOptions option: '${optionName.toLowerCase()}'"))
    }
  }

  @Test
  void typedReadOverloadsHandleAllSourcesAndSourceSpecificSemantics() {
    byte[] latin1Bytes = 'id,name\n1,Åsa\n'.getBytes('ISO-8859-1')
    File file = tempDir.resolve('people.csv').toFile()
    Files.write(file.toPath(), latin1Bytes)
    URL url = file.toURI().toURL()

    Matrix fromFile = CsvReader.read(file, new CsvReadOptions().charset('ISO-8859-1'))
    Matrix fromPath = CsvReader.read(file.toPath(), new CsvReadOptions().charset('ISO-8859-1').tableName('from-path'))
    Matrix fromUrl = CsvReader.read(url, new CsvReadOptions().charset('ISO-8859-1'))
    Matrix fromInputStream = CsvReader.read(new ByteArrayInputStream(latin1Bytes), new CsvReadOptions().charset('ISO-8859-1'))
    Matrix fromReader = CsvReader.read(new InputStreamReader(new ByteArrayInputStream(latin1Bytes), StandardCharsets.ISO_8859_1),
        new CsvReadOptions().charset('UTF-8'))
    Matrix fromString = CsvReader.readString('id,name\n1,Åsa\n', new CsvReadOptions().charset('UTF-16'))

    assertEquals(['1', 'Åsa'], fromFile.row(0))
    assertEquals('people', fromFile.matrixName)
    assertEquals('from-path', fromPath.matrixName)
    assertEquals(['1', 'Åsa'], fromUrl.row(0))
    assertEquals('people', fromUrl.matrixName)
    assertEquals('matrix', fromInputStream.matrixName)
    assertEquals(['1', 'Åsa'], fromInputStream.row(0))
    assertEquals('matrix', fromReader.matrixName)
    assertEquals(['1', 'Åsa'], fromReader.row(0), 'Reader-based input should ignore the charset option')
    assertEquals('matrix', fromString.matrixName)
    assertEquals(['1', 'Åsa'], fromString.row(0), 'String input should ignore the charset option')
  }

  @Test
  void typedReadAutoDetectsTsvAndMatchesSpiAndFluentApis() {
    File tsvFile = tempDir.resolve('auto.tsv').toFile()
    tsvFile.text = 'id\tname\n1\tAlice\n2\tBob\n'

    Matrix typed = CsvReader.read(tsvFile, new CsvReadOptions())
    Matrix spi = Matrix.read(tsvFile)
    Matrix fluent = CsvReader.read().tsv().from(tsvFile)
    Matrix typedUrl = CsvReader.read(tsvFile.toURI().toURL(), new CsvReadOptions())

    assertEquals(['id', 'name'], typed.columnNames())
    assertEquals(['2', 'Bob'], typed.row(1))
    assertEquals(typed.rowCount(), spi.rowCount())
    assertEquals(typed[0, 'id'], spi[0, 'id'])
    assertEquals(typed[0, 'name'], spi[0, 'name'])
    assertEquals(typed[1, 'id'], fluent[1, 'id'])
    assertEquals(typed[1, 'name'], typedUrl[1, 'name'])
  }

  @Test
  void typedReadSupportsDuplicateHeaderModeAndStringBoundaryCompatibility() {
    String csvContent = 'id,id\n1,2\n'

    Matrix typed = CsvReader.readString(csvContent, new CsvReadOptions().duplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL))
    Matrix mapBoundary = CsvReader.read([duplicateHeaderMode: 'ALLOW_ALL'], new StringReader(csvContent))

    assertEquals(['id', 'id'], typed.columnNames())
    assertEquals(['id', 'id'], mapBoundary.columnNames())
    assertEquals(['1', '2'], typed.row(0))
  }

  @Test
  void typedWriteOverloadsHandleAllTargetsAndMatchSpiAndFluentApis() {
    Matrix matrix = Matrix.builder()
        .matrixName('typed')
        .columnNames(['id', 'name'])
        .rows([
            ['1', 'Alice'],
            ['2', 'Bob']
        ])
        .types([String, String])
        .build()

    File typedFile = tempDir.resolve('typed.tsv').toFile()
    File spiFile = tempDir.resolve('spi.tsv').toFile()
    File fluentFile = tempDir.resolve('fluent.tsv').toFile()
    Path pathTarget = tempDir.resolve('path.csv')
    StringWriter writer = new StringWriter()

    CsvWriter.write(matrix, typedFile, new CsvWriteOptions())
    matrix.write(spiFile)
    CsvWriter.write(matrix).tsv().to(fluentFile)
    CsvWriter.write(matrix, pathTarget, new CsvWriteOptions().delimiter(';'))
    CsvWriter.write(matrix, writer, new CsvWriteOptions().delimiter(';').withHeader(false))
    String csvString = CsvWriter.writeString(matrix, new CsvWriteOptions().delimiter(';').withHeader(false))
    String writerContent = writer.buffer

    assertEquals(Files.readString(typedFile.toPath()), Files.readString(spiFile.toPath()))
    assertEquals(Files.readString(typedFile.toPath()), Files.readString(fluentFile.toPath()))
    assertTrue(Files.readString(typedFile.toPath()).contains('\t'))
    assertTrue(Files.readString(pathTarget).contains(';'))
    assertFalse(writerContent.contains('id;name'))
    assertEquals(writerContent, csvString)
  }

  @Test
  void typedWriteSupportsEscapeAndNullStringAndMatchesProviderAndFluentApis() {
    Matrix matrix = Matrix.builder()
        .matrixName('special')
        .columnNames(['name', 'note'])
        .rows([
            [null, 'A,B'],
            ['Bob', 'plain']
        ])
        .types([String, String])
        .build()

    CsvWriteOptions typedOptions = new CsvWriteOptions()
        .quote('')
        .escape('\\')
        .nullString('NULL')

    File spiFile = tempDir.resolve('special-spi.csv').toFile()
    String typed = CsvWriter.writeString(matrix, typedOptions)
    String fluent = CsvWriter.write(matrix)
        .quoteCharacter('')
        .escapeCharacter('\\')
        .nullString('NULL')
        .asString()
    matrix.write([
        quote     : '',
        escape    : '\\',
        nullString: 'NULL'
    ], spiFile)
    String spi = Files.readString(spiFile.toPath())

    assertEquals(typed, fluent)
    assertEquals(typed, spi)
    assertTrue(typed.contains('NULL'))
    assertTrue(typed.contains('A\\,B'))

    Matrix roundTripped = CsvReader.read()
        .quoteCharacter('')
        .escapeCharacter('\\')
        .nullString('NULL')
        .fromString(typed)
    assertNull(roundTripped[0, 'name'])
    assertEquals('A,B', roundTripped[0, 'note'])
  }
}
