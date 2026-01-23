package test.alipsa.matrix.avro

import org.apache.avro.Conversions
import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericDatumWriter
import org.apache.avro.generic.GenericRecord
import org.junit.jupiter.api.*

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate

import se.alipsa.matrix.avro.MatrixAvroReader
import se.alipsa.matrix.avro.AvroReadOptions
import se.alipsa.matrix.avro.exceptions.AvroValidationException
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*

@TestMethodOrder(MethodOrderer.OrderAnnotation)
class MatrixAvroReaderTest {

  private static File avroFile
  private static Schema schema
  private static LocalDate birth1 = LocalDate.of(1990, 1, 5)
  private static LocalDate birth2 = LocalDate.of(1984, 7, 23)
  private static Instant ts1 = Instant.parse("2024-03-01T12:34:56Z")
  private static Instant ts2 = Instant.parse("2024-12-24T08:09:10Z")

  @BeforeAll
  static void createAvroFixture() {
    schema = buildSchema()
    avroFile = Files.createTempFile("matrix-avro-reader-", ".avro").toFile()
    writeSampleOcfi(avroFile, schema)
  }

  @AfterAll
  static void cleanup() {
    if (avroFile != null) avroFile.delete()
  }

  @Test @Order(1)
  void readFile() {
    Matrix m = MatrixAvroReader.read(avroFile)
    assertBasicShapeAndValues(m)
  }

  @Test @Order(2)
  void readFromPath() {
    Path p = avroFile.toPath()
    Matrix m = MatrixAvroReader.read(p)
    assertBasicShapeAndValues(m)
  }

  @Test @Order(3)
  void readUrl() {
    URL url = avroFile.toURI().toURL()
    Matrix m = MatrixAvroReader.read(url)
    assertBasicShapeAndValues(m)
  }

  @Test @Order(4)
  void readFromInputStream() {
    InputStream is = new FileInputStream(avroFile)
    try {
      Matrix m = MatrixAvroReader.read(is, "FromStream")
      assertBasicShapeAndValues(m)
      // If you expose a name getter, you can assert it here:
      // assertEquals("FromStream", m.getName())
    } finally {
      is.close()
    }
  }

  @Test @Order(5)
  void readFromByteArray() {
    byte[] content = Files.readAllBytes(avroFile.toPath())
    Matrix m = MatrixAvroReader.read(content, "FromBytes")
    assertBasicShapeAndValues(m)
  }

  @Test @Order(6)
  void readFromByteArrayWithDefaultName() {
    byte[] content = Files.readAllBytes(avroFile.toPath())
    Matrix m = MatrixAvroReader.read(content)
    assertBasicShapeAndValues(m)
    assertEquals("AvroMatrix", m.matrixName)
  }

  // ---------- convenience method tests ----------

  @Test @Order(7)
  void testReadFile() {
    Matrix m = MatrixAvroReader.readFile(avroFile.absolutePath)
    assertBasicShapeAndValues(m)
  }

  @Test @Order(8)
  void testReadUrl() {
    Matrix m = MatrixAvroReader.readUrl(avroFile.toURI().toString())
    assertBasicShapeAndValues(m)
  }

  // ---------- validation tests ----------

  @Test @Order(10)
  void testValidationNullFile() {
    def ex = assertThrows(AvroValidationException) {
      MatrixAvroReader.read((File) null)
    }
    assertTrue(ex.message.contains("cannot be null"))
  }

  @Test @Order(11)
  void testValidationNullPath() {
    def ex = assertThrows(IllegalArgumentException) {
      MatrixAvroReader.read((Path) null)
    }
    assertTrue(ex.message.contains("cannot be null"))
  }

  @Test @Order(12)
  void testValidationNullUrl() {
    def ex = assertThrows(IllegalArgumentException) {
      MatrixAvroReader.read((URL) null)
    }
    assertTrue(ex.message.contains("cannot be null"))
  }

  @Test @Order(13)
  void testValidationNullInputStream() {
    def ex = assertThrows(IllegalArgumentException) {
      MatrixAvroReader.read((InputStream) null)
    }
    assertTrue(ex.message.contains("cannot be null"))
  }

