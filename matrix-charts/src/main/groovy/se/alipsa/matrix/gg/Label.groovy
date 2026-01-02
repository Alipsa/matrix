package se.alipsa.matrix.gg

import groovy.transform.CompileStatic

/**
 * Container for chart titles and axis labels.
 * Tracks whether axis labels were explicitly set to preserve intentional blanks.
 */
@CompileStatic
class Label {

  String title
  String subTitle
  String caption
  String x
  String y
  String legendTitle // called colour in ggplot2

  /** True when the x label was explicitly set via labs/xlab. */
  boolean xSet = false
  /** True when the y label was explicitly set via labs/ylab. */
  boolean ySet = false

  void setX(String x) {
    this.@x = x
    this.xSet = true
  }

  void setY(String y) {
    this.@y = y
    this.ySet = true
  }
}
