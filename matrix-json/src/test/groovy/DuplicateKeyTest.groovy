import org.junit.jupiter.api.Test
import se.alipsa.matrix.json.JsonImporter
import se.alipsa.matrix.core.Matrix

import static org.junit.jupiter.api.Assertions.*

class DuplicateKeyTest {

  @Test
  void testCollidingKeys() {
    // This JSON has keys that will collide after flattening
    // "a.b" as a literal key
    // "a": {"b": ...} which flattens to "a.b"
    String json = '''[
      {"a.b": 1, "a": {"b": 2}}
    ]'''

    // Should throw an exception when duplicate keys are detected
    def exception = assertThrows(IllegalArgumentException.class, {
      JsonImporter.parse(json)
    })
    
    assertTrue(exception.message.contains("Duplicate key detected"))
    assertTrue(exception.message.contains("a.b"))
  }

  @Test
  void testMultipleCollisions() {
    String json = '''[
      {"x.y.z": 1, "x": {"y.z": 2}, "x.y": {"z": 3}}
    ]'''

    // Should throw an exception when duplicate keys are detected
    def exception = assertThrows(IllegalArgumentException.class, {
      JsonImporter.parse(json)
    })
    
    assertTrue(exception.message.contains("Duplicate key detected"))
    assertTrue(exception.message.contains("x.y.z"))
  }

  @Test
  void testNoCollision() {
    String json = '''[
      {"a": {"b": 1}, "c": 2}
    ]'''

    Matrix m = JsonImporter.parse(json)
    
    assertEquals(2, m.columnCount())
    assertEquals(["a.b", "c"], m.columnNames())
  }
}
