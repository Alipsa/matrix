package se.alipsa.matrix.charm

/**
 * Typed labels specification for Charm core.
 */
class LabelsSpec extends Labels {

  /**
   * Builder-style title setter.
   *
   * @param value title value
   * @return this spec
   */
  LabelsSpec title(String value) {
    setTitle(value)
    this
  }

  /**
   * Builder-style subtitle setter.
   *
   * @param value subtitle value
   * @return this spec
   */
  LabelsSpec subtitle(String value) {
    setSubtitle(value)
    this
  }

  /**
   * Builder-style caption setter.
   *
   * @param value caption value
   * @return this spec
   */
  LabelsSpec caption(String value) {
    setCaption(value)
    this
  }

  /**
   * Copies this labels spec.
   *
   * @return copied labels spec
   */
  @Override
  LabelsSpec copy() {
    new LabelsSpec(
        title: title,
        subtitle: subtitle,
        caption: caption,
        x: x,
        y: y,
        guides: new LinkedHashMap<>(guides)
    )
  }

}
