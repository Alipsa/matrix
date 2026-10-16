package test.alipsa.matrix.arff

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.*

import se.alipsa.matrix.arff.ArffReadOptions
import se.alipsa.matrix.arff.ArffTypeDecl
import se.alipsa.matrix.arff.ArffWriteOptions
import se.alipsa.matrix.arff.MatrixArffReader
import se.alipsa.matrix.arff.MatrixArffWriter
import se.alipsa.matrix.core.Matrix

import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat

@SuppressWarnings('ClassSize')
@TestMethodOrder(MethodOrderer.OrderAnnotation)
class MatrixArffTest {

  private static File irisFile
  private static File tempDir

  @BeforeAll
  static void setup() {
    // Get the iris.arff file from test resources
    URL irisUrl = MatrixArffTest.getClassLoader().getResource('iris.arff')
    assertNotNull(irisUrl, 'iris.arff should be in test resources')
    irisFile = new File(irisUrl.toURI())

    // Create temp directory for output files
    tempDir = Files.createTempDirectory('matrix-arff-test').toFile()
  }

  @AfterAll
  static void cleanup() {
    if (tempDir?.exists()) {
      tempDir.listFiles()?.each { it.delete() }
      tempDir.delete()
    }
  }

  @Test @Order(1)
  void readIrisFromFile() {
    Matrix m = MatrixArffReader.read(irisFile)

    assertEquals('iris', m.matrixName)
    assertEquals(150, m.rowCount())
    assertEquals(5, m.columnCount())

    // Check column names
    List<String> expectedColumns = ['sepallength', 'sepalwidth', 'petallength', 'petalwidth', 'class']
    assertEquals(expectedColumns, m.columnNames())

    // Check first row values
    assertEquals(5.1, m[0, 'sepallength'])
    assertEquals(3.5, m[0, 'sepalwidth'])
    assertEquals(1.4, m[0, 'petallength'])
    assertEquals(0.2, m[0, 'petalwidth'])
    assertEquals('Iris-setosa', m[0, 'class'])

    // Check last row values
    assertEquals(5.9, m[149, 'sepallength'])
    assertEquals(3.0, m[149, 'sepalwidth'])
    assertEquals(5.1, m[149, 'petallength'])
    assertEquals(1.8, m[149, 'petalwidth'])
    assertEquals('Iris-virginica', m[149, 'class'])

    // Check types
    assertEquals(BigDecimal, m.type('sepallength'))
    assertEquals(BigDecimal, m.type('sepalwidth'))
    assertEquals(BigDecimal, m.type('petallength'))
    assertEquals(BigDecimal, m.type('petalwidth'))
    assertEquals(String, m.type('class'))
  }

  @Test @Order(2)
  void readIrisFromPath() {
    Matrix m = MatrixArffReader.read(irisFile.toPath())

    assertEquals('iris', m.matrixName)
    assertEquals(150, m.rowCount())
    assertEquals(5, m.columnCount())
  }

  @Test @Order(3)
  void readIrisFromUrl() {
    Matrix m = MatrixArffReader.read(irisFile.toURI().toURL())

    assertEquals('iris', m.matrixName)
    assertEquals(150, m.rowCount())
    assertEquals(5, m.columnCount())
  }

  @Test @Order(4)
  void readIrisFromInputStream() {
    new FileInputStream(irisFile).withStream { InputStream is ->
      Matrix m = MatrixArffReader.read(is, 'test-iris')

      // Note: The name from @RELATION in the file takes precedence over the default name
      assertEquals('iris', m.matrixName)
      assertEquals(150, m.rowCount())
      assertEquals(5, m.columnCount())
    }
  }

  @Test @Order(5)
  void writeAndReadRoundTrip() {
    // Read the original iris file
    Matrix original = MatrixArffReader.read(irisFile)

    // Write to a new file
    File outputFile = new File(tempDir, 'iris_output.arff')
    MatrixArffWriter.write(original, outputFile)

    // Read it back
    Matrix roundTripped = MatrixArffReader.read(outputFile)

    // Verify the data matches
    assertEquals(original.rowCount(), roundTripped.rowCount())
    assertEquals(original.columnCount(), roundTripped.columnCount())
    assertEquals(original.columnNames(), roundTripped.columnNames())

    // Check a few values
    [0, 50, 100, 149].each { int row ->
      original.columnNames().each { String col ->
        Object origVal = original[row, col]
        Object roundVal = roundTripped[row, col]
        if (origVal instanceof BigDecimal && roundVal instanceof BigDecimal) {
          assertEquals(((BigDecimal)origVal).doubleValue(), ((BigDecimal)roundVal).doubleValue(), 0.0001,
              "Mismatch at row $row, col $col")
        } else {
          assertEquals(origVal, roundVal, "Mismatch at row $row, col $col")
        }
      }
    }
  }

  @Test @Order(6)
  void writeToPath() {
    Matrix original = MatrixArffReader.read(irisFile)

    Path outputPath = tempDir.toPath().resolve('iris_path_output.arff')
    MatrixArffWriter.write(original, outputPath)

    assertTrue(Files.exists(outputPath))
    Matrix roundTripped = MatrixArffReader.read(outputPath)
    assertEquals(150, roundTripped.rowCount())
  }

  @Test @Order(7)
  void writeToOutputStream() {
    Matrix original = MatrixArffReader.read(irisFile)

    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    MatrixArffWriter.write(original, baos)

    String arffContent = baos.toString('UTF-8')
    assertTrue(arffContent.contains('@RELATION'))
    assertTrue(arffContent.contains('@ATTRIBUTE'))
    assertTrue(arffContent.contains('@DATA'))
    assertTrue(arffContent.contains('sepallength'))
    assertTrue(arffContent.contains('5.1'))
  }

  @Test @Order(8)
  void writeToWriter() {
    Matrix original = MatrixArffReader.read(irisFile)

    StringWriter sw = new StringWriter()
    MatrixArffWriter.write(original, sw)

    String arffContent = sw as String
    assertTrue(arffContent.contains('@RELATION'))
    assertTrue(arffContent.contains('@ATTRIBUTE'))
    assertTrue(arffContent.contains('@DATA'))
  }

  @Test @Order(9)
  void testNumericTypes() {
    // Create a matrix with numeric columns
    Matrix m = Matrix.builder('numeric_test')
        .columnNames('integers', 'decimals', 'longs')
        .columns(
            [1, 2, 3, 4, 5],
            [1.1, 2.2, 3.3, 4.4, 5.5],
            [100L, 200L, 300L, 400L, 500L]
        )
        .types([Integer, BigDecimal, Long])
        .build()

    File outputFile = new File(tempDir, 'numeric_test.arff')
    MatrixArffWriter.write(m, outputFile)

    Matrix roundTripped = MatrixArffReader.read(outputFile)
    assertEquals(5, roundTripped.rowCount())
    assertEquals(3, roundTripped.columnCount())
  }

