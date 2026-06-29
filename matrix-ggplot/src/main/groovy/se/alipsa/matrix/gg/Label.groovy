package se.alipsa.matrix.gg


/**
 * Container for chart titles and axis labels.
 * Tracks whether axis labels were explicitly set to preserve intentional blanks.
 */
class Label {

  String title
  String subTitle
  String caption
  String x
  String y
  /** Per-aesthetic legend titles keyed by normalized aesthetic name (e.g. 'color', 'fill', 'size'). colour is stored as 'color'. */
  Map<String, String> legendTitles = [:]

  /** True when the x label was explicitly set via labs/xlab. */
  boolean xSet = false
  /** True when the y label was explicitly set via labs/ylab. */
  boolean ySet = false

  /**
   * Set the x-axis label.
   * Also sets the xSet flag to true to indicate the label was explicitly set.
   */
  void setX(String x) {
    this.@x = x
    this.xSet = true
  }

  /**
   * Set the y-axis label.
   * Also sets the ySet flag to true to indicate the label was explicitly set.
   */
  void setY(String y) {
    this.@y = y
    this.ySet = true
  }
}
