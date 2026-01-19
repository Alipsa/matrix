package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.CssAttributeConfig

import static org.junit.jupiter.api.Assertions.*

/**
 * Tests for CssAttributeConfig class.
 */
class CssAttributeConfigTest {

  @Test
  void testDefaultConfiguration() {
    def config = new CssAttributeConfig()

    assertFalse(config.enabled, "CSS attributes should be disabled by default")
    assertTrue(config.includeClasses, "Classes should be included when enabled")
    assertTrue(config.includeIds, "IDs should be included when enabled")
    assertFalse(config.includeDataAttributes, "Data attributes should be disabled by default")
    assertNull(config.chartIdPrefix, "Chart ID prefix should be null by default")
    assertEquals('gg', config.idPrefix, "Default ID prefix should be 'gg'")
  }

  @Test
  void testMapConstructor() {
    def config = new CssAttributeConfig([
      enabled: true,
      includeClasses: false,
      includeIds: true,
      includeDataAttributes: true,
      chartIdPrefix: 'my-chart',
      idPrefix: 'custom'
    ])

    assertTrue(config.enabled)
    assertFalse(config.includeClasses)
    assertTrue(config.includeIds)
    assertTrue(config.includeDataAttributes)
    assertEquals('my-chart', config.chartIdPrefix)
    assertEquals('custom', config.idPrefix)
  }

  @Test
  void testPartialConfiguration() {
    def config = new CssAttributeConfig([enabled: true])

    assertTrue(config.enabled)
    assertTrue(config.includeClasses, "Should use default values for unspecified options")
    assertTrue(config.includeIds)
    assertFalse(config.includeDataAttributes)
  }

  @Test
  void testEmptyMapConstructor() {
    def config = new CssAttributeConfig([:])

    // Should use default values
    assertFalse(config.enabled)
    assertTrue(config.includeClasses)
    assertTrue(config.includeIds)
  }

  @Test
  void testChartIdPrefixVariations() {
    // Test various chart ID prefixes
    def config1 = new CssAttributeConfig([chartIdPrefix: 'iris'])
    assertEquals('iris', config1.chartIdPrefix)

    def config2 = new CssAttributeConfig([chartIdPrefix: 'My Chart'])
    assertEquals('My Chart', config2.chartIdPrefix)

    def config3 = new CssAttributeConfig([chartIdPrefix: '123'])
    assertEquals('123', config3.chartIdPrefix)
  }

  @Test
  void testIdPrefixFallback() {
    def config = new CssAttributeConfig([
      enabled: true,
      idPrefix: 'custom-prefix'
    ])

    assertEquals('custom-prefix', config.idPrefix)
    assertNull(config.chartIdPrefix)
  }

  @Test
  void testPhase2DataAttributes() {
    def config = new CssAttributeConfig([
      enabled: true,
      includeDataAttributes: true
    ])

    assertTrue(config.enabled)
    assertTrue(config.includeDataAttributes, "Phase 2 data attributes can be enabled")
  }

  @Test
  void testDisablingClassesAndIds() {
    def config = new CssAttributeConfig([
      enabled: true,
      includeClasses: false,
      includeIds: false
    ])

    assertTrue(config.enabled)
    assertFalse(config.includeClasses, "Classes can be disabled")
    assertFalse(config.includeIds, "IDs can be disabled")
  }
}
