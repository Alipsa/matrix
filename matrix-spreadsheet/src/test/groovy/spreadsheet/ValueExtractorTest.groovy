package spreadsheet

import org.junit.jupiter.api.Test
import se.alipsa.matrix.spreadsheet.ValueExtractor

import static org.junit.jupiter.api.Assertions.*

class ValueExtractorTest {

  // Concrete implementation for testing
  static class TestValueExtractor extends ValueExtractor {}

  @Test
  void testGetDoubleWithPercentage() {
    def extractor = new TestValueExtractor()

    // Test percentage parsing (the bug that was fixed in 1.1)
    Double result = extractor.getDouble("50%")
    assertNotNull(result, "Percentage should be parsed successfully")
    assertEquals(0.5, result, 0.0001, "50% should equal 0.5")

    result = extractor.getDouble("100%")
    assertNotNull(result, "100% should be parsed successfully")
    assertEquals(1.0, result, 0.0001, "100% should equal 1.0")

    result = extractor.getDouble("25%")
    assertNotNull(result, "25% should be parsed successfully")
    assertEquals(0.25, result, 0.0001, "25% should equal 0.25")
  }

  @Test
  void testGetDoubleWithNumber() {
    def extractor = new TestValueExtractor()

    assertEquals(42.0, extractor.getDouble(42.0))
    assertEquals(3.14, extractor.getDouble("3.14"), 0.0001)
    assertEquals(-10.5, extractor.getDouble("-10.5"), 0.0001)
  }

  @Test
  void testGetDoubleWithNull() {
    def extractor = new TestValueExtractor()
    assertNull(extractor.getDouble(null))
  }

  @Test
  void testGetDoubleWithInvalidString() {
    def extractor = new TestValueExtractor()
    assertNull(extractor.getDouble("not a number"))
  }

  @Test
  void testGetPercentage() {
    def extractor = new TestValueExtractor()

    assertEquals(0.5, extractor.getPercentage("50%"), 0.0001)
    assertEquals(1.0, extractor.getPercentage("100%"), 0.0001)
    assertEquals(0.75, extractor.getPercentage(0.75))  // Pass-through for doubles
    assertNull(extractor.getPercentage(null))
  }

  @Test
  void testGetInt() {
    assertEquals(42, ValueExtractor.getInt(42))
    assertEquals(42, ValueExtractor.getInt(42.4d))  // Rounds (double)
    assertEquals(43, ValueExtractor.getInt(42.6d))  // Rounds (double)
    assertEquals(42, ValueExtractor.getInt("42"))
    assertEquals(1, ValueExtractor.getInt(true))
    assertEquals(0, ValueExtractor.getInt(false))
    assertNull(ValueExtractor.getInt(null))
  }

  @Test
  void testGetLong() {
    def extractor = new TestValueExtractor()

    assertEquals(42L, extractor.getLong(42))
    assertEquals(42L, extractor.getLong(42.4d))  // Rounds (double)
    assertEquals(43L, extractor.getLong(42.6d))  // Rounds (double)
    assertEquals(42L, extractor.getLong("42"))
    assertEquals(1L, extractor.getLong(true))
    assertEquals(0L, extractor.getLong(false))
    assertNull(extractor.getLong(null))
  }

  @Test
  void testGetBoolean() {
    def extractor = new TestValueExtractor()

    // True cases
    assertTrue(extractor.getBoolean(true))
    assertTrue(extractor.getBoolean("true"))
    assertTrue(extractor.getBoolean("TRUE"))
    assertTrue(extractor.getBoolean("yes"))
    assertTrue(extractor.getBoolean("y"))
    assertTrue(extractor.getBoolean("1"))
    assertTrue(extractor.getBoolean(1))
    assertTrue(extractor.getBoolean(1.0))
    assertTrue(extractor.getBoolean("on"))
    assertTrue(extractor.getBoolean("ja"))
    assertTrue(extractor.getBoolean("j"))

    // False cases
    assertFalse(extractor.getBoolean(false))
    assertFalse(extractor.getBoolean("false"))
    assertFalse(extractor.getBoolean("no"))
    assertFalse(extractor.getBoolean("0"))
    assertFalse(extractor.getBoolean(0))
    assertFalse(extractor.getBoolean(0.0))

    // Null cases
    assertNull(extractor.getBoolean(null))
    assertNull(extractor.getBoolean(""))
  }

  @Test
  void testGetString() {
    assertEquals("hello", ValueExtractor.getString("hello"))
    assertEquals("42", ValueExtractor.getString(42))
    assertEquals("true", ValueExtractor.getString(true))
    assertNull(ValueExtractor.getString(null))
  }
}
