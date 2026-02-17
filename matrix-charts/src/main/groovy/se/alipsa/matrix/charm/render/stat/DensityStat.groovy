package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.util.NumberCoercionUtil
import se.alipsa.matrix.stats.kde.KernelDensity

/**
 * Density stat transformation - kernel density estimation.
 * Delegates to {@link KernelDensity} from matrix-stats.
 *
 * <p>Supports params:</p>
 * <ul>
 *   <li>{@code kernel} - kernel type ('gaussian', 'epanechnikov', 'uniform', 'triangular')</li>
 *   <li>{@code bw} - bandwidth (overrides automatic selection)</li>
 *   <li>{@code adjust} - bandwidth adjustment factor (default 1.0)</li>
 *   <li>{@code n} - number of evaluation points (default 512)</li>
 *   <li>{@code trim} - trim density to data range (default false)</li>
 *   <li>{@code from} - custom range start</li>
 *   <li>{@code to} - custom range end</li>
 * </ul>
 */
@CompileStatic
class DensityStat {

  /**
   * Computes kernel density estimate from x values.
   *
   * @param layer layer specification
   * @param data layer data
   * @return density curves as LayerData with x and y=density
   */
  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    Map<String, Object> kdeParams = buildKdeParams(StatEngine.effectiveParams(layer))
    Map<Object, List<LayerData>> groups = groupData(data)
    List<LayerData> result = []
    groups.each { Object _, List<LayerData> bucket ->
      List<Number> values = extractNumericX(bucket)
      if (values.size() < 2) {
        return
      }

      KernelDensity kde = new KernelDensity(values, kdeParams)
      double[] xVals = kde.getX()
      double[] densityVals = kde.getDensity()
      LayerData template = bucket.first()
      for (int i = 0; i < xVals.length; i++) {
        LayerData datum = new LayerData(
            x: xVals[i] as BigDecimal,
            y: densityVals[i] as BigDecimal,
            color: template?.color,
            fill: template?.fill,
            group: template?.group,
            rowIndex: -1
        )
        result << datum
      }
    }
    result
  }

  /**
   * Extracts numeric x values from LayerData list.
   *
   * @param data layer data
   * @return list of numeric values
   */
  static List<Number> extractNumericX(List<LayerData> data) {
    List<Number> values = []
    data.each { LayerData d ->
      BigDecimal v = NumberCoercionUtil.coerceToBigDecimal(d.x)
      if (v != null) {
        values << v
      }
    }
    values
  }

  /**
   * Builds KDE parameter map from stat params.
   *
   * @param params stat specification params
   * @return KDE-compatible params map
   */
  static Map<String, Object> buildKdeParams(Map<String, Object> params) {
    Map<String, Object> kdeParams = [:]
    if (params.kernel != null) kdeParams.kernel = params.kernel
    if (params.bw != null) kdeParams.bandwidth = params.bw
    if (params.adjust != null) kdeParams.adjust = params.adjust
    if (params.n != null) kdeParams.n = params.n
    if (params.trim != null) kdeParams.trim = params.trim
    if (params.from != null) kdeParams.from = params.from
    if (params.to != null) kdeParams.to = params.to
    kdeParams
  }

  private static Map<Object, List<LayerData>> groupData(List<LayerData> data) {
    Map<Object, List<LayerData>> groups = new LinkedHashMap<>()
    data.each { LayerData datum ->
      Object key = datum.group ?: datum.color ?: datum.fill ?: '__all__'
      List<LayerData> bucket = groups[key]
      if (bucket == null) {
        bucket = []
        groups[key] = bucket
      }
      bucket << datum
    }
    groups
  }
}
