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