  @Test @Order(10)
  void testMissingValues() {
    // Create ARFF content with missing values
    String arffContent = '''
@RELATION missing_test

@ATTRIBUTE name STRING
@ATTRIBUTE value NUMERIC

@DATA
'Alice',10.5
'Bob',?
?,20.5
'''.trim()

    ByteArrayInputStream bais = new ByteArrayInputStream(arffContent.getBytes('UTF-8'))
    Matrix m = MatrixArffReader.read(bais, 'missing_test')

    assertEquals(3, m.rowCount())
    assertEquals('Alice', m[0, 'name'])
    assertEquals(10.5, m[0, 'value'])
    assertEquals('Bob', m[1, 'name'])
    assertNull(m[1, 'value'])
    assertNull(m[2, 'name'])
    assertEquals(20.5, m[2, 'value'])
  }

  @Test @Order(11)
  void testNominalValues() {
    // Create a matrix and specify nominal mappings when writing
    Matrix m = Matrix.builder('nominal_test')
        .columnNames('category', 'value')
        .columns(
            ['A', 'B', 'A', 'C', 'B'],
            [1.0, 2.0, 3.0, 4.0, 5.0]
        )
        .types([String, BigDecimal])
        .build()

    File outputFile = new File(tempDir, 'nominal_test.arff')
    Map<String, List<String>> nominalMappings = ['category': ['A', 'B', 'C']]
    MatrixArffWriter.write(m, outputFile, nominalMappings)

    // Verify the output contains nominal type definition
    String content = outputFile.text
    assertTrue(content.contains('{A,B,C}'))

    // Read it back
    Matrix roundTripped = MatrixArffReader.read(outputFile)
    assertEquals(5, roundTripped.rowCount())
    assertEquals('A', roundTripped[0, 'category'])
    assertEquals('C', roundTripped[3, 'category'])
  }

  @Test @Order(12)
  void testQuotedValues() {
    // Create ARFF content with quoted values
    String arffContent = '''
@RELATION 'quoted test'

@ATTRIBUTE 'column with space' STRING
@ATTRIBUTE normal NUMERIC

@DATA
'value with, comma',10.5
'simple',20.5
'''.trim()

    ByteArrayInputStream bais = new ByteArrayInputStream(arffContent.getBytes('UTF-8'))
    Matrix m = MatrixArffReader.read(bais, 'quoted_test')

    assertEquals(2, m.rowCount())
    assertEquals('value with, comma', m[0, 'column with space'])
    assertEquals('simple', m[1, 'column with space'])
  }

  @Test @Order(13)
  void testIntegerType() {
    String arffContent = '''
@RELATION integer_test

@ATTRIBUTE id INTEGER
@ATTRIBUTE count INTEGER

@DATA
1,100
2,200
3,300
'''.trim()

    ByteArrayInputStream bais = new ByteArrayInputStream(arffContent.getBytes('UTF-8'))
    Matrix m = MatrixArffReader.read(bais)

    assertEquals(3, m.rowCount())
    assertEquals(Integer, m.type('id'))
    assertEquals(Integer, m.type('count'))
    assertEquals(1, m[0, 'id'])
    assertEquals(200, m[1, 'count'])
  }

  @Test @Order(14)
  void testCommentsAreIgnored() {
    String arffContent = '''
% This is a comment
% Another comment line

@RELATION comment_test

% Comment between declarations
@ATTRIBUTE value NUMERIC

% Comment before data
@DATA
% This comment should be ignored
1.0
2.0
% Another ignored comment
3.0
'''.trim()

    ByteArrayInputStream bais = new ByteArrayInputStream(arffContent.getBytes('UTF-8'))
    Matrix m = MatrixArffReader.read(bais)

    assertEquals(3, m.rowCount())
    assertEquals(1.0, m[0, 'value'])
    assertEquals(2.0, m[1, 'value'])
    assertEquals(3.0, m[2, 'value'])
  }

  @Test @Order(15)
  void testReadFromFilePathString() {
    Matrix m = MatrixArffReader.readFile(irisFile.absolutePath)
    assertEquals('iris', m.matrixName)
    assertEquals(150, m.rowCount())
  }

  @Test @Order(16)
  void testReadFromUrlString() {
    Matrix m = MatrixArffReader.readUrl(irisFile.toURI().toString())
    assertEquals('iris', m.matrixName)
    assertEquals(150, m.rowCount())
  }

  @Test @Order(17)
  void testReadFromStringAndReader() {
    String arffContent = '''
@RELATION string_test

@ATTRIBUTE name STRING
@ATTRIBUTE value NUMERIC

@DATA
'Alice',10.5
'''.trim()

    Matrix fromString = MatrixArffReader.readString(arffContent)
    assertEquals('string_test', fromString.matrixName)
    assertEquals(10.5, fromString[0, 'value'])

    Matrix fromReader = MatrixArffReader.read(new StringReader(arffContent))
    assertEquals('string_test', fromReader.matrixName)
    assertEquals('Alice', fromReader[0, 'name'])
  }

  @Test @Order(18)
  void testEscapedQuotesAndWhitespace() {
    String arffContent = '''
@RELATION 'quote test'

@ATTRIBUTE 'col with \\'quote' STRING
@ATTRIBUTE value STRING

@DATA
'  spaced value  ','O\\'Reilly'
'back\\\\slash','?'
'''.trim()

    Matrix m = MatrixArffReader.readString(arffContent)
    assertEquals('col with \'quote', m.columnNames()[0])
    assertEquals('  spaced value  ', m[0, 'col with \'quote'])
    assertEquals('O\'Reilly', m[0, 'value'])
    assertEquals('back\\slash', m[1, 'col with \'quote'])
    assertEquals('?', m[1, 'value'])
  }

  @Test @Order(19)
  void testLongAndBigIntegerWrittenAsNumeric() {
    Matrix m = Matrix.builder('long_test')
        .columnNames('longs', 'bigints')
        .columns(
            [1L, 2L],
            [123G, 456G]
        )
        .types([Long, BigInteger])
        .build()

    File outputFile = new File(tempDir, 'long_test.arff')
    MatrixArffWriter.write(m, outputFile)

    Matrix roundTripped = MatrixArffReader.read(outputFile)
    assertEquals(BigDecimal, roundTripped.type('longs'))
    assertEquals(BigDecimal, roundTripped.type('bigints'))
    assertEquals(1 as BigDecimal, roundTripped[0, 'longs'])
    assertEquals(456 as BigDecimal, roundTripped[1, 'bigints'])
  }

  @Test @Order(20)
  void testNominalValuesWithBraces() {
    String arffContent = '''
@RELATION brace_nominal

@ATTRIBUTE label {'value with }','{leading brace}','plain'}

@DATA
'value with }'
'{leading brace}'
plain
'''.trim()

    Matrix m = MatrixArffReader.readString(arffContent)
    assertEquals(3, m.rowCount())
    assertEquals('value with }', m[0, 'label'])
    assertEquals('{leading brace}', m[1, 'label'])
    assertEquals('plain', m[2, 'label'])
  }

  @Test @Order(21)
  void testWhitespaceBeforeQuotedValue() {
    String arffContent = '''
@RELATION whitespace_test

@ATTRIBUTE id INTEGER
@ATTRIBUTE note STRING

@DATA
1,  'text'
'''.trim()

    Matrix m = MatrixArffReader.readString(arffContent)
    assertEquals('text', m[0, 'note'])
  }

