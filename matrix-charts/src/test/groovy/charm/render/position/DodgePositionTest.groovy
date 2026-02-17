package charm.render.position

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.position.DodgePosition

import static org.junit.jupiter.api.Assertions.*

class DodgePositionTest {

  @Test
  void testDodgeGroupsByFill() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, fill: 'A', rowIndex: 0),
        new LayerData(x: 1, y: 20, fill: 'B', rowIndex: 1),
        new LayerData(x: 2, y: 15, fill: 'A', rowIndex: 2),
        new LayerData(x: 2, y: 25, fill: 'B', rowIndex: 3)
    ]
    List<LayerData> result = DodgePosition.compute(layer, data)
    assertEquals(4, result.size())

    // Within x=1, group A and B should have different x positions
    BigDecimal x1a = result[0].x as BigDecimal
    BigDecimal x1b = result[1].x as BigDecimal
    assertNotEquals(x1a, x1b)

    // Both should be offset from original x=1
    assertTrue(x1a < 1 || x1b < 1)
    assertTrue(x1a > 1 || x1b > 1)

    // y values should be preserved
    assertEquals(10, result[0].y)
    assertEquals(20, result[1].y)
  }

  @Test
  void testDodgeWithWidthParam() {
    PositionSpec pos = PositionSpec.of(CharmPositionType.DODGE, [width: 0.5])
    LayerSpec layer = new LayerSpec(
        GeomSpec.of(CharmGeomType.BAR),
        StatSpec.of(CharmStatType.IDENTITY),
        null, true, pos, [:]
    )
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, fill: 'A', rowIndex: 0),
        new LayerData(x: 1, y: 20, fill: 'B', rowIndex: 1)
    ]
    List<LayerData> result = DodgePosition.compute(layer, data)
    assertEquals(2, result.size())

    // Width=0.5 means offset range is [-0.25, 0.25]
    BigDecimal x0 = result[0].x as BigDecimal
    BigDecimal x1 = result[1].x as BigDecimal
    BigDecimal spread = (x1 - x0).abs()
    // With width=0.5 and 2 groups, spread should be 0.25
    assertEquals(0, new BigDecimal('0.25').compareTo(spread))
  }

  @Test
  void testDodgeSingleGroupPassthrough() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, fill: 'A', rowIndex: 0),
        new LayerData(x: 1, y: 20, fill: 'A', rowIndex: 1)
    ]
    List<LayerData> result = DodgePosition.compute(layer, data)
    assertEquals(2, result.size())

    // Single group: x values should be unchanged (copied, not same reference)
    assertEquals(1, result[0].x)
    assertEquals(1, result[1].x)
  }

  @Test
  void testDodgeUsesGroupField() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, group: 'G1', fill: 'same', rowIndex: 0),
        new LayerData(x: 1, y: 20, group: 'G2', fill: 'same', rowIndex: 1)
    ]
    List<LayerData> result = DodgePosition.compute(layer, data)
    assertEquals(2, result.size())

    // Group field takes priority over fill for grouping
    BigDecimal x0 = result[0].x as BigDecimal
    BigDecimal x1 = result[1].x as BigDecimal
    assertNotEquals(x0, x1)
  }

  @Test
  void testDodgeEmptyData() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = []
    List<LayerData> result = DodgePosition.compute(layer, data)
    assertTrue(result.isEmpty())
  }

  @Test
  void testDodgePreservesOtherFields() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 1, y: 10, fill: 'A', color: 'red', alpha: 0.5, rowIndex: 0),
        new LayerData(x: 1, y: 20, fill: 'B', color: 'blue', alpha: 0.8, rowIndex: 1)
    ]
    List<LayerData> result = DodgePosition.compute(layer, data)
    assertEquals('red', result[0].color)
    assertEquals(0.5, result[0].alpha)
    assertEquals('blue', result[1].color)
    assertEquals(0.8, result[1].alpha)
  }

  private static LayerSpec makeLayer(Map<String, Object> params = [:]) {
    new LayerSpec(
        GeomSpec.of(CharmGeomType.BAR),
        StatSpec.of(CharmStatType.IDENTITY),
        null, true,
        PositionSpec.of(CharmPositionType.DODGE),
        params
    )
  }
}
