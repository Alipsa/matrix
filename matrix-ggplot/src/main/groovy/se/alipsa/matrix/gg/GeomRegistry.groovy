package se.alipsa.matrix.gg


import se.alipsa.matrix.gg.geom.Geom
import se.alipsa.matrix.gg.geom.GeomArea
import se.alipsa.matrix.gg.geom.GeomBar
import se.alipsa.matrix.gg.geom.GeomBin2d
import se.alipsa.matrix.gg.geom.GeomBoxplot
import se.alipsa.matrix.gg.geom.GeomCol
import se.alipsa.matrix.gg.geom.GeomContour
import se.alipsa.matrix.gg.geom.GeomContourFilled
import se.alipsa.matrix.gg.geom.GeomCount
import se.alipsa.matrix.gg.geom.GeomCrossbar
import se.alipsa.matrix.gg.geom.GeomDensity
import se.alipsa.matrix.gg.geom.GeomDensity2d
import se.alipsa.matrix.gg.geom.GeomDensity2dFilled
import se.alipsa.matrix.gg.geom.GeomDotplot
import se.alipsa.matrix.gg.geom.GeomErrorbar
import se.alipsa.matrix.gg.geom.GeomErrorbarh
import se.alipsa.matrix.gg.geom.GeomFreqpoly
import se.alipsa.matrix.gg.geom.GeomFunction
import se.alipsa.matrix.gg.geom.GeomHex
import se.alipsa.matrix.gg.geom.GeomHistogram
import se.alipsa.matrix.gg.geom.GeomJitter
import se.alipsa.matrix.gg.geom.GeomLabel
import se.alipsa.matrix.gg.geom.GeomLine
import se.alipsa.matrix.gg.geom.GeomLinerange
import se.alipsa.matrix.gg.geom.GeomPath
import se.alipsa.matrix.gg.geom.GeomPoint
import se.alipsa.matrix.gg.geom.GeomPointrange
import se.alipsa.matrix.gg.geom.GeomPolygon
import se.alipsa.matrix.gg.geom.GeomQq
import se.alipsa.matrix.gg.geom.GeomQqLine
import se.alipsa.matrix.gg.geom.GeomQuantile
import se.alipsa.matrix.gg.geom.GeomRaster
import se.alipsa.matrix.gg.geom.GeomRect
import se.alipsa.matrix.gg.geom.GeomRibbon
import se.alipsa.matrix.gg.geom.GeomRug
import se.alipsa.matrix.gg.geom.GeomSegment
import se.alipsa.matrix.gg.geom.GeomSmooth
import se.alipsa.matrix.gg.geom.GeomSpoke
import se.alipsa.matrix.gg.geom.GeomStep
import se.alipsa.matrix.gg.geom.GeomText
import se.alipsa.matrix.gg.geom.GeomTile
import se.alipsa.matrix.gg.geom.GeomViolin

/**
 * Shared geom name registry used by stat geom selection and qplot overrides.
 */
@SuppressWarnings(['DuplicateMapLiteral', 'DuplicateStringLiteral'])
class GeomRegistry {

  private static final Closure<Geom> BIN2D_FACTORY = { Map p -> new GeomBin2d(p) }
  private static final Closure<Geom> CONTOUR_FILLED_FACTORY = { Map p -> new GeomContourFilled(p) }
  private static final Closure<Geom> DENSITY_2D_FACTORY = { Map p -> new GeomDensity2d(p) }
  private static final Closure<Geom> DENSITY_2D_FILLED_FACTORY = { Map p -> new GeomDensity2dFilled(p) }

  // Included stat-compatible geom names:
  // area, bar, bin2d, bin_2d, boxplot, col, contour, contour_filled, contourf,
  // count, crossbar, density, density_2d, density2d, density_2d_filled,
  // density2d_filled, dotplot, errorbar, errorbarh, freqpoly, function, hex,
  // histogram, jitter, label, line, linerange, path, point, pointrange, polygon,
  // qq, qq_line, quantile, raster, rect, ribbon, rug, segment, smooth, spoke,
  // step, text, tile, violin.
  //
  // Excluded dedicated-factory-only geom names:
  // abline, hline, vline: reference-line annotation geoms with no stat equivalent.
  // curve: requires paired endpoints (xend/yend) that general stat output cannot provide.
  // sf, sf_text, sf_label, map, mag: GIS/map geoms requiring spatial data.
  // parallel: plot-layout geom, not a data geom.
  // lm: convenience wrapper around GeomSmooth(method: 'lm'), not a distinct class.
  // blank, point_sampled: utility geoms with no output or specialized sampling semantics.
  //
  // Keep all keys lowercase. Callers normalize with toLowerCase(Locale.ROOT) before lookup.
  static final Map<String, Closure<Geom>> GEOM_REGISTRY = Collections.unmodifiableMap(new LinkedHashMap<String, Closure<Geom>>([
      area              : { Map p -> new GeomArea(p) },
      bar               : { Map p -> new GeomBar(p) },
      bin2d             : BIN2D_FACTORY,
      bin_2d            : BIN2D_FACTORY,
      boxplot           : { Map p -> new GeomBoxplot(p) },
      col               : { Map p -> new GeomCol(p) },
      contour           : { Map p -> new GeomContour(p) },
      contour_filled    : CONTOUR_FILLED_FACTORY,
      contourf          : CONTOUR_FILLED_FACTORY,
      count             : { Map p -> new GeomCount(p) },
      crossbar          : { Map p -> new GeomCrossbar(p) },
      density           : { Map p -> new GeomDensity(p) },
      density_2d        : DENSITY_2D_FACTORY,
      density2d         : DENSITY_2D_FACTORY,
      density_2d_filled : DENSITY_2D_FILLED_FACTORY,
      density2d_filled  : DENSITY_2D_FILLED_FACTORY,
      dotplot           : { Map p -> new GeomDotplot(p) },
      errorbar          : { Map p -> new GeomErrorbar(p) },
      errorbarh         : { Map p -> new GeomErrorbarh(p) },
      freqpoly          : { Map p -> new GeomFreqpoly(p) },
      function          : { Map p -> new GeomFunction(p) },
      hex               : { Map p -> new GeomHex(p) },
      histogram         : { Map p -> new GeomHistogram(p) },
      jitter            : { Map p -> new GeomJitter(p) },
      label             : { Map p -> new GeomLabel(p) },
      line              : { Map p -> new GeomLine(p) },
      linerange         : { Map p -> new GeomLinerange(p) },
      path              : { Map p -> new GeomPath(p) },
      point             : { Map p -> new GeomPoint(p) },
      pointrange        : { Map p -> new GeomPointrange(p) },
      polygon           : { Map p -> new GeomPolygon(p) },
      qq                : { Map p -> new GeomQq(p) },
      qq_line           : { Map p -> new GeomQqLine(p) },
      quantile          : { Map p -> new GeomQuantile(p) },
      raster            : { Map p -> new GeomRaster(p) },
      rect              : { Map p -> new GeomRect(p) },
      ribbon            : { Map p -> new GeomRibbon(p) },
      rug               : { Map p -> new GeomRug(p) },
      segment           : { Map p -> new GeomSegment(p) },
      smooth            : { Map p -> new GeomSmooth(p) },
      spoke             : { Map p -> new GeomSpoke(p) },
      step              : { Map p -> new GeomStep(p) },
      text              : { Map p -> new GeomText(p) },
      tile              : { Map p -> new GeomTile(p) },
      violin            : { Map p -> new GeomViolin(p) },
  ]))
}
