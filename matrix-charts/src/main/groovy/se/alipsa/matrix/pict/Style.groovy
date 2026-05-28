package se.alipsa.matrix.pict

import java.awt.Color

/**
 * Most of the styles are specified using java.awt classes (Because they are always accessible)
 * Of course, they will be translated to the Javafx or Swing equivalent when plotted (displayed).
 * <pre>
 * |------------------------------------------|
 * |            chart                         |
 * |   ------------------------------------   |
 * | c |          Title                   |   |
 * | h |   |---------------------------|  |   |
 * | a |   |  |---------------------|  |  | c |
 * | r |   |  |     Plot            |  |  | h |
 * | t |   |  |---------------------|  |  | a |
 * |   |   |          Legend           |  | r |
 * |   |   |---------------------------|  | t |
 * |   |                                  |   |
 * |   |----------------------------------|   |
 * |            chart                         |
 * |------------------------------------------|
 * </pre>
 * Title and Legend, and Plot can be positioned using clock
 * notation (1-12) as follows
 *
 * <pre>
 *   |-----------------------|
 *   | 11        12        1 |
 *   | 10                  2 |
 *   | 9                   3 |
 *   | 8                   4 |
 *   | 7          6        5 |
 *   |-----------------------|
 *  </pre>
 *  or in words
 *  <ol>
 *    <li>top right</li>
 *    <li>right top</li>
 *    <li>right center</li>
 *    <li>right bottom</li>
 *    <li>bottom right</li>
 *    <li>bottom center</li>
 *    <li>bottom left</li>
 *    <li>left bottom</li>
 *    <li>left center</li>
 *    <li>left top</li>
 *    <li>top left</li>
 *    <li>top center</li>
 *  </ol>
 */
class Style {

  /**
   * Legacy raw CSS text retained for source compatibility.
   *
   * <p>The Charm bridge does not inject arbitrary stylesheet text. Prefer Charm theme
   * settings and CSS attribute APIs for new code.</p>
   *
   * @deprecated Raw CSS is not applied by the Charm bridge.
   */
  @Deprecated
  String css

  /** The back ground color of the plot area (where the actual chart is drawn) */
  Color plotBackgroundColor

  /** The background color of the area outside of the plot area */
  Color chartBackgroundColor

  /** whether to show the title or not */
  Boolean titleVisible

  Boolean xAxisVisible
  Boolean yAxisVisible

  /** Maps numeric y-axis break values to custom display labels. */
  Map<String, String> yLabels = [:]

  /**
   * Sets legacy raw CSS text.
   *
   * @param css raw CSS text
   * @deprecated Raw CSS is not applied by the Charm bridge; use Charm theme settings instead.
   */
  @Deprecated
  void setCss(String css) {
    this.css = css
  }

  /**
   * Returns legacy raw CSS text.
   *
   * @return raw CSS text
   * @deprecated Raw CSS is not applied by the Charm bridge; use Charm theme settings instead.
   */
  @Deprecated
  String getCss() {
    return css
  }

  static enum Position {

    TOP, RIGHT, BOTTOM, LEFT

  }

}
