package charm.core

import org.junit.jupiter.api.Test
import se.alipsa.matrix.charm.Theme
import se.alipsa.matrix.charm.theme.CharmThemes
import se.alipsa.matrix.charm.theme.ElementBlank
import se.alipsa.matrix.charm.theme.ElementLine
import se.alipsa.matrix.charm.theme.ElementRect
import se.alipsa.matrix.charm.theme.ElementText

import static org.junit.jupiter.api.Assertions.*

class CharmThemeElementTest {

  @Test
  void testElementTextCopy() {
    ElementText text = new ElementText(
        family: 'serif', face: 'bold', size: 14,
        color: '#ff0000', hjust: 0, vjust: 1, angle: 45
    )
    ElementText copy = text.copy()
    assertNotSame(text, copy)
    assertEquals('serif', copy.family)
    assertEquals('bold', copy.face)
    assertEquals(14, copy.size)
    assertEquals('#ff0000', copy.color)
    assertEquals(0, copy.hjust)
    assertEquals(1, copy.vjust)
    assertEquals(45, copy.angle)
  }

  @Test
  void testElementLineCopy() {
    ElementLine line = new ElementLine(
        color: 'red', size: 2, linetype: 'dashed', lineend: 'round'
    )
    ElementLine copy = line.copy()
    assertNotSame(line, copy)
    assertEquals('red', copy.color)
    assertEquals(2, copy.size)
    assertEquals('dashed', copy.linetype)
    assertEquals('round', copy.lineend)
  }

  @Test
  void testElementRectCopy() {
    ElementRect rect = new ElementRect(
        fill: '#cccccc', color: 'black', size: 1, linetype: 'dotted'
    )
    ElementRect copy = rect.copy()
    assertNotSame(rect, copy)
    assertEquals('#cccccc', copy.fill)
    assertEquals('black', copy.color)
    assertEquals(1, copy.size)
    assertEquals('dotted', copy.linetype)
  }

  @Test
  void testElementBlankIsMarkerClass() {
    ElementBlank blank = new ElementBlank()
    assertNotNull(blank)
  }

  @Test
  void testThemeCopyPreservesAllFields() {
    Theme theme = new Theme()
    theme.plotBackground = new ElementRect(fill: '#fafafa')
    theme.plotTitle = new ElementText(size: 20, color: '#000')
    theme.panelGridMajor = new ElementLine(color: '#ddd')
    theme.legendPosition = 'bottom'
    theme.stripBackground = new ElementRect(fill: '#eee')
    theme.themeName = 'custom'
    theme.baseSize = 14
    theme.explicitNulls.add('panelGridMinor')

    Theme copy = theme.copy()
    assertNotSame(theme, copy)
    assertEquals('#fafafa', copy.plotBackground.fill)
    assertEquals(20, copy.plotTitle.size)
    assertEquals('#ddd', copy.panelGridMajor.color)
    assertEquals('bottom', copy.legendPosition)
    assertEquals('#eee', copy.stripBackground.fill)
    assertEquals('custom', copy.themeName)
    assertEquals(14, copy.baseSize)
    assertTrue(copy.explicitNulls.contains('panelGridMinor'))
  }

  @Test
  void testThemePlusMergesOverrides() {
    Theme base = CharmThemes.gray()
    Theme override = new Theme()
    override.plotBackground = new ElementRect(fill: '#ff0000')
    override.legendPosition = 'bottom'

    Theme merged = base.plus(override)
    assertEquals('#ff0000', merged.plotBackground.fill)
    assertEquals('bottom', merged.legendPosition)
    // Base values preserved where not overridden
    assertEquals('gray', merged.themeName)
  }

  @Test
  void testThemePlusExplicitNulls() {
    Theme base = CharmThemes.gray()
    assertNotNull(base.panelGridMajor)

    Theme override = new Theme()
    override.panelGridMajor = null
    override.explicitNulls.add('panelGridMajor')

    Theme merged = base.plus(override)
    assertNull(merged.panelGridMajor)
    assertTrue(merged.explicitNulls.contains('panelGridMajor'))
  }

  @Test
  void testPredefinedThemeGray() {
    Theme t = CharmThemes.gray()
    assertEquals('gray', t.themeName)
    assertEquals('#EBEBEB', t.panelBackground.fill)
    assertEquals('white', t.panelGridMajor.color)
  }

  @Test
  void testPredefinedThemeClassic() {
    Theme t = CharmThemes.classic()
    assertEquals('classic', t.themeName)
    assertTrue(t.explicitNulls.contains('panelGridMajor'))
    assertTrue(t.explicitNulls.contains('panelGridMinor'))
    assertEquals('black', t.axisLineX.color)
  }

  @Test
  void testPredefinedThemeBw() {
    Theme t = CharmThemes.bw()
    assertEquals('bw', t.themeName)
    assertEquals('white', t.panelBackground.fill)
    assertEquals('black', t.panelBackground.color)
  }

  @Test
  void testPredefinedThemeMinimal() {
    Theme t = CharmThemes.minimal()
    assertEquals('minimal', t.themeName)
    assertTrue(t.explicitNulls.contains('panelGridMinor'))
  }

  @Test
  void testPredefinedThemeVoid() {
    Theme t = CharmThemes.void_()
    assertEquals('void', t.themeName)
    assertTrue(t.explicitNulls.contains('panelBackground'))
    assertTrue(t.explicitNulls.contains('axisLineX'))
    assertTrue(t.explicitNulls.contains('axisTextX'))
  }

  @Test
  void testPredefinedThemeDark() {
    Theme t = CharmThemes.dark()
    assertEquals('dark', t.themeName)
    assertEquals('#3B3B3B', t.panelBackground.fill)
    assertEquals('#CCCCCC', t.axisTextX.color)
    assertEquals('#222222', t.plotBackground.fill)
  }

  @Test
  void testPredefinedThemeLight() {
    Theme t = CharmThemes.light()
    assertEquals('light', t.themeName)
    assertEquals('white', t.panelBackground.fill)
  }

  @Test
  void testPredefinedThemeLinedraw() {
    Theme t = CharmThemes.linedraw()
    assertEquals('linedraw', t.themeName)
    assertEquals('black', t.panelGridMajor.color)
  }

  @Test
  void testPredefinedThemeTest() {
    Theme t = CharmThemes.test(12, 'monospace')
    assertEquals('test', t.themeName)
    assertEquals(12, t.baseSize)
    assertEquals('monospace', t.baseFamily)
    assertNotNull(t.plotTitle)
    assertNotNull(t.axisTextX)
    assertTrue(t.explicitNulls.contains('panelGridMajor'))
  }

  @Test
  void testElementTextFromMapWithColourAlias() {
    ElementText text = new ElementText(colour: '#123456', size: 12)
    assertEquals('#123456', text.color)
    assertEquals(12, text.size)
  }

  @Test
  void testElementLineFromMapWithColourAlias() {
    ElementLine line = new ElementLine(colour: '#654321', linewidth: 3)
    assertEquals('#654321', line.color)
    assertEquals(3, line.size)
  }
}