  @Test @Order(14)
  void testValidationNullByteArray() {
    def ex = assertThrows(IllegalArgumentException) {
      MatrixAvroReader.read((byte[]) null)
    }
    assertTrue(ex.message.contains("cannot be null"))
  }

  @Test @Order(15)
  void testValidationNullFilePath() {
    def ex = assertThrows(IllegalArgumentException) {
      MatrixAvroReader.readFile(null)
    }
    assertTrue(ex.message.contains("cannot be null"))
  }

  @Test @Order(16)
  void testValidationNullUrlString() {
    def ex = assertThrows(IllegalArgumentException) {
      MatrixAvroReader.readUrl(null)
    }
    assertTrue(ex.message.contains("cannot be null"))
  }

  @Test @Order(17)
  void testValidationFileDoesNotExist() {
    def ex = assertThrows(AvroValidationException) {
      MatrixAvroReader.read(new File("/non/existent/path/to/file.avro"))
    }
    assertTrue(ex.message.contains("does not exist"))
  }

  @Test @Order(18)
  void testValidationFileIsDirectory() {
    File tempDir = Files.createTempDirectory("avro-test").toFile()
    try {
      def ex = assertThrows(AvroValidationException) {
        MatrixAvroReader.read(tempDir)
      }
      assertTrue(ex.message.contains("directory"))
    } finally {
      tempDir.delete()
    }
  }

  @Test @Order(19)
  void testValidationInvalidUrlString() {
    def ex = assertThrows(IllegalArgumentException) {
      MatrixAvroReader.readUrl("not a valid url")
    }
    assertTrue(ex.message.contains("Invalid URL string"))
  }

  // ---------- AvroReadOptions tests ----------

  @Test @Order(20)
  void testReadWithOptionsCustomName() {
    def options = new AvroReadOptions()
        .matrixName("CustomName")

    Matrix m = MatrixAvroReader.read(avroFile, options)
    assertBasicShapeAndValues(m)
    assertEquals("CustomName", m.matrixName)
  }

  @Test @Order(21)
  void testReadFromPathWithOptions() {
    def options = new AvroReadOptions()
        .matrixName("PathOptions")

    Matrix m = MatrixAvroReader.read(avroFile.toPath(), options)
    assertBasicShapeAndValues(m)
    assertEquals("PathOptions", m.matrixName)
  }

  @Test @Order(22)
  void testReadFromUrlWithOptions() {
    def options = new AvroReadOptions()
        .matrixName("UrlOptions")

    Matrix m = MatrixAvroReader.read(avroFile.toURI().toURL(), options)
    assertBasicShapeAndValues(m)
    assertEquals("UrlOptions", m.matrixName)
  }

  @Test @Order(23)
  void testReadFromByteArrayWithOptions() {
    byte[] content = Files.readAllBytes(avroFile.toPath())
    def options = new AvroReadOptions()
        .matrixName("ByteOptions")

    Matrix m = MatrixAvroReader.read(content, options)
    assertBasicShapeAndValues(m)
    assertEquals("ByteOptions", m.matrixName)
  }

  @Test @Order(24)
  void testReadFromInputStreamWithOptions() {
    def options = new AvroReadOptions()
        .matrixName("StreamOptions")

    InputStream is = new FileInputStream(avroFile)
    try {
      Matrix m = MatrixAvroReader.read(is, options)
      assertBasicShapeAndValues(m)
      assertEquals("StreamOptions", m.matrixName)
    } finally {
      is.close()
    }
  }

  @Test @Order(25)
  void testReadWithOptionsNullValidation() {
    def ex = assertThrows(IllegalArgumentException) {
      MatrixAvroReader.read(avroFile, (AvroReadOptions) null)
    }
    assertEquals("Options cannot be null", ex.message)
  }

  @Test @Order(26)
  void testReadWithOptionsDefaultName() {
    // When matrixName is not set, should use file name
    def options = new AvroReadOptions()

    Matrix m = MatrixAvroReader.read(avroFile, options)
    assertBasicShapeAndValues(m)
    // Name should be derived from file name (without extension)
    assertNotNull(m.matrixName)
    assertFalse(m.matrixName.isEmpty())
  }

  // ---------- custom exception tests ----------

