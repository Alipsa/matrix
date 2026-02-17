package charm.render.position

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.*
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.render.position.StackPosition

import static org.junit.jupiter.api.Assertions.*

class StackPositionTest {

  @Test
  void testStackSetsYminYmax() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 'A', y: 10, rowIndex: 0),
        new LayerData(x: 'A', y: 20, rowIndex: 1),
        new LayerData(x: 'A', y: 30, rowIndex: 2)
    ]
    List<LayerData> result = StackPosition.compute(layer, data)
    assertEquals(3, result.size())

    // First datum: ymin=0, ymax=10
    assertEquals(0, new BigDecimal('0').compareTo(result[0].ymin as BigDecimal))
    assertEquals(0, new BigDecimal('10').compareTo(result[0].ymax as BigDecimal))

    // Second datum: ymin=10, ymax=30
    assertEquals(0, new BigDecimal('10').compareTo(result[1].ymin as BigDecimal))
    assertEquals(0, new BigDecimal('30').compareTo(result[1].ymax as BigDecimal))

    // Third datum: ymin=30, ymax=60
    assertEquals(0, new BigDecimal('30').compareTo(result[2].ymin as BigDecimal))
    assertEquals(0, new BigDecimal('60').compareTo(result[2].ymax as BigDecimal))
  }

  @Test
  void testStackSetsYToCenter() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 'A', y: 10, rowIndex: 0),
        new LayerData(x: 'A', y: 20, rowIndex: 1)
    ]
    List<LayerData> result = StackPosition.compute(layer, data)

    // First: y = (0 + 10) / 2 = 5
    assertEquals(0, new BigDecimal('5').compareTo(result[0].y as BigDecimal))

    // Second: y = (10 + 30) / 2 = 20
    assertEquals(0, new BigDecimal('20').compareTo(result[1].y as BigDecimal))
  }

  @Test
  void testStackGroupsByXValue() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 'A', y: 10, rowIndex: 0),
        new LayerData(x: 'B', y: 20, rowIndex: 1),
        new LayerData(x: 'A', y: 30, rowIndex: 2)
    ]
    List<LayerData> result = StackPosition.compute(layer, data)
    assertEquals(3, result.size())

    // Grouping by x places A items together, then B
    // A group stacks: first A: ymin=0, ymax=10; second A: ymin=10, ymax=40
    assertEquals(0, new BigDecimal('0').compareTo(result[0].ymin as BigDecimal))
    assertEquals(0, new BigDecimal('10').compareTo(result[0].ymax as BigDecimal))

    // Second A: stacked on top of first A
    assertEquals(0, new BigDecimal('10').compareTo(result[1].ymin as BigDecimal))
    assertEquals(0, new BigDecimal('40').compareTo(result[1].ymax as BigDecimal))

    // B group is separate: ymin=0, ymax=20
    assertEquals(0, new BigDecimal('0').compareTo(result[2].ymin as BigDecimal))
    assertEquals(0, new BigDecimal('20').compareTo(result[2].ymax as BigDecimal))
  }

  @Test
  void testStackReverse() {
    PositionSpec pos = PositionSpec.of(CharmPositionType.STACK, [reverse: true])
    LayerSpec layer = new LayerSpec(
        GeomSpec.of(CharmGeomType.BAR),
        StatSpec.of(CharmStatType.IDENTITY),
        null, true, pos, [:]
    )
    List<LayerData> data = [
        new LayerData(x: 'A', y: 10, fill: 'X', rowIndex: 0),
        new LayerData(x: 'A', y: 20, fill: 'Y', rowIndex: 1)
    ]
    List<LayerData> result = StackPosition.compute(layer, data)
    assertEquals(2, result.size())

    // With reverse, the list is reversed so Y (y=20) is processed first, then X (y=10)
    // Result order follows processing order: [Y, X]
    // Y: ymin=0, ymax=20
    assertEquals(0, new BigDecimal('0').compareTo(result[0].ymin as BigDecimal))
    assertEquals(0, new BigDecimal('20').compareTo(result[0].ymax as BigDecimal))
    // X: ymin=20, ymax=30
    assertEquals(0, new BigDecimal('20').compareTo(result[1].ymin as BigDecimal))
    assertEquals(0, new BigDecimal('30').compareTo(result[1].ymax as BigDecimal))
  }

  @Test
  void testStackEmptyData() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = []
    List<LayerData> result = StackPosition.compute(layer, data)
    assertTrue(result.isEmpty())
  }

  @Test
  void testStackNullData() {
    LayerSpec layer = makeLayer()
    List<LayerData> result = StackPosition.compute(layer, null)
    assertNull(result)
  }

  @Test
  void testStackPreservesXAndOtherFields() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 'Cat', y: 10, fill: 'blue', color: 'red', rowIndex: 5)
    ]
    List<LayerData> result = StackPosition.compute(layer, data)
    assertEquals(1, result.size())
    assertEquals('Cat', result[0].x)
    assertEquals('blue', result[0].fill)
    assertEquals('red', result[0].color)
    assertEquals(5, result[0].rowIndex)
  }

  @Test
  void testStackWithNullY() {
    LayerSpec layer = makeLayer()
    List<LayerData> data = [
        new LayerData(x: 'A', y: null, rowIndex: 0),
        new LayerData(x: 'A', y: 10, rowIndex: 1)
    ]
    List<LayerData> result = StackPosition.compute(layer, data)
    assertEquals(2, result.size())

    // null y treated as 0: ymin=0, ymax=0
    assertEquals(0, new BigDecimal('0').compareTo(result[0].ymin as BigDecimal))
    assertEquals(0, new BigDecimal('0').compareTo(result[0].ymax as BigDecimal))

    // Second: ymin=0, ymax=10
    assertEquals(0, new BigDecimal('0').compareTo(result[1].ymin as BigDecimal))
    assertEquals(0, new BigDecimal('10').compareTo(result[1].ymax as BigDecimal))
  }

  private static LayerSpec makeLayer(Map<String, Object> params = [:]) {
    new LayerSpec(
        GeomSpec.of(CharmGeomType.BAR),
        StatSpec.of(CharmStatType.IDENTITY),
        null, true,
        PositionSpec.of(CharmPositionType.STACK),
        params
    )
  }
}
