import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertIterableEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static se.alipsa.matrix.core.util.TypeHelper.createObjectTypes
import static se.alipsa.matrix.core.util.TypeHelper.sanitizeTypes

import org.junit.jupiter.api.Test

class TypeHelperTest {

  @Test
  void testCreateObjectTypesUsesTemplateSize() {
    assertIterableEquals([Object, Object, Object], createObjectTypes(['a', 'b', 'c']))
  }

  @Test
  void testSanitizeTypesUsesObjectWhenNoTypesProvided() {
    assertIterableEquals([Object, Object], sanitizeTypes(['id', 'name']))
  }

  @Test
  void testSanitizeTypesWrapsPrimitiveTypes() {
    assertIterableEquals([Integer, String, Double], sanitizeTypes(['id', 'name', 'score'], [int, String, double]))
  }

  @Test
  void testSanitizeTypesUsesObjectsWhenExplicitTypesListIsEmpty() {
    assertIterableEquals([Object, Object], sanitizeTypes(['id', 'name'], []))
  }

  @Test
  void testSanitizeTypesRejectsMismatchedTypeCount() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
      sanitizeTypes(['id', 'name'], [Integer])
    }

    assertEquals('Number of columns (2) differs from number of datatypes provided (1)', ex.message)
  }
}
