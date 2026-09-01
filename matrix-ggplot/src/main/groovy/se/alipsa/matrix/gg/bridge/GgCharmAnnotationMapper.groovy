package se.alipsa.matrix.gg.bridge

import se.alipsa.matrix.charm.AnnotationSpec
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CustomAnnotationSpec
import se.alipsa.matrix.charm.GeomSpec
import se.alipsa.matrix.charm.LogticksAnnotationSpec
import se.alipsa.matrix.charm.MapAnnotationSpec
import se.alipsa.matrix.charm.RasterAnnotationSpec
import se.alipsa.matrix.charm.RectAnnotationSpec
import se.alipsa.matrix.charm.SegmentAnnotationSpec
import se.alipsa.matrix.charm.TextAnnotationSpec
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.gg.aes.Aes as GgAes
import se.alipsa.matrix.gg.aes.Factor
import se.alipsa.matrix.gg.geom.GeomCustom
import se.alipsa.matrix.gg.geom.GeomLogticks
import se.alipsa.matrix.gg.geom.GeomMap
import se.alipsa.matrix.gg.geom.GeomRasterAnn
import se.alipsa.matrix.gg.layer.Layer

/**
 * Maps gg annotation layers (custom, logticks, raster, map, and inline
 * text/rect/segment annotations) to their Charm {@link AnnotationSpec} equivalents.
 */
class GgCharmAnnotationMapper {

  private static final String AES_X = 'x'
  private static final String AES_Y = 'y'
  private static final String AES_XEND = 'xend'
  private static final String AES_YEND = 'yend'
  private static final String AES_XMIN = 'xmin'
  private static final String AES_XMAX = 'xmax'
  private static final String AES_YMIN = 'ymin'
  private static final String AES_YMAX = 'ymax'
  private static final String AES_COLOR = 'color'
  private static final String AES_FILL = 'fill'
  private static final String AES_GROUP = 'group'
  private static final String AES_LABEL = 'label'
  private static final String AES_MAP_ID = 'map_id'

  private static final String PARAM_MAPPING = 'mapping'
  private static final String PARAM_SOURCE = '__source'
  private static final String SOURCE_GG = 'gg'
  private static final String PARAM_MAP = 'map'

  private static final List<String> MAP_ANNOTATION_AESTHETICS = [
      AES_MAP_ID, AES_X, AES_Y, AES_GROUP, AES_FILL, AES_COLOR
  ]

  static List<AnnotationSpec> mapAnnotationLayer(Layer layer, int idx, List<String> reasons) {
    if (layer == null || layer.geom == null) {
      return []
    }
    GeomSpec geomSpec
    try {
      geomSpec = layer.geom.toCharmGeomSpec()
    } catch (Exception ignored) {
      return []
    }

    switch (geomSpec.type) {
      case CharmGeomType.CUSTOM:
        return mapCustomAnnotationLayer(layer, idx, reasons)
      case CharmGeomType.LOGTICKS:
        return mapLogticksAnnotationLayer(layer, idx)
      case CharmGeomType.RASTER_ANN:
        return mapRasterAnnotationLayer(layer, idx, reasons)
      case CharmGeomType.MAP:
        if (!layer.inheritAes) {
          return mapMapAnnotationLayer(layer, idx, reasons)
        }
        return []
      case CharmGeomType.TEXT:
      case CharmGeomType.RECT:
      case CharmGeomType.SEGMENT:
        if (!layer.inheritAes) {
          return mapInlineAnnotationLayer(layer, geomSpec.type, idx, reasons)
        }
        return []
      default:
        return []
    }
  }

  private static List<AnnotationSpec> mapCustomAnnotationLayer(Layer layer, int idx, List<String> reasons) {
    if (!(layer.geom instanceof GeomCustom)) {
      reasons.add("Layer ${idx} custom annotation has incompatible geom '${layer.geom.class.simpleName}'".toString())
      return []
    }
    GeomCustom geom = layer.geom as GeomCustom
    if (geom.grob == null) {
      reasons.add("Layer ${idx} custom annotation requires a grob".toString())
      return []
    }
    Matrix bounds = layer.data
    Map<String, Object> annotationParams = filterParams(layer.params, ['grob', AES_XMIN, AES_XMAX, AES_YMIN, AES_YMAX] as Set<String>)
    markSource(annotationParams)
    Map<String, BigDecimal> bounds4 = boundsParams(bounds)
    CustomAnnotationSpec spec = new CustomAnnotationSpec(
        grob: geom.grob,
        xmin: bounds4[AES_XMIN],
        xmax: bounds4[AES_XMAX],
        ymin: bounds4[AES_YMIN],
        ymax: bounds4[AES_YMAX],
        drawOrder: idx,
        params: annotationParams
    )
    [spec]
  }

  private static void markSource(Map<String, Object> params) {
    params[PARAM_SOURCE] = SOURCE_GG
  }

