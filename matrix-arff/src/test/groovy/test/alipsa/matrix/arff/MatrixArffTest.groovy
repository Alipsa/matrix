package test.alipsa.matrix.arff

import org.junit.jupiter.api.*
import se.alipsa.matrix.arff.MatrixArffReader
import se.alipsa.matrix.arff.MatrixArffWriter
import se.alipsa.matrix.core.Matrix

import java.nio.file.Files
import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.*

@TestMethodOrder(MethodOrderer.OrderAnnotation)
class MatrixArffTest {

  private static File irisFile
  private static File tempDir

  @BeforeAll
  static void setup() {
    // Get the iris.arff file from test resources
    URL irisUrl = MatrixArffTest.class.getClassLoader().getResource("iris.arff")
    assertNotNull(irisUrl, "iris.arff should be in test resources")
    irisFile = new File(irisUrl.toURI())

    // Create temp directory for output files
    tempDir = Files.createTempDirectory("matrix-arff-test").toFile()
  }

  @AfterAll
  static void cleanup() {
    if (tempDir != null && tempDir.exists()) {
      tempDir.listFiles()?.each { it.delete() }
      tempDir.delete()
    }
  }

  @Test @Order(1)
  void readIrisFromFile() {
    Matrix m = MatrixArffReader.read(irisFile)

    assertEquals("iris", m.matrixName)
    assertEquals(150, m.rowCount())
    assertEquals(5, m.columnCount())

    // Check column names
    List<String> expectedColumns = ["sepallength", "sepalwidth", "petallength", "petalwidth", "class"]
    assertEquals(expectedColumns, m.columnNames())

    // Check first row values
    assertEquals(new BigDecimal("5.1"), m[0, "sepallength"])
    assertEquals(new BigDecimal("3.5"), m[0, "sepalwidth"])
    assertEquals(new BigDecimal("1.4"), m[0, "petallength"])
    assertEquals(new BigDecimal("0.2"), m[0, "petalwidth"])
    assertEquals("Iris-setosa", m[0, "class"])

    // Check last row values
    assertEquals(new BigDecimal("5.9"), m[149, "sepallength"])
    assertEquals(new BigDecimal("3.0"), m[149, "sepalwidth"])
    assertEquals(new BigDecimal("5.1"), m[149, "petallength"])
    assertEquals(new BigDecimal("1.8"), m[149, "petalwidth"])
    assertEquals("Iris-virginica", m[149, "class"])

    // Check types
    assertEquals(BigDecimal, m.type("sepallength"))
    assertEquals(BigDecimal, m.type("sepalwidth"))
    assertEquals(BigDecimal, m.type("petallength"))
    assertEquals(BigDecimal, m.type("petalwidth"))
    assertEquals(String, m.type("class"))
  }

  @Test @Order(2)
  void readIrisFromPath() {
    Matrix m = MatrixArffReader.read(irisFile.toPath())

    assertEquals("iris", m.matrixName)
    assertEquals(150, m.rowCount())
    assertEquals(5, m.columnCount())
  }

  @Test @Order(3)
  void readIrisFromUrl() {
    Matrix m = MatrixArffReader.read(irisFile.toURI().toURL())

    assertEquals("iris", m.matrixName)
    assertEquals(150, m.rowCount())
    assertEquals(5, m.columnCount())
  }

  @Test @Order(4)
  void readIrisFromInputStream() {
    new FileInputStream(irisFile).withStream { InputStream is ->
      Matrix m = MatrixArffReader.read(is, "test-iris")

      // Note: The name from @RELATION in the file takes precedence over the default name
      assertEquals("iris", m.matrixName)
      assertEquals(150, m.rowCount())
      assertEquals(5, m.columnCount())
    }
  }

