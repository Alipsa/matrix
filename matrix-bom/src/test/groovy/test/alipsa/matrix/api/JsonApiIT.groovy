package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonReader
import se.alipsa.matrix.json.JsonWriter

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

/** Covers JSON indentation, nested values, and Matrix/Grid round trips. */
@Tag('json')
class JsonApiIT implements ApiItSupport {

  @Test
  void jsonRoundTripsNestedValuesAndGridData() {
    Matrix data = Matrix.builder([id: [1, 2], payload: [[a: 1], [b: [2, 3]]]], [Integer, Object], 'json').build()
    String json = JsonWriter.write(data).indent().asString()
    assertTrue(json.contains('\n'))
    Matrix restored = JsonReader.read(json)
    assertEquals(data.rowCount(), restored.rowCount())
    assertEquals(data.columnNames(), restored.columnNames())
    Matrix zeroColumns = Matrix.builder('empty').rows([[], []]).build()
    assertEquals(2, JsonReader.read(JsonWriter.write(zeroColumns).asString()).rowCount())
    Grid grid = new Grid([[1, 2], [3, 4]])
    assertEquals(2, JsonReader.read(JsonWriter.write(Matrix.builder('grid').data(grid).build()).asString()).rowCount())
  }
}
