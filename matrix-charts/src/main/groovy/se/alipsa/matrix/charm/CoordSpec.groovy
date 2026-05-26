package se.alipsa.matrix.charm

/**
 * Typed coordinate specification for Charm core.
 */
class CoordSpec extends Coord {

  /**
   * Builder-style coordinate type setter.
   *
   * @param value coord type value
   * @return this spec
   */
  CoordSpec type(CharmCoordType value) {
    setType(value)
    this
  }

  /**
   * Builder-style coordinate type setter.
   *
   * @param value coord type name
   * @return this spec
   */
  CoordSpec type(String value) {
    setType(value)
    this
  }

  /**
   * Builder-style coordinate type reset.
   *
   * @param value null coord type value
   * @return this spec
   */
  @SuppressWarnings('UnusedMethodParameter')
  CoordSpec type(Void value) {
    setType(value)
    this
  }

  /**
   * Returns the xlim parameter, if set.
   *
   * @return xlim value or null
   */
  List<Number> getXlim() {
    params.xlim as List<Number>
  }

  void setXlim(List<Number> value) {
    params.xlim = value
  }

  /**
   * Returns the ylim parameter, if set.
   *
   * @return ylim value or null
   */
  List<Number> getYlim() {
    params.ylim as List<Number>
  }

  void setYlim(List<Number> value) {
    params.ylim = value
  }

  /**
   * Returns the ratio parameter, if set.
   *
   * @return ratio value or null
   */
  BigDecimal getRatio() {
    params.ratio as BigDecimal
  }

  void setRatio(Number value) {
    params.ratio = value == null ? null : value as BigDecimal
  }

  /**
   * Returns the theta parameter, if set.
   *
   * @return theta value or null
   */
  String getTheta() {
    params.theta as String
  }

  void setTheta(String value) {
    params.theta = value
  }

  /**
   * Returns the start parameter, if set.
   *
   * @return start value or null
   */
  BigDecimal getStart() {
    params.start as BigDecimal
  }

  void setStart(Number value) {
    params.start = value == null ? null : value as BigDecimal
  }

  /**
   * Returns the direction parameter, if set.
   *
   * @return direction value or null
   */
  Integer getDirection() {
    params.direction as Integer
  }

  void setDirection(Integer value) {
    params.direction = value
  }

  /**
   * Returns the clip parameter, if set.
   *
   * @return clip value or null
   */
  String getClip() {
    params.clip as String
  }

  void setClip(String value) {
    params.clip = value
  }

  /**
   * Copies this coord spec.
   *
   * @return copied coord spec
   */
  @Override
  CoordSpec copy() {
    new CoordSpec(type: type, params: new LinkedHashMap<>(params))
  }

}
