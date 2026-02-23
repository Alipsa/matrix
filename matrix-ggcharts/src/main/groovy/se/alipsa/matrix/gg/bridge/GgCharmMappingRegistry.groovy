package se.alipsa.matrix.gg.bridge

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmCoordType
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.Scale as CharmScale
import se.alipsa.matrix.gg.coord.Coord as GgCoord
import se.alipsa.matrix.gg.coord.CoordCartesian
import se.alipsa.matrix.gg.coord.CoordFixed
import se.alipsa.matrix.gg.coord.CoordFlip
import se.alipsa.matrix.gg.coord.CoordMap
import se.alipsa.matrix.gg.coord.CoordPolar
import se.alipsa.matrix.gg.coord.CoordQuickmap
import se.alipsa.matrix.gg.coord.CoordRadial
import se.alipsa.matrix.gg.coord.CoordSf
import se.alipsa.matrix.gg.coord.CoordTrans
import se.alipsa.matrix.gg.geom.Geom as GgGeom
import se.alipsa.matrix.gg.geom.GeomBar
import se.alipsa.matrix.gg.geom.GeomBoxplot
import se.alipsa.matrix.gg.geom.GeomCol
import se.alipsa.matrix.gg.geom.GeomHistogram
import se.alipsa.matrix.gg.geom.GeomLine
import se.alipsa.matrix.gg.geom.GeomPoint
import se.alipsa.matrix.gg.geom.GeomSmooth
import se.alipsa.matrix.gg.geom.GeomTile
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.scale.Scale as GgScale
import se.alipsa.matrix.gg.scale.ScaleContinuous
import se.alipsa.matrix.gg.scale.ScaleDiscrete
import se.alipsa.matrix.gg.scale.ScaleXDate
import se.alipsa.matrix.gg.scale.ScaleXDatetime
import se.alipsa.matrix.gg.scale.ScaleXLog10
import se.alipsa.matrix.gg.scale.ScaleXReverse
import se.alipsa.matrix.gg.scale.ScaleXSqrt
import se.alipsa.matrix.gg.scale.ScaleXTime
import se.alipsa.matrix.gg.scale.ScaleYDate
import se.alipsa.matrix.gg.scale.ScaleYDatetime
import se.alipsa.matrix.gg.scale.ScaleYLog10
import se.alipsa.matrix.gg.scale.ScaleYReverse
import se.alipsa.matrix.gg.scale.ScaleYSqrt
import se.alipsa.matrix.gg.scale.ScaleYTime

/**
 * Registry/strategy mapper from gg domain objects to canonical Charm targets.
 *
 * The registry intentionally maps broad gg class families to parameterized Charm
 * model types, avoiding one adapter implementation per gg subtype.
 */
@CompileStatic
class GgCharmMappingRegistry {

  private final Map<Class<? extends GgGeom>, CharmGeomType> geomMappings = [
      (GeomPoint)    : CharmGeomType.POINT,
      (GeomLine)     : CharmGeomType.LINE,
      (GeomSmooth)   : CharmGeomType.SMOOTH,
      (GeomTile)     : CharmGeomType.TILE,
      (GeomCol)      : CharmGeomType.COL,
      (GeomBar)      : CharmGeomType.BAR,
      (GeomHistogram): CharmGeomType.HISTOGRAM,
      (GeomBoxplot)  : CharmGeomType.BOXPLOT
  ] as Map<Class<? extends GgGeom>, CharmGeomType>

  private final Map<StatType, CharmStatType> statMappings = [
      (StatType.IDENTITY)  : CharmStatType.IDENTITY,
      (StatType.COUNT)     : CharmStatType.COUNT,
      (StatType.BIN)       : CharmStatType.BIN,
      (StatType.BOXPLOT)   : CharmStatType.BOXPLOT,
      (StatType.SMOOTH)    : CharmStatType.SMOOTH,
      (StatType.QUANTILE)  : CharmStatType.QUANTILE,
      (StatType.SUMMARY)   : CharmStatType.SUMMARY,
      (StatType.DENSITY)   : CharmStatType.DENSITY,
      (StatType.YDENSITY)  : CharmStatType.YDENSITY,
      (StatType.DENSITY_2D): CharmStatType.DENSITY_2D,
      (StatType.BIN2D)     : CharmStatType.BIN2D,
      (StatType.BIN_HEX)   : CharmStatType.BIN_HEX,
      (StatType.SUMMARY_HEX): CharmStatType.SUMMARY_HEX,
      (StatType.SUMMARY_2D): CharmStatType.SUMMARY_2D,
      (StatType.CONTOUR)   : CharmStatType.CONTOUR,
      (StatType.ECDF)      : CharmStatType.ECDF,
      (StatType.QQ)        : CharmStatType.QQ,
      (StatType.QQ_LINE)   : CharmStatType.QQ_LINE,
      (StatType.ELLIPSE)   : CharmStatType.ELLIPSE,
      (StatType.SUMMARY_BIN): CharmStatType.SUMMARY_BIN,
      (StatType.UNIQUE)    : CharmStatType.UNIQUE,
      (StatType.FUNCTION)  : CharmStatType.FUNCTION,
      (StatType.SF)        : CharmStatType.SF,
      (StatType.SF_COORDINATES): CharmStatType.SF_COORDINATES,
      (StatType.SPOKE)     : CharmStatType.SPOKE,
      (StatType.ALIGN)     : CharmStatType.ALIGN
  ] as Map<StatType, CharmStatType>

