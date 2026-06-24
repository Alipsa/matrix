package se.alipsa.matrix.charm

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

/**
 * Scale configuration for one aesthetic axis/channel.
 */
@SuppressWarnings('DuplicateStringLiteral')
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
   * Creates a datetime transform scale.
   *
   * @return scale instance
   */
  static Scale datetime() {
    transform('datetime')
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
  static Scale custom(String id,
                       @ClosureParams(value = SimpleType, options = ['java.math.BigDecimal'])
                       Closure<BigDecimal> forward,
                       @ClosureParams(value = SimpleType, options = ['java.math.BigDecimal'])
                       Closure<BigDecimal> inverse = null) {
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
    ScaleType resolved = type ?: ScaleType.CONTINUOUS
    this.type = resolved
    if (resolved != ScaleType.TRANSFORM) {
      this.transformStrategy = null
    }
  }

  /**
   * Returns transform name.
   *
   * @return transform name
   */
  String getTransform() {
    transformStrategy?.id()
  }

  private void applyTransformStrategy(ScaleTransform transform) {
    this.transformStrategy = transform
    if (this.transformStrategy == null) {
      if (type == ScaleType.TRANSFORM) {
        type = ScaleType.CONTINUOUS
      }
      return
    }
    type = ScaleType.TRANSFORM
  }

  /**
   * Sets transform name.
   *
   * @param transformName transform name
   */
  void setTransform(String transformName) {
    applyTransformStrategy(ScaleTransforms.resolve(transformName))
  }

  /**
   * Sets transform strategy object.
   *
   * @param transform transform strategy object
   */
  void setTransform(ScaleTransform transform) {
    applyTransformStrategy(ScaleTransforms.resolve(transform))
  }

  /**
   * Clears transform strategy.
   *
   * @param transform null transform value
   */
  @SuppressWarnings('UnusedMethodParameter')
  void setTransform(Void transform) {
    applyTransformStrategy(null)
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
    applyTransformStrategy(transformStrategy)
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
   * Creates a manual color scale.
   *
   * @param namedValues named color map
   * @return scale instance
   */
  static Scale manual(Map<String, String> namedValues) {
    Scale s = new Scale(type: ScaleType.DISCRETE)
    s.params['colorType'] = 'manual'
    s.params['namedValues'] = namedValues == null ? [:] : new LinkedHashMap<String, String>(namedValues)
    s
  }

  /**
   * Creates a manual color scale.
   *
   * @param values color list
   * @return scale instance
   */
  static Scale manual(List<String> values) {
    Scale s = new Scale(type: ScaleType.DISCRETE)
    s.params['colorType'] = 'manual'
    s.params['values'] = values == null ? [] : new ArrayList<String>(values)
    s
  }

  /**
   * Creates a ColorBrewer color scale.
   *
   * @param palette palette name
   * @param direction 1 for normal, -1 for reversed
   * @return scale instance
   */
  static Scale brewer(String palette = 'Set1', int direction = 1) {
    Scale s = new Scale(type: ScaleType.DISCRETE)
    s.params['colorType'] = 'brewer'
    s.params['palette'] = palette
    s.params['direction'] = direction
    s
  }

  /**
   * Creates a ColorBrewer-based continuous interpolation scale.
   *
   * @param palette brewer palette name
   * @param direction 1 for normal, -1 for reversed
   * @return scale instance
   */
  static Scale distiller(String palette = 'RdYlBu', int direction = -1) {
    Scale s = new Scale(type: ScaleType.CONTINUOUS)
    s.params['colorType'] = 'distiller'
    s.params['palette'] = palette
    s.params['direction'] = direction
    s
  }

  /**
   * Creates a ColorBrewer-based binned continuous scale.
   *
   * @param palette brewer palette name
   * @param direction 1 for normal, -1 for reversed
   * @param nBreaks number of bins
   * @return scale instance
   */
  static Scale fermenter(String palette = 'YlOrRd', int direction = 1, int nBreaks = 6) {
    Scale s = new Scale(type: ScaleType.CONTINUOUS)
    s.params['colorType'] = 'fermenter'
    s.params['palette'] = palette
    s.params['direction'] = direction
    s.params['nBreaks'] = nBreaks
    s
  }

  /**
   * Creates a discrete grey scale.
   *
   * @param start start grey color
   * @param end end grey color
   * @return scale instance
   */
  static Scale grey(String start = '#EBEBEB', String end = '#4D4D4D') {
    Scale s = new Scale(type: ScaleType.DISCRETE)
    s.params['colorType'] = 'grey'
    s.params['start'] = start
    s.params['end'] = end
    s
  }

  /**
   * Creates a discrete hue scale.
   *
   * @param hStart start hue in degrees
   * @param hEnd end hue in degrees
   * @param direction 1 for normal, -1 for reversed
   * @return scale instance
   */
  static Scale hue(BigDecimal hStart = 15.0, BigDecimal hEnd = 375.0, int direction = 1) {
    Scale s = new Scale(type: ScaleType.DISCRETE)
    s.params['colorType'] = 'hue'
    s.params['hStart'] = hStart
    s.params['hEnd'] = hEnd
    s.params['direction'] = direction
    s
  }

  /**
   * Creates a two-color gradient color scale.
   *
   * @param low low-end color
   * @param high high-end color
   * @param mid optional midpoint color
   * @param midpoint optional midpoint value
   * @return scale instance
   */
  static Scale gradient(String low = '#132B43', String high = '#56B1F7',
                         String mid = null, Number midpoint = null) {
    Scale s = new Scale(type: ScaleType.CONTINUOUS)
    s.params['colorType'] = 'gradient'
    s.params['low'] = low
    s.params['high'] = high
    if (mid != null) {
      s.params['mid'] = mid
      s.params['midpoint'] = midpoint
    }
    s
  }

  /**
   * Creates a multi-stop gradient color scale.
   *
   * @param colors list of colors
   * @param values optional stop positions (0-1)
   * @return scale instance
   */
  static Scale gradientN(List<String> colors, List<BigDecimal> values = null) {
    Scale s = new Scale(type: ScaleType.CONTINUOUS)
    s.params['colorType'] = 'gradientN'
    s.params['colors'] = colors
    if (values != null) {
      s.params['gradientValues'] = values
    }
    s
  }

  /**
   * Creates a stepped two-color continuous scale.
   *
   * @param low low-end color
   * @param high high-end color
   * @param nBreaks number of bins
   * @return scale instance
   */
  static Scale steps(String low = '#132B43', String high = '#56B1F7', int nBreaks = 6) {
    Scale s = new Scale(type: ScaleType.CONTINUOUS)
    s.params['colorType'] = 'steps'
    s.params['low'] = low
    s.params['high'] = high
    s.params['nBreaks'] = nBreaks
    s
  }

  /**
   * Creates a stepped diverging continuous scale.
   *
   * @param low low-end color
   * @param mid middle color
   * @param high high-end color
   * @param nBreaks number of bins
   * @return scale instance
   */
  static Scale steps2(String low = '#832424', String mid = '#FFFFFF', String high = '#3B4CC0', int nBreaks = 7) {
    Scale s = new Scale(type: ScaleType.CONTINUOUS)
    s.params['colorType'] = 'steps2'
    s.params['low'] = low
    s.params['mid'] = mid
    s.params['high'] = high
    s.params['nBreaks'] = nBreaks
    s
  }

  /**
   * Creates a stepped multi-color continuous scale.
   *
   * @param colors ordered colors
   * @return scale instance
   */
  static Scale stepsN(List<String> colors) {
    Scale s = new Scale(type: ScaleType.CONTINUOUS)
    s.params['colorType'] = 'stepsN'
    s.params['colors'] = colors
    s
  }

  /**
   * Creates a viridis discrete color scale.
   *
   * @param option palette option (viridis, magma, inferno, plasma, etc.)
   * @param begin start of range (0-1)
   * @param end end of range (0-1)
   * @param direction 1 for normal, -1 for reversed
   * @param alpha transparency (0-1)
   * @return scale instance
   */
  static Scale viridis(String option = 'viridis', BigDecimal begin = 0.0,
                        BigDecimal end = 1.0, int direction = 1, BigDecimal alpha = 1.0) {
    Scale s = new Scale(type: ScaleType.DISCRETE)
    s.params['colorType'] = 'viridis_d'
    s.params['option'] = option
    s.params['begin'] = begin
    s.params['end'] = end
    s.params['direction'] = direction
    s.params['alpha'] = alpha
    s
  }

  /**
   * Creates an identity pass-through color scale.
   *
   * @param naValue color for missing values
   * @return scale instance
   */
  static Scale identity(String naValue = '#999999') {
    Scale s = new Scale(type: ScaleType.DISCRETE)
    s.params['colorType'] = 'identity'
    s.params['naValue'] = naValue
    s
  }

  /**
   * Creates a binned scale.
   *
   * @return scale instance
   */
  static Scale binned() {
    new Scale(type: ScaleType.BINNED)
  }

  /**
   * Creates a continuous radius scale.
   *
   * @param min minimum radius
   * @param max maximum radius
   * @return scale instance
   */
  static Scale radius(Number min = 1.0, Number max = 6.0) {
    Scale s = new Scale(type: ScaleType.CONTINUOUS)
    s.params['range'] = [min, max]
    s.params['aesthetic'] = 'radius'
    s
  }

  /**
   * Attaches secondary-axis metadata to this scale.
   *
   * @param secondaryAxis free-form secondary-axis parameters
   * @return this scale
   */
  Scale secondaryAxis(Map<String, Object> secondaryAxis) {
    params['secondaryAxis'] = secondaryAxis == null ? [:] : new LinkedHashMap<>(secondaryAxis)
    this
  }

  /**
   * Attaches a guide configuration to this scale.
   *
   * <p>Accepted values are {@link GuideSpec}, {@link GuideType}, guide type strings
   * (for example {@code 'legend'} or {@code 'colorbar'}), or {@code false} for
   * {@link GuideType#NONE}. Passing {@code null} removes any attached guide.</p>
   *
   * @param value guide spec
   * @return this scale
   */
  Scale guide(GuideSpec value) {
    setGuideSpec(value?.copy())
  }

  /**
   * Attaches a guide configuration to this scale.
   *
   * @param value guide type
   * @return this scale
   */
  Scale guide(GuideType value) {
    setGuideSpec(value == null ? null : new GuideSpec(value))
  }

  /**
   * Attaches a guide configuration to this scale.
   *
   * @param value guide type name
   * @return this scale
   */
  Scale guide(String value) {
    setGuideSpec(GuideUtils.coerceGuide(value, 'scale'))
  }

  /**
   * Attaches a guide configuration to this scale.
   *
   * @param value false suppresses guide rendering
   * @return this scale
   */
  Scale guide(boolean value) {
    if (value) {
      throw new CharmValidationException('Cannot set scale guide to true — use a GuideSpec, GuideType, or guide type name')
    }
    setGuideSpec(GuideSpec.none())
  }

  /**
   * Removes any attached guide configuration.
   *
   * @param value null guide value
   * @return this scale
   */
  @SuppressWarnings('UnusedMethodParameter')
  Scale guide(Void value) {
    setGuideSpec(null)
  }

  private Scale setGuideSpec(GuideSpec guideSpec) {
    if (guideSpec == null) {
      params.remove('guide')
    } else {
      params['guide'] = guideSpec
    }
    this
  }

  /**
   * Copies this scale.
   *
   * @return copied scale
   */
  Scale copy() {
    Map<String, Object> copiedParams = [:]
    params.each { String key, Object value ->
      copiedParams[key] = SpecCopyUtil.deepCopyValue(value)
    }
    new Scale(
        type: type,
        transformStrategy: transformStrategy,
        breaks: new ArrayList<>(breaks),
        labels: new ArrayList<>(labels),
        params: copiedParams
    )
  }

}
