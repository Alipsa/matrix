package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.alipsa.matrix.avro.MatrixAvroReader
import se.alipsa.matrix.avro.MatrixAvroWriter
import se.alipsa.matrix.core.MatrixAssertions

import static org.junit.jupiter.api.Assertions.assertEquals

/** Covers Avro inferred schema, explicit-name, optional values, and byte overloads. */
@Tag('avro')
class AvroApiIT implements ApiItSupport {

  @Test
  void avroRoundTripsTypedAndOptionalColumns() {
    def data = se.alipsa.matrix.core.Matrix.builder([id: [1, 2], label: ['a', null], when: [java.time.LocalDate.of(2024, 1, 1), null]],
        [Integer, String, java.time.LocalDate], 'avro').build()
    byte[] bytes = MatrixAvroWriter.writeBytes(data, true)
    def restored = MatrixAvroReader.read(bytes, 'restored')
    assertEquals('restored', restored.matrixName)
    assertEquals(data.columnNames(), restored.columnNames())
    MatrixAssertions.assertContentMatches(data, restored, data.diff(restored))
  }
}
