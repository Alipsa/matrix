package test.alipsa.matrix.avro

import org.junit.jupiter.api.Test
import se.alipsa.matrix.avro.exceptions.AvroConversionException
import se.alipsa.matrix.avro.exceptions.AvroSchemaException

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class AvroExceptionTest {

  @Test
  void testConversionExceptionMessageIncludesContext() {
    String longValue = "x" * 60
    def ex = new AvroConversionException("Failed", "colA", 3, "String", "INT", longValue)

    assertTrue(ex.message.contains("Failed"))
    assertTrue(ex.message.contains("column: colA"))
    assertTrue(ex.message.contains("row: 3"))
    assertTrue(ex.message.contains("String -> INT"))
    assertTrue(ex.message.contains("value: "))
    assertTrue(ex.message.contains("..."))
  }

  @Test
  void testSchemaExceptionMessageIncludesExpectedAndActual() {
    def ex = new AvroSchemaException("Schema mismatch", "colA", "INT", "String")

    assertEquals("colA", ex.columnName)
    assertEquals("INT", ex.expectedType)
    assertEquals("String", ex.actualType)
    assertTrue(ex.message.contains("expected: INT"))
    assertTrue(ex.message.contains("actual: String"))
  }
}
