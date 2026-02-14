package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Labels and guide titles for a Charm chart.
 */
@CompileStatic
class Labels {

  private String title
  private String subtitle
  private String caption
  private String x
  private String y
  private Map<String, String> guides = [:]

  /**
   * Returns chart title.
   *
   * @return title
   */
  String getTitle() {
    title
  }

  /**
   * Sets chart title.
   *
   * @param title title
   */
  void setTitle(String title) {
    this.title = title
  }

  /**
   * Returns chart subtitle.
   *
   * @return subtitle
   */
  String getSubtitle() {
    subtitle
  }

  /**
   * Sets chart subtitle.
   *
   * @param subtitle subtitle
   */
  void setSubtitle(String subtitle) {
    this.subtitle = subtitle
  }

  /**
   * Returns chart caption.
   *
   * @return caption
   */
  String getCaption() {
    caption
  }

  /**
   * Sets chart caption.
   *
   * @param caption caption
   */
  void setCaption(String caption) {
    this.caption = caption
  }

  /**
   * Returns x axis label.
   *
   * @return x label
   */
  String getX() {
    x
  }

  /**
   * Sets x axis label.
   *
   * @param x x label
   */
  void setX(String x) {
    this.x = x
  }

  /**
   * Returns y axis label.
   *
   * @return y label
   */
  String getY() {
    y
  }

  /**
   * Sets y axis label.
   *
   * @param y y label
   */
  void setY(String y) {
    this.y = y
  }

  /**
   * Returns guide label map.
   *
   * @return guide labels
   */
  Map<String, String> getGuides() {
    guides
  }

  /**
   * Sets guide label map.
   *
   * @param guides guide labels
   */
  void setGuides(Map<String, String> guides) {
    this.guides = guides == null ? [:] : new LinkedHashMap<>(guides)
  }

  /**
   * Copies this label spec.
   *
   * @return copied labels
   */
  Labels copy() {
    new Labels(
        title: title,
        subtitle: subtitle,
        caption: caption,
        x: x,
        y: y,
        guides: new LinkedHashMap<>(guides)
    )
  }
}
