package se.alipsa.matrix.charm

import groovy.transform.CompileStatic

/**
 * Base type for annotations defined in the plot specification.
 */
@CompileStatic
abstract class AnnotationSpec {

  /**
   * Creates a deep copy of this annotation.
   *
   * @return copied annotation
   */
  abstract AnnotationSpec copy()
}

/**
 * Text annotation specification.
 */
@CompileStatic
class TextAnnotationSpec extends AnnotationSpec {

  Number x
  Number y
  String label
  Map<String, Object> params = [:]

  /**
   * Copies this text annotation.
   *
   * @return copied annotation
   */
  @Override
  AnnotationSpec copy() {
    new TextAnnotationSpec(x: x, y: y, label: label, params: new LinkedHashMap<>(params))
  }
}

/**
 * Rectangle annotation specification.
 */
@CompileStatic
class RectAnnotationSpec extends AnnotationSpec {

  Number xmin
  Number xmax
  Number ymin
  Number ymax
  Map<String, Object> params = [:]

  /**
   * Copies this rectangle annotation.
   *
   * @return copied annotation
   */
  @Override
  AnnotationSpec copy() {
    new RectAnnotationSpec(
        xmin: xmin,
        xmax: xmax,
        ymin: ymin,
        ymax: ymax,
        params: new LinkedHashMap<>(params)
    )
  }
}

/**
 * Segment annotation specification.
 */
@CompileStatic
class SegmentAnnotationSpec extends AnnotationSpec {

  Number x
  Number xend
  Number y
  Number yend
  Map<String, Object> params = [:]

  /**
   * Copies this segment annotation.
   *
   * @return copied annotation
   */
  @Override
  AnnotationSpec copy() {
    new SegmentAnnotationSpec(
        x: x,
        xend: xend,
        y: y,
        yend: yend,
        params: new LinkedHashMap<>(params)
    )
  }
}
