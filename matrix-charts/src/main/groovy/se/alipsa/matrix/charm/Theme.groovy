package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Theme configuration with minimal grouped sections.
 */
@CompileStatic
class Theme {

  private Map<String, Object> legend = [:]
  private Map<String, Object> axis = [:]
  private Map<String, Object> text = [:]
  private Map<String, Object> grid = [:]
  private Map<String, Object> raw = [:]

  /**
   * Returns legend theme options.
   *
   * @return legend options
   */
  Map<String, Object> getLegend() {
    legend
  }

  /**
   * Sets legend theme options.
   *
   * @param legend legend options
   */
  void setLegend(Map<String, Object> legend) {
    this.legend = legend == null ? [:] : new LinkedHashMap<>(legend)
  }

  /**
   * Returns axis theme options.
   *
   * @return axis options
   */
  Map<String, Object> getAxis() {
    axis
  }

  /**
   * Sets axis theme options.
   *
   * @param axis axis options
   */
  void setAxis(Map<String, Object> axis) {
    this.axis = axis == null ? [:] : new LinkedHashMap<>(axis)
  }

  /**
   * Returns text theme options.
   *
   * @return text options
   */
  Map<String, Object> getText() {
    text
  }

  /**
   * Sets text theme options.
   *
   * @param text text options
   */
  void setText(Map<String, Object> text) {
    this.text = text == null ? [:] : new LinkedHashMap<>(text)
  }

  /**
   * Returns grid theme options.
   *
   * @return grid options
   */
  Map<String, Object> getGrid() {
    grid
  }

  /**
   * Sets grid theme options.
   *
   * @param grid grid options
   */
  void setGrid(Map<String, Object> grid) {
    this.grid = grid == null ? [:] : new LinkedHashMap<>(grid)
  }

  /**
   * Returns compatibility fallback values.
   *
   * @return raw options
   */
  Map<String, Object> getRaw() {
    raw
  }

  /**
   * Sets compatibility fallback values.
   *
   * @param raw raw options
   */
  void setRaw(Map<String, Object> raw) {
    this.raw = raw == null ? [:] : new LinkedHashMap<>(raw)
  }

  /**
   * Copies this theme.
   *
   * @return copied theme
   */
  Theme copy() {
    new Theme(
        legend: new LinkedHashMap<>(legend),
        axis: new LinkedHashMap<>(axis),
        text: new LinkedHashMap<>(text),
        grid: new LinkedHashMap<>(grid),
        raw: new LinkedHashMap<>(raw)
    )
  }
}
