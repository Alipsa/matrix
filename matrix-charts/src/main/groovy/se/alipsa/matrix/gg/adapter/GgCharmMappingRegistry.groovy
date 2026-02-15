package se.alipsa.matrix.gg.adapter

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CoordType
import se.alipsa.matrix.charm.Geom as CharmGeom
import se.alipsa.matrix.charm.Position as CharmPosition
import se.alipsa.matrix.charm.Scale as CharmScale
import se.alipsa.matrix.charm.Stat as CharmStat
import se.alipsa.matrix.gg.coord.Coord as GgCoord
import se.alipsa.matrix.gg.coord.CoordCartesian
import se.alipsa.matrix.gg.coord.CoordPolar
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

  private final Map<Class<? extends GgGeom>, CharmGeom> geomMappings = [
      (GeomPoint)    : CharmGeom.POINT,
      (GeomLine)     : CharmGeom.LINE,
      (GeomSmooth)   : CharmGeom.SMOOTH,
      (GeomTile)     : CharmGeom.TILE,
      (GeomCol)      : CharmGeom.COL,
      (GeomBar)      : CharmGeom.BAR,
      (GeomHistogram): CharmGeom.HISTOGRAM,
      (GeomBoxplot)  : CharmGeom.BOXPLOT
  ] as Map<Class<? extends GgGeom>, CharmGeom>

  private final Map<StatType, CharmStat> statMappings = [
      (StatType.IDENTITY): CharmStat.IDENTITY,
      (StatType.SMOOTH)  : CharmStat.SMOOTH
  ] as Map<StatType, CharmStat>

  private final Map<PositionType, CharmPosition> positionMappings = [
      (PositionType.IDENTITY): CharmPosition.IDENTITY,
      (PositionType.STACK)   : CharmPosition.STACK,
      (PositionType.DODGE)   : CharmPosition.DODGE
  ] as Map<PositionType, CharmPosition>

  private final Map<Class<? extends GgCoord>, CoordType> coordMappings = [
      (CoordCartesian): CoordType.CARTESIAN,
      (CoordPolar)    : CoordType.POLAR
  ] as Map<Class<? extends GgCoord>, CoordType>

  /**
   * Maps a gg geom instance to a canonical Charm geom.
   *
   * @param geom gg geom
   * @return mapped Charm geom, or null when unsupported
   */
  CharmGeom mapGeom(GgGeom geom) {
    if (geom == null) {
      return null
    }
    for (Map.Entry<Class<? extends GgGeom>, CharmGeom> entry : geomMappings.entrySet()) {
      if (entry.key.isInstance(geom)) {
        return entry.value
      }
    }
    null
  }

  /**
   * Maps a gg stat enum to a Charm stat enum.
   *
   * @param statType gg stat
   * @return mapped stat, or null when unsupported
   */
  CharmStat mapStat(StatType statType) {
    if (statType == null) {
      return CharmStat.IDENTITY
    }
    statMappings[statType]
  }

  /**
   * Maps a gg position enum to a Charm position enum.
   *
   * @param positionType gg position
   * @return mapped position, or null when unsupported
   */
  CharmPosition mapPosition(PositionType positionType) {
    if (positionType == null) {
      return CharmPosition.IDENTITY
    }
    positionMappings[positionType]
  }

  /**
   * Maps a gg coord instance to a Charm coord type.
   *
   * @param coord gg coord
   * @return mapped coord type, or null when unsupported
   */
  CoordType mapCoordType(GgCoord coord) {
    if (coord == null) {
      return CoordType.CARTESIAN
    }
    for (Map.Entry<Class<? extends GgCoord>, CoordType> entry : coordMappings.entrySet()) {
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
      return CharmScale.time()
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
