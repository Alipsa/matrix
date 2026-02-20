package charm.core

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.GuideSpec
import se.alipsa.matrix.charm.GuideType
import se.alipsa.matrix.charm.GuidesSpec

import static org.junit.jupiter.api.Assertions.*

class CharmGuideModelTest {

  @Test
  void testGuideTypeFromString() {
    assertEquals(GuideType.LEGEND, GuideType.fromString('legend'))
    assertEquals(GuideType.COLORBAR, GuideType.fromString('colorbar'))
    assertEquals(GuideType.COLORSTEPS, GuideType.fromString('colorsteps'))
    assertEquals(GuideType.COLORSTEPS, GuideType.fromString('coloursteps'))
    assertEquals(GuideType.NONE, GuideType.fromString('none'))
    assertEquals(GuideType.AXIS, GuideType.fromString('axis'))
    assertEquals(GuideType.AXIS_LOGTICKS, GuideType.fromString('axis_logticks'))
    assertEquals(GuideType.AXIS_THETA, GuideType.fromString('axis_theta'))
    assertEquals(GuideType.AXIS_STACK, GuideType.fromString('axis_stack'))
    assertEquals(GuideType.BINS, GuideType.fromString('bins'))
    assertEquals(GuideType.CUSTOM, GuideType.fromString('custom'))
    assertNull(GuideType.fromString('unknown'))
    assertNull(GuideType.fromString(null))
    assertNull(GuideType.fromString(''))
  }

  @Test
  void testGuideTypeFromStringCaseInsensitive() {
    assertEquals(GuideType.LEGEND, GuideType.fromString('LEGEND'))
    assertEquals(GuideType.COLORBAR, GuideType.fromString('ColorBar'))
    assertEquals(GuideType.COLORSTEPS, GuideType.fromString('ColourSteps'))
  }

  @Test
  void testGuideTypeAliases() {
    assertEquals(GuideType.COLORSTEPS, GuideType.fromString('colour_steps'))
    assertEquals(GuideType.COLORSTEPS, GuideType.fromString('color_steps'))
    assertEquals(GuideType.COLORBAR, GuideType.fromString('colour_bar'))
    assertEquals(GuideType.COLORBAR, GuideType.fromString('color_bar'))
    assertEquals(GuideType.COLORBAR, GuideType.fromString('colourbar'))
    assertEquals(GuideType.AXIS_LOGTICKS, GuideType.fromString('axis_log_ticks'))
    assertEquals(GuideType.AXIS_LOGTICKS, GuideType.fromString('logticks'))
  }

  @Test
  void testGuideSpecFactoryMethods() {
    GuideSpec legend = GuideSpec.legend()
    assertEquals(GuideType.LEGEND, legend.type)
    assertTrue(legend.params.isEmpty())

    GuideSpec colorbar = GuideSpec.colorbar()
    assertEquals(GuideType.COLORBAR, colorbar.type)

    GuideSpec steps = GuideSpec.colorsteps()
    assertEquals(GuideType.COLORSTEPS, steps.type)

    GuideSpec none = GuideSpec.none()
    assertEquals(GuideType.NONE, none.type)

    GuideSpec axis = GuideSpec.axis()
    assertEquals(GuideType.AXIS, axis.type)

    GuideSpec logTicks = GuideSpec.axisLogticks()
    assertEquals(GuideType.AXIS_LOGTICKS, logTicks.type)

    GuideSpec theta = GuideSpec.axisTheta()
    assertEquals(GuideType.AXIS_THETA, theta.type)

    GuideSpec stack = GuideSpec.axisStack()
    assertEquals(GuideType.AXIS_STACK, stack.type)

    GuideSpec bins = GuideSpec.bins()
    assertEquals(GuideType.BINS, bins.type)
  }

  @Test
  void testGuideSpecFactoryWithParams() {
    GuideSpec legend = GuideSpec.legend(title: 'My Legend', reverse: true)
    assertEquals(GuideType.LEGEND, legend.type)
    assertEquals('My Legend', legend.params['title'])
    assertEquals(true, legend.params['reverse'])
  }

