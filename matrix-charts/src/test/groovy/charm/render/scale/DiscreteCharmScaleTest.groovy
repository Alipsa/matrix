package charm.render.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.render.scale.DiscreteCharmScale

import static org.junit.jupiter.api.Assertions.*

class DiscreteCharmScaleTest {

  @Test
  void testLevelMappingToPixelCoordinates() {
    DiscreteCharmScale scale = new DiscreteCharmScale(
        scaleSpec: Scale.discrete(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        levels: ['A', 'B', 'C']
    )

    // Each band is 100px wide, centers at 50, 150, 250
    assertEquals(50.0, scale.transform('A'))
    assertEquals(150.0, scale.transform('B'))
    assertEquals(250.0, scale.transform('C'))
  }

  @Test
  void testBandwidth() {
    DiscreteCharmScale scale = new DiscreteCharmScale(
        scaleSpec: Scale.discrete(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        levels: ['A', 'B', 'C']
    )

    assertEquals(100.0, scale.bandwidth)
  }

  @Test
  void testBandwidthWithSingleLevel() {
    DiscreteCharmScale scale = new DiscreteCharmScale(
        scaleSpec: Scale.discrete(),
        rangeStart: 0.0,
        rangeEnd: 400.0,
        levels: ['only']
    )

    assertEquals(400.0, scale.bandwidth)
    assertEquals(200.0, scale.transform('only'))
  }

  @Test
  void testBandwidthWithEmptyLevels() {
    DiscreteCharmScale scale = new DiscreteCharmScale(
        scaleSpec: Scale.discrete(),
        rangeStart: 0.0,
        rangeEnd: 400.0,
        levels: []
    )

    assertEquals(0, scale.bandwidth)
  }

  @Test
  void testTransformUnknownLevelReturnsNull() {
    DiscreteCharmScale scale = new DiscreteCharmScale(
        scaleSpec: Scale.discrete(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        levels: ['A', 'B', 'C']
    )

    assertNull(scale.transform('D'))
  }

  @Test
  void testTransformNullReturnsNull() {
    DiscreteCharmScale scale = new DiscreteCharmScale(
        scaleSpec: Scale.discrete(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        levels: ['A', 'B']
    )

    assertNull(scale.transform(null))
  }

  @Test
  void testTransformEmptyLevelsReturnsNull() {
    DiscreteCharmScale scale = new DiscreteCharmScale(
        scaleSpec: Scale.discrete(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        levels: []
    )

    assertNull(scale.transform('A'))
  }

  @Test
  void testTicksReturnsLevels() {
    DiscreteCharmScale scale = new DiscreteCharmScale(
        scaleSpec: Scale.discrete(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        levels: ['X', 'Y', 'Z']
    )

    List<Object> ticks = scale.ticks(5)
    assertEquals(['X', 'Y', 'Z'], ticks)
  }

  @Test
  void testTickLabelsReturnsLevels() {
    DiscreteCharmScale scale = new DiscreteCharmScale(
        scaleSpec: Scale.discrete(),
        rangeStart: 0.0,
        rangeEnd: 300.0,
        levels: ['cat', 'dog', 'bird']
    )

    List<String> labels = scale.tickLabels(5)
    assertEquals(['cat', 'dog', 'bird'], labels)
  }

  @Test
  void testIsDiscreteReturnsTrue() {
    DiscreteCharmScale scale = new DiscreteCharmScale(
        scaleSpec: Scale.discrete(),
        rangeStart: 0.0,
        rangeEnd: 100.0,
        levels: ['A']
    )

    assertTrue(scale.isDiscrete())
  }

  @Test
  void testTransformWithNumericValueAsString() {
    // Numbers are converted to their string representation for lookup
    DiscreteCharmScale scale = new DiscreteCharmScale(
        scaleSpec: Scale.discrete(),
        rangeStart: 0.0,
        rangeEnd: 200.0,
        levels: ['1', '2']
    )

    assertEquals(50.0, scale.transform(1))
    assertEquals(150.0, scale.transform(2))
  }

  @Test
  void testLevelOrderIsPreserved() {
    DiscreteCharmScale scale = new DiscreteCharmScale(
        scaleSpec: Scale.discrete(),
        rangeStart: 0.0,
        rangeEnd: 400.0,
        levels: ['Z', 'A', 'M', 'B']
    )

    // Z is first level, should be at the first band center
    BigDecimal zPos = scale.transform('Z')
    BigDecimal aPos = scale.transform('A')
    BigDecimal mPos = scale.transform('M')
    BigDecimal bPos = scale.transform('B')

    assertTrue(zPos < aPos)
    assertTrue(aPos < mPos)
    assertTrue(mPos < bPos)
  }
}
