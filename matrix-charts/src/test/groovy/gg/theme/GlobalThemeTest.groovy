package gg.theme

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.theme.Theme

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.gg.GgPlot.*

/**
 * Tests for global theme management functions:
 * - theme_get()
 * - theme_set()
 * - theme_update()
 * - theme_replace()
 */
class GlobalThemeTest {

  private Theme originalTheme

  @BeforeEach
  void setUp() {
    // Save original theme before each test
    originalTheme = theme_get()
  }

  @AfterEach
  void tearDown() {
    // Restore original theme after each test
    theme_set(originalTheme)
  }

  @Test
  void testThemeGetReturnsDefault() {
    def theme = theme_get()
    assertNotNull(theme, 'theme_get should never return null')
    // Default is gray theme
    assertEquals('gray', theme.themeName)
    assertNotNull(theme.panelBackground)
  }

  @Test
  void testThemeSetChangesGlobalTheme() {
    def oldTheme = theme_set(theme_minimal())

    // Verify old theme returned
    assertNotNull(oldTheme)
    assertEquals('gray', oldTheme.themeName) // Default is gray

    // Verify new theme is active
    def currentTheme = theme_get()
    assertNotNull(currentTheme)
    assertEquals('minimal', currentTheme.themeName)
  }

  @Test
  void testThemeSetRejectsNull() {
    assertThrows(IllegalArgumentException) {
      theme_set(null)
    }
  }

  @Test
  void testThemeUpdate() {
    theme_set(theme_gray())

    // Update with custom element
    theme_update(['plot.title': element_text(size: 20)])

    def updated = theme_get()
    assertNotNull(updated.plotTitle)
    assertEquals(20, updated.plotTitle.size)

    // Should preserve other gray theme elements
    assertNotNull(updated.panelBackground)
  }

  @Test
  void testThemeReplaceIsAliasForSet() {
    def minimal = theme_minimal()

    def old1 = theme_set(minimal)
    def theme1 = theme_get()

    // Reset
    theme_set(old1)

    def old2 = theme_replace(minimal)
    def theme2 = theme_get()

    // Both should produce same result - compare theme names
    assertEquals(theme1.themeName, theme2.themeName)
    assertEquals('minimal', theme2.themeName)
  }

  @Test
  void testNewChartsUseGlobalTheme() {
    theme_set(theme_minimal())

    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 2], [3, 4]])
        .build()

    def chart = ggplot(data, aes('x', 'y')) + geom_point()

    // Chart should have minimal theme
    assertNotNull(chart.theme)
    assertEquals('minimal', chart.theme.themeName)
  }

  @Test
  void testExplicitChartThemeOverridesGlobal() {
    theme_set(theme_minimal())

    def data = Matrix.builder()
        .columnNames(['x', 'y'])
        .rows([[1, 2], [3, 4]])
        .build()

    def chart = ggplot(data, aes('x', 'y')) +
                geom_point() +
                theme_dark()

    // Chart should have dark theme, not minimal
    assertNotNull(chart.theme)
    assertEquals('dark', chart.theme.themeName)
  }

  @Test
  void testThreadSafety() throws InterruptedException {
    theme_set(theme_gray())

    def mainTheme = theme_get()
    def threadThemeCapture = new Theme[1]
    def threadChangedThemeCapture = new Theme[1]

    Thread thread = Thread.start {
      // Different thread should start with default (gray)
      def threadTheme = theme_get()
      threadThemeCapture[0] = threadTheme

      // Change theme in thread
      theme_set(theme_minimal())

      // Thread should now have minimal
      def newThreadTheme = theme_get()
      threadChangedThemeCapture[0] = newThreadTheme
    }

    thread.join()

    // Main thread theme should be unchanged
    def stillMainTheme = theme_get()
    assertEquals(mainTheme.themeName, stillMainTheme.themeName)
    assertEquals('gray', stillMainTheme.themeName)

    // Thread should have changed its own theme
    assertNotNull(threadThemeCapture[0])
    assertNotNull(threadChangedThemeCapture[0])
    assertEquals('gray', threadThemeCapture[0].themeName)
    assertEquals('minimal', threadChangedThemeCapture[0].themeName)
  }

  @Test
  void testThemeFunctionReturnsValue() {
    // Bug fix test - theme() should return Theme object
    def customTheme = theme(['plot.title': element_text(color: 'red')])

    assertNotNull(customTheme, 'theme() should return Theme object')
    assertTrue(customTheme instanceof Theme)
    assertNotNull(customTheme.plotTitle)
    assertEquals('red', customTheme.plotTitle.color)
  }

  @Test
  void testPredefinedThemesHaveNames() {
    // Verify all predefined themes have themeName set
    assertEquals('gray', theme_gray().themeName)
    assertEquals('minimal', theme_minimal().themeName)
    assertEquals('bw', theme_bw().themeName)
    assertEquals('classic', theme_classic().themeName)
    assertEquals('void', theme_void().themeName)
    assertEquals('light', theme_light().themeName)
    assertEquals('dark', theme_dark().themeName)
    assertEquals('linedraw', theme_linedraw().themeName)
    assertEquals('test', theme_test().themeName)
  }

  @Test
  void testThemeUpdateWithMultipleElements() {
    theme_set(theme_gray())

    // Update multiple elements at once
    theme_update([
        'plot.title': element_text(size: 20, face: 'bold'),
        'legend.position': 'bottom',
        'panel.grid.major': element_blank()
    ])

    def updated = theme_get()
    assertNotNull(updated.plotTitle)
    assertEquals(20, updated.plotTitle.size)
    assertEquals('bold', updated.plotTitle.face)
    assertEquals('bottom', updated.legendPosition)
    assertNull(updated.panelGridMajor)
  }

  @Test
  void testSetAndRestoreTheme() {
    // Save original
    def original = theme_get()

    // Set new theme
    def old1 = theme_set(theme_minimal())

    // Set another theme
    def old2 = theme_set(theme_dark())

    // Restore to minimal - verify by checking theme name
    theme_set(old2)
    assertEquals('minimal', theme_get().themeName)

    // Restore to original - verify by checking theme name
    theme_set(old1)
    assertEquals(original.themeName, theme_get().themeName)
  }
}
