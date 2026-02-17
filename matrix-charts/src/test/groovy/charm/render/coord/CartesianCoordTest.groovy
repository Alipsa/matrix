package charm.render.coord

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.coord.CartesianCoord

import static org.junit.jupiter.api.Assertions.*

class CartesianCoordTest {

  @Test
  void testPassThrough() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN)
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, rowIndex: 0),
        new LayerData(x: 2, y: 20, rowIndex: 1)
    ]
    List<LayerData> result = CartesianCoord.compute(coord, data)
    assertNotNull(result)
    assertEquals(2, result.size())
    assertEquals(1, result[0].x)
    assertEquals(10, result[0].y)
    assertEquals(2, result[1].x)
    assertEquals(20, result[1].y)
  }

  @Test
  void testEmptyData() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN)
    List<LayerData> data = []
    List<LayerData> result = CartesianCoord.compute(coord, data)
    assertTrue(result.isEmpty())
  }

  @Test
  void testNullData() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN)
    List<LayerData> result = CartesianCoord.compute(coord, null)
    assertNull(result)
  }

  @Test
  void testXlimClampsValues() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN, params: [xlim: [2, 8]])
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, rowIndex: 0),
        new LayerData(x: 5, y: 20, rowIndex: 1),
        new LayerData(x: 10, y: 30, rowIndex: 2)
    ]
    List<LayerData> result = CartesianCoord.compute(coord, data)
    assertEquals(3, result.size())
    // x=1 clamped to 2
    assertEquals(0, new BigDecimal('2').compareTo(result[0].x as BigDecimal))
    // x=5 within range, unchanged
    assertEquals(5, result[1].x)
    // x=10 clamped to 8
    assertEquals(0, new BigDecimal('8').compareTo(result[2].x as BigDecimal))
  }

  @Test
  void testYlimClampsValues() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN, params: [ylim: [5, 25]])
    List<LayerData> data = [
        new LayerData(x: 1, y: 3, rowIndex: 0),
        new LayerData(x: 2, y: 15, rowIndex: 1),
        new LayerData(x: 3, y: 30, rowIndex: 2)
    ]
    List<LayerData> result = CartesianCoord.compute(coord, data)
    assertEquals(3, result.size())
    // y=3 clamped to 5
    assertEquals(0, new BigDecimal('5').compareTo(result[0].y as BigDecimal))
    // y=15 within range, unchanged
    assertEquals(15, result[1].y)
    // y=30 clamped to 25
    assertEquals(0, new BigDecimal('25').compareTo(result[2].y as BigDecimal))
  }

  @Test
  void testBothLimsClampsValues() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN, params: [xlim: [0, 10], ylim: [0, 100]])
    List<LayerData> data = [
        new LayerData(x: -5, y: -10, rowIndex: 0),
        new LayerData(x: 5, y: 50, rowIndex: 1),
        new LayerData(x: 15, y: 150, rowIndex: 2)
    ]
    List<LayerData> result = CartesianCoord.compute(coord, data)
    assertEquals(3, result.size())

    assertEquals(0, new BigDecimal('0').compareTo(result[0].x as BigDecimal))
    assertEquals(0, new BigDecimal('0').compareTo(result[0].y as BigDecimal))

    assertEquals(5, result[1].x)
    assertEquals(50, result[1].y)

    assertEquals(0, new BigDecimal('10').compareTo(result[2].x as BigDecimal))
    assertEquals(0, new BigDecimal('100').compareTo(result[2].y as BigDecimal))
  }

  @Test
  void testNoLimsPassesThrough() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN, params: [:])
    List<LayerData> data = [
        new LayerData(x: 100, y: 200, rowIndex: 0)
    ]
    List<LayerData> result = CartesianCoord.compute(coord, data)
    // No limits: pass through (same reference since no copy needed)
    assertSame(data, result)
  }

  @Test
  void testPreservesNonNumericXValues() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN, params: [xlim: [0, 10]])
    List<LayerData> data = [
        new LayerData(x: 'category', y: 10, rowIndex: 0)
    ]
    List<LayerData> result = CartesianCoord.compute(coord, data)
    assertEquals(1, result.size())
    // Non-numeric x is not clamped
    assertEquals('category', result[0].x)
  }

  @Test
  void testPreservesAllFields() {
    CoordSpec coord = new CoordSpec(type: CharmCoordType.CARTESIAN)
    LayerData datum = new LayerData(
        x: 5, y: 10, color: 'red', fill: 'blue',
        xmin: 3, xmax: 7, ymin: 8, ymax: 12,
        group: 'G1', label: 'lbl', rowIndex: 3
    )
    datum.meta.custom = 'val'
    List<LayerData> result = CartesianCoord.compute(coord, [datum])
    // No limits: same reference
    assertSame(datum, result[0])
  }
}
