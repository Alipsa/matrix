package charm.render.scale

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.Scale
import se.alipsa.matrix.charm.render.scale.BinnedCharmScale
import se.alipsa.matrix.charm.render.scale.ColorCharmScale
import se.alipsa.matrix.charm.render.scale.ContinuousCharmScale
import se.alipsa.matrix.charm.render.scale.DiscreteCharmScale
import se.alipsa.matrix.charm.render.scale.ScaleEngine

import java.time.LocalDate

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class ScaleBreakLabelRuntimeTest {

  private static void assertNumericTicks(List<Object> actual, List<Number> expected) {
    assertEquals(expected.size(), actual.size())
    expected.eachWithIndex { Number value, int idx ->
      assertTrue(((actual[idx] as BigDecimal).compareTo(value as BigDecimal)) == 0)
    }
  }

  @Test
  void testContinuousScaleHonorsConfiguredBreaksAndLabels() {
    Scale spec = Scale.continuous()
    spec.breaks = [0, 50, 100]
    spec.labels = ['low', 'mid', 'high']

    ContinuousCharmScale scale = ScaleEngine.trainPositionalScale([0, 25, 50, 100], spec, 0, 200) as ContinuousCharmScale

    assertNumericTicks(scale.ticks(5), [0, 50, 100])
    assertEquals(['low', 'mid', 'high'], scale.tickLabels(5))
  }

  @Test
  void testBinnedScaleHonorsConfiguredBreaksAndLabels() {
    Scale spec = Scale.binned()
    spec.breaks = [0, 10, 20]
    spec.labels = ['start', 'mid', 'end']

    BinnedCharmScale scale = ScaleEngine.trainPositionalScale((0..20).collect { it }, spec, 0, 100) as BinnedCharmScale

    assertNumericTicks(scale.ticks(5), [0, 10, 20])
    assertEquals(['start', 'mid', 'end'], scale.tickLabels(5))
  }

  @Test
  void testDiscreteScaleHonorsConfiguredBreaksAndLabels() {
    Scale spec = Scale.discrete()
    spec.breaks = ['B', 'D']
    spec.labels = ['Bee', 'Dee']

    DiscreteCharmScale scale = ScaleEngine.trainPositionalScale(['A', 'B', 'C', 'D'], spec, 0, 100) as DiscreteCharmScale

    assertEquals(['B', 'D'], scale.ticks(5))
    assertEquals(['Bee', 'Dee'], scale.tickLabels(5))
  }

  @Test
  void testColorScaleHonorsConfiguredBreaksAndLabelsForDiscrete() {
    Scale spec = Scale.discrete()
    spec.breaks = ['beta']
    spec.labels = ['B']

    ColorCharmScale scale = ScaleEngine.trainColorScale(['alpha', 'beta', 'gamma'], spec)

    assertEquals(['beta'], scale.ticks(5))
    assertEquals(['B'], scale.tickLabels(5))
  }

  @Test
  void testColorScaleHonorsConfiguredBreaksAndLabelsForContinuous() {
    Scale spec = Scale.gradient('#000000', '#ffffff')
    spec.breaks = [10, 20]
    spec.labels = ['ten', 'twenty']

    ColorCharmScale scale = ScaleEngine.trainColorScale([0, 10, 20, 30], spec)

    assertNumericTicks(scale.ticks(5), [10, 20])
    assertEquals(['ten', 'twenty'], scale.tickLabels(5))
    assertTrue(scale.colorFor(10) != null)
  }

  @Test
  void testDiscreteScaleFilteredBreaksKeepLabelPairing() {
    Scale spec = Scale.discrete()
    spec.breaks = ['B', 'X', 'D']
    spec.labels = ['Bee', 'Missing', 'Dee']

    DiscreteCharmScale scale = ScaleEngine.trainPositionalScale(['A', 'B', 'C', 'D'], spec, 0, 100) as DiscreteCharmScale

    assertEquals(['B', 'D'], scale.ticks(5))
    assertEquals(['Bee', 'Dee'], scale.tickLabels(5))
  }

  @Test
  void testDiscreteColorScaleFilteredBreaksKeepLabelPairing() {
    Scale spec = Scale.discrete()
    spec.breaks = ['beta', 'missing', 'gamma']
    spec.labels = ['Beta', 'Missing', 'Gamma']

    ColorCharmScale scale = ScaleEngine.trainColorScale(['alpha', 'beta', 'gamma'], spec)

    assertEquals(['beta', 'gamma'], scale.ticks(5))
    assertEquals(['Beta', 'Gamma'], scale.tickLabels(5))
  }

  @Test
  void testContinuousTemporalScaleFallbackLabelsUseTemporalFormatting() {
    Scale spec = Scale.date()
    spec.breaks = [
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 1, 2),
        LocalDate.of(2025, 1, 3)
    ]
    spec.labels = ['start']

    ContinuousCharmScale scale = ScaleEngine.trainPositionalScale(
        [LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3)],
        spec,
        0,
        100
    ) as ContinuousCharmScale

    assertEquals(['start', '2025-01-02', '2025-01-03'], scale.tickLabels(5))
  }
}