  @Test @Order(5)
  void writeAndReadRoundTrip() {
    // Read the original iris file
    Matrix original = MatrixArffReader.read(irisFile)

    // Write to a new file
    File outputFile = new File(tempDir, "iris_output.arff")
    MatrixArffWriter.write(original, outputFile)

    // Read it back
    Matrix roundTripped = MatrixArffReader.read(outputFile)

    // Verify the data matches
    assertEquals(original.rowCount(), roundTripped.rowCount())
    assertEquals(original.columnCount(), roundTripped.columnCount())
    assertEquals(original.columnNames(), roundTripped.columnNames())

    // Check a few values
    for (int row : [0, 50, 100, 149]) {
      for (String col : original.columnNames()) {
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

    Path outputPath = tempDir.toPath().resolve("iris_path_output.arff")
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

    String arffContent = baos.toString("UTF-8")
    assertTrue(arffContent.contains("@RELATION"))
    assertTrue(arffContent.contains("@ATTRIBUTE"))
    assertTrue(arffContent.contains("@DATA"))
    assertTrue(arffContent.contains("sepallength"))
    assertTrue(arffContent.contains("5.1"))
  }

  @Test @Order(8)
  void writeToWriter() {
    Matrix original = MatrixArffReader.read(irisFile)

    StringWriter sw = new StringWriter()
    MatrixArffWriter.write(original, sw)

    String arffContent = sw.toString()
    assertTrue(arffContent.contains("@RELATION"))
    assertTrue(arffContent.contains("@ATTRIBUTE"))
    assertTrue(arffContent.contains("@DATA"))
  }

  @Test @Order(9)
  void testNumericTypes() {
    // Create a matrix with numeric columns
    Matrix m = Matrix.builder("numeric_test")
        .columnNames("integers", "decimals", "longs")
        .columns(
            [1, 2, 3, 4, 5] as List,
            [1.1, 2.2, 3.3, 4.4, 5.5] as List,
            [100L, 200L, 300L, 400L, 500L] as List
        )
        .types([Integer, BigDecimal, Long])
        .build()

    File outputFile = new File(tempDir, "numeric_test.arff")
    MatrixArffWriter.write(m, outputFile)

    Matrix roundTripped = MatrixArffReader.read(outputFile)
    assertEquals(5, roundTripped.rowCount())
    assertEquals(3, roundTripped.columnCount())
  }

  @Test @Order(10)
  void testMissingValues() {
    // Create ARFF content with missing values
    String arffContent = """
@RELATION missing_test

@ATTRIBUTE name STRING
@ATTRIBUTE value NUMERIC

@DATA
'Alice',10.5
'Bob',?
?,20.5
""".trim()

    ByteArrayInputStream bais = new ByteArrayInputStream(arffContent.getBytes("UTF-8"))
    Matrix m = MatrixArffReader.read(bais, "missing_test")

    assertEquals(3, m.rowCount())
    assertEquals("Alice", m[0, "name"])
    assertEquals(new BigDecimal("10.5"), m[0, "value"])
    assertEquals("Bob", m[1, "name"])
    assertNull(m[1, "value"])
    assertNull(m[2, "name"])
    assertEquals(new BigDecimal("20.5"), m[2, "value"])
  }

  @Test @Order(11)
  void testNominalValues() {
    // Create a matrix and specify nominal mappings when writing
    Matrix m = Matrix.builder("nominal_test")
        .columnNames("category", "value")
        .columns(
            ["A", "B", "A", "C", "B"] as List,
            [1.0, 2.0, 3.0, 4.0, 5.0] as List
        )
        .types([String, BigDecimal])
        .build()

    File outputFile = new File(tempDir, "nominal_test.arff")
    Map<String, List<String>> nominalMappings = ["category": ["A", "B", "C"]]
    MatrixArffWriter.write(m, outputFile, nominalMappings)

    // Verify the output contains nominal type definition
    String content = outputFile.text
    assertTrue(content.contains("{A,B,C}"))

    // Read it back
    Matrix roundTripped = MatrixArffReader.read(outputFile)
    assertEquals(5, roundTripped.rowCount())
    assertEquals("A", roundTripped[0, "category"])
    assertEquals("C", roundTripped[3, "category"])
  }

  @Test @Order(12)
  void testQuotedValues() {
    // Create ARFF content with quoted values
    String arffContent = """
@RELATION 'quoted test'

@ATTRIBUTE 'column with space' STRING
@ATTRIBUTE normal NUMERIC

@DATA
'value with, comma',10.5
'simple',20.5
""".trim()

    ByteArrayInputStream bais = new ByteArrayInputStream(arffContent.getBytes("UTF-8"))
    Matrix m = MatrixArffReader.read(bais, "quoted_test")

    assertEquals(2, m.rowCount())
    assertEquals("value with, comma", m[0, "column with space"])
    assertEquals("simple", m[1, "column with space"])
  }

  @Test @Order(13)
  void testIntegerType() {
    String arffContent = """
@RELATION integer_test

@ATTRIBUTE id INTEGER
@ATTRIBUTE count INTEGER

@DATA
1,100
2,200
3,300
""".trim()

    ByteArrayInputStream bais = new ByteArrayInputStream(arffContent.getBytes("UTF-8"))
    Matrix m = MatrixArffReader.read(bais)

    assertEquals(3, m.rowCount())
    assertEquals(Integer, m.type("id"))
    assertEquals(Integer, m.type("count"))
    assertEquals(1, m[0, "id"])
    assertEquals(200, m[1, "count"])
  }

  @Test @Order(14)
  void testCommentsAreIgnored() {
    String arffContent = """
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
""".trim()

    ByteArrayInputStream bais = new ByteArrayInputStream(arffContent.getBytes("UTF-8"))
    Matrix m = MatrixArffReader.read(bais)

    assertEquals(3, m.rowCount())
    assertEquals(new BigDecimal("1.0"), m[0, "value"])
    assertEquals(new BigDecimal("2.0"), m[1, "value"])
    assertEquals(new BigDecimal("3.0"), m[2, "value"])
  }
}
