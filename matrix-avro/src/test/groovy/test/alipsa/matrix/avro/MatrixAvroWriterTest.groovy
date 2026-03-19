package test.alipsa.matrix.avro

import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import org.apache.avro.file.DataFileReader
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.avro.AvroSchemaDecl
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.avro.AvroWriteOptions
import se.alipsa.matrix.avro.exceptions.AvroSchemaException
import se.alipsa.matrix.avro.exceptions.AvroValidationException

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.LocalTime

import static org.junit.jupiter.api.Assertions.*

class MatrixAvroWriterTest {

  @Test
  void schema_has_expected_logical_types() {
    // Build a tiny Matrix with the three columns of interest
    def cols = new LinkedHashMap<String, List<?>>() as LinkedHashMap<String, List<?>>
    cols["ldt"]   = [LocalDateTime.of(2024, 7, 1, 10, 20, 30, 999_000_000)] // nanos = .999
    cols["time"]  = [LocalTime.of(9, 10, 11, 345_000_000)]                  // 09:10:11.345
    cols["price"] = [new BigDecimal("456.78")]                               // precision=5, scale=2

    Matrix m = Matrix.builder("WriterSanity")
        .columns(cols)
        .types(LocalDateTime, LocalTime, BigDecimal)
        .build()

    // Write with decimal inference enabled
    File tmp = Files.createTempFile("matrix-avro-writer-schema-", ".avro").toFile()
    try {
      MatrixAvroWriter.write(m, tmp, true)

      // Read the written schema directly (no MatrixAvroReader)
      def reader = new DataFileReader<GenericRecord>(tmp, new GenericDatumReader<>())
      try {
        Schema fileSchema = reader.schema

        // ---- ldt: local-timestamp-micros on LONG ----
        Schema ldtSchema = nonNullFieldSchema(fileSchema, "ldt")
        assertEquals(Schema.Type.LONG, ldtSchema.getType(), "ldt should be LONG")
        assertNotNull(ldtSchema.getLogicalType(), "ldt must have a logical type")
        assertEquals("local-timestamp-micros", ldtSchema.getLogicalType().name, "ldt logicalType")

        // ---- time: time-millis on INT ----
        Schema timeSchema = nonNullFieldSchema(fileSchema, "time")
        assertEquals(Schema.Type.INT, timeSchema.getType(), "time should be INT")
        assertNotNull(timeSchema.getLogicalType(), "time must have a logical type")
        assertEquals("time-millis", timeSchema.getLogicalType().name, "time logicalType")

        // ---- price: decimal(bytes) with inferred precision/scale ----
        Schema priceSchema = nonNullFieldSchema(fileSchema, "price")
        assertEquals(Schema.Type.BYTES, priceSchema.getType(), "price should be BYTES when decimal")
        assertNotNull(priceSchema.getLogicalType(), "price must have a logical type")
        assertEquals("decimal", priceSchema.getLogicalType().name, "price logicalType")

        def dec = (LogicalTypes.Decimal) priceSchema.getLogicalType()
        assertEquals(5, dec.getPrecision(), "price decimal precision should be inferred as 5 (456.78)")
        assertEquals(2, dec.getScale(), "price decimal scale should be inferred as 2")
      } finally {
        reader.close()
      }
    } finally {
      tmp.delete()
    }
  }

  @Test
  void writeBytesBasic() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["id"] = [1, 2, 3]
    cols["name"] = ["Alice", "Bob", "Charlie"]
    cols["value"] = [10.5, 20.5, 30.5]

    Matrix m = Matrix.builder("ByteTest")
        .columns(cols)
        .types(Integer, String, BigDecimal)
        .build()

