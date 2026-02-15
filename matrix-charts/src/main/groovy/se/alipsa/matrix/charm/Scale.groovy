package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Scale configuration for one aesthetic axis/channel.
 */
@CompileStatic
class Scale {

  private ScaleType type = ScaleType.CONTINUOUS
  private ScaleTransform transformStrategy
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
    new Scale(type: ScaleType.TRANSFORM, transformStrategy: ScaleTransforms.named(transformName))
  }

  /**
   * Creates a transformed scale from a strategy object.
   *
   * @param transform strategy object
   * @return scale instance
   */
  static Scale transform(ScaleTransform transform) {
    new Scale(type: ScaleType.TRANSFORM, transformStrategy: transform)
  }

  /**
   * Creates a date transform scale.
   *
   * @return scale instance
   */
  static Scale date() {
    transform('date')
  }

  /**
   * Creates a time transform scale.
   *
   * @return scale instance
   */
  static Scale time() {
    transform('time')
  }

  /**
   * Creates a reverse transform scale.
   *
   * @return scale instance
   */
  static Scale reverse() {
    transform('reverse')
  }

  /**
   * Creates a custom transformed scale.
   *
   * @param id transform id
   * @param forward forward transform
   * @param inverse inverse transform
   * @return scale instance
   */
  static Scale custom(String id, Closure<BigDecimal> forward, Closure<BigDecimal> inverse = null) {
    transform(ScaleTransforms.custom(id, forward, inverse))
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
    transformStrategy?.id()
  }

  /**
   * Sets transform name.
   *
   * @param transform transform name
   */
  void setTransform(Object transform) {
    this.transformStrategy = ScaleTransforms.resolve(transform)
    if (this.transformStrategy != null) {
      type = ScaleType.TRANSFORM
    }
  }

  /**
   * Returns transform strategy object.
   *
   * @return strategy object
   */
  ScaleTransform getTransformStrategy() {
    transformStrategy
  }

  /**
   * Sets transform strategy object.
   *
   * @param transformStrategy strategy object
   */
  void setTransformStrategy(ScaleTransform transformStrategy) {
    this.transformStrategy = transformStrategy
    if (transformStrategy != null) {
      type = ScaleType.TRANSFORM
    }
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
        transformStrategy: transformStrategy,
        breaks: new ArrayList(breaks),
        labels: new ArrayList<>(labels),
        params: new LinkedHashMap<>(params)
    )
  }
}
