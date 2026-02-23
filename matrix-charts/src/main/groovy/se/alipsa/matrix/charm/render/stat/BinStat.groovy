package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.ValueConverter

/**
 * Bin stat transformation - bins continuous data into intervals.
 * Used by geom_histogram.
 *
 * <p>Supports params:</p>
 * <ul>
 *   <li>{@code bins} - number of bins (default 30)</li>
 *   <li>{@code binwidth} - explicit bin width (overrides bins)</li>
 *   <li>{@code boundary} - bin boundary alignment</li>
 *   <li>{@code closed} - which side is closed ('left' or 'right', default 'right')</li>
 * </ul>
 */
@CompileStatic
class BinStat {

  /**
   * Bins x values from data and returns one LayerData per bin with count as y.
   *
   * @param layer layer specification (params: bins, binwidth, boundary, closed)
   * @param data layer data
   * @return binned LayerData with x=center, y=count, meta: binStart, binEnd, density, xmin, xmax
   */
  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    List<BigDecimal> values = data.collect { LayerData d ->
      ValueConverter.asBigDecimal(d.x)
    }.findAll { BigDecimal v -> v != null } as List<BigDecimal>

    if (values.isEmpty()) {
      return []
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    BigDecimal min = values.min()
    BigDecimal max = values.max()
    if (min == max) {
      max = max + 1
    }

    BigDecimal range = max - min
    int bins
    BigDecimal binwidth

    BigDecimal paramBinwidth = ValueConverter.asBigDecimal(params.binwidth)
    if (paramBinwidth != null && paramBinwidth > 0) {
      binwidth = paramBinwidth
      int rawBins = (range / binwidth).toBigInteger().intValue()
      bins = 1.max(rawBins) as int
      // Extend max to cover full bins
      max = min + binwidth * bins
    } else {
      BigDecimal paramBins = ValueConverter.asBigDecimal(params.bins)
      bins = paramBins != null ? paramBins.intValue() : 30
      if (bins < 1) bins = 30
      binwidth = range / bins
    }

    // Boundary alignment
    BigDecimal boundary = ValueConverter.asBigDecimal(params.boundary)
    if (boundary != null) {
      BigDecimal shift = ((min - boundary) / binwidth).toBigInteger() as BigDecimal
      min = boundary + shift * binwidth
      max = min + bins * binwidth
    }

    int totalValues = values.size()
    List<Integer> counts = new ArrayList<>(Collections.nCopies(bins, 0))
    values.each { BigDecimal value ->
      int idx = binwidth > 0 ? ((value - min) / binwidth).intValue() : 0
      idx = 0.max(idx.min(bins - 1)) as int
      counts[idx] = counts[idx] + 1
    }

    LayerData template = data.first()
    List<LayerData> result = []
    for (int i = 0; i < bins; i++) {
      BigDecimal binStart = min + binwidth * i
      BigDecimal binEnd = binStart + binwidth
      BigDecimal center = binStart + binwidth / 2
      BigDecimal density = binwidth > 0 ? counts[i] / (totalValues * binwidth) : 0

      LayerData datum = new LayerData(
          x: center,
          y: counts[i],
          color: template?.color,
          fill: template?.fill,
          rowIndex: -1
      )
      datum.meta.binStart = binStart
      datum.meta.binEnd = binEnd
      datum.meta.density = density
      datum.meta.xmin = binStart
      datum.meta.xmax = binEnd
      result << datum
    }
    result
  }
}
