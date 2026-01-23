package test.alipsa.matrix.avro

import org.apache.avro.LogicalTypes
import org.apache.avro.Schema
import org.apache.avro.file.DataFileReader
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.avro.MatrixAvroWriter

import java.nio.file.Files
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

    byte[] avroBytes = MatrixAvroWriter.writeBytes(m, false)
    assertNotNull(avroBytes)
    assertTrue(avroBytes.length > 0)
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
