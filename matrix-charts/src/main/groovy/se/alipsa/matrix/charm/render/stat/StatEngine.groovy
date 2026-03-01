package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.util.Logger

/**
 * Central dispatch hub for statistical transformations.
 * Routes stat computation to the appropriate stat class based on the layer's stat type.
 */
@CompileStatic
class StatEngine {

  private static final Logger log = Logger.getLogger(StatEngine)

  /**
   * Merges layer params with stat spec params into a single effective params map.
   * Stat spec params take priority over layer params.
   *
   * @param layer layer specification
   * @return merged params map
   */
  static Map<String, Object> effectiveParams(LayerSpec layer) {
    Map<String, Object> merged = new LinkedHashMap<>(layer.params ?: [:])
    if (layer.statSpec.params) {
      merged.putAll(layer.statSpec.params)
    }
    merged
  }

  /**
   * Applies the statistical transformation specified by the layer's stat type.
   *
   * @param layer layer specification containing stat type and params
   * @param data raw layer data from aesthetic mapping
   * @return transformed layer data
   */
  static List<LayerData> apply(LayerSpec layer, List<LayerData> data) {
    CharmStatType statType = layer.statType
    switch (statType) {
      case CharmStatType.IDENTITY -> IdentityStat.compute(layer, data)
      case CharmStatType.COUNT -> CountStat.compute(layer, data)
      case CharmStatType.BIN -> BinStat.compute(layer, data)
      case CharmStatType.BOXPLOT -> BoxplotStat.compute(layer, data)
      case CharmStatType.SMOOTH -> SmoothStat.compute(layer, data)
      case CharmStatType.DENSITY -> DensityStat.compute(layer, data)
      case CharmStatType.YDENSITY -> YDensityStat.compute(layer, data)
      case CharmStatType.SUMMARY -> SummaryStat.compute(layer, data)
      case CharmStatType.BIN2D -> Bin2DStat.compute(layer, data)
      case CharmStatType.CONTOUR -> ContourStat.compute(layer, data)
      case CharmStatType.ECDF -> EcdfStat.compute(layer, data)
      case CharmStatType.QQ -> QqStat.compute(layer, data)
      case CharmStatType.QQ_LINE -> QqLineStat.compute(layer, data)
      case CharmStatType.FUNCTION -> FunctionStat.compute(layer, data)
      case CharmStatType.SUMMARY_BIN -> SummaryBinStat.compute(layer, data)
      case CharmStatType.UNIQUE -> UniqueStat.compute(layer, data)
      case CharmStatType.SAMPLE -> SampleStat.compute(layer, data)
      case CharmStatType.QUANTILE -> QuantileStat.compute(layer, data)
      case CharmStatType.DENSITY_2D -> Density2DStat.compute(layer, data)
      case CharmStatType.BIN_HEX -> BinHexStat.compute(layer, data)
      case CharmStatType.SUMMARY_HEX -> SummaryHexStat.compute(layer, data)
      case CharmStatType.SUMMARY_2D -> Summary2DStat.compute(layer, data)
      case CharmStatType.ELLIPSE -> EllipseStat.compute(layer, data)
      case CharmStatType.SF -> SfStat.compute(layer, data)
      case CharmStatType.SF_COORDINATES -> SfCoordinatesStat.compute(layer, data)
      case CharmStatType.SPOKE -> SpokeStat.compute(layer, data)
      case CharmStatType.ALIGN -> AlignStat.compute(layer, data)
      default -> {
        log.debug("Unimplemented stat type ${statType}, falling back to identity")
        data
      }
    }
  }
}
