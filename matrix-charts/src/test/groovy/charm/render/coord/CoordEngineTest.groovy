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
  void testDispatchPolar() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.POLAR)
    List<LayerData> data = [new LayerData(x: 1, y: 2, rowIndex: 0)]
    List<LayerData> result = CoordEngine.apply(coord, data)
    assertNotNull(result)
    assertEquals(1, result.size())
    assertNotEquals(1, result[0].x)
    assertNotEquals(2, result[0].y)
  }

  @Test
  void testDispatchRadial() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.RADIAL)
    List<LayerData> data = [new LayerData(x: 0.25, y: -2, rowIndex: 0)]
    List<LayerData> result = CoordEngine.apply(coord, data)
    assertNotNull(result)
    assertEquals(1, result.size())
    // Radial clamps radius to non-negative before polar transform.
    assertEquals(0, (result[0].x as BigDecimal).compareTo(0.0))
    assertEquals(0, (result[0].y as BigDecimal).compareTo(0.0))
  }

  @Test
  void testDispatchTrans() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.TRANS, params: [x: 'log10', y: 'sqrt'])
    List<LayerData> data = [new LayerData(x: 100, y: 9, rowIndex: 0)]
    List<LayerData> result = CoordEngine.apply(coord, data)
    assertNotNull(result)
    assertEquals(1, result.size())
    assertEquals(0, (result[0].x as BigDecimal).compareTo(2.0))
    assertEquals(0, (result[0].y as BigDecimal).compareTo(3.0))
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

  @Test
  void testDispatchMap() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.MAP, params: [projection: 'mercator'])
    List<LayerData> data = [new LayerData(x: 10, y: 45, rowIndex: 0)]
    List<LayerData> result = CoordEngine.apply(coord, data)
    assertNotNull(result)
    assertEquals(1, result.size())
    assertNotEquals(10, result[0].x)
    assertNotEquals(45, result[0].y)
  }

  @Test
  void testMapAppliesLimitsBeforeProjection() {
    CoordSpec coord = new CoordSpec(
        type: CharmCoordType.MAP,
        params: [projection: 'mercator', xlim: [0, 5], ylim: [0, 30]]
    )
    List<LayerData> data = [new LayerData(x: 10, y: 45, rowIndex: 0)]

    List<LayerData> result = CoordEngine.apply(coord, data)

    assertEquals(1, result.size())
    assertEquals(Math.toRadians(5), result[0].x as double, 1.0e-9d)
    double expectedY = Math.log(Math.tan(Math.PI / 4 + Math.toRadians(30) / 2))
    assertEquals(expectedY, result[0].y as double, 1.0e-9d)
  }

  @Test
  void testDispatchQuickmap() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.QUICKMAP)
    List<LayerData> data = [new LayerData(x: 10, y: 45, rowIndex: 0)]
    List<LayerData> result = CoordEngine.apply(coord, data)
    assertNotNull(result)
    assertEquals(1, result.size())
    assertNotEquals(45, result[0].y)
  }

  @Test
  void testQuickmapAppliesYLimitsBeforeScaling() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.QUICKMAP, params: [ylim: [0, 30]])
    List<LayerData> data = [new LayerData(x: 10, y: 45, rowIndex: 0)]

    List<LayerData> result = CoordEngine.apply(coord, data)

    assertEquals(1, result.size())
    double expectedY = 30 * Math.cos(Math.toRadians(30))
    assertEquals(expectedY, result[0].y as double, 1.0e-9d)
  }

  @Test
  void testDispatchSf() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.SF)
    List<LayerData> data = [new LayerData(x: 1, y: 2, rowIndex: 0)]
    List<LayerData> result = CoordEngine.apply(coord, data)
    assertNotNull(result)
    assertEquals(1, result.size())
    assertEquals(1, result[0].x)
    assertEquals(2, result[0].y)
  }
}