  private static Map<String, BigDecimal> boundsParams(Matrix bounds) {
    [
        (AES_XMIN): se.alipsa.matrix.charm.AnnotationConstants.getPositionValue(bounds, AES_XMIN, 0),
        (AES_XMAX): se.alipsa.matrix.charm.AnnotationConstants.getPositionValue(bounds, AES_XMAX, 0),
        (AES_YMIN): se.alipsa.matrix.charm.AnnotationConstants.getPositionValue(bounds, AES_YMIN, 0),
        (AES_YMAX): se.alipsa.matrix.charm.AnnotationConstants.getPositionValue(bounds, AES_YMAX, 0)
    ]
  }

  private static List<AnnotationSpec> mapLogticksAnnotationLayer(Layer layer, int idx) {
    if (!(layer.geom instanceof GeomLogticks)) {
      return []
    }
    GeomLogticks geom = layer.geom as GeomLogticks
    Map<String, Object> params = [
        base     : geom.base,
        sides    : geom.sides,
        outside  : geom.outside,
        scaled   : geom.scaled,
        short    : geom.shortLength,
        mid      : geom.midLength,
        long     : geom.longLength,
        colour   : geom.colour,
        linewidth: geom.linewidth,
        linetype : geom.linetype,
        alpha    : geom.alpha
    ]
    params.putAll(filterParams(layer.params, [] as Set<String>))
    markSource(params)
    [new LogticksAnnotationSpec(drawOrder: idx, params: params)]
  }

  private static List<AnnotationSpec> mapRasterAnnotationLayer(Layer layer, int idx, List<String> reasons) {
    if (!(layer.geom instanceof GeomRasterAnn)) {
      reasons.add("Layer ${idx} raster annotation has incompatible geom '${layer.geom.class.simpleName}'".toString())
      return []
    }
    GeomRasterAnn geom = layer.geom as GeomRasterAnn
    if (geom.raster == null) {
      reasons.add("Layer ${idx} raster annotation requires raster data".toString())
      return []
    }
    Matrix bounds = layer.data
    Map<String, BigDecimal> bounds4 = boundsParams(bounds)
    RasterAnnotationSpec spec = new RasterAnnotationSpec(
        raster: geom.raster.collect { List<String> row -> row == null ? [] : new ArrayList<>(row) },
        xmin: bounds4[AES_XMIN],
        xmax: bounds4[AES_XMAX],
        ymin: bounds4[AES_YMIN],
        ymax: bounds4[AES_YMAX],
        interpolate: geom.interpolate,
        drawOrder: idx,
        params: filterParams(layer.params, ['raster', AES_XMIN, AES_XMAX, AES_YMIN, AES_YMAX, 'interpolate'] as Set<String>)
    )
    markSource(spec.params)
    [spec]
  }

  private static List<AnnotationSpec> mapMapAnnotationLayer(Layer layer, int idx, List<String> reasons) {
    if (!(layer.geom instanceof GeomMap)) {
      reasons.add("Layer ${idx} map annotation has incompatible geom '${layer.geom.class.simpleName}'".toString())
      return []
    }
    GeomMap geom = layer.geom as GeomMap
    Matrix mapData = geom.map ?: (layer.params?.get(PARAM_MAP) instanceof Matrix ? layer.params.get(PARAM_MAP) as Matrix : null)
    if (mapData == null) {
      reasons.add("Layer ${idx} annotation_map requires a map Matrix".toString())
      return []
    }
    MapAnnotationSpec spec = new MapAnnotationSpec(
        map: mapData,
        data: layer.data,
        drawOrder: idx,
        mapping: extractMapMapping(layer.aes),
        params: filterParams(layer.params, [PARAM_MAP, 'data', PARAM_MAPPING] as Set<String>)
    )
    markSource(spec.params)
    [spec]
  }

  private static List<AnnotationSpec> mapInlineAnnotationLayer(
      Layer layer,
      CharmGeomType type,
      int idx,
      List<String> reasons
  ) {
    Matrix data = layer.data
    GgAes aes = layer.aes
    if (data == null || aes == null || data.rowCount() == 0) {
      reasons.add("Layer ${idx} annotation ${type} is missing data/aes bindings".toString())
      return []
    }

    switch (type) {
      case CharmGeomType.TEXT:
        return mapTextAnnotations(data, aes, layer.params, idx, reasons)
      case CharmGeomType.RECT:
        return mapRectAnnotations(data, aes, layer.params, idx, reasons)
      case CharmGeomType.SEGMENT:
        return mapSegmentAnnotations(data, aes, layer.params, idx, reasons)
      default:
        return []
    }
  }

