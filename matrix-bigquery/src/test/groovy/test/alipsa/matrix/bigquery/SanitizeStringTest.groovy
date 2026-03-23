package test.alipsa.matrix.bigquery

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertTrue

import groovy.transform.CompileStatic

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test

import se.alipsa.matrix.bigquery.Bq

import java.io.StringWriter

@CompileStatic
class SanitizeStringTest {

  @Test
  void testSanitizeStringNull() {
    assertNull(Bq.sanitizeString(null))
  }

  @Test
  void testSanitizeStringPreservesNormalStrings() {
    assertEquals("Hello World", Bq.sanitizeString("Hello World"))
    assertEquals("Test 123", Bq.sanitizeString("Test 123"))
    assertEquals("", Bq.sanitizeString(""))
  }

  @Test
  void testSanitizeStringPreservesTabsNewlinesAndFormatCharacters() {
    String value = "Hello\tWorld\nZero\u200DWidth"

    assertEquals(value, Bq.sanitizeString(value))
  }

  @Test
  void testSanitizeStringPreservesNulCharacters() {
    String value = "Hello\u0000World"

    assertEquals(value, Bq.sanitizeString(value))
  }

  @Test
  void testSanitizeStringNonString() {
    assertEquals("123", Bq.sanitizeString(123))
    assertEquals("true", Bq.sanitizeString(true))
    assertEquals("45.67", Bq.sanitizeString(45.67))
  }

  @Test
  void testJacksonEscapesControlCharactersWithoutDataLoss() {
    String value = "Hello\tWorld\nNull\u0000End"
    String payload = writeJsonPayload(value)

    assertTrue(payload.contains("\\t"))
    assertTrue(payload.contains("\\n"))
    assertTrue(payload.contains("\\u0000"))

    String roundTrip = new ObjectMapper().readTree(payload).get('text').asText()
    assertEquals(value, roundTrip)
  }

  private static String writeJsonPayload(String value) {
    StringWriter output = new StringWriter()
    JsonGenerator json = new JsonFactory().createGenerator(output)
    json.writeStartObject()
    json.writeStringField('text', Bq.sanitizeString(value))
    json.writeEndObject()
    json.close()
    output.toString()
  }
}
