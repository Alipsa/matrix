package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Transformation strategy for Charm scales.
 */
@CompileStatic
interface ScaleTransform {

  /**
   * Returns the stable transform identifier.
   *
   * @return transform id
   */
  String id()

  /**
   * Applies forward transform.
   *
   * @param value input value
   * @return transformed value
   */
  BigDecimal apply(BigDecimal value)

  /**
   * Applies inverse transform.
   *
   * @param value transformed value
   * @return original-space value
   */
  BigDecimal invert(BigDecimal value)
}

@CompileStatic
abstract class BaseScaleTransform implements ScaleTransform {

  @Override
  String toString() {
    id()
  }
}

@CompileStatic
class Log10ScaleTransform extends BaseScaleTransform {

  @Override
  String id() {
    'log10'
  }

  @Override
  BigDecimal apply(BigDecimal value) {
    value == null || value <= 0 ? null : value.log10()
  }

  @Override
  BigDecimal invert(BigDecimal value) {
    value == null ? null : (10 ** value) as BigDecimal
  }
}

@CompileStatic
class SqrtScaleTransform extends BaseScaleTransform {

  @Override
  String id() {
    'sqrt'
  }

  @Override
  BigDecimal apply(BigDecimal value) {
    value == null || value < 0 ? null : value.sqrt()
  }

  @Override
  BigDecimal invert(BigDecimal value) {
    value == null ? null : value ** 2
  }
}

@CompileStatic
class ReverseScaleTransform extends BaseScaleTransform {

  @Override
  String id() {
    'reverse'
  }

  @Override
  BigDecimal apply(BigDecimal value) {
    value == null ? null : -value
  }

  @Override
  BigDecimal invert(BigDecimal value) {
    value == null ? null : -value
  }
}

@CompileStatic
class DateScaleTransform extends BaseScaleTransform {

  @Override
  String id() {
    'date'
  }

  @Override
  BigDecimal apply(BigDecimal value) {
    value
  }

  @Override
  BigDecimal invert(BigDecimal value) {
    value
  }
}

@CompileStatic
class TimeScaleTransform extends BaseScaleTransform {

  @Override
  String id() {
    'time'
  }

  @Override
  BigDecimal apply(BigDecimal value) {
    value
  }

  @Override
  BigDecimal invert(BigDecimal value) {
    value
  }
}

@CompileStatic
class CustomScaleTransform extends BaseScaleTransform {

  private final String transformId
  private final Closure<BigDecimal> forward
  private final Closure<BigDecimal> inverse

  /**
   * Creates a custom transform.
   *
   * @param transformId transform id
   * @param forward forward transform closure
   * @param inverse inverse transform closure
   */
  CustomScaleTransform(String transformId, Closure<BigDecimal> forward, Closure<BigDecimal> inverse = null) {
    String value = transformId?.trim()
    if (!value) {
      throw new IllegalArgumentException('transformId cannot be blank')
    }
    if (forward == null) {
      throw new IllegalArgumentException('forward transform closure cannot be null')
    }
    this.transformId = value
    this.forward = forward
    this.inverse = inverse
  }

  @Override
  String id() {
    transformId
  }

  @Override
  BigDecimal apply(BigDecimal value) {
    value == null ? null : forward.call(value)
  }

  @Override
  BigDecimal invert(BigDecimal value) {
    if (value == null) {
      return null
    }
    if (inverse != null) {
      return inverse.call(value)
    }
    value
  }
}

/**
 * Registry and factory methods for transform strategies.
 */
@CompileStatic
class ScaleTransforms {

  private static final Map<String, ScaleTransform> BUILTIN = [
      log10  : new Log10ScaleTransform(),
      sqrt   : new SqrtScaleTransform(),
      reverse: new ReverseScaleTransform(),
      date   : new DateScaleTransform(),
      time   : new TimeScaleTransform()
  ]

  /**
   * Resolves a transform from object input.
   *
   * @param value transform instance or transform id
   * @return transform strategy
   */
  static ScaleTransform resolve(Object value) {
    if (value == null) {
      return null
    }
    if (value instanceof ScaleTransform) {
      return value as ScaleTransform
    }
    if (value instanceof CharSequence) {
      return named(value.toString())
    }
    throw new CharmValidationException("Unsupported transform input: ${value.getClass().name}")
  }

  /**
   * Looks up a named built-in transform.
   *
   * @param name transform name
   * @return transform strategy
   */
  static ScaleTransform named(String name) {
    String key = name?.trim()?.toLowerCase(Locale.ROOT)
    ScaleTransform transform = key == null ? null : BUILTIN[key]
    if (transform == null) {
      throw new CharmValidationException(
          "Unsupported transform '${name}'. Supported transforms: ${BUILTIN.keySet().join(', ')}"
      )
    }
    transform
  }

  /**
   * Creates a custom transform strategy.
   *
   * @param id transform id
   * @param forward forward transform closure
   * @param inverse inverse transform closure
   * @return custom transform
   */
  static ScaleTransform custom(String id, Closure<BigDecimal> forward, Closure<BigDecimal> inverse = null) {
    new CustomScaleTransform(id, forward, inverse)
  }
}