  private final Map<PositionType, CharmPositionType> positionMappings = [
      (PositionType.IDENTITY): CharmPositionType.IDENTITY,
      (PositionType.STACK)   : CharmPositionType.STACK,
      (PositionType.DODGE)   : CharmPositionType.DODGE,
      (PositionType.DODGE2)  : CharmPositionType.DODGE2,
      (PositionType.FILL)    : CharmPositionType.FILL,
      (PositionType.JITTER)  : CharmPositionType.JITTER,
      (PositionType.NUDGE)   : CharmPositionType.NUDGE
  ] as Map<PositionType, CharmPositionType>

  // Subclasses must appear before parent classes so isInstance checks match the most specific type first.
  private final Map<Class<? extends GgCoord>, CharmCoordType> coordMappings = [
      (CoordQuickmap) : CharmCoordType.QUICKMAP,
      (CoordSf)       : CharmCoordType.SF,
      (CoordMap)      : CharmCoordType.MAP,
      (CoordRadial)   : CharmCoordType.RADIAL,
      (CoordTrans)    : CharmCoordType.TRANS,
      (CoordFixed)    : CharmCoordType.FIXED,
      (CoordCartesian): CharmCoordType.CARTESIAN,
      (CoordFlip)     : CharmCoordType.FLIP,
      (CoordPolar)    : CharmCoordType.POLAR
  ] as Map<Class<? extends GgCoord>, CharmCoordType>

  /**
   * Maps a gg geom instance to a canonical Charm geom type.
   *
   * @param geom gg geom
   * @return mapped CharmGeomType, or null when unsupported
   */
  CharmGeomType mapGeom(GgGeom geom) {
    if (geom == null) {
      return null
    }
    for (Map.Entry<Class<? extends GgGeom>, CharmGeomType> entry : geomMappings.entrySet()) {
      if (entry.key.isInstance(geom)) {
        return entry.value
      }
    }
    null
  }

  /**
   * Maps a gg stat enum to a CharmStatType.
   *
   * @param statType gg stat
   * @return mapped CharmStatType, or null when unsupported
   */
  CharmStatType mapStat(StatType statType) {
    if (statType == null) {
      return CharmStatType.IDENTITY
    }
    statMappings[statType]
  }

  /**
   * Maps a gg position enum to a CharmPositionType.
   *
   * @param positionType gg position
   * @return mapped CharmPositionType, or null when unsupported
   */
  CharmPositionType mapPosition(PositionType positionType) {
    if (positionType == null) {
      return CharmPositionType.IDENTITY
    }
    positionMappings[positionType]
  }

  /**
   * Maps a gg coord instance to a CharmCoordType.
   *
   * @param coord gg coord
   * @return mapped CharmCoordType, or null when unsupported
   */
  CharmCoordType mapCoordType(GgCoord coord) {
    if (coord == null) {
      return CharmCoordType.CARTESIAN
    }
    for (Map.Entry<Class<? extends GgCoord>, CharmCoordType> entry : coordMappings.entrySet()) {
      if (entry.key.isInstance(coord)) {
        return entry.value
      }
    }
    null
  }

  /**
   * Maps an explicit gg scale to a Charm scale.
   *
   * @param scale gg scale
   * @param aesthetic normalized aesthetic (`x`, `y`, `color`, `fill`)
   * @return mapped Charm scale, or null when unsupported
   */
  CharmScale mapScale(GgScale scale, String aesthetic) {
    if (scale == null) {
      return null
    }
    return switch (aesthetic) {
      case 'x', 'y' -> mapPositionalScale(scale)
      case 'color', 'fill' -> scale instanceof ScaleDiscrete ? CharmScale.discrete() : null
      default -> null
    }
  }

  /**
   * Normalizes gg aesthetic naming variants.
   *
   * @param aesthetic aesthetic name
   * @return normalized name
   */
  static String normalizeAesthetic(String aesthetic) {
    if (aesthetic == null) {
      return null
    }
    String normalized = aesthetic.trim().toLowerCase(Locale.ROOT)
    normalized == 'colour' ? 'color' : normalized
  }

  private static CharmScale mapPositionalScale(GgScale scale) {
    if (scale instanceof ScaleXLog10 || scale instanceof ScaleYLog10) {
      return CharmScale.transform('log10')
    }
    if (scale instanceof ScaleXSqrt || scale instanceof ScaleYSqrt) {
      return CharmScale.transform('sqrt')
    }
    if (scale instanceof ScaleXReverse || scale instanceof ScaleYReverse) {
      return CharmScale.transform('reverse')
    }
    if (scale instanceof ScaleXDate || scale instanceof ScaleYDate) {
      return CharmScale.date()
    }
    if (scale instanceof ScaleXTime || scale instanceof ScaleYTime ||
        scale instanceof ScaleXDatetime || scale instanceof ScaleYDatetime) {
      return null
    }
    if (scale instanceof ScaleDiscrete) {
      return CharmScale.discrete()
    }
    if (scale instanceof ScaleContinuous) {
      return CharmScale.continuous()
    }
    null
  }
}
