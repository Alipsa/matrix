package se.alipsa.matrix.gg

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Svg
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.coord.CoordCartesian
import se.alipsa.matrix.gg.facet.Facet
import se.alipsa.matrix.gg.geom.Geom
import se.alipsa.matrix.gg.geom.GeomBar
import se.alipsa.matrix.gg.geom.GeomCol
import se.alipsa.matrix.gg.geom.GeomErrorbar
import se.alipsa.matrix.gg.geom.GeomPoint
import se.alipsa.matrix.gg.geom.GeomSegment
import se.alipsa.matrix.gg.geom.GeomSmooth
import se.alipsa.matrix.gg.layer.Layer
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.position.Position
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.render.GgRenderer
import se.alipsa.matrix.gg.stat.Stats
import se.alipsa.matrix.gg.theme.Theme

/**
 * Main chart container for Grammar of Graphics plots.
 * Collects layers, scales, coordinates, themes, and facets,
 * then renders them to SVG on demand.
 */
@CompileStatic
class GgChart {

  /** Base dataset for the chart */
  Matrix data

  /** Global aesthetic mappings */
  Aes globalAes

  /** Composition layers */
  List<Layer> layers = []

  /** Coordinate system (default: Cartesian) */
  Coord coord

  /** Scale specifications */
  List<Scale> scales = []

  /** Theme configuration */
  Theme theme

  /** Faceting specification */
  Facet facet

  /** Labels (title, subtitle, axis labels, etc.) */
  Label labels

  /** Guide specifications for legends/colorbars */
  Guides guides

  /** Chart width in pixels */
  int width = 800

  /** Chart height in pixels */
  int height = 600

  private static final Set<String> STAT_PARAM_KEYS = [
      'method', 'n', 'se', 'level', 'formula', 'degree',
      'bins', 'binwidth', 'fun', 'fun.y', 'fun.data', 'fun.args', 'width', 'coef',
      'geometry', 'quantiles'
  ] as Set<String>

  private static final Map<PositionType, Set<String>> POSITION_PARAM_KEYS = [
      (PositionType.DODGE): ['width'] as Set<String>,
      (PositionType.DODGE2): ['width', 'padding', 'reverse'] as Set<String>,
      (PositionType.STACK): ['reverse'] as Set<String>,
      (PositionType.FILL): ['reverse'] as Set<String>,
      (PositionType.JITTER): ['width', 'height', 'seed'] as Set<String>,
      (PositionType.NUDGE): ['x', 'y'] as Set<String>
  ] as Map<PositionType, Set<String>>

  /**
   * Create a new chart with data and global aesthetics.
   */
  GgChart(Matrix data, Aes aes) {
    this.data = data
    this.globalAes = aes
  }

  /**
   * Add a geometric layer to the chart.
   * Creates a new Layer with the geom and default stat/position.
   */
  GgChart plus(Geom geom) {
    // Extract stat-related params from geom params
    Map statParams = [:]
    Aes layerAes = null
    StatType statOverride = geom.defaultStat
    PositionType positionOverride = geom.defaultPosition
    Map positionParams = [:]
    if (geom.params) {
      // Copy params that are relevant for stats
      STAT_PARAM_KEYS.each { key ->
        if (geom.params.containsKey(key)) {
          statParams[key] = geom.params[key]
        }
      }
      if (geom.params.containsKey('stat')) {
        StatType parsed = parseStatType(geom.params['stat'])
        if (parsed != null) {
          statOverride = parsed
        }
      }
      if (geom.params.containsKey('position')) {
        Position positionSpec = parsePositionSpec(geom.params['position'])
        if (positionSpec != null) {
          positionOverride = positionSpec.positionType
          if (positionSpec.params) {
            positionParams.putAll(positionSpec.params)
          }
        }
      }
      positionParams.putAll(extractPositionParams(positionOverride, geom.params, positionParams))
      // Extract mapping parameter as layer aes
      if (geom.params.containsKey('mapping')) {
        def mapping = geom.params['mapping']
        if (mapping instanceof Aes) {
          layerAes = mapping as Aes
        }
      }
    }

    Layer layer = new Layer(
        geom: geom,
        stat: statOverride,
        position: positionOverride,
        aes: layerAes,  // Layer-specific aesthetics from mapping parameter
        params: geom.params ?: [:],
        statParams: statParams,
        positionParams: positionParams
    )
    layers << layer
    return this
  }

