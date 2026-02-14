package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Scale configuration for one aesthetic axis/channel.
 */
@CompileStatic
class Scale {

  private ScaleType type = ScaleType.CONTINUOUS
  private String transform
  private List breaks = []
  private List<String> labels = []
  private Map<String, Object> params = [:]

  /**
   * Creates a continuous scale.
   *
   * @return scale instance
   */
  static Scale continuous() {
    new Scale(type: ScaleType.CONTINUOUS)
  }

  /**
   * Creates a discrete scale.
   *
   * @return scale instance
   */
  static Scale discrete() {
    new Scale(type: ScaleType.DISCRETE)
  }

  /**
   * Creates a transformed scale.
   *
   * @param transformName transform id
   * @return scale instance
   */
  static Scale transform(String transformName) {
    new Scale(type: ScaleType.TRANSFORM, transform: transformName)
  }

  /**
   * Returns scale type.
   *
   * @return scale type
   */
  ScaleType getType() {
    type
  }

  /**
   * Sets scale type.
   *
   * @param type scale type
   */
  void setType(ScaleType type) {
    this.type = type ?: ScaleType.CONTINUOUS
  }

  /**
   * Returns transform name.
   *
   * @return transform name
   */
  String getTransform() {
    transform
  }

  /**
   * Sets transform name.
   *
   * @param transform transform name
   */
  void setTransform(String transform) {
    this.transform = transform
  }

  /**
   * Returns breaks values.
   *
   * @return breaks values
   */
  List getBreaks() {
    breaks
  }

  /**
   * Sets breaks values.
   *
   * @param breaks breaks list
   */
  void setBreaks(List breaks) {
    this.breaks = breaks == null ? [] : new ArrayList(breaks)
  }

  /**
   * Returns display labels.
   *
   * @return labels
   */
  List<String> getLabels() {
    labels
  }

  /**
   * Sets display labels.
   *
   * @param labels labels
   */
  void setLabels(List<String> labels) {
    this.labels = labels == null ? [] : new ArrayList<>(labels)
  }

  /**
   * Returns free-form scale parameters.
   *
   * @return params map
   */
  Map<String, Object> getParams() {
    params
  }

  /**
   * Sets free-form scale parameters.
   *
   * @param params params map
   */
  void setParams(Map<String, Object> params) {
    this.params = params == null ? [:] : new LinkedHashMap<>(params)
  }

  /**
   * Copies this scale.
   *
   * @return copied scale
   */
  Scale copy() {
    new Scale(
        type: type,
        transform: transform,
        breaks: new ArrayList(breaks),
        labels: new ArrayList<>(labels),
        params: new LinkedHashMap<>(params)
    )
  }
}