  @Test @Order(30)
  void testValidationExceptionForNullFile() {
    def ex = assertThrows(AvroValidationException) {
      MatrixAvroReader.read((File) null)
    }
    assertEquals("file", ex.parameterName)
    assertNotNull(ex.suggestion)
    assertTrue(ex.message.contains("cannot be null"))
  }

  @Test @Order(31)
  void testValidationExceptionForNonExistentFile() {
    def ex = assertThrows(AvroValidationException) {
      MatrixAvroReader.read(new File("/non/existent/path.avro"))
    }
    assertEquals("file", ex.parameterName)
    assertNotNull(ex.suggestion)
    assertTrue(ex.message.contains("does not exist"))
  }

  @Test @Order(32)
  void testValidationExceptionForDirectory() {
    File tempDir = Files.createTempDirectory("avro-test").toFile()
    try {
      def ex = assertThrows(AvroValidationException) {
        MatrixAvroReader.read(tempDir)
      }
      assertEquals("file", ex.parameterName)
      assertNotNull(ex.suggestion)
      assertTrue(ex.message.contains("directory"))
    } finally {
      tempDir.delete()
    }
  }

  // ---------- helpers ----------

  private static Schema buildSchema() {
    // language=JSON
    def schemaJson = """
    {
      "type": "record",
      "name": "Person",
      "fields": [
        {"name":"name",   "type":"string"},
        {"name":"age",    "type":"int"},
        {"name":"birthday", "type":{"type":"int","logicalType":"date"}},
        {"name":"ts",     "type":{"type":"long","logicalType":"timestamp-millis"}},
        {"name":"price",  "type":{"type":"bytes","logicalType":"decimal","precision":10,"scale":2}}
      ]
    }
    """.stripIndent()
    new Schema.Parser().parse(schemaJson)
  }

  private static void writeSampleOcfi(File outFile, Schema schema) {
    def writer = new DataFileWriter<GenericRecord>(new GenericDatumWriter<>(schema))
    writer.create(schema, outFile)
    try {
      writer.append(makeRecord(schema,
          "Alice", 30, birth1, ts1, new BigDecimal("12.34")))
      writer.append(makeRecord(schema,
          "Bob", 43, birth2, ts2, new BigDecimal("56.78")))
    } finally {
      writer.close()
    }
  }

  private static GenericRecord makeRecord(Schema schema,
                                          String name, int age,
                                          LocalDate birthday, Instant ts,
                                          BigDecimal price) {
    def rec = new GenericData.Record(schema)
    rec.put("name", name)
    rec.put("age", age)
    rec.put("birthday", (int) birthday.toEpochDay())               // date: days since epoch
    rec.put("ts", ts.toEpochMilli())                               // timestamp-millis

    // decimal to bytes
    Schema priceSchema = schema.getField("price").schema()
    LogicalTypes.Decimal dec = (LogicalTypes.Decimal) priceSchema.getLogicalType()
    ByteBuffer bb = new Conversions.DecimalConversion().toBytes(price, priceSchema, dec)
    rec.put("price", bb)
    return rec
  }

  /** Shared assertions verifying both structure and decoded logical types. */
  private static void assertBasicShapeAndValues(Matrix m) {
    // shape
    assertEquals(2, m.rowCount())
    assertEquals(5, m.columnCount())

    // values (Groovy indexing works even in JUnit tests written in Groovy)
    assertEquals("Alice", m[0, "name"])
    assertEquals(30, m[0, "age"])
    assertEquals(birth1, m[0, "birthday"])
    assertEquals(ts1, m[0, "ts"])
    assertEquals(new BigDecimal("12.34"), m[0, "price"])

    assertEquals("Bob", m[1, "name"])
    assertEquals(43, m[1, "age"])
    assertEquals(birth2, m[1, "birthday"])
    assertEquals(ts2, m[1, "ts"])
    assertEquals(new BigDecimal("56.78"), m[1, "price"])

    // Basic type checks to ensure logical conversions landed correctly
    assertTrue(m["birthday"][0] instanceof LocalDate)
    assertTrue(m["ts"][0] instanceof Instant)
    assertTrue(m["price"][0] instanceof BigDecimal)
  }
}