  /**
   * Parse a stat specification into a StatType enum value.
   * <p>
   * This method accepts several input types:
   * <ul>
   *   <li>A {@link StatType} enum value (returned as-is)</li>
   *   <li>A {@link Stats} object (extracts its statType property)</li>
   *   <li>A string or CharSequence (case-insensitive matching to known stat names)</li>
   * </ul>
   * Supported stat names are: 'identity', 'count', 'bin', 'boxplot', 'smooth',
   * 'summary', 'density', 'ydensity', 'bin2d', 'contour', 'ecdf', 'qq', 'qq_line',
   * 'unique', 'function', 'sf', and 'sf_coordinates'.
   *
   * @param stat the stat specification to parse (may be null, StatType, Stat, or String)
   * @return the corresponding StatType, or null if the input is null
   * @throws IllegalArgumentException if the stat string is not recognized or the type is unsupported
   */
  private static StatType parseStatType(Object stat) {
    if (stat == null) {
      return null
    }
    if (stat instanceof StatType) {
      return stat as StatType
    }
    if (stat instanceof Stats) {
      return (stat as Stats).statType
    }
    if (stat instanceof CharSequence) {
      switch (stat.toString().trim().toLowerCase(Locale.ROOT)) {
        case 'identity':
          return StatType.IDENTITY
        case 'count':
          return StatType.COUNT
        case 'bin':
          return StatType.BIN
        case 'boxplot':
          return StatType.BOXPLOT
        case 'smooth':
          return StatType.SMOOTH
        case 'summary':
          return StatType.SUMMARY
        case 'density':
          return StatType.DENSITY
        case 'ydensity':
          return StatType.YDENSITY
        case 'bin2d':
          return StatType.BIN2D
        case 'contour':
          return StatType.CONTOUR
        case 'ecdf':
          return StatType.ECDF
        case 'qq':
          return StatType.QQ
        case 'qq_line':
        case 'qqline':
          return StatType.QQ_LINE
        case 'unique':
          return StatType.UNIQUE
        case 'function':
          return StatType.FUNCTION
        case 'sf':
          return StatType.SF
        case 'sf_coordinates':
        case 'sf_coords':
          return StatType.SF_COORDINATES
        default:
          throw new IllegalArgumentException("Unsupported stat: ${stat}")
      }
    }
    throw new IllegalArgumentException("Unsupported stat type: ${stat.getClass().name}")
  }

  private static Position parsePositionSpec(def position) {
    if (position == null) {
      return null
    }
    if (position instanceof Position) {
      return position as Position
    }
    if (position instanceof PositionType) {
      return new Position(position as PositionType)
    }
    if (position instanceof CharSequence) {
      PositionType parsed = parsePositionType(position.toString())
      if (parsed != null) {
        return new Position(parsed)
      }
    }
    return null
  }

  private static PositionType parsePositionType(String position) {
    if (position == null) {
      return null
    }
    switch (position.toLowerCase(Locale.ROOT)) {
      case 'identity':
        return PositionType.IDENTITY
      case 'dodge':
        return PositionType.DODGE
      case 'dodge2':
        return PositionType.DODGE2
      case 'stack':
        return PositionType.STACK
      case 'fill':
        return PositionType.FILL
      case 'jitter':
        return PositionType.JITTER
      case 'nudge':
        return PositionType.NUDGE
      default:
        throw new IllegalArgumentException(
            "Unknown position type: '${position}'. Supported types: identity, dodge, dodge2, stack, fill, jitter, nudge")
    }
  }

  private static Map extractPositionParams(PositionType positionType, Map params, Map existing = [:]) {
    if (positionType == null || params == null) {
      return existing
    }
    Set<String> keys = POSITION_PARAM_KEYS[positionType]
    if (keys == null || keys.isEmpty()) {
      return existing
    }
    Map result = new LinkedHashMap<>(existing)
    keys.each { key ->
      if (params.containsKey(key) && !result.containsKey(key)) {
        result[key] = params[key]
      }
    }
    return result
  }

  /**
   * Add multiple components (geoms, stats, scales, themes, etc.) in a single step.
   * Nested lists are flattened.
   */
  GgChart plus(Iterable<?> parts) {
    if (parts == null) {
      return this
    }
    for (Object part : parts) {
      if (part == null) {
        continue
      }
      if (part instanceof Iterable) {
        plus(part as Iterable<?>)
        continue
      }
      if (part instanceof Geom) {
        plus(part as Geom)
        continue
      }
      if (part instanceof Theme) {
        plus(part as Theme)
        continue
      }
      if (part instanceof Layer) {
        plus(part as Layer)
        continue
      }
      if (part instanceof Scale) {
        plus(part as Scale)
        continue
      }
      if (part instanceof Facet) {
        plus(part as Facet)
        continue
      }
      if (part instanceof Coord) {
        plus(part as Coord)
        continue
      }
      if (part instanceof Label) {
        plus(part as Label)
        continue
      }
      if (part instanceof Guides) {
        plus(part as Guides)
        continue
      }
      if (part instanceof Stats) {
        plus(part as Stats)
        continue
      }
      throw new IllegalArgumentException("Unsupported gg component: ${part.getClass().name}")
    }
    return this
  }

