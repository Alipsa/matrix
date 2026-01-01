package se.alipsa.matrix.gg

import groovy.transform.CompileStatic
import se.alipsa.groovy.svg.Svg
import se.alipsa.groovy.svg.io.SvgWriter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.coord.Coord
import se.alipsa.matrix.gg.coord.CoordCartesian
import se.alipsa.matrix.gg.facet.Facet
import se.alipsa.matrix.gg.geom.Geom
import se.alipsa.matrix.gg.geom.GeomPoint
import se.alipsa.matrix.gg.geom.GeomSmooth
import se.alipsa.matrix.gg.layer.Layer
import se.alipsa.matrix.gg.layer.StatType
import se.alipsa.matrix.gg.layer.PositionType
import se.alipsa.matrix.gg.scale.Scale
import se.alipsa.matrix.gg.render.GgRenderer
import se.alipsa.matrix.gg.stat.Stat
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

  /** Chart width in pixels */
  int width = 800

  /** Chart height in pixels */
  int height = 600

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
    if (geom.params) {
      // Copy params that are relevant for stats
      ['method', 'n', 'se', 'level', 'formula', 'degree', 'bins', 'binwidth', 'fun', 'fun.y'].each { key ->
        if (geom.params.containsKey(key)) {
          statParams[key] = geom.params[key]
        }
      }
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
        stat: geom.defaultStat,
        position: geom.defaultPosition,
        aes: layerAes,  // Layer-specific aesthetics from mapping parameter
        params: geom.params ?: [:],
        statParams: statParams
    )
    layers << layer
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
    this.labels = labels
    return this
  }

  /**
   * Add a statistical transformation.
   * Creates a new layer with the stat applied.
   * Note: In ggplot2, stats can create their own geom (e.g., stat_summary with geom: "line")
   */
  GgChart plus(Stat stat) {
    if (stat == null) {
      return this
    }

    Map statParams = stat.params ? new LinkedHashMap<>(stat.params) : [:]
    Geom geom = null
    Object geomParam = statParams.remove('geom')
    if (geomParam instanceof Geom) {
      geom = geomParam as Geom
    } else if (geomParam instanceof String) {
      geom = geomFromName(geomParam as String)
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

  private static Geom geomFromName(String name) {
    if (name == null) {
      return null
    }
    switch (name) {
      case 'point':
        return new GeomPoint()
      case 'smooth':
        return new GeomSmooth()
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