    byte[] avroBytes = MatrixAvroWriter.writeBytes(m, true)
    assertNotNull(avroBytes)
    assertTrue(avroBytes.length > 0, "Byte array should not be empty")
  }

  @Test
  void writeBytesRoundTrip() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["id"] = [1, 2, 3]
    cols["score"] = [95.5, 87.3, 92.1]

    Matrix original = Matrix.builder("RoundTrip")
        .columns(cols)
        .types(Integer, BigDecimal)
        .build()

    // Write to byte array
    byte[] avroBytes = MatrixAvroWriter.writeBytes(original, true)

    // Read it back
    Matrix result = se.alipsa.matrix.avro.MatrixAvroReader.read(avroBytes, "RoundTrip")

    assertEquals(original.rowCount(), result.rowCount())
    assertEquals(original.columnCount(), result.columnCount())
    assertEquals(original.columnNames(), result.columnNames())
  }

  @Test
  void writeBytesWithoutInference() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["price"] = [new BigDecimal("123.45"), new BigDecimal("678.90")]

    Matrix m = Matrix.builder("NoInference")
        .columns(cols)
        .types(BigDecimal)
        .build()

    // Note: When inferPrecisionAndScale=false, BigDecimal columns are stored as Avro doubles.
    // This may lose precision for values that exceed double's precision limits.
    byte[] avroBytes = MatrixAvroWriter.writeBytes(m, false)
    assertNotNull(avroBytes)
    assertTrue(avroBytes.length > 0)
  }

  @Test
  void schema_infers_decimal_for_object_column() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["price"] = [new BigDecimal("12.30"), new BigDecimal("456.789")]

    Matrix m = Matrix.builder("ObjectDecimal")
        .columns(cols)
        .types(Object)
        .build()

    File tmp = Files.createTempFile("matrix-avro-writer-object-decimal-", ".avro").toFile()
    try {
      MatrixAvroWriter.write(m, tmp, true)
      def reader = new DataFileReader<GenericRecord>(tmp, new GenericDatumReader<>())
      try {
        Schema schema = reader.schema
        Schema priceSchema = nonNullFieldSchema(schema, "price")
        assertEquals(Schema.Type.BYTES, priceSchema.getType())
        assertNotNull(priceSchema.getLogicalType())
        assertEquals("decimal", priceSchema.getLogicalType().name)
        def dec = (LogicalTypes.Decimal) priceSchema.getLogicalType()
        assertEquals(6, dec.getPrecision())
        assertEquals(3, dec.getScale())
      } finally {
        reader.close()
      }
    } finally {
      tmp.delete()
    }
  }

  // ---------- validation tests ----------

  @Test
  void testValidationNullPath() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["id"] = [1, 2]
    Matrix m = Matrix.builder("Test").columns(cols).types(Integer).build()

    def ex = assertThrows(IllegalArgumentException) {
      MatrixAvroWriter.write(m, (Path) null)
    }
    assertEquals("Path cannot be null", ex.message)
  }

  @Test
  void testValidationNullOutputStream() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["id"] = [1, 2]
    Matrix m = Matrix.builder("Test").columns(cols).types(Integer).build()

    def ex = assertThrows(IllegalArgumentException) {
      MatrixAvroWriter.write(m, (OutputStream) null)
    }
    assertEquals("OutputStream cannot be null", ex.message)
  }

  @Test
  void testValidationWriteBytesNullMatrix() {
    def ex = assertThrows(AvroValidationException) {
      MatrixAvroWriter.writeBytes(null)
    }
    assertEquals("matrix", ex.parameterName)
    assertTrue(ex.message.contains("cannot be null"))
  }

  @Test
  void testValidationWriteBytesEmptyMatrix() {
    Matrix m = Matrix.builder("Empty").build()
    def ex = assertThrows(AvroValidationException) {
      MatrixAvroWriter.writeBytes(m)
    }
    assertEquals("matrix", ex.parameterName)
    assertTrue(ex.message.contains("at least one column"))
  }

  @Test
  void testWriteToOutputStream() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["id"] = [1, 2, 3]
    cols["name"] = ["Alice", "Bob", "Charlie"]

    Matrix m = Matrix.builder("StreamTest")
        .columns(cols)
        .types(Integer, String)
        .build()

    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    MatrixAvroWriter.write(m, baos, false)

    byte[] bytes = baos.toByteArray()
    assertNotNull(bytes)
    assertTrue(bytes.length > 0, "Output stream should contain data")

    // Verify it can be read back
    Matrix result = se.alipsa.matrix.avro.MatrixAvroReader.read(bytes, "StreamTest")
    assertEquals(3, result.rowCount())
    assertEquals(2, result.columnCount())
  }

  @Test
  void testWriteCreatesParentDirectory() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["id"] = [1]
    Matrix m = Matrix.builder("Test").columns(cols).types(Integer).build()

    // Create a temp directory and then a nested path that doesn't exist yet
    File tempDir = Files.createTempDirectory("avro-parent-test").toFile()
    File nestedFile = new File(tempDir, "nested/subdir/test.avro")

    try {
      assertFalse(nestedFile.parentFile.exists(), "Parent directory should not exist yet")

      MatrixAvroWriter.write(m, nestedFile)

      assertTrue(nestedFile.exists(), "File should be created")
      assertTrue(nestedFile.parentFile.exists(), "Parent directory should be created")
    } finally {
      // Clean up
      if (nestedFile.exists()) nestedFile.delete()
      new File(tempDir, "nested/subdir").delete()
      new File(tempDir, "nested").delete()
      tempDir.delete()
    }
  }

  // ---------- AvroWriteOptions tests ----------

  @Test
  void testWriteWithOptions() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["id"] = [1, 2, 3]
    cols["name"] = ["Alice", "Bob", "Charlie"]

    Matrix m = Matrix.builder("OptionsTest")
        .columns(cols)
        .types(Integer, String)
        .build()

    def options = new AvroWriteOptions()
        .namespace("com.example.test")
        .schemaName("TestData")

    File tmp = Files.createTempFile("avro-options-", ".avro").toFile()
    try {
      MatrixAvroWriter.write(m, tmp, options)

      // Verify schema has correct namespace and name
      def reader = new DataFileReader<GenericRecord>(tmp, new GenericDatumReader<>())
      try {
        Schema schema = reader.schema
        assertEquals("TestData", schema.name)
        assertEquals("com.example.test", schema.namespace)
      } finally {
        reader.close()
      }
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testWriteDefaultsSchemaNameFromMatrixName() {
    Matrix m = Matrix.builder("Orders")
        .columns(id: [1, 2], amount: [new BigDecimal("12.34"), new BigDecimal("56.78")])
        .types(Integer, BigDecimal)
        .build()

    File tmp = Files.createTempFile("avro-default-schema-name-", ".avro").toFile()
    try {
      MatrixAvroWriter.write(m, tmp, false)

      def reader = new DataFileReader<GenericRecord>(tmp, new GenericDatumReader<>())
      try {
        assertEquals("Orders", reader.schema.name)
      } finally {
        reader.close()
      }
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testWriteFallsBackToMatrixSchemaWhenMatrixNameBlank() {
    Matrix m = Matrix.builder("")
        .columns(id: [1, 2])
        .types(Integer)
        .build()

    File tmp = Files.createTempFile("avro-fallback-schema-name-", ".avro").toFile()
    try {
      MatrixAvroWriter.write(m, tmp, false)

      def reader = new DataFileReader<GenericRecord>(tmp, new GenericDatumReader<>())
      try {
        assertEquals("MatrixSchema", reader.schema.name)
      } finally {
        reader.close()
      }
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testWriteOptionsDefaultSchemaNameFromMatrixName() {
    Matrix m = Matrix.builder("Invoices")
        .columns(id: [1, 2], total: [10, 20])
        .types(Integer, Integer)
        .build()

    File tmp = Files.createTempFile("avro-options-default-schema-name-", ".avro").toFile()
    try {
      MatrixAvroWriter.write(m, tmp, new AvroWriteOptions())

      def reader = new DataFileReader<GenericRecord>(tmp, new GenericDatumReader<>())
      try {
        assertEquals("Invoices", reader.schema.name)
      } finally {
        reader.close()
      }
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testWriteWithDeflateCompression() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["id"] = (1..100).toList()
    cols["data"] = (1..100).collect { "This is test data row $it with some repeated content" }

    Matrix m = Matrix.builder("CompressionTest")
        .columns(cols)
        .types(Integer, String)
        .build()

    File uncompressed = Files.createTempFile("avro-uncompressed-", ".avro").toFile()
    File compressed = Files.createTempFile("avro-compressed-", ".avro").toFile()

    try {
      // Write without compression
      MatrixAvroWriter.write(m, uncompressed)

      // Write with deflate compression
      def options = new AvroWriteOptions()
          .compression(AvroWriteOptions.Compression.DEFLATE)
          .compressionLevel(9)
      MatrixAvroWriter.write(m, compressed, options)

      // Compressed file should be smaller
      assertTrue(compressed.length() < uncompressed.length(),
          "Compressed file (${compressed.length()}) should be smaller than uncompressed (${uncompressed.length()})")

      // Verify data can still be read correctly
      Matrix result = se.alipsa.matrix.avro.MatrixAvroReader.read(compressed)
      assertEquals(100, result.rowCount())
      assertEquals(2, result.columnCount())
    } finally {
      uncompressed.delete()
      compressed.delete()
    }
  }

  @Test
  void testInvalidCompressionLevelForSnappyFailsFast() {
    def ex = assertThrows(IllegalArgumentException) {
      new AvroWriteOptions()
          .compression(AvroWriteOptions.Compression.SNAPPY)
          .compressionLevel(6)
    }
    assertEquals("SNAPPY compression does not support compressionLevel; use -1", ex.message)
  }

  @Test
  void testInvalidCompressionLevelForFromMapFailsFast() {
    def ex = assertThrows(IllegalArgumentException) {
      AvroWriteOptions.fromMap([
          compression     : 'snappy',
          compressionLevel: 6
      ])
    }
    assertEquals("SNAPPY compression does not support compressionLevel; use -1", ex.message)
  }

  @Test
  void testInvalidSyncIntervalFailsFast() {
    def ex = assertThrows(IllegalArgumentException) {
      new AvroWriteOptions().syncInterval(16)
    }
    assertTrue(ex.message.contains("syncInterval must be 0"))
  }

  @Test
  void testInvalidSyncIntervalFromMapFailsFast() {
    def ex = assertThrows(IllegalArgumentException) {
      AvroWriteOptions.fromMap([syncInterval: 16])
    }
    assertTrue(ex.message.contains("syncInterval must be 0"))
  }

  @Test
  void testWriteBytesWithOptions() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["value"] = [new BigDecimal("123.45"), new BigDecimal("678.90")]

    Matrix m = Matrix.builder("BytesOptions")
        .columns(cols)
        .types(BigDecimal)
        .build()

    def options = new AvroWriteOptions()
        .inferPrecisionAndScale(true)
        .schemaName("DecimalData")

    byte[] bytes = MatrixAvroWriter.writeBytes(m, options)
    assertNotNull(bytes)
    assertTrue(bytes.length > 0)

    // Verify round-trip
    Matrix result = se.alipsa.matrix.avro.MatrixAvroReader.read(bytes)
    assertEquals(2, result.rowCount())
    assertEquals(new BigDecimal("123.45"), result[0, "value"])
  }

  @Test
  void testExplicitDecimalColumnSchemaOverridesInferenceDefaults() {
    Matrix m = Matrix.builder("ExplicitDecimal")
        .columns(amount: [new BigDecimal("12.340"), new BigDecimal("56.780")])
        .types(BigDecimal)
        .build()

    File tmp = Files.createTempFile("avro-explicit-decimal-", ".avro").toFile()
    try {
      MatrixAvroWriter.write(m, tmp, new AvroWriteOptions()
          .inferPrecisionAndScale(false)
          .columnSchema('amount', AvroSchemaDecl.decimal(12, 3)))

      def reader = new DataFileReader<GenericRecord>(tmp, new GenericDatumReader<>())
      try {
        Schema amountSchema = nonNullFieldSchema(reader.schema, 'amount')
        assertEquals(Schema.Type.BYTES, amountSchema.type)
        assertEquals('decimal', amountSchema.logicalType.name)
        LogicalTypes.Decimal decimal = amountSchema.logicalType as LogicalTypes.Decimal
        assertEquals(12, decimal.precision)
        assertEquals(3, decimal.scale)
      } finally {
        reader.close()
      }
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testColumnSchemaCanForceMapEncoding() {
    Matrix m = Matrix.builder("ForceMap")
        .columns(props: [[x: 1, y: 2], [x: 3, y: 4]])
        .types(Map)
        .build()

    File tmp = Files.createTempFile("avro-force-map-", ".avro").toFile()
    try {
      MatrixAvroWriter.write(m, tmp, new AvroWriteOptions()
          .columnSchema('props', AvroSchemaDecl.map(AvroSchemaDecl.type(Integer))))

      def reader = new DataFileReader<GenericRecord>(tmp, new GenericDatumReader<>())
      try {
        Schema propsSchema = nonNullFieldSchema(reader.schema, 'props')
        assertEquals(Schema.Type.MAP, propsSchema.type)
        Schema valueSchema = propsSchema.valueType.types.find { Schema it -> it.type != Schema.Type.NULL }
        assertEquals(Schema.Type.INT, valueSchema.type)
      } finally {
        reader.close()
      }
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testColumnSchemaCanForceRecordEncoding() {
    Matrix m = Matrix.builder("ForceRecord")
        .columns(props: [[x: 1], [y: 2], null])
        .types(Map)
        .build()

    File tmp = Files.createTempFile("avro-force-record-", ".avro").toFile()
    try {
      MatrixAvroWriter.write(m, tmp, new AvroWriteOptions()
          .columnSchema('props', AvroSchemaDecl.record('PropsRecord', [
              x: AvroSchemaDecl.type(Integer),
              y: AvroSchemaDecl.type(Integer)
          ])))

      def reader = new DataFileReader<GenericRecord>(tmp, new GenericDatumReader<>())
      try {
        Schema propsSchema = nonNullFieldSchema(reader.schema, 'props')
        assertEquals(Schema.Type.RECORD, propsSchema.type)
        assertEquals('PropsRecord', propsSchema.name)
        assertEquals(['x', 'y'], propsSchema.fields*.name())
      } finally {
        reader.close()
      }
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testColumnSchemaCanForceArrayElementType() {
    Matrix m = Matrix.builder("ForceArray")
        .columns(tags: [[1, 2], [3L, null]])
        .types(List)
        .build()

    File tmp = Files.createTempFile("avro-force-array-", ".avro").toFile()
    try {
      MatrixAvroWriter.write(m, tmp, new AvroWriteOptions()
          .columnSchema('tags', AvroSchemaDecl.array(AvroSchemaDecl.type(Long))))

      def reader = new DataFileReader<GenericRecord>(tmp, new GenericDatumReader<>())
      try {
        Schema tagsSchema = nonNullFieldSchema(reader.schema, 'tags')
        assertEquals(Schema.Type.ARRAY, tagsSchema.type)
        Schema elementSchema = tagsSchema.elementType.types.find { Schema it -> it.type != Schema.Type.NULL }
        assertEquals(Schema.Type.LONG, elementSchema.type)
      } finally {
        reader.close()
      }
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testWriteOptionsRoundTripToMap() {
    AvroWriteOptions options = new AvroWriteOptions()
        .inferPrecisionAndScale(true)
        .namespace("se.alipsa.matrix.roundtrip")
        .compression(AvroWriteOptions.Compression.DEFLATE)
        .compressionLevel(9)
        .syncInterval(64000)
        .columnSchema('amount', AvroSchemaDecl.decimal(10, 2))

    Map<String, ?> roundTrip = options.toMap()

    assertEquals(true, roundTrip.inferPrecisionAndScale)
    assertEquals("se.alipsa.matrix.roundtrip", roundTrip.namespace)
    assertEquals("DEFLATE", roundTrip.compression)
    assertEquals(9, roundTrip.compressionLevel)
    assertEquals(64000, roundTrip.syncInterval)
    assertFalse(roundTrip.containsKey("schemaName"))
    assertEquals('decimal', (((roundTrip.columnSchemas as Map).amount) as Map).kind)
  }

  @Test
  void testWriteWithOptionsNullValidation() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["id"] = [1]
    Matrix m = Matrix.builder("Test").columns(cols).types(Integer).build()

    File tmp = Files.createTempFile("avro-test-", ".avro").toFile()
    try {
      def ex = assertThrows(IllegalArgumentException) {
        MatrixAvroWriter.write(m, tmp, (AvroWriteOptions) null)
      }
      assertEquals("Options cannot be null", ex.message)
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testUnknownColumnSchemaFailsFast() {
    Matrix m = Matrix.builder("UnknownColumn")
        .columns(id: [1, 2])
        .types(Integer)
        .build()

    File tmp = Files.createTempFile("avro-unknown-column-schema-", ".avro").toFile()
    try {
      IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
        MatrixAvroWriter.write(m, tmp, new AvroWriteOptions()
            .columnSchema('missing', AvroSchemaDecl.type(Integer)))
      }
      assertEquals("columnSchemas['missing'] does not match any Matrix column", ex.message)
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testWriteToOutputStreamWithOptions() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["id"] = [1, 2]
    cols["name"] = ["Test1", "Test2"]

    Matrix m = Matrix.builder("StreamOptions")
        .columns(cols)
        .types(Integer, String)
        .build()

    def options = new AvroWriteOptions()
        .namespace("stream.test")
        .schemaName("StreamData")

    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    MatrixAvroWriter.write(m, baos, options)

    byte[] bytes = baos.toByteArray()
    assertTrue(bytes.length > 0)

    Matrix result = se.alipsa.matrix.avro.MatrixAvroReader.read(bytes)
    assertEquals(2, result.rowCount())
  }

  // ---------- custom exception tests ----------

  @Test
  void testValidationExceptionForNullMatrix() {
    File tmp = Files.createTempFile("avro-test-", ".avro").toFile()
    try {
      def ex = assertThrows(AvroValidationException) {
        MatrixAvroWriter.write(null, tmp)
      }
      assertEquals("matrix", ex.parameterName)
      assertNotNull(ex.suggestion)
      assertTrue(ex.message.contains("cannot be null"))
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testValidationExceptionForEmptyMatrix() {
    Matrix m = Matrix.builder("Empty").build()
    File tmp = Files.createTempFile("avro-test-", ".avro").toFile()
    try {
      def ex = assertThrows(AvroValidationException) {
        MatrixAvroWriter.write(m, tmp)
      }
      assertEquals("matrix", ex.parameterName)
      assertNotNull(ex.suggestion)
      assertTrue(ex.message.contains("at least one column"))
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testValidationExceptionForNullFileParam() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["id"] = [1, 2]
    Matrix m = Matrix.builder("Test").columns(cols).types(Integer).build()

    def ex = assertThrows(AvroValidationException) {
      MatrixAvroWriter.write(m, (File) null)
    }
    assertEquals("file", ex.parameterName)
    assertNotNull(ex.suggestion)
    assertTrue(ex.message.contains("cannot be null"))
  }

  @Test
  void testValidationExceptionForColumnSizeMismatch() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["id"] = [1, 2, 3]
    cols["name"] = ["Alice", "Bob"]

    Matrix m = Matrix.builder("Mismatch")
        .columns(cols)
        .types(Integer, String)
        .build()

    File tmp = Files.createTempFile("avro-test-", ".avro").toFile()
    try {
      def ex = assertThrows(AvroValidationException) {
        MatrixAvroWriter.write(m, tmp)
      }
      assertEquals("matrix", ex.parameterName)
      assertEquals(2, ex.rowNumber)
      assertNotNull(ex.suggestion)
      assertTrue(ex.message.contains("row: 2"))
    } finally {
      tmp.delete()
    }
  }

  @Test
  void testSchemaExceptionForTypeMismatch() {
    def cols = new LinkedHashMap<String, List<?>>()
    cols["props"] = [[a: 1], [1, 2]]

    Matrix m = Matrix.builder("TypeMismatch")
        .columns(cols)
        .types(Object)
        .build()

    File tmp = Files.createTempFile("avro-test-", ".avro").toFile()
    try {
      def ex = assertThrows(AvroSchemaException) {
        MatrixAvroWriter.write(m, tmp)
      }
      assertEquals("props", ex.columnName)
      assertEquals("RECORD", ex.expectedType)
      assertEquals("ArrayList", ex.actualType)
      assertTrue(ex.message.contains("expected"))
    } finally {
      tmp.delete()
    }
  }

  // Helper: unwrap ["null", T] to T
  private static Schema nonNullFieldSchema(Schema record, String fieldName) {
    Schema s = record.getField(fieldName).schema()
    if (s.getType() == Schema.Type.UNION) {
      for (Schema t : s.getTypes()) {
        if (t.getType() != Schema.Type.NULL) return t
      }
      fail("Union for field '$fieldName' had no non-null type")
    }
    return s
  }
}
