package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.core.ValueConverter

/**
 * Lightweight contour stat that groups x/y samples by z-level slices.
 */
@CompileStatic
class ContourStat {

  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    if (data == null || data.isEmpty()) {
      return []
    }

    List<LayerData> withZ = data.findAll { LayerData datum ->
      Object zCandidate = datum.label != null ? datum.label : datum.meta?.z
      ValueConverter.asBigDecimal(datum.x) != null &&
          ValueConverter.asBigDecimal(datum.y) != null &&
          ValueConverter.asBigDecimal(zCandidate) != null
    }
    if (withZ.isEmpty()) {
      return data
    }

    int bins = ValueConverter.asBigDecimal(StatEngine.effectiveParams(layer).bins)?.intValue() ?: 10
    if (bins < 1) {
      bins = 10
    }

    List<BigDecimal> zValues = withZ.collect { LayerData datum ->
      Object zCandidate = datum.label != null ? datum.label : datum.meta?.z
      ValueConverter.asBigDecimal(zCandidate)
    }
    BigDecimal zMin = zValues.min()
    BigDecimal zMax = zValues.max()
    if (zMin == zMax) {
      zMax = zMax + 1
    }
    BigDecimal step = (zMax - zMin) / bins
    if (step <= 0) {
      step = 1
    }

    List<LayerData> result = []
    withZ.each { LayerData datum ->
      Object zCandidate = datum.label != null ? datum.label : datum.meta?.z
      BigDecimal z = ValueConverter.asBigDecimal(zCandidate)
      int levelIdx = ((z - zMin) / step).intValue()
      if (levelIdx < 0) levelIdx = 0
      if (levelIdx > bins - 1) levelIdx = bins - 1
      BigDecimal level = zMin + levelIdx * step
      LayerData out = new LayerData(
          x: datum.x,
          y: datum.y,
          color: datum.color,
          fill: datum.fill,
          group: "level-${levelIdx}",
          rowIndex: datum.rowIndex
      )
      out.meta.level = level
      out.meta.z = z
      result << out
    }

    result.sort { LayerData a, LayerData b ->
      int g = (a.group?.toString() ?: '') <=> (b.group?.toString() ?: '')
      if (g != 0) {
        return g
      }
      BigDecimal x1 = ValueConverter.asBigDecimal(a.x) ?: 0
      BigDecimal x2 = ValueConverter.asBigDecimal(b.x) ?: 0
      x1 <=> x2
    }
  }
}