  private static List<AnnotationSpec> mapTextAnnotations(
      Matrix data,
      GgAes aes,
      Map params,
      int idx,
      List<String> reasons
  ) {
    String xCol = resolveColumnRef(aes, AES_X)
    String yCol = resolveColumnRef(aes, AES_Y)
    String labelCol = resolveColumnRef(aes, AES_LABEL)
    if (xCol == null || yCol == null || labelCol == null) {
      reasons.add("Layer ${idx} text annotation requires x, y, and label columns".toString())
      return []
    }

    Map<String, Object> style = filterParams(params, [AES_X, AES_Y, AES_LABEL] as Set<String>)
    List<AnnotationSpec> specs = []
    for (int row = 0; row < data.rowCount(); row++) {
      Number x = coerceNumber(data[row, xCol])
      Number y = coerceNumber(data[row, yCol])
      String label = data[row, labelCol]
      if (x == null || y == null || label == null) {
        continue
      }
      specs << new TextAnnotationSpec(x: x, y: y, label: label, drawOrder: idx, params: new LinkedHashMap<>(style))
    }
    specs
  }

  private static List<AnnotationSpec> mapRectAnnotations(
      Matrix data,
      GgAes aes,
      Map params,
      int idx,
      List<String> reasons
  ) {
    String xminCol = resolveColumnRef(aes, AES_XMIN)
    String xmaxCol = resolveColumnRef(aes, AES_XMAX)
    String yminCol = resolveColumnRef(aes, AES_YMIN)
    String ymaxCol = resolveColumnRef(aes, AES_YMAX)
    if (xminCol == null || xmaxCol == null || yminCol == null || ymaxCol == null) {
      reasons.add("Layer ${idx} rect annotation requires xmin, xmax, ymin, and ymax columns".toString())
      return []
    }

    Map<String, Object> style = filterParams(params, [AES_XMIN, AES_XMAX, AES_YMIN, AES_YMAX] as Set<String>)
    List<AnnotationSpec> specs = []
    for (int row = 0; row < data.rowCount(); row++) {
      Number xmin = coerceNumber(data[row, xminCol])
      Number xmax = coerceNumber(data[row, xmaxCol])
      Number ymin = coerceNumber(data[row, yminCol])
      Number ymax = coerceNumber(data[row, ymaxCol])
      if (xmin == null || xmax == null || ymin == null || ymax == null) {
        continue
      }
      specs << new RectAnnotationSpec(
          xmin: xmin,
          xmax: xmax,
          ymin: ymin,
          ymax: ymax,
          drawOrder: idx,
          params: new LinkedHashMap<>(style)
      )
    }
    specs
  }

  private static List<AnnotationSpec> mapSegmentAnnotations(
      Matrix data,
      GgAes aes,
      Map params,
      int idx,
      List<String> reasons
  ) {
    String xCol = resolveColumnRef(aes, AES_X)
    String yCol = resolveColumnRef(aes, AES_Y)
    String xendCol = resolveColumnRef(aes, AES_XEND)
    String yendCol = resolveColumnRef(aes, AES_YEND)
    if (xCol == null || yCol == null || xendCol == null || yendCol == null) {
      reasons.add("Layer ${idx} segment annotation requires x, y, xend, and yend columns".toString())
      return []
    }

    Map<String, Object> style = filterParams(params, [AES_X, AES_Y, AES_XEND, AES_YEND] as Set<String>)
    List<AnnotationSpec> specs = []
    for (int row = 0; row < data.rowCount(); row++) {
      Number x = coerceNumber(data[row, xCol])
      Number y = coerceNumber(data[row, yCol])
      Number xend = coerceNumber(data[row, xendCol])
      Number yend = coerceNumber(data[row, yendCol])
      if (x == null || y == null || xend == null || yend == null) {
        continue
      }
      specs << new SegmentAnnotationSpec(
          x: x,
          y: y,
          xend: xend,
          yend: yend,
          drawOrder: idx,
          params: new LinkedHashMap<>(style)
      )
    }
    specs
  }

  private static Map<String, String> extractMapMapping(GgAes aes) {
    Map<String, String> mapping = [:]
    if (aes == null) {
      return mapping
    }
    MAP_ANNOTATION_AESTHETICS.each { String aesthetic ->
      String value = resolveColumnRef(aes, aesthetic)
      if (value != null) {
        mapping[aesthetic] = value
      }
    }
    mapping
  }

  private static String resolveColumnRef(GgAes aes, String aesthetic) {
    if (aes == null) {
      return null
    }
    Object value = aes.getAestheticValue(aesthetic)
    if (value instanceof CharSequence) {
      return value.toString()
    }
    if (value instanceof Factor && (value as Factor).value instanceof CharSequence) {
      return (value as Factor).value.toString()
    }
    null
  }

  private static Number coerceNumber(Object value) {
    if (value instanceof Number) {
      return value as Number
    }
    ValueConverter.asBigDecimal(value)
  }

  private static Map<String, Object> filterParams(Map source, Set<String> excluded) {
    Map<String, Object> params = GgCharmCompiler.deepCopyMap(source)
    excluded.each { String key ->
      params.remove(key)
    }
    params
  }
}
