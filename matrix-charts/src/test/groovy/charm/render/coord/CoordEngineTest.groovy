package charm.render.coord

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.coord.CoordEngine

import static org.junit.jupiter.api.Assertions.*

class CoordEngineTest {

  @Test
  void testDispatchCartesian() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN)
    List<LayerData> data = [new LayerData(x: 1, y: 2, rowIndex: 0)]
    List<LayerData> result = CoordEngine.apply(coord, data)
    assertNotNull(result)
    assertEquals(1, result.size())
    assertEquals(1, result[0].x)
    assertEquals(2, result[0].y)
  }

  @Test
  void testDispatchFlip() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FLIP)
    List<LayerData> data = [new LayerData(x: 1, y: 2, rowIndex: 0)]
    List<LayerData> result = CoordEngine.apply(coord, data)
    assertNotNull(result)
    assertEquals(1, result.size())
    // Flip swaps x and y
    assertEquals(2, result[0].x)
    assertEquals(1, result[0].y)
  }

  @Test
  void testDispatchFixed() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.FIXED, params: [ratio: 2.0])
    List<LayerData> data = [new LayerData(x: 1, y: 2, rowIndex: 0)]
    List<LayerData> result = CoordEngine.apply(coord, data)
    assertNotNull(result)
    assertEquals(1, result.size())
    // Fixed passes through like Cartesian at data level
    assertEquals(1, result[0].x)
    assertEquals(2, result[0].y)
  }

  @Test
  void testUnimplementedCoordFallsBackToCartesian() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.POLAR)
    List<LayerData> data = [new LayerData(x: 1, y: 2, rowIndex: 0)]
    List<LayerData> result = CoordEngine.apply(coord, data)
    assertNotNull(result)
    assertEquals(1, result.size())
    assertEquals(1, result[0].x)
    assertEquals(2, result[0].y)
  }

  @Test
  void testNullCoordSpecDefaultsToCartesian() {
    List<LayerData> data = [new LayerData(x: 1, y: 2, rowIndex: 0)]
    List<LayerData> result = CoordEngine.apply(null, data)
    assertNotNull(result)
    assertEquals(1, result.size())
    assertEquals(1, result[0].x)
    assertEquals(2, result[0].y)
  }
}