  @Test
  void testGuideSpecCustomFactory() {
    Closure renderer = { ctx -> }
    GuideSpec custom = GuideSpec.custom(renderer, [width: 100, height: 50] as Map<String, Object>)
    assertEquals(GuideType.CUSTOM, custom.type)
    assertNotNull(custom.params['renderClosure'])
    assertEquals(100, custom.params['width'])
    assertEquals(50, custom.params['height'])
  }

  @Test
  void testGuideSpecCopy() {
    GuideSpec original = GuideSpec.legend(title: 'Test', nested: [a: 1, b: 2])
    GuideSpec copy = original.copy()

    assertEquals(original.type, copy.type)
    assertEquals(original.params['title'], copy.params['title'])
    assertFalse(copy.params.is(original.params))

    // Verify deep copy of nested map
    Map originalNested = original.params['nested'] as Map
    Map copyNested = copy.params['nested'] as Map
    assertFalse(copyNested.is(originalNested))
    originalNested['a'] = 999
    assertEquals(1, copyNested['a'])
  }

  @Test
  void testGuidesSpecSetAndGet() {
    GuidesSpec guides = new GuidesSpec()
    guides.setSpec('color', GuideSpec.legend())
    guides.setSpec('fill', GuideSpec.colorbar())
    guides.setSpec('size', GuideSpec.none())

    assertEquals(GuideType.LEGEND, guides.getSpec('color').type)
    assertEquals(GuideType.COLORBAR, guides.getSpec('fill').type)
    assertEquals(GuideType.NONE, guides.getSpec('size').type)
    assertNull(guides.getSpec('unknown'))
    assertNull(guides.getSpec(null))
  }

  @Test
  void testGuidesSpecColourNormalization() {
    GuidesSpec guides = new GuidesSpec()
    guides.setSpec('colour', GuideSpec.legend())

    // 'colour' normalizes to 'color'
    assertNotNull(guides.getSpec('color'))
    assertNotNull(guides.getSpec('colour'))
    assertEquals(GuideType.LEGEND, guides.getSpec('color').type)
  }

  @Test
  void testGuidesSpecCopy() {
    GuidesSpec original = new GuidesSpec()
    original.setSpec('color', GuideSpec.legend(title: 'Test'))
    original.setSpec('fill', GuideSpec.colorbar())

    GuidesSpec copy = original.copy()

    assertFalse(copy.is(original))
    assertFalse(copy.specs.is(original.specs))
    assertEquals(2, copy.specs.size())
    assertEquals(GuideType.LEGEND, copy.getSpec('color').type)
    assertEquals('Test', copy.getSpec('color').params['title'])

    // Verify independence
    original.setSpec('size', GuideSpec.none())
    assertNull(copy.getSpec('size'))
  }

  @Test
  void testGuidesSpecPlus() {
    GuidesSpec a = new GuidesSpec()
    a.setSpec('color', GuideSpec.legend())
    a.setSpec('fill', GuideSpec.colorbar())

    GuidesSpec b = new GuidesSpec()
    b.setSpec('color', GuideSpec.none())
    b.setSpec('size', GuideSpec.legend())

    GuidesSpec merged = a + b

    // 'color' from b overrides a
    assertEquals(GuideType.NONE, merged.getSpec('color').type)
    // 'fill' from a is preserved
    assertEquals(GuideType.COLORBAR, merged.getSpec('fill').type)
    // 'size' from b is added
    assertEquals(GuideType.LEGEND, merged.getSpec('size').type)
  }

  @Test
  void testGuidesSpecIsEmpty() {
    GuidesSpec empty = new GuidesSpec()
    assertTrue(empty.isEmpty())

    empty.setSpec('color', GuideSpec.legend())
    assertFalse(empty.isEmpty())
  }

  @Test
  void testGuidesSpecConstructorWithMap() {
    GuidesSpec guides = new GuidesSpec([
        color: GuideSpec.legend(),
        fill : GuideSpec.none()
    ])
    assertEquals(2, guides.specs.size())
    assertEquals(GuideType.LEGEND, guides.getSpec('color').type)
    assertEquals(GuideType.NONE, guides.getSpec('fill').type)
  }
}