  /**
   * Set or merge the theme for the chart.
   * If a theme is already set, the new theme is merged with it
   * (new theme's non-null properties override existing ones).
   */
  GgChart plus(Theme theme) {
    if (this.theme == null) {
      this.theme = theme
    } else {
      // Merge with existing theme
      this.theme = this.theme + theme
    }
    return this
  }

  /**
   * Add a pre-built layer.
   */
  GgChart plus(Layer layer) {
    if (layer == null) {
      return this
    }
    layers << layer
    return this
  }

  /**
   * Add a scale specification.
   */
  GgChart plus(Scale scale) {
    scales << scale
    return this
  }

  /**
   * Set the faceting specification.
   */
  GgChart plus(Facet facet) {
    this.facet = facet
    return this
  }

  /**
   * Set the coordinate system.
   */
  GgChart plus(Coord coord) {
    this.coord = coord
    return this
  }

  /**
   * Set labels (title, subtitle, axis labels, etc.)
   */
  GgChart plus(Label labels) {
    if (labels == null) {
      return this
    }
    if (this.labels == null) {
      this.labels = labels
      return this
    }
    if (labels.title) this.labels.title = labels.title
    if (labels.subTitle) this.labels.subTitle = labels.subTitle
    if (labels.caption) this.labels.caption = labels.caption
    if (labels.legendTitle) this.labels.legendTitle = labels.legendTitle
    if (labels.xSet) {
      this.labels.x = labels.x
      this.labels.xSet = true
    }
    if (labels.ySet) {
      this.labels.y = labels.y
      this.labels.ySet = true
    }
    return this
  }

  /**
   * Add guide specifications for legends/colorbars.
   */
  GgChart plus(Guides guides) {
    if (guides == null) {
      return this
    }
    if (this.guides == null) {
      this.guides = guides
    } else {
      this.guides = this.guides + guides
    }
    return this
  }

  /**
   * Add a statistical transformation.
   * Creates a new layer with the stat applied.
   * Note: In ggplot2, stats can create their own geom (e.g., stat_summary with geom: "line")
   */
  GgChart plus(Stats stat) {
    if (stat == null) {
      return this
    }

    Map statParams = [:]
    Map geomParams = [:]
    if (stat.params) {
      stat.params.each { key, value ->
        if (STAT_PARAM_KEYS.contains(key as String)) {
          statParams[key] = value
        } else {
          geomParams[key] = value
        }
      }
    }
    Geom geom = null
    Object geomParam = statParams.remove('geom') ?: geomParams.remove('geom')
    if (geomParam instanceof Geom) {
      geom = geomParam as Geom
    } else if (geomParam instanceof String) {
      geom = geomFromName(geomParam as String, geomParams)
    }

    Layer layer = new Layer(
        geom: geom,
        stat: stat.statType ?: StatType.IDENTITY,
        position: geom?.defaultPosition ?: PositionType.IDENTITY,
        aes: null,
        params: geom?.params ?: [:],
        statParams: statParams
    )
    layers << layer
    return this
  }

  private static Geom geomFromName(String name, Map params = [:]) {
    if (name == null) {
      return null
    }
    switch (name) {
      case 'point':
        return new GeomPoint(params ?: [:])
      case 'smooth':
        return new GeomSmooth(params ?: [:])
      case 'bar':
        return new GeomBar(params ?: [:])
      case 'col':
        return new GeomCol(params ?: [:])
      case 'errorbar':
        return new GeomErrorbar(params ?: [:])
      case 'segment':
        return new GeomSegment(params ?: [:])
      default:
        return null
    }
  }

  /**
   * Render the chart to SVG.
   * Always produces a new Svg object.
   * @return The rendered SVG object
   */
  Svg render() {
    // Use default coordinate system if not set
    if (coord == null) {
      coord = new CoordCartesian()
    }

    // Use GgRenderer to render the chart
    GgRenderer renderer = new GgRenderer()
    return renderer.render(this)
  }
}