  @Test @Order(22)
  void testWriteString() {
    Matrix m = Matrix.builder('string_test')
        .columnNames('id', 'name', 'value')
        .columns(
            [1, 2, 3],
            ['Alice', 'Bob', 'Charlie'],
            [10.5, 20.5, 30.5]
        )
        .types([Integer, String, BigDecimal])
        .build()

    String arffContent = MatrixArffWriter.writeString(m)

    assertNotNull(arffContent)
    assertTrue(arffContent.contains('@RELATION string_test'))
    assertTrue(arffContent.contains('@ATTRIBUTE id INTEGER'))
    assertTrue(arffContent.contains('@ATTRIBUTE name'))
    assertTrue(arffContent.contains('@ATTRIBUTE value NUMERIC'))
    assertTrue(arffContent.contains('@DATA'))
    assertTrue(arffContent.contains('Alice'))
    assertTrue(arffContent.contains('10.5'))
  }

  @Test @Order(23)
  void testWriteStringWithNominalMappings() {
    Matrix m = Matrix.builder('nominal_string_test')
        .columnNames('category', 'value')
        .columns(
            ['A', 'B', 'A', 'C'],
            [1, 2, 3, 4]
        )
        .types([String, Integer])
        .build()

    Map<String, List<String>> nominalMappings = ['category': ['A', 'B', 'C', 'D']]
    String arffContent = MatrixArffWriter.writeString(m, nominalMappings)

    assertNotNull(arffContent)
    assertTrue(arffContent.contains('@RELATION nominal_string_test'))
    assertTrue(arffContent.contains('{A,B,C,D}'))
    assertTrue(arffContent.contains('@DATA'))
    assertTrue(arffContent.contains('A,1'))
    assertTrue(arffContent.contains('C,4'))
  }

  @Test @Order(24)
  void testWriteStringRoundTrip() {
    Matrix original = Matrix.builder('roundtrip_test')
        .columnNames('id', 'name', 'score')
        .columns(
            [1, 2, 3],
            ['Alice', 'Bob', 'Charlie'],
            [95.5, 87.3, 92.1]
        )
        .types([Integer, String, BigDecimal])
        .build()

    String arffString = MatrixArffWriter.writeString(original)
    Matrix roundTripped = MatrixArffReader.readString(arffString)

    assertEquals(original.rowCount(), roundTripped.rowCount())
    assertEquals(original.columnCount(), roundTripped.columnCount())
    assertEquals(original.columnNames(), roundTripped.columnNames())

    // Check values
    assertEquals(1, roundTripped[0, 'id'])
    assertEquals('Alice', roundTripped[0, 'name'])
    assertEquals(95.5, roundTripped[0, 'score'])
    assertEquals(3, roundTripped[2, 'id'])
    assertEquals('Charlie', roundTripped[2, 'name'])
  }

  @Test @Order(25)
  void testWriteStringProducesSameOutputAsWriter() {
    Matrix m = Matrix.builder('compare_test')
        .columnNames('x', 'y')
        .columns(
            [1, 2, 3],
            [10, 20, 30]
        )
        .types([Integer, Integer])
        .build()

    // Get output from writeString()
    String fromWriteString = MatrixArffWriter.writeString(m)

    // Get output from write(Writer)
    StringWriter sw = new StringWriter()
    MatrixArffWriter.write(m, sw)
    String fromWriter = sw as String

    // They should be identical
    assertEquals(fromWriter, fromWriteString)
  }

  @Test @Order(26)
  void testReadSparseRows() {
    String arffContent = '''
@RELATION sparse_test

@ATTRIBUTE score NUMERIC
@ATTRIBUTE note STRING
@ATTRIBUTE status {yes,no}
@ATTRIBUTE count INTEGER

@DATA
{0 1.5,2 yes}
{1 'hello',3 4}
{}
'''.trim()

    Matrix m = MatrixArffReader.readString(arffContent)

    assertEquals('sparse_test', m.matrixName)
    assertEquals(3, m.rowCount())
    assertEquals(4, m.columnCount())

    assertEquals(1.5, m[0, 'score'])
    assertEquals('hello', m[0, 'note'])
    assertEquals('yes', m[0, 'status'])
    assertEquals(0, m[0, 'count'])

    assertEquals(BigDecimal.ZERO, m[1, 'score'])
    assertEquals('hello', m[1, 'note'])
    assertEquals('yes', m[1, 'status'])
    assertEquals(4, m[1, 'count'])

    assertEquals(BigDecimal.ZERO, m[2, 'score'])
    assertEquals('hello', m[2, 'note'])
    assertEquals('yes', m[2, 'status'])
    assertEquals(0, m[2, 'count'])
  }

  @Test @Order(27)
  void testReadSparseRowsWithQuotedStringsAndMissingValues() {
    String arffContent = '''
@RELATION sparse_quotes

@ATTRIBUTE note STRING
@ATTRIBUTE category {'value with comma',plain}
@ATTRIBUTE amount NUMERIC

@DATA
{0 'text, with comma',1 'value with comma'}
{0 'O\\'Reilly',2 ?}
'''.trim()

    Matrix m = MatrixArffReader.readString(arffContent)

    assertEquals(2, m.rowCount())
    assertEquals('text, with comma', m[0, 'note'])
    assertEquals('value with comma', m[0, 'category'])
    assertEquals(BigDecimal.ZERO, m[0, 'amount'])

    assertEquals('O\'Reilly', m[1, 'note'])
    assertEquals('value with comma', m[1, 'category'])
    assertNull(m[1, 'amount'])
  }

  @Test @Order(28)
  void testSparseRowsRejectDuplicateIndices() {
    String arffContent = '''
@RELATION sparse_duplicate

@ATTRIBUTE value NUMERIC
@ATTRIBUTE name STRING

@DATA
{0 1.0,0 2.0}
'''.trim()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString(arffContent)
    }

