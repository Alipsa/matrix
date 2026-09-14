import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertThrows

import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonReadOptions
import se.alipsa.matrix.json.JsonWriteOptions
import se.alipsa.matrix.json.JsonWriter

class JsonOptionsTest {

  @Test
  void validatesColumnFormatterEntriesAcrossOptionsAndWriter() {
    assertEquals('columnFormatters column name cannot be null or blank', assertThrows(IllegalArgumentException) {
      JsonWriteOptions.fromMap([columnFormatters: [(null): { it }]])
    }.message)
    assertThrows(IllegalArgumentException) {
      JsonWriteOptions.fromMap([columnFormatters: [(' '): { it }]])
    }
    assertThrows(IllegalArgumentException) {
      JsonWriteOptions.fromMap([columnFormatters: [(1): { it }]])
    }
    assertThrows(IllegalArgumentException) {
      JsonWriteOptions.fromMap([columnFormatters: [a: 'not a closure']])
    }
    assertThrows(IllegalArgumentException) {
      JsonWriteOptions.fromMap([columnFormatters: [a: null]])
    }
    new JsonWriteOptions().columnFormatters([a: { it }])
    JsonWriter.write(Matrix.builder().data(a: [1]).build()).formatter('a') { it }
  }

  @Test
  void validatesDatePatternsAcrossOptionsEntryPoints() {
    [null, '', 'not a pattern'].each { String pattern ->
      assertThrows(IllegalArgumentException) { new JsonWriteOptions().dateFormat(pattern) }
    }
    ['', 'not a pattern'].each { String pattern ->
      assertThrows(IllegalArgumentException) { new JsonWriteOptions().dateTimeFormat(pattern) }
    }
    new JsonWriteOptions().dateTimeFormat(null)
    assertThrows(IllegalArgumentException) { JsonWriteOptions.fromMap([dateFormat: '']) }
    assertThrows(IllegalArgumentException) { JsonWriteOptions.fromMap([dateTimeFormat: 'not a pattern']) }

    JsonWriteOptions options = JsonWriteOptions.fromMap([dateTimeFormat: 'HH:mm'])
    assertEquals('HH:mm', options.toMap().dateTimeFormat)
    assertFalse(new JsonWriteOptions().toMap().containsKey('dateTimeFormat'))
  }

  @Test
  void validatesValuesAssignedThroughProperties() {
    JsonWriteOptions writeOptions = new JsonWriteOptions()
    assertThrows(IllegalArgumentException) { writeOptions.dateFormat = '' }
    assertThrows(IllegalArgumentException) { writeOptions.dateTimeFormat = 'not a pattern' }
    assertThrows(IllegalArgumentException) { writeOptions.columnFormatters = [(null): { it }] }

    JsonReadOptions readOptions = new JsonReadOptions()
    assertThrows(IllegalArgumentException) { readOptions.types = ['not a class'] }
  }

  @Test
  void validatesTypesOptions() {
    assertNull(JsonReadOptions.fromMap([types: null]).types)
    assertThrows(IllegalArgumentException) { JsonReadOptions.fromMap([types: 'String']) }
    assertThrows(IllegalArgumentException) { JsonReadOptions.fromMap([types: [String, 'not a class']]) }
    assertThrows(IllegalArgumentException) { JsonReadOptions.fromMap([types: [String] as Object[]]) }
    assertThrows(IllegalArgumentException) { JsonReadOptions.fromMap([types: [1, 2] as int[]]) }
    assertThrows(IllegalArgumentException) { new JsonReadOptions().types([String, null]) }
    new JsonReadOptions().types([String, Integer])
    assertEquals([String, Integer], JsonReadOptions.fromMap([types: [String, Integer] as Class[]]).types)
  }
}
