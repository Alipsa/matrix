package se.alipsa.matrix.gg.geom

import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.CharmGeomType
import se.alipsa.matrix.charm.CharmPositionType
import se.alipsa.matrix.charm.CharmStatType
import se.alipsa.matrix.charm.GeomSpec
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.layer.PositionType
import java.util.Locale

/**
 * Base class for geometric objects (geoms).
 * Geoms determine how data is visually represented (points, lines, bars, etc.).
 */
@CompileStatic
class Geom {

  /** Default statistical transformation for this geom */
  StatType defaultStat = StatType.IDENTITY

  /** Default position adjustment for this geom */
  PositionType defaultPosition = PositionType.IDENTITY

  /** Fixed aesthetic parameters (not mapped to data) */
  Map params = [:]

  /** Required aesthetic mappings for this geom */
  List<String> requiredAes = []

  /** Optional aesthetic mappings with defaults */
  Map<String, Object> defaultAes = [:]

  /** Canonical Charm geom spec used by the direct delegation path. */
  GeomSpec geomSpec

  private static final Map<StatType, CharmStatType> STAT_TYPE_MAP = [
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

  private static final Map<PositionType, CharmPositionType> POSITION_TYPE_MAP = [
      (PositionType.IDENTITY): CharmPositionType.IDENTITY,
      (PositionType.DODGE)   : CharmPositionType.DODGE,
      (PositionType.DODGE2)  : CharmPositionType.DODGE2,
      (PositionType.STACK)   : CharmPositionType.STACK,
      (PositionType.FILL)    : CharmPositionType.FILL,
      (PositionType.JITTER)  : CharmPositionType.JITTER,
      (PositionType.NUDGE)   : CharmPositionType.NUDGE
  ] as Map<PositionType, CharmPositionType>

  /**
   * Returns this geom represented as a Charm {@link GeomSpec}.
   * The spec is lazily created and then cached.
   *
   * @return charm geom spec
   */
  GeomSpec toCharmGeomSpec() {
    if (geomSpec == null) {
      geomSpec = buildGeomSpec()
    }
    geomSpec.copy()
  }

  private GeomSpec buildGeomSpec() {
    CharmGeomType geomType = resolveGeomType()
    Map<String, Object> geomParams = params == null ? [:] : new LinkedHashMap<>(params as Map<String, Object>)
    List<String> geomRequiredAes = requiredAes == null ? [] : new ArrayList<>(requiredAes)
    Map<String, Object> geomDefaultAes = defaultAes == null ? [:] : new LinkedHashMap<>(defaultAes)
    new GeomSpec(
        geomType,
        geomParams,
        geomRequiredAes,
        geomDefaultAes,
        STAT_TYPE_MAP[defaultStat] ?: CharmStatType.IDENTITY,
        POSITION_TYPE_MAP[defaultPosition] ?: CharmPositionType.IDENTITY
    )
  }

  private static final Map<String, CharmGeomType> GEOM_NAME_OVERRIDES = [
      'Bin2d'          : CharmGeomType.BIN2D,
      'Density2d'      : CharmGeomType.DENSITY_2D,
      'Density2dFilled': CharmGeomType.DENSITY_2D_FILLED
  ]

  private CharmGeomType resolveGeomType() {
    String simpleName = this.class.simpleName
    String withoutPrefix = simpleName.startsWith('Geom') ? simpleName.substring(4) : simpleName
    CharmGeomType override = GEOM_NAME_OVERRIDES[withoutPrefix]
    if (override != null) {
      return override
    }
    String enumName = withoutPrefix
        .replaceAll(/([a-z])([A-Z])/, '$1_$2')
        .replaceAll(/([A-Za-z])([0-9])/, '$1_$2')
        .replaceAll(/([0-9])([A-Za-z])/, '$1_$2')
        .toUpperCase(Locale.ROOT)
    try {
      return CharmGeomType.valueOf(enumName)
    } catch (IllegalArgumentException ignored) {
      throw new IllegalArgumentException("Unable to map gg geom '${simpleName}' to CharmGeomType '${enumName}'")
    }
  }

}
