package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Typed theme specification for Charm core.
 */
@CompileStatic
class ThemeSpec extends Theme {

  /**
   * Builder-style legend section setter.
   *
   * @param values legend values
   * @return this spec
   */
  ThemeSpec legend(Map<String, Object> values) {
    setLegend(values)
    this
  }

  /**
   * Builder-style axis section setter.
   *
   * @param values axis values
   * @return this spec
   */
  ThemeSpec axis(Map<String, Object> values) {
    setAxis(values)
    this
  }

  /**
   * Builder-style text section setter.
   *
   * @param values text values
   * @return this spec
   */
  ThemeSpec text(Map<String, Object> values) {
    setText(values)
    this
  }

  /**
   * Builder-style grid section setter.
   *
   * @param values grid values
   * @return this spec
   */
  ThemeSpec grid(Map<String, Object> values) {
    setGrid(values)
    this
  }

  /**
   * Copies this theme spec.
   *
   * @return copied theme spec
   */
  @Override
  ThemeSpec copy() {
    new ThemeSpec(
        legend: new LinkedHashMap<>(legend),
        axis: new LinkedHashMap<>(axis),
        text: new LinkedHashMap<>(text),
        grid: new LinkedHashMap<>(grid),
        raw: new LinkedHashMap<>(raw)
    )
  }
}
