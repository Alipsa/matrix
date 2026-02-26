package se.alipsa.matrix.charm

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Base type for annotations defined in the plot specification.
 */
@CompileStatic
abstract class AnnotationSpec {

  /**
   * Draw order relative to layers within a panel.
   * Lower values render earlier (behind higher-order elements).
   */
  int drawOrder = 0

  /**
   * Creates a deep copy of this annotation.
   *
   * @return copied annotation
   */
  abstract AnnotationSpec copy()
}

/**
 * Text annotation specification.
 *
 * <p>Common styling properties ({@code color}, {@code fill}, {@code alpha})
 * are declared as explicit fields for IDE autocomplete. Values are written
 * through to {@link #params} so the renderer reads them unchanged.</p>
 */
@CompileStatic
class TextAnnotationSpec extends AnnotationSpec {

  Number x
  Number y
  String label
  Map<String, Object> params = [:]

  /** Stroke/text colour (write-through to params). */
  String color

  /** Fill colour (write-through to params). */
  String fill

  /** Opacity 0–1 (write-through to params). */
  Number alpha

  void setColor(String value) {
    this.@color = value
    params['color'] = value
  }

  /**
   * Sets the colour aesthetic (British spelling alias).
   *
   * @param value colour value
   */
  void setColour(String value) {
    setColor(value)
  }

  void setFill(String value) {
    this.@fill = value
    params['fill'] = value
  }

  void setAlpha(Number value) {
    this.@alpha = value
    params['alpha'] = value
  }

  /**
   * Copies this text annotation.
   *
   * @return copied annotation
   */
  @Override
  AnnotationSpec copy() {
    TextAnnotationSpec c = new TextAnnotationSpec(
        x: x,
        y: y,
        label: label,
        drawOrder: drawOrder,
        params: new LinkedHashMap<>(params)
    )
    c.@color = this.@color
    c.@fill = this.@fill
    c.@alpha = this.@alpha
    c
  }
}

/**
 * Rectangle annotation specification.
 *
 * <p>Common styling properties ({@code color}, {@code fill}, {@code alpha})
 * are declared as explicit fields for IDE autocomplete. Values are written
 * through to {@link #params} so the renderer reads them unchanged.</p>
 */
@CompileStatic
class RectAnnotationSpec extends AnnotationSpec {

  Number xmin
  Number xmax
  Number ymin
  Number ymax
  Map<String, Object> params = [:]

  /** Stroke colour (write-through to params). */
  String color

  /** Fill colour (write-through to params). */
  String fill

  /** Opacity 0–1 (write-through to params). */
  Number alpha

  void setColor(String value) {
    this.@color = value
    params['color'] = value
  }

  /**
   * Sets the colour aesthetic (British spelling alias).
   *
   * @param value colour value
   */
  void setColour(String value) {
    setColor(value)
  }

  void setFill(String value) {
    this.@fill = value
    params['fill'] = value
  }

  void setAlpha(Number value) {
    this.@alpha = value
    params['alpha'] = value
  }

  /**
   * Copies this rectangle annotation.
   *
   * @return copied annotation
   */
  @Override
  AnnotationSpec copy() {
    RectAnnotationSpec c = new RectAnnotationSpec(
        xmin: xmin,
        xmax: xmax,
        ymin: ymin,
        ymax: ymax,
        drawOrder: drawOrder,
        params: new LinkedHashMap<>(params)
    )
    c.@color = this.@color
    c.@fill = this.@fill
    c.@alpha = this.@alpha
    c
  }
}

/**
 * Segment annotation specification.
 *
 * <p>Common styling properties ({@code color}, {@code fill}, {@code alpha})
 * are declared as explicit fields for IDE autocomplete. Values are written
 * through to {@link #params} so the renderer reads them unchanged.</p>
 */
@CompileStatic
class SegmentAnnotationSpec extends AnnotationSpec {

  Number x
  Number xend
  Number y
  Number yend
  Map<String, Object> params = [:]

  /** Stroke colour (write-through to params). */
  String color

  /** Fill colour (write-through to params). */
  String fill

  /** Opacity 0–1 (write-through to params). */
  Number alpha

  void setColor(String value) {
    this.@color = value
    params['color'] = value
  }

  /**
   * Sets the colour aesthetic (British spelling alias).
   *
   * @param value colour value
   */
  void setColour(String value) {
    setColor(value)
  }

  void setFill(String value) {
    this.@fill = value
    params['fill'] = value
  }

  void setAlpha(Number value) {
    this.@alpha = value
    params['alpha'] = value
  }

  /**
   * Copies this segment annotation.
   *
   * @return copied annotation
   */
  @Override
  AnnotationSpec copy() {
    SegmentAnnotationSpec c = new SegmentAnnotationSpec(
        x: x,
        xend: xend,
        y: y,
        yend: yend,
        drawOrder: drawOrder,
        params: new LinkedHashMap<>(params)
    )
    c.@color = this.@color
    c.@fill = this.@fill
    c.@alpha = this.@alpha
    c
  }
}

/**
 * Custom grob annotation specification.
 */
@CompileStatic
class CustomAnnotationSpec extends AnnotationSpec {

  Object grob
  Number xmin
  Number xmax
  Number ymin
  Number ymax
  Map<String, Object> params = [:]

  /**
   * Copies this custom annotation.
   *
   * @return copied annotation
   */
  @Override
  AnnotationSpec copy() {
    new CustomAnnotationSpec(
        grob: grob,
        xmin: xmin,
        xmax: xmax,
        ymin: ymin,
        ymax: ymax,
        drawOrder: drawOrder,
        params: new LinkedHashMap<>(params)
    )
  }
}

/**
 * Log tick annotation specification.
 */
@CompileStatic
class LogticksAnnotationSpec extends AnnotationSpec {

  Map<String, Object> params = [:]

  /**
   * Copies this logticks annotation.
   *
   * @return copied annotation
   */
  @Override
  AnnotationSpec copy() {
    new LogticksAnnotationSpec(drawOrder: drawOrder, params: new LinkedHashMap<>(params))
  }
}

/**
 * Raster annotation specification.
 */
@CompileStatic
class RasterAnnotationSpec extends AnnotationSpec {

  List<List<String>> raster = []
  Number xmin
  Number xmax
  Number ymin
  Number ymax
  boolean interpolate = false
  Map<String, Object> params = [:]

  /**
   * Copies this raster annotation.
   *
   * @return copied annotation
   */
  @Override
  AnnotationSpec copy() {
    new RasterAnnotationSpec(
        raster: raster.collect { List<String> row -> row == null ? [] : new ArrayList<>(row) },
        xmin: xmin,
        xmax: xmax,
        ymin: ymin,
        ymax: ymax,
        interpolate: interpolate,
        drawOrder: drawOrder,
        params: new LinkedHashMap<>(params)
    )
  }
}

/**
 * Map annotation specification.
 */
@CompileStatic
class MapAnnotationSpec extends AnnotationSpec {

  Matrix map
  Matrix data
  Map<String, String> mapping = [:]
  Map<String, Object> params = [:]

  /**
   * Copies this map annotation.
   *
   * @return copied annotation
   */
  @Override
  AnnotationSpec copy() {
    new MapAnnotationSpec(
        map: map,
        data: data,
        drawOrder: drawOrder,
        mapping: new LinkedHashMap<>(mapping),
        params: new LinkedHashMap<>(params)
    )
  }
}