    assertTrue(exception.message.contains('Duplicate sparse ARFF attribute index 0'))
  }

  @Test @Order(29)
  void testSparseRowsRejectOutOfRangeIndices() {
    String arffContent = '''
@RELATION sparse_bounds

@ATTRIBUTE value NUMERIC
@ATTRIBUTE name STRING

@DATA
{2 'oops'}
'''.trim()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString(arffContent)
    }

    assertTrue(exception.message.contains('out of bounds'))
  }

  @Test @Order(30)
  void testSparseRowsRejectNegativeIndices() {
    String arffContent = '''
@RELATION sparse_negative

@ATTRIBUTE value NUMERIC
@ATTRIBUTE name STRING

@DATA
{-1 'oops'}
'''.trim()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString(arffContent)
    }

    assertTrue(exception.message.contains('out of bounds'))
  }

  @Test @Order(31)
  void testMixedDenseAndSparseRows() {
    String arffContent = '''
@RELATION mixed_rows

@ATTRIBUTE id INTEGER
@ATTRIBUTE note STRING
@ATTRIBUTE score NUMERIC

@DATA
1,'dense',10.5
{0 2,2 20.5}
3,'dense again',?
{1 'sparse only'}
'''.trim()

    Matrix m = MatrixArffReader.readString(arffContent)

    assertEquals(4, m.rowCount())
    assertEquals(1, m[0, 'id'])
    assertEquals('dense', m[0, 'note'])
    assertEquals(10.5, m[0, 'score'])

    assertEquals(2, m[1, 'id'])
    assertEquals('dense', m[1, 'note'])
    assertEquals(20.5, m[1, 'score'])

    assertEquals(3, m[2, 'id'])
    assertEquals('dense again', m[2, 'note'])
    assertNull(m[2, 'score'])

    assertEquals(0, m[3, 'id'])
    assertEquals('sparse only', m[3, 'note'])
    assertEquals(BigDecimal.ZERO, m[3, 'score'])
  }

  @Test @Order(32)
  void testWriteOptionsDisableNominalInference() {
    Matrix m = Matrix.builder('schema_options')
        .columnNames('category', 'value')
        .columns(
            ['A', 'B', 'A', 'C'],
            [1, 2, 3, 4]
        )
        .types([String, Integer])
        .build()

    String arffContent = MatrixArffWriter.writeString(m, new ArffWriteOptions().inferNominals(false))

    assertTrue(arffContent.contains('@ATTRIBUTE category STRING'))
  }

  @Test @Order(33)
  void testWriteOptionsCustomNominalThreshold() {
    Matrix m = Matrix.builder('nominal_threshold')
        .columnNames('category', 'value')
        .columns(
            ['A', 'B', 'A', 'B'],
            [1, 2, 3, 4]
        )
        .types([String, Integer])
        .build()

    String arffContent = MatrixArffWriter.writeString(m, new ArffWriteOptions().nominalThreshold(1))

    assertTrue(arffContent.contains('@ATTRIBUTE category STRING'))
  }

  @Test @Order(34)
  void testWriteOptionsForceStringOutput() {
    Matrix m = Matrix.builder('force_string')
        .columnNames('category', 'value')
        .columns(
            ['A', 'B', 'A', 'C'],
            [1, 2, 3, 4]
        )
        .types([String, Integer])
        .build()

    String arffContent = MatrixArffWriter.writeString(
        m,
        new ArffWriteOptions().attributeTypesByColumn([category: ArffTypeDecl.STRING])
    )

    assertTrue(arffContent.contains('@ATTRIBUTE category STRING'))
  }

  @Test @Order(35)
  void testWriteOptionsForceNominalOutput() {
    List<String> labels = (1..12).collect { int i -> "label$i" }
    Matrix m = Matrix.builder('force_nominal')
        .columns(label: labels)
        .types([String])
        .build()

    String arffContent = MatrixArffWriter.writeString(
        m,
        new ArffWriteOptions().nominalColumns(['label'])
    )

    assertTrue(arffContent.contains('@ATTRIBUTE label {'))
    assertTrue(arffContent.contains('label1'))
    assertTrue(arffContent.contains('label12'))
  }

  @Test @Order(36)
  void testWriteOptionsPreserveExplicitNominalOrdering() {
    Matrix m = Matrix.builder('nominal_order')
        .columns(category: ['A', 'B', 'C'])
        .types([String])
        .build()

    String arffContent = MatrixArffWriter.writeString(
        m,
        new ArffWriteOptions().nominalMappings([category: ['B', 'A', 'C']])
    )

    assertTrue(arffContent.contains('@ATTRIBUTE category {B,A,C}'))
  }

  @Test @Order(37)
  void testNominalSentinelValuesRoundTripAsLiterals() {
    Matrix m = Matrix.builder('nominal_literals')
        .columns(category: ['', '?', '%comment', 'normal'])
        .types([String])
        .build()

    ArffWriteOptions options = new ArffWriteOptions().nominalMappings([
        category: ['', '?', '%comment', 'normal']
    ])
    String arffContent = MatrixArffWriter.writeString(m, options)
    Matrix result = MatrixArffReader.readString(arffContent)

    assertTrue(arffContent.contains("@ATTRIBUTE category {'','?','\\%comment',normal}"))
    assertTrue(arffContent.contains("''"))
    assertTrue(arffContent.contains("'?'"))
    assertTrue(arffContent.contains("'\\%comment'"))
    assertEquals('', result[0, 'category'])
    assertEquals('?', result[1, 'category'])
    assertEquals('%comment', result[2, 'category'])
    assertEquals('normal', result[3, 'category'])
  }

  @Test @Order(38)
  void testWriteOptionsGlobalDateFormat() {
    SimpleDateFormat inputFormat = new SimpleDateFormat('yyyy-MM-dd', Locale.US)
    inputFormat.timeZone = TimeZone.getTimeZone('UTC')
    Matrix m = Matrix.builder('date_format')
        .columns(created: [inputFormat.parse('2026-03-17'), inputFormat.parse('2026-03-18')])
        .types([Date])
        .build()

    String arffContent = MatrixArffWriter.writeString(
        m,
        new ArffWriteOptions().dateFormat('yyyy-MM-dd')
    )

    assertTrue(arffContent.contains("@ATTRIBUTE created DATE 'yyyy-MM-dd'"))
    assertTrue(arffContent.contains("'2026-03-17'"))
  }

  @Test @Order(39)
  void testWriteOptionsPerColumnDateFormats() {
    SimpleDateFormat inputFormat = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss', Locale.US)
    inputFormat.timeZone = TimeZone.getTimeZone('UTC')
    Date created = inputFormat.parse('2026-03-17 11:12:13')
    Date updated = inputFormat.parse('2026-03-18 14:15:16')
    Matrix m = Matrix.builder('date_format_columns')
        .columnNames('created', 'updated')
        .columns(
            [created],
            [updated]
        )
        .types([Date, Date])
        .build()

    String arffContent = MatrixArffWriter.writeString(
        m,
        new ArffWriteOptions()
            .dateFormat('yyyy-MM-dd')
            .dateFormatsByColumn([updated: 'yyyy/MM/dd HH:mm'])
    )

    assertTrue(arffContent.contains("@ATTRIBUTE created DATE 'yyyy-MM-dd'"))
    assertTrue(arffContent.contains("@ATTRIBUTE updated DATE 'yyyy/MM/dd HH:mm'"))
    assertTrue(arffContent.contains("'2026-03-17'"))
    assertTrue(arffContent.contains("'2026/03/18 14:15'"))
  }

  @Test @Order(40)
  void testInvalidDateIsRejected() {
    String arffContent = '''
@RELATION invalid_date

@ATTRIBUTE created DATE 'yyyy-MM-dd'

@DATA
'2026-02-31'
'''.trim()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString(arffContent)
    }

    assertTrue(exception.message.contains("Invalid DATE value '2026-02-31'"))
    assertTrue(exception.message.contains('line 6'))
  }

  @Test @Order(41)
  void testDateOutputUsesUtc() {
    Matrix m = Matrix.builder('utc_dates')
        .columns(created: [Instant.parse('2026-03-18T14:15:16Z')])
        .types([Instant])
        .build()

    String arffContent = MatrixArffWriter.writeString(
        m,
        new ArffWriteOptions().dateFormat("yyyy-MM-dd'T'HH:mm:ss")
    )

    assertTrue(arffContent.contains("'2026-03-18T14:15:16'"))
  }

  @Test @Order(42)
  void testWriteOptionsRejectConflictingTypeConfiguration() {
    Matrix m = Matrix.builder('conflict')
        .columns(category: ['A', 'B'])
        .types([String])
        .build()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      MatrixArffWriter.writeString(
          m,
          new ArffWriteOptions()
              .nominalMappings([category: ['B', 'A']])
              .attributeTypesByColumn([category: ArffTypeDecl.STRING])
      )
    }

    assertTrue(exception.message.contains('nominal configuration'))
  }

  @Test @Order(43)
  void testTypedReadOverloadsUseArffReadOptions() {
    String arffContent = '''
@ATTRIBUTE id INTEGER
@ATTRIBUTE label STRING
@DATA
1,'alpha'
2,'beta'
'''.trim()
    File file = new File(tempDir, 'typed_read.arff')
    file.text = arffContent
    ArffReadOptions options = new ArffReadOptions().fallbackMatrixName('typed_fallback')

    Matrix fromFile = MatrixArffReader.read(file, options)
    Matrix fromPath = MatrixArffReader.read(file.toPath(), options)
    Matrix fromUrl = MatrixArffReader.read(file.toURI().toURL(), options)
    Matrix fromString = MatrixArffReader.readString(arffContent, options)
    Matrix fromReader = MatrixArffReader.read(new StringReader(arffContent), options)

    new FileInputStream(file).withCloseable { InputStream is ->
      Matrix fromInputStream = MatrixArffReader.read(is, options)
      assertEquals('typed_fallback', fromInputStream.matrixName)
      assertEquals('beta', fromInputStream[1, 'label'])
    }

    [fromFile, fromPath, fromUrl, fromString, fromReader].each { Matrix matrix ->
      assertEquals('typed_fallback', matrix.matrixName)
      assertEquals(2, matrix.rowCount())
      assertEquals(1, matrix[0, 'id'])
      assertEquals('beta', matrix[1, 'label'])
    }
  }

  @Test @Order(44)
  void testTypedWriteOverloadsProduceSameOutput() {
    SimpleDateFormat inputFormat = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss', Locale.US)
    inputFormat.timeZone = TimeZone.getTimeZone('UTC')
    Matrix m = Matrix.builder('typed_write')
        .columnNames('created', 'category', 'value')
        .columns(
            [inputFormat.parse('2026-03-18 09:10:11')],
            ['A'],
            [1]
        )
        .types([Date, String, Integer])
        .build()
    ArffWriteOptions options = new ArffWriteOptions()
        .inferNominals(false)
        .attributeTypesByColumn([
            created : ArffTypeDecl.DATE,
            category: ArffTypeDecl.STRING
        ])
        .dateFormatsByColumn([created: 'yyyy-MM-dd'])
    String expected = MatrixArffWriter.writeString(m, options)

    File file = new File(tempDir, 'typed_write_file.arff')
    MatrixArffWriter.write(m, file, options)
    assertEquals(expected, file.getText('UTF-8'))

    Path path = tempDir.toPath().resolve('typed_write_path.arff')
    MatrixArffWriter.write(m, path, options)
    assertEquals(expected, path.toFile().getText('UTF-8'))

    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    MatrixArffWriter.write(m, baos, options)
    assertEquals(expected, baos.toString('UTF-8'))

    StringWriter sw = new StringWriter()
    MatrixArffWriter.write(m, sw, options)
    assertEquals(expected, sw as String)
  }

  @Test @Order(45)
  void testUnknownAttributeTypeDefaultsToString() {
    String arffContent = '''
@RELATION unknown_type

@ATTRIBUTE note CUSTOMTYPE
@DATA
hello
'''.trim()

    Matrix m = MatrixArffReader.readString(arffContent)

    assertEquals(String, m.type('note'))
    assertEquals('hello', m[0, 'note'])
  }

  @Test @Order(46)
  void testStrictUnknownAttributeTypeFails() {
    String arffContent = '''
@RELATION unknown_type_strict

@ATTRIBUTE note CUSTOMTYPE
@DATA
hello
'''.trim()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString(arffContent, 'unknown_type_strict', new ArffReadOptions().strict(true))
    }

    assertTrue(exception.message.contains('Unknown @ATTRIBUTE type \'CUSTOMTYPE\''))
    assertTrue(exception.message.contains('line 3'))
  }

  @Test @Order(47)
  void testDenseRowsStayLenientByDefault() {
    String arffContent = '''
@RELATION lenient_rows

@ATTRIBUTE first STRING
@ATTRIBUTE second NUMERIC

@DATA
'only first'
'first',2.5,99
'''.trim()

    Matrix m = MatrixArffReader.readString(arffContent)

    assertEquals(2, m.rowCount())
    assertEquals('only first', m[0, 'first'])
    assertNull(m[0, 'second'])
    assertEquals('first', m[1, 'first'])
    assertEquals(2.5, m[1, 'second'])
  }

  @Test @Order(48)
  void testStrictDenseRowsRejectMissingValues() {
    String arffContent = '''
@RELATION strict_missing_row

@ATTRIBUTE first STRING
@ATTRIBUTE second NUMERIC

@DATA
'only first'
'''.trim()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString(arffContent, 'strict_missing_row', new ArffReadOptions().strict(true))
    }

    assertTrue(exception.message.contains('Row length mismatch'))
    assertTrue(exception.message.contains('expected 2 values but found 1'))
    assertTrue(exception.message.contains('line 7'))
  }

  @Test @Order(49)
  void testStrictDenseRowsRejectExtraValues() {
    String arffContent = '''
@RELATION strict_extra_row

@ATTRIBUTE first STRING
@ATTRIBUTE second NUMERIC

@DATA
'first',2.5,99
'''.trim()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString(arffContent, 'strict_extra_row', new ArffReadOptions().strict(true))
    }

    assertTrue(exception.message.contains('Row length mismatch'))
    assertTrue(exception.message.contains('expected 2 values but found 3'))
    assertTrue(exception.message.contains('line 7'))
  }

  @Test @Order(50)
  void testMalformedDenseRowReportsLineContext() {
    String arffContent = '''
@RELATION malformed_dense

@ATTRIBUTE note STRING

@DATA
'unterminated
'''.trim()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString(arffContent)
    }

    assertTrue(exception.message.contains('Unterminated quoted data row'))
    assertTrue(exception.message.contains('line 6'))
    assertTrue(exception.message.contains("'unterminated"))
  }

  @Test @Order(51)
  void testMalformedSparseRowReportsLineContext() {
    String arffContent = '''
@RELATION malformed_sparse

@ATTRIBUTE note STRING

@DATA
{0 'unterminated}
'''.trim()

    IllegalArgumentException exception = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString(arffContent)
    }

    assertTrue(exception.message.contains('Unterminated quoted sparse value'))
    assertTrue(exception.message.contains('line 6'))
    assertTrue(exception.message.contains("{0 'unterminated}"))
  }


  @Test
  void writerRejectsInvalidCellsBeforeOpeningFile() {
    def target = new File(tempDir, 'preserved.arff')
    [[ArffTypeDecl.NUMERIC, '1,5'], [ArffTypeDecl.INTEGER, 1.5], [ArffTypeDecl.DATE, 'abc'],
     [ArffTypeDecl.NUMERIC, '١٢.5'], [ArffTypeDecl.INTEGER, '?']].each { type, value ->
      target.text = 'original'
      def m = Matrix.builder('m').data(c: [value]).types(value.class).build()
      def error = assertThrows(IllegalArgumentException) {
        MatrixArffWriter.write(m, target, new ArffWriteOptions().attributeTypesByColumn([c: type]))
      }
      assertTrue(error.message.contains("Column 'c'"))
      assertTrue(error.message.contains('row 0'))
      assertEquals('original', target.text)
    }
  }

  @Test
  void headerValidationReportsSourceLocations() {
    ['@relationship r', '@DATAX', '@atribute a numeric', '@relation r\n@attribute a numeric',
     '@relation r\n@attribute a numeric\n1'].each { text ->
      def error = assertThrows(IllegalArgumentException) { MatrixArffReader.readString(text) }
      assertTrue(error.message.contains('line'))
    }
  }

  @Test
  void nominalDeclarationsUseWekaSeparatorsAndRejectDuplicates() {
    ['c{a,b,}', 'c {a b}'].each { declaration ->
      def m = MatrixArffReader.readString("@relation r\n@attribute $declaration\n@data\na\nb")
      assertEquals(['a', 'b'], m.column('c'))
    }
    def error = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString('@relation r\n@attribute c {a,a}\n@data\na')
    }
    assertTrue(error.message.contains('line 2'))
    assertTrue(error.message.contains('@attribute c {a,a}'))
  }

  @Test
  void writeErrorsReachTheCaller() {
    def m = Matrix.builder('m').data(c: [1]).types(Integer).build()
    Writer broken = new Writer() {
      @Override
      void write(char[] chars, int offset, int length) { throw new IOException('disk full') }
      @Override
      void flush() { }
      @Override
      void close() { }
    }
    def error = assertThrows(IOException) { MatrixArffWriter.write(m, broken) }
    assertEquals('disk full', error.message)
  }

  @Test
  @SuppressWarnings('LocaleSetDefault')
  void readsHeaderUnderTurkishLocale() {
    Locale saved = Locale.default
    try {
      Locale.default = Locale.forLanguageTag('tr-TR')
      def m = MatrixArffReader.readString('@relation r\n@attribute a numeric\n@attribute b string\n@attribute c integer\n@data\n1,x,2')
      assertEquals(['a', 'b', 'c'], m.columnNames())
      assertEquals([BigDecimal, String, Integer], m.types())
      assertEquals(1, m.rowCount())
    } finally {
      Locale.default = saved
    }
  }

  @Test
  void bomEmptyInputAndQuotedKeywordBoundaries() {
    def text = "\uFEFF@relation'r'\n@attribute'a b' numeric\n@data\n1"
    assertEquals('r', MatrixArffReader.readString(text).matrixName)
    assertEquals(['a b'], MatrixArffReader.read(new ByteArrayInputStream(text.getBytes('UTF-8'))).columnNames())
    ['', '% comment\n\n'].each { assertEquals(0, MatrixArffReader.readString(it).rowCount()) }
  }

  @Test
  void integerTypeCompatibilityAndSparseDefaults() {
    def text = '@relation r\n@attribute c integer\n@data\n3000000000\n{}'
    def error = assertThrows(IllegalArgumentException) { MatrixArffReader.readString(text) }
    assertTrue(error.message.contains('integerType(Long)'))
    def m = MatrixArffReader.readString(text, new ArffReadOptions().integerType(Long))
    assertEquals([3000000000L, 0L], m.column('c'))
    assertEquals(Long, m[1, 'c'].class)
    m = MatrixArffReader.readString(text.replace('3000000000', '1.5'), new ArffReadOptions().integerType(BigDecimal))
    assertEquals(1.5, m[0, 'c'])
    assertEquals(0.0, m[1, 'c'])
    assertEquals(BigDecimal, m[1, 'c'].class)
    assertThrows(IllegalArgumentException) { new ArffReadOptions().integerType(Double) }
    assertEquals(Integer, new ArffReadOptions().integerType(null).integerType)
    ['Long', 'java.lang.Long', 'long'].each { name ->
      assertEquals(Long, ArffReadOptions.fromMap([integerType: name]).integerType)
    }
    assertEquals(Long, ArffReadOptions.fromMap(new ArffReadOptions().integerType(Long).toMap()).integerType)
    def target = new File(tempDir, 'integer-spi.arff')
    target.text = text
    assertEquals(Long, Matrix.read([integerType: 'Long'], target).type('c'))
  }

  @Test
  void validForcedNumericAndDateCellsStillRoundTrip() {
    [ArffTypeDecl.NUMERIC, ArffTypeDecl.REAL, ArffTypeDecl.INTEGER].each { type ->
      def opts = new ArffWriteOptions().attributeTypesByColumn([c: type])
      def m = Matrix.builder('m').data(c: ['1', '2', null]).types(String).build()
      assert MatrixArffReader.readString(MatrixArffWriter.writeString(m, opts)).column('c') == [1, 2, null]
      ['', ' ', '?', ' 1 ', '1 ', '١٢'].each { value ->
        m = Matrix.builder('m').data(c: [value]).types(String).build()
        assertThrows(IllegalArgumentException) { MatrixArffWriter.writeString(m, opts) }
      }
      m = Matrix.builder('m').data(c: [1.0d, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY]).types(Double).build()
      assert MatrixArffReader.readString(MatrixArffWriter.writeString(m, opts)).column('c') == [1, null, null, null]
      m = Matrix.builder('m').data(c: [1.0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY]).types(Float).build()
      assert MatrixArffReader.readString(MatrixArffWriter.writeString(m, opts)).column('c') == [1, null, null, null]
    }
    def m = Matrix.builder('m').data(c: [3000000000L]).types(Long).build()
    def text = MatrixArffWriter.writeString(m, new ArffWriteOptions().attributeTypesByColumn([c: ArffTypeDecl.INTEGER]))
    assertEquals(3000000000L, MatrixArffReader.readString(text, new ArffReadOptions().integerType(Long))[0, 'c'])
    [se.alipsa.matrix.arff.ArffDateMode.UTC, se.alipsa.matrix.arff.ArffDateMode.WEKA].each { mode ->
      def opts = new ArffWriteOptions().attributeTypesByColumn([c: ArffTypeDecl.DATE]).dateMode(mode)
      m = Matrix.builder('m').data(c: ['2024-01-02T03:04:05']).types(String).build()
      text = MatrixArffWriter.writeString(m, opts)
      assertNotNull(MatrixArffReader.readString(text, new ArffReadOptions().dateMode(mode))[0, 'c'])
      m = Matrix.builder('m').data(c: ['2024-01-02T03:04:05 junk']).types(String).build()
      assertThrows(IllegalArgumentException) { MatrixArffWriter.writeString(m, opts) }
    }
    m = Matrix.builder('m').data(c: ['2024-01-02']).types(String).build()
    text = MatrixArffWriter.writeString(m, new ArffWriteOptions().attributeTypesByColumn([c: ArffTypeDecl.DATE])
        .dateFormatsByColumn([c: 'yyyy-MM-dd']))
    assertEquals(1704153600000L, MatrixArffReader.readString(text)[0, 'c'].time)
  }

  @Test
  void schemaFailuresLeaveTargetsUntouched() {
    def target = new File(tempDir, 'schema.arff')
    def cases = [
        [Matrix.builder('m').data(c: [1]).types(Integer).build(), new ArffWriteOptions().nominalColumns(['c'])],
        [Matrix.builder('m').data(c: [new Date()]).types(Date).build(), new ArffWriteOptions().dateFormat('yyyy-qq')],
        [Matrix.builder('m').data(c: ['x']).types(String).build(), new ArffWriteOptions().attributeTypesByColumn([c: ArffTypeDecl.RELATIONAL])],
        [Matrix.builder('m').data(c: ['a', 'z']).types(String).build(), new ArffWriteOptions().nominalMappings([c: ['a', 'b']])]
    ]
    cases.each { m, opts ->
      target.text = 'original'
      assertThrows(IllegalArgumentException) { MatrixArffWriter.write(m, target, opts) }
      assertEquals('original', target.text)
      def stream = new ByteArrayOutputStream()
      assertThrows(IllegalArgumentException) { MatrixArffWriter.write(m, stream, opts) }
      assertEquals(0, stream.size())
    }
    def duplicate = Matrix.builder().columnNames(['a', 'a']).rows([[1, 2], [3, 4]]).build()
    assertThrows(IllegalArgumentException) { MatrixArffWriter.writeString(duplicate) }
    def nested = Matrix.builder('m').data(bag: [duplicate]).types(Matrix).build()
    assertThrows(IllegalArgumentException) { MatrixArffWriter.writeString(nested) }
  }

  @Test
  void printWriterErrorStatesAreReported() {
    def m = Matrix.builder('m').data(c: [1]).types(Integer).build()
    Writer broken = new Writer() {
      @Override
      void write(char[] chars, int offset, int length) { throw new IOException('disk full') }
      @Override
      void flush() { }
      @Override
      void close() { }
    }
    def pw = new PrintWriter(broken)
    assertThrows(IOException) { MatrixArffWriter.write(m, pw) }
    def error = assertThrows(IllegalArgumentException) { MatrixArffWriter.write(m, pw) }
    assertTrue(error.message.contains('already in an error state'))
  }

  @Test
  void charsetOptionsAndStrictDecoding() {
    def latin = java.nio.charset.StandardCharsets.ISO_8859_1
    def m = Matrix.builder('m').data(c: ['Malmö']).types(String).build()
    def output = new ByteArrayOutputStream()
    MatrixArffWriter.write(m, output, new ArffWriteOptions().charset(latin))
    assertTrue(new String(output.toByteArray(), latin).contains('Malmö'))
    assertEquals('Malmö', MatrixArffReader.read(new ByteArrayInputStream(output.toByteArray()), new ArffReadOptions().charset(latin))[0, 'c'])
    def error = assertThrows(IllegalArgumentException) { MatrixArffReader.read(new ByteArrayInputStream(output.toByteArray())) }
    assertTrue(error.message.contains('charset'))
    def target = new File(tempDir, 'latin-spi.arff')
    m.write([charset: 'ISO-8859-1'], target)
    assertEquals('Malmö', Matrix.read([charset: 'ISO-8859-1'], target)[0, 'c'])
    [new ArffReadOptions(), new ArffWriteOptions()].each { opts ->
      assertEquals(latin, opts.charset(latin).class.fromMap(opts.toMap()).charset)
      assertEquals(java.nio.charset.StandardCharsets.UTF_8, opts.charset(null).charset)
    }
    assertTrue(Matrix.listReadOptions('arff').contains('integerType'))
    assertTrue(Matrix.listReadOptions('arff').contains('charset'))
    assertTrue(Matrix.listWriteOptions('arff').contains('charset'))
  }

  @Test
  void encodingFailuresPreserveFilesAndStreams() {
    def latin = java.nio.charset.StandardCharsets.ISO_8859_1
    def opts = new ArffWriteOptions().charset(latin).inferNominals(false)
    def cases = [
        [Matrix.builder('€').data(c: ['x']).types(String).build(), opts, "Relation name '€'"],
        [Matrix.builder('m').data('€': ['x']).types(String).build(), opts, "Attribute name '€'"],
        [Matrix.builder('m').data(c: ['a€b']).types(String).build(), opts, "Column 'c' row 0"],
        [Matrix.builder('m').data(c: ['a€b']).types(String).build(), new ArffWriteOptions().charset(latin), "Declaration of column 'c'"],
        [Matrix.builder('m').data(d: [null]).types(Date).build(), new ArffWriteOptions().charset(latin)
            .dateMode(se.alipsa.matrix.arff.ArffDateMode.WEKA).dateFormat("yyyy'€'"), "Declaration of column 'd'"]
    ]
    def nested = Matrix.builder('n').data('€': ['x']).types(String).build()
    cases << [Matrix.builder('m').data(bag: [nested]).types(Matrix).build(), opts, "Attribute name 'bag.€'"]
    nested = Matrix.builder('n').data(s: ['€']).types(String).build()
    cases << [Matrix.builder('m').data(bag: [nested]).types(Matrix).build(), opts, "Column 'bag[0].s' row 0"]
    nested = Matrix.builder('n').data(s: ['x']).types(String).build()
    cases << [Matrix.builder('m').data('€': [nested]).types(Matrix).build(), opts, "Attribute name '€'"]
    cases << [Matrix.builder('m').data(c: ['\uD800']).types(String).build(), new ArffWriteOptions().inferNominals(false), "Column 'c' row 0"]
    cases.each { m, options, message ->
      def target = new File(tempDir, 'encoding.arff')
      target.text = 'original'
      def error = assertThrows(IllegalArgumentException) { MatrixArffWriter.write(m, target, options) }
      assertTrue(error.message.contains(message), error.message)
      assertTrue(error.message.contains('cannot be encoded'), error.message)
      assertTrue(error.message.contains(options.charset.name()), error.message)
      assertEquals('original', target.text)
      def output = new ByteArrayOutputStream()
      assertThrows(IllegalArgumentException) { MatrixArffWriter.write(m, output, options) }
      assertEquals(0, output.size())
    }
    assertTrue(MatrixArffWriter.writeString(cases[0][0], opts).contains('€'))
  }

  @Test
  @SuppressWarnings('LocaleSetDefault')
  void wekaMonthNamesAndNestedDatesAreCheckedWithoutCaching() {
    def saved = Locale.default
    def mode = se.alipsa.matrix.arff.ArffDateMode.WEKA
    try {
      Locale.default = Locale.forLanguageTag('de-DE')
      def date = new GregorianCalendar(2024, Calendar.MARCH, 1).time
      def m = Matrix.builder('m').data(d: [date]).types(Date).build()
      def opts = new ArffWriteOptions().dateMode(mode).dateFormat('MMMM').charset(java.nio.charset.StandardCharsets.US_ASCII)
      assertThrows(IllegalArgumentException) { MatrixArffWriter.write(m, new ByteArrayOutputStream(), opts) }
    } finally {
      Locale.default = saved
    }
    def parts = [new Date(1704164645000L), new Date(1704251045000L)].collect { date ->
      Matrix.builder('n').data(d: [date]).types(Date).build()
    }
    def m = Matrix.builder('m').data(bag: parts).types(Matrix).build()
    def output = new ByteArrayOutputStream()
    MatrixArffWriter.write(m, output, new ArffWriteOptions().dateMode(mode).charset(java.nio.charset.StandardCharsets.ISO_8859_1))
    def read = MatrixArffReader.read(new ByteArrayInputStream(output.toByteArray()), new ArffReadOptions().dateMode(mode)
        .charset(java.nio.charset.StandardCharsets.ISO_8859_1))
    assertEquals(parts[0][0, 'd'], read[0, 'bag'][0, 'd'])
    assertEquals(parts[1][0, 'd'], read[1, 'bag'][0, 'd'])
  }

  @Test
  void rootLocaleDateTextIsAscii() {
    def format = new SimpleDateFormat('GyYMLwWDdFEuaHkKhmsSzZX', Locale.ROOT)
    format.timeZone = TimeZone.getTimeZone('UTC')
    (0..<12).each { month ->
      (1..7).each { day ->
        def text = format.format(new GregorianCalendar(2024, month, day).time)
        assertTrue(text.chars().allMatch { it <= 0x7F })
      }
    }
  }

  @Test
  void booleanNominalsAndInferenceThreshold() {
    def m = Matrix.builder('m').data(b: [true, false]).types(Boolean).build()
    [new ArffWriteOptions(), new ArffWriteOptions().nominalColumns(['b'])].each { opts ->
      def text = MatrixArffWriter.writeString(m, opts)
      assertTrue(text.contains('@ATTRIBUTE b {false,true}'))
      assertEquals(['true', 'false'], MatrixArffReader.readString(text).column('b'))
    }
    m = Matrix.builder('m').data(c: (0..<30).collect { "v$it".toString() }).types(String).build()
    assertTrue(MatrixArffWriter.writeString(m, new ArffWriteOptions().nominalThreshold(5)).contains('@ATTRIBUTE c STRING'))
    m = Matrix.builder('m').data(c: ['a', 'b', 'c']).types(String).build()
    assertTrue(MatrixArffWriter.writeString(m, new ArffWriteOptions().nominalThreshold(5)).contains('@ATTRIBUTE c {a,b,c}'))
  }

  @Test
  void readsAndWritesInsideZipFileSystem() {
    def zip = new File(tempDir, 'nio.zip').toPath()
    java.nio.file.FileSystems.newFileSystem(URI.create("jar:${zip.toUri()}"), [create: 'true']).withCloseable { fs ->
      def path = fs.getPath('/data/out.arff')
      def m = Matrix.builder('m').data(c: [1]).types(Integer).build()
      MatrixArffWriter.write(m, path)
      assertEquals([1], MatrixArffReader.read(path).column('c'))
    }
    assertThrows(IllegalArgumentException) {
      MatrixArffWriter.write(Matrix.builder('m').data(c: [1]).types(Integer).build(), (Path) null)
    }
  }

  @Test
  void sparseNestedRowMismatchesAndNominalSourceLocations() {
    def header = '@relation r\n@attribute a numeric\n@attribute bag relational\n@attribute x numeric\n@attribute y numeric\n@end bag\n@data\n'
    def text = header + '{0 1, 1 \'2\\n3,4\'}'
    def m = MatrixArffReader.readString(text)
    assert m[0, 'bag'].rows() == [[2, null], [3, 4]]
    assertThrows(IllegalArgumentException) { MatrixArffReader.readString(text, new ArffReadOptions().failOnRowLengthMismatch(true)) }
    def error = assertThrows(IllegalArgumentException) {
      MatrixArffReader.readString(header.replace('@attribute x numeric', '@attribute x {a,a}'))
    }
    assertTrue(error.message.contains('line 4'))
    assertTrue(error.message.contains('@attribute x {a,a}'))
    assertEquals(['', 'a'], MatrixArffReader.readString("@relation r\n@attribute c {'',a}\n@data\n''\na").column('c'))
  }

  @Test
  void numericUnicodeAndNestedDateDeclarationsFailBeforeBytesAreWritten() {
    def latin = java.nio.charset.StandardCharsets.ISO_8859_1
    def cases = [ArffTypeDecl.NUMERIC, ArffTypeDecl.REAL, ArffTypeDecl.INTEGER].collect { type ->
      [Matrix.builder('m').data(c: [type == ArffTypeDecl.INTEGER ? '١٢' : '١٢.5']).types(String).build(),
       new ArffWriteOptions().charset(latin).attributeTypesByColumn([c: type])]
    }
    def nested = Matrix.builder('n').data(d: [null]).types(Date).build()
    cases << [Matrix.builder('m').data(bag: [nested]).types(Matrix).build(), new ArffWriteOptions().charset(latin)
        .dateMode(se.alipsa.matrix.arff.ArffDateMode.WEKA).dateFormat("yyyy'€'")]
    cases.each { m, options ->
      def target = new File(tempDir, 'numeric-unicode.arff')
      target.text = 'original'
      assertThrows(IllegalArgumentException) { MatrixArffWriter.write(m, target, options) }
      assertEquals('original', target.text)
      def stream = new ByteArrayOutputStream()
      assertThrows(IllegalArgumentException) { MatrixArffWriter.write(m, stream, options) }
      assertEquals(0, stream.size())
    }
    def emoji = Matrix.builder('m').data(c: ['😀']).types(String).build()
    def stream = new ByteArrayOutputStream()
    MatrixArffWriter.write(emoji, stream)
    assertEquals('😀', MatrixArffReader.read(new ByteArrayInputStream(stream.toByteArray()))[0, 'c'])
    def m = Matrix.builder('m').data(c: ['日本']).types(String).build()
    stream = new ByteArrayOutputStream()
    MatrixArffWriter.write(m, stream, new ArffWriteOptions().charset(java.nio.charset.Charset.forName('Shift_JIS')))
    assertEquals('日本', MatrixArffReader.read(new ByteArrayInputStream(stream.toByteArray()),
        new ArffReadOptions().charset(java.nio.charset.Charset.forName('Shift_JIS')))[0, 'c'])
  }

  @Test
  void streamEncoderFinishesWithoutClosingCallerStream() {
    def m = Matrix.builder('m').data(c: ['日本']).types(String).build()
    def output = new ByteArrayOutputStream() {
      boolean closed
      @Override
      void close() {
        closed = true
        super.close()
      }
    }
    def charset = java.nio.charset.Charset.forName('ISO-2022-JP')
    MatrixArffWriter.write(m, output, new ArffWriteOptions().charset(charset))
    assertFalse(output.closed)
    assertEquals('日本', MatrixArffReader.read(new ByteArrayInputStream(output.toByteArray()),
        new ArffReadOptions().charset(charset))[0, 'c'])
  }

  @Test
  void sparseIntegerDefaultsReuseTypedZeroConstants() {
    def header = '@relation sparse\n' + (0..<64).collect { "@attribute c$it integer" }.join('\n')
    def text = header + '\n@data\n{}\n{}\n{}'
    [(Integer): 0, (Long): 0L, (BigDecimal): BigDecimal.ZERO].each { Class type, Number zero ->
      def m = MatrixArffReader.readString(text, new ArffReadOptions().integerType(type))
      m.rows().each { row ->
        row.each { value ->
          assertEquals(type, value.class)
          assertSame(zero, value)
        }
      }
    }
  }
}
