package se.alipsa.matrix.charm

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.theme.ElementLine
import se.alipsa.matrix.charm.theme.ElementRect
import se.alipsa.matrix.charm.theme.ElementText

/**
 * Typed theme specification for Charm core.
 *
 * Extends {@link Theme} with builder-style methods for fluent configuration.
 */
@CompileStatic
class ThemeSpec extends Theme {

  /**
   * Builder-style plot background setter.
   *
   * @param value plot background element
   * @return this spec
   */
  ThemeSpec plotBackground(ElementRect value) {
    setPlotBackground(value)
    this
  }

  /**
   * Builder-style panel background setter.
   *
   * @param value panel background element
   * @return this spec
   */
  ThemeSpec panelBackground(ElementRect value) {
    setPanelBackground(value)
    this
  }

  /**
   * Builder-style panel grid major setter.
   *
   * @param value grid major element
   * @return this spec
   */
  ThemeSpec panelGridMajor(ElementLine value) {
    setPanelGridMajor(value)
    this
  }

  /**
   * Builder-style panel grid minor setter.
   *
   * @param value grid minor element
   * @return this spec
   */
  ThemeSpec panelGridMinor(ElementLine value) {
    setPanelGridMinor(value)
    this
  }

  /**
   * Builder-style legend position setter.
   *
   * @param value legend position
   * @return this spec
   */
  ThemeSpec legendPosition(Object value) {
    setLegendPosition(value)
    this
  }

  /**
   * Builder-style theme name setter.
   *
   * @param value theme name
   * @return this spec
   */
  ThemeSpec themeName(String value) {
    setThemeName(value)
    this
  }

  /**
   * Copies this theme spec.
   *
   * @return copied theme spec
   */
  @Override
  ThemeSpec copy() {
    Theme baseCopy = super.copy()
    ThemeSpec spec = new ThemeSpec()
    // Copy all fields from the base copy
    baseCopy.properties.each { key, value ->
      String k = key as String
      if (k != 'class' && spec.hasProperty(k)) {
        spec.setProperty(k, value)
      }
    }
    spec
  }
}
